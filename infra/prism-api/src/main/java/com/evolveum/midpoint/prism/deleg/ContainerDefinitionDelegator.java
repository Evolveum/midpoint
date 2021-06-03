/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.deleg;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

public interface ContainerDefinitionDelegator<C extends Containerable> extends ItemDefinitionDelegator<PrismContainer<C>>, PrismContainerDefinition<C> {

    @Override
    PrismContainerDefinition<C> delegate();

    @Override
    default Class<C> getCompileTimeClass() {
        return delegate().getCompileTimeClass();
    }

    @Override
    default ComplexTypeDefinition getComplexTypeDefinition() {
        return delegate().getComplexTypeDefinition();
    }

    @Override
    default String getDefaultNamespace() {
        return delegate().getDefaultNamespace();
    }

    @Override
    default List<String> getIgnoredNamespaces() {
        return delegate().getIgnoredNamespaces();
    }

    @Override
    default List<? extends ItemDefinition> getDefinitions() {
        return delegate().getDefinitions();
    }

    @Override
    default Collection<ItemName> getItemNames() {
        return delegate().getItemNames();
    }

    @Override
    default <ID extends ItemDefinition> ID findLocalItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz,
            boolean caseInsensitive) {
        return delegate().findLocalItemDefinition(name, clazz, caseInsensitive);
    }

    @Override
    default <ID extends ItemDefinition> ID findLocalItemDefinition(@NotNull QName name) {
        return delegate().findLocalItemDefinition(name);
    }

    @Override
    default boolean isCompletelyDefined() {
        return delegate().isCompletelyDefined();
    }

    @Override
    default List<PrismPropertyDefinition> getPropertyDefinitions() {
        return delegate().getPropertyDefinitions();
    }

    @Override
    default <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path) {
        return delegate().findItemDefinition(path);
    }

    @Override
    default PrismReferenceDefinition findReferenceDefinition(@NotNull ItemName name) {
        return delegate().findReferenceDefinition(name);
    }

    @Override
    default <T extends ItemDefinition> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        return delegate().findItemDefinition(path, clazz);
    }


    @Override
    default PrismContainerDefinition<C> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
        return delegate().cloneWithReplacedDefinition(itemName, newDefinition);
    }

    @Override
    default void replaceDefinition(QName itemName, ItemDefinition newDefinition) {
        delegate().replaceDefinition(itemName, newDefinition);
    }

    @Override
    default PrismContainerValue<C> createValue() {
        return delegate().createValue();
    }

    @Override
    default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(@NotNull String name) {
        return delegate().findContainerDefinition(name);
    }

    @Override
    default boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    default boolean canRepresent(@NotNull QName type) {
        return delegate().canRepresent(type);
    }

    @Override
    default <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest,
            @NotNull Class<ID> clazz) {
        return delegate().findNamedItemDefinition(firstName, rest, clazz);
    }

    @Override
    default <T> PrismPropertyDefinition<T> findPropertyDefinition(@NotNull ItemPath path) {
        return delegate().findPropertyDefinition(path);
    }

    @Override
    default PrismReferenceDefinition findReferenceDefinition(@NotNull ItemPath path) {
        return delegate().findReferenceDefinition(path);
    }

    @Override
    default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(@NotNull ItemPath path) {
        return delegate().findContainerDefinition(path);
    }

    @Override
    default ContainerDelta<C> createEmptyDelta(ItemPath path) {
        return delegate().createEmptyDelta(path);
    }

    @Override
    default Class<C> getTypeClass() {
        return delegate().getTypeClass();
    }

    @Override
    default Optional<ComplexTypeDefinition> structuredType() {
        return delegate().structuredType();
    }

}
