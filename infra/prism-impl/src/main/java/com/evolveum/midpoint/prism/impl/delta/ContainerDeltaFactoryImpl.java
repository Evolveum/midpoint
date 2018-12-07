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

package com.evolveum.midpoint.prism.impl.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *
 */
public class ContainerDeltaFactoryImpl implements DeltaFactory.Container {

	@NotNull private final PrismContext prismContext;

	ContainerDeltaFactoryImpl(@NotNull PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	@Override
	public <C extends Containerable> ContainerDelta<C> create(ItemPath path, PrismContainerDefinition<C> definition) {
		return new ContainerDeltaImpl<>(path, definition, prismContext);
	}

	@Override
	public ContainerDelta create(PrismContainerDefinition itemDefinition) {
		return new ContainerDeltaImpl<>(itemDefinition, prismContext);
	}

	@Override
	public ContainerDelta create(ItemPath parentPath, QName name, PrismContainerDefinition itemDefinition) {
		return new ContainerDeltaImpl<>(parentPath, name, itemDefinition, prismContext);
	}

	@Override
    public <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createDelta(ItemPath containerPath,
			Class<O> type) {
		PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		return createDelta(containerPath, objectDefinition);
	}

	@Override
    public <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createDelta(ItemPath containerPath,
			PrismObjectDefinition<O> objectDefinition) {
		return ContainerDeltaImpl.createDelta(containerPath, objectDefinition);
	}

	@Override
    public <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createDelta(ItemPath containerPath,
			PrismContainerDefinition<O> objectDefinition) {
		return ContainerDeltaImpl.createDelta(containerPath, objectDefinition);
	}

	@Override
    public <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationAdd(
			ItemPath containerPath,
			Class<O> type, T containerable) throws SchemaException {
		return ContainerDeltaImpl.createModificationAdd(containerPath, type, prismContext, containerable);
	}

	@Override
    public <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationAdd(
			ItemPath containerPath,
			Class<O> type, PrismContainerValue<T> cValue) throws SchemaException {
		return ContainerDeltaImpl.createModificationAdd(containerPath, type, prismContext, cValue);
	}

	@Override
    public <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationDelete(
			ItemPath containerPath,
			Class<O> type, T containerable) throws SchemaException {
		return ContainerDeltaImpl.createModificationDelete(containerPath, type, prismContext, containerable);
	}

	@Override
    public <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationDelete(
			ItemPath containerPath,
			Class<O> type, PrismContainerValue<T> cValue) throws SchemaException {
		return ContainerDeltaImpl.createModificationDelete(containerPath, type, prismContext, cValue);
	}

	@Override
    public <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationReplace(
			ItemPath containerPath,
			Class<O> type, T containerable) throws SchemaException {
		return ContainerDeltaImpl.createModificationReplace(containerPath, type, prismContext, containerable);
	}

	@Override
    public <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationReplace(ItemPath containerPath,
			Class<O> type, Collection<T> containerables) throws SchemaException {
		return ContainerDeltaImpl.createModificationReplace(containerPath, type, prismContext, containerables);
	}

	@Override
    public <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationReplace(
			ItemPath containerPath,
			Class<O> type, PrismContainerValue<T> cValue) throws SchemaException {
		return ContainerDeltaImpl.createModificationReplace(containerPath, type, prismContext, cValue);
	}

	// cValues should be parent-less
	@Deprecated
	@Override
    public Collection<? extends ItemDelta> createModificationReplaceContainerCollection(ItemName containerName,
			PrismObjectDefinition<?> objectDefinition, PrismContainerValue... cValues) {
		return ContainerDeltaImpl.createModificationReplaceContainerCollection(containerName, objectDefinition, cValues);
	}

	// cValues should be parent-less
	@Deprecated
	@Override
    public <T extends Containerable> ContainerDeltaImpl<T> createModificationReplace(ItemName containerName,
			PrismObjectDefinition<?> objectDefinition, PrismContainerValue... cValues) {
		return ContainerDeltaImpl.createModificationReplace(containerName, objectDefinition, cValues);
	}

}
