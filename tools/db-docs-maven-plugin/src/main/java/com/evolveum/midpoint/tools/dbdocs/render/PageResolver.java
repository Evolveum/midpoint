/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.render;

import java.nio.file.Path;

import com.evolveum.midpoint.tools.dbdocs.model.DocRegion;

/**
 * Resolves generated AsciiDoc page paths for script pages and split postgres.sql region pages.
 */
public class PageResolver {

    /**
     * Returns the generated script page path for one SQL file.
     */
    public Path scriptPagePath(Path sourceFile) {
        return Path.of(DbSchemaAsciiDocRenderer.SCRIPTS_DIRECTORY, scriptOutputFileName(sourceFile));
    }

    /**
     * Returns the generated AsciiDoc file name for a source SQL file.
     */
    public static String scriptOutputFileName(Path sqlFile) {
        String fileName = sqlFile.getFileName().toString();
        if (fileName.endsWith(".sql")) {
            fileName = fileName.substring(0, fileName.length() - ".sql".length());
        }
        return fileName + ".adoc";
    }

    /**
     * Returns the generated region page path for split postgres.sql documentation.
     */
    public Path postgresRegionPagePath(DocRegion region) {
        return Path.of(DbSchemaAsciiDocRenderer.SCRIPTS_DIRECTORY, "postgres", region.slug() + ".adoc");
    }

    /**
     * Returns the page containing an object from the given SQL file.
     */
    public Path pageFor(Path sourceFile, boolean splitScript, DocRegion region) {
        return splitScript ? postgresRegionPagePath(region) : scriptPagePath(sourceFile);
    }

}
