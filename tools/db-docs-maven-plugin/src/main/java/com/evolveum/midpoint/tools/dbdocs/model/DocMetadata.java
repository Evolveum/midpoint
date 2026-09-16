/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.model;

/**
 * Human-readable documentation metadata extracted from SQL annotation comments.
 */
public final class DocMetadata {

    public static final DocMetadata EMPTY = new DocMetadata(null, null, null, null, null, null, null, null);

    private final String description;
    private final String type;
    private final String since;
    private final String usedFor;
    private final String change;
    private final String regionSlug;
    private final String regionTitle;
    private final String regionDescription;

    public DocMetadata(String description, String type, String since, String usedFor, String change) {
        this(description, type, since, usedFor, change, null, null, null);
    }

    public DocMetadata(
            String description,
            String type,
            String since,
            String usedFor,
            String change,
            String regionSlug,
            String regionTitle,
            String regionDescription) {
        this.description = description;
        this.type = type;
        this.since = since;
        this.usedFor = usedFor;
        this.change = change;
        this.regionSlug = regionSlug;
        this.regionTitle = regionTitle;
        this.regionDescription = regionDescription;
    }

    public String description() {
        return description;
    }

    public String type() {
        return type;
    }

    public String since() {
        return since;
    }

    public String usedFor() {
        return usedFor;
    }

    public String change() {
        return change;
    }

    public String regionSlug() {
        return regionSlug;
    }

    public String regionTitle() {
        return regionTitle;
    }

    public String regionDescription() {
        return regionDescription;
    }

    public boolean startsRegion() {
        return regionSlug != null && !regionSlug.isBlank();
    }
}
