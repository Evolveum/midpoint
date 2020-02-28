/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */
public interface PrismContainerDefinition<C extends Containerable> extends ItemDefinition<PrismContainer<C>>, LocalDefinitionStore {

    Class<C> getCompileTimeClass();

    ComplexTypeDefinition getComplexTypeDefinition();

    String getDefaultNamespace();

    List<String> getIgnoredNamespaces();

    List<? extends ItemDefinition> getDefinitions();

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
