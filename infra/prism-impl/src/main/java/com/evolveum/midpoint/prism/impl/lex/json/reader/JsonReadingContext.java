/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.reader;

import com.evolveum.midpoint.prism.TypeDefinition;
import com.evolveum.midpoint.prism.impl.ParsingContextImpl;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.lex.json.DefinitionContext;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.fasterxml.jackson.core.JsonParser;

import java.util.Optional;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

/**
 * TODO
 */
class JsonReadingContext {

    @NotNull final JsonParser parser;
    @NotNull final ParsingContextImpl prismParsingContext;
    @NotNull final LexicalProcessor.RootXNodeHandler objectHandler;
    @NotNull final AbstractReader.YamlTagResolver yamlTagResolver;

    private boolean aborted;
    private @NotNull SchemaRegistry schemaRegistry;

    JsonReadingContext(@NotNull JsonParser parser, @NotNull ParsingContextImpl prismParsingContext,
            @NotNull LexicalProcessor.RootXNodeHandler objectHandler, @NotNull AbstractReader.YamlTagResolver yamlTagResolver,
            @NotNull SchemaRegistry schemaRegistry) {
        this.parser = parser;
        this.prismParsingContext = prismParsingContext;
        this.objectHandler = objectHandler;
        this.yamlTagResolver = yamlTagResolver;
        this.schemaRegistry = schemaRegistry;
    }


    public boolean isAborted() {
        return aborted;
    }

    public void setAborted() {
        this.aborted = true;
    }

    String getPositionSuffix() {
        return String.valueOf(parser.getCurrentLocation());
    }

    @NotNull
    String getPositionSuffixIfPresent() {
        return " At: " + getPositionSuffix();
    }


    public Optional<TypeDefinition> resolveType(QName typeName) {
        return Optional.ofNullable(schemaRegistry.findTypeDefinitionByType(typeName));
    }


    public DefinitionContext rootDefinition() {
        return DefinitionContext.root(schemaRegistry);
    }


    public @NotNull DefinitionContext replaceDefinition(@NotNull DefinitionContext definition, QName typeName) {
        TypeDefinition typeDef = schemaRegistry.findTypeDefinitionByType(typeName);
        return DefinitionContext.awareFromType(definition.getName(), typeDef);
    }
}
