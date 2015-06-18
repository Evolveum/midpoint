/**
 * Copyright (c) 2015 Evolveum
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

import java.util.Collection;

import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * @author semancik
 *
 */
public class ResourceObjectIdentification {
	
	private ObjectClassComplexTypeDefinition objectClassDefinition;
	private Collection<? extends ResourceAttribute<?>> identifiers;
	// TODO: identification strategy
	
	public ResourceObjectIdentification(ObjectClassComplexTypeDefinition objectClassDefinition, Collection<? extends ResourceAttribute<?>> identifiers) {
		this.objectClassDefinition = objectClassDefinition;
		this.identifiers = identifiers;
	}

	public Collection<? extends ResourceAttribute<?>> getIdentifiers() {
		return identifiers;
	}

	public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
		return objectClassDefinition;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identifiers == null) ? 0 : identifiers.hashCode());
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
		if (identifiers == null) {
			if (other.identifiers != null)
				return false;
		} else if (!identifiers.equals(other.identifiers))
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
				+ ": " + identifiers + ")";
	}
	
}
