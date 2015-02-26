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
	
}
