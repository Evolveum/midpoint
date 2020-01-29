/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.DisplayableValue;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *  Factory for prism definitions (Definition and all its subtypes in prism-api).
 */
public interface DefinitionFactory {

    MutableComplexTypeDefinition createComplexTypeDefinition(QName name);

    <T> MutablePrismPropertyDefinition<T> createPropertyDefinition(QName name, QName typeName);

    MutablePrismReferenceDefinition createReferenceDefinition(QName name, QName typeName);

    MutablePrismContainerDefinition<?> createContainerDefinition(QName name, ComplexTypeDefinition ctd);

    <T> PrismPropertyDefinition<T> createPropertyDefinition(QName name, QName typeName, Collection<? extends DisplayableValue<T>> allowedValues, T defaultValue);
}
