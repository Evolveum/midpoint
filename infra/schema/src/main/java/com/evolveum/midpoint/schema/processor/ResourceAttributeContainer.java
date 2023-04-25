/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * TODO
 */
@SuppressWarnings("rawtypes")
public interface ResourceAttributeContainer extends PrismContainer<ShadowAttributesType> {

    static ResourceAttributeContainer convertFromContainer(PrismContainer<?> origAttrContainer,
            ResourceObjectDefinition resourceObjectDefinition) throws SchemaException {
        if (origAttrContainer == null) {
            return null;
        }
        QName elementName = origAttrContainer.getElementName();
        ResourceAttributeContainer attributesContainer = createEmptyContainer(elementName, resourceObjectDefinition);
        for (Item item: origAttrContainer.getValue().getItems()) {
            if (item instanceof PrismProperty) {
                attributesContainer.add(
                        resourceObjectDefinition.propertyToAttribute((PrismProperty<?>) item));
            } else {
                throw new SchemaException("Cannot process item of type "+item.getClass().getSimpleName()+", attributes can only be properties");
            }
        }
        return attributesContainer;
    }

    static ResourceAttributeContainerImpl createEmptyContainer(QName elementName,
            ResourceObjectDefinition resourceObjectDefinition) {
        ResourceAttributeContainerDefinition attributesContainerDefinition =
                new ResourceAttributeContainerDefinitionImpl(
                        elementName,
                        resourceObjectDefinition);
        return new ResourceAttributeContainerImpl(elementName, attributesContainerDefinition);
    }

    @Override
    ResourceAttributeContainerDefinition getDefinition();

    default @NotNull ResourceObjectDefinition getResourceObjectDefinitionRequired() {
        ResourceAttributeContainerDefinition definition =
                MiscUtil.stateNonNull(
                        getDefinition(),
                        () -> "No definition in " + this);
        return MiscUtil.stateNonNull(
                definition.getComplexTypeDefinition(),
                () -> "No resource object definition in " + definition);
    }

    /**
     * TODO review docs
     *
     * Returns set of resource object attributes.
     *
     * The order of attributes is insignificant.
     *
     * The returned set is imutable! Any change to it will be ignored.
     *
     * @return set of resource object attributes.
     */
    @NotNull Collection<ResourceAttribute<?>> getAttributes();

    void add(ResourceAttribute<?> attribute) throws SchemaException;

    /**
     * Adds a {@link PrismProperty}, converting to {@link ResourceAttribute} if needed.
     *
     * Requires the resource object definition (i.e. complex type definition) be present.
     */
    void addAdoptedIfNeeded(@NotNull PrismProperty<?> attribute) throws SchemaException;

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
    Collection<ResourceAttribute<?>> getPrimaryIdentifiers();

    /**
     * TODO review docs
     *
     * Returns a (single) secondary identifier.
     *
     * This method returns a property that acts as an secondary identifier for
     * the resource object. Secondary identifiers are used to confirm primary
     * identification of resource object.
     *
     * Returns null if no secondary identifier is defined.
     *
     * Resource objects may have multiple (composite) identifiers, but this
     * method assumes that there is only a single identifier. The method will
     * throw exception if that assumption is not satisfied.
     *
     * @return secondary identifier property
     * @throws IllegalStateException
     *             if resource object has multiple secondary identifiers
     */
    <T> PrismProperty<T> getSecondaryIdentifier();

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
    Collection<ResourceAttribute<?>> getSecondaryIdentifiers();

    Collection<ResourceAttribute<?>> getAllIdentifiers();

    @NotNull
    Collection<ResourceAttribute<?>> extractAttributesByDefinitions(
            Collection<? extends ResourceAttributeDefinition> definitions);

    /**
     * TODO review docs
     *
     * Returns description attribute of a resource object.
     *
     * Returns null if there is no description attribute or the attribute is not
     * known.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return description attribute of a resource object.
     * @throws IllegalStateException
     *             if there is no definition for the referenced attributed
     */
    ResourceAttribute<String> getDescriptionAttribute();

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
    ResourceAttribute<String> getNamingAttribute();

    /**
     * TODO review docs
     *
     * Returns display name attribute of a resource object.
     *
     * Returns null if there is no display name attribute or the attribute is
     * not known.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return display name attribute of a resource object.
     * @throws IllegalStateException
     *             if there is no definition for the referenced attributed
     */
    ResourceAttribute getDisplayNameAttribute();

    /**
     * TODO review docs
     *
     * Returns the native object class string for the resource object.
     *
     * Native object class is the name of the Resource Object Definition (Object
     * Class) as it is seen by the resource itself. The name of the Resource
     * Object Definition may be constrained by XSD or other syntax and therefore
     * may be "mangled" to conform to such syntax. The <i>native object
     * class</i> value will contain unmangled name (if available).
     *
     * Returns null if there is no native object class or the native object
     * class is not known.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return native object class
     * @throws IllegalStateException
     *             if there is more than one description attribute.
     */
    String getNativeObjectClass();

    /**
     * TODO review docs
     * TODO consider removal
     *
     * Indicates whether definition is should be used as default account type.
     *
     * If true value is returned then the definition should be used as a default
     * account type definition. This is a way how a resource connector may
     * suggest applicable object classes (resource object definitions) for
     * accounts.
     *
     * If no information about account type is present, false should be
     * returned. This method must return true only if isAccountType() returns
     * true.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of at-most-one value should be done at the time of
     * schema parsing. The exception may not even be thrown at all if the
     * implementation is not able to determine duplicity.
     *
     * @return true if the definition should be used as account type.
     * @throws IllegalStateException
     *             if more than one default account is suggested in the schema.
     */
    boolean isDefaultInAKind();

    /**
     * Finds a specific attribute in the resource object by name.
     *
     * Returns null if nothing is found.
     *
     * @param attributeQName
     *            attribute name to find.
     * @return found attribute or null
     */
    <X> ResourceAttribute<X> findAttribute(QName attributeQName);

    default boolean containsAttribute(QName attributeName) {
        return findAttribute(attributeName) != null;
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
    <X> ResourceAttribute<X> findAttribute(ResourceAttributeDefinition attributeDefinition);

    <X> ResourceAttribute<X> findOrCreateAttribute(ResourceAttributeDefinition attributeDefinition) throws SchemaException;

    <X> ResourceAttribute<X> findOrCreateAttribute(QName attributeName) throws SchemaException;

    <T> boolean contains(ResourceAttribute<T> attr);

    @Override
    ResourceAttributeContainer clone();
}
