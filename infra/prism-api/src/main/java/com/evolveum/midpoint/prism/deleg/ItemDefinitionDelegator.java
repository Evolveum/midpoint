/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.deleg;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

public interface ItemDefinitionDelegator<I extends Item<?,?>> extends DefinitionDelegator, ItemDefinition<I> {

    @Override
    ItemDefinition<I> delegate();

    @Override
    default boolean canRead() {
        return delegate().canRead();
    }

    @Override
    default @NotNull ItemName getItemName() {
        return delegate().getItemName();
    }

    @Override
    default String getNamespace() {
        return delegate().getNamespace();
    }

    @Override
    default int getMinOccurs() {
        return delegate().getMinOccurs();
    }

    @Override
    default int getMaxOccurs() {
        return delegate().getMaxOccurs();
    }

    @Override
    default boolean isMandatory() {
        return delegate().isMandatory();
    }

    @Override
    default boolean isOptional() {
        return delegate().isOptional();
    }

    @Override
    default boolean isOperational() {
        return delegate().isOperational();
    }

    @Override
    default boolean isIndexOnly() {
        return delegate().isIndexOnly();
    }

    @Override
    default boolean canModify() {
        return delegate().canModify();
    }

    @Override
    default boolean isInherited() {
        return delegate().isInherited();
    }

    @Override
    default boolean isDynamic() {
        return delegate().isDynamic();
    }

    @Override
    default boolean canAdd() {
        return delegate().canAdd();
    }

    @Override
    default QName getSubstitutionHead() {
        return delegate().getSubstitutionHead();
    }

    @Override
    default boolean isHeterogeneousListItem() {
        return delegate().isHeterogeneousListItem();
    }

    @Override
    default PrismReferenceValue getValueEnumerationRef() {
        return delegate().getValueEnumerationRef();
    }

    @Override
    default boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
        return delegate().isValidFor(elementQName, clazz);
    }

    @Override
    default boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition> clazz,
            boolean caseInsensitive) {
        return delegate().isValidFor(elementQName, clazz, caseInsensitive);
    }

    @Override
    default void adoptElementDefinitionFrom(ItemDefinition otherDef) {
        delegate().adoptElementDefinitionFrom(otherDef);
    }

    @Override
    default @NotNull I instantiate() throws SchemaException {
        return delegate().instantiate();
    }

    @Override
    default @NotNull I instantiate(QName name) throws SchemaException {
        return delegate().instantiate(name);
    }

    @Override
    default <T extends ItemDefinition> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        if (path.isEmpty()) {
            if (clazz.isAssignableFrom(this.getClass())) {
                return (T) this;
            } else {
                throw new IllegalArgumentException("Looking for definition of class " + clazz + " but found " + this);
            }
        } else {
            return null;
        }
    }

    @Override
    default ItemDelta createEmptyDelta(ItemPath path) {
        return delegate().createEmptyDelta(path);
    }

    @Override
    default ItemDefinition<I> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
        return delegate().deepClone(ultraDeep, postCloneAction);
    }

    @Override
    default ItemDefinition<I> deepClone(Map<QName, ComplexTypeDefinition> ctdMap,
            Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        return delegate().deepClone(ctdMap, onThisPath, postCloneAction);
    }

    @Override
    default void debugDumpShortToString(StringBuilder sb) {
        delegate().debugDumpShortToString(sb);
    }

    @Override
    default boolean canBeDefinitionOf(I item) {
        return delegate().canBeDefinitionOf(item);
    }

    @Override
    default boolean canBeDefinitionOf(PrismValue pvalue) {
        return delegate().canBeDefinitionOf(pvalue);
    }

    @Override
    default Optional<ComplexTypeDefinition> structuredType() {
        return delegate().structuredType();
    }


}
