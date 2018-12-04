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

package com.evolveum.midpoint.prism;

import javax.xml.namespace.QName;

/**
 *
 */
public interface MutablePrismContainerDefinition<C extends Containerable> extends PrismContainerDefinition<C>, MutableItemDefinition<PrismContainer<C>> {
	void setCompileTimeClass(Class<C> compileTimeClass);

	MutablePrismPropertyDefinition<?> createPropertyDefinition(QName name, QName propType, int minOccurs, int maxOccurs);
	MutablePrismPropertyDefinition<?> createPropertyDefinition(QName name, QName propType);
	MutablePrismPropertyDefinition<?> createPropertyDefinition(String localName, QName propType);

	MutablePrismContainerDefinition<?> createContainerDefinition(QName name, QName typeName, int minOccurs, int maxOccurs);
	MutablePrismContainerDefinition<?> createContainerDefinition(QName name, ComplexTypeDefinition ctd, int minOccurs, int maxOccurs);
}
