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

package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;

/**
 *
 */
public class ItemDeltaUtil {

	public static boolean isEmpty(ItemDeltaType itemDeltaType) {
		if (itemDeltaType == null) {
			return true;
		}
		if (itemDeltaType.getModificationType() == ModificationTypeType.REPLACE) {
			return false;
		}
		return !itemDeltaType.getValue().isEmpty();
	}

	public static <IV extends PrismValue,ID extends ItemDefinition> PrismValueDeltaSetTriple<IV> toDeltaSetTriple(
			Item<IV, ID> item,
			ItemDelta<IV, ID> delta, PrismContext prismContext) {
		if (item == null && delta == null) {
			return null;
		}
		if (delta == null) {
			PrismValueDeltaSetTriple<IV> triple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();
			triple.addAllToZeroSet(PrismValueCollectionsUtil.cloneCollection(item.getValues()));
			return triple;
		}
		return delta.toDeltaSetTriple(item);
	}

	// TODO move to Item
	public static <V extends PrismValue, D extends ItemDefinition> ItemDelta<V, D> createAddDeltaFor(Item<V, D> item) {
		ItemDelta<V, D> rv = item.createDelta(item.getPath());
		rv.addValuesToAdd(item.getClonedValues());
		return rv;
	}

	// TODO move to Item
	@SuppressWarnings("unchecked")
	public static <V extends PrismValue, D extends ItemDefinition> ItemDelta<V, D> createAddDeltaFor(Item<V, D> item,
			PrismValue value) {
		ItemDelta<V, D> rv = item.createDelta(item.getPath());
		rv.addValueToAdd((V) CloneUtil.clone(value));
		return rv;
	}

}
