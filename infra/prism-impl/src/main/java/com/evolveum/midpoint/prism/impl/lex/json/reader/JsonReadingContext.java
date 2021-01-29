/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.reader;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.ParsingContextImpl;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.fasterxml.jackson.core.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;

/**
 * TODO
 */
class JsonReadingContext {

    @NotNull final JsonParser parser;
    @NotNull final ParsingContextImpl prismParsingContext;
    @NotNull final LexicalProcessor.RootXNodeHandler objectHandler;
    @NotNull final AbstractReader.YamlTagResolver yamlTagResolver;
    @NotNull final PrismContext prismContext;

    private boolean aborted;

    // Elements that should be skipped when filling-in default namespaces - those that are explicitly set with no-NS ('#name').
    // (Values for these entries are not important. Only key presence is relevant.)
    @NotNull final IdentityHashMap<XNodeImpl, Object> noNamespaceElementNames = new IdentityHashMap<>();

    JsonReadingContext(@NotNull JsonParser parser, @NotNull ParsingContextImpl prismParsingContext,
            @NotNull LexicalProcessor.RootXNodeHandler objectHandler, @NotNull AbstractReader.YamlTagResolver yamlTagResolver,
            @NotNull PrismContext prismContext) {
        this.parser = parser;
        this.prismParsingContext = prismParsingContext;
        this.objectHandler = objectHandler;
        this.yamlTagResolver = yamlTagResolver;
        this.prismContext = prismContext;
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
}
