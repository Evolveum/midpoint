/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaValidator;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Function;

import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import static com.evolveum.midpoint.prism.path.ItemPath.checkNoSpecialSymbols;

/**
 * @author Radovan Semancik
 *
 */
public abstract class ItemDeltaImpl<V extends PrismValue,D extends ItemDefinition> extends AbstractFreezable implements ItemDelta<V, D> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemDeltaImpl.class);

    /**
     * Name of the property
     */
    protected ItemName elementName;
    /**
     * Parent path of the property (path to the property container)
     */
    protected ItemPath parentPath;
    protected ItemPath fullPath;            // lazily evaluated
    protected D definition;

    protected Collection<V> valuesToReplace = null;
    protected Collection<V> valuesToAdd = null;
    protected Collection<V> valuesToDelete = null;
    protected Collection<V> estimatedOldValues = null;

    transient private PrismContext prismContext;

    protected ItemDeltaImpl(D itemDefinition, PrismContext prismContext) {
        if (itemDefinition == null) {
            throw new IllegalArgumentException("Attempt to create item delta without a definition");
        }
        //checkPrismContext(prismContext, itemDefinition);
        this.prismContext = prismContext;
        this.elementName = itemDefinition.getItemName();
        this.parentPath = ItemPath.EMPTY_PATH;
        this.definition = itemDefinition;
    }

    protected ItemDeltaImpl(QName elementName, D itemDefinition, PrismContext prismContext) {
        //checkPrismContext(prismContext, itemDefinition);
        this.prismContext = prismContext;
        this.elementName = ItemName.fromQName(elementName);
        this.parentPath = ItemPath.EMPTY_PATH;
        this.definition = itemDefinition;
    }

    protected ItemDeltaImpl(ItemPath itemPath, QName elementName, D itemDefinition, PrismContext prismContext) {
        //checkPrismContext(prismContext, itemDefinition);
        this.parentPath = prismContext.toUniformPath(itemPath);
        checkNoSpecialSymbols(parentPath);
        this.prismContext = prismContext;
        this.elementName = ItemName.fromQName(elementName);
        this.definition = itemDefinition;
    }

    protected ItemDeltaImpl(ItemPath path, D itemDefinition, PrismContext prismContext) {
        //checkPrismContext(prismContext, itemDefinition);
        checkNoSpecialSymbols(path);
        this.prismContext = prismContext;

        if (path == null) {
            throw new IllegalArgumentException("Null path specified while creating item delta");
        }
        if (path.isEmpty()) {
            this.elementName = null;
        } else {
            Object last = path.last();
            if (!ItemPath.isName(last)) {
                throw new IllegalArgumentException("Invalid delta path "+path+". Delta path must always point to item, not to value");
            }
            this.elementName = ItemPath.toName(last);
            this.parentPath = path.allExceptLast();
        }
        this.definition = itemDefinition;
    }

    // currently unused; we allow deltas without prismContext, except for some operations (e.g. serialization to ItemDeltaType)
