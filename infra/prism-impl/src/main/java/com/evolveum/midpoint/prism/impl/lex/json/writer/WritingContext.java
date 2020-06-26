/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.writer;

import com.evolveum.midpoint.prism.SerializationContext;

import com.evolveum.midpoint.util.exception.SystemException;

import com.fasterxml.jackson.core.JsonGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;

/**
 * TODO
 */
abstract class WritingContext<G extends JsonGenerator> implements AutoCloseable {

    @NotNull private final StringWriter out = new StringWriter();
    @NotNull final G generator;
    @NotNull final SerializationContext prismSerializationContext;

    WritingContext(@Nullable SerializationContext prismSerializationContext) {
        this.generator = createJacksonGenerator(out);
        this.prismSerializationContext = prismSerializationContext != null ?
                prismSerializationContext :
                new SerializationContext(null);
    }

    abstract G createJacksonGenerator(StringWriter out);

    String getOutput() {
        return out.toString();
    }

    @Override
    public void close() {
        try {
            generator.close();
        } catch (IOException e) {
            throw new SystemException("Error writing to JSON/YAML: " + e.getMessage(), e);
        }
    }

    abstract boolean supportsInlineTypes();

    abstract void writeInlineType(QName typeName) throws IOException;

    abstract void resetInlineTypeIfPossible();

    abstract boolean supportsMultipleDocuments();

    abstract void newDocument() throws IOException;
}
