/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.api.conndev;

import java.util.List;

/**
 * The language/format a connector-development script artifact is written in. Replaces an earlier
 * {@code boolean yaml} that could only ever mean "YAML or not" - adding a future format here (its
 * name, canonical extension, and this file's two {@code switch}es where the compiler will point out
 * the gap) is the single place that needs touching, instead of another boolean threaded everywhere.
 */
public enum ConnDevScriptFormat {

    GROOVY("groovy"),
    YAML("yaml", "yml");

    /** Canonical extension (no dot), used when writing a filename in this format. */
    public final String extension;

    private final List<String> recognizedExtensions;

    ConnDevScriptFormat(String... extensions) {
        this.extension = extensions[0];
        this.recognizedExtensions = List.of(extensions);
    }

    public static final ConnDevScriptFormat DEFAULT = GROOVY;

    /** Parses a generation response's {@code format} field, case-insensitively; absent/unrecognized defaults to {@link #DEFAULT}. */
    public static ConnDevScriptFormat fromResponseField(String rawFormat) {
        for (var format : values()) {
            if (format.name().equalsIgnoreCase(rawFormat)) {
                return format;
            }
        }
        return DEFAULT;
    }

    /** Detects the format from a filename's extension; absent/unrecognized defaults to {@link #DEFAULT}. */
    public static ConnDevScriptFormat fromFilename(String filename) {
        if (filename != null) {
            String lower = filename.toLowerCase();
            for (var format : values()) {
                for (var ext : format.recognizedExtensions) {
                    if (lower.endsWith("." + ext)) {
                        return format;
                    }
                }
            }
        }
        return DEFAULT;
    }

    /** Whether {@code filename} ends in an extension any known format recognizes. */
    public static boolean hasRecognizedExtension(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        for (var format : values()) {
            for (var ext : format.recognizedExtensions) {
                if (lower.endsWith("." + ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Replaces {@code filename}'s trailing extension with this format's canonical one. */
    public String withExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }
        int lastDot = filename.lastIndexOf('.');
        String base = lastDot >= 0 ? filename.substring(0, lastDot) : filename;
        return base + "." + extension;
    }
}
