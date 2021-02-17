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
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *
 */
@SuppressWarnings("rawtypes")
public interface ResourceAttributeContainer extends PrismContainer<ShadowAttributesType> {

    static ResourceAttributeContainer convertFromContainer(PrismContainer<?> origAttrContainer,
            ObjectClassComplexTypeDefinition objectClassDefinition) throws SchemaException {
        if (origAttrContainer == null || origAttrContainer.getValue() == null) {
            return null;
        }
        QName elementName = origAttrContainer.getElementName();
        ResourceAttributeContainer attributesContainer = createEmptyContainer(elementName, objectClassDefinition);
        for (Item item: origAttrContainer.getValue().getItems()) {
            if (item instanceof PrismProperty) {
                PrismProperty<?> property = (PrismProperty)item;
                QName attributeName = property.getElementName();
                ResourceAttributeDefinition attributeDefinition = objectClassDefinition.findAttributeDefinition(attributeName);
                if (attributeDefinition == null) {
                    throw new SchemaException("No definition for attribute "+attributeName+" in object class "+objectClassDefinition);
                }
                ResourceAttribute attribute = new ResourceAttributeImpl(attributeName, attributeDefinition , property.getPrismContext());
                for(PrismPropertyValue pval: property.getValues()) {
                    attribute.add(pval.clone());
                }
                attributesContainer.add(attribute);
                attribute.applyDefinition(attributeDefinition);
            } else {
                throw new SchemaException("Cannot process item of type "+item.getClass().getSimpleName()+", attributes can only be properties");
            }
        }
        return attributesContainer;
    }

    static ResourceAttributeContainerImpl createEmptyContainer(QName elementName,
            ObjectClassComplexTypeDefinition objectClassDefinition) {
        ResourceAttributeContainerDefinition attributesContainerDefinition = new ResourceAttributeContainerDefinitionImpl(elementName,
                objectClassDefinition, objectClassDefinition.getPrismContext());
        return new ResourceAttributeContainerImpl(elementName, attributesContainerDefinition , objectClassDefinition.getPrismContext());
    }

    @Override
    ResourceAttributeContainerDefinition getDefinition();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    Collection<ResourceAttribute<?>> getAttributes();

    void add(ResourceAttribute<?> attribute) throws SchemaException;

    PrismProperty<?> getPrimaryIdentifier();

    Collection<ResourceAttribute<?>> getPrimaryIdentifiers();

    <T> PrismProperty<T> getSecondaryIdentifier();

    Collection<ResourceAttribute<?>> getSecondaryIdentifiers();

    Collection<ResourceAttribute<?>> getAllIdentifiers();

    @NotNull
    Collection<ResourceAttribute<?>> extractAttributesByDefinitions(
            Collection<? extends ResourceAttributeDefinition> definitions);

    ResourceAttribute<String> getDescriptionAttribute();

    ResourceAttribute<String> getNamingAttribute();

    ResourceAttribute getDisplayNameAttribute();

    String getNativeObjectClass();

    ShadowKindType getKind();

    boolean isDefaultInAKind();

    @SuppressWarnings("unchecked")
    <X> ResourceAttribute<X> findAttribute(QName attributeQName);

    <X> ResourceAttribute<X> findAttribute(ResourceAttributeDefinition attributeDefinition);

    <X> ResourceAttribute<X> findOrCreateAttribute(ResourceAttributeDefinition attributeDefinition) throws SchemaException;

    <X> ResourceAttribute<X> findOrCreateAttribute(QName attributeName) throws SchemaException;

    <T> boolean contains(ResourceAttribute<T> attr);

    @Override
    ResourceAttributeContainer clone();
}
