/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.ParsingContextImpl;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor.RootXNodeHandler;
import com.evolveum.midpoint.prism.impl.lex.json.AbstractReader.YamlTagResolver;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.fasterxml.jackson.core.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * TODO better name
 */
class ReadOperation {



    /**
     * Source of JSON/YAML data.
     */
    @NotNull final JsonParser parser;

    /**
     * TODO
     */
    @NotNull final JsonReadingContext ctx;
    @NotNull final YamlTagResolver yamlTagResolver;
    @NotNull final PrismContext prismContext;
    final ParsingContextImpl prismParsingContext;

    @NotNull final RootXNodeHandler objectHandler;
    private final boolean expectingMultipleObjects;

    ReadOperation(@NotNull JsonParser parser, @NotNull JsonReadingContext ctx, @NotNull RootXNodeHandler objectHandler,
            boolean expectingMultipleObjects, @NotNull YamlTagResolver yamlTagResolver, @NotNull PrismContext prismContext) {
        this.parser = parser;
        this.ctx = ctx;
        this.prismParsingContext = ctx.prismParsingContext;
        this.objectHandler = objectHandler;
        this.expectingMultipleObjects = expectingMultipleObjects;
        this.yamlTagResolver = yamlTagResolver;
        this.prismContext = prismContext;
    }

    public void execute() throws IOException, SchemaException {
        parser.nextToken();
        if (parser.currentToken() == null) {
            throw new SchemaException("Nothing to parse: the input is empty.");
        }

        do {
            DocumentReadOperation documentReadOperation = new DocumentReadOperation(this);
            if (expectingMultipleObjects) {
                documentReadOperation.readMulti();
            } else {
                documentReadOperation.read();
            }
            if (documentReadOperation.isAborted()) {
                break;
            }
        } while (parser.nextToken() != null); // YAML multi-document files
    }
}
