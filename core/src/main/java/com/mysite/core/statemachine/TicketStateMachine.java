package com.mysite.core.statemachine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.mysite.core.exception.InvalidTransitionException;

/**
 * Enforces the ticket lifecycle state machine. All status transition rules live here only.
 * <p>
 * <strong>Allowed transitions</strong> (no other pairings are permitted):
 * </p>
 * <table border="1">
 *   <caption>Transition table</caption>
 *   <tr><th>From</th><th>To</th></tr>
 *   <tr><td>Open</td><td>In Progress</td></tr>
 *   <tr><td>In Progress</td><td>Resolved</td></tr>
 *   <tr><td>Resolved</td><td>Closed</td></tr>
 *   <tr><td>Open</td><td>Cancelled</td></tr>
 *   <tr><td>In Progress</td><td>Cancelled</td></tr>
 * </table>
 * <p>
 * {@link TicketStatus#CLOSED} and {@link TicketStatus#CANCELLED} are terminal — no outgoing
 * transitions. Initial status on ticket creation is {@link TicketStatus#OPEN} (enforced by
 * {@code TicketService}, not this class).
 * </p>
 */
public class TicketStateMachine {

    /**
     * Immutable transition table: each source status maps to the set of allowed target statuses.
     */
    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = buildTransitionTable();

    private static Map<TicketStatus, Set<TicketStatus>> buildTransitionTable() {
        Map<TicketStatus, Set<TicketStatus>> table = new EnumMap<>(TicketStatus.class);

        table.put(TicketStatus.OPEN,
                Collections.unmodifiableSet(EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED)));
        table.put(TicketStatus.IN_PROGRESS,
                Collections.unmodifiableSet(EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED)));
        table.put(TicketStatus.RESOLVED,
                Collections.unmodifiableSet(EnumSet.of(TicketStatus.CLOSED)));
        table.put(TicketStatus.CLOSED,
                Collections.unmodifiableSet(EnumSet.noneOf(TicketStatus.class)));
        table.put(TicketStatus.CANCELLED,
                Collections.unmodifiableSet(EnumSet.noneOf(TicketStatus.class)));

        return Collections.unmodifiableMap(table);
    }

    /**
     * @param from current status
     * @param to   proposed target status
     * @return {@code true} if the transition is allowed by the state machine
     */
    public boolean canTransition(TicketStatus from, TicketStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.get(from).contains(to);
    }

    /**
     * Validates a transition and throws if it is not allowed.
     *
     * @param from current status
     * @param to   proposed target status
     * @throws IllegalArgumentException if {@code from} or {@code to} is null
     * @throws InvalidTransitionException when the transition is not permitted
     */
    public void assertCanTransition(TicketStatus from, TicketStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        if (!canTransition(from, to)) {
            throw new InvalidTransitionException(from, to);
        }
    }

    /**
     * Returns the valid next statuses for the given current status.
     * Terminal states ({@link TicketStatus#CLOSED}, {@link TicketStatus#CANCELLED}) return an
     * empty set.
     *
     * @param from current status
     * @return unmodifiable set of allowed target statuses (never {@code null})
     */
    public Set<TicketStatus> allowedNextStatuses(TicketStatus from) {
        if (from == null) {
            return Set.of();
        }
        return TRANSITIONS.get(from);
    }

    /**
     * Label-based overload of {@link #canTransition(TicketStatus, TicketStatus)}.
     *
     * @throws IllegalArgumentException if either label is unknown
     */
    public boolean canTransition(String fromLabel, String toLabel) {
        return canTransition(TicketStatus.fromLabel(fromLabel), TicketStatus.fromLabel(toLabel));
    }

    /**
     * Label-based overload of {@link #assertCanTransition(TicketStatus, TicketStatus)}.
     *
     * @throws IllegalArgumentException if either label is unknown
     * @throws InvalidTransitionException when the transition is not permitted
     */
    public void assertCanTransition(String fromLabel, String toLabel) {
        assertCanTransition(TicketStatus.fromLabel(fromLabel), TicketStatus.fromLabel(toLabel));
    }

    /**
     * Label-based overload of {@link #allowedNextStatuses(TicketStatus)}.
     *
     * @throws IllegalArgumentException if {@code fromLabel} is unknown
     */
    public Set<TicketStatus> allowedNextStatuses(String fromLabel) {
        return allowedNextStatuses(TicketStatus.fromLabel(fromLabel));
    }
}
