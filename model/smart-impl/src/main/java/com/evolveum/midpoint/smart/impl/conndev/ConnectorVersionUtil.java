/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version bumping for the low-code connector import flow: an imported connector is developed
 * under a new (minor-bumped) version, so the imported bundle can coexist with the original one.
 */
public final class ConnectorVersionUtil {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)*)(.*)$");

    private ConnectorVersionUtil() {
    }

    /**
     * Bumps the version by one minor level: {@code 1.0 → 1.1}, {@code 1.2.3 → 1.3.0} (the patch
     * is reset to zero), {@code 0.2-SNAPSHOT → 0.3-SNAPSHOT} (a trailing qualifier is preserved).
     * A single-component version gets a second component ({@code 2 → 2.1}). Versions that cannot
     * be parsed (no leading numeric components) are returned unchanged.
     */
    public static String bumpMinor(String version) {
        if (version == null || version.isBlank()) {
            return version;
        }
        var matcher = VERSION_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            return version;
        }
        var parts = matcher.group(1).split("\\.");
        var suffix = matcher.group(2);

        var bumped = new StringBuilder(parts[0]);
        if (parts.length == 1) {
            bumped.append(".1");
        } else {
            bumped.append('.').append(Long.parseLong(parts[1]) + 1);
            if (parts.length >= 3) {
                bumped.append(".0");
            }
        }
        // Join the qualifier with a dash unless it already starts with one or with a dot
        // (e.g. {@code 1.2.RC1} keeps its dot, {@code 1.2-SNAPSHOT} keeps its dash).
        if (!suffix.isEmpty() && suffix.charAt(0) != '-' && suffix.charAt(0) != '.') {
            bumped.append('-');
        }
        return bumped.append(suffix).toString();
    }
}
