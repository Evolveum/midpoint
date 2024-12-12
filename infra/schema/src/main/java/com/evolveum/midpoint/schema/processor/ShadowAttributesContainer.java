/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

/**
 * TODO
 */
@SuppressWarnings("rawtypes")
public interface ShadowAttributesContainer extends ShadowItemsContainer, PrismContainer<ShadowAttributesType> {

    static ShadowAttributesContainerImpl createEmptyContainer(
            QName elementName, ResourceObjectDefinition resourceObjectDefinition) {
        ShadowAttributesContainerDefinition attributesContainerDefinition =
                new ShadowAttributesContainerDefinitionImpl(
                        elementName,
                        resourceObjectDefinition.getAttributesComplexTypeDefinition());
        return new ShadowAttributesContainerImpl(elementName, attributesContainerDefinition);
    }

    @Override
    ShadowAttributesContainerDefinition getDefinition();

    default @NotNull ShadowAttributesContainerDefinition getDefinitionRequired() {
        return MiscUtil.stateNonNull(
                getDefinition(),
                () -> "No definition in " + this);
    }

    default @NotNull ResourceObjectDefinition getResourceObjectDefinitionRequired() {
        ShadowAttributesContainerDefinition definition = getDefinitionRequired();
        return MiscUtil.stateNonNull(
                definition.getResourceObjectDefinition(),
                () -> "No resource object definition in " + definition);
    }

    /**
     * TODO review docs
     *
     * Returns set of resource object attributes.
     *
     * The order of attributes is insignificant.
     *
     * The returned set is immutable.
     *
     * @return set of resource object attributes.
     */
    @NotNull Collection<ShadowSimpleAttribute<?>> getSimpleAttributes();

    /** Returns a detached, immutable list. */
    @NotNull Collection<ShadowReferenceAttribute> getReferenceAttributes();

    @NotNull Collection<ShadowAttribute<?, ?, ?, ?>> getAttributes();

    void add(ShadowAttribute<?, ?, ?, ?> attribute) throws SchemaException;

    /**
     * This method exists just to avoid confusion between {@link #add(Item)} and {@link #add(ShadowAttribute)}
     * for values that conform to both of these signatures (e.g., {@link ShadowSimpleAttribute}).
     */
    default void addAttribute(ShadowAttribute<?, ?, ?, ?> attribute) throws SchemaException {
        add(attribute);
    }

//    /**
//     * Adds a {@link PrismProperty}, converting to {@link ResourceAttribute} if needed.
//     *
//     * Requires the resource object definition (i.e. complex type definition) be present.
//     */
//    void addAdoptedIfNeeded(@NotNull PrismProperty<?> attribute) throws SchemaException;

    /**
     * Returns a (single) primary identifier.
     *
     * This method returns a property that acts as an (primary) identifier for
     * the resource object. Primary identifiers are used to access the resource
     * objects, retrieve them from resource, identify objects for modifications,
     * etc.
     *
     * Returns null if no identifier is defined.
     *
     * Resource objects may have multiple (composite) identifiers, but this
     * method assumes that there is only a single identifier. The method will
     * throw exception if that assumption is not satisfied.
     *
     * @return identifier property
     * @throws IllegalStateException
     *             if resource object has multiple identifiers
     */
    PrismProperty<?> getPrimaryIdentifier();

    /**
     * TODO review docs
     *
     * Returns primary identifiers.
     *
     * This method returns properties that act as (primary) identifiers for the
     * resource object. Primary identifiers are used to access the resource
     * objects, retrieve them from resource, identify objects for modifications,
     * etc.
     *
     * Returns empty set if no identifier is defined. Must not return null.
     *
     * Resource objects may have multiple (composite) identifiers, all of them
     * are returned.
     *
     * The returned set it immutable! Any modifications will be lost.
     *
     * @return set of identifier properties
     */
    @NotNull Collection<ShadowSimpleAttribute<?>> getPrimaryIdentifiers();

    /**
     * TODO review docs
     *
     * Returns secondary identifiers.
     *
     * This method returns properties that act as secondary identifiers for the
     * resource object. Secondary identifiers are used to confirm primary
     * identification of resource object.
     *
     * Returns empty set if no identifier is defined. Must not return null.
     *
     * Resource objects may have multiple (composite) identifiers, all of them
     * are returned.
     *
     * @return set of secondary identifier properties
     */
    @NotNull Collection<ShadowSimpleAttribute<?>> getSecondaryIdentifiers();

    @NotNull Collection<ShadowSimpleAttribute<?>> getAllIdentifiers();

    /**
     * TODO review docs
     *
     * Specifies which resource attribute should be used as a "technical" name
     * for the account. This name will appear in log files and other troubleshooting
     * tools. The name should be a form of unique identifier that can be used to
     * locate the resource object for diagnostics. It should not contain white chars and
     * special chars if that can be avoided and it should be reasonable short.

     * It is different from a display name attribute. Display name is intended for a
     * common user or non-technical administrator (such as role administrator). The
     * naming attribute is intended for technical IDM administrators and developers.
     *
     * @return attribute that should be used as a "technical" name
     *                 for the account.
     */
    ShadowSimpleAttribute<String> getNamingAttribute();

    ShadowAttribute<?, ?, ?, ?> findAttribute(QName attrName);

    /**
     * Finds a specific attribute in the resource object by name.
     *
     * Returns null if nothing is found.
     *
     * @param attributeQName
     *            attribute name to find.
     * @return found attribute or null
     */
    <X> ShadowSimpleAttribute<X> findSimpleAttribute(QName attributeQName);

    ShadowReferenceAttribute findReferenceAttribute(QName attributeQName);

    default boolean containsAttribute(QName attributeName) {
        return findSimpleAttribute(attributeName) != null;
    }

    /**
     * Finds a specific attribute in the resource object by definition.
     *
     * Returns null if nothing is found.
     *
     * @param attributeDefinition
     *            attribute definition to find.
     * @return found attribute or null
     */
    <X> ShadowSimpleAttribute<X> findSimpleAttribute(ShadowSimpleAttributeDefinition attributeDefinition);

    <X> ShadowSimpleAttribute<X> findOrCreateSimpleAttribute(ShadowSimpleAttributeDefinition attributeDefinition) throws SchemaException;

    <X> ShadowSimpleAttribute<X> findOrCreateSimpleAttribute(QName attributeName) throws SchemaException;

    ShadowReferenceAttribute findOrCreateReferenceAttribute(QName attributeName) throws SchemaException;

    //ShadowAttribute<?, ?, ?, ?> findOrCreateAttribute(QName attributeName) throws SchemaException;

    default ShadowAttributesContainer addSimpleAttribute(QName attributeName, Object realValue) throws SchemaException {
        findOrCreateSimpleAttribute(attributeName)
                .setRealValue(realValue);
        return this;
    }

    default ShadowAttributesContainer addReferenceAttribute(QName attributeName, ShadowReferenceAttributeValue value)
            throws SchemaException {
        findOrCreateReferenceAttribute(attributeName).add(value);
        return this;
    }

    default ShadowAttributesContainer addReferenceAttribute(QName attributeName, AbstractShadow shadow, boolean full)
            throws SchemaException {
        return addReferenceAttribute(attributeName, ShadowReferenceAttributeValue.fromShadow(shadow, full));
    }

    <T> boolean contains(ShadowSimpleAttribute<T> attr);

    @Override
    ShadowAttributesContainer clone();

    void remove(ShadowAttribute<?, ?, ?, ?> item);

    void removeAttribute(@NotNull ItemName name);
}
