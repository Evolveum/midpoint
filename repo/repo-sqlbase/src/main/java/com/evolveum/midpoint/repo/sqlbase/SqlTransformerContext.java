/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Holds various component dependencies that are used during schema to DB transformations.
 * Components can be obtained to execute calls on them, but preferably the needed logic
 * can be implemented here (better abstraction).
 */
public class SqlTransformerContext {

    private final PrismContext prismContext;
    private final RelationRegistry relationRegistry;

    public SqlTransformerContext(PrismContext prismContext,
            RelationRegistry relationRegistry) {
        this.prismContext = prismContext;
        this.relationRegistry = relationRegistry;
    }

    public <T> Class<? extends T> qNameToSchemaClass(QName qName) {
        return prismContext.getSchemaRegistry().determineClassForTypeRequired(qName);
    }

    public QName schemaClassToQName(Class<?> schemaClass) {
        return prismContext.getSchemaRegistry().determineTypeForClassRequired(schemaClass);
    }

    public QName normalizeRelation(QName qName) {
        return relationRegistry.normalizeRelation(qName);
    }

    @NotNull
    public PrismSerializer<String> serializer(SqlRepoContext sqlRepoContext) {
        return prismContext.serializerFor(
                sqlRepoContext.getJdbcRepositoryConfiguration().getFullObjectFormat());
    }

    public <T extends Objectable> ParseResult<T> parsePrismObject(String serializedForm)
            throws SchemaException {
        // "Postel mode": be tolerant what you read. We need this to tolerate (custom) schema changes
        ParsingContext parsingContext = prismContext.createParsingContextForCompatibilityMode();
        PrismObject<T> prismObject = prismContext.parserFor(serializedForm)
                .context(parsingContext).parse();
        return new ParseResult<>(parsingContext, prismObject);
    }

    public <T> T parseRealValue(String serializedResult, Class<T> clazz) throws SchemaException {
        return prismContext.parserFor(serializedResult).parseRealValue(clazz);
    }

    /**
     * Sometimes delegation is not enough - we need Prism context for schema type construction
     * with definitions (parameter to constructor).
     */
    public PrismContext prismContext() {
        return prismContext;
    }

    public static class ParseResult<T extends Objectable> {
        public final ParsingContext parsingContext;
        public final PrismObject<T> prismObject;

        public ParseResult(ParsingContext parsingContext, PrismObject<T> prismObject) {
            this.parsingContext = parsingContext;
            this.prismObject = prismObject;
        }
    }
}
