/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
 package com.evolveum.midpoint.prism.impl.lex;

import com.evolveum.midpoint.prism.ParserSource;
import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Takes care of converting between XNode tree and specific lexical representation (XML, JSON, YAML). As a special case,
 * NullLexicalProcessor uses XNode tree itself as a lexical representation.
 *
 * @author semancik
 *
 */
public interface LexicalProcessor<T> {

    @NotNull
    RootXNodeImpl read(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException;

    @NotNull
    List<RootXNodeImpl> readObjects(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException;

    /**
     * Note that this interface does not contain handleError method. It seems that we are currently not able to successfully
     * recover from JSON/YAML parsing errors - so, after first exception we would get quite random (garbage) data from the
     * input stream. It is much more safe just to abort processing in that case.
     */
    @FunctionalInterface
    interface RootXNodeHandler {
        /**
         * Called when a RootXNode was successfully retrieved from the input.
         * @return true if the processing should continue
         */
        boolean handleData(RootXNodeImpl node);
    }

    void readObjectsIteratively(@NotNull ParserSource source, @NotNull ParsingContext parsingContext, RootXNodeHandler handler) throws SchemaException, IOException;

    /**
     * Checks if the processor can read from a given file. (Guessed by file extension, for now.)
     * Used for autodetection of language.
     */
    boolean canRead(@NotNull File file) throws IOException;

    /**
     * Checks if the processor can read from a given string. Note this is only an approximative information (for now).
     * Used for autodetection of language.
     */
    boolean canRead(@NotNull String dataString);

    /**
     * Serializes a root node into XNode tree.
     */
    @NotNull
    T write(@NotNull RootXNode xnode, @Nullable SerializationContext serializationContext) throws SchemaException;

    /**
     * Serializes a non-root node into XNode tree.
     * So, xnode SHOULD NOT be a root node (at least for now).
     *
     * TODO consider removing - replacing by the previous form.
     */
    @NotNull
    T write(@NotNull XNode xnode, @NotNull QName rootElementName, @Nullable SerializationContext serializationContext) throws SchemaException;

    /**
     * TODO
     *
     * Not supported for NullLexicalProcessor, though.
     */
    @NotNull
    T write(@NotNull List<RootXNodeImpl> roots, @Nullable SerializationContext context) throws SchemaException;
}
