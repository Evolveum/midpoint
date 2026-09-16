/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts top-level SQL statements before handing selected statements to JSqlParser.
 *
 * JSqlParser remains the parser for individual supported statements. This extractor is intentionally narrow: it exists
 * because parsing the whole PostgreSQL script with CCJSqlParserUtil.parseStatements(...) does not handle the current
 * mix of PostgreSQL-specific statements and dollar-quoted PL/pgSQL bodies reliably enough for this generator.
 */
public class SqlStatementExtractor {

    /**
     * Reads a SQL file and extracts top-level statements.
     */
    List<String> extract(Path sqlFile) throws IOException {
        return extract(Files.readString(sqlFile, StandardCharsets.UTF_8));
    }

    /**
     * Extracts top-level statements from SQL text.
     */
    List<String> extract(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        SplitState state = new SplitState();

        int index = 0;
        while (index < sql.length()) {
            char c = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';

            int nextIndex = consumeSpecialState(sql, current, state, index, c, next);
            if (nextIndex >= 0) {
                index = nextIndex + 1;
                continue;
            }

            nextIndex = startSpecialState(sql, current, state, index, c, next);
            if (nextIndex >= 0) {
                index = nextIndex + 1;
                continue;
            }

            if (updateQuoteStateAndSplit(statements, current, state, c)) {
                index++;
                continue;
            }

            current.append(c);
            index++;
        }

        addStatement(statements, current);
        return statements;
    }

    private int consumeSpecialState(
            String sql, StringBuilder current, SplitState state, int index, char c, char next) {
        if (state.inLineComment) {
            current.append(c);
            if (c == '\n') {
                state.inLineComment = false;
            }
            return index;
        }

        if (state.inBlockComment) {
            current.append(c);
            if (c == '*' && next == '/') {
                current.append(next);
                state.inBlockComment = false;
                return index + 1;
            }
            return index;
        }

        if (state.dollarQuoteTag != null) {
            if (sql.startsWith(state.dollarQuoteTag, index)) {
                current.append(state.dollarQuoteTag);
                int consumedIndex = index + state.dollarQuoteTag.length() - 1;
                state.dollarQuoteTag = null;
                return consumedIndex;
            }

            current.append(c);
            return index;
        }

        return -1;
    }

    private int startSpecialState(
            String sql, StringBuilder current, SplitState state, int index, char c, char next) {
        if (!state.inSingleQuote && !state.inDoubleQuote && c == '-' && next == '-') {
            current.append(c).append(next);
            state.inLineComment = true;
            return index + 1;
        }

        if (!state.inSingleQuote && !state.inDoubleQuote && c == '/' && next == '*') {
            current.append(c).append(next);
            state.inBlockComment = true;
            return index + 1;
        }

        if (!state.inSingleQuote && !state.inDoubleQuote && c == '$') {
            String tag = findDollarQuoteTag(sql, index);
            if (tag != null) {
                current.append(tag);
                state.dollarQuoteTag = tag;
                return index + tag.length() - 1;
            }
        }

        return -1;
    }

    private boolean updateQuoteStateAndSplit(
            List<String> statements, StringBuilder current, SplitState state, char c) {
        if (!state.inDoubleQuote && c == '\'') {
            state.inSingleQuote = !state.inSingleQuote;
            return false;
        }

        if (!state.inSingleQuote && c == '"') {
            state.inDoubleQuote = !state.inDoubleQuote;
            return false;
        }

        if (!state.inSingleQuote && !state.inDoubleQuote && c == ';') {
            addStatement(statements, current);
            return true;
        }

        return false;
    }

    /**
     * Removes leading SQL comments before statement type detection.
     */
    String stripLeadingComments(String statement) {
        String stripped = statement.stripLeading();
        boolean changed;

        do {
            changed = false;
            if (stripped.startsWith("--")) {
                int lineEnd = stripped.indexOf('\n');
                stripped = lineEnd >= 0 ? stripped.substring(lineEnd + 1).stripLeading() : "";
                changed = true;
            } else if (stripped.startsWith("/*")) {
                int commentEnd = stripped.indexOf("*/");
                stripped = commentEnd >= 0 ? stripped.substring(commentEnd + 2).stripLeading() : "";
                changed = true;
            }
        } while (changed);

        return stripped;
    }

    private String findDollarQuoteTag(String sql, int start) {
        int end = sql.indexOf('$', start + 1);
        if (end < 0) {
            return null;
        }

        String tagBody = sql.substring(start + 1, end);
        if (!tagBody.matches("[A-Za-z_][A-Za-z0-9_]*|")) {
            return null;
        }

        return sql.substring(start, end + 1);
    }

    private void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
        current.setLength(0);
    }

    /**
     * Current quote/comment state of the lightweight statement splitter.
     */
    private static final class SplitState {
        private String dollarQuoteTag;
        private boolean inSingleQuote;
        private boolean inDoubleQuote;
        private boolean inLineComment;
        private boolean inBlockComment;
    }
}
