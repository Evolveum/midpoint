/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO
 */
public interface PrismContainerDefinition<C extends Containerable> extends ItemDefinition<PrismContainer<C>>, LocalDefinitionStore {

    Class<C> getCompileTimeClass();

    ComplexTypeDefinition getComplexTypeDefinition();

    String getDefaultNamespace();

    List<String> getIgnoredNamespaces();

    List<? extends ItemDefinition> getDefinitions();

    /**
     * Returns names of items that are defined within this container definition. They do NOT include items that can be put into
     * instantiated container by means of "xsd:any" mechanism.
     */
    default Collection<ItemName> getItemNames() {
        return getDefinitions().stream()
                .map(ItemDefinition::getItemName)
                .collect(Collectors.toSet());
    }

    /**
     * Returns true if the instantiated container can contain only items that are explicitly defined here.
     */
    boolean isCompletelyDefined();

    List<PrismPropertyDefinition> getPropertyDefinitions();

    @Override
    ContainerDelta<C> createEmptyDelta(ItemPath path);

    @NotNull
    @Override
    PrismContainerDefinition<C> clone();

    PrismContainerDefinition<C> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition);

    void replaceDefinition(QName itemName, ItemDefinition newDefinition);

    PrismContainerValue<C> createValue();

    boolean isEmpty();

    boolean canRepresent(@NotNull QName type);

    @Override
    MutablePrismContainerDefinition<C> toMutable();

    @Override
    Class<C> getTypeClass();
}
