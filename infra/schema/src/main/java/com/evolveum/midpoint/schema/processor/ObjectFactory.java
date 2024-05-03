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

    public static <T> ShadowSimpleAttribute<T> createResourceAttribute(QName name, ShadowSimpleAttributeDefinition<T> definition) {
        return new ShadowSimpleAttributeImpl<>(name, definition);
    }

    /**
     * Creates {@link ShadowSimpleAttributeDefinition} with given parameters.
     *
     * The created definition is effectively immutable.
     */
    @VisibleForTesting
    public static <T> ShadowSimpleAttributeDefinition<T> createResourceAttributeDefinition(
            @NotNull QName name, @NotNull QName typeName) throws SchemaException {
        return ShadowSimpleAttributeDefinitionImpl.create(
                createNativeItemDefinition(name, typeName));
    }

    public static <T> NativeShadowAttributeDefinitionImpl<T> createNativeItemDefinition(
            @NotNull QName name, @NotNull QName typeName) {
        return new NativeShadowAttributeDefinitionImpl<>(ItemName.fromQName(name), typeName);
    }

    public static ShadowAttributesContainer createResourceAttributeContainer(
            QName name, ResourceAttributeContainerDefinition definition) {
        return new ShadowAttributesContainerImpl(name, definition);
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
