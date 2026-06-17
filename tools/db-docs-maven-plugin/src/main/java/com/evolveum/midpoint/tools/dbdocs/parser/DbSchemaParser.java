/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.evolveum.midpoint.tools.dbdocs.model.DocMetadata;
import com.evolveum.midpoint.tools.dbdocs.model.DocRegion;
import com.evolveum.midpoint.tools.dbdocs.model.IndexDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SchemaDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SqlFileDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SqlObjectDoc;
import com.evolveum.midpoint.tools.dbdocs.model.TableDoc;
import com.evolveum.midpoint.tools.dbdocs.parser.annotations.RegionTracker;
import com.evolveum.midpoint.tools.dbdocs.parser.annotations.SqlAnnotationExtractor;
import com.evolveum.midpoint.tools.dbdocs.parser.postgres.PostgresStatementFallbackParser;
import com.evolveum.midpoint.tools.dbdocs.parser.sql.SqlAlterTableParser;
import com.evolveum.midpoint.tools.dbdocs.parser.sql.SqlIndexParser;
import com.evolveum.midpoint.tools.dbdocs.parser.sql.SqlObjectParser;
import com.evolveum.midpoint.tools.dbdocs.parser.sql.SqlTableParser;

/**
 * Orchestrates parsing of configured SQL files into one schema documentation model.
 *
 * The parser extracts statements, reads documentation annotations, tracks documentation regions,
 * and delegates actual SQL parsing to specialized parsers.
 */
public class DbSchemaParser {

    private static final String DO_BLOCK_PREFIX = "DO";
    private static final String ALTER_TABLE_PREFIX = "ALTER TABLE";
    private static final String DO_BLOCK_NAME_PREFIX = "do-block-";
    private static final String ALTER_TABLE_NAME_PREFIX = "alter-table-";

    private final SqlStatementExtractor statementExtractor = new SqlStatementExtractor();
    private final SqlAnnotationExtractor annotationExtractor = new SqlAnnotationExtractor();
    private final UpgradeChangeParser upgradeChangeParser = new UpgradeChangeParser(annotationExtractor);
    private final SqlTableParser tableParser = new SqlTableParser();
    private final SqlAlterTableParser alterTableParser = new SqlAlterTableParser();
    private final SqlIndexParser indexParser = new SqlIndexParser();
    private final SqlObjectParser objectParser = new SqlObjectParser();
    private final PostgresStatementFallbackParser postgresFallbackParser =
            new PostgresStatementFallbackParser();

    /**
     * Parses SQL files and returns one schema documentation model.
     */
    public SchemaDoc parse(List<Path> sqlFiles) throws IOException {
        List<SqlFileDoc> sourceFiles = new ArrayList<>();
        for (Path sqlFile : sqlFiles) {
            sourceFiles.add(new SqlFileDoc(sqlFile, SqlFileDoc.category(sqlFile)));
        }

        return parseSourceFiles(sourceFiles);
    }

    private SchemaDoc parseSourceFiles(List<SqlFileDoc> sourceFiles) throws IOException {
        ParsedSchemaAccumulator accumulator = new ParsedSchemaAccumulator();

        for (SqlFileDoc sourceFile : sourceFiles) {
            parseSourceFile(sourceFile, accumulator);
        }

        return accumulator.toSchemaDoc(sourceFiles);
    }

    private void parseSourceFile(SqlFileDoc sourceFile, ParsedSchemaAccumulator accumulator) throws IOException {
        RegionTracker regionTracker = new RegionTracker();
        int doBlockNumber = 1;
        int alterTableNumber = 1;

        for (String statement : statementExtractor.extract(sourceFile.path())) {
            String strippedStatement = statementExtractor.stripLeadingComments(statement);
            DocMetadata metadata = annotationExtractor.extractLeadingMetadata(statement);
            DocRegion currentRegion = regionTracker.regionFor(statement, metadata);

            if (sourceFile.category() == SqlFileDoc.Category.UPGRADE) {
                upgradeChangeParser.parse(sourceFile.path(), statement, strippedStatement, metadata)
                        .ifPresent(accumulator::addUpgradeChange);
                continue;
            }

            if (isDoBlock(strippedStatement)) {
                if (parseDoBlock(sourceFile, doBlockNumber, metadata, currentRegion, accumulator)) {
                    doBlockNumber++;
                }
                continue;
            }

            if (parseMergeableSchemaStatement(sourceFile, statement, strippedStatement, metadata, currentRegion, accumulator)) {
                continue;
            }

            if (isAlterTable(strippedStatement) && isDocumentedObject(metadata)) {
                parseDocumentedAlterTable(sourceFile, alterTableNumber, metadata, currentRegion, accumulator);
                alterTableNumber++;
                continue;
            }

            parseStandaloneObject(sourceFile, strippedStatement, metadata, currentRegion, accumulator);
        }
    }

