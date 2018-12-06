/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *
 */
@SuppressWarnings("rawtypes")
public interface ResourceAttributeContainer extends PrismContainer {

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
