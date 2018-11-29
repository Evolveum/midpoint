/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.util.Collection;

import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import org.jetbrains.annotations.NotNull;

/**
 * Object Reference is a property that describes reference to an object. It is
 * used to represent association between objects. For example reference from
 * User object to Account objects that belong to the user. The reference is a
 * simple uni-directional link using an OID as an identifier.
 *
 * This type should be used for all object references so the implementations can
 * detect them and automatically resolve them.
 *
 * @author semancik
 *
 */
public interface PrismReference extends Item<PrismReferenceValue,PrismReferenceDefinition> {
//	public PrismReference(QName name) {
//        super(name);
//    }
//
//	PrismReference(QName name, PrismReferenceDefinition definition, PrismContext prismContext) {
//		super(name, definition, prismContext);
//	}

	/**
	 * {@inheritDoc}
	 */
	PrismReferenceValue getValue();

	@Override
	Referencable getRealValue();

	@NotNull
	@Override
	Collection<Referencable> getRealValues();

	boolean add(@NotNull PrismReferenceValue value);

	boolean merge(PrismReferenceValue value);

	String getOid();

	PolyString getTargetName();

	PrismReferenceValue findValueByOid(String oid);

	@Override
	Object find(ItemPath path);

	@Override
	<IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

	@Override
	ReferenceDelta createDelta();

	@Override
	ReferenceDelta createDelta(ItemPath path);

	@Override
	PrismReference clone();

	@Override
	PrismReference cloneComplex(CloneStrategy strategy);

	@Override
	String toString();

	@Override
	String debugDump(int indent);

}
