package EnergyCommunity.core.mcpServer.tools.impl;

import EnergyCommunity.core.mcpServer.port.ForecastQueryPort;
import EnergyCommunity.core.mcpServer.protocol.McpToolResult;
import EnergyCommunity.core.mcpServer.protocol.McpToolSchema;
import EnergyCommunity.core.mcpServer.tools.GetForecastsToolContract;
import EnergyCommunity.core.mcpServer.tools.model.ForecastReadResult;
import EnergyCommunity.core.mcpServer.tools.model.GeoLocationInput;
import EnergyCommunity.core.mcpServer.util.McpJsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of <b>MCP Tool 2 (Read)</b>: {@code get_forecasts(location)}.
 */
public class GetForecastsTool implements GetForecastsToolContract {

    private static final String DESCRIPTION =
            "Reads the next 24-hour solar radiation forecast (W/m^2) and dynamic grid electricity prices (EUR/kWh) for a location.";

    private final ForecastQueryPort forecastQueryPort;
    private final McpToolSchema inputSchema;

    public GetForecastsTool(ForecastQueryPort forecastQueryPort) {
        this.forecastQueryPort = Objects.requireNonNull(forecastQueryPort, "forecastQueryPort must not be null");
        Map<String, Map<String, Object>> props = new LinkedHashMap<>();
        props.put("latitude", Map.of("type", "number", "description", "Latitude in decimal degrees (-90 to 90)."));
        props.put("longitude", Map.of("type", "number", "description", "Longitude in decimal degrees (-180 to 180)."));
        props.put("region_name", Map.of("type", "string", "description", "Optional region or city name."));
        this.inputSchema = new McpToolSchema("object", props, List.of("latitude", "longitude"));
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
    public ForecastReadResult getForecasts(GeoLocationInput location) {
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
        if (Double.isNaN(location.latitude()) || location.latitude() < -90 || location.latitude() > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (Double.isNaN(location.longitude()) || location.longitude() < -180 || location.longitude() > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        ForecastReadResult result = forecastQueryPort.fetchForecastForLocation(location);
        if (result == null) {
            throw new IllegalStateException("No forecast available for location: " + location);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public McpToolResult execute(Map<String, Object> arguments) {
        String reqId = UUID.randomUUID().toString();
        if (arguments == null) {
            return McpToolResult.error(reqId, TOOL_NAME, "Missing required arguments: latitude, longitude");
        }
        try {
            Map<String, Object> src = arguments.get("location") instanceof Map<?, ?> nested
                    ? (Map<String, Object>) nested
                    : arguments;
            Double lat = parseDouble(src.get("latitude"));
            Double lon = parseDouble(src.get("longitude"));
            if (lat == null || lon == null) {
                return McpToolResult.error(reqId, TOOL_NAME, "Numeric latitude and longitude are required");
            }
            Object rawRegion = src.getOrDefault("region_name", src.get("regionName"));
            String region = rawRegion != null && !rawRegion.toString().isBlank() ? rawRegion.toString().trim() : null;

            ForecastReadResult forecast = getForecasts(new GeoLocationInput(lat, lon, region));
            return McpToolResult.success(reqId, TOOL_NAME, McpJsonMapper.toJson(forecast), forecast);
        } catch (Exception e) {
            return McpToolResult.error(reqId, TOOL_NAME, e.getMessage());
        }
    }

    private Double parseDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        try {
            return value == null ? null : Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
