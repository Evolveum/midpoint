/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser.annotations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.tools.dbdocs.model.DocMetadata;
import com.evolveum.midpoint.tools.dbdocs.model.UpgradeAffectedObjectDoc;

/**
 * Extracts supported documentation annotations from SQL comments.
 */
public class SqlAnnotationExtractor {

    private static final String SCRIPT_DESCRIPTION = "@script-description:";
    private static final String ANNOTATION_PREFIX = "@";
    private static final char ANNOTATION_SEPARATOR = ':';
    private static final char AFFECTS_SEPARATOR = '|';
    private static final char QUOTE = '"';

    private static final Set<String> TABLE_CONSTRAINT_STARTS = Set.of(
            "CHECK", "CONSTRAINT", "PRIMARY", "FOREIGN", "UNIQUE");
    private static final Set<AnnotationKey> MULTI_LINE_KEYS = Set.of(
            AnnotationKey.DESCRIPTION, AnnotationKey.CHANGE, AnnotationKey.REGION_DESCRIPTION);

    /**
     * Extracts annotation metadata placed immediately before a SQL statement.
     */
    public DocMetadata extractLeadingMetadata(String statement) {
        AnnotationMetadataBuilder builder = new AnnotationMetadataBuilder();
        for (SqlAnnotation annotation : leadingAnnotations(statement)) {
            builder.add(annotation);
        }
        return builder.isEmpty() ? DocMetadata.EMPTY : builder.toMetadata();
    }

    /**
     * Extracts the file-level script description from a leading SQL block comment.
     */
    public String extractScriptDescription(String statement) {
        String description = null;
        boolean inScriptDescription = false;
        boolean inBlockComment = false;

        for (String line : statement.lines().toList()) {
            String strippedLine = line.stripLeading();
            if (!inBlockComment && !SqlCommentSupport.startsBlockComment(strippedLine)) {
                if (strippedLine.isBlank() || SqlCommentSupport.isLineComment(strippedLine)) {
                    continue;
                }
                break;
            }

            String commentText = SqlCommentSupport.fromBlockComment(line);
            boolean blockContinues = SqlCommentSupport.continuesBlockComment(strippedLine);
            inBlockComment = blockContinues;
            if (startsScriptDescription(commentText)) {
                description = commentText.substring(SCRIPT_DESCRIPTION.length()).strip();
                inScriptDescription = true;
            } else if (inScriptDescription && (!commentText.isBlank() || blockContinues)) {
                description = SqlCommentSupport.appendContinuation(description, commentText);
            }

            if (!inBlockComment && inScriptDescription) {
                break;
            }
        }

        return description;
    }

    /**
     * Extracts repeated {@code @affects} annotations from an upgrade change annotation block.
     */
    public List<UpgradeAffectedObjectDoc> extractAffectedObjects(String statement) {
        List<UpgradeAffectedObjectDoc> affectedObjects = new ArrayList<>();
        for (SqlAnnotation annotation : leadingAnnotations(statement)) {
            if (annotation.key() == AnnotationKey.AFFECTS) {
                UpgradeAffectedObjectDoc affectedObject = affectedObject(annotation.value());
                if (affectedObject != null) {
                    affectedObjects.add(affectedObject);
                }
            }
        }
        return affectedObjects;
    }

    /**
     * Extracts column-level annotation metadata from a CREATE TABLE statement.
     *
     * JSqlParser does not preserve preceding SQL comments on ColumnDefinition objects, so column annotations are
     * attached in this pre-processing step before CREATE TABLE is parsed.
     *
     * Normal developer comments between a column annotation and the column reset the pending annotation block.
     */
    public Map<String, DocMetadata> extractColumnMetadata(String statement) {
        Map<String, DocMetadata> metadataByColumn = new LinkedHashMap<>();
        AnnotationMetadataBuilder pendingMetadata = new AnnotationMetadataBuilder();
        AnnotationMetadataBuilder blockMetadata = null;
        boolean inBlockComment = false;

        for (String line : statement.lines().toList()) {
            String strippedLine = line.stripLeading();

            if (inBlockComment) {
                collectBlockAnnotation(blockMetadata, line);
                inBlockComment = SqlCommentSupport.continuesBlockComment(strippedLine);
                if (!inBlockComment) {
                    pendingMetadata = blockMetadata.isEmpty() ? new AnnotationMetadataBuilder() : blockMetadata;
                    blockMetadata = null;
                }
                continue;
            }

            SqlAnnotation annotation = annotationFromLine(line);
            if (annotation != null) {
                pendingMetadata.add(annotation);
                continue;
            }

            if (line.isBlank()) {
                continue;
            }

            if (SqlCommentSupport.isLineComment(strippedLine)) {
                pendingMetadata = new AnnotationMetadataBuilder();
                continue;
            }

            if (SqlCommentSupport.startsBlockComment(strippedLine)) {
                blockMetadata = new AnnotationMetadataBuilder();
                collectBlockAnnotation(blockMetadata, line);
                inBlockComment = SqlCommentSupport.continuesBlockComment(strippedLine);
                if (!inBlockComment) {
                    pendingMetadata = blockMetadata.isEmpty() ? new AnnotationMetadataBuilder() : blockMetadata;
                    blockMetadata = null;
                }
                continue;
            }

            if (!pendingMetadata.isEmpty()) {
                addColumnMetadata(metadataByColumn, pendingMetadata, line);
                pendingMetadata = new AnnotationMetadataBuilder();
            }
        }

        return metadataByColumn;
    }

