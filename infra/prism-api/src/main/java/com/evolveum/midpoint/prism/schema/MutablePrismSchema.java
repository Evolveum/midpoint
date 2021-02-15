/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 *
 */
public interface MutablePrismSchema extends PrismSchema {

    void add(@NotNull Definition def);

    // used for connector and resource schemas
    void parseThis(Element element, boolean isRuntime, String shortDescription, PrismContext prismContext) throws SchemaException;

    MutablePrismContainerDefinition createPropertyContainerDefinition(String localTypeName);

    MutablePrismContainerDefinition createPropertyContainerDefinition(String localElementName, String localTypeName);

    ComplexTypeDefinition createComplexTypeDefinition(QName typeName);

    PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName);

    PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName);

    void addDelayedItemDefinition(DefinitionSupplier o);

    void addSubstitution(QName substitutionHead, ItemDefinition<?> definition);
}
