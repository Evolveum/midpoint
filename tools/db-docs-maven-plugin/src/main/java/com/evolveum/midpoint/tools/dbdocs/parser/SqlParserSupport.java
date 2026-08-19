/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser;

import java.util.List;
import java.util.Optional;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.Index;

/**
 * Small shared helpers for JSqlParser-based SQL parsers.
 */
public final class SqlParserSupport {

    private SqlParserSupport() {
    }

    /**
     * Parses SQL and returns it only if JSqlParser produced the requested statement type.
     */
    public static <T extends Statement> Optional<T> parseAs(String statement, Class<T> type) {
        return parseStatement(statement)
                .filter(type::isInstance)
                .map(type::cast);
    }

    /**
     * Parses SQL with JSqlParser, returning empty for unsupported statements.
     */
    public static Optional<Statement> parseStatement(String statement) {
        try {
            return Optional.of(CCJSqlParserUtil.parse(trimTrailingSemicolon(statement)));
        } catch (JSQLParserException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * Removes SQL identifier quotes used by PostgreSQL scripts.
     */
    public static String normalizeIdentifier(String identifier) {
        return identifier != null ? identifier.replace("\"", "") : "";
    }

    /**
     * Returns index column names using JSqlParser's structured column list.
     */
    public static List<String> columnNames(Index index) {
        if (index.getColumnsNames() != null) {
            return index.getColumnsNames();
        }

        return index.getColumns() != null
                ? index.getColumns().stream().map(Index.ColumnParams::getColumnName).toList()
                : List.of();
    }

    private static String trimTrailingSemicolon(String statement) {
        String trimmed = statement.strip();
        return trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
