package EnergyCommunity.core.mcpServer.port.impl;

import EnergyCommunity.core.mcpServer.port.ProposalStoreContract;
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
 * Thread-safe in-memory implementation of {@link ProposalStoreContract} for AI-generated {@link BatteryProposal} records.
 */
public class InMemoryProposalStore implements ProposalStoreContract {

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

    @Override
    public List<BatteryProposal> getAllProposals() {
        return proposals.values().stream()
                .sorted(Comparator.comparing(BatteryProposal::createdAt).reversed())
                .toList();
    }

    @Override
    public List<BatteryProposal> getProposalsByUser(String userId) {
        return proposals.values().stream()
                .filter(p -> userId != null && userId.equals(p.userId()))
                .sorted(Comparator.comparing(BatteryProposal::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<BatteryProposal> findById(String proposalId) {
        return proposalId == null ? Optional.empty() : Optional.ofNullable(proposals.get(proposalId));
    }

    @Override
    public BatteryProposal acceptProposal(String proposalId) {
        return transitionFromPending(proposalId, ProposalStatus.ACCEPTED);
    }

    @Override
    public BatteryProposal rejectProposal(String proposalId) {
        return transitionFromPending(proposalId, ProposalStatus.REJECTED);
    }

    private BatteryProposal transitionFromPending(String proposalId, ProposalStatus nextStatus) {
        return proposals.compute(proposalId, (id, existing) -> {
            if (existing == null) {
                throw new NoSuchElementException("Proposal not found: " + proposalId);
            }
            if (existing.status() != ProposalStatus.PENDING) {
                throw new IllegalStateException(
                        "Cannot transition proposal " + proposalId + " from " + existing.status() + " to " + nextStatus
                );
            }
            return new BatteryProposal(
                    existing.id(), existing.userId(), existing.houseId(), existing.description(),
                    existing.reasoning(), existing.actionType(), existing.sellPercent(),
                    existing.executeAt(), existing.estimatedProfit(), nextStatus, existing.createdAt()
            );
        });
    }
}
