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

import com.evolveum.midpoint.util.DisplayableValue;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *
 */
public class DefinitionFactoryImpl implements DefinitionFactory {

	@NotNull private final PrismContextImpl prismContext;

	public DefinitionFactoryImpl(@NotNull PrismContextImpl prismContext) {
		this.prismContext = prismContext;
	}

	@Override
	public ComplexTypeDefinitionImpl createComplexTypeDefinition(QName name) {
		return new ComplexTypeDefinitionImpl(name, prismContext);
	}

	@Override
	public MutablePrismPropertyDefinition createPropertyDefinition(QName name, QName typeName) {
		return new PrismPropertyDefinitionImpl<>(name, typeName, prismContext);
	}

	@Override
	public MutablePrismReferenceDefinition createReferenceDefinition(QName name, QName typeName) {
		return new PrismReferenceDefinitionImpl(name, typeName, prismContext);
	}

	@Override
	public MutablePrismContainerDefinition<?> createContainerDefinition(QName name, ComplexTypeDefinition ctd) {
		return new PrismContainerDefinitionImpl<>(name, ctd, prismContext);
	}

	@Override
	public <T> MutablePrismPropertyDefinition<T> createPropertyDefinition(QName name, QName typeName,
			Collection<? extends DisplayableValue<T>> allowedValues, T defaultValue) {
		return new PrismPropertyDefinitionImpl<>(name, typeName, prismContext, allowedValues, defaultValue);
	}
}
