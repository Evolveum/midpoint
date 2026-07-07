/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser.annotations;

import com.evolveum.midpoint.tools.dbdocs.model.DocMetadata;
import com.evolveum.midpoint.tools.dbdocs.model.DocRegion;

/**
 * Tracks the current documentation region while statements are parsed from one SQL file.
 */
public class RegionTracker {

    private static final String REGION = "@region:";
    private static final String REGION_TITLE = "@regionTitle:";
    private static final String REGION_DESCRIPTION = "@regionDescription:";

    private DocRegion currentRegion;
    private int nextRegionOrder;

    /**
     * Updates the current region when the statement starts a new documentation region.
     * Returns the region that should be assigned to this statement.
     */
    public DocRegion regionFor(String statement, DocMetadata metadata) {
        DocRegion region = regionFromStatement(statement, nextRegionOrder);
        if (region != null) {
            currentRegion = region;
            nextRegionOrder++;
        } else if (metadata.startsRegion()) {
            // Fallback for region annotations parsed as regular leading metadata.
            currentRegion = new DocRegion(
                    metadata.regionSlug(),
                    metadata.regionTitle(),
                    metadata.regionDescription(),
                    nextRegionOrder++);
        }

        return currentRegion;
    }

    private DocRegion regionFromStatement(String statement, int order) {
        String slug = null;
        String title = null;
        String description = null;
        boolean regionDescriptionContinues = false;
        boolean inBlockComment = false;

        /*
         * Region annotations are section-level directives, not object annotations.
         * They may be followed by normal developer comments before the SQL statement starts.
         */
        for (String line : statement.lines().toList()) {
            String strippedLine = line.stripLeading();
            boolean blockComment = inBlockComment || SqlCommentSupport.startsBlockComment(strippedLine);
            String commentText = blockComment
                    ? SqlCommentSupport.fromBlockComment(line)
                    : SqlCommentSupport.fromLineComment(line);
            if (commentText == null && strippedLine.isEmpty()) {
                commentText = "";
            }

            if (commentText == null) {
                break;
            }
            inBlockComment = blockComment && SqlCommentSupport.continuesBlockComment(strippedLine);

            slug = valueOrCurrent(commentText, REGION, slug);
            title = valueOrCurrent(commentText, REGION_TITLE, title);
            String newDescription = annotationValue(commentText, REGION_DESCRIPTION);
            if (newDescription != null) {
                description = newDescription;
                regionDescriptionContinues = blockComment;
                continue;
            }

            if (regionDescriptionContinues && blockComment && !commentText.isBlank()) {
                description = SqlCommentSupport.appendContinuation(description, commentText);
            } else if (!blockComment) {
                regionDescriptionContinues = false;
            }
        }

        return slug != null ? new DocRegion(slug, title, description, order) : null;
    }

    private String annotationValue(String commentText, String annotationPrefix) {
        return commentText.regionMatches(true, 0, annotationPrefix, 0, annotationPrefix.length())
                ? commentText.substring(annotationPrefix.length()).trim()
                : null;
    }

    private String valueOrCurrent(String commentText, String annotationPrefix, String currentValue) {
        String value = annotationValue(commentText, annotationPrefix);
        return value != null ? value : currentValue;
    }

}
