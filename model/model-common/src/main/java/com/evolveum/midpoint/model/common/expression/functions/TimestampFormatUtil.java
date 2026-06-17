/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.functions;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author semancik
 */
public class TimestampFormatUtil {

    private static final Map<String, String> POSIX_TO_JAVA_MAP = new HashMap<>();

    static {
        POSIX_TO_JAVA_MAP.put("%Y", "yyyy");
        POSIX_TO_JAVA_MAP.put("%y", "yy");
        POSIX_TO_JAVA_MAP.put("%m", "MM");
        POSIX_TO_JAVA_MAP.put("%d", "dd");
        POSIX_TO_JAVA_MAP.put("%H", "HH");
        POSIX_TO_JAVA_MAP.put("%M", "mm");
        POSIX_TO_JAVA_MAP.put("%S", "ss");
        POSIX_TO_JAVA_MAP.put("%z", "XX");
        POSIX_TO_JAVA_MAP.put("%Z", "z");
    }

    /**
     * Translates a POSIX-style format string (%Y-%m-%d) to Java style (yyyy-MM-dd)
     */
    private static String translatePattern(String posixFormat) {
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile("%[A-Za-z]").matcher(posixFormat);
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(POSIX_TO_JAVA_MAP.getOrDefault(m.group(), m.group())));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * strptime: Parses a string into a ZonedDateTime.
     * POSIX-like function, compatible with other CEL implementations and UNIX world.
     */
    public static ZonedDateTime strptime(String input, String posixFormat) {
        String javaPattern = translatePattern(posixFormat);

        // Use SMART resolver to handle dates strictly but reasonably
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(javaPattern)
                .withZone(ZoneId.of("UTC"))
                .withResolverStyle(ResolverStyle.SMART);

        // Parse to ZonedDateTime first to capture all fields
        return ZonedDateTime.parse(input, formatter);
    }

    /**
     * strftime: Formats an Instant into a string
     */
    public static String strftime(Instant instant, String posixFormat) {
        String javaPattern = translatePattern(posixFormat);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(javaPattern)
                .withZone(ZoneId.of("UTC"));

        return formatter.format(instant);
    }

    /**
     * strftime: Formats an Instant into a string
     */
    public static String strftime(ZonedDateTime zdt, String posixFormat) {
        String javaPattern = translatePattern(posixFormat);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(javaPattern);

        return formatter.format(zdt);
    }
}
