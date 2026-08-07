/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser.postgres;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evolveum.midpoint.tools.dbdocs.model.DocMetadata;
import com.evolveum.midpoint.tools.dbdocs.model.DocRegion;
import com.evolveum.midpoint.tools.dbdocs.model.SqlObjectDoc;
import static com.evolveum.midpoint.tools.dbdocs.parser.SqlParserSupport.normalizeIdentifier;

/**
 * Contains narrow PostgreSQL compatibility fallbacks for statements that are not exposed by JSqlParser 5.3 as
 * structured AST objects. JSqlParser remains the primary parser. These fallbacks should be removed or reduced if a
 * future stable JSqlParser version supports these statements.
 */
public class PostgresStatementFallbackParser {

    private static final Pattern CREATE_ENUM = Pattern.compile(
            "(?is)^CREATE\\s+TYPE\\s+([^\\s]+)\\s+AS\\s+ENUM\\s*\\((.*)\\)\\s*$");
    private static final Pattern ENUM_VALUE = Pattern.compile("'([^']*)'");
    private static final Pattern CREATE_EXTENSION = Pattern.compile(
            "(?is)^CREATE\\s+EXTENSION\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([^\\s;]+).*");
    private static final Pattern CREATE_TRIGGER = Pattern.compile(
            "(?is)^CREATE\\s+TRIGGER\\s+([^\\s]+).*?\\s+ON\\s+([^\\s]+).*?"
                    + "EXECUTE\\s+(?:FUNCTION|PROCEDURE)\\s+([^\\s(]+).*");

    /**
     * Tries to parse one of the PostgreSQL statements that JSqlParser 5.3 does not expose as a structured AST object.
     */
    public Optional<SqlObjectDoc> parseIfSupported(String statement, Path sourceFile, DocRegion region, DocMetadata metadata) {
        Matcher enumMatcher = CREATE_ENUM.matcher(statement);
        if (enumMatcher.matches()) {
            return Optional.of(parseEnumType(enumMatcher, sourceFile, region, metadata));
        }

        Matcher extensionMatcher = CREATE_EXTENSION.matcher(statement);
        if (extensionMatcher.matches()) {
            return Optional.of(parseExtension(extensionMatcher, sourceFile, region, metadata));
        }

        Matcher triggerMatcher = CREATE_TRIGGER.matcher(statement);
        if (triggerMatcher.matches()) {
            return Optional.of(parseTrigger(triggerMatcher, sourceFile, region, metadata));
        }

        return Optional.empty();
    }

    private SqlObjectDoc parseEnumType(Matcher matcher, Path sourceFile, DocRegion region, DocMetadata metadata) {
        List<String> values = new ArrayList<>();
        Matcher valueMatcher = ENUM_VALUE.matcher(matcher.group(2));
        while (valueMatcher.find()) {
            values.add(valueMatcher.group(1));
        }

        return new SqlObjectDoc(
                normalizeIdentifier(matcher.group(1)),
                sourceFile,
                region,
                SqlObjectDoc.Kind.ENUM_TYPE,
                metadata,
                Map.of(),
                values);
    }

    private SqlObjectDoc parseExtension(Matcher matcher, Path sourceFile, DocRegion region, DocMetadata metadata) {
        return new SqlObjectDoc(
                normalizeIdentifier(matcher.group(1)),
                sourceFile,
                region,
                SqlObjectDoc.Kind.EXTENSION,
                metadata,
                Map.of(),
                List.of());
    }

    private SqlObjectDoc parseTrigger(Matcher matcher, Path sourceFile, DocRegion region, DocMetadata metadata) {
        return new SqlObjectDoc(
                normalizeIdentifier(matcher.group(1)),
                sourceFile,
                region,
                SqlObjectDoc.Kind.TRIGGER,
                metadata,
                Map.of(
                        SqlObjectDoc.DETAIL_TABLE, normalizeIdentifier(matcher.group(2)),
                        SqlObjectDoc.DETAIL_EXECUTES, normalizeIdentifier(matcher.group(3))),
                List.of());
    }

}
