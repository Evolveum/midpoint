/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class ItemValueWithOrigin<V extends PrismValue> implements DebugDumpable {
	
	private V itemValue;
	private Mapping<V> mapping;
	private Construction construction;
	
	public ItemValueWithOrigin(V propertyValue,
			Mapping<V> mapping, Construction accountConstruction) {
		super();
		this.itemValue = propertyValue;
		this.mapping = mapping;
		this.construction = accountConstruction;
	}

    @Deprecated
	public V getPropertyValue() {
		return itemValue;
	}

    // the same as above, but with correct name
    public V getItemValue() {
        return itemValue;
    }

    // use with care
    public void setItemValue(V value) {
        this.itemValue = value;
    }
	
	public Mapping<?> getMapping() {
		return mapping;
	}
	
	public Construction getConstruction() {
		return construction;
	}

	public boolean equalsRealValue(V pvalue) {
		if (itemValue == null) {
			return false;
		}
		return itemValue.equalsRealValue(pvalue);
	}
	
	public ItemValueWithOrigin<V> clone() {
		ItemValueWithOrigin<V> clone = new ItemValueWithOrigin<V>(itemValue, mapping, construction);
		copyValues(clone);
		return clone;
	}

	protected void copyValues(ItemValueWithOrigin<V> clone) {
		if (this.itemValue != null) {
			clone.itemValue = (V) this.itemValue.clone();
		}
		if (this.mapping != null) {
			clone.mapping = this.mapping.clone();
		}
		clone.construction = this.construction;
	}
	
	public static <V extends PrismValue> DeltaSetTriple<ItemValueWithOrigin<V>> createOutputTriple(Mapping<V> mapping) {
		PrismValueDeltaSetTriple<V> outputTriple = mapping.getOutputTriple();
		if (outputTriple == null) {
			return null;
		}
		Collection<ItemValueWithOrigin<V>> zeroIvwoSet = convertSet(outputTriple.getZeroSet(), mapping);
		Collection<ItemValueWithOrigin<V>> plusIvwoSet = convertSet(outputTriple.getPlusSet(), mapping);
		Collection<ItemValueWithOrigin<V>> minusIvwoSet = convertSet(outputTriple.getMinusSet(), mapping);
		DeltaSetTriple<ItemValueWithOrigin<V>> ivwoTriple = new DeltaSetTriple<ItemValueWithOrigin<V>>(zeroIvwoSet, plusIvwoSet, minusIvwoSet);
		return ivwoTriple;
	}
	
	private static <V extends PrismValue> Collection<ItemValueWithOrigin<V>> convertSet(Collection<V> valueSet, Mapping<V> mapping) {
		if (valueSet == null) {
			return null;
		}
		Collection<ItemValueWithOrigin<V>> ivwoSet = new ArrayList<ItemValueWithOrigin<V>>(valueSet.size());
		for (V value: valueSet) {
			ItemValueWithOrigin<V> ivwo = new ItemValueWithOrigin<V>(value, mapping, null);
			ivwoSet.add(ivwo);
		}
		return ivwoSet;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ItemValueWithOrigin:\n");
		DebugUtil.debugDumpWithLabel(sb, "itemValue", itemValue, indent +1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabelToString(sb, "mapping", mapping, indent +1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabelToString(sb, "construction", construction, indent +1);
		return sb.toString();
	}

	@Override
	public String toString() {
		return "ItemValueWithOrigin(" + itemValue + ", M="
				+ mapping + ", C=" + construction + ")";
	}

}
