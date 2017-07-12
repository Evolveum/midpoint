/*
 * Copyright (c) 2010-2017 Evolveum
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
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class DefinitionSearchContextItemImpl<ID extends ItemDefinition> implements GlobalDefinitionSearchContext<ID> {

//	private final DefinitionSearchImplementation provider;
//	private final Class<? extends ID> definitionClass;
//
//	public DefinitionSearchContextItemImpl(DefinitionSearchImplementation provider, Class<? extends ID> definitionClass) {
//		this.provider = provider;
//		this.definitionClass = definitionClass;
//	}
//
//	@Override
//	public ID byElementName(@NotNull QName elementName) {
//		return provider.findItemDefinition(elementName, definitionClass);
//	}
//
//	@Override
//	public ID byType(@NotNull QName type) {
//		return provider.findItemDefinitionByType(type, definitionClass);
//	}
//
//	@Override
//	public <C extends Containerable> ID byCompileTimeClass(@NotNull Class<C> clazz) {
//		if (PrismObjectDefinition.class.isAssignableFrom(definitionClass)) {
//			return (ID) provider.findObjectDefinitionByCompileTimeClass((Class<Objectable>) clazz);
//		} else if (PrismContainerDefinition.class.isAssignableFrom(definitionClass)) {
//			return (ID) provider.findItemDefinitionByCompileTimeClass(clazz);
//		} else {
//			throw new UnsupportedOperationException("Only containers and prism objects can be searched by compile-time class. Not "
//				+ definitionClass + " by " + clazz);
//		}
//	}
}
