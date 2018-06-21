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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;


/**
 * Resource Object.
 *
 * Resource Object understands resource-specific annotations, such as
 * identifiers, native object class, etc.
 *
 * Object class can be determined by using the definition (inherited from
 * PropertyContainer)
 *
 * @author Radovan Semancik
 *
 */
@SuppressWarnings("rawtypes")
public final class ResourceAttributeContainer extends PrismContainer {
    private static final long serialVersionUID = 8878851067509560312L;

    /**
	 * The constructors should be used only occasionally (if used at all).
	 * Use the factory methods in the ResourceObjectDefintion instead.
	 */
	public ResourceAttributeContainer(QName name, ResourceAttributeContainerDefinition definition, PrismContext prismContext) {
		super(name, definition, prismContext);
	}

	@Override
	public ResourceAttributeContainerDefinition getDefinition() {
        PrismContainerDefinition prismContainerDefinition = super.getDefinition();
        if (prismContainerDefinition == null) {
        	return null;
        }
        if (prismContainerDefinition instanceof ResourceAttributeContainerDefinition) {
		    return (ResourceAttributeContainerDefinition) prismContainerDefinition;
        } else {
            throw new IllegalStateException("definition should be " + ResourceAttributeContainerDefinition.class + " but it is " + prismContainerDefinition.getClass() + " instead; definition = " + prismContainerDefinition.debugDump(0));
        }
	}

	/**
	 * Returns set of resource object attributes.
	 *
	 * The order of attributes is insignificant.
	 *
	 * The returned set is imutable! Any change to it will be ignored.
	 *
	 * @return set of resource object attributes.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Collection<ResourceAttribute<?>> getAttributes() {
		// TODO: Iterate over the list to assert correct types
		return (Set) getValue().getProperties();
	}

	public void add(ResourceAttribute<?> attribute) throws SchemaException {
		super.add(attribute);
	}

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
	public PrismProperty<?> getPrimaryIdentifier() {
		Collection<ResourceAttribute<?>> attrDefs = getPrimaryIdentifiers();
		if (attrDefs.size() > 1){
			throw new IllegalStateException("Resource object has more than one identifier.");
		}

		for (PrismProperty<?> p : attrDefs){
			return p;
		}

		return null;
	}

	/**
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
	public Collection<ResourceAttribute<?>> getPrimaryIdentifiers() {
		return extractAttributesByDefinitions(getDefinition().getPrimaryIdentifiers());
	}

	/**
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
	public <T> PrismProperty<T> getSecondaryIdentifier() {
		Collection<ResourceAttribute<?>> secondaryIdentifiers = getSecondaryIdentifiers();
		if (secondaryIdentifiers.size() > 1){
			throw new IllegalStateException("Resource object has more than one identifier.");
		}
		for (PrismProperty<?> p : secondaryIdentifiers){
			return (PrismProperty<T>) p;
		}
		return null;
	}

	/**
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
	public Collection<ResourceAttribute<?>> getSecondaryIdentifiers() {
		return extractAttributesByDefinitions(getDefinition().getSecondaryIdentifiers());
	}

	public Collection<ResourceAttribute<?>> getAllIdentifiers() {
		return extractAttributesByDefinitions(getDefinition().getAllIdentifiers());
	}

	private Collection<ResourceAttribute<?>> extractAttributesByDefinitions(Collection<? extends ResourceAttributeDefinition> definitions) {
		Collection<ResourceAttribute<?>> attributes = new ArrayList<>(definitions.size());
		for (ResourceAttributeDefinition attrDef : definitions) {
			for (ResourceAttribute<?> property : getAttributes()){
				if (attrDef.getName().equals(property.getElementName())){
					property.setDefinition(attrDef);
					attributes.add(property);
				}
			}
		}
		return attributes;
	}

	/**
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
	public ResourceAttribute<String> getDescriptionAttribute() {
		if (getDefinition() == null) {
			return null;
		}
		return findAttribute(getDefinition().getDisplayNameAttribute());
	}

	/**
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
	 * 				for the account.
	 */
	public ResourceAttribute<String> getNamingAttribute() {
		if (getDefinition() == null) {
			return null;
		}
		if (getDefinition().getNamingAttribute()==null) {
			return null;
		}
		return findAttribute(getDefinition().getNamingAttribute());
	}

