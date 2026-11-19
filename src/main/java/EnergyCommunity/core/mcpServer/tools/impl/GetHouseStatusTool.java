package EnergyCommunity.core.mcpServer.tools.impl;

import EnergyCommunity.core.mcpServer.port.HouseStatusQueryPort;
import EnergyCommunity.core.mcpServer.protocol.McpToolResult;
import EnergyCommunity.core.mcpServer.protocol.McpToolSchema;
import EnergyCommunity.core.mcpServer.tools.GetHouseStatusToolContract;
import EnergyCommunity.core.mcpServer.tools.model.HouseStatusReadResult;
import EnergyCommunity.core.mcpServer.util.McpJsonMapper;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of <b>MCP Tool 1 (Read)</b>: {@code get_house_status(user_id)}.
 */
public class GetHouseStatusTool implements GetHouseStatusToolContract {

    private static final String DESCRIPTION =
            "Reads live house telemetry for a homeowner: battery level (%), capacity (kWh), "
                    + "power consumption (kW), solar output (kW), coordinates, and EV proximity status.";

    private final HouseStatusQueryPort houseStatusQueryPort;
    private final McpToolSchema inputSchema;

    public GetHouseStatusTool(HouseStatusQueryPort houseStatusQueryPort) {
        this.houseStatusQueryPort = Objects.requireNonNull(houseStatusQueryPort, "houseStatusQueryPort must not be null");
        this.inputSchema = new McpToolSchema(
                "object",
                Map.of("user_id", Map.of("type", "string", "description", "Homeowner user identifier.")),
                List.of("user_id")
        );
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public McpToolSchema getInputSchema() {
        return inputSchema;
    }

    @Override
    public HouseStatusReadResult getHouseStatus(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user_id must not be blank");
        }
        return houseStatusQueryPort.fetchHouseStatusByUserId(userId.trim())
                .orElseThrow(() -> new NoSuchElementException("No house status found for user_id: " + userId));
    }

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        String reqId = UUID.randomUUID().toString();
        Object rawUserId = arguments == null ? null : arguments.getOrDefault("user_id", arguments.get("userId"));
        if (rawUserId == null || rawUserId.toString().isBlank()) {
            return McpToolResult.error(reqId, TOOL_NAME, "Missing required argument: user_id");
        }
        try {
            HouseStatusReadResult status = getHouseStatus(rawUserId.toString());
            return McpToolResult.success(reqId, TOOL_NAME, McpJsonMapper.toJson(status), status);
        } catch (Exception e) {
            return McpToolResult.error(reqId, TOOL_NAME, e.getMessage());
        }
    }
}
