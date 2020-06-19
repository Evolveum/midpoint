/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonFactory;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.exception.SchemaException;

public class JsonReader extends AbstractReader {

    public JsonReader(@NotNull SchemaRegistry schemaRegistry) {
        super(schemaRegistry);
    }

    @Override
    public boolean canRead(@NotNull File file) {
        return file.getName().endsWith(".json");
    }

    @Override
    public boolean canRead(@NotNull String dataString) {
        return dataString.startsWith("{");
    }

    @Override
    protected com.fasterxml.jackson.core.JsonParser createJacksonParser(InputStream stream) throws SchemaException, IOException {
        JsonFactory factory = new JsonFactory();
        try {
            return factory.createParser(stream);
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    protected QName tagToTypeName(Object tid, JsonParsingContext ctx) throws IOException, SchemaException {
        return null;
    }
}
