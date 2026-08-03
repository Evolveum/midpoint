/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser.annotations;

/**
 * Supported documentation annotation keys in SQL comments.
 */
enum AnnotationKey {
    SCRIPT_DESCRIPTION("script-description"),
    DESCRIPTION("description"),
    TYPE("type"),
    SINCE("since"),
    USED_FOR("usedFor"),
    CHANGE("change"),
    REGION("region"),
    REGION_TITLE("regionTitle"),
    REGION_DESCRIPTION("regionDescription"),
    AFFECTS("affects");

    private final String text;

    AnnotationKey(String text) {
        this.text = text;
    }

    static AnnotationKey from(String key) {
        for (AnnotationKey value : values()) {
            if (value.text.equalsIgnoreCase(key)) {
                return value;
            }
        }
        return null;
    }
}
