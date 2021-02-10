/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.writer;

import java.io.IOException;
import java.util.List;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.impl.lex.LexicalUtils;
import com.evolveum.midpoint.prism.impl.xnode.ListXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeDefinition;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Writes XNode into JSON/YAML.
 */
abstract public class AbstractWriter {

    private XNodeDefinition.Root schema;

    public AbstractWriter(@NotNull SchemaRegistry schemaRegistry) {
        schema = schemaRegistry != null ? XNodeDefinition.root(schemaRegistry) : XNodeDefinition.empty();
    }

    public XNodeDefinition getSchema() {
        return schema;
    }

    @NotNull
    public String write(@NotNull XNode xnode, @NotNull QName rootElementName, SerializationContext prismSerializationContext) throws SchemaException{
        return write(LexicalUtils.createRootXNode((XNodeImpl) xnode, rootElementName), prismSerializationContext);
    }

    @NotNull
    public String write(@NotNull RootXNode root, SerializationContext prismSerializationContext) throws SchemaException {
        return writeInternal((RootXNodeImpl) root, prismSerializationContext, false);
    }

    @NotNull
    public String write(@NotNull List<RootXNodeImpl> roots, @Nullable SerializationContext prismSerializationContext) throws SchemaException {
        ListXNodeImpl objectsList = new ListXNodeImpl();
        for (RootXNodeImpl root : roots) {
            objectsList.add(root.toMapXNode());
        }
        return writeInternal(objectsList, prismSerializationContext, true);
    }

    @NotNull
    private String writeInternal(@NotNull XNodeImpl root, SerializationContext prismSerializationContext, boolean useMultiDocument) throws SchemaException {
        try (WritingContext<?> ctx = createWritingContext(prismSerializationContext)) {
            DocumentWriter documentWriter = new DocumentWriter(ctx, schema);
            if (root instanceof ListXNodeImpl && !root.isEmpty() && useMultiDocument && ctx.supportsMultipleDocuments()) {
                // Note we cannot serialize empty lists in multi-document mode.
                // It would result in empty content and an exception during serialization.
                documentWriter.writeListAsSeparateDocuments((ListXNodeImpl) root);
            } else {
                documentWriter.write(root);
            }
            ctx.close(); // in order to get complete output
            return ctx.getOutput();
        } catch (JsonProcessingException ex) {
            throw new SchemaException("Error during writing to JSON/YAML: " + ex.getMessage(), ex);
        } catch (IOException ioe) {
            // There should be no IOExceptions as we are serializing to a string.
            throw new SystemException("Error during writing to JSON/YAML: " + ioe.getMessage(), ioe);
        }
    }

    abstract WritingContext<?> createWritingContext(SerializationContext prismSerializationContext);
}
