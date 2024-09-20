/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.polystring.NormalizerRegistry;

import com.evolveum.midpoint.schema.processor.ResourceSchemaRegistry;

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
    @Autowired private NormalizerRegistry normalizerRegistry;
    @Autowired private ResourceSchemaRegistry resourceSchemaRegistry;

    private static SchemaService instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    @VisibleForTesting
    public static void init(
            @NotNull PrismContext prismContext,
            @NotNull RelationRegistry relationRegistry,
            @NotNull MatchingRuleRegistry matchingRuleRegistry,
            @NotNull NormalizerRegistry normalizerRegistry) {
        SchemaService newInstance = new SchemaService();
        newInstance.prismContext = prismContext;
        newInstance.relationRegistry = relationRegistry;
        newInstance.matchingRuleRegistry = matchingRuleRegistry;
        newInstance.normalizerRegistry = normalizerRegistry;
        newInstance.init();
    }

    public static SchemaService get() {
        return instance;
    }

    public @NotNull PrismContext prismContext() {
        return prismContext;
    }

    public @NotNull RelationRegistry relationRegistry() {
        return relationRegistry;
    }

    public @NotNull MatchingRuleRegistry matchingRuleRegistry() {
        return matchingRuleRegistry;
    }

    public @NotNull ResourceSchemaRegistry resourceSchemaRegistry() {
        return resourceSchemaRegistry;
    }

    public GetOperationOptionsBuilder getOperationOptionsBuilder() {
        return new GetOperationOptionsBuilderImpl(prismContext);
    }

    public @NotNull PrismSerializer<String> createStringSerializer(@NotNull String language) {
        return prismContext.serializerFor(language);
    }

    public @NotNull PrismParserNoIO parserFor(@NotNull String serializedForm) {
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

    public @NotNull PrismReferenceValue createReferenceValue(
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
