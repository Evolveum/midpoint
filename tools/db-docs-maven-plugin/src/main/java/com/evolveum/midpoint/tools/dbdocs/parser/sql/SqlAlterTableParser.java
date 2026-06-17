/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import net.sf.jsqlparser.statement.ReferentialAction;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;

import com.evolveum.midpoint.tools.dbdocs.model.ColumnDoc;
import com.evolveum.midpoint.tools.dbdocs.model.DocMetadata;
import com.evolveum.midpoint.tools.dbdocs.model.ForeignKeyDoc;
import com.evolveum.midpoint.tools.dbdocs.parser.SqlParserSupport;

import static com.evolveum.midpoint.tools.dbdocs.parser.SqlParserSupport.normalizeIdentifier;

/**
 * Parses only the ALTER TABLE forms that can be merged into existing table documentation:
 * ADD COLUMN and ADD CONSTRAINT FOREIGN KEY.
 *
 * JSqlParser is used for structural parsing, but this class intentionally maps only the
 * narrow ALTER forms that affect generated table columns and foreign keys.
 */
public class SqlAlterTableParser {

    private static final Set<String> CONSTRAINT_STARTS = Set.of(
            "NOT", "NULL", "PRIMARY", "UNIQUE", "REFERENCES", "CHECK", "CONSTRAINT");

    /**
     * Tries to parse ALTER TABLE ADD COLUMN and convert the added column into table documentation.
     */
    public Optional<AddedColumn> parseAddedColumn(String statement, DocMetadata metadata) {
        return parseAlter(statement)
                .flatMap(alter -> addedColumn(alter)
                        .map(column -> new AddedColumn(tableName(alter), columnDoc(column, metadata))));
    }

    /**
     * Tries to parse ALTER TABLE ADD CONSTRAINT FOREIGN KEY and convert it into table foreign-key documentation.
     */
    public Optional<AddedForeignKeys> parseAddedForeignKeys(String statement) {
        return parseAlter(statement)
                .map(alter -> new AddedForeignKeys(tableName(alter), addedForeignKeys(alter)))
                .filter(added -> !added.foreignKeys().isEmpty());
    }

    private Optional<Alter> parseAlter(String statement) {
        return SqlParserSupport.parseAs(statement, Alter.class);
    }

    private Optional<AlterExpression.ColumnDataType> addedColumn(Alter alter) {
        return alter.getAlterExpressions().stream()
                .filter(expression -> expression.getOperation() == AlterOperation.ADD)
                .filter(expression -> expression.getColDataTypeList() != null)
                .filter(expression -> expression.getColDataTypeList().size() == 1)
                .map(expression -> expression.getColDataTypeList().get(0))
                .findFirst();
    }

    private ColumnDoc columnDoc(ColumnDefinition column, DocMetadata metadata) {
        String columnName = normalizeIdentifier(column.getColumnName());
        List<String> specs = column.getColumnSpecs() != null ? column.getColumnSpecs() : List.of();
        boolean required = containsSequence(specs, "NOT", "NULL");
        String defaultValue = defaultValue(specs);

        return new ColumnDoc(
                columnName,
                column.getColDataType() != null ? column.getColDataType().toString() : "",
                metadata.description() != null ? metadata : defaultMetadata(columnName),
                required,
                defaultValue,
                false,
                constraints(required, defaultValue));
    }

    private List<String> constraints(boolean required, String defaultValue) {
        if (required && defaultValue != null) {
            return List.of("required", "default `" + defaultValue + "`");
        } else if (required) {
            return List.of("required");
        } else if (defaultValue != null) {
            return List.of("default `" + defaultValue + "`");
        }
        return List.of();
    }

    private List<ForeignKeyDoc> addedForeignKeys(Alter alter) {
        return alter.getAlterExpressions().stream()
                .filter(expression -> expression.getOperation() == AlterOperation.ADD)
                .flatMap(expression -> foreignKeys(expression).stream())
                .toList();
    }