	/**
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
	public ResourceAttribute getDisplayNameAttribute() {
		if (getDefinition() == null) {
			return null;
		}
		return findAttribute(getDefinition().getDisplayNameAttribute());
	}

	/**
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
	public String getNativeObjectClass() {
		return getDefinition() == null ? null : getDefinition().getNativeObjectClass();
	}

	public ShadowKindType getKind() {
		ResourceAttributeContainerDefinition definition = getDefinition();
		return (definition != null ? definition.getKind() : null);
	}

	/**
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
	public boolean isDefaultInAKind() {
		ResourceAttributeContainerDefinition definition = getDefinition();
		return (definition != null ? definition.isDefaultInAKind() : null);
	}

	/**
	 * Finds a specific attribute in the resource object by name.
	 *
	 * Returns null if nothing is found.
	 *
	 * @param attributeQName
	 *            attribute name to find.
	 * @return found attribute or null
	 */
	@SuppressWarnings("unchecked")
	public <X> ResourceAttribute<X> findAttribute(QName attributeQName) {
		return (ResourceAttribute<X>) super.findProperty(attributeQName);
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
	public <X> ResourceAttribute<X> findAttribute(ResourceAttributeDefinition attributeDefinition) {
		return (ResourceAttribute<X>) getValue().findProperty(attributeDefinition);
	}

	public <X> ResourceAttribute<X> findOrCreateAttribute(ResourceAttributeDefinition attributeDefinition) throws SchemaException {
		return (ResourceAttribute<X>) getValue().findOrCreateProperty(attributeDefinition);
	}

	public <X> ResourceAttribute<X> findOrCreateAttribute(QName attributeName) throws SchemaException {
		return (ResourceAttribute<X>) getValue().findOrCreateProperty(attributeName);
	}

	public <T> boolean contains(ResourceAttribute<T> attr) {
		return getValue().contains(attr);
	}

	public static ResourceAttributeContainer convertFromContainer(PrismContainer<?> origAttrContainer,
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
				ResourceAttribute attribute = new ResourceAttribute(attributeName, attributeDefinition , property.getPrismContext());
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

	public static ResourceAttributeContainer createEmptyContainer(QName elementName, ObjectClassComplexTypeDefinition objectClassDefinition) {
		ResourceAttributeContainerDefinition attributesContainerDefinition = new ResourceAttributeContainerDefinitionImpl(elementName,
				objectClassDefinition, objectClassDefinition.getPrismContext());
		return new ResourceAttributeContainer(elementName, attributesContainerDefinition , objectClassDefinition.getPrismContext());
	}

	@Override
	public ResourceAttributeContainer clone() {
		return cloneComplex(CloneStrategy.LITERAL);
	}
	
	@Override
	public ResourceAttributeContainer cloneComplex(CloneStrategy strategy) {
		ResourceAttributeContainer clone = new ResourceAttributeContainer(getElementName(), getDefinition(), getPrismContext());
		copyValues(strategy, clone);
		return clone;
	}

	protected void copyValues(CloneStrategy strategy, ResourceAttributeContainer clone) {
		super.copyValues(strategy, clone);
		// Nothing to copy
	}


	@Override
	public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw,
			ConsistencyCheckScope scope) {
		super.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
		List<PrismContainerValue> values = getValues();
		if (values == null) {
			throw new IllegalStateException("Null values in ResourceAttributeContainer");
		}
		if (values.isEmpty()) {
			return;
		}
		if (values.size() > 1) {
			throw new IllegalStateException(values.size()+" values in ResourceAttributeContainer, expected just one");
		}
		PrismContainerValue value = values.get(0);
		List<Item<?,?>> items = value.getItems();
		if (items == null) {
			return;
		}
		for (Item item: items) {
			if (!(item instanceof ResourceAttribute)) {
				throw new IllegalStateException("Found illegal item in ResourceAttributeContainer: "+item+" ("+item.getClass()+")");
			}
		}
	}

	/**
	 * Return a human readable name of this class suitable for logs.
	 */
	protected String getDebugDumpClassName() {
		return "RAC";
	}

}
