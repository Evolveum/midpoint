/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.reader;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * TODO better name
 */
class MultiDocumentReader {

    /**
     * TODO
     */
    @NotNull private final JsonReadingContext ctx;

    MultiDocumentReader(@NotNull JsonReadingContext ctx) {
        this.ctx = ctx;
    }

    public void read(boolean expectingMultipleObjects) throws IOException, SchemaException {
        do {
            new DocumentReader(ctx).read(expectingMultipleObjects);
        } while (!ctx.isAborted() && ctx.parser.nextToken() != null); // YAML multi-document files
    }
}
