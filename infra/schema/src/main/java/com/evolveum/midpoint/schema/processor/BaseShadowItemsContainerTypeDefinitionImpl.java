/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemMerger;
import com.evolveum.midpoint.prism.key.NaturalKeyDefinition;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * TODO
 */
abstract class BaseShadowItemsContainerTypeDefinitionImpl implements ShadowItemsComplexTypeDefinition {

    @Override
    public @Nullable QName getExtensionForType() {
        return null;
    }

    @Override
    public boolean isReferenceMarker() {
        return false;
    }

    @Override
    public boolean isContainerMarker() {
        return true;
    }

    @Override
    public boolean isObjectMarker() {
        return false;
    }

    @Override
    public boolean isXsdAnyMarker() {
        return true;
    }

    public boolean isListMarker() {
        return false;
    }

    @Override
    public @Nullable QName getDefaultItemTypeName() {
        return null;
    }

    @Override
    public @Nullable QName getDefaultReferenceTargetTypeName() {
        return null;
    }

    @Override
    public @Nullable String getDefaultNamespace() {
        return null;
    }

    @Override
    public @NotNull List<String> getIgnoredNamespaces() {
        return List.of();
    }

    @Override
    public void merge(ComplexTypeDefinition otherComplexTypeDef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return getDefinitions().isEmpty();
    }

    @Override
    public @NotNull ComplexTypeDefinition deepClone(@NotNull DeepCloneOperation operation) {
        throw new UnsupportedOperationException("FIXME");
    }

    @Override
    public boolean hasSubstitutions() {
        return false;
    }

    @Override
    public Optional<ItemDefinition<?>> substitution(QName name) {
        return Optional.empty();
    }

    @Override
    public ComplexTypeDefinitionMutator mutator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Class<?> getCompileTimeClass() {
        return null;
    }

    @Override
    public @Nullable QName getSuperType() {
        return null;
    }

    @Override
    public @NotNull Collection<TypeDefinition> getStaticSubTypes() {
        return List.of();
    }

    @Override
    public Integer getInstantiationOrder() {
        return null;
    }

    @Override
    public boolean canRepresent(QName typeName) {
        return QNameUtil.match(typeName, getTypeName());
    }

    @Override
    public abstract @NotNull BaseShadowItemsContainerTypeDefinitionImpl clone();

    @Override
    public @Nullable NaturalKeyDefinition getNaturalKeyInstance() {
        return null;
    }

    @Override
    public @Nullable List<QName> getNaturalKeyConstituents() {
        return List.of();
    }

    @Override
    public @Nullable ItemMerger getMergerInstance(@NotNull MergeStrategy strategy, @Nullable OriginMarker originMarker) {
        return null;
    }

    @Override
    public @Nullable String getMergerIdentifier() {
        return null;
    }
}
