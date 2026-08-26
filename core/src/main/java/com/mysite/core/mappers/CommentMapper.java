package com.mysite.core.mappers;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.mysite.core.dto.CommentDTO;
import com.mysite.core.util.TimeUtil;

/**
 * Maps comment Content Fragment elements to {@link CommentDTO} (read path).
 * <p>
 * CF element {@code commentId} maps to DTO field {@code id}. Used by the Content Fragment
 * comment repository adapter; write mapping remains in the repository.
 * </p>
 */
public final class CommentMapper {

    public static final String ELEMENT_COMMENT_ID = "commentId";
    public static final String ELEMENT_TICKET_ID = "ticketId";
    public static final String ELEMENT_MESSAGE = "message";
    public static final String ELEMENT_CREATED_BY = "createdBy";
    public static final String ELEMENT_CREATED_AT = "createdAt";

    private CommentMapper() {
    }

    /**
     * Maps a comment Content Fragment to a {@link CommentDTO}.
     *
     * @param contentFragment comment content fragment (non-null)
     * @return populated DTO; fields may be null when elements are absent or blank
     */
    public static CommentDTO toDto(ContentFragment contentFragment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(getElementContent(contentFragment, ELEMENT_COMMENT_ID));
        dto.setTicketId(getElementContent(contentFragment, ELEMENT_TICKET_ID));
        dto.setMessage(getElementContent(contentFragment, ELEMENT_MESSAGE));
        dto.setCreatedBy(getElementContent(contentFragment, ELEMENT_CREATED_BY));
        dto.setCreatedAt(TimeUtil.parseInstant(getElementContent(contentFragment, ELEMENT_CREATED_AT)));
        return dto;
    }

    /**
     * Reads the parent {@code ticketId} element value from a comment fragment.
     *
     * @param contentFragment comment content fragment
     * @return ticket id text, or {@code null} when absent
     */
    public static String readTicketId(ContentFragment contentFragment) {
        return getElementContent(contentFragment, ELEMENT_TICKET_ID);
    }

    private static String getElementContent(ContentFragment contentFragment, String elementName) {
        ContentElement element = contentFragment.getElement(elementName);
        if (element == null) {
            return null;
        }
        return element.getContent();
    }
}
