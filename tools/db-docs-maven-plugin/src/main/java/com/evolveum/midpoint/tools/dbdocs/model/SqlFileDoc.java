/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.model;

import java.nio.file.Path;

/**
 * Documentation model for one source SQL file.
 */
public final class SqlFileDoc {

    /**
     * Category of SQL file handled by the generator.
     */
    public enum Category {
        INITIAL_SCHEMA,
        UPGRADE
    }

    public static Category category(Path sqlFile) {
        return sqlFile.getFileName().toString().contains("upgrade")
                ? Category.UPGRADE
                : Category.INITIAL_SCHEMA;
    }

    private final Path path;
    private final Category category;
    private final String scriptDescription;

    public SqlFileDoc(Path path, Category category) {
        this(path, category, null);
    }

    public SqlFileDoc(Path path, Category category, String scriptDescription) {
        this.path = path;
        this.category = category;
        this.scriptDescription = scriptDescription;
    }

    public Path path() {
        return path;
    }

    public Category category() {
        return category;
    }

    public String scriptDescription() {
        return scriptDescription;
    }

    public SqlFileDoc withScriptDescription(String description) {
        return new SqlFileDoc(path, category, description);
    }
}
