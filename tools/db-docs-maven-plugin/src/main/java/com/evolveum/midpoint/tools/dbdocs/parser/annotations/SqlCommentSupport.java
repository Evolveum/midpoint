/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser.annotations;

/**
 * Extracts normalized text from SQL line and block comments.
 */
final class SqlCommentSupport {

    private static final String LINE_COMMENT_PREFIX = "--";
    private static final String BLOCK_COMMENT_START = "/*";
    private static final String BLOCK_COMMENT_END = "*/";

    private SqlCommentSupport() {
    }

    static boolean isLineComment(String strippedLine) {
        return strippedLine.startsWith(LINE_COMMENT_PREFIX);
    }

    static boolean startsBlockComment(String strippedLine) {
        return strippedLine.startsWith(BLOCK_COMMENT_START);
    }

    static boolean continuesBlockComment(String strippedLine) {
        return !strippedLine.contains(BLOCK_COMMENT_END);
    }

    static String asLineComment(String commentText) {
        return LINE_COMMENT_PREFIX + " " + commentText;
    }

    static String fromLineComment(String line) {
        String strippedLine = line.stripLeading();
        if (!isLineComment(strippedLine)) {
            return null;
        }

        return strippedLine.substring(LINE_COMMENT_PREFIX.length()).stripLeading();
    }

    static String fromBlockComment(String line) {
        String text = line.stripLeading();
        if (startsBlockComment(text)) {
            text = text.substring(BLOCK_COMMENT_START.length());
        }

        int end = text.indexOf(BLOCK_COMMENT_END);
        if (end >= 0) {
            text = text.substring(0, end);
        }

        String stripped = text.stripLeading();
        if (stripped.startsWith("*")) {
            return stripped.substring(1).stripLeading();
        }
        return stripped;
    }

    static String appendContinuation(String value, String continuation) {
        String normalizedContinuation = continuation.stripTrailing();
        if (value == null || value.isBlank()) {
            return normalizedContinuation;
        }
        return value + "\n" + normalizedContinuation;
    }
}
