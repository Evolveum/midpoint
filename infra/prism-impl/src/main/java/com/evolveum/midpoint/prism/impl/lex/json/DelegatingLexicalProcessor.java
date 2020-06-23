/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ParserSource;
import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;

import com.evolveum.midpoint.prism.impl.lex.json.reader.AbstractReader;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DelegatingLexicalProcessor implements LexicalProcessor<String> {

    @NotNull private final AbstractReader reader;
    @NotNull private final AbstractWriter writer;

    public DelegatingLexicalProcessor(@NotNull AbstractReader reader, @NotNull AbstractWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public @NotNull RootXNodeImpl read(@NotNull ParserSource source, @NotNull ParsingContext parsingContext)
            throws SchemaException, IOException {
        return reader.read(source, parsingContext);
    }

    @Override
    public @NotNull List<RootXNodeImpl> readObjects(@NotNull ParserSource source, @NotNull ParsingContext parsingContext)
            throws SchemaException, IOException {
        return reader.readObjects(source, parsingContext);
    }

    @Override
    public void readObjectsIteratively(@NotNull ParserSource source, @NotNull ParsingContext parsingContext,
            RootXNodeHandler handler) throws SchemaException, IOException {
        reader.readObjectsIteratively(source, parsingContext, handler);
    }

    @Override
    public boolean canRead(@NotNull File file) throws IOException {
        return reader.canRead(file);
    }

    @Override
    public boolean canRead(@NotNull String dataString) {
        return reader.canRead(dataString);
    }

    @NotNull
    @Override
    public String write(@NotNull RootXNode xnode, @Nullable SerializationContext serializationContext) throws SchemaException {
        return writer.write(xnode, serializationContext);
    }

    @NotNull
    @Override
    public String write(@NotNull XNode xnode,
            @NotNull QName rootElementName, @Nullable SerializationContext serializationContext) throws SchemaException {
        return writer.write(xnode, rootElementName, serializationContext);
    }

    @NotNull
    @Override
    public String write(@NotNull List<RootXNodeImpl> roots, @Nullable QName aggregateElementName,
            @Nullable SerializationContext context) throws SchemaException {
        return writer.write(roots, aggregateElementName, context);
    }
}
