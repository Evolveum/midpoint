/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 *
 */
class DocumentReadOperation {

    /**
     * Source of JSON/YAML data.
     */
    @NotNull final JsonParser parser;

    @NotNull final JsonReadingContext ctx;

    final ParsingContext prismParsingContext;

    @NotNull final LexicalProcessor.RootXNodeHandler objectHandler;

    @NotNull final AbstractReader.YamlTagResolver yamlTagResolver;

    private boolean aborted;

    DocumentReadOperation(ReadOperation parentOperation) {
        this.parser = parentOperation.parser;
        this.ctx = parentOperation.ctx;
        this.prismParsingContext = parentOperation.prismParsingContext;
        this.objectHandler = parentOperation.objectHandler;
        this.yamlTagResolver = parentOperation.yamlTagResolver;
    }

    void readMulti() throws IOException, SchemaException {
        if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
            parser.nextToken();
            while (!aborted && parser.getCurrentToken() != JsonToken.END_ARRAY) {
                read();
            }
        } else {
            read();
        }
    }

    void read() throws IOException, SchemaException {
        new XNodeReadOperation(this).read();
    }

    void setAborted() {
        aborted = true;
    }

    public boolean isAborted() {
        return aborted;
    }
}
