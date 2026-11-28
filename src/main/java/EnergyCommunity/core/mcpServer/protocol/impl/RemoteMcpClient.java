package EnergyCommunity.core.mcpServer.protocol.impl;

import EnergyCommunity.core.mcpServer.protocol.McpServerContract;
import EnergyCommunity.core.mcpServer.protocol.McpServerInfo;
import EnergyCommunity.core.mcpServer.protocol.McpTool;
import EnergyCommunity.core.mcpServer.protocol.McpToolCallRequest;
import EnergyCommunity.core.mcpServer.protocol.McpToolResult;
import EnergyCommunity.core.mcpServer.protocol.McpToolSchema;
import EnergyCommunity.core.mcpServer.util.McpJsonMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Remote HTTP JSON-RPC 2.0 MCP client implementing {@link McpServerContract}.
 * Enables AI agents to connect over HTTP to a remotely deployed {@link RemoteHttpMcpServer}.
 */
public class RemoteMcpClient implements McpServerContract {

    private final String endpointUrl;
    private final HttpClient httpClient;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private volatile McpServerInfo serverInfo;

    public RemoteMcpClient(String endpointUrl) {
        this(endpointUrl, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    public RemoteMcpClient(String endpointUrl, HttpClient httpClient) {
        this.endpointUrl = Objects.requireNonNull(endpointUrl, "endpointUrl must not be null").trim();
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public void start() {
        JsonNode result = sendJsonRpc("initialize", Map.of(
                "protocolVersion", RemoteHttpMcpServer.PROTOCOL_VERSION,
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "energy-optimization-agent", "version", "1.0.0")
        )).path("result");

        this.serverInfo = new McpServerInfo(
                result.path("serverInfo").path("name").asText(RemoteHttpMcpServer.SERVER_NAME),
                result.path("serverInfo").path("version").asText(RemoteHttpMcpServer.SERVER_VERSION),
                result.path("protocolVersion").asText(RemoteHttpMcpServer.PROTOCOL_VERSION),
                endpointUrl
        );
        sendNotification("notifications/initialized");
        this.connected.set(true);
    }

    @Override
    public void stop() {
        this.connected.set(false);
    }

    @Override
    public boolean isRunning() {
        return connected.get();
    }

    @Override
    public McpServerInfo getServerInfo() {
        return serverInfo != null ? serverInfo : new McpServerInfo(
                RemoteHttpMcpServer.SERVER_NAME, RemoteHttpMcpServer.SERVER_VERSION,
                RemoteHttpMcpServer.PROTOCOL_VERSION, endpointUrl);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<McpTool> listTools() {
        JsonNode toolsNode = sendJsonRpc("tools/list", Map.of()).path("result").path("tools");
        List<McpTool> tools = new ArrayList<>();
        if (toolsNode.isArray()) {
            for (JsonNode node : toolsNode) {
                JsonNode schemaNode = node.path("inputSchema");
                Map<String, Map<String, Object>> props = new LinkedHashMap<>();
                if (schemaNode.has("properties") && schemaNode.get("properties").isObject()) {
                    McpJsonMapper.toMap(schemaNode.get("properties")).forEach((k, v) -> {
                        if (v instanceof Map<?, ?> propMap) props.put(k, (Map<String, Object>) propMap);
                    });
                }
                List<String> required = new ArrayList<>();
                schemaNode.path("required").forEach(req -> required.add(req.asText()));
                McpToolSchema schema = new McpToolSchema(schemaNode.path("type").asText("object"), props, List.copyOf(required));
                tools.add(new RemoteToolProxy(node.path("name").asText(), node.path("description").asText(""), schema));
            }
        }
        return List.copyOf(tools);
    }

    @Override
    public McpToolResult callTool(McpToolCallRequest request) {
        String reqId = (request.requestId() != null && !request.requestId().isBlank())
                ? request.requestId() : UUID.randomUUID().toString();
        try {
            JsonNode response = sendJsonRpc("tools/call", Map.of(
                    "name", request.toolName(),
                    "arguments", request.arguments() != null ? request.arguments() : Map.of()
            ));
            if (response.hasNonNull("error")) {
                return McpToolResult.error(reqId, request.toolName(), response.path("error").path("message").asText("Remote MCP error"));
            }
            JsonNode result = response.path("result");
            String text = result.path("content").isArray() && !result.path("content").isEmpty()
                    ? result.path("content").get(0).path("text").asText("") : "";
            return new McpToolResult(reqId, request.toolName(), result.path("isError").asBoolean(false), text, null);
        } catch (Exception e) {
            return McpToolResult.error(reqId, request.toolName(), "Remote MCP call failed: " + e.getMessage());
        }
    }

    private JsonNode sendJsonRpc(String method, Map<String, Object> params) {
        Map<String, Object> body = Map.of("jsonrpc", "2.0", "id", UUID.randomUUID().toString(), "method", method, "params", params);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(McpJsonMapper.toJson(body)))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new IllegalStateException("Remote MCP server returned HTTP " + resp.statusCode());
            }
            return McpJsonMapper.parseTree(resp.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to call remote MCP method '" + method + "': " + e.getMessage(), e);
        }
    }

    private void sendNotification(String method) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(McpJsonMapper.toJson(Map.of("jsonrpc", "2.0", "method", method))))
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    private final class RemoteToolProxy implements McpTool {
        private final String name;
        private final String description;
        private final McpToolSchema schema;

        private RemoteToolProxy(String name, String description, McpToolSchema schema) {
            this.name = name;
            this.description = description;
            this.schema = schema;
        }

        @Override public String getName() { return name; }
        @Override public String getDescription() { return description; }
        @Override public McpToolSchema getInputSchema() { return schema; }
        @Override public McpToolResult execute(Map<String, Object> arguments) {
            return callTool(new McpToolCallRequest(UUID.randomUUID().toString(), name, arguments));
        }
    }
}
