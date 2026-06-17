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

    public SqlFileDoc(Path path, Category category) {
        this.path = path;
        this.category = category;
    }

    public Path path() {
        return path;
    }

    public Category category() {
        return category;
    }
}
