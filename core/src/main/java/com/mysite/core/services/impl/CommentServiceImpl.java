package com.mysite.core.services.impl;

import com.mysite.core.dto.CommentDTO;
import com.mysite.core.exception.TicketNotFoundException;
import com.mysite.core.repositories.CommentRepository;
import com.mysite.core.repositories.TicketRepository;
import com.mysite.core.services.CommentService;
import com.mysite.core.validation.CommentValidator;
import com.mysite.core.validation.TicketValidator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Default {@link CommentService} implementation.
 * <p>
 * Field validation is delegated to {@link CommentValidator}; ticket existence checks use
 * {@link TicketRepository} in this service.
 * </p>
 */
@Component(service = CommentService.class)
public class CommentServiceImpl implements CommentService {

    private static final Logger LOG = LoggerFactory.getLogger(CommentServiceImpl.class);

    @Reference(target = "(impl.type=contentfragment)")
    private CommentRepository commentRepository;

    @Reference(target = "(impl.type=contentfragment)")
    private TicketRepository ticketRepository;

    @Override
    public CommentDTO addComment(String ticketId, CommentDTO comment) {
        String normalizedTicketId = TicketValidator.requireTicketId(ticketId);
        requireTicketExists(normalizedTicketId);

        CommentValidator.validateComment(comment);

        CommentDTO toPersist = new CommentDTO();
        toPersist.setTicketId(normalizedTicketId);
        toPersist.setMessage(comment.getMessage().trim());
        toPersist.setCreatedBy(comment.getCreatedBy().trim());

        CommentDTO persisted = commentRepository.add(toPersist);
        LOG.info("Added comment {} to ticket {}", persisted.getId(), normalizedTicketId);
        return persisted;
    }

    @Override
    public List<CommentDTO> listComments(String ticketId) {
        String normalizedTicketId = TicketValidator.requireTicketId(ticketId);
        requireTicketExists(normalizedTicketId);

        List<CommentDTO> comments = commentRepository.listByTicket(normalizedTicketId);
        if (comments == null) {
            return Collections.emptyList();
        }
        return comments;
    }

    private void requireTicketExists(String ticketId) {
        if (ticketRepository.getById(ticketId).isEmpty()) {
            throw new TicketNotFoundException(ticketId);
        }
    }
}
