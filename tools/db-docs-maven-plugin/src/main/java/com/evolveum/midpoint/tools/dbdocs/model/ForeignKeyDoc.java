/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.model;

/**
 * Documentation model for a foreign key reference.
 */
public final class ForeignKeyDoc {

    private final String localColumn;
    private final String referencedTable;
    private final String referencedColumn;
    private final String deleteRule;

    public ForeignKeyDoc(String localColumn, String referencedTable, String referencedColumn, String deleteRule) {
        this.localColumn = localColumn;
        this.referencedTable = referencedTable;
        this.referencedColumn = referencedColumn;
        this.deleteRule = deleteRule;
    }

    public String localColumn() {
        return localColumn;
    }

    public String referencedTable() {
        return referencedTable;
    }

    public String referencedColumn() {
        return referencedColumn;
    }

    public String deleteRule() {
        return deleteRule;
    }
}
