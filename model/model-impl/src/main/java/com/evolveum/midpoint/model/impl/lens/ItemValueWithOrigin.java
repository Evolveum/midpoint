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
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.projector.ValueMatcher;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class ItemValueWithOrigin<V extends PrismValue, D extends ItemDefinition> implements DebugDumpable {
	
	private V itemValue;
	private PrismValueDeltaSetTripleProducer<V, D> mapping;
	private Construction construction;
	
	public ItemValueWithOrigin(V propertyValue,
			PrismValueDeltaSetTripleProducer<V, D> mapping, Construction accountConstruction) {
		super();
		this.itemValue = propertyValue;
		this.mapping = mapping;
		this.construction = accountConstruction;
	}

    // the same as above, but with correct name
    public V getItemValue() {
        return itemValue;
    }

    // use with care
    public void setItemValue(V value) {
        this.itemValue = value;
    }
	
	public PrismValueDeltaSetTripleProducer<V, D> getMapping() {
		return mapping;
	}
	
	public Construction getConstruction() {
		return construction;
	}

	public ObjectType getSource() {
		return construction != null ? construction.getSource() : null;
	}

	public boolean isValid() {
		return construction == null || construction.isValid();
	}
	
	public <T> boolean equalsRealValue(V pvalue, ValueMatcher<T> valueMatcher) throws SchemaException {
		if (itemValue == null) {
			return false;
		}
		if (valueMatcher == null) {
			return itemValue.equalsRealValue(pvalue);
		} else {
			// this must be a property, otherwise there would be no matcher
			return valueMatcher.match(((PrismPropertyValue<T>)itemValue).getValue(), 
					((PrismPropertyValue<T>)pvalue).getValue());
		}
	}
	
	public ItemValueWithOrigin<V,D> clone() {
		ItemValueWithOrigin<V,D> clone = new ItemValueWithOrigin<>(itemValue, mapping, construction);
		copyValues(clone);
		return clone;
	}

	protected void copyValues(ItemValueWithOrigin<V,D> clone) {
		if (this.itemValue != null) {
			clone.itemValue = (V) this.itemValue.clone();
		}
		if (this.mapping != null) {
			clone.mapping = this.mapping.clone();
		}
		clone.construction = this.construction;
	}
	
	public static <V extends PrismValue, D extends ItemDefinition> DeltaSetTriple<ItemValueWithOrigin<V,D>> createOutputTriple(PrismValueDeltaSetTripleProducer<V, D> mapping) {
		PrismValueDeltaSetTriple<V> outputTriple = mapping.getOutputTriple();
		if (outputTriple == null) {
			return null;
		}
		Collection<ItemValueWithOrigin<V,D>> zeroIvwoSet = convertSet(outputTriple.getZeroSet(), mapping);
		Collection<ItemValueWithOrigin<V,D>> plusIvwoSet = convertSet(outputTriple.getPlusSet(), mapping);
		Collection<ItemValueWithOrigin<V,D>> minusIvwoSet = convertSet(outputTriple.getMinusSet(), mapping);
		DeltaSetTriple<ItemValueWithOrigin<V,D>> ivwoTriple = new DeltaSetTriple<>(zeroIvwoSet, plusIvwoSet, minusIvwoSet);
		return ivwoTriple;
	}

	@NotNull
	private static <V extends PrismValue, D extends ItemDefinition> Collection<ItemValueWithOrigin<V,D>> convertSet(
			@NotNull Collection<V> valueSet, PrismValueDeltaSetTripleProducer<V, D> mapping) {
		Collection<ItemValueWithOrigin<V,D>> ivwoSet = new ArrayList<>(valueSet.size());
		for (V value: valueSet) {
			ItemValueWithOrigin<V,D> ivwo = new ItemValueWithOrigin<>(value, mapping, null);
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
