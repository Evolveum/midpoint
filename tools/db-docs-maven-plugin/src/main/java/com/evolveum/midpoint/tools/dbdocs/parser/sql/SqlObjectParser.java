/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser.sql;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.evolveum.midpoint.tools.dbdocs.model.DocMetadata;
import com.evolveum.midpoint.tools.dbdocs.model.DocRegion;
import com.evolveum.midpoint.tools.dbdocs.model.SqlObjectDoc;
import com.evolveum.midpoint.tools.dbdocs.parser.SqlParserSupport;

import net.sf.jsqlparser.statement.create.function.CreateFunction;
import net.sf.jsqlparser.statement.create.procedure.CreateProcedure;
import net.sf.jsqlparser.statement.create.schema.CreateSchema;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.Statement;

/**
 * Converts JSqlParser-supported non-table schema object statements into documentation models.
 *
 * PostgreSQL objects not exposed by JSqlParser as structured AST objects are handled by
 * PostgresStatementFallbackParser.
 */
public class SqlObjectParser {

    /**
     * Tries to parse a non-table schema object statement using only JSqlParser's structured AST objects.
     */
    public Optional<SqlObjectDoc> parseIfSupported(String statement, Path sourceFile, DocRegion region, DocMetadata metadata) {
        return SqlParserSupport.parseStatement(statement)
                .flatMap(parsedStatement -> parse(parsedStatement, sourceFile, region, metadata));
    }

    private Optional<SqlObjectDoc> parse(
            Statement statement, Path sourceFile, DocRegion region, DocMetadata metadata) {
        if (statement instanceof CreateView createView) {
            return Optional.of(parseView(createView, sourceFile, region, metadata));
        } else if (statement instanceof CreateFunction createFunction) {
            return Optional.of(parseFunction(createFunction, sourceFile, region, metadata));
        } else if (statement instanceof CreateProcedure createProcedure) {
            return Optional.of(parseProcedure(createProcedure, sourceFile, region, metadata));
        } else if (statement instanceof CreateSchema createSchema) {
            return Optional.of(parseSchema(createSchema, sourceFile, region, metadata));
        }
        return Optional.empty();
    }

    private SqlObjectDoc parseView(CreateView createView, Path sourceFile, DocRegion region, DocMetadata metadata) {
        SqlObjectDoc.Kind kind = createView.isMaterialized()
                ? SqlObjectDoc.Kind.MATERIALIZED_VIEW
                : SqlObjectDoc.Kind.VIEW;
        return new SqlObjectDoc(
                SqlParserSupport.normalizeIdentifier(createView.getView().getFullyQualifiedName()),
                sourceFile,
                region,
                kind,
                metadata,
                Map.of(),
                List.of());
    }

    private SqlObjectDoc parseFunction(CreateFunction createFunction, Path sourceFile, DocRegion region, DocMetadata metadata) {
        return new SqlObjectDoc(
                SqlParserSupport.normalizeIdentifier(routineName(createFunction.getFunctionDeclarationParts())),
                sourceFile,
                region,
                SqlObjectDoc.Kind.FUNCTION,
                metadata,
                Map.of(),
                List.of());
    }

    private SqlObjectDoc parseProcedure(CreateProcedure createProcedure, Path sourceFile, DocRegion region, DocMetadata metadata) {
        return new SqlObjectDoc(
                SqlParserSupport.normalizeIdentifier(routineName(createProcedure.getFunctionDeclarationParts())),
                sourceFile,
                region,
                SqlObjectDoc.Kind.PROCEDURE,
                metadata,
                Map.of(),
                List.of());
    }

    private SqlObjectDoc parseSchema(CreateSchema createSchema, Path sourceFile, DocRegion region, DocMetadata metadata) {
        String name = createSchema.getSchemaName() != null
                ? createSchema.getSchemaName()
                : createSchema.getAuthorization();
        return new SqlObjectDoc(
                SqlParserSupport.normalizeIdentifier(name),
                sourceFile,
                region,
                SqlObjectDoc.Kind.SCHEMA,
                metadata,
                Map.of(),
                List.of());
    }

    /**
     * JSqlParser returns routine declaration parts with the routine name as the first item.
     */
    private String routineName(List<String> declarationParts) {
        if (declarationParts == null || declarationParts.isEmpty()) {
            return "";
        }

        return declarationParts.get(0);
    }
}
