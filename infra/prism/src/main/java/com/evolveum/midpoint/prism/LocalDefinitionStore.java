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

import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface LocalDefinitionStore {

	// (1) single-name resolution

	// (1a) derived
	default <ID extends ItemDefinition> ID findItemDefinition(@NotNull QName name) {
		return (ID) findItemDefinition(name, ItemDefinition.class);
	}

	default <T> PrismPropertyDefinition<T> findPropertyDefinition(@NotNull QName name) {
		return findItemDefinition(name, PrismPropertyDefinition.class);
	}

	default PrismReferenceDefinition findReferenceDefinition(@NotNull QName name) {
		return findItemDefinition(name, PrismReferenceDefinition.class);
	}

	default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(@NotNull QName name) {
		return findItemDefinition(name, PrismContainerDefinition.class);
	}

	default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(@NotNull String name) {
		return findItemDefinition(new QName(name), PrismContainerDefinition.class);
	}

	default <ID extends ItemDefinition> ID findItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz) {
		return findItemDefinition(name, clazz, false);
	}

	// (1b) core

	@Nullable
	<ID extends ItemDefinition> ID findItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz, boolean caseInsensitive);

	// (2) path resolution
	// (2a) derived

	default <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path) {
		return (ID) findItemDefinition(path, ItemDefinition.class);
	}

	default <T> PrismPropertyDefinition<T> findPropertyDefinition(@NotNull ItemPath path) {
		return findItemDefinition(path, PrismPropertyDefinition.class);
	}

	default PrismReferenceDefinition findReferenceDefinition(@NotNull ItemPath path) {
		return findItemDefinition(path, PrismReferenceDefinition.class);
	}

	default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(@NotNull ItemPath path) {
		return findItemDefinition(path, PrismContainerDefinition.class);
	}

	// (2b) core

	<ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz);

	<ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest, @NotNull Class<ID> clazz);

}
