/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.util;

import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * A class defining old item state (before change), delta (change) and new item state (after change).
 * This is a useful class used to describe how the item has changed without the need to re-apply the delta
 * several times. The delta can be applied once, and then all the rest of the code will have all the data
 * available. This is mostly just a convenience class that groups those three things together.
 * There is only a very little logic on top of that.
 *
 * @author Radovan Semancik
 */
public class ItemDeltaItem<V extends PrismValue,D extends ItemDefinition> implements DebugDumpable {

    private Item<V,D> itemOld;
    private ItemDelta<V,D> delta;
    private Item<V,D> itemNew;
    // We need explicit definition, because source may be completely null.
    // No item, no delta, nothing. In that case we won't be able to crete properly-typed
    // variable from the source.
    private D definition;
    private ItemPath resolvePath = ItemPath.EMPTY_PATH;

    // Residual path is a temporary solution to Structured attributes in 3.x and 4.x.
    // It should disappear in 5.x.
    private ItemPath residualPath = null;

    // The deltas in sub-items. E.g. if this object represents "ContainerDeltaContainer"
    // this property contains property deltas that may exist inside the container.
    private Collection<? extends ItemDelta<?,?>> subItemDeltas;

    // For clone and ObjectDeltaObject
    protected ItemDeltaItem() { }

    public ItemDeltaItem(Item<V,D> itemOld, ItemDelta<V,D> delta, Item<V,D> itemNew, D definition) {
        super();
        validate(itemOld, "itemOld");
        validate(delta);
        validate(itemNew, "itemNew");
        this.itemOld = itemOld;
        this.delta = delta;
        this.itemNew = itemNew;
        if (definition == null) {
            // Try to automatically determine definition from content.
            this.definition = determineDefinition();
            if (this.definition == null) {
                throw new IllegalArgumentException("Cannot determine definition from content in "+this);
            }
        } else {
            this.definition = definition;
        }

    }

    private D determineDefinition() {
        if (itemNew != null && itemNew.getDefinition() != null) {
            return itemNew.getDefinition();
        }
        if (itemOld != null && itemOld.getDefinition() != null) {
            return itemOld.getDefinition();
        }
        if (delta != null && delta.getDefinition() != null) {
            return delta.getDefinition();
        }
        return null;
    }

    public ItemDeltaItem(ItemDeltaItem<V,D> idi) {
        super();
        this.itemOld = idi.getItemOld();
        validate(itemOld, "itemOld");
        this.itemNew = idi.getItemNew();
        validate(itemNew, "itemNew");
        this.delta = idi.getDelta();
        validate(delta);
        this.definition = idi.getDefinition();
        Validate.notNull(this.definition, "No definition in source IDI, cannot create IDI");
    }

    public ItemDeltaItem(Item<V,D> item) {
        super();
        this.itemOld = item;
        this.itemNew = item;
        validate(itemOld, "item");
        this.delta = null;
        this.definition = item.getDefinition();
        Validate.notNull(this.definition, "No definition in item, cannot create IDI");
    }

