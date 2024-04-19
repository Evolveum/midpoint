/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Provides information about resource object attributes.
 */
public interface AttributeDefinitionStore
    extends LocalItemDefinitionStore {

    /**
     * Returns all attribute definitions as an unmodifiable collection.
     * Should be the same content as returned by `getDefinitions`.
     *
     * The returned value is a {@link List} because of the contract of {@link ComplexTypeDefinition#getDefinitions()}.
     */
    @NotNull List<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions();

    /**
     * Returns all attribute definitions of given type as an unmodifiable collection.
     *
     */
    default @NotNull <AD extends ResourceAttributeDefinition<?>> Collection<AD> getAttributeDefinitions(Class<AD> type) {
        //noinspection unchecked
        return getAttributeDefinitions().stream()
                .filter(def -> type.isAssignableFrom(def.getClass()))
                .map(def -> (AD) def)
                .toList();
    }

    /**
     * Finds a definition of an attribute with a given name. Returns null if nothing is found.
     */
    default @Nullable <T> ResourceAttributeDefinition<T> findAttributeDefinition(QName name) {
        return findAttributeDefinition(name, false);
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link SchemaException} if it's not there.
     */
    default @NotNull <T> ResourceAttributeDefinition<T> findAttributeDefinitionRequired(@NotNull QName name)
            throws SchemaException {
        return findAttributeDefinitionRequired(name, () -> "");
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link IllegalStateException} if it's not there.
     */
    default @NotNull ResourceAttributeDefinition<?> findAttributeDefinitionStrictlyRequired(@NotNull QName name) {
        return findAttributeDefinitionStrictlyRequired(name, () -> "");
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link SchemaException} if it's not there.
     */
    default @NotNull <T> ResourceAttributeDefinition<T> findAttributeDefinitionRequired(
            @NotNull QName name, @NotNull Supplier<String> contextSupplier)
            throws SchemaException {
        return MiscUtil.requireNonNull(
                findAttributeDefinition(name),
                () -> new SchemaException("No definition of attribute " + name + " in " + this + contextSupplier.get()));
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link IllegalStateException} if it's not there.
     */
    default @NotNull ResourceAttributeDefinition<?> findAttributeDefinitionStrictlyRequired(
            @NotNull QName name, @NotNull Supplier<String> contextSupplier) {
        return MiscUtil.requireNonNull(
                findAttributeDefinition(name),
                () -> new IllegalStateException("No definition of attribute " + name + " in " + this + contextSupplier.get()));
    }

    /**
     * Finds a attribute definition by looking at the property name.
     *
     * Returns null if nothing is found.
     *
     * @param name property definition name
     * @param caseInsensitive if true, ignoring the case
     * @return found property definition or null
     */
    default <T> @Nullable ResourceAttributeDefinition<T> findAttributeDefinition(QName name, boolean caseInsensitive) {
        //noinspection unchecked
        return findLocalItemDefinition(
                ItemName.fromQName(name), ResourceAttributeDefinition.class, caseInsensitive);
    }

    /**
     * Finds attribute definition using local name only.
     *
     * BEWARE: Ignores attributes in namespaces other than "ri:" (e.g. icfs:uid and icfs:name).
     */
    @VisibleForTesting
    default <T> ResourceAttributeDefinition<T> findAttributeDefinition(String name) {
        return findAttributeDefinition(
                new QName(MidPointConstants.NS_RI, name));
    }

    /** A convenience variant of {@link #findAttributeDefinition(String)}. */
    @VisibleForTesting
    default <T> @NotNull ResourceAttributeDefinition<T> findAttributeDefinitionRequired(String name) throws SchemaException {
        return findAttributeDefinitionRequired(
                new QName(MidPointConstants.NS_RI, name));
    }

    /**
     * Returns true if the object class has any index-only attributes.
     */
    default boolean hasIndexOnlyAttributes() {
        return getAttributeDefinitions().stream()
                .anyMatch(ItemDefinition::isIndexOnly);
    }

    /**
     * Returns true if there is an attribute with the given name defined.
     */
    default boolean containsAttributeDefinition(@NotNull QName attributeName) {
        return findAttributeDefinition(attributeName) != null;
    }

    /** Real values should have no duplicates. */
    @SuppressWarnings("unchecked")
    default <T> @NotNull ResourceAttribute<T> instantiateAttribute(@NotNull QName attrName, @NotNull T... realValues)
            throws SchemaException {
        //noinspection unchecked
        return ((ResourceAttributeDefinition<T>) findAttributeDefinitionRequired(attrName))
                .instantiateFromRealValues(List.of(realValues));
    }

    default @NotNull Collection<ItemName> getAllAttributesNames() {
        return getAttributeDefinitions(ResourceAttributeDefinition.class).stream()
                .map(ItemDefinition::getItemName)
                .toList();
    }
}
