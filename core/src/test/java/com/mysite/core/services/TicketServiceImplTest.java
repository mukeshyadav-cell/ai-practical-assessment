package com.mysite.core.services;

import com.mysite.core.dto.TicketDTO;
import com.mysite.core.dto.UserDTO;
import com.mysite.core.exception.InvalidTransitionException;
import com.mysite.core.exception.TicketNotEditableException;
import com.mysite.core.exception.TicketNotFoundException;
import com.mysite.core.exception.UnknownUserException;
import com.mysite.core.exception.ValidationException;
import com.mysite.core.repositories.TicketRepository;
import com.mysite.core.repositories.UserRepository;
import com.mysite.core.services.impl.TicketServiceImpl;
import com.mysite.core.statemachine.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TicketServiceImpl} — business-logic orchestration with mocked
 * repositories and the real {@link com.mysite.core.statemachine.TicketStateMachine}.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final String TICKET_ID = "TKT-1001";
    private static final String AGENT_ID = "agent-1";

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private UserDTO seededAgent;

    @BeforeEach
    void setUp() {
        seededAgent = new UserDTO(AGENT_ID, "Agent One", "agent1@example.com");
    }

    // --- createTicket ---

    @Test
    void shouldForceStatusOpenOnCreate() {
        TicketDTO input = validCreateInput();
        input.setStatus(TicketStatus.RESOLVED.getLabel());

        when(ticketRepository.create(any(TicketDTO.class))).thenAnswer(invocation -> {
            TicketDTO persisted = invocation.getArgument(0);
            persisted.setId(TICKET_ID);
            return persisted;
        });

        ticketService.createTicket(input);

        ArgumentCaptor<TicketDTO> captor = ArgumentCaptor.forClass(TicketDTO.class);
        verify(ticketRepository).create(captor.capture());
        assertEquals(TicketStatus.OPEN.getLabel(), captor.getValue().getStatus());
    }

    @Test
    void shouldRejectBlankTitle() {
        TicketDTO nullTitle = validCreateInput();
        nullTitle.setTitle(null);
        assertThrows(ValidationException.class, () -> ticketService.createTicket(nullTitle));

        TicketDTO blankTitle = validCreateInput();
        blankTitle.setTitle("   ");
        ValidationException ex = assertThrows(ValidationException.class,
                () -> ticketService.createTicket(blankTitle));
        assertEquals("title", ex.getField());
        assertEquals("VALIDATION_ERROR", ex.errorCode());

        verifyNoInteractions(ticketRepository);
    }

    @Test
    void shouldRejectInvalidPriority() {
        TicketDTO input = validCreateInput();
        input.setPriority("High");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> ticketService.createTicket(input));
        assertEquals("priority", ex.getField());
        assertEquals("VALIDATION_ERROR", ex.errorCode());
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void shouldRejectUnknownAssigneeOnCreate() {
        TicketDTO input = validCreateInput();
        input.setAssignedTo("nobody");
        when(userRepository.getById("nobody")).thenReturn(Optional.empty());

        UnknownUserException ex = assertThrows(UnknownUserException.class,
                () -> ticketService.createTicket(input));
        assertEquals("nobody", ex.getUserId());
        assertEquals("UNKNOWN_USER", ex.errorCode());
        verify(ticketRepository, never()).create(any());
    }

    @Test
    void shouldAllowBlankAssignee() {
        TicketDTO input = validCreateInput();
        input.setAssignedTo("  ");

        when(ticketRepository.create(any(TicketDTO.class))).thenAnswer(invocation -> {
            TicketDTO persisted = invocation.getArgument(0);
            persisted.setId(TICKET_ID);
            return persisted;
        });

        ticketService.createTicket(input);

        verifyNoInteractions(userRepository);
        ArgumentCaptor<TicketDTO> captor = ArgumentCaptor.forClass(TicketDTO.class);
        verify(ticketRepository).create(captor.capture());
        assertEquals(null, captor.getValue().getAssignedTo());
    }

    @Test
    void shouldReturnCreatedTicket() {
        TicketDTO input = validCreateInput();
        input.setAssignedTo(AGENT_ID);
        when(userRepository.getById(AGENT_ID)).thenReturn(Optional.of(seededAgent));
        when(ticketRepository.create(any(TicketDTO.class))).thenAnswer(invocation -> {
            TicketDTO persisted = invocation.getArgument(0);
            persisted.setId(TICKET_ID);
            persisted.setCreatedAt(Instant.parse("2025-06-01T12:00:00Z"));
            return persisted;
        });

        TicketDTO created = ticketService.createTicket(input);

        assertEquals(TICKET_ID, created.getId());
        verify(ticketRepository).create(any(TicketDTO.class));
    }

    // --- getTicket ---

    @Test
    void shouldReturnTicketWhenFound() {
        TicketDTO existing = existingOpenTicket();
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(existing));

        TicketDTO result = ticketService.getTicket(TICKET_ID);

        assertSame(existing, result);
        verify(ticketRepository).getById(TICKET_ID);
    }

    @Test
    void shouldThrowNotFoundWhenMissing() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.empty());

        TicketNotFoundException ex = assertThrows(TicketNotFoundException.class,
                () -> ticketService.getTicket(TICKET_ID));
        assertEquals(TICKET_ID, ex.getTicketId());
        assertEquals("NOT_FOUND", ex.errorCode());
    }

    // --- updateTicket ---

    @Test
    void shouldUpdateMutableFields() {
        TicketDTO existing = existingOpenTicket();
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(existing));
        when(ticketRepository.update(any(TicketDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketDTO changes = new TicketDTO();
        changes.setTitle("Updated title");
        changes.setDescription("Updated description");
        changes.setPriority("P1");

        TicketDTO updated = ticketService.updateTicket(TICKET_ID, changes);

        assertEquals("Updated title", updated.getTitle());
        assertEquals("Updated description", updated.getDescription());
        assertEquals("P1", updated.getPriority());
        verify(ticketRepository).update(existing);
    }

    @Test
    void shouldThrowNotEditableWhenTerminal() {
        TicketDTO closed = existingOpenTicket();
        closed.setStatus(TicketStatus.CLOSED.getLabel());
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(closed));

        TicketDTO changes = new TicketDTO();
        changes.setTitle("New title");

        TicketNotEditableException ex = assertThrows(TicketNotEditableException.class,
                () -> ticketService.updateTicket(TICKET_ID, changes));
        assertEquals(TICKET_ID, ex.getTicketId());
        assertEquals(TicketStatus.CLOSED.getLabel(), ex.getStatus());
        assertEquals("TICKET_NOT_EDITABLE", ex.errorCode());
        verify(ticketRepository, never()).update(any());
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissing() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.empty());

        TicketDTO changes = new TicketDTO();
        changes.setTitle("New title");

        assertThrows(TicketNotFoundException.class,
                () -> ticketService.updateTicket(TICKET_ID, changes));
        verify(ticketRepository, never()).update(any());
    }

    @Test
    void shouldNotChangeStatusOrAssigneeViaUpdate() {
        TicketDTO existing = existingOpenTicket();
        existing.setAssignedTo(AGENT_ID);
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(existing));
        when(ticketRepository.update(any(TicketDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketDTO changes = new TicketDTO();
        changes.setTitle("Only title change");

        ticketService.updateTicket(TICKET_ID, changes);

        ArgumentCaptor<TicketDTO> captor = ArgumentCaptor.forClass(TicketDTO.class);
        verify(ticketRepository).update(captor.capture());
        assertEquals(TicketStatus.OPEN.getLabel(), captor.getValue().getStatus());
        assertEquals(AGENT_ID, captor.getValue().getAssignedTo());
    }

    // --- reassignTicket ---

    @Test
    void shouldReassignToValidUser() {
        TicketDTO existing = existingOpenTicket();
        existing.setAssignedTo("agent-2");
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(existing));
        when(userRepository.getById(AGENT_ID)).thenReturn(Optional.of(seededAgent));
        when(ticketRepository.update(any(TicketDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketDTO updated = ticketService.reassignTicket(TICKET_ID, AGENT_ID);

        assertEquals(AGENT_ID, updated.getAssignedTo());
        verify(ticketRepository).update(existing);
        verify(userRepository).getById(AGENT_ID);
    }

    @Test
    void shouldRejectUnknownUserOnReassign() {
        TicketDTO existing = existingOpenTicket();
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(existing));
        when(userRepository.getById("nobody")).thenReturn(Optional.empty());

        UnknownUserException ex = assertThrows(UnknownUserException.class,
                () -> ticketService.reassignTicket(TICKET_ID, "nobody"));
        assertEquals("nobody", ex.getUserId());
        verify(ticketRepository, never()).update(any());
    }

    @Test
    void shouldRejectReassignOnTerminalTicket() {
        TicketDTO cancelled = existingOpenTicket();
        cancelled.setStatus(TicketStatus.CANCELLED.getLabel());
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(cancelled));

        assertThrows(TicketNotEditableException.class,
                () -> ticketService.reassignTicket(TICKET_ID, AGENT_ID));
        verify(userRepository, never()).getById(anyString());
        verify(ticketRepository, never()).update(any());
    }

    // --- changeStatus ---

    @Test
    void shouldAllowValidTransition() {
        TicketDTO existing = existingOpenTicket();
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(existing));
        when(ticketRepository.update(any(TicketDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketDTO updated = ticketService.changeStatus(TICKET_ID, TicketStatus.IN_PROGRESS.getLabel());

        assertEquals(TicketStatus.IN_PROGRESS.getLabel(), updated.getStatus());
        verify(ticketRepository).update(existing);
    }

    @Test
    void shouldRejectInvalidTransition() {
        TicketDTO existing = existingOpenTicket();
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(existing));

        InvalidTransitionException ex = assertThrows(InvalidTransitionException.class,
                () -> ticketService.changeStatus(TICKET_ID, TicketStatus.CLOSED.getLabel()));
        assertEquals(TicketStatus.OPEN, ex.getFrom());
        assertEquals(TicketStatus.CLOSED, ex.getTo());
        assertEquals("INVALID_TRANSITION", ex.errorCode());
        verify(ticketRepository, never()).update(any());
    }

    @Test
    void shouldRejectUnknownStatusLabel() {
        TicketDTO existing = existingOpenTicket();
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(existing));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> ticketService.changeStatus(TICKET_ID, "Bogus"));
        assertEquals("status", ex.getField());
        verify(ticketRepository, never()).update(any());
    }

    @Test
    void shouldThrowNotFoundWhenChangingStatusOfMissingTicket() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
                () -> ticketService.changeStatus(TICKET_ID, TicketStatus.IN_PROGRESS.getLabel()));
        verify(ticketRepository, never()).update(any());
    }

    // --- listTickets ---

    @Test
    void shouldSearchWhenQueryProvided() {
        TicketDTO match = existingOpenTicket();
        when(ticketRepository.searchByTitle("network")).thenReturn(List.of(match));

        List<TicketDTO> results = ticketService.listTickets(null, "network");

        assertEquals(1, results.size());
        verify(ticketRepository).searchByTitle("network");
        verify(ticketRepository, never()).getAll();
        verify(ticketRepository, never()).findByStatus(anyString());
    }

    @Test
    void shouldFilterByStatusWhenOnlyStatusProvided() {
        TicketDTO open = existingOpenTicket();
        when(ticketRepository.findByStatus(TicketStatus.OPEN.getLabel())).thenReturn(List.of(open));

        List<TicketDTO> results = ticketService.listTickets(TicketStatus.OPEN.getLabel(), null);

        assertEquals(1, results.size());
        verify(ticketRepository).findByStatus(TicketStatus.OPEN.getLabel());
        verify(ticketRepository, never()).getAll();
        verify(ticketRepository, never()).searchByTitle(anyString());
    }

    @Test
    void shouldReturnAllWhenNoFilters() {
        TicketDTO ticket = existingOpenTicket();
        when(ticketRepository.getAll()).thenReturn(List.of(ticket));

        List<TicketDTO> results = ticketService.listTickets(null, null);

        assertEquals(1, results.size());
        verify(ticketRepository).getAll();
        verify(ticketRepository, never()).findByStatus(anyString());
        verify(ticketRepository, never()).searchByTitle(anyString());
    }

    @Test
    void shouldSortByCreatedAtDescending() {
        TicketDTO older = existingOpenTicket();
        older.setId("TKT-1001");
        older.setCreatedAt(Instant.parse("2025-01-01T10:00:00Z"));

        TicketDTO newer = existingOpenTicket();
        newer.setId("TKT-1002");
        newer.setCreatedAt(Instant.parse("2025-06-01T10:00:00Z"));

        when(ticketRepository.getAll()).thenReturn(List.of(older, newer));

        List<TicketDTO> results = ticketService.listTickets(null, null);

        assertEquals("TKT-1002", results.get(0).getId());
        assertEquals("TKT-1001", results.get(1).getId());
    }

    // --- helpers ---

    private TicketDTO validCreateInput() {
        TicketDTO dto = new TicketDTO();
        dto.setTitle("Test ticket");
        dto.setDescription("Test description");
        dto.setPriority("P2");
        dto.setCreatedBy(AGENT_ID);
        return dto;
    }

    private TicketDTO existingOpenTicket() {
        TicketDTO dto = new TicketDTO();
        dto.setId(TICKET_ID);
        dto.setTitle("Existing ticket");
        dto.setDescription("Existing description");
        dto.setPriority("P2");
        dto.setStatus(TicketStatus.OPEN.getLabel());
        dto.setCreatedBy(AGENT_ID);
        dto.setCreatedAt(Instant.parse("2025-03-01T09:00:00Z"));
        dto.setUpdatedAt(Instant.parse("2025-03-01T09:00:00Z"));
        return dto;
    }
}
