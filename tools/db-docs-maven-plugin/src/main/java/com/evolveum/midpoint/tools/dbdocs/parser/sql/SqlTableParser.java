/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser.sql;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.evolveum.midpoint.tools.dbdocs.model.ColumnDoc;
import com.evolveum.midpoint.tools.dbdocs.model.DocMetadata;
import com.evolveum.midpoint.tools.dbdocs.model.DocRegion;
import com.evolveum.midpoint.tools.dbdocs.model.ForeignKeyDoc;
import com.evolveum.midpoint.tools.dbdocs.model.TableDoc;
import com.evolveum.midpoint.tools.dbdocs.parser.SqlParserSupport;
import com.evolveum.midpoint.tools.dbdocs.parser.postgres.PostgresCreateTableNormalizer;

import net.sf.jsqlparser.statement.ReferentialAction;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;

import static com.evolveum.midpoint.tools.dbdocs.parser.SqlParserSupport.normalizeIdentifier;

/**
 * Parses CREATE TABLE statements into table documentation models.
 *
 * PostgreSQL-specific table syntax is normalized before JSqlParser sees the statement.
 * This parser then extracts the table columns, primary keys, foreign keys, inheritance,
 * partition metadata, and inline REFERENCES constraints that should appear in generated docs.
 */
public class SqlTableParser {

    private static final Set<String> CONSTRAINT_STARTS = Set.of(
            "NOT", "NULL", "PRIMARY", "UNIQUE", "REFERENCES", "CHECK", "CONSTRAINT");

    private final PostgresCreateTableNormalizer normalizer = new PostgresCreateTableNormalizer();

    /**
     * Tries to parse a statement as CREATE TABLE using JSqlParser after PostgreSQL compatibility normalization.
     */
    public Optional<TableDoc> parseIfSupported(
            String statement,
            Path sourceFile,
            DocRegion region,
            DocMetadata metadata,
            Map<String, DocMetadata> metadataByColumn) {
        PostgresCreateTableNormalizer.Result normalized = normalizer.normalize(statement);
        if (normalized.partitionOf() != null) {
            return Optional.of(partitionTableDoc(normalized, sourceFile, region, metadata));
        }

        return SqlParserSupport.parseAs(normalized.sql(), CreateTable.class)
                .map(createTable -> toTableDoc(createTable, sourceFile, region, metadata, metadataByColumn, normalized));
    }

    private TableDoc partitionTableDoc(
            PostgresCreateTableNormalizer.Result normalized, Path sourceFile, DocRegion region, DocMetadata metadata) {
        return new TableDoc(
                normalized.tableName(),
                sourceFile,
                region,
                metadata,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                normalized.inheritsFrom(),
                normalized.partitionOf(),
                normalized.parseNote());
    }

    private TableDoc toTableDoc(
            CreateTable createTable,
            Path sourceFile,
            DocRegion region,
            DocMetadata metadata,
            Map<String, DocMetadata> metadataByColumn,
            PostgresCreateTableNormalizer.Result normalized) {
        Set<String> primaryKeyColumns = primaryKeyColumns(createTable);
        List<String> primaryKeyColumnList = List.copyOf(primaryKeyColumns);
        List<ColumnDoc> columns = columns(createTable, primaryKeyColumns, metadataByColumn);
        List<ForeignKeyDoc> foreignKeys = tableForeignKeys(createTable);

        if (createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition columnDefinition : createTable.getColumnDefinitions()) {
                ForeignKeyDoc foreignKey = inlineForeignKey(columnDefinition);
                if (foreignKey != null) {
                    foreignKeys.add(foreignKey);
                }
            }
        }

