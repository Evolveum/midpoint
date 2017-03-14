/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface ResourceAttributeDefinition<T> extends PrismPropertyDefinition<T> {

	@NotNull
	ResourceAttribute<T> instantiate();

	@NotNull
	ResourceAttribute<T> instantiate(QName name);

	Boolean getReturnedByDefault();

	boolean isReturnedByDefault();

	boolean isIdentifier(ResourceAttributeContainerDefinition objectDefinition);

	boolean isIdentifier(ObjectClassComplexTypeDefinition objectDefinition);

	boolean isSecondaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition);

	String getNativeAttributeName();

	String getFrameworkAttributeName();

	@NotNull
	@Override
	ResourceAttributeDefinition<T> clone();
}
