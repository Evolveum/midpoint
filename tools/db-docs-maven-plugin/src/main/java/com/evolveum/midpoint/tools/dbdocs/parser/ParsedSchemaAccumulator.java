/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.tools.dbdocs.model.ColumnDoc;
import com.evolveum.midpoint.tools.dbdocs.model.ForeignKeyDoc;
import com.evolveum.midpoint.tools.dbdocs.model.IndexDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SchemaDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SqlFileDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SqlObjectDoc;
import com.evolveum.midpoint.tools.dbdocs.model.TableDoc;
import com.evolveum.midpoint.tools.dbdocs.model.UpgradeChangeDoc;
import com.evolveum.midpoint.tools.dbdocs.parser.sql.SqlAlterTableParser;

/**
 * Collects parsed schema fragments and assembles the final documentation model.
 *
 * Some information, such as indexes and ALTER TABLE additions, is parsed separately
 * and merged into the corresponding table before the final SchemaDoc is created.
 */
public class ParsedSchemaAccumulator {

    private final Map<String, TableDoc> tables = new LinkedHashMap<>();
    private final Map<String, List<IndexDoc>> indexesByTable = new HashMap<>();
    private final Map<String, List<ColumnDoc>> addedColumnsByTable = new HashMap<>();
    private final Map<String, List<ForeignKeyDoc>> addedForeignKeysByTable = new HashMap<>();
    private final List<SqlObjectDoc> sqlObjects = new ArrayList<>();
    private final List<UpgradeChangeDoc> upgradeChanges = new ArrayList<>();

    public void addTable(TableDoc table) {
        tables.put(table.name(), table);
    }

    public void addIndex(IndexDoc index) {
        indexesByTable.computeIfAbsent(index.tableName(), ignored -> new ArrayList<>()).add(index);
    }

    public void addSqlObject(SqlObjectDoc sqlObject) {
        sqlObjects.add(sqlObject);
    }

    public void addUpgradeChange(UpgradeChangeDoc upgradeChange) {
        upgradeChanges.add(upgradeChange);
    }

    public void addColumn(SqlAlterTableParser.AddedColumn addedColumn) {
        addedColumnsByTable.computeIfAbsent(tableKey(addedColumn.tableName()), ignored -> new ArrayList<>())
                .add(addedColumn.column());
    }

    public void addForeignKeys(SqlAlterTableParser.AddedForeignKeys addedForeignKeys) {
        addedForeignKeysByTable.computeIfAbsent(tableKey(addedForeignKeys.tableName()), ignored -> new ArrayList<>())
                .addAll(addedForeignKeys.foreignKeys());
    }

    /**
     * Builds the final schema model by merging parsed indexes and ALTER TABLE additions into tables.
     */
    public SchemaDoc toSchemaDoc(List<SqlFileDoc> sourceFiles) {
        List<TableDoc> tableDocs = new ArrayList<>();
        for (TableDoc table : tables.values()) {
            tableDocs.add(withParsedAdditions(
                    table,
                    indexesByTable.getOrDefault(table.name(), List.of()),
                    addedColumnsByTable.getOrDefault(tableKey(table.name()), List.of()),
                    addedForeignKeysByTable.getOrDefault(tableKey(table.name()), List.of())));
        }
        tableDocs.sort(Comparator.comparing(TableDoc::name));
        sqlObjects.sort(Comparator.comparing(SqlObjectDoc::kind).thenComparing(SqlObjectDoc::name));

        return new SchemaDoc(tableDocs, sqlObjects, sourceFiles, upgradeChanges);
    }

    /**
     * Returns a table copy with indexes, added columns, and added foreign keys merged in.
     *
     * Columns added by ALTER TABLE are skipped if the CREATE TABLE parser already saw a column
     * with the same name.
     */
    private TableDoc withParsedAdditions(
            TableDoc table,
            List<IndexDoc> indexes,
            List<ColumnDoc> addedColumns,
            List<ForeignKeyDoc> addedForeignKeys) {
        List<ColumnDoc> columns = new ArrayList<>(table.columns());
        for (ColumnDoc addedColumn : addedColumns) {
            if (columns.stream().noneMatch(column -> column.name().equalsIgnoreCase(addedColumn.name()))) {
                columns.add(addedColumn);
            }
        }

        List<ForeignKeyDoc> foreignKeys = new ArrayList<>(table.foreignKeys());
        foreignKeys.addAll(addedForeignKeys);

        return new TableDoc(
                table.name(),
                table.sourceFile(),
                table.region(),
                table.metadata(),
                directColumns(table, columns),
                table.primaryKeyColumns(),
                indexes,
                foreignKeys,
                table.inheritsFrom(),
                table.partitionOf(),
                table.parseNote());
    }

    private List<ColumnDoc> directColumns(TableDoc table, List<ColumnDoc> columns) {
        if (!table.primaryKeyColumns().isEmpty() || table.inheritsFrom() == null) {
            return columns;
        }

        return columns.stream()
                .map(this::withoutPrimaryKeyConstraint)
                .toList();
    }

    private ColumnDoc withoutPrimaryKeyConstraint(ColumnDoc column) {
        if (!column.primaryKey()) {
            return column;
        }

        return new ColumnDoc(
                column.name(),
                column.type(),
                column.metadata(),
                column.required(),
                column.defaultValue(),
                false,
                column.constraints().stream()
                        .filter(constraint -> !"primary key".equals(constraint))
                        .toList());
    }

    private String tableKey(String tableName) {
        return tableName.toLowerCase();
    }
}
