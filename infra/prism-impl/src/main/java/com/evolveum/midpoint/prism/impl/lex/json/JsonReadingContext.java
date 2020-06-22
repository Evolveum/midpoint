/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import com.evolveum.midpoint.prism.impl.ParsingContextImpl;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.fasterxml.jackson.core.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;

/**
 *  TODO probably remove this class
 */
class JsonReadingContext {
    @NotNull final JsonParser parser;
    @NotNull final ParsingContextImpl prismParsingContext;

    // TODO consider getting rid of these IdentityHashMaps by support default namespace marking and resolution
    //  directly in XNode structures (like it was done for Map XNode keys recently).

    // Definitions of namespaces ('@ns') within maps; to be applied after parsing.
    @NotNull final IdentityHashMap<MapXNodeImpl, String> defaultNamespaces = new IdentityHashMap<>();
    // Elements that should be skipped when filling-in default namespaces - those that are explicitly set with no-NS ('#name').
    // (Values for these entries are not important. Only key presence is relevant.)
    @NotNull final IdentityHashMap<XNodeImpl, Object> noNamespaceElementNames = new IdentityHashMap<>();

    JsonReadingContext(@NotNull JsonParser parser, @NotNull ParsingContextImpl prismParsingContext) {
        this.parser = parser;
        this.prismParsingContext = prismParsingContext;
    }

    JsonReadingContext createChildContext() {
        return new JsonReadingContext(parser, prismParsingContext);
    }
}
