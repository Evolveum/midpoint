/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.api.conndev;

import java.util.Locale;
import java.util.Map;

/**
 * Resolves the content type of connector-development documentation files.
 *
 * <p>A declared type (e.g. the one reported by a browser for an uploaded file) is preserved as-is
 * when it is meaningful; a blank or generic type ({@code application/octet-stream}, ...) is
 * detected from the file name suffix instead. The resolved value is stored in the development's
 * processed-documentation element and is the one the generation service upload sends, so the
 * service parses the file with the parser the content type designates (the suffix table mirrors
 * the service's own upload parsing).
 */
public final class DocumentationContentTypes {

    private static final Map<String, String> SUFFIXES = Map.ofEntries(
            Map.entry(".json", "application/json"),
            Map.entry(".conndev", "application/com.evolveum.conndev+json"),
            Map.entry(".yaml", "application/yaml"),
            Map.entry(".yml", "application/yaml"),
            Map.entry(".html", "text/html"),
            Map.entry(".htm", "text/html"),
            Map.entry(".xml", "application/xml"),
            Map.entry(".sql", "application/sql"),
            Map.entry(".csv", "text/csv"),
            Map.entry(".md", "text/markdown"),
            Map.entry(".txt", "text/plain"),
            Map.entry(".adoc", "text/x-asciidoc"),
            Map.entry(".asc", "text/x-asciidoc"),
            Map.entry(".graphql", "application/graphql"),
            Map.entry(".gql", "application/graphql"),
            Map.entry(".log", "text/plain"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

    private DocumentationContentTypes() {
    }

    /**
     * Resolves the content type of a documentation file: {@code declaredContentType} (the type
     * reported by the upload source) is preserved when it is non-blank and not generic; otherwise
     * the type is detected from the {@code fileName} suffix.
     *
     * @return the resolved content type, or {@code null} when neither source yields one
     */
    public static String resolve(String declaredContentType, String fileName) {
        var declared = normalize(declaredContentType);
        if (!declared.isEmpty() && !isGeneric(declared)) {
            return declared;
        }
        return detect(fileName);
    }

    /**
     * Detects the content type from the file name suffix.
     *
     * @return the detected content type, or {@code null} for an unknown suffix
     */
    public static String detect(String fileName) {
        if (fileName == null) {
            return null;
        }
        var dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return null;
        }
        return SUFFIXES.get(fileName.substring(dot).toLowerCase(Locale.ROOT));
    }

    private static String normalize(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isGeneric(String contentType) {
        return contentType.isEmpty()
                || "application/octet-stream".equals(contentType)
                || "binary/octet-stream".equals(contentType);
    }
}