    /**
     * JSqlParser exposes foreign-key ALTER expressions in different shapes depending on syntax.
     * Keep both extraction paths to preserve composite foreign keys, e.g.
     * FOREIGN KEY (ownerOid, assignmentCid) REFERENCES m_assignment (ownerOid, cid).
     */
    private List<ForeignKeyDoc> foreignKeys(AlterExpression expression) {
        if (expression.getIndex() instanceof ForeignKeyIndex foreignKeyIndex) {
            return foreignKeys(foreignKeyIndex);
        }

        if (expression.getFkColumns() != null && expression.getFkSourceTable() != null) {
            return foreignKeys(
                    expression.getFkColumns(),
                    normalizeIdentifier(expression.getFkSourceTable()),
                    expression.getFkSourceColumns() != null ? expression.getFkSourceColumns() : List.of(),
                    deleteRule(expression));
        }

        return List.of();
    }

    private List<ForeignKeyDoc> foreignKeys(ForeignKeyIndex foreignKeyIndex) {
        return foreignKeys(
                SqlParserSupport.columnNames(foreignKeyIndex),
                foreignKeyIndex.getTable() != null
                        ? normalizeIdentifier(foreignKeyIndex.getTable().getFullyQualifiedName())
                        : "",
                foreignKeyIndex.getReferencedColumnNames() != null
                        ? foreignKeyIndex.getReferencedColumnNames()
                        : List.of(),
                deleteRule(foreignKeyIndex));
    }

    private List<ForeignKeyDoc> foreignKeys(
            List<String> localColumns, String referencedTable, List<String> referencedColumns, String deleteRule) {
        List<ForeignKeyDoc> foreignKeys = new ArrayList<>();
        for (int i = 0; i < localColumns.size(); i++) {
            foreignKeys.add(new ForeignKeyDoc(
                    normalizeIdentifier(localColumns.get(i)),
                    referencedTable,
                    i < referencedColumns.size() ? normalizeIdentifier(referencedColumns.get(i)) : null,
                    deleteRule));
        }
        return foreignKeys;
    }

    private boolean containsSequence(List<String> tokens, String first, String second) {
        for (int i = 0; i < tokens.size() - 1; i++) {
            if (first.equalsIgnoreCase(tokens.get(i)) && second.equalsIgnoreCase(tokens.get(i + 1))) {
                return true;
            }
        }
        return false;
    }

    private String defaultValue(List<String> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            if (!"DEFAULT".equalsIgnoreCase(tokens.get(i))) {
                continue;
            }

            List<String> parts = new ArrayList<>();
            for (int j = i + 1; j < tokens.size() && !isConstraintStart(tokens.get(j)); j++) {
                parts.add(tokens.get(j));
            }
            return parts.isEmpty() ? null : String.join(" ", parts);
        }
        return null;
    }

    private boolean isConstraintStart(String token) {
        return CONSTRAINT_STARTS.contains(token.toUpperCase(Locale.ROOT));
    }

    private String deleteRule(ForeignKeyIndex foreignKeyIndex) {
        ReferentialAction deleteAction = foreignKeyIndex.getReferentialAction(ReferentialAction.Type.DELETE);
        return deleteAction != null
                ? deleteAction.getAction().getAction()
                : foreignKeyIndex.getOnDeleteReferenceOption();
    }

    private String deleteRule(AlterExpression expression) {
        ReferentialAction deleteAction = expression.getReferentialAction(ReferentialAction.Type.DELETE);
        return deleteAction != null ? deleteAction.getAction().getAction() : null;
    }

    /**
     * Provides fallback descriptions for discriminator columns that are added by ALTER TABLE statements
     * and therefore may not have column annotations in the original CREATE TABLE block.
     */
    private DocMetadata defaultMetadata(String columnName) {
        return switch (columnName) {
            case "objectType" -> new DocMetadata("Object type discriminator for this table.", null, null, null, null);
            case "containerType" -> new DocMetadata("Container type discriminator for this table.", null, null, null, null);
            case "referenceType" -> new DocMetadata("Reference type discriminator for this table.", null, null, null, null);
            default -> DocMetadata.EMPTY;
        };
    }

    private String tableName(Alter alter) {
        return normalizeIdentifier(alter.getTable().getFullyQualifiedName());
    }

    /**
     * Parsed ALTER TABLE ADD COLUMN result ready to be merged into table documentation.
     */
    public record AddedColumn(String tableName, ColumnDoc column) {
    }

    /**
     * Parsed ALTER TABLE foreign-key result ready to be merged into table documentation.
     */
    public record AddedForeignKeys(String tableName, List<ForeignKeyDoc> foreignKeys) {
        public AddedForeignKeys {
            foreignKeys = List.copyOf(foreignKeys);
        }
    }
}
