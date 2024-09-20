/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SerializableSchema;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Freezable;
import com.evolveum.midpoint.prism.schema.SchemaBuilder;
import com.evolveum.midpoint.schema.processor.NativeObjectClassDefinition.NativeObjectClassDefinitionBuilder;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

/**
 * The resource schema as obtained from the connector (or manually specified via XSD).
 * Not a {@link PrismSchema}. Needed only to build "refined" {@link CompleteResourceSchema}.
 */
public interface NativeResourceSchema
        extends SerializableSchema, Serializable, Freezable, Cloneable, DebugDumpable {

    static boolean isNullOrEmpty(NativeResourceSchema schema) {
        return schema == null || schema.isEmpty();
    }

    static boolean isNotEmpty(NativeResourceSchema schema) {
        return !isNullOrEmpty(schema);
    }

    @Nullable NativeObjectClassDefinition findObjectClassDefinition(@NotNull QName objectClassName);

    @Nullable NativeReferenceTypeDefinition findReferenceTypeDefinition(@NotNull QName className);

    // Not very performant; should not be called often
    @NotNull Collection<? extends NativeObjectClassDefinition> getObjectClassDefinitions();

    // Not very performant; should not be called often
    @NotNull Collection<? extends NativeReferenceTypeDefinition> getReferenceTypeDefinitions();

    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    @NotNull NativeResourceSchema clone();

    @NotNull Document serializeToXsd() throws SchemaException;

    interface NativeResourceSchemaBuilder extends SchemaBuilder {

        @NotNull NativeObjectClassDefinitionBuilder newComplexTypeDefinitionLikeBuilder(String localTypeName);

        @NotNull NativeResourceSchema getObjectBuilt();

        void computeReferenceTypes() throws SchemaException;
    }
}
