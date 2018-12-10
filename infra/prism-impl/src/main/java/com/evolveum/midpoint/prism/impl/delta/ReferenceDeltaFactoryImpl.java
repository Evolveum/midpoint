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
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *
 */
public class ReferenceDeltaFactoryImpl implements DeltaFactory.Reference {

	@NotNull private final PrismContext prismContext;

	ReferenceDeltaFactoryImpl(@NotNull PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	@Override
	public ReferenceDelta create(ItemPath path, PrismReferenceDefinition definition) {
		return new ReferenceDeltaImpl(path, definition, prismContext);
	}

	@Override
	public ReferenceDelta create(PrismReferenceDefinition itemDefinition) {
		return new ReferenceDeltaImpl(itemDefinition, prismContext);
	}

	@Override
	public ReferenceDelta create(ItemPath parentPath, QName name, PrismReferenceDefinition itemDefinition) {
		return new ReferenceDeltaImpl(parentPath, name, itemDefinition, prismContext);
	}

	@Override
	public ReferenceDelta createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition, String oid) {
		return ReferenceDeltaImpl.createModificationReplace(path, objectDefinition, oid);
	}

	@Override
	public <O extends Objectable> ReferenceDelta createModificationReplace(ItemPath path, Class<O> type,
			String oid) {
		return ReferenceDeltaImpl.createModificationReplace(path, type, prismContext, oid);
	}

	@Override
	public ReferenceDelta createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition,
			PrismReferenceValue refValue) {
		return ReferenceDeltaImpl.createModificationReplace(path, objectDefinition, refValue);
	}

	@Override
	public ReferenceDelta createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition,
			Collection<PrismReferenceValue> refValues) {
		return ReferenceDeltaImpl.createModificationReplace(path, objectDefinition, refValues);
	}

	@Override
	public Collection<? extends ItemDelta> createModificationAddCollection(ItemName propertyName,
			PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue) {
		return ReferenceDeltaImpl.createModificationAddCollection(propertyName, objectDefinition, refValue);
	}

	@Override
	public ReferenceDelta createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
			String oid) {
		return ReferenceDeltaImpl.createModificationAdd(path, objectDefinition, oid);
	}

	@Override
	public ReferenceDelta createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
			PrismReferenceValue refValue) {
		return ReferenceDeltaImpl.createModificationAdd(path, objectDefinition, refValue);
	}

	@Override
	public ReferenceDelta createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
			Collection<PrismReferenceValue> refValues) {
		return ReferenceDeltaImpl.createModificationAdd(path, objectDefinition, refValues);
	}

	@Override
	public <T extends Objectable> ReferenceDelta createModificationAdd(Class<T> type, ItemName refName,
			PrismReferenceValue refValue) {
		return ReferenceDeltaImpl.createModificationAdd(type, refName, prismContext, refValue);
	}

	@Override
	public <T extends Objectable> Collection<? extends ItemDelta> createModificationAddCollection(Class<T> type, ItemName refName,
			String targetOid) {
		return ReferenceDeltaImpl.createModificationAddCollection(type, refName, prismContext, targetOid);
	}

	@Override
	public <T extends Objectable> Collection<? extends ItemDelta> createModificationAddCollection(Class<T> type, ItemName refName,
			PrismReferenceValue refValue) {
		return ReferenceDeltaImpl.createModificationAddCollection(type, refName, prismContext, refValue);
	}

	@Override
	public <T extends Objectable> ReferenceDelta createModificationAdd(Class<T> type, ItemName refName,
			PrismObject<?> refTarget) {
		return ReferenceDeltaImpl.createModificationAdd(type, refName, prismContext, refTarget);
	}

	@Override
	public <T extends Objectable> Collection<? extends ItemDelta> createModificationAddCollection(Class<T> type, ItemName refName,
			PrismObject<?> refTarget) {
		return ReferenceDeltaImpl.createModificationAddCollection(type, refName, prismContext, refTarget);
	}

	@Override
	public Collection<? extends ItemDelta> createModificationDeleteCollection(QName propertyName,
			PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue) {
		return ReferenceDeltaImpl.createModificationDeleteCollection(propertyName, objectDefinition, refValue);
	}

	@Override
	public ReferenceDelta createModificationDelete(ItemPath path, PrismObjectDefinition<?> objectDefinition,
			Collection<PrismReferenceValue> refValues) {
		return ReferenceDeltaImpl.createModificationDelete(path, objectDefinition, refValues);
	}

	@Override
	public ReferenceDelta createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
			String oid) {
		return ReferenceDeltaImpl.createModificationDelete(refName, objectDefinition, oid);
	}

	@Override
	public ReferenceDelta createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
			PrismObject<?> refTarget) {
		return ReferenceDeltaImpl.createModificationDelete(refName, objectDefinition, refTarget, prismContext);
	}

	@Override
	public ReferenceDelta createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
			PrismReferenceValue refValue) {
		return ReferenceDeltaImpl.createModificationDelete(refName, objectDefinition, refValue);
	}

	@Override
	public <T extends Objectable> ReferenceDelta createModificationDelete(Class<T> type, QName refName,
			PrismReferenceValue refValue) {
		return ReferenceDeltaImpl.createModificationDelete(type, refName, prismContext, refValue);
	}

	@Override
	public <T extends Objectable> Collection<? extends ItemDelta> createModificationDeleteCollection(Class<T> type, QName refName,
			PrismReferenceValue refValue) {
		return ReferenceDeltaImpl.createModificationDeleteCollection(type, refName, prismContext, refValue);
	}

	@Override
	public <T extends Objectable> ReferenceDelta createModificationDelete(Class<T> type, QName refName,
			PrismObject<?> refTarget) {
		return ReferenceDeltaImpl.createModificationDelete(type, refName, prismContext, refTarget);
	}

	@Override
	public <T extends Objectable> Collection<? extends ItemDelta> createModificationDeleteCollection(Class<T> type, QName refName,
			PrismObject<?> refTarget) {
		return ReferenceDeltaImpl.createModificationDeleteCollection(type, refName, prismContext, refTarget);
	}
}
