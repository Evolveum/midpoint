/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evolveum.midpoint.tools.dbdocs.model.DocMetadata;
import com.evolveum.midpoint.tools.dbdocs.model.UpgradeChangeDoc;
import com.evolveum.midpoint.tools.dbdocs.parser.annotations.SqlAnnotationExtractor;

/**
 * Parses documented upgrade changes from midPoint upgrade SQL scripts.
 *
 * Upgrade scripts wrap real schema changes in apply_change(...) or apply_audit_change(...).
 * We only need the wrapper change number here; the SQL body inside the dollar-quoted argument
 * is documented through annotations, not parsed as normal schema SQL.
 */
public class UpgradeChangeParser {

    private static final Pattern UPGRADE_CHANGE_CALL = Pattern.compile(
            "(?is)^CALL\\s+(?:apply_audit_change|apply_change)\\s*\\(\\s*(\\d+)\\s*,.*");

    private final SqlAnnotationExtractor annotationExtractor;

    /**
     * Creates an upgrade parser using the shared annotation extractor.
     */
    public UpgradeChangeParser(SqlAnnotationExtractor annotationExtractor) {
        this.annotationExtractor = annotationExtractor;
    }

    /**
     * Parses a documented upgrade statement, if it has upgrade metadata.
     */
    public Optional<UpgradeChangeDoc> parse(
            Path sourceFile, String statement, String strippedStatement, DocMetadata metadata) {
        if (!isDocumentedChange(metadata)) {
            return Optional.empty();
        }

        return Optional.of(new UpgradeChangeDoc(
                sourceFile,
                metadata,
                changeNumber(strippedStatement),
                annotationExtractor.extractAffectedObjects(statement)));
    }

    private boolean isDocumentedChange(DocMetadata metadata) {
        return metadata.change() != null || metadata.description() != null || metadata.since() != null;
    }

    private String changeNumber(String statement) {
        Matcher matcher = UPGRADE_CHANGE_CALL.matcher(statement);
        return matcher.matches() ? matcher.group(1) : "-";
    }
}
