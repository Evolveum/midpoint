/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.NativeResourceSchema.NativeResourceSchemaBuilder;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 *  EXPERIMENTAL
 */
@Experimental
public class ObjectFactory {

    public static <T> ResourceAttribute<T> createResourceAttribute(QName name, ResourceAttributeDefinition<T> definition) {
        return new ResourceAttributeImpl<>(name, definition);
    }

    /**
     * Creates {@link ResourceAttributeDefinition} with given parameters.
     *
     * The created definition is effectively immutable.
     */
    @VisibleForTesting
    public static <T> ResourceAttributeDefinition<T> createResourceAttributeDefinition(
            @NotNull QName name, @NotNull QName typeName) throws SchemaException {
        return ResourceAttributeDefinitionImpl.create(
                createNativeItemDefinition(name, typeName));
    }

    public static <T> NativeShadowItemDefinitionImpl<T> createNativeItemDefinition(
            @NotNull QName name, @NotNull QName typeName) {
        return new NativeShadowItemDefinitionImpl<>(ItemName.fromQName(name), typeName);
    }

    public static ResourceAttributeContainer createResourceAttributeContainer(
            QName name, ResourceAttributeContainerDefinition definition) {
        return new ResourceAttributeContainerImpl(name, definition);
    }

    public static NativeResourceSchemaBuilder createNativeResourceSchemaBuilder() {
        return new NativeResourceSchemaImpl();
    }

    public static PrismObjectDefinition<ShadowType> constructObjectDefinition(
            ResourceAttributeContainerDefinition rACD,
            ShadowAssociationsContainerDefinition rAsCD) {
        // Almost-shallow clone of object definition and complex type
        PrismObjectDefinition<ShadowType> shadowDefinition =
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        // FIXME eliminate double cloning!
        return shadowDefinition
                .cloneWithNewDefinition(ShadowType.F_ATTRIBUTES, rACD)
                .cloneWithNewDefinition(ShadowType.F_ASSOCIATIONS, rAsCD);
    }
}