//    private void checkPrismContext(PrismContext prismContext, ItemDefinition itemDefinition) {
//        if (prismContext == null) {
//            throw new IllegalStateException("No prismContext in delta for " + itemDefinition);
//        }
//    }

    public ItemName getElementName() {
        return elementName;
    }

    public void setElementName(QName elementName) {
        checkMutable();
        this.elementName = ItemName.fromQName(elementName);
        this.fullPath = null;
    }

    public ItemPath getParentPath() {
        return parentPath;
    }

    public void setParentPath(ItemPath parentPath) {
        checkMutable();
        this.parentPath = parentPath;
        this.fullPath = null;
    }

    @NotNull
    @Override
    public ItemPath getPath() {
        if (fullPath != null) {
            return fullPath;
        }
        if (getParentPath() == null) {
            throw new IllegalStateException("No parent path in "+this);
        }
        fullPath = getParentPath().append(elementName);
        return fullPath;
    }

    public D getDefinition() {
        return definition;
    }

    public void setDefinition(D definition) {
        checkMutable();
        this.definition = definition;
    }

    @Override
    public void accept(Visitor visitor) {
        accept(visitor, true);
    }

    public void accept(Visitor visitor, boolean includeOldValues) {
        visitor.visit(this);
        if (getValuesToAdd() != null) {
            for (V pval : getValuesToAdd()) {
                pval.accept(visitor);
            }
        }
        if (getValuesToDelete() != null) {
            for (V pval : getValuesToDelete()) {
                pval.accept(visitor);
            }
        }
        if (getValuesToReplace() != null) {
            for (V pval : getValuesToReplace()) {
                pval.accept(visitor);
            }
        }
        if (includeOldValues && getEstimatedOldValues() != null) {
            for (V pval : getEstimatedOldValues()) {
                pval.accept(visitor);
            }
        }
    }

    public int size() {
        return sizeSet(valuesToReplace) + sizeSet(valuesToAdd) + sizeSet(valuesToDelete);
    }

    private int sizeSet(Collection<V> set) {
        if (set == null) {
            return 0;
        } else {
            return set.size();
        }
    }

    // TODO think if estimated old values have to be visited as well
    @Override
    public void accept(Visitor visitor, ItemPath path, boolean recursive) {
        if (path == null || path.isEmpty()) {
            if (recursive) {
                accept(visitor);
            } else {
                visitor.visit(this);
            }
        } else {
            Long id;
            ItemPath rest;
            if (path.startsWithId()) {
                id = path.firstToId();
                rest = path.rest();
            } else if (path.startsWithName()) {
                id = null;
                rest = path;
            } else {
                throw new IllegalArgumentException("Unexpected first path segment "+path);
            }
            if (id == null) {
                // visit all values
                if (getValuesToAdd() != null) {
                    for (V pval : getValuesToAdd()) {
                        pval.accept(visitor, rest, recursive);
                    }
                }
                if (getValuesToDelete() != null) {
                    for (V pval : getValuesToDelete()) {
                        pval.accept(visitor, rest, recursive);
                    }
                }
                if (getValuesToReplace() != null) {
                    for (V pval : getValuesToReplace()) {
                        pval.accept(visitor, rest, recursive);
                    }
                }
            } else {
                acceptSet(getValuesToAdd(), id, visitor, rest, recursive);
                acceptSet(getValuesToDelete(), id, visitor, rest, recursive);
                acceptSet(getValuesToReplace(), id, visitor, rest, recursive);
            }
        }
    }

    private void acceptSet(Collection<V> set, Long id, Visitor visitor, ItemPath rest, boolean recursive) {
        if (set == null) {
            return;
        }
        for (V pval : set) {
            if (pval instanceof PrismContainerValue<?>) {
                PrismContainerValue<?> cval = (PrismContainerValue<?>)pval;
                if (id == null || id.equals(cval.getId())) {
                    pval.accept(visitor, rest, recursive);
                }
            } else {
                throw new IllegalArgumentException("Attempt to fit container id to "+pval.getClass());
            }
        }
    }

    public void applyDefinition(D definition) throws SchemaException {
        checkMutable();
        this.definition = definition;
        if (getValuesToAdd() != null) {
            for (V pval : getValuesToAdd()) {
                pval.applyDefinition(definition);
            }
        }
        if (getValuesToDelete() != null) {
            for (V pval : getValuesToDelete()) {
                pval.applyDefinition(definition);
            }
        }
        if (getValuesToReplace() != null) {
            for (V pval : getValuesToReplace()) {
                pval.applyDefinition(definition);
            }
        }
    }

    public boolean hasCompleteDefinition() {
        return getDefinition() != null;
    }


    public PrismContext getPrismContext() {
        return prismContext;
    }

    public abstract Class<? extends Item> getItemClass();

    public Collection<V> getValuesToAdd() {
        return valuesToAdd;
    }

    public void clearValuesToAdd() {
        checkMutable();
        valuesToAdd = null;
    }

    public Collection<V> getValuesToDelete() {
        return valuesToDelete;
    }

    public void clearValuesToDelete() {
        checkMutable();
        valuesToDelete = null;
    }

    public Collection<V> getValuesToReplace() {
        return valuesToReplace;
    }

    public void clearValuesToReplace() {
        checkMutable();
        valuesToReplace = null;
    }

    public void addValuesToAdd(Collection<V> newValues) {
        checkMutable();
        if (newValues == null) {
            return;
        }
        for (V val : newValues) {
            addValueToAdd(val);
        }
    }

    public void addValuesToAdd(V... newValues) {
        checkMutable();
        for (V val : newValues) {
            addValueToAdd(val);
        }
    }

    public void addValueToAdd(V newValue) {
        checkMutable();
        if (valuesToReplace != null) {
            throw new IllegalStateException("Delta " + this
                + " already has values to replace ("+valuesToReplace+"), attempt to add value ("+newValue+") is an error");
        }
        if (valuesToAdd == null) {
            valuesToAdd = newValueCollection();
        }
        if (PrismValueCollectionsUtil.containsRealValue(valuesToAdd,newValue)) {
            return;
        }
        valuesToAdd.add(newValue);
        newValue.setParent(this);
        newValue.recompute();
    }

    public boolean removeValueToAdd(PrismValue valueToRemove) {
        return removeValue(valueToRemove, valuesToAdd, false);
    }

    public boolean removeValueToDelete(PrismValue valueToRemove) {
        return removeValue(valueToRemove, valuesToDelete, true);
    }

    public boolean removeValueToReplace(PrismValue valueToRemove) {
        return removeValue(valueToRemove, valuesToReplace, false);
    }

    private boolean removeValue(PrismValue valueToRemove, Collection<V> set, boolean toDelete) {
        checkMutable();
        boolean removed = false;
        if (set == null) {
            return false;
        }
        Iterator<V> valuesIterator = set.iterator();
        while (valuesIterator.hasNext()) {
            V existingValue = valuesIterator.next();
            if (existingValue.equals(valueToRemove, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS)
                    || toDelete && existingValue.representsSameValue(valueToRemove, false)) {        // the same algorithm as when deleting the item value
                valuesIterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    public void mergeValuesToAdd(Collection<V> newValues) {
        checkMutable();
        if (newValues == null) {
            return;
        }
        for (V val : newValues) {
            mergeValueToAdd(val);
        }
    }

    public void mergeValuesToAdd(V[] newValues) {
        checkMutable();
        if (newValues == null) {
            return;
        }
        for (V val : newValues) {
            mergeValueToAdd(val);
        }
    }

    public void mergeValueToAdd(V newValue) {
        checkMutable();
        if (valuesToReplace != null) {
            if (!PrismValueCollectionsUtil.containsRealValue(valuesToReplace, newValue)) {
                valuesToReplace.add(newValue);
                newValue.setParent(this);
            }
        } else {
            if (!removeValueToDelete(newValue)) {
                addValueToAdd(newValue);
            }
        }
    }

    public void addValuesToDelete(Collection<V> newValues) {
        checkMutable();
        if (newValues == null) {
            return;
        }
        for (V val : newValues) {
            addValueToDelete(val);
        }
    }

    public void addValuesToDelete(V... newValues) {
        checkMutable();
        for (V val : newValues) {
            addValueToDelete(val);
        }
    }

    public void addValueToDelete(V newValue) {
        checkMutable();
        if (valuesToReplace != null) {
            throw new IllegalStateException("Delta " + this
                    + " already has values to replace ("+valuesToReplace+"), attempt to set value to delete ("+newValue+")");
        }
        if (valuesToDelete == null) {
            valuesToDelete = newValueCollection();
        }
        if (containsEquivalentValue(valuesToDelete, newValue)) {
            return;
        }
        valuesToDelete.add(newValue);
        newValue.setParent(this);
        newValue.recompute();
    }

    protected boolean containsEquivalentValue(Collection<V> collection, V value) {
        if (collection == null) {
            return false;
        }
        for (V colVal: collection) {
            if (isValueEquivalent(colVal, value)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isValueEquivalent(V a, V b) {
        return a.equals(b, EquivalenceStrategy.REAL_VALUE);
    }

    public void mergeValuesToDelete(Collection<V> newValues) {
        checkMutable();
        for (V val : newValues) {
            mergeValueToDelete(val);
        }
    }

    public void mergeValuesToDelete(V[] newValues) {
        checkMutable();
        for (V val : newValues) {
            mergeValueToDelete(val);
        }
    }

    public void mergeValueToDelete(V newValue) {
        checkMutable();
        if (valuesToReplace != null) {
            removeValueToReplace(newValue);
        } else {
            if (!removeValueToAdd(newValue)) {
                addValueToDelete(newValue);
            }
        }
    }

    public void resetValuesToAdd() {
        checkMutable();
        valuesToAdd = null;
    }

    public void resetValuesToDelete() {
        checkMutable();
        valuesToDelete = null;
    }

    public void resetValuesToReplace() {
        checkMutable();
        valuesToReplace = null;
    }

    public void setValuesToReplace(Collection<V> newValues) {
        checkMutable();
        if (newValues == null) {
            return;
        }
        if (valuesToAdd != null) {
            throw new IllegalStateException("Delta " + this
                    + " already has values to add ("+valuesToAdd+"), attempt to set value to replace ("+newValues+")");
        }
        if (valuesToDelete != null) {
            throw new IllegalStateException("Delta " + this
                    + " already has values to delete, attempt to set value to replace");
        }
        if (valuesToReplace == null) {
            valuesToReplace = newValueCollection();
        } else {
            valuesToReplace.clear();
        }
        for (V val : newValues) {
            valuesToReplace.add(val);
            val.setParent(this);
            val.recompute();
        }
    }

    public void setValuesToReplace(V... newValues) {
        checkMutable();
        if (valuesToAdd != null) {
            throw new IllegalStateException("Delta " + this
                    + " already has values to add, attempt to set value to replace");
        }
        if (valuesToDelete != null) {
            throw new IllegalStateException("Delta " + this
                    + " already has values to delete, attempt to set value to replace");
        }
        if (valuesToReplace == null) {
            valuesToReplace = newValueCollection();
        } else {
            valuesToReplace.clear();
        }
        for (V val : newValues) {
            valuesToReplace.add(val);
            val.setParent(this);
            val.recompute();
        }
    }

    /**
     * Sets empty value to replace. This efficiently means removing all values.
     */
    public void setValueToReplace() {
        checkMutable();
        if (valuesToReplace == null) {
            valuesToReplace = newValueCollection();
        } else {
            valuesToReplace.clear();
        }
    }

    public void setValueToReplace(V newValue) {
        checkMutable();
        if (valuesToAdd != null) {
            throw new IllegalStateException("Delta " + this
                    + " already has values to add, attempt to set value to replace");
        }
        if (valuesToDelete != null) {
            throw new IllegalStateException("Delta " + this
                    + " already has values to delete, attempt to set value to replace");
        }
        if (valuesToReplace == null) {
            valuesToReplace = newValueCollection();
        } else {
            valuesToReplace.clear();
        }
        if (newValue != null) {
            valuesToReplace.add(newValue);
            newValue.setParent(this);
            newValue.recompute();
        }
    }

    public void addValueToReplace(V newValue) {
        checkMutable();
        if (valuesToAdd != null) {
            throw new IllegalStateException("Delta " + this
                    + " already has values to add, attempt to set value to replace");
        }
        if (valuesToDelete != null) {
            throw new IllegalStateException("Delta " + this
                    + " already has values to delete, attempt to set value to replace");
        }
        if (valuesToReplace == null) {
            valuesToReplace = newValueCollection();
        }
        if (newValue != null) {
            valuesToReplace.add(newValue);
            newValue.setParent(this);
            newValue.recompute();
        }
    }

    public void mergeValuesToReplace(Collection<V> newValues) {
        checkMutable();
        // No matter what type the delta was before. We are just discarding all the previous
        // state as the replace that we are applying will overwrite that anyway.
        valuesToAdd = null;
        valuesToDelete = null;
        setValuesToReplace(newValues);
    }

    public void mergeValuesToReplace(V[] newValues) {
        checkMutable();
        // No matter what type the delta was before. We are just discarding all the previous
        // state as the replace that we are applying will overwrite that anyway.
        valuesToAdd = null;
        valuesToDelete = null;
        setValuesToReplace(newValues);
    }

    public void mergeValueToReplace(V newValue) {
        checkMutable();
        // No matter what type the delta was before. We are just discarding all the previous
        // state as the replace that we are applying will overwrite that anyway.
        valuesToAdd = null;
        valuesToDelete = null;
        setValueToReplace(newValue);
    }

    private Collection<V> newValueCollection() {
        return new ArrayList<>();
    }

    public boolean isValueToAdd(V value) {
        return isValueSet(value, false, valuesToAdd);
    }

    public boolean isValueToAdd(V value, boolean ignoreMetadata) {
        return isValueSet(value, ignoreMetadata, valuesToAdd);
    }

    public boolean isValueToDelete(V value) {
        return isValueSet(value, false, valuesToDelete);
    }

    public boolean isValueToDelete(V value, boolean ignoreMetadata) {
        return isValueSet(value, ignoreMetadata, valuesToDelete);
    }

    public boolean isValueToReplace(V value) {
        return isValueSet(value, false, valuesToReplace);
    }

    public boolean isValueToReplace(V value, boolean ignoreMetadata) {
        return isValueSet(value, ignoreMetadata, valuesToReplace);
    }

    private boolean isValueSet(V value, boolean ignoreMetadata, Collection<V> set) {
        if (set == null) {
            return false;
        }
        for (V myVal: set) {
            if (myVal.equals(value, ignoreMetadata ? EquivalenceStrategy.IGNORE_METADATA : EquivalenceStrategy.NOT_LITERAL)) {
                return true;
            }
        }
        return false;
    }

    public V getAnyValue() {
        V anyValue = getAnyValue(valuesToAdd);
        if (anyValue != null) {
            return anyValue;
        }
        anyValue = getAnyValue(valuesToDelete);
        if (anyValue != null) {
            return anyValue;
        }
        anyValue = getAnyValue(valuesToReplace);
        if (anyValue != null) {
            return anyValue;
        }
        return null;
    }

    private V getAnyValue(Collection<V> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        return set.iterator().next();
    }

    public boolean isEmpty() {
        return valuesToAdd == null && valuesToDelete == null && valuesToReplace == null;
    }

    // TODO merge with isEmpty
    public boolean isInFactEmpty() {
        return CollectionUtils.isEmpty(valuesToAdd) && CollectionUtils.isEmpty(valuesToDelete) && valuesToReplace == null;
    }

    public boolean addsAnyValue() {
        return hasAnyValue(valuesToAdd) || hasAnyValue(valuesToReplace);
    }

    private boolean hasAnyValue(Collection<V> set) {
        return (set != null && !set.isEmpty());
    }

    public void foreach(Processor<V> processor) {
        foreachSet(processor, valuesToAdd);
        foreachSet(processor, valuesToDelete);
        foreachSet(processor, valuesToReplace);
    }

    private void foreachSet(Processor<V> processor, Collection<V> set) {
        if (set == null) {
            return;
        }
        for (V val: set) {
            processor.process(val);
        }
    }

    /**
     * Returns estimated state of the old value before the delta is applied.
     * This information is not entirely reliable. The state might change
     * between the value is read and the delta is applied. This is property
     * is optional and even if provided it is only for for informational
     * purposes.
     *
     * If this method returns null then it should be interpreted as "I do not know".
     * In that case the delta has no information about the old values.
     * If this method returns empty collection then it should be interpreted that
     * we know that there were no values in this item before the delta was applied.
     *
     * @return estimated state of the old value before the delta is applied (may be null).
     */
    public Collection<V> getEstimatedOldValues() {
        return estimatedOldValues;
    }

    public void setEstimatedOldValues(Collection<V> estimatedOldValues) {
        checkMutable();
        this.estimatedOldValues = estimatedOldValues;
    }

    public void addEstimatedOldValues(Collection<V> newValues) {
        checkMutable();
        for (V val : newValues) {
            addEstimatedOldValue(val);
        }
    }

    public void addEstimatedOldValues(V... newValues) {
        checkMutable();
        for (V val : newValues) {
            addEstimatedOldValue(val);
        }
    }

    public void addEstimatedOldValue(V newValue) {
        checkMutable();
        if (estimatedOldValues == null) {
            estimatedOldValues = newValueCollection();
        }
        if (PrismValueCollectionsUtil.containsRealValue(estimatedOldValues,newValue)) {
            return;
        }
        estimatedOldValues.add(newValue);
        newValue.setParent(this);
        newValue.recompute();
    }

    public void normalize() {
        checkMutable();
        normalize(valuesToAdd);
        normalize(valuesToDelete);
        normalize(valuesToReplace);
    }

    private void normalize(Collection<V> set) {
        if (set == null) {
            return;
        }
        for (V value : set) {
            value.normalize();
        }
    }

    public boolean isReplace() {
        return (valuesToReplace != null);
    }

    public boolean isAdd() {
        return (valuesToAdd != null && !valuesToAdd.isEmpty());
    }

    public boolean isDelete() {
        return (valuesToDelete != null && !valuesToDelete.isEmpty());
    }

    public void clear() {
        checkMutable();
        valuesToReplace = null;
        valuesToAdd = null;
        valuesToDelete = null;
    }


    /**
     * Filters out all delta values that are meaningless to apply. E.g. removes all values to add that the property already has,
     * removes all values to delete that the property does not have, etc.
     * Returns null if the delta is not needed at all.
     *
     * @param assumeMissingItems Assumes that some items in the object may be missing. So replacing them by null or deleting some
     *                           values from them cannot be narrowed out.
     */
    public ItemDelta<V,D> narrow(PrismObject<? extends Objectable> object, boolean assumeMissingItems) {
        return narrow(object, null, assumeMissingItems);
    }
    /**
     * Filters out all delta values that are meaningless to apply. E.g. removes all values to add that the property already has,
     * removes all values to delete that the property does not have, etc.
     * Returns null if the delta is not needed at all.
     *
     * @param assumeMissingItems Assumes that some items in the object may be missing. So replacing them by null or deleting some
     *                           values from them cannot be narrowed out.
     */
    public ItemDelta<V,D> narrow(PrismObject<? extends Objectable> object, Comparator<V> comparator, boolean assumeMissingItems) {
        checkMutable();
        Item<V,D> currentItem = object.findItem(getPath());
        if (currentItem == null) {
            if (assumeMissingItems || object.isIncomplete()) {
                return this;        // we know nothing about missing item
            }
            if (valuesToDelete != null) {
                ItemDelta<V, D> clone = clone();
                clone.resetValuesToDelete();
                return clone;
            } else if (CollectionUtils.isEmpty(valuesToAdd) && CollectionUtils.isEmpty(valuesToReplace)) {
                return null;    // i.e. either "replace to ()" or "add ()" or empty delta altogether => can skip
            } else {
                // Nothing to narrow
                return this;
            }
        } else {
            if (isReplace()) {
                // We can narrow replace deltas only if the replace set matches
                // current item exactly. Otherwise we may lose some values.
                // And of course we can do this only on complete items.
                if (!currentItem.isIncomplete() && currentItem.valuesEqual(valuesToReplace, comparator)) {
                    return null;
                } else {
                    return this;
                }
            } else {
                ItemDelta<V,D> clone = clone();
                if (!currentItem.isIncomplete() && clone.getValuesToDelete() != null) {
                    clone.getValuesToDelete()
                            .removeIf(valueToDelete -> !currentItem.containsEquivalentValue(valueToDelete, comparator));
                    if (clone.getValuesToDelete().isEmpty()) {
                        clone.resetValuesToDelete();
                    }
                }
                if (clone.getValuesToAdd() != null) {
                    clone.getValuesToAdd().removeIf(
                            valueToAdd -> currentItem.containsEquivalentValue(valueToAdd, comparator)
                                    && !containsEquivalentValue(clone.getValuesToDelete(), valueToAdd));
                    if (clone.getValuesToAdd().isEmpty()) {
                        clone.resetValuesToAdd();
                    }
                }
                return clone;
            }
        }
    }

    /**
     * Checks if the delta is redundant w.r.t. current state of the object.
     * I.e. if it changes the current object state.
     */
    public boolean isRedundant(PrismObject<? extends Objectable> object, boolean assumeMissingItems) {
        Comparator<V> comparator = (o1, o2) -> {
            if (o1.equals(o2, EquivalenceStrategy.IGNORE_METADATA)) {
                return 0;
            } else {
                return 1;
            }
        };
        return isRedundant(object, comparator, assumeMissingItems);
    }

    public boolean isRedundant(PrismObject<? extends Objectable> object, Comparator<V> comparator, boolean assumeMissingItems) {
        Item<V,D> currentItem = object.findItem(getPath());
        if (currentItem == null) {
            if (valuesToReplace != null) {
                return valuesToReplace.isEmpty();
            }
            return !hasAnyValue(valuesToAdd);
        } else {
            if (valuesToReplace != null) {
                return MiscUtil.unorderedCollectionCompare(valuesToReplace, currentItem.getValues(), comparator);
            }
            ItemDeltaImpl<V,D> narrowed = (ItemDeltaImpl<V, D>) narrow(object, comparator, assumeMissingItems);
            return narrowed == null || narrowed.hasAnyValue(narrowed.valuesToAdd) || narrowed.hasAnyValue(narrowed.valuesToDelete);
        }
    }

    public void validate() throws SchemaException {
        validate(null);
    }

    public void validate(String contextDescription) throws SchemaException {
        if (definition == null) {
            throw new IllegalStateException("Attempt to validate delta without a definition: "+this);
        }
        if (definition.isSingleValue()) {
            if (valuesToAdd != null && valuesToAdd.size() > 1) {
                throw new SchemaException("Attempt to add "+valuesToAdd.size()+" values to a single-valued item "+getPath() +
                        (contextDescription == null ? "" : " in "+contextDescription) + "; values: "+valuesToAdd);
            }
            if (valuesToReplace != null && valuesToReplace.size() > 1) {
                throw new SchemaException("Attempt to replace "+valuesToReplace.size()+" values to a single-valued item "+getPath() +
                        (contextDescription == null ? "" : " in "+contextDescription) + "; values: "+valuesToReplace);
            }
        }
        if (definition.isMandatory()) {
            if (valuesToReplace != null && valuesToReplace.isEmpty()) {
                throw new SchemaException("Attempt to clear all values of a mandatory item "+getPath() +
                        (contextDescription == null ? "" : " in "+contextDescription));
            }
        }
    }

    public void validateValues(ItemDeltaValidator<V> validator) throws SchemaException {
        validateValues(validator, getEstimatedOldValues());
    }

    public void validateValues(ItemDeltaValidator<V> validator, Collection<V> oldValues) throws SchemaException {
        validateSet(valuesToAdd, PlusMinusZero.PLUS, validator);
        validateSet(valuesToDelete, PlusMinusZero.MINUS, validator);
        if (isReplace()) {
            for (V val: getValuesToReplace()) {
                if (oldValues != null && PrismValueCollectionsUtil.containsRealValue(oldValues, val)) {
                    validator.validate(PlusMinusZero.ZERO, val);
                } else {
                    validator.validate(PlusMinusZero.PLUS, val);
                }
            }
            if (oldValues != null) {
                for (V val: getValuesToReplace()) {
                    if (!PrismValueCollectionsUtil.containsRealValue(getValuesToReplace(), val)) {
                        validator.validate(PlusMinusZero.MINUS, val);
                    }
                }
            }
        }
    }

    private void validateSet(Collection<V> set, PlusMinusZero plusMinusZero,
            ItemDeltaValidator<V> validator) {
        if (set != null) {
            for (V val: set) {
                validator.validate(plusMinusZero, val);
            }
        }
    }

    public void checkConsistence() {
        checkConsistence(ConsistencyCheckScope.THOROUGH);
    }

    public void checkConsistence(ConsistencyCheckScope scope) {
        checkConsistence(false, false, scope);
    }

    public void checkConsistence(boolean requireDefinition, boolean prohibitRaw, ConsistencyCheckScope scope) {
        if (scope.isThorough() && parentPath == null) {
            throw new IllegalStateException("Null parent path in " + this);
        }
        if (scope.isThorough() && requireDefinition && definition == null) {
            throw new IllegalStateException("Null definition in "+this);
        }
        if (scope.isThorough() && valuesToReplace != null && (valuesToAdd != null || valuesToDelete != null)) {
            throw new IllegalStateException(
                    "The delta cannot be both 'replace' and 'add/delete' at the same time");
        }
        assertSetConsistence(valuesToReplace, "replace", requireDefinition, prohibitRaw, scope);
        assertSetConsistence(valuesToAdd, "add", requireDefinition, prohibitRaw, scope);
        assertSetConsistence(valuesToDelete, "delete", requireDefinition, prohibitRaw, scope);
    }

    private void assertSetConsistence(Collection<V> values, String type, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
        if (values == null) {
            return;
        }
        // This may be not be 100% correct but we can tolerate it now
        // if (values.isEmpty()) {
        // throw new
        // IllegalStateException("The "+type+" values set in "+this+" is not-null but it is empty");
        // }
        for (V val : values) {
            if (scope.isThorough()) {
                if (val == null) {
                    throw new IllegalStateException("Null value in the " + type + " values set in " + this);
                }
                if (val.getParent() != this) {
                    throw new IllegalStateException("Wrong parent for " + val + " in " + type + " values set in " + this + ": " + val.getParent());
                }
            }
            val.checkConsistenceInternal(this, requireDefinitions, prohibitRaw, scope);
        }
    }

    /**
     * Distributes the replace values of this delta to add and delete with
     * respect to provided existing values.
     */
    public void distributeReplace(Collection<V> existingValues) {
        checkMutable();
        Collection<V> origValuesToReplace = getValuesToReplace();
        // We have to clear before we distribute, otherwise there will be replace/add or replace/delete conflict
        clearValuesToReplace();
        if (existingValues != null) {
            for (V existingVal : existingValues) {
                if (!isIn(origValuesToReplace, existingVal)) {
                    addValueToDelete((V) existingVal.clone());
                }
            }
        }
        for (V replaceVal : origValuesToReplace) {
            if (!isIn(existingValues, replaceVal) && !isIn(getValuesToAdd(), replaceVal)) {
                addValueToAdd((V) replaceVal.clone());
            }
        }
    }

    private boolean isIn(Collection<V> values, V val) {
        if (values == null) {
            return false;
        }
        for (V v : values) {
            if (v.equals(val, EquivalenceStrategy.REAL_VALUE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Merge specified delta to this delta. This delta is assumed to be
     * chronologically earlier, delta provided in the parameter is chronologically later.
     *
     * TODO do we expect that the paths of "this" delta and deltaToMerge are the same?
     * From the code it seems so.
     */
    public void merge(ItemDelta<V,D> deltaToMerge) {
        checkMutable();
//        if (!getPath().equivalent(deltaToMerge.getPath())) {
//            throw new AssertionError("Different paths in itemDelta merge: this=" + this + ", deltaToMerge=" + deltaToMerge);
//        }
        if (deltaToMerge.isEmpty()) {
            return;
        }
        if (deltaToMerge.getValuesToReplace() != null) {
            mergeValuesToReplace(PrismValueCollectionsUtil.cloneValues(deltaToMerge.getValuesToReplace()));
        } else {
            if (deltaToMerge.getValuesToAdd() != null) {
                mergeValuesToAdd(PrismValueCollectionsUtil.cloneValues(deltaToMerge.getValuesToAdd()));
            }
            if (deltaToMerge.getValuesToDelete() != null) {
                mergeValuesToDelete(PrismValueCollectionsUtil.cloneValues(deltaToMerge.getValuesToDelete()));
            }
        }

        // merge old values only if anything present. Merge them just first time
        if (deltaToMerge.getEstimatedOldValues() != null) {
            mergeOldValues(PrismValueCollectionsUtil.cloneValues(deltaToMerge.getEstimatedOldValues()));
        }

        // We do not want to clean up the sets during merging (e.g. in removeValue methods) because the set
        // may become empty and the a values may be added later. So just clean it up when all is done.
        removeEmptySets();
    }

    private void mergeOldValues(Collection<V> oldValues) {
        // there are situations when we don't know old value when firstly create delta
        // so if there estimatedOldValues are null we try to merge them from current delta
        // if estimatedOldValues != null we don't do anything
        if (estimatedOldValues != null) {
            return;
        }

        estimatedOldValues = newValueCollection();

        for (V oldValue : oldValues) {
            estimatedOldValues.add(oldValue);
        }
    }

    private void removeEmptySets() {
        // Do not remove replace set, even if it is empty.
        // Empty replace set is not the same as no replace set
        if (valuesToAdd != null && valuesToAdd.isEmpty()) {
            valuesToAdd = null;
        }
        if (valuesToDelete != null && valuesToDelete.isEmpty()) {
            valuesToDelete = null;
        }
    }

    public Collection<V> getValueChanges(PlusMinusZero mode) {
        Collection<V> out = new ArrayList<>();

        if (isReplace()) {
            switch (mode) {
                case PLUS:
                    setSubtract(out, valuesToReplace, estimatedOldValues);
                    break;
                case MINUS:
                    setSubtract(out, estimatedOldValues, valuesToReplace);
                    break;
                case ZERO:
                    setIntersection(out, valuesToReplace, estimatedOldValues);
            }
        } else {
            switch (mode) {
                case PLUS:
                    setAddAll(out, valuesToAdd);
                    break;
                case MINUS:
                    setAddAll(out, valuesToDelete);
                    break;
                case ZERO:
                    setAddAll(out, estimatedOldValues);
                    break;
            }
        }

        return out;
    }

    private void setSubtract(Collection<V> out, Collection<V> subtrahend, Collection<V> minuend) {
        if (subtrahend == null) {
            return;
        }
        for (V sube: subtrahend) {
            if (minuend == null) {
                out.add(sube);
            } else {
                if (!minuend.contains(sube)) {
                    out.add(sube);
                }
            }
        }
    }

    private void setIntersection(Collection<V> out, Collection<V> a, Collection<V> b) {
        if (a == null || b == null) {
            return;
        }
        for (V ae: a) {
            if (b.contains(ae)) {
                out.add(ae);
            }
        }
    }

    private void setAddAll(Collection<V> out, Collection<V> in) {
        if (in != null) {
            out.addAll(in);
        }
    }

    /**
     * Transforms the delta to the simplest (and safest) form. E.g. it will transform add delta for
     * single-value properties to replace delta.
     */
    public void simplify() {
        checkMutable();
        ItemDefinition itemDefinition = getDefinition();
        if (itemDefinition == null) {
            throw new IllegalStateException("Attempt to simplify delta without a definition");
        }
        if (itemDefinition.isSingleValue() && isAdd()) {
            valuesToReplace = valuesToAdd;
            valuesToAdd = null;
            valuesToDelete = null;
        }
    }

    private void cleanupAllTheWayUp(Item<?,?> item) {
        if (item.isEmpty()) {
            PrismValue itemParent = item.getParent();
            if (itemParent != null) {
                ((PrismContainerValue<?>)itemParent).remove(item);
                if (itemParent.isEmpty()) {
                    Itemable itemGrandparent = itemParent.getParent();
                    if (itemGrandparent != null) {
                        if (itemGrandparent instanceof Item<?,?>) {
                            cleanupAllTheWayUp((Item<?,?>)itemGrandparent);
                        }
                    }
                }
            }
        }
    }

    public void applyTo(PrismContainerValue containerValue) throws SchemaException {
        applyTo(containerValue, ParameterizedEquivalenceStrategy.DEFAULT_FOR_DELTA_APPLICATION);
    }

    public void applyTo(PrismContainerValue containerValue, ParameterizedEquivalenceStrategy strategy) throws SchemaException {
        ItemPath deltaPath = getPath();
        if (ItemPath.isEmpty(deltaPath)) {
            throw new IllegalArgumentException("Cannot apply empty-path delta " + this + " directly to a PrismContainerValue " + containerValue);
        }
        Item subItem = containerValue.findOrCreateItem(deltaPath, getItemClass(), getDefinition());
        applyToMatchingPath(subItem, strategy);
    }

    public void applyTo(Item item) throws SchemaException {
        applyTo(item, ParameterizedEquivalenceStrategy.DEFAULT_FOR_DELTA_APPLICATION);
    }

    public void applyTo(Item item, ParameterizedEquivalenceStrategy strategy) throws SchemaException {
        ItemPath itemPath = item.getPath();
        ItemPath deltaPath = getPath();
        CompareResult compareComplex = itemPath.compareComplex(deltaPath);
        if (compareComplex == CompareResult.EQUIVALENT) {
            applyToMatchingPath(item, strategy);
            cleanupAllTheWayUp(item);
        } else if (compareComplex == CompareResult.SUBPATH) {
            if (item instanceof PrismContainer<?>) {
                PrismContainer<?> container = (PrismContainer<?>)item;
                ItemPath remainderPath = deltaPath.remainder(itemPath);
                Item subItem = container.findOrCreateItem(remainderPath, getItemClass(), getDefinition());
                applyToMatchingPath(subItem, strategy);
            } else {
                throw new SchemaException("Cannot apply delta "+this+" to "+item+" as delta path is below the item path and the item is not a container");
            }
        } else if (compareComplex == CompareResult.SUPERPATH) {
            throw new SchemaException("Cannot apply delta "+this+" to "+item+" as delta path is above the item path");
        } else if (compareComplex == CompareResult.NO_RELATION) {
            throw new SchemaException("Cannot apply delta "+this+" to "+item+" as paths do not match (item:"+itemPath+", delta:"+deltaPath+")");
        }
    }

    /**
     * Applies delta to item were path of the delta and path of the item matches (skips path checks).
     */
    public void applyToMatchingPath(Item item, ParameterizedEquivalenceStrategy strategy) throws SchemaException {
        if (item == null) {
            return;
        }
        if (item.getDefinition() == null && getDefinition() != null){
            //noinspection unchecked
            item.applyDefinition(getDefinition());
        }
        if (!getItemClass().isAssignableFrom(item.getClass())) {
            throw new SchemaException("Cannot apply delta "+this+" to "+item+" because the deltas is applicable only to "+getItemClass().getSimpleName());
        }
        if (valuesToReplace != null) {
            // FIXME This is a temporary solution (ugly hack). We do this to avoid O(n^2) comparisons when replacing
            //  a lot of values, like 100K members for a group. But the serious solution is to employ hashing in ItemImpl,
            //  and resolve this efficiency issue (1) for all cases - i.e. ADD+DELETE+REPLACE, (2) preserving uniqueness checking.
            //  See MID-5889.
            item.clear();
            //noinspection unchecked
            item.addAll(PrismValueCollectionsUtil.cloneCollection(valuesToReplace), false, strategy);
        } else {
            if (valuesToDelete != null) {
                //noinspection unchecked
                item.removeAll(valuesToDelete);
            }
            if (valuesToAdd != null) {
                if (item.getDefinition() != null && item.getDefinition().isSingleValue()) {
                    //noinspection unchecked
                    item.replaceAll(PrismValueCollectionsUtil.cloneCollection(valuesToAdd), strategy);
                } else {
                    for (V valueToAdd : valuesToAdd) {
                        //noinspection unchecked
                        if (!item.contains(valueToAdd, strategy)) {
                            //noinspection unchecked
                            item.add(valueToAdd.clone(), false);
                        }
                    }
                }
            }
        }
        // Application of delta might have removed values therefore leaving empty items.
        // Those needs to be cleaned-up (removed) as empty item is not a legal state.
        cleanupAllTheWayUp(item);
    }

    public ItemDelta<?,?> getSubDelta(ItemPath path) {
        return this;
    }

    public boolean isApplicableTo(Item item) {
        if (item == null) {
            return false;
        }
        if (!isApplicableToType(item)) {
            return false;
        }
        // TODO: maybe check path?
        return true;
    }

    protected abstract boolean isApplicableToType(Item item);

    /**
     * Returns the "new" state of the property - the state that would be after
     * the delta is applied.
     *
     * WARNING: Output of this method should be used for preview only.
     * It should NOT be placed into prism structures. Not even cloned.
     * This method may return dummy items or similar items that are not usable.
     * Values in the items should be OK, but they may need cloning.
     */
    public Item<V,D> getItemNew() throws SchemaException {
        return getItemNew(null);
    }

    /**
     * Returns the "new" state of the property - the state that would be after
     * the delta is applied.
     *
     * WARNING: Output of this method should be used for preview only.
     * It should NOT be placed into prism structures. Not even cloned.
     * This method may return dummy items or similar items that are not usable.
     * Values in the items should be OK, but they may need cloning.
     */
    public Item<V,D> getItemNew(Item<V,D> itemOld) throws SchemaException {
        if (definition == null) {
            throw new IllegalStateException("No definition in "+this);
        }
        if (isEmpty()) {
            return itemOld;
        }
        Item<V,D> itemNew;
        // We cannot just instantiate or clone the item here. Path will not match. E.g. the deltas may work on subitems of this container.
        // We need to create a dummy item that has a proper path.
        itemNew = prismContext.itemFactory().createDummyItem(itemOld, definition, fullPath);
        applyTo(itemNew);
        return itemNew;
    }

    /**
     * Returns the "new" state of the property - the state that would be after
     * the delta is applied.
     *
     * WARNING: This is supposed to work only if the paths of the the delta and the item matches.
     * E.g. it is NOT usable for application of subdeltas to containers.
     */
    public Item<V,D> getItemNewMatchingPath(Item<V,D> itemOld) throws SchemaException {
        if (definition == null) {
            throw new IllegalStateException("No definition in "+this);
        }
        Item<V,D> itemNew;
        if (itemOld == null) {
            if (isEmpty()) {
                return null;
            }
            itemNew = definition.instantiate(getElementName());
        } else {
            itemNew = itemOld.clone();
        }
        applyToMatchingPath(itemNew, ParameterizedEquivalenceStrategy.DEFAULT_FOR_DELTA_APPLICATION);
        return itemNew;
    }

    /**
     * Returns true if the other delta is a complete subset of this delta.
     * I.e. if all the statements of the other delta are already contained
     * in this delta. As a consequence it also returns true if the two
     * deltas are equal.
     */
    public boolean contains(ItemDelta<V,D> other) {
        return contains(other, EquivalenceStrategy.REAL_VALUE);
    }
    /**
     * Returns true if the other delta is a complete subset of this delta.
     * I.e. if all the statements of the other delta are already contained
     * in this delta. As a consequence it also returns true if the two
     * deltas are equal.
     */

    public boolean contains(ItemDelta<V,D> other, EquivalenceStrategy strategy) {
        if (!this.getPath().equivalent(other.getPath())) {
            return false;
        }
        if (!PrismValueCollectionsUtil.containsAll(this.valuesToAdd, other.getValuesToAdd(), strategy)) {
            return false;
        }
        if (!PrismValueCollectionsUtil.containsAll(this.valuesToDelete, other.getValuesToDelete(), strategy)) {
            return false;
        }
        if (!PrismValueCollectionsUtil.containsAll(this.valuesToReplace, other.getValuesToReplace(), strategy)) {
            return false;
        }
        return true;
    }

    public void filterValues(Function<V, Boolean> function) {
        checkMutable();
        filterValuesSet(this.valuesToAdd, function);
        filterValuesSet(this.valuesToDelete, function);
        filterValuesSet(this.valuesToReplace, function);
    }

    private void filterValuesSet(Collection<V> set, Function<V, Boolean> function) {
        if (set == null) {
            return;
        }
        Iterator<V> iterator = set.iterator();
        while (iterator.hasNext()) {
            Boolean keep = function.apply(iterator.next());
            if (keep == null || !keep) {
                iterator.remove();
            }
        }
    }

    public abstract ItemDeltaImpl<V,D> clone();

    public ItemDeltaImpl<V,D> cloneWithChangedParentPath(ItemPath newParentPath) {
        ItemDeltaImpl<V,D> clone = clone();
        clone.setParentPath(newParentPath);
        return clone;
    }

    protected void copyValues(ItemDeltaImpl<V,D> clone) {
        clone.definition = this.definition;
        clone.elementName = this.elementName;
        clone.parentPath = this.parentPath;
        clone.fullPath = this.fullPath;
        clone.valuesToAdd = cloneSet(clone, this.valuesToAdd);
        clone.valuesToDelete = cloneSet(clone, this.valuesToDelete);
        clone.valuesToReplace = cloneSet(clone, this.valuesToReplace);
        clone.estimatedOldValues = cloneSet(clone, this.estimatedOldValues);
    }

    private Collection<V> cloneSet(ItemDeltaImpl clone, Collection<V> thisSet) {
        if (thisSet == null) {
            return null;
        }
        Collection<V> clonedSet = newValueCollection();
        for (V thisVal : thisSet) {
            V clonedVal = (V) thisVal.clone();
            clonedVal.setParent(clone);
            clonedSet.add(clonedVal);
        }
        return clonedSet;
    }

    public PrismValueDeltaSetTriple<V> toDeltaSetTriple() {
        return toDeltaSetTriple(null);
    }

    public PrismValueDeltaSetTriple<V> toDeltaSetTriple(Item<V,D> itemOld) {
        PrismValueDeltaSetTriple<V> triple = new PrismValueDeltaSetTripleImpl<>();
        if (isReplace()) {
            triple.getPlusSet().addAll(PrismValueCollectionsUtil.cloneCollection(getValuesToReplace()));
            if (itemOld != null) {
                triple.getMinusSet().addAll(PrismValueCollectionsUtil.cloneCollection(itemOld.getValues()));
            }
            return triple;
        }
        if (isAdd()) {
            triple.getPlusSet().addAll(PrismValueCollectionsUtil.cloneCollection(getValuesToAdd()));
        }
        if (isDelete()) {
            triple.getMinusSet().addAll(PrismValueCollectionsUtil.cloneCollection(getValuesToDelete()));
        }
        if (itemOld != null && itemOld.getValues() != null) {
            for (V itemVal: itemOld.getValues()) {
                if (!PrismValueCollectionsUtil.containsRealValue(valuesToDelete, itemVal) && !PrismValueCollectionsUtil
                        .containsRealValue(valuesToAdd, itemVal)) {
                    triple.getZeroSet().add((V) itemVal.clone());
                }
            }
        }
        return triple;
    }

    public void assertDefinitions(String sourceDescription) throws SchemaException {
            assertDefinitions(false, sourceDescription);
    }

    public void assertDefinitions(boolean tolarateRawValues, String sourceDescription) throws SchemaException {
        if (tolarateRawValues && isRaw()) {
            return;
        }
        if (definition == null) {
            throw new SchemaException("No definition in "+this+" in "+sourceDescription);
        }
        assertDefinitions(tolarateRawValues, valuesToAdd, "values to add in "+sourceDescription);
        assertDefinitions(tolarateRawValues, valuesToReplace, "values to replace in "+sourceDescription);
        assertDefinitions(tolarateRawValues, valuesToDelete, "values to delete in "+sourceDescription);
    }

    private void assertDefinitions(boolean tolarateRawValues, Collection<V> values, String sourceDescription) throws SchemaException {
        if (values == null) {
            return;
        }
        for(V val: values) {
            if (val instanceof PrismContainerValue<?>) {
                PrismContainerValue<?> cval = (PrismContainerValue<?>)val;
                for (Item<?,?> item: cval.getItems()) {
                    item.assertDefinitions(tolarateRawValues, cval.toString()+" in "+sourceDescription);
                }
            }
        }
    }

    public boolean isRaw() {
        Boolean isRaw = MiscUtil.and(isRawSet(valuesToAdd), isRawSet(valuesToReplace), isRawSet(valuesToDelete));
        if (isRaw == null) {
            return false;
        }
        return isRaw;
    }

    private Boolean isRawSet(Collection<V> set) {
        if (set == null) {
            return null;
        }
        for (V val: set) {
            if (!val.isRaw()) {
                return false;
            }
        }
        return true;
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        this.prismContext = prismContext;
        reviveSet(valuesToAdd, prismContext);
        reviveSet(valuesToDelete, prismContext);
        reviveSet(valuesToReplace, prismContext);
    }

    private void reviveSet(Collection<V> set, PrismContext prismContext) throws SchemaException {
        if (set == null) {
            return;
        }
        for (V val: set) {
            val.revive(prismContext);
        }
    }

    public void applyDefinition(D itemDefinition, boolean force) throws SchemaException {
        if (this.definition != null && !force) {
            return;
        }
        this.definition = itemDefinition;
        applyDefinitionSet(valuesToAdd, itemDefinition, force);
        applyDefinitionSet(valuesToReplace, itemDefinition, force);
        applyDefinitionSet(valuesToDelete, itemDefinition, force);
    }

    private void applyDefinitionSet(Collection<V> set, ItemDefinition itemDefinition, boolean force) throws SchemaException {
        if (set == null) {
            return;
        }
        for (V val: set) {
            val.applyDefinition(itemDefinition, force);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((definition == null) ? 0 : definition.hashCode());
        result = prime * result + ((elementName == null) ? 0 : elementName.hashCode());
        // if equals uses parentPath.equivalent call, we should not use default implementation of parentPath.hashCode
        //result = prime * result + ((parentPath == null) ? 0 : parentPath.hashCode());
        return result;
    }

    /**
     * Deltas are equivalent if they have the same result when
     * applied to an object. I.e. meta-data and other "decorations"
     * such as old values are not considered in this comparison.
     */
    public boolean equivalent(ItemDelta other) {
        if (elementName == null) {
            if (other.getElementName() != null) return false;
        } else if (!QNameUtil.match(elementName, elementName)) {
            return false;
        }
        if (parentPath == null) {
            if (other.getParentPath() != null) return false;
        } else if (!parentPath.equivalent(other.getParentPath())) {
            return false;
        }
        if (!equivalentSetRealValue(this.valuesToAdd, other.getValuesToAdd(), false)) {
            return false;
        }
        if (!equivalentSetRealValue(this.valuesToDelete, other.getValuesToDelete(), true)) {
            return false;
        }
        if (!equivalentSetRealValue(this.valuesToReplace, other.getValuesToReplace(), false)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ItemDeltaImpl other = (ItemDeltaImpl) obj;
        if (definition == null) {
            if (other.definition != null) return false;
        } else if (!definition.equals(other.definition)) {
            return false;
        }
        if (elementName == null) {
            if (other.elementName != null) return false;
        } else if (!elementName.equals(other.elementName)) {
            return false;
        }
        if (parentPath == null) {
            if (other.parentPath != null) return false;
        } else if (!parentPath.equivalent(other.parentPath)) {                   // or "equals" ?
            return false;
        }
        if (!equivalentSetRealValue(this.valuesToAdd, other.valuesToAdd, false)) {
            return false;
        }
        if (!equivalentSetRealValue(this.valuesToDelete, other.valuesToDelete, true)) {       // TODO ok?
            return false;
        }
        if (!equivalentSetRealValue(this.valuesToReplace, other.valuesToReplace, false)) {
            return false;
        }
        if (!equivalentSetRealValue(this.estimatedOldValues, other.estimatedOldValues, false)) {
            return false;
        }
        return true;
    }

    private boolean equivalentSetRealValue(Collection<V> thisValue, Collection<V> otherValues, boolean isDelete) {
        return MiscUtil.unorderedCollectionEquals(thisValue, otherValues,
                (v1, v2) -> {
                    if (v1 != null && v2 != null) {
                        if (!isDelete || !(v1 instanceof PrismContainerValue) || !(v2 instanceof PrismContainerValue)) {
                            // Here it is questionable if we should consider adding "assignment id=1 (A)" and "assignment id=2 (A)"
                            // - i.e. assignments with the same real value but different identifiers - the same delta.
                            // Historically, we considered it as such. But the question is if it's correct.
                            return v1.equals(v2, EquivalenceStrategy.REAL_VALUE);
                        } else {
                            // But for container values to be deleted, they can be referred to either using IDs or values.
                            // If content is used - but no IDs - the content must be equal.
                            // If IDs are used - and are the same - the content is irrelevant.
                            // The problem is if one side has content with ID, and the other has the same content without ID.
                            // This might have the same or different effect, depending on the content it is applied to.
                            // See MID-3828
                            return v1.equals(v2, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS) || v1.representsSameValue(v2, false);
                        }
                    } else {
                        return false;
                    }
                });
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("(");
        sb.append(parentPath).append(" / ").append(PrettyPrinter.prettyPrint(elementName));
        if (valuesToReplace != null) {
            sb.append(", REPLACE");
        }

        if (valuesToAdd != null) {
            sb.append(", ADD");
        }

        if (valuesToDelete != null) {
            sb.append(", DELETE");
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        if (DebugUtil.isDetailedDebugDump()) {
            sb.append(getClass().getSimpleName()).append(":");
        }
        ItemPath path = getPath();
        sb.append(path);

        if (definition != null && DebugUtil.isDetailedDebugDump()) {
            sb.append(" ").append(definition);
        }

        if (valuesToReplace != null) {
            sb.append("\n");
            dumpValues(sb, "REPLACE", valuesToReplace, indent + 1);
        }

        if (valuesToAdd != null) {
            sb.append("\n");
            dumpValues(sb, "ADD", valuesToAdd, indent + 1);
        }

        if (valuesToDelete != null) {
            sb.append("\n");
            dumpValues(sb, "DELETE", valuesToDelete, indent + 1);
        }

        if (estimatedOldValues != null) {
            sb.append("\n");
            dumpValues(sb, "OLD", estimatedOldValues, indent + 1);
        }

        return sb.toString();

    }

    protected void dumpValues(StringBuilder sb, String label, Collection<V> values, int indent) {
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(label).append(": ");
        if (values == null) {
            sb.append("(null)");
        } else {
            if (DebugUtil.isDetailedDebugDump()) {
                for (V value: values) {
                    sb.append("\n");
                    sb.append(value.debugDump(indent + 1));
                }
            } else {
                Iterator<V> i = values.iterator();
                while (i.hasNext()) {
                    V value = i.next();
                    sb.append(value.toHumanReadableString());
                    if (i.hasNext()) {
                        sb.append(", ");
                    }
                }
            }
        }
    }

    public void addToReplaceDelta() {
        checkMutable();
        if (isReplace()) {
            throw new IllegalStateException("Delta is a REPLACE delta, not an ADD one");
        }
        valuesToReplace = valuesToAdd;
        if (valuesToReplace == null) {
            valuesToReplace = new ArrayList<>(0);
        }
        valuesToAdd = null;
    }

    public ItemDelta<V,D> createReverseDelta() {
        ItemDeltaImpl<V,D> reverseDelta = clone();
        Collection<V> cloneValuesToAdd = reverseDelta.valuesToAdd;
        Collection<V> cloneValuesToDelete = reverseDelta.valuesToDelete;
        Collection<V> cloneValuesToReplace = reverseDelta.valuesToReplace;
        Collection<V> cloneEstimatedOldValues = reverseDelta.estimatedOldValues;

        reverseDelta.valuesToAdd = cloneValuesToDelete;
        reverseDelta.valuesToDelete = cloneValuesToAdd;
        if (cloneValuesToReplace != null) {
            reverseDelta.valuesToReplace = cloneEstimatedOldValues;
            if (reverseDelta.valuesToReplace == null) {
                // We want to force replace delta here. Otherwise the reverse delta
                // may look like empty. We do not explicitly have old values here,
                // so this is a bit tricky and not entirely correct. But we can live
                // with that for now.
                reverseDelta.valuesToReplace = new ArrayList<>(0);
            }
            reverseDelta.estimatedOldValues = cloneValuesToReplace;
        } else {
            // TODO: what about estimatedOldValues here?
        }

        return reverseDelta;
    }

    public V findValueToAddOrReplace(V value) {
        V found = findValue(valuesToAdd, value);
        if (found == null) {
            found = findValue(valuesToReplace, value);
        }
        return found;
    }

    private V findValue(Collection<V> values, V value) {
        if (values == null) {
            return null;
        }
        return values.stream()
                .filter(v -> v.equals(value, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS))
                .findFirst().orElse(null);
    }

    /**
     * Set origin type to all values and subvalues
     */
    public void setOriginTypeRecursive(final OriginType originType) {
        checkMutable();
        accept((visitable) -> {
            if (visitable instanceof PrismValue) {
                ((PrismValue)visitable).setOriginType(originType);
            }
        });
    }
}
