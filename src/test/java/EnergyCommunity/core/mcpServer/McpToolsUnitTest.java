package EnergyCommunity.core.mcpServer;

import EnergyCommunity.core.mcpServer.port.ForecastQueryPort;
import EnergyCommunity.core.mcpServer.port.HouseStatusQueryPort;
import EnergyCommunity.core.mcpServer.port.ProposalStoreContract;
import EnergyCommunity.core.mcpServer.port.impl.InMemoryProposalStore;
import EnergyCommunity.core.mcpServer.protocol.McpToolResult;
import EnergyCommunity.core.mcpServer.protocol.impl.DefaultMcpToolRegistry;
import EnergyCommunity.core.mcpServer.tools.impl.GetForecastsTool;
import EnergyCommunity.core.mcpServer.tools.impl.GetHouseStatusTool;
import EnergyCommunity.core.mcpServer.tools.impl.ProposeBatteryScheduleTool;
import EnergyCommunity.core.mcpServer.tools.model.BatteryActionType;
import EnergyCommunity.core.mcpServer.tools.model.ForecastPointData;
import EnergyCommunity.core.mcpServer.tools.model.ForecastReadResult;
import EnergyCommunity.core.mcpServer.tools.model.GeoLocationInput;
import EnergyCommunity.core.mcpServer.tools.model.HouseStatusReadResult;
import EnergyCommunity.core.mcpServer.tools.model.ProposalStatus;
import EnergyCommunity.core.mcpServer.tools.model.ProposalSubmissionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class McpToolsUnitTest {

    private ProposalStoreContract proposalStore;
    private GetHouseStatusTool getHouseStatusTool;
    private GetForecastsTool getForecastsTool;
    private ProposeBatteryScheduleTool proposeBatteryScheduleTool;

    @BeforeEach
    void setUp() {
        HouseStatusQueryPort housePort = new HouseStatusQueryPort() {
            private final HouseStatusReadResult sample = new HouseStatusReadResult(
                    "1", "101", new GeoLocationInput(45.7489, 21.2087, "Timisoara-Central"),
                    82, 15.0, 1.4, 3.8, false, Instant.parse("2026-11-19T12:00:00Z")
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

        ForecastQueryPort forecastPort = location -> new ForecastReadResult(
                location,
                Instant.parse("2026-11-19T12:00:00Z"),
                24,
                List.of(
                        new ForecastPointData(Instant.parse("2026-11-20T13:00:00Z"), 780.0, 0.16),
                        new ForecastPointData(Instant.parse("2026-11-20T19:00:00Z"), 0.0, 0.46)
                )
        );

        proposalStore = new InMemoryProposalStore();
        getHouseStatusTool = new GetHouseStatusTool(housePort);
        getForecastsTool = new GetForecastsTool(forecastPort);
        proposeBatteryScheduleTool = new ProposeBatteryScheduleTool(proposalStore);
    }

    @Test
    void registryRegistersLooksUpAndUnregistersAllThreeTools() {
        DefaultMcpToolRegistry registry = new DefaultMcpToolRegistry();
        registry.registerTool(getHouseStatusTool);
        registry.registerTool(getForecastsTool);
        registry.registerTool(proposeBatteryScheduleTool);

        assertEquals(3, registry.getAllTools().size());
        assertTrue(registry.getTool("get_house_status").isPresent());
        assertTrue(registry.getTool("get_forecasts").isPresent());
        assertTrue(registry.getTool("propose_battery_schedule").isPresent());
        assertTrue(registry.unregisterTool("get_forecasts"));
        assertEquals(2, registry.getAllTools().size());
    }

    @Test
    void getHouseStatusReturnsLiveSnapshotAndHandlesMissingUser() {
        McpToolResult ok = getHouseStatusTool.execute(Map.of("user_id", "1"));
        assertFalse(ok.isError());
        HouseStatusReadResult status = (HouseStatusReadResult) ok.structuredPayload();
        assertEquals("101", status.houseId());
        assertEquals(82, status.batteryLevelPercent());

        assertTrue(getHouseStatusTool.execute(Map.of()).isError());
        assertTrue(getHouseStatusTool.execute(Map.of("user_id", "999")).isError());
    }

    @Test
    void getForecastsSupportsFlatAndNestedLocationAndValidatesBounds() {
        McpToolResult flat = getForecastsTool.execute(Map.of(
                "latitude", 45.7489, "longitude", 21.2087, "region_name", "Timisoara-Central"
        ));
        assertFalse(flat.isError());
        assertEquals(2, ((ForecastReadResult) flat.structuredPayload()).points().size());

        McpToolResult nested = getForecastsTool.execute(Map.of(
                "location", Map.of("latitude", 45.75, "longitude", 21.21, "regionName", "Timisoara-West")
        ));
        assertFalse(nested.isError());
        assertEquals("Timisoara-West", ((ForecastReadResult) nested.structuredPayload()).location().regionName());

        assertTrue(getForecastsTool.execute(Map.of("latitude", 120.0, "longitude", 21.2)).isError());
    }

    @Test
    void proposeBatteryScheduleCreatesPendingProposalAndEnforcesValidStatusTransitions() {
        Map<String, Object> planArgs = Map.of(
                "user_id", "1",
                "action_plan", Map.of(
                        "house_id", "101",
                        "action_type", "SELL_TO_GRID",
                        "battery_percentage", 40,
                        "execute_at", "2026-11-20T19:00:00Z",
                        "estimated_profit_eur", 2.76,
                        "description", "Sell 40% of battery at 19:00 UTC",
                        "reasoning", "Evening tariff peak reaches 0.46 EUR/kWh"
                )
        );

        McpToolResult first = proposeBatteryScheduleTool.execute(planArgs);
        assertFalse(first.isError());
        ProposalSubmissionResult sub1 = (ProposalSubmissionResult) first.structuredPayload();
        assertTrue(sub1.acceptedBySystem());
        assertEquals(ProposalStatus.PENDING, sub1.proposal().status());
        assertEquals(BatteryActionType.SELL_TO_GRID, sub1.proposal().actionType());

        String id1 = sub1.proposal().id();
        assertEquals(ProposalStatus.ACCEPTED, proposalStore.acceptProposal(id1).status());
        assertThrows(IllegalStateException.class, () -> proposalStore.rejectProposal(id1));
        assertThrows(IllegalStateException.class, () -> proposalStore.acceptProposal(id1));

        McpToolResult second = proposeBatteryScheduleTool.execute(planArgs);
        String id2 = ((ProposalSubmissionResult) second.structuredPayload()).proposal().id();
        assertEquals(ProposalStatus.REJECTED, proposalStore.rejectProposal(id2).status());
        assertThrows(IllegalStateException.class, () -> proposalStore.acceptProposal(id2));
        assertEquals(2, proposalStore.getProposalsByUser("1").size());
    }

    @Test
    void proposeBatteryScheduleRejectsInvalidInputs() {
        assertTrue(proposeBatteryScheduleTool.execute(Map.of(
                "user_id", "1",
                "action_plan", Map.of("action_type", "SELL_TO_GRID", "battery_percentage", 150,
                        "execute_at", "2026-11-20T19:00:00Z", "description", "Out of range")
        )).isError());

        assertTrue(proposeBatteryScheduleTool.execute(Map.of(
                "user_id", "1",
                "action_plan", Map.of("action_type", "INVALID_ACTION", "battery_percentage", 25,
                        "execute_at", "2026-11-20T19:00:00Z", "description", "Bad action")
        )).isError());
    }
}
