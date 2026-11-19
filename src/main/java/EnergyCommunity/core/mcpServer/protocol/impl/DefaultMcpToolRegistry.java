package EnergyCommunity.core.mcpServer.protocol.impl;

import EnergyCommunity.core.mcpServer.protocol.McpTool;
import EnergyCommunity.core.mcpServer.protocol.McpToolRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Thread-safe, insertion-ordered implementation of {@link McpToolRegistry}.
 */
public class DefaultMcpToolRegistry implements McpToolRegistry {

    private final Map<String, McpTool> tools = Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public void registerTool(McpTool tool) {
        Objects.requireNonNull(tool, "tool must not be null");
        if (tool.getName() == null || tool.getName().isBlank()) {
            throw new IllegalArgumentException("MCP tool name must not be blank");
        }
        tools.put(tool.getName(), tool);
    }

    @Override
    public boolean unregisterTool(String toolName) {
        return toolName != null && !toolName.isBlank() && tools.remove(toolName) != null;
    }

    @Override
    public Optional<McpTool> getTool(String toolName) {
        return (toolName == null || toolName.isBlank()) ? Optional.empty() : Optional.ofNullable(tools.get(toolName));
    }

    @Override
    public List<McpTool> getAllTools() {
        synchronized (tools) {
            return List.copyOf(tools.values());
        }
    }
}
