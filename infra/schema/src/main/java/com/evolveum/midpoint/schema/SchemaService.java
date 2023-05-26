/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Aggregation of various schema and prism managed components for convenience.
 * The purpose is rather practical, to avoid too many injections.
 * Most used methods are provided directly.
 */
@Component
public class SchemaService {

    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;

    private static SchemaService instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    @VisibleForTesting
    public static void init(
            PrismContext prismContext, RelationRegistry relationRegistry, MatchingRuleRegistry matchingRuleRegistry) {
        SchemaService newInstance = new SchemaService();
        newInstance.prismContext = prismContext;
        newInstance.relationRegistry = relationRegistry;
        newInstance.matchingRuleRegistry = matchingRuleRegistry;
        newInstance.init();
    }

    public static SchemaService get() {
        return instance;
    }

    public PrismContext prismContext() {
        return prismContext;
    }

    public RelationRegistry relationRegistry() {
        return relationRegistry;
    }

    public MatchingRuleRegistry matchingRuleRegistry() {
        return matchingRuleRegistry;
    }

    public GetOperationOptionsBuilder getOperationOptionsBuilder() {
        return new GetOperationOptionsBuilderImpl(prismContext);
    }

    @NotNull
    public PrismSerializer<String> createStringSerializer(@NotNull String language) {
        return prismContext.serializerFor(language);
    }

    @NotNull
    public PrismParserNoIO parserFor(@NotNull String serializedForm) {
        return prismContext.parserFor(serializedForm);
    }

    public CanonicalItemPath createCanonicalItemPath(ItemPath path, QName objectType) {
        return prismContext.createCanonicalItemPath(path, objectType);
    }

    public <T> Class<? extends T> typeQNameToSchemaClass(QName qName) {
        return prismContext.getSchemaRegistry().determineClassForTypeRequired(qName);
    }

    public QName schemaClassToTypeQName(Class<?> schemaClass) {
        return prismContext.getSchemaRegistry().determineTypeForClassRequired(schemaClass);
    }

    public @NotNull QName normalizeRelation(QName qName) {
        return relationRegistry.normalizeRelation(qName);
    }

    @NotNull
    public PrismReferenceValue createReferenceValue(
            @NotNull String oid, @NotNull Class<? extends ObjectType> schemaType) {
        return prismContext.itemFactory().createReferenceValue(oid,
                prismContext.getSchemaRegistry().determineTypeForClass(schemaType));

    }

    public <C extends Containerable> PrismContainerDefinition<C>
    findContainerDefinitionByCompileTimeClass(Class<C> containerableType) {
        return prismContext.getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(containerableType);
    }
}