    /**
     * Parses statements that are merged into table/schema documentation before standalone objects are considered.
     */
    private boolean parseMergeableSchemaStatement(
            SqlFileDoc sourceFile,
            String statement,
            String strippedStatement,
            DocMetadata metadata,
            DocRegion currentRegion,
            ParsedSchemaAccumulator accumulator) {
        return parseTable(sourceFile, statement, strippedStatement, metadata, currentRegion, accumulator)
                || parseAddedColumn(strippedStatement, metadata, accumulator)
                || parseAddedForeignKeys(strippedStatement, accumulator)
                || parseIndex(strippedStatement, metadata, accumulator);
    }

    private boolean parseTable(
            SqlFileDoc sourceFile,
            String statement,
            String strippedStatement,
            DocMetadata metadata,
            DocRegion currentRegion,
            ParsedSchemaAccumulator accumulator) {
        Optional<TableDoc> table = tableParser.parseIfSupported(
                strippedStatement,
                sourceFile.path(),
                currentRegion,
                metadata,
                annotationExtractor.extractColumnMetadata(statement));

        table.ifPresent(accumulator::addTable);
        return table.isPresent();
    }

    private boolean parseAddedColumn(
            String strippedStatement, DocMetadata metadata, ParsedSchemaAccumulator accumulator) {
        Optional<SqlAlterTableParser.AddedColumn> addedColumn =
                alterTableParser.parseAddedColumn(strippedStatement, metadata);
        addedColumn.ifPresent(accumulator::addColumn);
        return addedColumn.isPresent();
    }

    private boolean parseAddedForeignKeys(String strippedStatement, ParsedSchemaAccumulator accumulator) {
        Optional<SqlAlterTableParser.AddedForeignKeys> addedForeignKeys =
                alterTableParser.parseAddedForeignKeys(strippedStatement);
        addedForeignKeys.ifPresent(accumulator::addForeignKeys);
        return addedForeignKeys.isPresent();
    }

    private boolean parseIndex(
            String strippedStatement, DocMetadata metadata, ParsedSchemaAccumulator accumulator) {
        Optional<IndexDoc> index = indexParser.parseIfSupported(strippedStatement, metadata);
        index.ifPresent(accumulator::addIndex);
        return index.isPresent();
    }

    private boolean parseDoBlock(
            SqlFileDoc sourceFile,
            int doBlockNumber,
            DocMetadata metadata,
            DocRegion currentRegion,
            ParsedSchemaAccumulator accumulator) {
        if (!isDocumentedObject(metadata)) {
            return false;
        }

        // PL/pgSQL bodies are not parsed structurally; annotated DO blocks are documented as procedural blocks.
        accumulator.addSqlObject(new SqlObjectDoc(
                DO_BLOCK_NAME_PREFIX + doBlockNumber,
                sourceFile.path(),
                currentRegion,
                SqlObjectDoc.Kind.DO_BLOCK,
                metadata,
                Map.of(),
                List.of()));
        return true;
    }

    private void parseDocumentedAlterTable(
            SqlFileDoc sourceFile,
            int alterTableNumber,
            DocMetadata metadata,
            DocRegion currentRegion,
            ParsedSchemaAccumulator accumulator) {
        // Supported ALTER TABLE additions are merged above. Other annotated ALTER TABLE statements are documented
        // as fallback objects.
        accumulator.addSqlObject(new SqlObjectDoc(
                ALTER_TABLE_NAME_PREFIX + alterTableNumber,
                sourceFile.path(),
                currentRegion,
                SqlObjectDoc.Kind.ALTER_TABLE,
                metadata,
                Map.of(),
                List.of()));
    }

    private void parseStandaloneObject(
            SqlFileDoc sourceFile,
            String strippedStatement,
            DocMetadata metadata,
            DocRegion currentRegion,
            ParsedSchemaAccumulator accumulator) {
        Optional<SqlObjectDoc> sqlObject = objectParser.parseIfSupported(
                strippedStatement, sourceFile.path(), currentRegion, metadata);
        if (sqlObject.isEmpty()) {
            sqlObject = postgresFallbackParser.parseIfSupported(
                    strippedStatement, sourceFile.path(), currentRegion, metadata);
        }

        sqlObject.ifPresent(accumulator::addSqlObject);
    }

    private boolean isDocumentedObject(DocMetadata metadata) {
        return metadata.description() != null || metadata.since() != null || metadata.usedFor() != null;
    }

    private boolean isDoBlock(String statement) {
        return statement.stripLeading().regionMatches(true, 0, DO_BLOCK_PREFIX, 0, DO_BLOCK_PREFIX.length());
    }

    private boolean isAlterTable(String statement) {
        return statement.stripLeading().regionMatches(true, 0, ALTER_TABLE_PREFIX, 0, ALTER_TABLE_PREFIX.length());
    }

}
