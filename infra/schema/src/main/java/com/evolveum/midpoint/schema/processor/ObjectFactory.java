/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.annotation.Experimental;

import javax.xml.namespace.QName;

/**
 *  EXPERIMENTAL
 */
@Experimental
public class ObjectFactory {

    public static <T> ResourceAttribute<T> createResourceAttribute(QName name, ResourceAttributeDefinition<T> definition, PrismContext prismContext) {
        return new ResourceAttributeImpl<>(name, definition, prismContext);
    }

    public static <T> MutableResourceAttributeDefinition<T> createResourceAttributeDefinition(QName name, QName typeName,
            PrismContext prismContext) {
        return new ResourceAttributeDefinitionImpl<T>(name, typeName, prismContext);
    }

    public static ResourceAttributeContainer createResourceAttributeContainer(QName name, ResourceAttributeContainerDefinition definition,
            PrismContext prismContext) {
        return new ResourceAttributeContainerImpl(name, definition, prismContext);
    }

    public static ResourceAttributeContainerDefinition createResourceAttributeContainerDefinition(QName name,
            ObjectClassComplexTypeDefinition complexTypeDefinition, PrismContext prismContext) {
        return new ResourceAttributeContainerDefinitionImpl(name, complexTypeDefinition, prismContext);
    }

    public static MutableResourceSchema createResourceSchema(String namespace, PrismContext prismContext) {
        return new ResourceSchemaImpl(namespace, prismContext);
    }
}
