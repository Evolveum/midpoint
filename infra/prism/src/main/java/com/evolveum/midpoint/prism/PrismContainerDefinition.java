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

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */
public interface PrismContainerDefinition<C extends Containerable> extends ItemDefinition<PrismContainer<C>>, LocalDefinitionStore {

	Class<C> getCompileTimeClass();

	ComplexTypeDefinition getComplexTypeDefinition();

	@Override
	void revive(PrismContext prismContext);

	String getDefaultNamespace();

	List<String> getIgnoredNamespaces();

	List<? extends ItemDefinition> getDefinitions();

	List<PrismPropertyDefinition> getPropertyDefinitions();

	@Override
	ContainerDelta<C> createEmptyDelta(ItemPath path);

	@NotNull
	@Override
	PrismContainerDefinition<C> clone();

	PrismContainerDefinition<C> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition);

	void replaceDefinition(QName itemName, ItemDefinition newDefinition);

	PrismContainerValue<C> createValue();

	boolean isEmpty();
}
