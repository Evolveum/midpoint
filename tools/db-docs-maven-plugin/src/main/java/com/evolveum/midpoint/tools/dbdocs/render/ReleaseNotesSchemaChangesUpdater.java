/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.tools.dbdocs.model.SchemaDoc;
import com.evolveum.midpoint.tools.dbdocs.model.UpgradeChangeDoc;

/**
 * Updates the bounded generated native PostgreSQL schema changes block in release notes.
 *
 * The updater only replaces content between known markers. It does not insert the block automatically,
 * so release notes authors keep control over the section placement.
 */
public class ReleaseNotesSchemaChangesUpdater {

    private static final String START_MARKER = "// DB-DOCS-SCHEMA-CHANGES-START";
    private static final String END_MARKER = "// DB-DOCS-SCHEMA-CHANGES-END";

    private final VelocityTemplateRenderer templateRenderer = new VelocityTemplateRenderer();
    private final AsciiDocFormat format = new AsciiDocFormat();

    /**
     * Updates release notes in place. Returns false when required release-notes metadata or markers are missing.
     */
    public boolean update(Path releaseNotesPath, SchemaDoc schemaDoc) throws IOException {
        String content = Files.readString(releaseNotesPath, StandardCharsets.UTF_8);
        String releaseVersion = releaseVersion(content);
        if (releaseVersion == null || releaseVersion.isBlank()) {
            return false;
        }

        String updatedContent = updatedContent(content, renderBlock(schemaDoc, releaseVersion));
        if (updatedContent == null) {
            return false;
        }

        Files.writeString(
                releaseNotesPath,
                updatedContent,
                StandardCharsets.UTF_8);
        return true;
    }

    private String updatedContent(String content, String block) {
        int start = content.indexOf(START_MARKER);
        int end = content.indexOf(END_MARKER);
        if (start < 0 || end <= start) {
            return null;
        }

        int afterEnd = end + END_MARKER.length();
        return content.substring(0, start) + block + content.substring(afterEnd);
    }

    private String renderBlock(SchemaDoc schemaDoc, String releaseVersion) {
        List<UpgradeChangeDoc> changes = schemaDoc.upgradeChanges().stream()
                .filter(change -> releaseVersion.equals(change.metadata().since()))
                .sorted(Comparator.comparingInt(UpgradeChangeDoc::numericChangeNumber).reversed())
                .toList();

        Map<String, Object> context = new HashMap<>();
        context.put("changes", changes);
        context.put("releaseVersion", releaseVersion);
        context.put("r", format);
        return templateRenderer.render("release-notes-schema-changes.adoc.vm", context).stripTrailing();
    }

    private String releaseVersion(String content) {
        for (String line : content.lines().toList()) {
            String trimmed = line.trim();
            String value = valueAfterPrefix(trimmed, ":release-version:");
            if (value != null) {
                return unquote(value);
            }

            value = valueAfterPrefix(trimmed, "release-version:");
            if (value != null) {
                return unquote(value);
            }
        }

        return null;
    }

    private String valueAfterPrefix(String line, String prefix) {
        if (!line.startsWith(prefix)) {
            return null;
        }

        return line.substring(prefix.length()).trim();
    }

    private String unquote(String value) {
        if (value.length() >= 2
                && (value.startsWith("'") && value.endsWith("'") || value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

}
