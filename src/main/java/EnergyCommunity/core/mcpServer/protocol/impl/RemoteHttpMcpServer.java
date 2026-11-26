package EnergyCommunity.core.mcpServer.protocol.impl;

import EnergyCommunity.core.mcpServer.agent.AgentSchedulerContract;
import EnergyCommunity.core.mcpServer.agent.EnergyOptimizationAgentContract;
import EnergyCommunity.core.mcpServer.port.ProposalStoreContract;
import EnergyCommunity.core.mcpServer.protocol.McpServerContract;
import EnergyCommunity.core.mcpServer.protocol.McpServerInfo;
import EnergyCommunity.core.mcpServer.protocol.McpTool;
import EnergyCommunity.core.mcpServer.protocol.McpToolCallRequest;
import EnergyCommunity.core.mcpServer.protocol.McpToolRegistry;
import EnergyCommunity.core.mcpServer.protocol.McpToolResult;
import EnergyCommunity.core.mcpServer.protocol.McpToolSchema;
import EnergyCommunity.core.mcpServer.util.McpJsonMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Remotely accessible Model Context Protocol (MCP) server over HTTP JSON-RPC 2.0 and SSE.
 * Exposes {@code POST /mcp}, {@code GET /sse} + {@code POST /messages}, {@code GET /health},
 * {@code POST /api/mcp/agent/run}, and {@code /api/mcp/proposals} operational endpoints.
 */
public class RemoteHttpMcpServer implements McpServerContract {

    public static final String SERVER_NAME = "energy-community-mcp-server";
    public static final String SERVER_VERSION = "1.0.0";
    public static final String PROTOCOL_VERSION = "2025-03-26";

    private final String bindHost;
    private final int configuredPort;
    private final McpToolRegistry toolRegistry;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, OutputStream> sseSessions = new ConcurrentHashMap<>();

    private volatile HttpServer httpServer;
    private volatile ExecutorService executorService;
    private volatile int boundPort;

    private volatile EnergyOptimizationAgentContract agent;
    private volatile AgentSchedulerContract scheduler;
    private volatile ProposalStoreContract proposalStore;

    public RemoteHttpMcpServer(String bindHost, int port, McpToolRegistry toolRegistry) {
        this.bindHost = (bindHost == null || bindHost.isBlank()) ? "0.0.0.0" : bindHost.trim();
        this.configuredPort = port;
        this.boundPort = port;
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
    }

    public void attachOperationalComponents(EnergyOptimizationAgentContract agent,
                                            AgentSchedulerContract scheduler,
                                            ProposalStoreContract proposalStore) {
        this.agent = agent;
        this.scheduler = scheduler;
        this.proposalStore = proposalStore;
    }

    public int getBoundPort() {
        return boundPort;
    }

    @Override
    public synchronized void start() {
        if (running.get()) {
            return;
        }
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(bindHost, configuredPort), 0);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            server.setExecutor(executor);

            server.createContext("/mcp", this::handleMcpEndpoint);
            server.createContext("/sse", this::handleSseEndpoint);
            server.createContext("/messages", this::handleSseMessagesEndpoint);
            server.createContext("/health", this::handleHealthEndpoint);
            server.createContext("/api/mcp/agent", this::handleAgentEndpoint);
            server.createContext("/api/mcp/proposals", this::handleProposalsEndpoint);

            server.start();
            this.httpServer = server;
            this.executorService = executor;
            this.boundPort = server.getAddress().getPort();
            this.running.set(true);

            System.out.println("[MCP] Remote MCP Server started at " + getServerInfo().endpointUrl()
                    + " (" + listTools().size() + " tools registered)");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start RemoteHttpMcpServer: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        sseSessions.values().forEach(out -> {
            try {
                out.close();
            } catch (IOException ignored) {
            }
        });
        sseSessions.clear();

