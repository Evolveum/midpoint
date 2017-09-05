/**
 * Copyright (c) 2015-2016 Evolveum
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ResourceObjectIdentification implements Serializable {
	private static final long serialVersionUID = 1L;

	private ObjectClassComplexTypeDefinition objectClassDefinition;
	private Collection<? extends ResourceAttribute<?>> primaryIdentifiers;
	private Collection<? extends ResourceAttribute<?>> secondaryIdentifiers;
	// TODO: identification strategy

	public ResourceObjectIdentification(ObjectClassComplexTypeDefinition objectClassDefinition,
			Collection<? extends ResourceAttribute<?>> primaryIdentifiers,
			Collection<? extends ResourceAttribute<?>> secondaryIdentifiers) {
		this.objectClassDefinition = objectClassDefinition;
		this.primaryIdentifiers = primaryIdentifiers;
		this.secondaryIdentifiers = secondaryIdentifiers;
	}

	public Collection<? extends ResourceAttribute<?>> getPrimaryIdentifiers() {
		return primaryIdentifiers;
	}

	public <T> ResourceAttribute<T> getPrimaryIdentifier() throws SchemaException {
		if (primaryIdentifiers == null || primaryIdentifiers.isEmpty()) {
			return null;
		}
		if (primaryIdentifiers.size() > 1) {
			throw new SchemaException("More than one primary identifier in "+this);
		}
		return (ResourceAttribute<T>) primaryIdentifiers.iterator().next();
	}

	public Collection<? extends ResourceAttribute<?>> getSecondaryIdentifiers() {
		return secondaryIdentifiers;
	}

	public <T> ResourceAttribute<T> getSecondaryIdentifier() throws SchemaException {
		if (secondaryIdentifiers == null || secondaryIdentifiers.isEmpty()) {
			return null;
		}
		if (secondaryIdentifiers.size() > 1) {
			throw new SchemaException("More than one secondary identifier in "+this);
		}
		return (ResourceAttribute<T>) secondaryIdentifiers.iterator().next();
	}


	public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
		return objectClassDefinition;
	}

	public static ResourceObjectIdentification create(ObjectClassComplexTypeDefinition objectClassDefinition,
			Collection<? extends ResourceAttribute<?>> allIdentifiers) throws SchemaException {
		if (allIdentifiers == null) {
			throw new IllegalArgumentException("Cannot create ResourceObjectIdentification with null identifiers");
		}
		Collection<? extends ResourceAttribute<?>> primaryIdentifiers =  null;
		Collection<? extends ResourceAttribute<?>> secondaryIdentifiers = null;
		for (ResourceAttribute<?> identifier: allIdentifiers) {
			if (objectClassDefinition.isPrimaryIdentifier(identifier.getElementName())) {
				if (primaryIdentifiers == null) {
					primaryIdentifiers = new ArrayList<>();
				}
				((Collection)primaryIdentifiers).add(identifier);
			} else if (objectClassDefinition.isSecondaryIdentifier(identifier.getElementName())) {
				if (secondaryIdentifiers == null) {
					secondaryIdentifiers = new ArrayList<>();
				}
				((Collection)secondaryIdentifiers).add(identifier);
			} else {
				throw new SchemaException("Attribute "+identifier+" is neither primary not secondary identifier in object class "+objectClassDefinition);
			}
		}
		return new ResourceObjectIdentification(objectClassDefinition, primaryIdentifiers, secondaryIdentifiers);
	}

	public static ResourceObjectIdentification createFromAttributes(ObjectClassComplexTypeDefinition objectClassDefinition,
			Collection<? extends ResourceAttribute<?>> attributes) throws SchemaException {
		Collection<? extends ResourceAttribute<?>> primaryIdentifiers =  null;
		Collection<? extends ResourceAttribute<?>> secondaryIdentifiers = null;
		for (ResourceAttribute<?> identifier: attributes) {
			if (objectClassDefinition.isPrimaryIdentifier(identifier.getElementName())) {
				if (primaryIdentifiers == null) {
					primaryIdentifiers = new ArrayList<>();
				}
				((Collection)primaryIdentifiers).add(identifier);
			} else if (objectClassDefinition.isSecondaryIdentifier(identifier.getElementName())) {
				if (secondaryIdentifiers == null) {
					secondaryIdentifiers = new ArrayList<>();
				}
				((Collection)secondaryIdentifiers).add(identifier);
			}
		}
		return new ResourceObjectIdentification(objectClassDefinition, primaryIdentifiers, secondaryIdentifiers);
	}

	public static ResourceObjectIdentification createFromShadow(ObjectClassComplexTypeDefinition objectClassDefinition,
			ShadowType shadowType) throws SchemaException {
		return createFromAttributes(objectClassDefinition, ShadowUtil.getAttributes(shadowType));
	}

	public void validatePrimaryIdenfiers() {
		if (!hasPrimaryIdentifiers()) {
			throw new IllegalStateException("No primary identifiers in " + this);
		}
	}

	public boolean hasPrimaryIdentifiers() {
		return primaryIdentifiers != null && !primaryIdentifiers.isEmpty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((primaryIdentifiers == null) ? 0 : primaryIdentifiers.hashCode());
		result = prime * result + ((objectClassDefinition == null) ? 0 : objectClassDefinition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceObjectIdentification other = (ResourceObjectIdentification) obj;
		if (primaryIdentifiers == null) {
			if (other.primaryIdentifiers != null)
				return false;
		} else if (!primaryIdentifiers.equals(other.primaryIdentifiers))
			return false;
		if (objectClassDefinition == null) {
			if (other.objectClassDefinition != null)
				return false;
		} else if (!objectClassDefinition.equals(other.objectClassDefinition))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ResourceObjectIdentification(" + PrettyPrinter.prettyPrint(objectClassDefinition.getTypeName())
				+ ": primary=" + primaryIdentifiers + ", secondary=" + secondaryIdentifiers + ")";
	}

}
