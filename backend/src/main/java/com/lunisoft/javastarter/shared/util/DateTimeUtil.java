package com.lunisoft.javastarter.shared.util;

import java.time.*;

/**
 * The one calendar every business date of Dispo Ménage is read in: French wall-clock time.
 *
 * <p>Instants are stored in UTC, but every calendar decision — which day, which month, what time
 * of day — is taken in {@link #PARIS}. Never reach for {@code ZoneOffset.UTC} or the JVM default
 * zone for such decisions; go through these helpers instead.
 */
public final class DateTimeUtil {

    private DateTimeUtil() {}

    /** Zone id of the French time zone, for annotations that need a compile-time constant ({@code @Scheduled}). */
    public static final String PARIS_ID = "Europe/Paris";

    /** The French time zone (CET / CEST, daylight saving included). */
    public static final ZoneId PARIS = ZoneId.of(PARIS_ID);

    /** Today's date in France. */
    public static LocalDate today() {
        return LocalDate.now(PARIS);
    }

    /** The current French wall-clock date-time. */
    public static ZonedDateTime now() {
        return ZonedDateTime.now(PARIS);
    }

    public static Instant startOfYear(Integer year) {
        return LocalDate.of(year, 1, 1).atStartOfDay(PARIS).toInstant();
    }

    public static Instant endOfYear(Integer year) {
        return LocalDate.of(year, 12, 31).atTime(LocalTime.MAX).atZone(PARIS).toInstant();
    }
}
