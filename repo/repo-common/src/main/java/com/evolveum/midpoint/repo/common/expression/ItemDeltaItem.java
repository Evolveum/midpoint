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
package com.evolveum.midpoint.repo.common.expression;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class ItemDeltaItem<V extends PrismValue,D extends ItemDefinition> implements DebugDumpable {

	Item<V,D> itemOld;
	ItemDelta<V,D> delta;
	Item<V,D> itemNew;
	ItemPath resolvePath = ItemPath.EMPTY_PATH;
	ItemPath residualPath = null;

	// The deltas in sub-items. E.g. if this object represents "ContainerDeltaContainer"
	// this property contains property deltas that may exist inside the container.
	Collection<? extends ItemDelta<?,?>> subItemDeltas;

	public ItemDeltaItem() { }

	public ItemDeltaItem(Item<V,D> itemOld, ItemDelta<V,D> delta, Item<V,D> itemNew) {
		super();
		this.itemOld = itemOld;
		this.delta = delta;
		this.itemNew = itemNew;
	}

	public ItemDeltaItem(ItemDeltaItem<V,D> idi) {
		super();
		this.itemOld = idi.getItemOld();
		this.itemNew = idi.getItemNew();
		this.delta = idi.getDelta();
	}

	public ItemDeltaItem(Item<V,D> item) {
		super();
		this.itemOld = item;
		this.itemNew = item;
		this.delta = null;
	}

	public Item<V,D> getItemOld() {
		return itemOld;
	}

	public void setItemOld(Item<V,D> itemOld) {
		this.itemOld = itemOld;
	}

	public ItemDelta<V,D> getDelta() {
		return delta;
	}

	public void setDelta(ItemDelta<V,D> delta) {
		this.delta = delta;
	}

	public Item<V,D> getItemNew() {
		return itemNew;
	}

	public void setItemNew(Item<V,D> itemNew) {
		this.itemNew = itemNew;
	}

	public Item<V,D> getAnyItem() {
		if (itemOld != null) {
			return itemOld;
		}
		return itemNew;
	}

	public ItemPath getResidualPath() {
		return residualPath;
	}

	public void setResidualPath(ItemPath residualPath) {
		this.residualPath = residualPath;
	}

	public ItemPath getResolvePath() {
		return resolvePath;
	}

	public void setResolvePath(ItemPath resolvePath) {
		this.resolvePath = resolvePath;
	}

	public Collection<? extends ItemDelta<?,?>> getSubItemDeltas() {
		return subItemDeltas;
	}

	public void setSubItemDeltas(Collection<? extends ItemDelta<?,?>> subItemDeltas) {
		this.subItemDeltas = subItemDeltas;
	}

	public boolean isNull() {
		return itemOld == null && itemNew == null && delta == null && subItemDeltas == null;
	}

	public QName getElementName() {
		Item<V,D> anyItem = getAnyItem();
		if (anyItem != null) {
			return anyItem.getElementName();
		}
		if (delta != null) {
			return delta.getElementName();
		}
		return null;
	}

	public ItemDefinition getDefinition() {
		Item<V,D> anyItem = getAnyItem();
		if (anyItem != null) {
			return anyItem.getDefinition();
		}
		if (delta != null) {
			return delta.getDefinition();
		}
		return null;
	}

	public void recompute() throws SchemaException {
		if (delta != null) {
			itemNew = delta.getItemNewMatchingPath(itemOld);
		} else {
			itemNew = itemOld;
		}
		if (subItemDeltas != null && !subItemDeltas.isEmpty()) {
			if (itemNew == null) {
				throw new SchemaException("Cannot apply subitem delta to null new item");
			}
			for (ItemDelta<?,?> subItemDelta: subItemDeltas) {
				itemNew = (Item<V,D>) subItemDelta.getItemNew((Item) itemNew);
			}
		}
	}

	public <IV extends PrismValue, ID extends ItemDefinition> ItemDeltaItem<IV,ID> findIdi(ItemPath path) {
		if (path.isEmpty()) {
			return (ItemDeltaItem<IV,ID>) this;
		}
		Item<IV,ID> subItemOld = null;
		ItemPath subResidualPath = null;
		ItemPath newResolvePath = resolvePath.subPath(path);
		if (itemOld != null) {
			PartiallyResolvedItem<IV,ID> partialItemOld = itemOld.findPartial(path);
			if (partialItemOld != null) {
				subItemOld = partialItemOld.getItem();
				subResidualPath = partialItemOld.getResidualPath();
			}
		}
		Item<IV,ID> subItemNew = null;
		if (itemNew != null) {
			PartiallyResolvedItem<IV,ID> partialItemNew = itemNew.findPartial(path);
			if (partialItemNew != null) {
				subItemNew = partialItemNew.getItem();
				if (subResidualPath == null) {
					subResidualPath = partialItemNew.getResidualPath();
				}
			}
		}
		ItemDelta<IV,ID> subDelta= null;
		if (delta != null) {
			if (delta instanceof ContainerDelta<?>) {
				subDelta = (ItemDelta<IV,ID>) ((ContainerDelta<?>)delta).getSubDelta(path);
			} else {
				CompareResult compareComplex = delta.getPath().compareComplex(newResolvePath);
				if (compareComplex == CompareResult.EQUIVALENT || compareComplex == CompareResult.SUBPATH) {
					subDelta = (ItemDelta<IV,ID>) delta;
				}
			}
		}
		ItemDeltaItem<IV,ID> subIdi = new ItemDeltaItem<IV,ID>(subItemOld, subDelta, subItemNew);
		subIdi.setResidualPath(subResidualPath);
		subIdi.resolvePath = newResolvePath;

		if (subItemDeltas != null) {
			Item<IV,ID> subAnyItem = subIdi.getAnyItem();
			Collection<ItemDelta<?,?>> subSubItemDeltas = new ArrayList<>();
			for (ItemDelta<?,?> subItemDelta: subItemDeltas) {
				CompareResult compareComplex = subItemDelta.getPath().compareComplex(subAnyItem.getPath());
				if (compareComplex == CompareResult.EQUIVALENT || compareComplex == CompareResult.SUBPATH) {
					subSubItemDeltas.add(subItemDelta);
				}
			}
			if (!subSubItemDeltas.isEmpty()) {
				// Niceness optimization
				if (subDelta == null && subSubItemDeltas.size() == 1) {
					ItemDelta<?,?> subSubItemDelta = subSubItemDeltas.iterator().next();
					if (subSubItemDelta.isApplicableTo(subAnyItem)) {
						subDelta = (ItemDelta<IV,ID>) subSubItemDelta;
						subIdi.setDelta(subDelta);
					} else {
						subIdi.setSubItemDeltas(subSubItemDeltas);
					}
				} else {
					subIdi.setSubItemDeltas(subSubItemDeltas);
				}
			}
		}

		return subIdi;
	}

	public PrismValueDeltaSetTriple<V> toDeltaSetTriple() {
		return ItemDelta.toDeltaSetTriple(itemOld, delta);
	}

	public boolean isContainer() {
		Item<V,D> item = getAnyItem();
		if (item != null) {
			return item instanceof PrismContainer<?>;
		}
		if (getDelta() != null) {
			return getDelta() instanceof ContainerDelta<?>;
		}
		return false;
	}

	public boolean isProperty() {
		Item<V,D> item = getAnyItem();
		if (item != null) {
			return item instanceof PrismProperty<?>;
		}
		if (getDelta() != null) {
			return getDelta() instanceof PropertyDelta<?>;
		}
		return false;
	}

	/**
	 * @return
	 */
	public boolean isStructuredProperty() {
		if (!isProperty()) {
			return false;
		}
		PrismProperty<?> property = (PrismProperty<?>) getAnyItem();
		Object realValue = property.getAnyRealValue();
		if (realValue != null) {
			return realValue instanceof Structured;
		}
		PropertyDelta<?> delta = (PropertyDelta<?>) getDelta();
		realValue = delta.getAnyRealValue();
		if (realValue != null) {
			return realValue instanceof Structured;
		}
		return false;
	}

	// Assumes that this IDI represents structured property
	public <X> ItemDeltaItem<PrismPropertyValue<X>,PrismPropertyDefinition<X>> resolveStructuredProperty(ItemPath resolvePath, PrismPropertyDefinition outputDefinition, ItemPath outputPath) {
		ItemDeltaItem<PrismPropertyValue<Structured>,PrismPropertyDefinition<Structured>> thisIdi = (ItemDeltaItem<PrismPropertyValue<Structured>,PrismPropertyDefinition<Structured>>)this;
		PrismProperty<X> outputPropertyNew = resolveStructuredPropertyItem((PrismProperty<Structured>) thisIdi.getItemNew(), resolvePath, outputDefinition);
		PrismProperty<X> outputPropertyOld = resolveStructuredPropertyItem((PrismProperty<Structured>) thisIdi.getItemOld(), resolvePath, outputDefinition);
		PropertyDelta<X> outputDelta = resolveStructuredPropertyDelta((PropertyDelta<Structured>) thisIdi.getDelta(), resolvePath, outputDefinition, outputPath);
		return new ItemDeltaItem<PrismPropertyValue<X>,PrismPropertyDefinition<X>>(outputPropertyOld, outputDelta, outputPropertyNew);
	}

	private <X> PrismProperty<X> resolveStructuredPropertyItem(PrismProperty<Structured> sourceProperty, ItemPath resolvePath, PrismPropertyDefinition outputDefinition) {
		if (sourceProperty == null) {
			return null;
		}
		PrismProperty<X> outputProperty = outputDefinition.instantiate();
		for (Structured sourceRealValue: sourceProperty.getRealValues()) {
			X outputRealValue = (X) sourceRealValue.resolve(resolvePath);
			outputProperty.addRealValue(outputRealValue);
		}
		return outputProperty;
	}

	private <X> PropertyDelta<X> resolveStructuredPropertyDelta(PropertyDelta<Structured> sourceDelta, ItemPath resolvePath, PrismPropertyDefinition outputDefinition, ItemPath outputPath) {
		if (sourceDelta == null) {
			return null;
		}
		PropertyDelta<X> outputDelta = (PropertyDelta<X>) outputDefinition.createEmptyDelta(outputPath);
		Collection<PrismPropertyValue<X>> outputValuesToAdd = resolveStructuredDeltaSet(sourceDelta.getValuesToAdd(), resolvePath);
		if (outputValuesToAdd != null) {
			outputDelta.addValuesToAdd(outputValuesToAdd);
		}
		Collection<PrismPropertyValue<X>> outputValuesToDelete = resolveStructuredDeltaSet(sourceDelta.getValuesToDelete(), resolvePath);
		if (outputValuesToDelete != null) {
			outputDelta.addValuesToDelete(outputValuesToDelete);
		}
		Collection<PrismPropertyValue<X>> outputValuesToReplace = resolveStructuredDeltaSet(sourceDelta.getValuesToReplace(), resolvePath);
		if (outputValuesToReplace != null) {
			outputDelta.setValuesToReplace(outputValuesToReplace);
		}
		return outputDelta;
	}

	private <X> Collection<PrismPropertyValue<X>> resolveStructuredDeltaSet(Collection<PrismPropertyValue<Structured>> set, ItemPath resolvePath) {
		if (set == null) {
			return null;
		}
		Collection<PrismPropertyValue<X>> outputSet = new ArrayList<PrismPropertyValue<X>>(set.size());
		for (PrismPropertyValue<Structured> structuredPVal: set) {
			Structured structured = structuredPVal.getValue();
			X outputRval = (X) structured.resolve(resolvePath);
			outputSet.add(new PrismPropertyValue<X>(outputRval));
		}
		return outputSet;
	}

	public void applyDefinition(D def, boolean force) throws SchemaException {
		if (itemNew != null) {
			itemNew.applyDefinition(def, force);
		}
		if (itemOld != null) {
			itemOld.applyDefinition(def, force);
		}
		if (delta != null) {
			delta.applyDefinition(def, force);
		}
	}

	public ItemDeltaItem<V,D> clone() {
		ItemDeltaItem<V,D> clone = new ItemDeltaItem<>();
		copyValues(clone);
		return clone;
	}

	protected void copyValues(ItemDeltaItem<V,D> clone) {
		if (this.itemNew != null) {
			clone.itemNew = this.itemNew.clone();
		}
		if (this.delta != null) {
			clone.delta = this.delta.clone();
		}
		if (this.itemOld != null) {
			clone.itemOld = this.itemOld.clone();
		}
		clone.residualPath = this.residualPath;
		clone.resolvePath = this.resolvePath;
		if (this.subItemDeltas != null) {
			clone.subItemDeltas = ItemDelta.cloneCollection(this.subItemDeltas);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((delta == null) ? 0 : delta.hashCode());
		result = prime * result + ((itemNew == null) ? 0 : itemNew.hashCode());
		result = prime * result + ((itemOld == null) ? 0 : itemOld.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemDeltaItem other = (ItemDeltaItem) obj;
		if (delta == null) {
			if (other.delta != null)
				return false;
		} else if (!delta.equals(other.delta))
			return false;
		if (itemNew == null) {
			if (other.itemNew != null)
				return false;
		} else if (!itemNew.equals(other.itemNew))
			return false;
		if (itemOld == null) {
			if (other.itemOld != null)
				return false;
		} else if (!itemOld.equals(other.itemOld))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "IDI(old=" + itemOld + ", delta=" + delta + ", new=" + itemNew + ")";
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "ItemDeltaItem", indent);
		DebugUtil.debugDumpWithLabelLn(sb, "itemOld", itemOld, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "delta", delta, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "itemNew", itemNew, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "resolvePath", resolvePath, indent + 1);
		DebugUtil.debugDumpWithLabelToString(sb, "residualPath", residualPath, indent + 1);
		return sb.toString();
	}

	private V getSingleValue(Item<V, D> item) {
		if (item == null || item.isEmpty()) {
			return null;
		} else if (item.size() == 1) {
			return item.getValue(0);
		} else {
			throw new IllegalStateException("Multiple values where single one was expected: " + item);
		}
	}

	public V getSingleValue(boolean evaluateOld) {
		return getSingleValue(evaluateOld ? itemOld : itemNew);
	}
}
