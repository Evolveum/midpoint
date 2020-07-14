/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import com.evolveum.midpoint.prism.ParserSource;
import com.evolveum.midpoint.prism.ParserXNodeSource;
import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.List;

/**
 * "Null" processor that reads XNodes into XNodes and writes XNodes as XNodes.
 */
public class NullLexicalProcessor implements LexicalProcessor<XNodeImpl> {

    @NotNull
    @Override
    public RootXNodeImpl read(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) {
        if (!(source instanceof ParserXNodeSource)) {
            throw new IllegalStateException("Unsupported parser source: " + source.getClass().getName());
        }
        return (RootXNodeImpl) ((ParserXNodeSource) source).getXNode();
    }

    @NotNull
    @Override
    public List<RootXNodeImpl> readObjects(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readObjectsIteratively(@NotNull ParserSource source, @NotNull ParsingContext parsingContext,
            RootXNodeHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRead(@NotNull File file) {
        return false;
    }

    @Override
    public boolean canRead(@NotNull String dataString) {
        return false;
    }

    @NotNull
    @Override
    public XNodeImpl write(@NotNull RootXNode xnode, @Nullable SerializationContext serializationContext) throws SchemaException {
        return (XNodeImpl) xnode;
    }

    @NotNull
    @Override
    public XNodeImpl write(@NotNull XNode xnode, @NotNull QName rootElementName, @Nullable SerializationContext serializationContext)
            throws SchemaException {
        return (XNodeImpl) xnode;
    }

    @NotNull
    @Override
    public XNodeImpl write(@NotNull List<RootXNodeImpl> roots,
            @Nullable SerializationContext context) throws SchemaException {
        throw new UnsupportedOperationException("NullLexicalProcessor.write is not supported for a collection of objects");
    }
}
