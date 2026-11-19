package EnergyCommunity.core.mcpServer.port.impl;

import EnergyCommunity.core.mcpServer.port.ProposalSubmissionPort;
import EnergyCommunity.core.mcpServer.tools.model.BatteryActionPlan;
import EnergyCommunity.core.mcpServer.tools.model.BatteryProposal;
import EnergyCommunity.core.mcpServer.tools.model.ProposalStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for AI-generated {@link BatteryProposal} records.
 */
public class InMemoryProposalStore implements ProposalSubmissionPort {

    private final Map<String, BatteryProposal> proposals = new ConcurrentHashMap<>();

    @Override
    public BatteryProposal submitProposal(String userId, BatteryActionPlan plan) {
        String id = "prop-" + UUID.randomUUID().toString().substring(0, 8);
        String houseId = (plan.houseId() != null && !plan.houseId().isBlank()) ? plan.houseId() : "house-" + userId;
        BatteryProposal created = new BatteryProposal(
                id, userId, houseId, plan.description(), plan.reasoning(), plan.actionType(),
                plan.batteryPercentage(), plan.executeAt(), plan.estimatedProfitEur(),
                ProposalStatus.PENDING, Instant.now()
        );
        proposals.put(id, created);
        return created;
    }

    public List<BatteryProposal> getAllProposals() {
        return proposals.values().stream()
                .sorted(Comparator.comparing(BatteryProposal::createdAt).reversed())
                .toList();
    }

    public List<BatteryProposal> getProposalsByUser(String userId) {
        return proposals.values().stream()
                .filter(p -> userId != null && userId.equals(p.userId()))
                .sorted(Comparator.comparing(BatteryProposal::createdAt).reversed())
                .toList();
    }

    public Optional<BatteryProposal> findById(String proposalId) {
        return proposalId == null ? Optional.empty() : Optional.ofNullable(proposals.get(proposalId));
    }

    public BatteryProposal acceptProposal(String proposalId) {
        return transitionStatus(proposalId, ProposalStatus.ACCEPTED);
    }

    public BatteryProposal rejectProposal(String proposalId) {
        return transitionStatus(proposalId, ProposalStatus.REJECTED);
    }

    private BatteryProposal transitionStatus(String proposalId, ProposalStatus nextStatus) {
        return proposals.compute(proposalId, (id, existing) -> {
            if (existing == null) {
                throw new NoSuchElementException("Proposal not found: " + proposalId);
            }
            return new BatteryProposal(
                    existing.id(), existing.userId(), existing.houseId(), existing.description(),
                    existing.reasoning(), existing.actionType(), existing.sellPercent(),
                    existing.executeAt(), existing.estimatedProfit(), nextStatus, existing.createdAt()
            );
        });
    }
}
