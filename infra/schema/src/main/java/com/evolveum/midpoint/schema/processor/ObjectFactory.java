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
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import javax.xml.namespace.QName;

/**
 *  EXPERIMENTAL
 */
@Experimental
public class ObjectFactory {

    @TestOnly
    public static <T> ShadowSimpleAttribute<T> createSimpleAttribute(QName name) {
        return new ShadowSimpleAttributeImpl<>(name, null);
    }

    /**
     * Creates {@link ShadowSimpleAttributeDefinition} with given parameters.
     *
     * The created definition is effectively immutable.
     */
    @TestOnly
    public static <T> ShadowSimpleAttributeDefinition<T> createSimpleAttributeDefinition(
            @NotNull QName name, @NotNull QName typeName) throws ConfigurationException {
        return ShadowSimpleAttributeDefinitionImpl.create(
                createNativeAttributeDefinition(name, typeName));
    }

    public static <T> NativeShadowAttributeDefinitionImpl<T> createNativeAttributeDefinition(
            @NotNull QName name, @NotNull QName typeName) {
        return new NativeShadowAttributeDefinitionImpl<>(ItemName.fromQName(name), typeName);
    }

    public static NativeResourceSchemaBuilder createNativeResourceSchemaBuilder() {
        return new NativeResourceSchemaImpl();
    }

    public static PrismObjectDefinition<ShadowType> constructObjectDefinition(
            ShadowAttributesContainerDefinition rACD,
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
