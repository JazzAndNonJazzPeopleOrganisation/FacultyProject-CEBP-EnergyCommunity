package EnergyCommunity.core.mcpServer.port;

import EnergyCommunity.core.mcpServer.tools.model.BatteryProposal;

import java.util.List;
import java.util.Optional;

/**
 * Extended proposal storage and lifecycle contract used by operational endpoints and tests.
 * <p>
 * Kept separate from {@link ProposalSubmissionPort} (Interface Segregation Principle) so the
 * MCP AI tool can only submit new {@code PENDING} proposals and cannot self-approve or reject them.
 */
public interface ProposalStoreContract extends ProposalSubmissionPort {

    /**
     * Retrieves all stored battery proposals ordered from newest to oldest.
     *
     * @return List of all proposals
     */
    List<BatteryProposal> getAllProposals();

    /**
     * Retrieves all battery proposals belonging to a specific homeowner.
     *
     * @param userId Homeowner user identifier
     * @return List of proposals for the user
     */
    List<BatteryProposal> getProposalsByUser(String userId);

    /**
     * Looks up a battery proposal by its unique identifier.
     *
     * @param proposalId Proposal identifier
     * @return Optional containing the proposal if found
     */
    Optional<BatteryProposal> findById(String proposalId);

    /**
     * Transitions a {@code PENDING} proposal to {@code ACCEPTED}.
     *
     * @param proposalId Proposal identifier
     * @return Updated proposal with status {@code ACCEPTED}
     * @throws java.util.NoSuchElementException if the proposal does not exist
     * @throws IllegalStateException            if the proposal is not in {@code PENDING} state
     */
    BatteryProposal acceptProposal(String proposalId);

    /**
     * Transitions a {@code PENDING} proposal to {@code REJECTED}.
     *
     * @param proposalId Proposal identifier
     * @return Updated proposal with status {@code REJECTED}
     * @throws java.util.NoSuchElementException if the proposal does not exist
     * @throws IllegalStateException            if the proposal is not in {@code PENDING} state
     */
    BatteryProposal rejectProposal(String proposalId);
}