    public ItemDeltaItem(Item<V,D> item, D definition) {
        super();
        this.itemOld = item;
        this.itemNew = item;
        validate(itemOld, "item");
        this.delta = null;
        this.definition = definition;
        Validate.notNull(this.definition, "No definition in item, cannot create IDI");
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

    /**
     * Returns new item that is a result of delta application. May return null if there is no
     * new item.
     *
     * WARNING: Output of this method should be used for preview only.
     * It should NOT be placed into prism structures. Not even cloned.
     * This method may return dummy items or similar items that are not usable.
     * Values in the items should be OK, but they may need cloning.
     */
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

    @NotNull        // FIXME beware, ObjectDeltaObject.getDefinition seems to return null sometimes (probably should be fixed there)
    public D getDefinition() {
        return definition;
    }

//    public void setDefinition(@NotNull D definition) {
//        Validate.notNull(definition, "Attempt to set null IDI definition");
//        this.definition = definition;
//    }

    public void recompute() throws SchemaException {
        if (delta == null && (subItemDeltas == null || subItemDeltas.isEmpty())) {
            itemNew = itemOld;
            return;
        }
        itemNew = null;
        if (delta != null) {
            itemNew = delta.getItemNewMatchingPath(itemOld);
        }
        if (subItemDeltas != null && !subItemDeltas.isEmpty()) {
            if (itemNew == null) {
                // Subitem deltas on an non-existing container. This should be fine to create an empty container here.
                itemNew = definition.getPrismContext().itemFactory().createDummyItem(itemOld, definition, resolvePath);
            }
            for (ItemDelta<?,?> subItemDelta: subItemDeltas) {
                subItemDelta.applyTo(itemNew);
            }
        }
    }

    public <IV extends PrismValue, ID extends ItemDefinition> ItemDeltaItem<IV,ID> findIdi(@NotNull ItemPath path) throws SchemaException {
        return findIdi(path, null);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> ItemDeltaItem<IV,ID> findIdi(@NotNull ItemPath path, @Nullable DefinitionResolver<D,ID> additionalDefinitionResolver) throws SchemaException {
        if (path.isEmpty()) {
            //noinspection unchecked
            return (ItemDeltaItem<IV,ID>) this;
        }

        Item<IV,ID> subItemOld;
        ItemPath subResidualPath = null;
        ItemPath newResolvePath = resolvePath.append(path);
        if (itemOld != null) {
            PartiallyResolvedItem<IV,ID> partialItemOld = itemOld.findPartial(path);
            if (partialItemOld != null) {
                subItemOld = partialItemOld.getItem();
                subResidualPath = partialItemOld.getResidualPath();
            } else {
                subItemOld = null;
            }
        } else {
            subItemOld = null;
        }

        Item<IV,ID> subItemNew;
        if (itemNew != null) {
            PartiallyResolvedItem<IV,ID> partialItemNew = itemNew.findPartial(path);
            if (partialItemNew != null) {
                subItemNew = partialItemNew.getItem();
                if (subResidualPath == null) {
                    subResidualPath = partialItemNew.getResidualPath();
                }
            } else {
                subItemNew = null;
            }
        } else {
            subItemNew = null;
        }

        ItemDelta<IV,ID> subDelta = null;
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

        ID subDefinition;
        if (definition instanceof PrismContainerDefinition<?>) {
            subDefinition = ((PrismContainerDefinition<?>)definition).findItemDefinition(path);
        } else {
            throw new IllegalArgumentException("Attempt to resolve definition on non-container " + definition + " in " +this);
        }
        if (subDefinition == null && subItemNew != null) {
            subDefinition = subItemNew.getDefinition();
        }
        if (subDefinition == null && subDelta != null) {
            subDefinition = subDelta.getDefinition();
        }
        if (subDefinition == null && subItemOld != null) {
            subDefinition = subItemOld.getDefinition();
        }
        if (subDefinition == null && additionalDefinitionResolver != null) {
            subDefinition = additionalDefinitionResolver.resolve(definition, path);
        }
        if (subDefinition == null) {
            throw new SchemaException("No definition for item "+path+" in "+this);
        }

        ItemDeltaItem<IV,ID> subIdi = new ItemDeltaItem<>(subItemOld, subDelta, subItemNew, subDefinition);
        subIdi.setResidualPath(subResidualPath);
        subIdi.resolvePath = newResolvePath;

        if (subItemDeltas != null) {
            Item<IV,ID> subAnyItem = subIdi.getAnyItem();
            Collection<ItemDelta<?,?>> subSubItemDeltas = new ArrayList<>();
            if (subAnyItem != null) {
                for (ItemDelta<?, ?> subItemDelta : subItemDeltas) {
                    CompareResult compareComplex = subItemDelta.getPath().compareComplex(subAnyItem.getPath());
                    if (compareComplex == CompareResult.EQUIVALENT || compareComplex == CompareResult.SUBPATH) {
                        subSubItemDeltas.add(subItemDelta);
                    }
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

    public PrismValueDeltaSetTriple<V> toDeltaSetTriple(PrismContext prismContext) throws SchemaException {
        return ItemDeltaUtil.toDeltaSetTriple(itemOld, delta, prismContext);
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
    public <X> ItemDeltaItem<PrismPropertyValue<X>,PrismPropertyDefinition<X>> resolveStructuredProperty(
            ItemPath resolvePath, PrismPropertyDefinition outputDefinition,
            PrismContext prismContext) {
        ItemDeltaItem<PrismPropertyValue<Structured>,PrismPropertyDefinition<Structured>> thisIdi = (ItemDeltaItem<PrismPropertyValue<Structured>,PrismPropertyDefinition<Structured>>)this;
        PrismProperty<X> outputPropertyNew = resolveStructuredPropertyItem((PrismProperty<Structured>) thisIdi.getItemNew(), resolvePath, outputDefinition);
        PrismProperty<X> outputPropertyOld = resolveStructuredPropertyItem((PrismProperty<Structured>) thisIdi.getItemOld(), resolvePath, outputDefinition);
        PropertyDelta<X> outputDelta = resolveStructuredPropertyDelta((PropertyDelta<Structured>) thisIdi.getDelta(), resolvePath, outputDefinition, prismContext);
        return new ItemDeltaItem<>(outputPropertyOld, outputDelta, outputPropertyNew, outputDefinition);
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

    private <X> PropertyDelta<X> resolveStructuredPropertyDelta(PropertyDelta<Structured> sourceDelta, ItemPath resolvePath,
            PrismPropertyDefinition outputDefinition, PrismContext prismContext) {
        if (sourceDelta == null) {
            return null;
        }
        // Path in output delta has no meaning anyway. The delta will never be applied, as it references sub-property object.
        //noinspection unchecked
        PropertyDelta<X> outputDelta = (PropertyDelta<X>) outputDefinition.createEmptyDelta(ItemPath.EMPTY_PATH);
        Collection<PrismPropertyValue<X>> outputValuesToAdd = resolveStructuredDeltaSet(sourceDelta.getValuesToAdd(), resolvePath, prismContext);
        if (outputValuesToAdd != null) {
            outputDelta.addValuesToAdd(outputValuesToAdd);
        }
        Collection<PrismPropertyValue<X>> outputValuesToDelete = resolveStructuredDeltaSet(sourceDelta.getValuesToDelete(), resolvePath,
                prismContext);
        if (outputValuesToDelete != null) {
            outputDelta.addValuesToDelete(outputValuesToDelete);
        }
        Collection<PrismPropertyValue<X>> outputValuesToReplace = resolveStructuredDeltaSet(sourceDelta.getValuesToReplace(), resolvePath,
                prismContext);
        if (outputValuesToReplace != null) {
            outputDelta.setValuesToReplace(outputValuesToReplace);
        }
        return outputDelta;
    }

    private <X> Collection<PrismPropertyValue<X>> resolveStructuredDeltaSet(Collection<PrismPropertyValue<Structured>> set,
            ItemPath resolvePath, PrismContext prismContext) {
        if (set == null) {
            return null;
        }
        Collection<PrismPropertyValue<X>> outputSet = new ArrayList<>(set.size());
        for (PrismPropertyValue<Structured> structuredPVal: set) {
            Structured structured = structuredPVal.getValue();
            X outputRval = (X) structured.resolve(resolvePath);
            outputSet.add(prismContext.itemFactory().createPropertyValue(outputRval));
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
        clone.definition = this.definition;
        clone.residualPath = this.residualPath;
        clone.resolvePath = this.resolvePath;
        if (this.subItemDeltas != null) {
            clone.subItemDeltas = ItemDeltaCollectionsUtil.cloneCollection(this.subItemDeltas);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemDeltaItem<?, ?> that = (ItemDeltaItem<?, ?>) o;
        return Objects.equals(itemOld, that.itemOld) && Objects.equals(delta, that.delta) && Objects.equals(itemNew, that.itemNew) && Objects.equals(definition, that.definition) && Objects.equals(resolvePath, that.resolvePath) && Objects.equals(residualPath, that.residualPath) && Objects.equals(subItemDeltas, that.subItemDeltas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemOld, delta, itemNew, definition, resolvePath, residualPath, subItemDeltas);
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
        DebugUtil.debugDumpWithLabelLn(sb, "subItemDeltas", subItemDeltas, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "definition", definition, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "resolvePath", resolvePath, indent + 1);
        DebugUtil.debugDumpWithLabelToString(sb, "residualPath", residualPath, indent + 1);
        return sb.toString();
    }

    private V getSingleValue(Item<V, D> item) {
        if (item == null || item.isEmpty()) {
            return null;
        } else if (item.size() == 1) {
            return item.getAnyValue();
        } else {
            throw new IllegalStateException("Multiple values where single one was expected: " + item);
        }
    }

    public V getSingleValue(boolean old) {
        return getSingleValue(old ? itemOld : itemNew);
    }

    private void validate(Item<V, D> item, String desc) {
        if (item != null && item.getDefinition() == null) {
            throw new IllegalArgumentException("Attempt to set "+desc+" without definition");
        }
    }

    private void validate(ItemDelta<V, D> delta) {
        if (delta != null && delta.getDefinition() == null) {
            throw new IllegalArgumentException("Attempt to set delta without definition");
        }
    }
}
