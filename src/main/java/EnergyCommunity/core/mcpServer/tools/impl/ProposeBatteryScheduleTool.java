package EnergyCommunity.core.mcpServer.tools.impl;

import EnergyCommunity.core.mcpServer.port.ProposalSubmissionPort;
import EnergyCommunity.core.mcpServer.protocol.McpToolResult;
import EnergyCommunity.core.mcpServer.protocol.McpToolSchema;
import EnergyCommunity.core.mcpServer.tools.ProposeBatteryScheduleToolContract;
import EnergyCommunity.core.mcpServer.tools.model.BatteryActionPlan;
import EnergyCommunity.core.mcpServer.tools.model.BatteryActionType;
import EnergyCommunity.core.mcpServer.tools.model.BatteryProposal;
import EnergyCommunity.core.mcpServer.tools.model.ProposalSubmissionResult;
import EnergyCommunity.core.mcpServer.util.McpJsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of <b>MCP Tool 3 (Propose Change)</b>: {@code propose_battery_schedule(user_id, action_plan)}.
 */
public class ProposeBatteryScheduleTool implements ProposeBatteryScheduleToolContract {

    private static final String DESCRIPTION =
            "Submits a PENDING next-day battery schedule proposal for user approval in the mobile app "
                    + "(SELL_TO_GRID, CHARGE_FROM_SOLAR, CHARGE_FROM_GRID, HOLD_FOR_EV, DISCHARGE_TO_HOME).";

    private final ProposalSubmissionPort proposalSubmissionPort;
    private final McpToolSchema inputSchema;

    public ProposeBatteryScheduleTool(ProposalSubmissionPort proposalSubmissionPort) {
        this.proposalSubmissionPort = Objects.requireNonNull(proposalSubmissionPort, "proposalSubmissionPort must not be null");

        Map<String, Object> planProps = new LinkedHashMap<>();
        planProps.put("house_id", Map.of("type", "string", "description", "Target house identifier."));
        planProps.put("action_type", Map.of("type", "string", "enum", List.of(
                "SELL_TO_GRID", "CHARGE_FROM_SOLAR", "CHARGE_FROM_GRID", "HOLD_FOR_EV", "DISCHARGE_TO_HOME")));
        planProps.put("battery_percentage", Map.of("type", "integer", "description", "Battery capacity percentage (0-100)."));
        planProps.put("execute_at", Map.of("type", "string", "description", "ISO-8601 UTC execution timestamp."));
        planProps.put("estimated_profit_eur", Map.of("type", "number", "description", "Estimated profit or savings in EUR."));
        planProps.put("description", Map.of("type", "string", "description", "Human-readable proposal summary."));
        planProps.put("reasoning", Map.of("type", "string", "description", "Weather and tariff correlation explanation."));

        Map<String, Object> actionPlanSchema = Map.of(
                "type", "object",
                "properties", planProps,
                "required", List.of("action_type", "battery_percentage", "execute_at", "description")
        );
        Map<String, Map<String, Object>> props = new LinkedHashMap<>();
        props.put("user_id", Map.of("type", "string", "description", "Target homeowner user ID."));
        props.put("action_plan", actionPlanSchema);
        this.inputSchema = new McpToolSchema("object", props, List.of("user_id", "action_plan"));
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
    public ProposalSubmissionResult proposeBatterySchedule(String userId, BatteryActionPlan plan) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("user_id must not be blank");
        if (plan == null || plan.actionType() == null || plan.executeAt() == null) {
            throw new IllegalArgumentException("action_plan, action_type, and execute_at are required");
        }
        if (plan.batteryPercentage() < 0 || plan.batteryPercentage() > 100) {
            throw new IllegalArgumentException("battery_percentage must be between 0 and 100");
        }
        if (plan.description() == null || plan.description().isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        BatteryProposal created = proposalSubmissionPort.submitProposal(userId.trim(), plan);
        String msg = String.format("Pending proposal %s created for user %s (%s %d%% at %s).",
                created.id(), created.userId(), created.actionType(), created.sellPercent(), created.executeAt());
        return new ProposalSubmissionResult(true, created, msg);
    }

    @Override
    @SuppressWarnings("unchecked")
    public McpToolResult execute(Map<String, Object> arguments) {
        String reqId = UUID.randomUUID().toString();
        if (arguments == null) {
            return McpToolResult.error(reqId, TOOL_NAME, "Missing required arguments: user_id, action_plan");
        }
        try {
            String userId = str(arguments, "user_id", "userId");
            Object rawPlan = arguments.getOrDefault("action_plan", arguments.get("actionPlan"));
            Map<String, Object> planMap;
            if (rawPlan instanceof Map<?, ?> m) {
                planMap = (Map<String, Object>) m;
            } else if (rawPlan instanceof String s && s.trim().startsWith("{")) {
                planMap = McpJsonMapper.toMap(McpJsonMapper.parseTree(s));
            } else if (arguments.containsKey("action_type") || arguments.containsKey("actionType")) {
                planMap = arguments;
            } else {
                throw new IllegalArgumentException("Missing required object argument: action_plan");
            }

            ProposalSubmissionResult result = proposeBatterySchedule(userId, parseActionPlan(planMap));
            return McpToolResult.success(reqId, TOOL_NAME, McpJsonMapper.toJson(result), result);
        } catch (Exception e) {
            return McpToolResult.error(reqId, TOOL_NAME, e.getMessage());
        }
    }

    private BatteryActionPlan parseActionPlan(Map<String, Object> map) {
        String rawType = str(map, "action_type", "actionType");
        if (rawType == null) throw new IllegalArgumentException("action_type is required");
        BatteryActionType type = BatteryActionType.valueOf(rawType.toUpperCase(Locale.ROOT));

        Double pct = num(map, "battery_percentage", "batteryPercentage", "sell_percent", "sellPercent");
        if (pct == null) throw new IllegalArgumentException("battery_percentage is required");

        String rawTime = str(map, "execute_at", "executeAt");
        if (rawTime == null) throw new IllegalArgumentException("execute_at is required");

        Double profit = num(map, "estimated_profit_eur", "estimatedProfitEur", "estimated_profit", "estimatedProfit");
        String description = str(map, "description");
        String reasoning = str(map, "reasoning");

        return new BatteryActionPlan(
                str(map, "house_id", "houseId"),
                type,
                (int) Math.round(pct),
                parseInstant(rawTime),
                profit != null ? profit : 0.0,
                description,
                reasoning != null ? reasoning : description
        );
    }

    private Instant parseInstant(String raw) {
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e2) {
                LocalTime time = LocalTime.parse(raw);
                Instant candidate = LocalDateTime.of(LocalDate.now(ZoneOffset.UTC), time).toInstant(ZoneOffset.UTC);
                return candidate.isAfter(Instant.now()) ? candidate : candidate.plusSeconds(86400);
            }
        }
    }

    private String str(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null && !v.toString().isBlank()) return v.toString().trim();
        }
        return null;
    }

    private Double num(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v instanceof Number n) return n.doubleValue();
            if (v != null) {
                try {
                    return Double.parseDouble(v.toString().trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}