    private void addColumnMetadata(
            Map<String, DocMetadata> metadataByColumn, AnnotationMetadataBuilder pendingMetadata, String line) {
        String columnName = columnName(line);
        if (columnName != null && !isTableConstraint(columnName)) {
            metadataByColumn.put(columnName, pendingMetadata.toMetadata());
        }
    }

    private boolean isTableConstraint(String token) {
        return TABLE_CONSTRAINT_STARTS.contains(token.toUpperCase(Locale.ROOT));
    }

    /**
     * Parses leading statement annotations.
     *
     * Normal developer comments between annotations and the SQL statement reset the annotation block.
     * This keeps object annotations attached only when they are placed directly before the object.
     */
    private List<SqlAnnotation> leadingAnnotations(String statement) {
        LeadingAnnotationCollector collector = new LeadingAnnotationCollector();

        for (String line : statement.lines().toList()) {
            if (collector.process(line) == LineResult.STOP) {
                break;
            }
        }

        return collector.annotations();
    }

    /**
     * Parses an @affects value in the form:
     * object-kind object-name | change type | description
     */
    private UpgradeAffectedObjectDoc affectedObject(String value) {
        int firstSeparator = value.indexOf(AFFECTS_SEPARATOR);
        if (firstSeparator < 0) {
            return null;
        }

        int secondSeparator = value.indexOf(AFFECTS_SEPARATOR, firstSeparator + 1);
        if (secondSeparator < 0) {
            return null;
        }

        String objectExpression = value.substring(0, firstSeparator).strip();
        String changeType = value.substring(firstSeparator + 1, secondSeparator).strip();
        String description = value.substring(secondSeparator + 1).strip();
        if (objectExpression.isEmpty() || changeType.isEmpty() || description.isEmpty()) {
            return null;
        }

        return new UpgradeAffectedObjectDoc(objectName(objectExpression), changeType, description);
    }

    private String objectName(String objectExpression) {
        int firstWhitespace = firstWhitespace(objectExpression);
        if (firstWhitespace < 0) {
            return objectExpression;
        }

        String name = objectExpression.substring(firstWhitespace).strip();
        return name.isEmpty() ? objectExpression : name;
    }

    private int firstWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean startsScriptDescription(String commentText) {
        return commentText.regionMatches(true, 0, SCRIPT_DESCRIPTION, 0, SCRIPT_DESCRIPTION.length());
    }

    private void collectBlockAnnotation(AnnotationMetadataBuilder builder, String line) {
        String commentText = SqlCommentSupport.fromBlockComment(line);
        if (commentText.isBlank() && !SqlCommentSupport.continuesBlockComment(line.stripLeading())) {
            return;
        }

        SqlAnnotation annotation = annotationFromCommentText(commentText);
        if (annotation != null) {
            builder.add(annotation);
        } else {
            builder.appendToLast(commentText);
        }
    }

    /**
     * Parses one SQL line comment in the form {@code -- @key: value}.
     */
    private SqlAnnotation annotationFromLine(String line) {
        String commentText = SqlCommentSupport.fromLineComment(line);
        if (commentText == null || !commentText.startsWith(ANNOTATION_PREFIX)) {
            return null;
        }

        int separator = commentText.indexOf(ANNOTATION_SEPARATOR);
        if (separator < 0) {
            return null;
        }

        AnnotationKey key = AnnotationKey.from(commentText.substring(1, separator).trim());
        if (key == null) {
            return null;
        }

        return new SqlAnnotation(key, commentText.substring(separator + 1).strip());
    }

    private SqlAnnotation annotationFromCommentText(String commentText) {
        return annotationFromLine(SqlCommentSupport.asLineComment(commentText));
    }

    /**
     * Reads the first identifier from a potential column definition line.
     */
    private String columnName(String line) {
        String strippedLine = line.stripLeading();
        if (strippedLine.isEmpty()) {
            return null;
        }

        if (strippedLine.charAt(0) == QUOTE) {
            int closingQuote = strippedLine.indexOf(QUOTE, 1);
            return closingQuote > 1 ? strippedLine.substring(1, closingQuote) : null;
        }

        int end = identifierEnd(strippedLine);
        return end > 0 ? strippedLine.substring(0, end) : null;
    }

