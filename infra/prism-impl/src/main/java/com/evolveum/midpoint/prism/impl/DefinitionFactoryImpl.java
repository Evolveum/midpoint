/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.DisplayableValue;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *
 */
public class DefinitionFactoryImpl implements DefinitionFactory {

    @NotNull private final PrismContextImpl prismContext;

    DefinitionFactoryImpl(@NotNull PrismContextImpl prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    public ComplexTypeDefinitionImpl createComplexTypeDefinition(QName name) {
        return new ComplexTypeDefinitionImpl(name, prismContext);
    }

    @Override
    public <T> MutablePrismPropertyDefinition<T> createPropertyDefinition(QName name, QName typeName) {
        return new PrismPropertyDefinitionImpl<>(name, typeName, prismContext);
    }

    @Override
    public MutablePrismReferenceDefinition createReferenceDefinition(QName name, QName typeName) {
        return new PrismReferenceDefinitionImpl(name, typeName, prismContext);
    }

    @Override
    public MutablePrismContainerDefinition<?> createContainerDefinition(QName name, ComplexTypeDefinition ctd) {
        return new PrismContainerDefinitionImpl<>(name, ctd, prismContext);
    }

    @Override
    public <T> MutablePrismPropertyDefinition<T> createPropertyDefinition(QName name, QName typeName,
            Collection<? extends DisplayableValue<T>> allowedValues, T defaultValue) {
        return new PrismPropertyDefinitionImpl<>(name, typeName, prismContext, allowedValues, defaultValue);
    }
}