        if (httpServer != null) {
            httpServer.stop(1);
            httpServer = null;
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        System.out.println("[MCP] Remote MCP Server stopped.");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public McpServerInfo getServerInfo() {
        String displayHost = "0.0.0.0".equals(bindHost) ? "localhost" : bindHost;
        return new McpServerInfo(SERVER_NAME, SERVER_VERSION, PROTOCOL_VERSION,
                "http://" + displayHost + ":" + boundPort + "/mcp");
    }

    @Override
    public List<McpTool> listTools() {
        return toolRegistry.getAllTools();
    }

    @Override
    public McpToolResult callTool(McpToolCallRequest request) {
        if (request == null || request.toolName() == null || request.toolName().isBlank()) {
            return McpToolResult.error(UUID.randomUUID().toString(), "unknown", "Tool name must not be blank");
        }
        String reqId = (request.requestId() != null && !request.requestId().isBlank())
                ? request.requestId() : UUID.randomUUID().toString();
        Optional<McpTool> toolOpt = toolRegistry.getTool(request.toolName());
        if (toolOpt.isEmpty()) {
            return McpToolResult.error(reqId, request.toolName(), "Unknown MCP tool: " + request.toolName());
        }
        try {
            McpToolResult raw = toolOpt.get().execute(request.arguments() != null ? request.arguments() : Map.of());
            return new McpToolResult(reqId, request.toolName(), raw.isError(), raw.contentJson(), raw.structuredPayload());
        } catch (Exception e) {
            return McpToolResult.error(reqId, request.toolName(), "Tool execution error: " + e.getMessage());
        }
    }

    private void handleMcpEndpoint(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, Map.of("serverInfo", getServerInfo(), "transport", "streamable-http", "tools", serializeToolsList()));
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method Not Allowed. Use POST with JSON-RPC 2.0 body."));
            return;
        }
        DispatchResult result = processJsonRpc(readBody(exchange));
        if (result.notificationOnly()) {
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        } else {
            sendJson(exchange, result.httpStatus(), result.body());
        }
    }

    private void handleSseEndpoint(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Use GET for SSE stream."));
            return;
        }
        String sessionId = UUID.randomUUID().toString();
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, 0);

        OutputStream out = exchange.getResponseBody();
        sseSessions.put(sessionId, out);
        try {
            out.write(("event: endpoint\ndata: /messages?sessionId=" + sessionId + "\n\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            while (running.get()) {
                Thread.sleep(15_000);
                out.write(": keep-alive\n\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (IOException | InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            sseSessions.remove(sessionId);
            exchange.close();
        }
    }

    private void handleSseMessagesEndpoint(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Use POST for MCP messages."));
            return;
        }
        DispatchResult result = processJsonRpc(readBody(exchange));
        String sessionId = extractQueryParam(exchange.getRequestURI().getQuery(), "sessionId");
        if (!result.notificationOnly() && sessionId != null) {
            OutputStream sseOut = sseSessions.get(sessionId);
            if (sseOut != null) {
                try {
                    synchronized (sseOut) {
                        sseOut.write(("event: message\ndata: " + McpJsonMapper.toJson(result.body()) + "\n\n").getBytes(StandardCharsets.UTF_8));
                        sseOut.flush();
                    }
                } catch (IOException ignored) {
                    sseSessions.remove(sessionId);
                }
            }
        }
        if (result.notificationOnly()) {
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        } else {
            sendJson(exchange, result.httpStatus(), result.body());
        }
    }

    private void handleHealthEndpoint(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", isRunning() ? "UP" : "DOWN");
        health.put("server", getServerInfo());
        health.put("toolCount", listTools().size());
        health.put("tools", listTools().stream().map(McpTool::getName).toList());
        health.put("schedulerActive", scheduler != null && scheduler.isSchedulerActive());
        sendJson(exchange, 200, health);
    }

    private void handleAgentEndpoint(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Use POST /api/mcp/agent/run or POST /api/mcp/agent/run/{userId}"));
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if ("/api/mcp/agent/run".equals(path) || "/api/mcp/agent/run/".equals(path)) {
            if (scheduler != null) {
                sendJson(exchange, 200, scheduler.triggerImmediateRun());
            } else if (agent != null) {
                sendJson(exchange, 200, agent.runNightlyCommunityOptimization());
            } else {
                sendJson(exchange, 503, Map.of("error", "Optimization agent is not attached."));
            }
            return;
        }
        if (path.startsWith("/api/mcp/agent/run/") && agent != null) {
            String userId = path.substring("/api/mcp/agent/run/".length()).trim();
            sendJson(exchange, userId.isBlank() ? 400 : 200,
                    userId.isBlank() ? Map.of("error", "Missing userId.") : agent.optimizeEnergyForUser(userId));
            return;
        }
        sendJson(exchange, agent == null ? 503 : 404, Map.of("error", "Agent endpoint unavailable: " + path));
    }

    private void handleProposalsEndpoint(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if (proposalStore == null) {
            sendJson(exchange, 503, Map.of("error", "Proposal store is not attached."));
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        try {
            if ("GET".equalsIgnoreCase(method) && ("/api/mcp/proposals".equals(path) || "/api/mcp/proposals/".equals(path))) {
                sendJson(exchange, 200, proposalStore.getAllProposals());
            } else if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/mcp/proposals/user/")) {
                sendJson(exchange, 200, proposalStore.getProposalsByUser(path.substring("/api/mcp/proposals/user/".length()).trim()));
            } else if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/mcp/proposals/") && path.endsWith("/accept")) {
                String id = path.substring("/api/mcp/proposals/".length(), path.length() - "/accept".length());
                sendJson(exchange, 200, proposalStore.acceptProposal(id));
            } else if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/mcp/proposals/") && path.endsWith("/reject")) {
                String id = path.substring("/api/mcp/proposals/".length(), path.length() - "/reject".length());
                sendJson(exchange, 200, proposalStore.rejectProposal(id));
            } else {
                sendJson(exchange, 404, Map.of("error", "Unknown proposals endpoint: " + path));
            }
        } catch (NoSuchElementException e) {
            sendJson(exchange, 404, Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, Map.of("error", e.getMessage()));
        }
    }

    private DispatchResult processJsonRpc(String rawBody) {
        JsonNode root;
        try {
            root = McpJsonMapper.parseTree(rawBody);
        } catch (Exception e) {
            return new DispatchResult(false, 400, rpcError(null, -32700, "Parse error: " + e.getMessage()));
        }
        if (root == null || !root.isObject()) {
            return new DispatchResult(false, 400, rpcError(null, -32600, "Invalid Request"));
        }
        Object id = root.has("id") && !root.get("id").isNull()
                ? (root.get("id").isNumber() ? root.get("id").numberValue() : root.get("id").asText())
                : null;
        String method = root.path("method").asText(null);
        if (method == null || method.isBlank()) {
            return new DispatchResult(false, 400, rpcError(id, -32600, "Invalid Request: missing method"));
        }
        if (method.startsWith("notifications/")) {
            return new DispatchResult(true, 202, null);
        }

        return switch (method) {
            case "initialize" -> new DispatchResult(false, 200, rpcSuccess(id, Map.of(
                    "protocolVersion", PROTOCOL_VERSION,
                    "capabilities", Map.of("tools", Map.of("listChanged", false)),
                    "serverInfo", Map.of("name", SERVER_NAME, "version", SERVER_VERSION)
            )));
            case "ping" -> new DispatchResult(false, 200, rpcSuccess(id, Map.of()));
            case "tools/list" -> new DispatchResult(false, 200, rpcSuccess(id, Map.of("tools", serializeToolsList())));
            case "tools/call" -> handleToolsCall(id, root.get("params"));
            default -> new DispatchResult(false, 200, rpcError(id, -32601, "Method not found: " + method));
        };
    }

    private DispatchResult handleToolsCall(Object id, JsonNode params) {
        if (params == null || !params.isObject() || params.path("name").asText("").isBlank()) {
            return new DispatchResult(false, 200, rpcError(id, -32602, "Invalid params: tool 'name' is required"));
        }
        Map<String, Object> args = params.has("arguments") && params.get("arguments").isObject()
                ? McpJsonMapper.toMap(params.get("arguments")) : Map.of();
        String reqId = id != null ? id.toString() : UUID.randomUUID().toString();
        McpToolResult toolResult = callTool(new McpToolCallRequest(reqId, params.get("name").asText(), args));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", List.of(Map.of("type", "text", "text", toolResult.contentJson() != null ? toolResult.contentJson() : "")));
        payload.put("isError", toolResult.isError());
        return new DispatchResult(false, 200, rpcSuccess(id, payload));
    }

    private List<Map<String, Object>> serializeToolsList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (McpTool tool : listTools()) {
            McpToolSchema schema = tool.getInputSchema();
            list.add(Map.of(
                    "name", tool.getName(),
                    "description", tool.getDescription(),
                    "inputSchema", Map.of("type", schema.type(), "properties", schema.properties(), "required", schema.required())
            ));
        }
        return list;
    }

    private Map<String, Object> rpcSuccess(Object id, Object result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("jsonrpc", "2.0");
        map.put("id", id);
        map.put("result", result);
        return map;
    }

    private Map<String, Object> rpcError(Object id, int code, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("jsonrpc", "2.0");
        map.put("id", id);
        map.put("error", Map.of("code", code, "message", message));
        return map;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = McpJsonMapper.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Accept, Mcp-Session-Id");
    }

    private String extractQueryParam(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && key.equals(parts[0])) return parts[1];
        }
        return null;
    }

    private record DispatchResult(boolean notificationOnly, int httpStatus, Map<String, Object> body) {
    }
}