    private int identifierEnd(String value) {
        int end = 0;
        while (end < value.length() && isIdentifierCharacter(value.charAt(end))) {
            end++;
        }
        return end;
    }

    private boolean isIdentifierCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private enum LineResult {
        CONTINUE,
        STOP
    }

    /**
     * Collects one leading annotation block before a SQL statement.
     *
     * The collector stops when SQL starts. If a normal comment appears after an annotation block,
     * the block is discarded because the annotation is no longer directly attached to the statement.
     */
    private final class LeadingAnnotationCollector {

        private AnnotationMetadataBuilder builder = new AnnotationMetadataBuilder();
        private AnnotationMetadataBuilder blockBuilder;
        private boolean inAnnotationBlock;
        private boolean inBlockComment;

        private LineResult process(String line) {
            String strippedLine = line.stripLeading();

            if (inBlockComment) {
                collectBlockAnnotation(blockBuilder, line);
                inBlockComment = SqlCommentSupport.continuesBlockComment(strippedLine);
                if (!inBlockComment) {
                    finishBlockComment();
                }
                return LineResult.CONTINUE;
            }

            SqlAnnotation annotation = annotationFromLine(line);
            if (annotation != null) {
                builder.add(annotation);
                inAnnotationBlock = true;
                return LineResult.CONTINUE;
            }

            if (line.isBlank()) {
                return inAnnotationBlock ? LineResult.STOP : LineResult.CONTINUE;
            }

            if (SqlCommentSupport.isLineComment(strippedLine)) {
                resetAnnotationBlock();
                return LineResult.CONTINUE;
            }

            if (SqlCommentSupport.startsBlockComment(strippedLine)) {
                blockBuilder = new AnnotationMetadataBuilder();
                collectBlockAnnotation(blockBuilder, line);
                inBlockComment = SqlCommentSupport.continuesBlockComment(strippedLine);
                if (!inBlockComment) {
                    finishBlockComment();
                }
                return LineResult.CONTINUE;
            }

            return LineResult.STOP;
        }

        private void finishBlockComment() {
            if (blockBuilder.isEmpty()) {
                resetAnnotationBlock();
            } else {
                builder = blockBuilder;
                inAnnotationBlock = true;
            }
            blockBuilder = null;
        }

        private void resetAnnotationBlock() {
            if (inAnnotationBlock) {
                builder = new AnnotationMetadataBuilder();
                inAnnotationBlock = false;
            }
        }

        private List<SqlAnnotation> annotations() {
            return builder.annotations();
        }
    }

    private record SqlAnnotation(AnnotationKey key, String value) {}

    /**
     * Collects parsed annotations and converts the supported metadata annotations to DocMetadata.
     */
    private static class AnnotationMetadataBuilder {

        private final List<SqlAnnotation> annotations = new ArrayList<>();

        void add(SqlAnnotation annotation) {
            annotations.add(annotation);
        }

        void appendToLast(String continuation) {
            if (annotations.isEmpty()) {
                return;
            }

            SqlAnnotation lastAnnotation = annotations.get(annotations.size() - 1);
            if (!MULTI_LINE_KEYS.contains(lastAnnotation.key())) {
                return;
            }

            annotations.set(
                    annotations.size() - 1,
                    new SqlAnnotation(lastAnnotation.key(), SqlCommentSupport.appendContinuation(lastAnnotation.value(), continuation)));
        }

        boolean isEmpty() {
            return annotations.isEmpty();
        }

        List<SqlAnnotation> annotations() {
            return List.copyOf(annotations);
        }

        DocMetadata toMetadata() {
            String description = null;
            String type = null;
            String since = null;
            String usedFor = null;
            String change = null;
            String regionSlug = null;
            String regionTitle = null;
            String regionDescription = null;

            for (SqlAnnotation annotation : annotations) {
                switch (annotation.key()) {
                    case DESCRIPTION -> description = annotation.value();
                    case TYPE -> type = annotation.value();
                    case SINCE -> since = annotation.value();
                    case USED_FOR -> usedFor = annotation.value();
                    case CHANGE -> change = annotation.value();
                    case REGION -> regionSlug = annotation.value();
                    case REGION_TITLE -> regionTitle = annotation.value();
                    case REGION_DESCRIPTION -> regionDescription = annotation.value();
                    case AFFECTS -> {
                        // @affects is handled separately for upgrade change details.
                    }
                }
            }

            return new DocMetadata(description, type, since, usedFor, change, regionSlug, regionTitle, regionDescription);
        }
    }
}
