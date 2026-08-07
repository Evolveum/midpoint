/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.model;

import java.util.Objects;

/**
 * User-facing documentation region extracted from SQL region annotations.
 */
public final class DocRegion {

    public static final DocRegion UNCLASSIFIED = new DocRegion(
            "unclassified",
            "Unclassified",
            "Schema objects without explicit documentation region.",
            Integer.MAX_VALUE);

    private final String slug;
    private final String title;
    private final String description;
    private final int order;

    public DocRegion(String slug, String title, String description, int order) {
        this.slug = normalizeSlug(slug);
        this.title = title != null && !title.isBlank() ? title : titleFromSlug(this.slug);
        this.description = description;
        this.order = order;
    }

    public String slug() {
        return slug;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public int order() {
        return order;
    }

    private static String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return "unclassified";
        }
        return slug.strip().toLowerCase().replaceAll("[^a-z0-9_-]+", "-");
    }

    private static String titleFromSlug(String slug) {
        String[] words = slug.replace('_', '-').split("-");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                title.append(word.substring(1));
            }
        }
        return title.isEmpty() ? "Unclassified" : title.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DocRegion other)) {
            return false;
        }
        return slug.equals(other.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slug);
    }
}
