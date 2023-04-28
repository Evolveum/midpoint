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
    @NotNull
    List<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions();

    /**
     * Returns all attribute definitions of given type as an unmodifiable collection.
     *
     */
    @NotNull
    default <AD extends ResourceAttributeDefinition<?>> Collection<AD> getAttributeDefinitions(Class<AD> type) {
        //noinspection unchecked
        return getAttributeDefinitions().stream()
                .filter(def -> type.isAssignableFrom(def.getClass()))
                .map(def -> (AD) def)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Finds a definition of an attribute with a given name. Returns null if nothing is found.
     */
    @Nullable
    default ResourceAttributeDefinition<?> findAttributeDefinition(QName name) {
        return findAttributeDefinition(name, false);
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link SchemaException} if it's not there.
     */
    @NotNull
    default ResourceAttributeDefinition<?> findAttributeDefinitionRequired(@NotNull QName name)
            throws SchemaException {
        return findAttributeDefinitionRequired(name, () -> "");
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link IllegalStateException} if it's not there.
     */
    @NotNull
    default ResourceAttributeDefinition<?> findAttributeDefinitionStrictlyRequired(@NotNull QName name) {
        return findAttributeDefinitionStrictlyRequired(name, () -> "");
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link SchemaException} if it's not there.
     */
    @NotNull
    default ResourceAttributeDefinition<?> findAttributeDefinitionRequired(
            @NotNull QName name, @NotNull Supplier<String> contextSupplier)
            throws SchemaException {
        return MiscUtil.requireNonNull(
                findAttributeDefinition(name),
                () -> new SchemaException("no definition of attribute " + name + " in " + this + contextSupplier.get()));
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link IllegalStateException} if it's not there.
     */
    @NotNull
    default ResourceAttributeDefinition<?> findAttributeDefinitionStrictlyRequired(
            @NotNull QName name, @NotNull Supplier<String> contextSupplier) {
        return MiscUtil.requireNonNull(
                findAttributeDefinition(name),
                () -> new IllegalStateException("no definition of attribute " + name + " in " + this + contextSupplier.get()));
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
    @Nullable
    default ResourceAttributeDefinition<?> findAttributeDefinition(QName name, boolean caseInsensitive) {
        return findLocalItemDefinition(
                ItemName.fromQName(name), ResourceAttributeDefinition.class, caseInsensitive);
    }

    /**
     * Finds attribute definition using local name only.
     *
     * BEWARE: Ignores attributes in namespaces other than "ri:" (e.g. icfs:uid and icfs:name).
     */
    @VisibleForTesting
    default ResourceAttributeDefinition<?> findAttributeDefinition(String name) {
        return findAttributeDefinition(
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

    default Collection<? extends QName> getNamesOfAttributesWithOutboundExpressions() {
        return getAttributeDefinitions().stream()
                .filter(attrDef -> attrDef.getOutboundMappingBean() != null)
                .map(ItemDefinition::getItemName)
                .collect(Collectors.toCollection(HashSet::new));
    }

    default Collection<? extends QName> getNamesOfAttributesWithInboundExpressions() {
        return getAttributeDefinitions().stream()
                .filter(attrDef -> !attrDef.getInboundMappingBeans().isEmpty())
                .map(ItemDefinition::getItemName)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Converts a {@link PrismProperty} into corresponding {@link ResourceAttribute}.
     * Used in the process of "definition application" in `applyDefinitions` and similar methods.
     */
    default <T> @NotNull ResourceAttribute<T> propertyToAttribute(PrismProperty<T> property)
            throws SchemaException {
        QName attributeName = property.getElementName();
        //noinspection unchecked
        ResourceAttributeDefinition<T> attributeDefinition =
                (ResourceAttributeDefinition<T>) findAttributeDefinition(attributeName);
        if (attributeDefinition == null) {
            throw new SchemaException("No definition for attribute " + attributeName + " in " + this);
        }
        ResourceAttribute<T> attribute = new ResourceAttributeImpl<>(attributeName, attributeDefinition);
        for (PrismPropertyValue<T> pval : property.getValues()) {
            // MID-5833 This is manual copy process, could we could assume original property is correctly constructed
            attribute.addIgnoringEquivalents(pval.clone());
        }
        attribute.applyDefinition(attributeDefinition);
        attribute.setIncomplete(property.isIncomplete());
        return attribute;
    }
}
