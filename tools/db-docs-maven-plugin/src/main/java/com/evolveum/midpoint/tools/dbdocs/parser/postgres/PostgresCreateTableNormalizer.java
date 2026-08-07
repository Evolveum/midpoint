/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser.postgres;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evolveum.midpoint.tools.dbdocs.parser.SqlParserSupport;

/**
 * Performs only the PostgreSQL CREATE TABLE adjustments needed before handing the statement to JSqlParser.
 */
public class PostgresCreateTableNormalizer {

    private static final Pattern CREATE_TABLE_PARTITION = Pattern.compile(
            "(?is)^CREATE\\s+TABLE\\s+([^\\s(]+)\\s+PARTITION\\s+OF\\s+([^\\s(]+).*");
    private static final Pattern CREATE_TABLE_INHERITS = Pattern.compile(
            "(?is)(.*\\))\\s+INHERITS\\s*\\(([^)]+)\\)\\s*$");
    private static final Pattern CREATE_TABLE_PARTITION_BY = Pattern.compile(
            "(?is)(.*\\))\\s+PARTITION\\s+BY\\s+.*$");

    /**
     * Returns SQL normalized for JSqlParser plus PostgreSQL table metadata.
     */
    public Result normalize(String statement) {
        Matcher partitionMatcher = CREATE_TABLE_PARTITION.matcher(statement);
        if (partitionMatcher.matches()) {
            return new Result(
                    statement,
                    SqlParserSupport.normalizeIdentifier(partitionMatcher.group(1)),
                    null,
                    SqlParserSupport.normalizeIdentifier(partitionMatcher.group(2)),
                    "Columns are inherited from the partitioned table.");
        }

        String normalized = statement.replaceAll("(?i)\\)\\s+NO\\s+INHERIT", ")");
        String inheritsFrom = null;
        Matcher inheritsMatcher = CREATE_TABLE_INHERITS.matcher(normalized);
        if (inheritsMatcher.matches()) {
            normalized = inheritsMatcher.group(1);
            inheritsFrom = SqlParserSupport.normalizeIdentifier(inheritsMatcher.group(2).trim());
        }

        Matcher partitionByMatcher = CREATE_TABLE_PARTITION_BY.matcher(normalized);
        if (partitionByMatcher.matches()) {
            normalized = partitionByMatcher.group(1);
        }

        return new Result(
                normalized,
                null,
                inheritsFrom,
                null,
                null);
    }

    /**
     * Result of PostgreSQL CREATE TABLE normalization.
     */
    public static class Result {

        private final String sql;
        private final String tableName;
        private final String inheritsFrom;
        private final String partitionOf;
        private final String parseNote;

        private Result(String sql, String tableName, String inheritsFrom, String partitionOf, String parseNote) {
            this.sql = sql;
            this.tableName = tableName;
            this.inheritsFrom = inheritsFrom;
            this.partitionOf = partitionOf;
            this.parseNote = parseNote;
        }

        public String sql() {
            return sql;
        }

        public String tableName() {
            return tableName;
        }

        public String inheritsFrom() {
            return inheritsFrom;
        }

        public String partitionOf() {
            return partitionOf;
        }

        public String parseNote() {
            return parseNote;
        }
    }
}
