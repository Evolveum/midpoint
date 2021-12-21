/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableComplexTypeDefinition;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.function.Consumer;

/**
 * Interface allowing modifications of an object class definition.
 */
public interface MutableResourceObjectClassDefinition extends ResourceObjectClassDefinition, MutableComplexTypeDefinition {

    void add(ItemDefinition<?> definition);

    void addPrimaryIdentifierName(QName name);

    void addSecondaryIdentifierName(QName name);

    void setDescriptionAttributeName(QName name);

    void setNamingAttributeName(QName name);

    void setDisplayNameAttributeName(QName name);

    void setNativeObjectClass(String nativeObjectClass);

    void setAuxiliary(boolean auxiliary);

    void setDefaultAccountDefinition(boolean defaultAccountType);

    /**
     * Returned value is immutable.
     */
    @VisibleForTesting
    <T> ResourceAttributeDefinition<T> createAttributeDefinition(
            @NotNull QName name,
            @NotNull QName typeName,
            @NotNull Consumer<MutableRawResourceAttributeDefinition<?>> customizer);

    /**
     * Returned value is immutable.
     */
    @VisibleForTesting
    default <T> ResourceAttributeDefinition<T> createAttributeDefinition(
            @NotNull String localName,
            @NotNull QName typeName,
            @NotNull Consumer<MutableRawResourceAttributeDefinition<?>> customizer) {
        return createAttributeDefinition(
                new QName(MidPointConstants.NS_RI, localName),
                typeName,
                customizer);
    }
}
