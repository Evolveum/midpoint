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

package com.evolveum.midpoint.prism.schema;

import com.evolveum.midpoint.prism.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * Used to retrieve definition from 'global definition store' - i.e. store that contains a group of related definition(s),
 * sharing e.g. a common namespace. Such stores are prism schemas and schema registry itself.
 *
 * Note: although all of these methods are '@Nullable', we don't mark them as such, to avoid false 'may produce NPE'
 * warnings for cases that will never produce nulls (like searching for known items/CTDs).
 *
 * @author mederly
 */
@SuppressWarnings("unused")
public interface GlobalDefinitionsStore extends DefinitionsStore {

	// new API

//	@Override
//	GlobalDefinitionSearchContext<ItemDefinition<?>> findItemDefinition();
//
//	@Override
//	GlobalDefinitionSearchContext<PrismPropertyDefinition> findPropertyDefinition();
//
//	@Override
//	GlobalDefinitionSearchContext<PrismReferenceDefinition> findReferenceDefinition();
//
//	@Override
//	GlobalDefinitionSearchContext<PrismContainerDefinition<? extends Containerable>> findContainerDefinition();
//
//	GlobalDefinitionSearchContext<PrismObjectDefinition<? extends Objectable>> findObjectDefinition();
//
//	GlobalDefinitionSearchContext<ComplexTypeDefinition> findComplexTypeDefinition();

	// old API

	// PrismObject-related

	// core methods

	<CD extends PrismContainerDefinition> CD findContainerDefinitionByCompileTimeClass(
			@NotNull Class<? extends Containerable> compileTimeClass, @NotNull Class<CD> definitionClass);

	<ID extends ItemDefinition> ID findItemDefinitionByType(@NotNull QName typeName, @NotNull Class<ID> definitionClass);

	<ID extends ItemDefinition> ID findItemDefinitionByElementName(@NotNull QName elementName, @NotNull Class<ID> definitionClass);

	<C extends Containerable> ComplexTypeDefinition findComplexTypeDefinitionByCompileTimeClass(@NotNull Class<C> compileTimeClass);

	ComplexTypeDefinition findComplexTypeDefinitionByType(@NotNull QName typeName);

	// non-core (derived) methods

	@SuppressWarnings("unchecked")
	default <O extends Objectable> PrismObjectDefinition<O> findObjectDefinitionByCompileTimeClass(@NotNull Class<O> compileTimeClass) {
		return findContainerDefinitionByCompileTimeClass(compileTimeClass, PrismObjectDefinition.class);
	}

	@SuppressWarnings("unchecked")
	default <O extends Objectable> PrismObjectDefinition<O> findObjectDefinitionByType(@NotNull QName typeName) {
		return findItemDefinitionByType(typeName, PrismObjectDefinition.class);
	}

	@SuppressWarnings("unchecked")
	default <O extends Objectable> PrismObjectDefinition<O> findObjectDefinitionByElementName(@NotNull QName elementName) {
		return findItemDefinitionByElementName(elementName, PrismObjectDefinition.class);
	}

	// PrismContainer-related

	@SuppressWarnings("unchecked")
	default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByCompileTimeClass(@NotNull Class<C> compileTimeClass) {
		return findContainerDefinitionByCompileTimeClass(compileTimeClass, PrismContainerDefinition.class);
	}

	@SuppressWarnings("unchecked")
	default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByType(@NotNull QName typeName) {
		return findItemDefinitionByType(typeName, PrismContainerDefinition.class);
	}

	@SuppressWarnings("unchecked")
	default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByElementName(@NotNull QName elementName) {
		return findItemDefinitionByElementName(elementName, PrismContainerDefinition.class);
	}

	// PrismReference-related

	default PrismReferenceDefinition findReferenceDefinitionByElementName(@NotNull QName elementName) {
		return findItemDefinitionByElementName(elementName, PrismReferenceDefinition.class);
	}

	// PrismProperty-related

	default PrismPropertyDefinition findPropertyDefinitionByElementName(@NotNull QName elementName) {
		return findItemDefinitionByElementName(elementName, PrismPropertyDefinition.class);
	}

	// Item-related

	default ItemDefinition findItemDefinitionByType(@NotNull QName typeName) {
		return findItemDefinitionByType(typeName, ItemDefinition.class);
	}

	default ItemDefinition findItemDefinitionByElementName(@NotNull QName elementName) {
		return findItemDefinitionByElementName(elementName, ItemDefinition.class);
	}

	@Deprecated
	default <ID extends ItemDefinition> ID findItemDefinition(@NotNull String localElementName, @NotNull Class<ID> definitionClass) {
		return findItemDefinitionByElementName(new QName(localElementName), definitionClass);
	}

	@Deprecated
	default <ID extends ItemDefinition> ID findItemDefinition(@NotNull QName elementName, @NotNull Class<ID> definitionClass) {
		return findItemDefinitionByElementName(elementName, definitionClass);
	}

	// ComplexTypeDefinition-related

	@Deprecated default ComplexTypeDefinition findComplexTypeDefinition(@NotNull QName typeName) {
		return findComplexTypeDefinitionByType(typeName);
	}


}
