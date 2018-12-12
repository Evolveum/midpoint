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

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.DisplayableValue;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *  Factory for prism definitions (Definition and all its subtypes in prism-api).
 */
public interface DefinitionFactory {

	MutableComplexTypeDefinition createComplexTypeDefinition(QName name);

	<T> MutablePrismPropertyDefinition<T> createPropertyDefinition(QName name, QName typeName);

	MutablePrismReferenceDefinition createReferenceDefinition(QName name, QName typeName);

	MutablePrismContainerDefinition<?> createContainerDefinition(QName name, ComplexTypeDefinition ctd);

	<T> PrismPropertyDefinition<T> createPropertyDefinition(QName name, QName typeName, Collection<? extends DisplayableValue<T>> allowedValues, T defaultValue);
}
