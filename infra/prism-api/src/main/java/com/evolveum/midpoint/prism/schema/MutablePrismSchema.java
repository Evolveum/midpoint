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

package com.evolveum.midpoint.prism.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 *
 */
public interface MutablePrismSchema extends PrismSchema {

	void setNamespace(@NotNull String namespace);

	void add(@NotNull Definition def);

	// used for connector and resource schemas
	void parseThis(Element element, boolean isRuntime, String shortDescription, PrismContext prismContext) throws SchemaException;

	MutablePrismContainerDefinition createPropertyContainerDefinition(String localTypeName);

	MutablePrismContainerDefinition createPropertyContainerDefinition(String localElementName, String localTypeName);

	ComplexTypeDefinition createComplexTypeDefinition(QName typeName);

	PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName);

	PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName);

	void addDelayedItemDefinition(DefinitionSupplier o);
}