        return new TableDoc(
                normalizeIdentifier(createTable.getTable().getFullyQualifiedName()),
                sourceFile,
                region,
                metadata,
                columns,
                primaryKeyColumnList,
                List.of(),
                foreignKeys,
                normalized.inheritsFrom(),
                normalized.partitionOf(),
                normalized.parseNote());
    }

    private List<ColumnDoc> columns(
            CreateTable createTable, Set<String> primaryKeyColumns, Map<String, DocMetadata> metadataByColumn) {
        if (createTable.getColumnDefinitions() == null) {
            return List.of();
        }

        List<ColumnDoc> columns = new ArrayList<>();
        for (ColumnDefinition columnDefinition : createTable.getColumnDefinitions()) {
            columns.add(toColumnDoc(columnDefinition, primaryKeyColumns, metadataByColumn));
        }
        return columns;
    }

    private ColumnDoc toColumnDoc(
            ColumnDefinition columnDefinition, Set<String> primaryKeyColumns, Map<String, DocMetadata> metadataByColumn) {
        String columnName = normalizeIdentifier(columnDefinition.getColumnName());
        ColumnProperties properties = columnProperties(columnDefinition, primaryKeyColumns, columnName);

        return new ColumnDoc(
                columnName,
                columnDefinition.getColDataType() != null ? columnDefinition.getColDataType().toString() : "",
                metadataByColumn.getOrDefault(columnName, DocMetadata.EMPTY),
                properties.required(),
                properties.defaultValue(),
                properties.primaryKey(),
                properties.constraints());
    }

    private ColumnProperties columnProperties(
            ColumnDefinition columnDefinition, Set<String> primaryKeyColumns, String columnName) {
        List<String> specs = columnDefinition.getColumnSpecs() != null
                ? columnDefinition.getColumnSpecs()
                : List.of();
        boolean primaryKey = primaryKeyColumns.contains(columnName) || containsSpecSequence(specs, "PRIMARY", "KEY");
        boolean required = primaryKey || containsSpecSequence(specs, "NOT", "NULL");
        String defaultValue = defaultValue(specs);

        return new ColumnProperties(primaryKey, required, defaultValue, constraints(primaryKey, required, defaultValue));
    }

    private List<String> constraints(boolean primaryKey, boolean required, String defaultValue) {
        List<String> constraints = new ArrayList<>();
        if (primaryKey) {
            constraints.add("primary key");
        }
        if (required) {
            constraints.add("required");
        }
        if (defaultValue != null) {
            constraints.add("default `" + defaultValue + "`");
        }
        return constraints;
    }

    private List<ForeignKeyDoc> tableForeignKeys(CreateTable createTable) {
        List<ForeignKeyDoc> foreignKeys = new ArrayList<>();
        if (createTable.getIndexes() == null) {
            return foreignKeys;
        }

        for (Index index : createTable.getIndexes()) {
            if (index instanceof ForeignKeyIndex foreignKeyIndex) {
                foreignKeys.addAll(toForeignKeyDocs(foreignKeyIndex));
            }
        }

        return foreignKeys;
    }

    private List<ForeignKeyDoc> toForeignKeyDocs(ForeignKeyIndex foreignKeyIndex) {
        ReferentialAction deleteAction = foreignKeyIndex.getReferentialAction(ReferentialAction.Type.DELETE);
        return foreignKeys(
                SqlParserSupport.columnNames(foreignKeyIndex),
                foreignKeyIndex.getTable() != null
                        ? normalizeIdentifier(foreignKeyIndex.getTable().getFullyQualifiedName())
                        : "",
                foreignKeyIndex.getReferencedColumnNames() != null
                        ? foreignKeyIndex.getReferencedColumnNames()
                        : List.of(),
                deleteAction != null ? deleteAction.getAction().getAction() : null);
    }

    private List<ForeignKeyDoc> foreignKeys(
            List<String> localColumns, String referencedTable, List<String> referencedColumns, String deleteRule) {
        List<ForeignKeyDoc> foreignKeys = new ArrayList<>();

        for (int i = 0; i < localColumns.size(); i++) {
            foreignKeys.add(new ForeignKeyDoc(
                    normalizeIdentifier(localColumns.get(i)),
                    referencedTable,
                    referencedColumnAt(referencedColumns, i),
                    deleteRule));
        }

        return foreignKeys;
    }

    private String referencedColumnAt(List<String> referencedColumns, int index) {
        return index < referencedColumns.size()
                ? normalizeIdentifier(referencedColumns.get(index))
                : null;
    }

    /**
     * Extracts inline column REFERENCES constraints from JSqlParser column specs.
     *
     * Table-level foreign keys are exposed separately through CreateTable.getIndexes().
     */
    private ForeignKeyDoc inlineForeignKey(ColumnDefinition columnDefinition) {
        List<String> specs = columnDefinition.getColumnSpecs();
        if (specs == null) {
            return null;
        }

        for (int i = 0; i < specs.size(); i++) {
            if (!"REFERENCES".equalsIgnoreCase(specs.get(i)) || i + 1 >= specs.size()) {
                continue;
            }

            String referencedTable = normalizeIdentifier(specs.get(i + 1));
            String referencedColumn = referencedColumn(specs, i);
            String[] referenceTarget = splitReferenceTarget(referencedTable);

            if (referenceTarget != null) {
                referencedTable = referenceTarget[0];
                referencedColumn = referenceTarget[1];
            }

            return new ForeignKeyDoc(
                    normalizeIdentifier(columnDefinition.getColumnName()),
                    referencedTable,
                    referencedColumn,
                    deleteRule(specs, i));
        }

        return null;
    }

    private String referencedColumn(List<String> specs, int referencesIndex) {
        if (referencesIndex + 3 < specs.size() && "(".equals(specs.get(referencesIndex + 2))) {
            return normalizeIdentifier(specs.get(referencesIndex + 3));
        } else if (referencesIndex + 2 < specs.size()) {
            String parenthesizedColumn = unparenthesized(specs.get(referencesIndex + 2));
            if (parenthesizedColumn != null) {
                return normalizeIdentifier(parenthesizedColumn);
            }
        }

        return null;
    }

    private String deleteRule(List<String> specs, int referencesIndex) {
        for (int i = referencesIndex + 1; i < specs.size() - 2; i++) {
            if ("ON".equalsIgnoreCase(specs.get(i)) && "DELETE".equalsIgnoreCase(specs.get(i + 1))) {
                return specs.get(i + 2);
            }
        }

        return null;
    }

    private String[] splitReferenceTarget(String referenceTarget) {
        int openingParenthesis = referenceTarget.indexOf('(');
        int closingParenthesis = referenceTarget.lastIndexOf(')');
        if (openingParenthesis <= 0 || closingParenthesis <= openingParenthesis) {
            return null;
        }

        return new String[] {
                normalizeIdentifier(referenceTarget.substring(0, openingParenthesis)),
                normalizeIdentifier(referenceTarget.substring(openingParenthesis + 1, closingParenthesis))
        };
    }

    private Set<String> primaryKeyColumns(CreateTable createTable) {
        Set<String> primaryKeyColumns = new LinkedHashSet<>();

        if (createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition columnDefinition : createTable.getColumnDefinitions()) {
                List<String> specs = columnDefinition.getColumnSpecs() != null
                        ? columnDefinition.getColumnSpecs()
                        : List.of();
                if (containsSpecSequence(specs, "PRIMARY", "KEY")) {
                    primaryKeyColumns.add(normalizeIdentifier(columnDefinition.getColumnName()));
                }
            }
        }

        if (createTable.getIndexes() == null) {
            return primaryKeyColumns;
        }

        for (Index index : createTable.getIndexes()) {
            if (!isPrimaryKey(index)) {
                continue;
            }

            for (String columnName : SqlParserSupport.columnNames(index)) {
                primaryKeyColumns.add(normalizeIdentifier(columnName));
            }
        }

        return primaryKeyColumns;
    }

    private boolean isPrimaryKey(Index index) {
        String type = index.getType();
        if (type != null && "PRIMARY KEY".equalsIgnoreCase(type)) {
            return true;
        }

        return index.toString().toUpperCase(Locale.ROOT).startsWith("PRIMARY KEY");
    }

    private boolean containsSpecSequence(List<String> specs, String first, String second) {
        for (int i = 0; i < specs.size() - 1; i++) {
            if (first.equalsIgnoreCase(specs.get(i)) && second.equalsIgnoreCase(specs.get(i + 1))) {
                return true;
            }
        }

        return false;
    }

    private String defaultValue(List<String> specs) {
        for (int i = 0; i < specs.size(); i++) {
            if (!"DEFAULT".equalsIgnoreCase(specs.get(i))) {
                continue;
            }

            List<String> defaultParts = new ArrayList<>();
            for (int j = i + 1; j < specs.size(); j++) {
                String spec = specs.get(j);
                if (isConstraintStart(spec)) {
                    break;
                }
                defaultParts.add(spec);
            }

            if (!defaultParts.isEmpty()) {
                return String.join(" ", defaultParts);
            }
        }

        return null;
    }

    private boolean isConstraintStart(String spec) {
        return CONSTRAINT_STARTS.contains(spec.toUpperCase(Locale.ROOT));
    }

    private String unparenthesized(String token) {
        if (!token.startsWith("(") || !token.endsWith(")") || token.length() <= 2) {
            return null;
        }

        return token.substring(1, token.length() - 1);
    }

    /**
     * Parsed column flags and display constraints used to create a ColumnDoc.
     */
    private record ColumnProperties(
            boolean primaryKey, boolean required, String defaultValue, List<String> constraints) {
    }
}
