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

import com.evolveum.midpoint.prism.PrismContext;

import javax.xml.namespace.QName;

/**
 *  EXPERIMENTAL
 */
public class ObjectFactory {

	public static <T> ResourceAttribute<T> createResourceAttribute(QName name, ResourceAttributeDefinition<T> definition, PrismContext prismContext) {
		return new ResourceAttributeImpl<>(name, definition, prismContext);
	}

	public static <T> MutableResourceAttributeDefinition<T> createResourceAttributeDefinition(QName name, QName typeName,
			PrismContext prismContext) {
		return new ResourceAttributeDefinitionImpl<T>(name, typeName, prismContext);
	}

	public static ResourceAttributeContainer createResourceAttributeContainer(QName name, ResourceAttributeContainerDefinition definition,
			PrismContext prismContext) {
		return new ResourceAttributeContainerImpl(name, definition, prismContext);
	}

	public static ResourceAttributeContainerDefinition createResourceAttributeContainerDefinition(QName name,
			ObjectClassComplexTypeDefinition complexTypeDefinition, PrismContext prismContext) {
		return new ResourceAttributeContainerDefinitionImpl(name, complexTypeDefinition, prismContext);
	}

	public static MutableResourceSchema createResourceSchema(String namespace, PrismContext prismContext) {
		return new ResourceSchemaImpl(namespace, prismContext);
	}
}
