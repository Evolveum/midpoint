/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 *
 */
public interface MutableComplexTypeDefinition extends ComplexTypeDefinition, MutableTypeDefinition {

    void add(ItemDefinition<?> definition);

    void delete(QName itemName);

    MutablePrismPropertyDefinition<?> createPropertyDefinition(QName name, QName typeName);

    MutablePrismPropertyDefinition<?> createPropertyDefinition(String name, QName typeName);

    @NotNull
    ComplexTypeDefinition clone();

    void setExtensionForType(QName type);

    void setAbstract(boolean value);

    void setSuperType(QName superType);

    void setObjectMarker(boolean value);

    void setContainerMarker(boolean value);

    void setReferenceMarker(boolean value);

    void setDefaultNamespace(String namespace);

    void setIgnoredNamespaces(@NotNull List<String> ignoredNamespaces);

    void setXsdAnyMarker(boolean value);

    void setListMarker(boolean value);

    void setCompileTimeClass(Class<?> compileTimeClass);

    void replaceDefinition(QName itemName, ItemDefinition newDefinition);
}
