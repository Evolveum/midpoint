/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.reader;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonToken;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Reads a single JSON/YAML document.
 * It contains either a single RootXNode or a list of RootXNodes.
 * ("c:objects" method of storing multiple objects is no longer supported)
 */
class DocumentReader {

    @NotNull private final JsonReadingContext ctx;
    private final PrismNamespaceContext nsContext;

    DocumentReader(@NotNull JsonReadingContext ctx, PrismNamespaceContext context) {
        this.ctx = ctx;
        this.nsContext = context;
    }

    void read(boolean expectingMultipleObjects) throws IOException, SchemaException {
        if (expectingMultipleObjects && ctx.parser.getCurrentToken() == JsonToken.START_ARRAY) {
            ctx.parser.nextToken();
            while (!ctx.isAborted() && ctx.parser.getCurrentToken() != JsonToken.END_ARRAY) {
                read();
                ctx.parser.nextToken(); // END_OBJECT to START_OBJECT or END_ARRAY
            }
        } else {
            read();
        }
    }

    private void read() throws IOException, SchemaException {
        new RootObjectReader(ctx, nsContext).read();
    }
}
