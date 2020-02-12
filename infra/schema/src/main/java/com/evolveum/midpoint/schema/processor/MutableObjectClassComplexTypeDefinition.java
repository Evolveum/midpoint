/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableComplexTypeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.google.common.annotations.VisibleForTesting;

import javax.xml.namespace.QName;

/**
 *
 */
public interface MutableObjectClassComplexTypeDefinition extends ObjectClassComplexTypeDefinition, MutableComplexTypeDefinition {

    void add(ItemDefinition<?> definition);

    void addPrimaryIdentifier(ResourceAttributeDefinition<?> identifier);

    void addSecondaryIdentifier(ResourceAttributeDefinition<?> identifier);

    void setDescriptionAttribute(ResourceAttributeDefinition<?> descriptionAttribute);

    void setNamingAttribute(ResourceAttributeDefinition<?> namingAttribute);

    void setNamingAttribute(QName namingAttribute);

    void setNativeObjectClass(String nativeObjectClass);

    void setAuxiliary(boolean auxiliary);

    void setKind(ShadowKindType kind);

    void setDefaultInAKind(boolean defaultAccountType);

    void setIntent(String intent);

    void setDisplayNameAttribute(ResourceAttributeDefinition<?> displayName);

    void setDisplayNameAttribute(QName displayName);

    @VisibleForTesting
    <X> ResourceAttributeDefinitionImpl<X> createAttributeDefinition(QName name, QName typeName);

    @VisibleForTesting
    <X> ResourceAttributeDefinitionImpl<X> createAttributeDefinition(String localName, QName typeName);

    @VisibleForTesting
    <X> ResourceAttributeDefinition<X> createAttributeDefinition(String localName, String localTypeName);
}
