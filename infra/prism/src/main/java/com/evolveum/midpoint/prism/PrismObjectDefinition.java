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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface PrismObjectDefinition<O extends Objectable> extends PrismContainerDefinition<O> {

	@Override
	@NotNull
	PrismObject<O> instantiate() throws SchemaException;

	@Override
	@NotNull
	PrismObject<O> instantiate(QName name) throws SchemaException;

	@NotNull
	PrismObjectDefinition<O> clone();

	@Override
	PrismObjectDefinition<O> deepClone(boolean ultraDeep);

	PrismObjectDefinition<O> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition);

	PrismContainerDefinition<?> getExtensionDefinition();

	@Override
	PrismObjectValue<O> createValue();
}
