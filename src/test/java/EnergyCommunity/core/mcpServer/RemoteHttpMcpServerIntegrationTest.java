package EnergyCommunity.core.mcpServer;

import EnergyCommunity.core.mcpServer.port.ForecastQueryPort;
import EnergyCommunity.core.mcpServer.port.HouseStatusQueryPort;
import EnergyCommunity.core.mcpServer.port.ProposalStoreContract;
import EnergyCommunity.core.mcpServer.port.impl.InMemoryProposalStore;
import EnergyCommunity.core.mcpServer.protocol.McpTool;
import EnergyCommunity.core.mcpServer.protocol.McpToolCallRequest;
import EnergyCommunity.core.mcpServer.protocol.McpToolResult;
import EnergyCommunity.core.mcpServer.protocol.impl.DefaultMcpToolRegistry;
import EnergyCommunity.core.mcpServer.protocol.impl.RemoteHttpMcpServer;
import EnergyCommunity.core.mcpServer.protocol.impl.RemoteMcpClient;
import EnergyCommunity.core.mcpServer.tools.impl.GetForecastsTool;
import EnergyCommunity.core.mcpServer.tools.impl.GetHouseStatusTool;
import EnergyCommunity.core.mcpServer.tools.impl.ProposeBatteryScheduleTool;
import EnergyCommunity.core.mcpServer.tools.model.ForecastPointData;
import EnergyCommunity.core.mcpServer.tools.model.ForecastReadResult;
import EnergyCommunity.core.mcpServer.tools.model.GeoLocationInput;
import EnergyCommunity.core.mcpServer.tools.model.HouseStatusReadResult;
import EnergyCommunity.core.mcpServer.util.McpJsonMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RemoteHttpMcpServerIntegrationTest {

    private RemoteHttpMcpServer server;
    private ProposalStoreContract proposalStore;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        HouseStatusQueryPort housePort = new HouseStatusQueryPort() {
            private final HouseStatusReadResult sample = new HouseStatusReadResult(
                    "1", "101", new GeoLocationInput(45.7489, 21.2087, "Timisoara"),
                    82, 15.0, 1.4, 3.8, false, Instant.parse("2026-11-26T10:00:00Z")
            );

            @Override
            public Optional<HouseStatusReadResult> fetchHouseStatusByUserId(String userId) {
                return "1".equals(userId) ? Optional.of(sample) : Optional.empty();
            }

            @Override
            public List<String> listActiveUserIdsForOptimization() {
                return List.of("1");
            }
        };

        ForecastQueryPort forecastPort = loc -> new ForecastReadResult(
                loc, Instant.parse("2026-11-26T10:00:00Z"), 24,
                List.of(new ForecastPointData(Instant.parse("2026-11-27T19:00:00Z"), 0.0, 0.46))
        );

        proposalStore = new InMemoryProposalStore();
        DefaultMcpToolRegistry registry = new DefaultMcpToolRegistry();
        registry.registerTool(new GetHouseStatusTool(housePort));
        registry.registerTool(new GetForecastsTool(forecastPort));
        registry.registerTool(new ProposeBatteryScheduleTool(proposalStore));

        server = new RemoteHttpMcpServer("127.0.0.1", 0, registry);
        server.attachOperationalComponents(null, null, proposalStore);
        server.start();

        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        baseUrl = "http://127.0.0.1:" + server.getBoundPort();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void jsonRpcEndpointSupportsInitializeListToolsCallToolsAndErrors() throws Exception {
        HttpResponse<String> init = postJson(baseUrl + "/mcp",
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
        assertEquals(200, init.statusCode());
        assertEquals("2025-03-26", McpJsonMapper.parseTree(init.body()).path("result").path("protocolVersion").asText());

        HttpResponse<String> list = postJson(baseUrl + "/mcp",
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}");
        assertEquals(3, McpJsonMapper.parseTree(list.body()).path("result").path("tools").size());

        HttpResponse<String> unknown = postJson(baseUrl + "/mcp",
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"unknown/op\",\"params\":{}}");
        assertEquals(-32601, McpJsonMapper.parseTree(unknown.body()).path("error").path("code").asInt());
    }

    @Test
    void remoteMcpClientInvokesAllThreeToolsOverHttp() {
        RemoteMcpClient client = new RemoteMcpClient(baseUrl + "/mcp");
        client.start();
        assertTrue(client.isRunning());

        List<McpTool> tools = client.listTools();
        assertEquals(3, tools.size());

        McpToolResult status = client.callTool(new McpToolCallRequest("r1", "get_house_status", Map.of("user_id", "1")));
        assertFalse(status.isError());
        assertTrue(status.contentJson().contains("\"batteryLevelPercent\":82"));

        McpToolResult forecast = client.callTool(new McpToolCallRequest("r2", "get_forecasts",
                Map.of("latitude", 45.7489, "longitude", 21.2087)));
        assertFalse(forecast.isError());
        assertTrue(forecast.contentJson().contains("0.46"));

        McpToolResult proposal = client.callTool(new McpToolCallRequest("r3", "propose_battery_schedule", Map.of(
                "user_id", "1",
                "action_plan", Map.of("house_id", "101", "action_type", "SELL_TO_GRID", "battery_percentage", 40,
                        "execute_at", "2026-11-27T19:00:00Z", "estimated_profit_eur", 2.76,
                        "description", "Sell 40% at 19:00 UTC")
        )));
        assertFalse(proposal.isError());
        assertEquals(1, proposalStore.getAllProposals().size());

        client.stop();
        assertFalse(client.isRunning());
    }

    @Test
    void healthAndProposalLifecycleEndpointsEnforcePendingConstraint() throws Exception {
        HttpResponse<String> health = get(baseUrl + "/health");
        assertEquals(200, health.statusCode());
        assertEquals("UP", McpJsonMapper.parseTree(health.body()).path("status").asText());

        // Submit one proposal via MCP, then inspect & accept via HTTP
        postJson(baseUrl + "/mcp", """
                {"jsonrpc":"2.0","id":10,"method":"tools/call","params":{"name":"propose_battery_schedule","arguments":{
                  "user_id":"1","action_plan":{"house_id":"101","action_type":"SELL_TO_GRID","battery_percentage":35,
                  "execute_at":"2026-11-27T19:00:00Z","description":"Sell 35% at peak"}}}}
                """);

        JsonNode proposals = McpJsonMapper.parseTree(get(baseUrl + "/api/mcp/proposals/user/1").body());
        assertEquals(1, proposals.size());
        String proposalId = proposals.get(0).path("id").asText();

        HttpResponse<String> accept = postJson(baseUrl + "/api/mcp/proposals/" + proposalId + "/accept", "{}");
        assertEquals(200, accept.statusCode());
        assertEquals("ACCEPTED", McpJsonMapper.parseTree(accept.body()).path("status").asText());

        // Attempting to reject an already ACCEPTED proposal returns HTTP 409 Conflict
        HttpResponse<String> conflict = postJson(baseUrl + "/api/mcp/proposals/" + proposalId + "/reject", "{}");
        assertEquals(409, conflict.statusCode());
    }

    private HttpResponse<String> postJson(String url, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
