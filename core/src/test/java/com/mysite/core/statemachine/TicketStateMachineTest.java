package com.mysite.core.statemachine;

import com.mysite.core.exception.InvalidTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TicketStateMachine} — proves AC-22–AC-35 transition rules.
 */
class TicketStateMachineTest {

    private TicketStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new TicketStateMachine();
    }

    // --- Valid transitions (individual) ---

    @Test
    @DisplayName("AC-22: Open -> In Progress is allowed")
    void shouldAllowOpenToInProgress() {
        assertValidTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("AC-23: In Progress -> Resolved is allowed")
    void shouldAllowInProgressToResolved() {
        assertValidTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);
    }

    @Test
    @DisplayName("AC-24: Resolved -> Closed is allowed")
    void shouldAllowResolvedToClosed() {
        assertValidTransition(TicketStatus.RESOLVED, TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("AC-25: Open -> Cancelled is allowed")
    void shouldAllowOpenToCancelled() {
        assertValidTransition(TicketStatus.OPEN, TicketStatus.CANCELLED);
    }

    @Test
    @DisplayName("AC-26: In Progress -> Cancelled is allowed")
    void shouldAllowInProgressToCancelled() {
        assertValidTransition(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);
    }

    @ParameterizedTest(name = "valid transition {0} -> {1}")
    @MethodSource("validTransitionPairs")
    @DisplayName("All valid transitions succeed")
    void shouldAllowAllValidTransitions(TicketStatus from, TicketStatus to) {
        assertValidTransition(from, to);
    }

    // --- Invalid transitions (individual) ---

    @Test
    @DisplayName("AC-27: Open -> Resolved is rejected")
    void shouldRejectOpenToResolved() {
        assertInvalidTransition(TicketStatus.OPEN, TicketStatus.RESOLVED);
    }

    @Test
    @DisplayName("AC-28: Open -> Closed is rejected")
    void shouldRejectOpenToClosed() {
        assertInvalidTransition(TicketStatus.OPEN, TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("AC-29: In Progress -> Closed is rejected")
    void shouldRejectInProgressToClosed() {
        assertInvalidTransition(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED);
    }

    @Test
    void shouldRejectInProgressToOpen() {
        assertInvalidTransition(TicketStatus.IN_PROGRESS, TicketStatus.OPEN);
    }

    @Test
    @DisplayName("AC-30: Resolved -> Open is rejected")
    void shouldRejectResolvedToOpen() {
        assertInvalidTransition(TicketStatus.RESOLVED, TicketStatus.OPEN);
    }

    @Test
    @DisplayName("AC-31: Resolved -> In Progress is rejected")
    void shouldRejectResolvedToInProgress() {
        assertInvalidTransition(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS);
    }

    @Test
    void shouldRejectResolvedToCancelled() {
        assertInvalidTransition(TicketStatus.RESOLVED, TicketStatus.CANCELLED);
    }

    @Test
    @DisplayName("AC-32: Closed -> Open is rejected (terminal source)")
    void shouldRejectClosedToOpen() {
        assertInvalidTransition(TicketStatus.CLOSED, TicketStatus.OPEN);
    }

    @Test
    @DisplayName("AC-33: Closed -> In Progress is rejected")
    void shouldRejectClosedToInProgress() {
        assertInvalidTransition(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("AC-34: Cancelled -> Open is rejected (terminal source)")
    void shouldRejectCancelledToOpen() {
        assertInvalidTransition(TicketStatus.CANCELLED, TicketStatus.OPEN);
    }

    @Test
    void shouldRejectCancelledToInProgress() {
        assertInvalidTransition(TicketStatus.CANCELLED, TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("AC-35: Cancelled -> Resolved is rejected")
    void shouldRejectCancelledToResolved() {
        assertInvalidTransition(TicketStatus.CANCELLED, TicketStatus.RESOLVED);
    }

    @ParameterizedTest(name = "invalid transition {0} -> {1}")
    @MethodSource("invalidTransitionPairs")
    @DisplayName("Comprehensive matrix of invalid transitions is rejected")
    void shouldRejectAllInvalidTransitions(TicketStatus from, TicketStatus to) {
        assertInvalidTransition(from, to);
    }

    // --- Same-state transitions ---

    @ParameterizedTest(name = "same-state transition {0} -> {0}")
    @EnumSource(TicketStatus.class)
    @DisplayName("Same-state transitions are rejected")
    void shouldRejectSameStateTransition(TicketStatus status) {
        assertInvalidTransition(status, status);
    }

    // --- allowedNextStatuses ---

    @Test
    void shouldReturnCorrectNextStatusesForOpen() {
        assertEquals(
                EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
                stateMachine.allowedNextStatuses(TicketStatus.OPEN));
    }

    @Test
    void shouldReturnCorrectNextStatusesForInProgress() {
        assertEquals(
                EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
                stateMachine.allowedNextStatuses(TicketStatus.IN_PROGRESS));
    }

    @Test
    void shouldReturnCorrectNextStatusesForResolved() {
        assertEquals(
                EnumSet.of(TicketStatus.CLOSED),
                stateMachine.allowedNextStatuses(TicketStatus.RESOLVED));
    }

    @Test
    void shouldReturnEmptyNextStatusesForClosed() {
        assertEquals(Set.of(), stateMachine.allowedNextStatuses(TicketStatus.CLOSED));
    }

    @Test
    void shouldReturnEmptyNextStatusesForCancelled() {
        assertEquals(Set.of(), stateMachine.allowedNextStatuses(TicketStatus.CANCELLED));
    }

    @Test
    void shouldReturnEmptyNextStatusesForNullStatus() {
        assertEquals(Set.of(), stateMachine.allowedNextStatuses((TicketStatus) null));
    }

    @Test
    void shouldReturnCorrectNextStatusesForOpenLabel() {
        assertEquals(
                EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
                stateMachine.allowedNextStatuses("Open"));
    }

    // --- Null status handling ---

    @Test
    void shouldRejectNullStatusInCanTransition() {
        assertFalse(stateMachine.canTransition((TicketStatus) null, TicketStatus.OPEN));
        assertFalse(stateMachine.canTransition(TicketStatus.OPEN, (TicketStatus) null));
        assertFalse(stateMachine.canTransition((TicketStatus) null, (TicketStatus) null));
    }

    @Test
    void shouldRejectNullStatusInAssertCanTransition() {
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.assertCanTransition((TicketStatus) null, TicketStatus.OPEN));
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.assertCanTransition(TicketStatus.OPEN, (TicketStatus) null));
    }

    // --- Exception content ---

    @Test
    void shouldIncludeFromAndToInException() {
        InvalidTransitionException ex = assertThrows(
                InvalidTransitionException.class,
                () -> stateMachine.assertCanTransition(TicketStatus.OPEN, TicketStatus.CLOSED));

        assertEquals(TicketStatus.OPEN, ex.getFrom());
        assertEquals(TicketStatus.CLOSED, ex.getTo());
        assertEquals("INVALID_TRANSITION", ex.errorCode());
        assertTrue(ex.getMessage().contains(TicketStatus.OPEN.getLabel()));
        assertTrue(ex.getMessage().contains(TicketStatus.CLOSED.getLabel()));
    }

    // --- Label handling ---

    @Test
    void shouldRejectUnknownStatusLabel() {
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.canTransition("Open", "NotARealStatus"));
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.assertCanTransition("Open", "NotARealStatus"));
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.allowedNextStatuses("Bogus"));
    }

    @Test
    void shouldMapLabelsToEnumCorrectly() {
        assertEquals(TicketStatus.IN_PROGRESS, TicketStatus.fromLabel("In Progress"));
    }

    @Test
    void shouldAllowValidTransitionViaLabels() {
        assertTrue(stateMachine.canTransition("Open", "In Progress"));
        assertDoesNotThrow(() -> stateMachine.assertCanTransition("Open", "In Progress"));
    }

    @Test
    void shouldRejectInvalidTransitionViaLabels() {
        InvalidTransitionException ex = assertThrows(
                InvalidTransitionException.class,
                () -> stateMachine.assertCanTransition("Open", "Closed"));

        assertEquals(TicketStatus.OPEN, ex.getFrom());
        assertEquals(TicketStatus.CLOSED, ex.getTo());
        assertEquals("INVALID_TRANSITION", ex.errorCode());
    }

    @Test
    void shouldValidateStatusLabels() {
        assertTrue(TicketStatus.isValidLabel("Open"));
        assertTrue(TicketStatus.isValidLabel("In Progress"));
        assertFalse(TicketStatus.isValidLabel(null));
        assertFalse(TicketStatus.isValidLabel(""));
        assertFalse(TicketStatus.isValidLabel("NotARealStatus"));
    }

    // --- Method sources ---

    static Stream<Arguments> validTransitionPairs() {
        return Stream.of(
                Arguments.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.OPEN, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED));
    }

    static Stream<Arguments> invalidTransitionPairs() {
        Set<String> validKeys = Set.of(
                key(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                key(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
                key(TicketStatus.RESOLVED, TicketStatus.CLOSED),
                key(TicketStatus.OPEN, TicketStatus.CANCELLED),
                key(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED));

        Stream.Builder<Arguments> builder = Stream.builder();
        for (TicketStatus from : TicketStatus.values()) {
            for (TicketStatus to : TicketStatus.values()) {
                if (!validKeys.contains(key(from, to))) {
                    builder.add(Arguments.of(from, to));
                }
            }
        }
        return builder.build();
    }

    private static String key(TicketStatus from, TicketStatus to) {
        return from.name() + "->" + to.name();
    }

    private void assertValidTransition(TicketStatus from, TicketStatus to) {
        assertTrue(stateMachine.canTransition(from, to),
                () -> "Expected canTransition true for " + from + " -> " + to);
        assertDoesNotThrow(() -> stateMachine.assertCanTransition(from, to),
                () -> "Expected assertCanTransition to succeed for " + from + " -> " + to);
    }

    private void assertInvalidTransition(TicketStatus from, TicketStatus to) {
        assertFalse(stateMachine.canTransition(from, to),
                () -> "Expected canTransition false for " + from + " -> " + to);
        assertThrows(InvalidTransitionException.class,
                () -> stateMachine.assertCanTransition(from, to),
                () -> "Expected InvalidTransitionException for " + from + " -> " + to);
    }
}
