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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * Used to retrieve definition from 'local definition store' - i.e. store that contains definition(s) related to one parent item.
 * Such stores are prism containers and complex types.
 *
 * Before midPoint 3.5, some of these methods tried to resolve definitions globally (if the store could contain 'any' definitions).
 * However, starting from 3.5, this is a responsibility of a client. It can call methods in SchemaRegistry to help with that.
 *
 * Note: Although these methods can return null, they are not marked as @Nullable. It is because we want avoid false warnings
 * about possible NPEs when used e.g. to find definitions that certainly exist (like c:user etc).
 *
 * @author mederly
 */
public interface LocalDefinitionStore {

	// (1) single-name resolution

	// (1a) core
	<ID extends ItemDefinition> ID findItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz, boolean caseInsensitive);

	// (1b) derived
	@SuppressWarnings("unchecked")
	default <ID extends ItemDefinition> ID findItemDefinition(@NotNull QName name) {
		return (ID) findItemDefinition(name, ItemDefinition.class);
	}

	@SuppressWarnings("unchecked")
	default <T> PrismPropertyDefinition<T> findPropertyDefinition(@NotNull QName name) {
		return findItemDefinition(name, PrismPropertyDefinition.class);
	}

	@SuppressWarnings("unchecked")
	default PrismReferenceDefinition findReferenceDefinition(@NotNull QName name) {
		return findItemDefinition(name, PrismReferenceDefinition.class);
	}

	@SuppressWarnings("unchecked")
	default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(@NotNull QName name) {
		return findItemDefinition(name, PrismContainerDefinition.class);
	}

	@SuppressWarnings("unchecked")
	default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(@NotNull String name) {
		return findItemDefinition(new QName(name), PrismContainerDefinition.class);
	}

	default <ID extends ItemDefinition> ID findItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz) {
		return findItemDefinition(name, clazz, false);
	}

	// (2) path resolution
	// (2a) core

	<ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz);

	<ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest, @NotNull Class<ID> clazz);

	// (2b) derived

	@SuppressWarnings("unchecked")
	default <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path) {
		return (ID) findItemDefinition(path, ItemDefinition.class);
	}

	@SuppressWarnings("unchecked")
	default <T> PrismPropertyDefinition<T> findPropertyDefinition(@NotNull ItemPath path) {
		return findItemDefinition(path, PrismPropertyDefinition.class);
	}

	default PrismReferenceDefinition findReferenceDefinition(@NotNull ItemPath path) {
		return findItemDefinition(path, PrismReferenceDefinition.class);
	}

	@SuppressWarnings("unchecked")
	default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(@NotNull ItemPath path) {
		return findItemDefinition(path, PrismContainerDefinition.class);
	}


}
