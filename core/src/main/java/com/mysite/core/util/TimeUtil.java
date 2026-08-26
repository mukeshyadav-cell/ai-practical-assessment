package com.mysite.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * ISO-8601 conversions between {@link Instant} and text stored in Content Fragment elements.
 */
public final class TimeUtil {

    private static final Logger LOG = LoggerFactory.getLogger(TimeUtil.class);

    private TimeUtil() {
    }

    /**
     * Parses an ISO-8601 instant string.
     *
     * @param iso instant text (e.g. {@code 2025-08-26T10:30:00Z})
     * @return parsed instant, or {@code null} when input is null/blank or unparseable
     */
    public static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(iso);
        } catch (DateTimeParseException e) {
            LOG.warn("Unable to parse ISO-8601 instant value '{}'", iso, e);
            return null;
        }
    }

    /**
     * Formats an instant as ISO-8601 text ({@link Instant#toString()}).
     *
     * @param instant instant to format
     * @return ISO-8601 string, or {@code null} when {@code instant} is null
     */
    public static String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
