/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.tools.dbdocs.model;

import java.util.List;

/**
 * Documentation model for all schema information parsed from configured SQL files.
 */
public final class SchemaDoc {

    private final List<TableDoc> tables;
    private final List<SqlObjectDoc> sqlObjects;
    private final List<SqlFileDoc> sourceFiles;
    private final List<UpgradeChangeDoc> upgradeChanges;

    public SchemaDoc(
            List<TableDoc> tables,
            List<SqlObjectDoc> sqlObjects,
            List<SqlFileDoc> sourceFiles,
            List<UpgradeChangeDoc> upgradeChanges) {
        this.tables = List.copyOf(tables);
        this.sqlObjects = List.copyOf(sqlObjects);
        this.sourceFiles = List.copyOf(sourceFiles);
        this.upgradeChanges = List.copyOf(upgradeChanges);
    }

    public List<TableDoc> tables() {
        return tables;
    }

    public List<SqlObjectDoc> sqlObjects() {
        return sqlObjects;
    }

    public List<SqlFileDoc> sourceFiles() {
        return sourceFiles;
    }

    public List<UpgradeChangeDoc> upgradeChanges() {
        return upgradeChanges;
    }
}
