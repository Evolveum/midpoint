/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.google.common.annotations.VisibleForTesting;

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
    public static <T> ResourceAttributeDefinition<T> createResourceAttributeDefinition(QName name, QName typeName) {
        return ResourceAttributeDefinitionImpl.create(
                createRawResourceAttributeDefinition(name, typeName));
    }

    /**
     * Creates {@link RawResourceAttributeDefinition}. It is mutable but not directly instantiable.
     */
    public static <T> MutableRawResourceAttributeDefinition<T> createRawResourceAttributeDefinition(QName name, QName typeName) {
        return new RawResourceAttributeDefinitionImpl<>(name, typeName);
    }

    public static ResourceAttributeContainer createResourceAttributeContainer(
            QName name, ResourceAttributeContainerDefinition definition) {
        return new ResourceAttributeContainerImpl(name, definition);
    }

    public static ResourceAttributeContainerDefinition createResourceAttributeContainerDefinition(
            QName name, ResourceObjectDefinition resourceObjectDefinition) {
        return new ResourceAttributeContainerDefinitionImpl(name, resourceObjectDefinition);
    }

    public static MutableResourceSchema createResourceSchema() {
        return new ResourceSchemaImpl();
    }

    public static PrismObjectDefinition<ShadowType> constructObjectDefinition(ResourceAttributeContainerDefinition rACD) {
        // Almost-shallow clone of object definition and complex type
        PrismObjectDefinition<ShadowType> shadowDefinition =
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        return shadowDefinition.cloneWithReplacedDefinition(ShadowType.F_ATTRIBUTES, rACD);
    }
}
