package com.mysite.core.mappers;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.mysite.core.dto.TicketDTO;
import com.mysite.core.util.TimeUtil;

/**
 * Maps ticket Content Fragment elements to {@link TicketDTO} (read path).
 * <p>
 * CF element {@code ticketId} maps to DTO field {@code id}. Used by the Content Fragment ticket
 * repository adapter; write mapping remains in the repository.
 * </p>
 */
public final class TicketMapper {

    public static final String ELEMENT_TICKET_ID = "ticketId";
    public static final String ELEMENT_TITLE = "title";
    public static final String ELEMENT_DESCRIPTION = "description";
    public static final String ELEMENT_PRIORITY = "priority";
    public static final String ELEMENT_STATUS = "status";
    public static final String ELEMENT_ASSIGNED_TO = "assignedTo";
    public static final String ELEMENT_CREATED_BY = "createdBy";
    public static final String ELEMENT_CREATED_AT = "createdAt";
    public static final String ELEMENT_UPDATED_AT = "updatedAt";

    private TicketMapper() {
    }

    /**
     * Maps a ticket Content Fragment to a {@link TicketDTO}.
     *
     * @param contentFragment ticket content fragment (non-null)
     * @return populated DTO; fields may be null when elements are absent or blank
     */
    public static TicketDTO toDto(ContentFragment contentFragment) {
        TicketDTO dto = new TicketDTO();
        dto.setId(getElementContent(contentFragment, ELEMENT_TICKET_ID));
        dto.setTitle(getElementContent(contentFragment, ELEMENT_TITLE));
        dto.setDescription(getElementContent(contentFragment, ELEMENT_DESCRIPTION));
        dto.setPriority(getElementContent(contentFragment, ELEMENT_PRIORITY));
        dto.setStatus(getElementContent(contentFragment, ELEMENT_STATUS));
        dto.setAssignedTo(blankToNull(getElementContent(contentFragment, ELEMENT_ASSIGNED_TO)));
        dto.setCreatedBy(getElementContent(contentFragment, ELEMENT_CREATED_BY));
        dto.setCreatedAt(TimeUtil.parseInstant(getElementContent(contentFragment, ELEMENT_CREATED_AT)));
        dto.setUpdatedAt(TimeUtil.parseInstant(getElementContent(contentFragment, ELEMENT_UPDATED_AT)));
        return dto;
    }

    /**
     * Reads the {@code ticketId} element value from a ticket fragment.
     *
     * @param contentFragment ticket content fragment
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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
