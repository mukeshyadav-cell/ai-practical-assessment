package com.mysite.core.services;

import com.mysite.core.dto.CommentDTO;
import com.mysite.core.dto.TicketDTO;
import com.mysite.core.exception.TicketNotFoundException;
import com.mysite.core.exception.ValidationException;
import com.mysite.core.repositories.CommentRepository;
import com.mysite.core.repositories.TicketRepository;
import com.mysite.core.services.impl.CommentServiceImpl;
import com.mysite.core.statemachine.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CommentServiceImpl} — comment business logic with mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    private static final String TICKET_ID = "TKT-1001";
    private static final String OTHER_TICKET_ID = "TKT-9999";
    private static final String AGENT_ID = "agent-1";
    private static final String COMMENT_ID = "CMT-1001";

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    // --- addComment ---

    @Test
    void shouldAddCommentWhenTicketExistsAndMessageValid() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(openTicket()));
        CommentDTO input = validCommentInput();
        CommentDTO persisted = persistedComment("Looks good");
        when(commentRepository.add(any(CommentDTO.class))).thenReturn(persisted);

        CommentDTO result = commentService.addComment(TICKET_ID, input);

        assertEquals(COMMENT_ID, result.getId());
        assertEquals(TICKET_ID, result.getTicketId());
        assertEquals("Looks good", result.getMessage());
        verify(commentRepository).add(any(CommentDTO.class));
    }

    @Test
    void shouldRejectEmptyMessage() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(openTicket()));

        CommentDTO nullMessage = validCommentInput();
        nullMessage.setMessage(null);
        assertThrows(ValidationException.class,
                () -> commentService.addComment(TICKET_ID, nullMessage));

        CommentDTO blankMessage = validCommentInput();
        blankMessage.setMessage("");
        ValidationException ex = assertThrows(ValidationException.class,
                () -> commentService.addComment(TICKET_ID, blankMessage));
        assertEquals("VALIDATION_ERROR", ex.errorCode());

        verify(commentRepository, never()).add(any());
    }

    @Test
    void shouldRejectWhitespaceOnlyMessage() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(openTicket()));

        CommentDTO input = validCommentInput();
        input.setMessage("   ");

        assertThrows(ValidationException.class,
                () -> commentService.addComment(TICKET_ID, input));
        verify(commentRepository, never()).add(any());
    }

    @Test
    void shouldThrowNotFoundWhenTicketMissing() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.empty());

        TicketNotFoundException ex = assertThrows(TicketNotFoundException.class,
                () -> commentService.addComment(TICKET_ID, validCommentInput()));
        assertEquals(TICKET_ID, ex.getTicketId());
        assertEquals("NOT_FOUND", ex.errorCode());
        verify(commentRepository, never()).add(any());
    }

    @Test
    void shouldAllowCommentOnClosedTicket() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(ticketWithStatus(TicketStatus.CLOSED)));
        when(commentRepository.add(any(CommentDTO.class))).thenAnswer(invocation -> {
            CommentDTO toPersist = invocation.getArgument(0);
            toPersist.setId(COMMENT_ID);
            toPersist.setCreatedAt(Instant.parse("2025-06-01T12:00:00Z"));
            return toPersist;
        });

        CommentDTO result = commentService.addComment(TICKET_ID, validCommentInput());

        assertNotNull(result.getId());
        verify(commentRepository).add(any(CommentDTO.class));
    }

    @Test
    void shouldAllowCommentOnCancelledTicket() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(ticketWithStatus(TicketStatus.CANCELLED)));
        when(commentRepository.add(any(CommentDTO.class))).thenAnswer(invocation -> {
            CommentDTO toPersist = invocation.getArgument(0);
            toPersist.setId(COMMENT_ID);
            return toPersist;
        });

        commentService.addComment(TICKET_ID, validCommentInput());

        verify(commentRepository).add(any(CommentDTO.class));
    }

    @Test
    void shouldUsePathTicketIdAsAuthoritative() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(openTicket()));
        when(commentRepository.add(any(CommentDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentDTO input = validCommentInput();
        input.setTicketId(OTHER_TICKET_ID);

        commentService.addComment(TICKET_ID, input);

        ArgumentCaptor<CommentDTO> captor = ArgumentCaptor.forClass(CommentDTO.class);
        verify(commentRepository).add(captor.capture());
        assertEquals(TICKET_ID, captor.getValue().getTicketId());
    }

    @Test
    void shouldRejectBlankCreatedBy() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(openTicket()));

        CommentDTO nullCreatedBy = validCommentInput();
        nullCreatedBy.setCreatedBy(null);
        assertThrows(ValidationException.class,
                () -> commentService.addComment(TICKET_ID, nullCreatedBy));

        CommentDTO blankCreatedBy = validCommentInput();
        blankCreatedBy.setCreatedBy("  ");
        ValidationException ex = assertThrows(ValidationException.class,
                () -> commentService.addComment(TICKET_ID, blankCreatedBy));
        assertEquals("createdBy", ex.getField());
        assertEquals("VALIDATION_ERROR", ex.errorCode());

        verify(commentRepository, never()).add(any());
    }

    // --- listComments ---

    @Test
    void shouldReturnCommentsWhenTicketExists() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(openTicket()));
        CommentDTO first = persistedComment("First");
        CommentDTO second = persistedComment("Second");
        second.setId("CMT-1002");
        second.setCreatedAt(Instant.parse("2025-06-02T12:00:00Z"));
        when(commentRepository.listByTicket(TICKET_ID)).thenReturn(List.of(first, second));

        List<CommentDTO> results = commentService.listComments(TICKET_ID);

        assertEquals(2, results.size());
        assertSame(first, results.get(0));
        assertSame(second, results.get(1));
        verify(commentRepository).listByTicket(TICKET_ID);
    }

    @Test
    void shouldThrowNotFoundWhenListingCommentsOfMissingTicket() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.empty());

        TicketNotFoundException ex = assertThrows(TicketNotFoundException.class,
                () -> commentService.listComments(TICKET_ID));
        assertEquals(TICKET_ID, ex.getTicketId());
        verify(commentRepository, never()).listByTicket(any());
    }

    @Test
    void shouldReturnEmptyListWhenNoComments() {
        when(ticketRepository.getById(TICKET_ID)).thenReturn(Optional.of(openTicket()));
        when(commentRepository.listByTicket(TICKET_ID)).thenReturn(Collections.emptyList());

        List<CommentDTO> results = commentService.listComments(TICKET_ID);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // --- helpers ---

    private CommentDTO validCommentInput() {
        CommentDTO comment = new CommentDTO();
        comment.setMessage("  Looks good  ");
        comment.setCreatedBy(AGENT_ID);
        return comment;
    }

    private CommentDTO persistedComment(String message) {
        CommentDTO comment = new CommentDTO();
        comment.setId(COMMENT_ID);
        comment.setTicketId(TICKET_ID);
        comment.setMessage(message);
        comment.setCreatedBy(AGENT_ID);
        comment.setCreatedAt(Instant.parse("2025-06-01T12:00:00Z"));
        return comment;
    }

    private TicketDTO openTicket() {
        return ticketWithStatus(TicketStatus.OPEN);
    }

    private TicketDTO ticketWithStatus(TicketStatus status) {
        TicketDTO ticket = new TicketDTO();
        ticket.setId(TICKET_ID);
        ticket.setTitle("Test ticket");
        ticket.setDescription("Description");
        ticket.setPriority("P2");
        ticket.setStatus(status.getLabel());
        ticket.setCreatedBy(AGENT_ID);
        ticket.setCreatedAt(Instant.parse("2025-03-01T09:00:00Z"));
        ticket.setUpdatedAt(Instant.parse("2025-03-01T09:00:00Z"));
        return ticket;
    }
}
