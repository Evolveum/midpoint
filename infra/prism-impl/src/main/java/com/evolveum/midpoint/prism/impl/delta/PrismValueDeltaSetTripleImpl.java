/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.Processor;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Collection;
import java.util.Iterator;

/**
 * DeltaSetTriple that is limited to hold prism values. By limiting to the PrismValue descendants we gain advantage to be
 * cloneable and ability to compare real values.
 *
 * @author Radovan Semancik
 */
public class PrismValueDeltaSetTripleImpl<V extends PrismValue> extends DeltaSetTripleImpl<V> implements
        PrismValueDeltaSetTriple<V> {

    public PrismValueDeltaSetTripleImpl() {
        super();
    }

    public PrismValueDeltaSetTripleImpl(Collection<V> zeroSet, Collection<V> plusSet, Collection<V> minusSet) {
        super(zeroSet, plusSet, minusSet);
    }

    /**
     * Distributes a value in this triple similar to the placement of other value in the other triple.
     * E.g. if the value "otherMember" is in the zero set in "otherTriple" then "myMember" will be placed
     * in zero set in this triple.
     */
    public <O extends PrismValue> void distributeAs(V myMember, PrismValueDeltaSetTriple<O> otherTriple, O otherMember) {
        otherTriple.getZeroSet();
        if (PrismValueCollectionsUtil.containsRealValue(otherTriple.getZeroSet(), otherMember)) {
            zeroSet.add(myMember);
        }
        otherTriple.getPlusSet();
        if (PrismValueCollectionsUtil.containsRealValue(otherTriple.getPlusSet(), otherMember)) {
            plusSet.add(myMember);
        }
        otherTriple.getMinusSet();
        if (PrismValueCollectionsUtil.containsRealValue(otherTriple.getMinusSet(), otherMember)) {
            minusSet.add(myMember);
        }
    }

    @Override
    protected boolean presentInSet(Collection<V> set, V item) {
        return PrismValueCollectionsUtil.containsRealValue(set, item);
    }

    public Class<V> getValueClass() {
        V anyValue = getAnyValue();
        if (anyValue == null) {
            return null;
        }
        return (Class<V>) anyValue.getClass();
    }

    public Class<?> getRealValueClass() {
        V anyValue = getAnyValue();
        if (anyValue == null) {
            return null;
        }
        if (anyValue instanceof PrismPropertyValue<?>) {
            PrismPropertyValue<?> pval = (PrismPropertyValue<?>)anyValue;
            Object realValue = pval.getValue();
            if (realValue == null) {
                return null;
            }
            return realValue.getClass();
        } else {
            return null;
        }
    }

    public boolean isRaw() {
        return (isRaw(zeroSet) || isRaw(plusSet) || isRaw(minusSet));
    }

    private boolean isRaw(Collection<V> set) {
        if (set == null) {
            return false;
        }
        for (V item: set) {
            if (item.isRaw()) {
                return true;
            }
        }
        return false;
    }

    public void applyDefinition(ItemDefinition itemDefinition) throws SchemaException {
        applyDefinition(zeroSet, itemDefinition);
        applyDefinition(plusSet, itemDefinition);
        applyDefinition(minusSet, itemDefinition);
    }

    private void applyDefinition(Collection<V> set, ItemDefinition itemDefinition) throws SchemaException {
        if (set == null) {
            return;
        }
        for (V item: set) {
            item.applyDefinition(itemDefinition);
        }
    }

    /**
     * Sets specified source type for all values in all sets
     */
    public void setOriginType(OriginType sourceType) {
        setOriginType(zeroSet, sourceType);
        setOriginType(plusSet, sourceType);
        setOriginType(minusSet, sourceType);
    }

    private void setOriginType(Collection<V> set, OriginType sourceType) {
        if (set != null) {
            for (V val: set) {
                val.setOriginType(sourceType);
            }
        }
    }

    /**
     * Sets specified origin object for all values in all sets
     */
    public void setOriginObject(Objectable originObject) {
        setOriginObject(zeroSet, originObject);
        setOriginObject(plusSet, originObject);
        setOriginObject(minusSet, originObject);
    }

    private void setOriginObject(Collection<V> set, Objectable originObject) {
        if (set != null) {
            for (V val: set) {
                val.setOriginObject(originObject);
            }
        }
    }

    public void removeEmptyValues(boolean allowEmptyValues) {
        removeEmptyValues(plusSet, allowEmptyValues);
        removeEmptyValues(zeroSet, allowEmptyValues);
        removeEmptyValues(minusSet, allowEmptyValues);
    }


    private void removeEmptyValues(Collection<V> set, boolean allowEmptyRealValues) {
        if (set == null) {
            return;
        }
        Iterator<V> iterator = set.iterator();
        while (iterator.hasNext()) {
            V val = iterator.next();
            if (val == null || val.isEmpty()) {
                iterator.remove();
                continue;
            }
            if (!allowEmptyRealValues) {
                if (val instanceof PrismPropertyValue<?>) {
                    Object realValue = ((PrismPropertyValue<?>)val).getValue();
                    if (realValue instanceof String) {
                        if (((String)realValue).isEmpty()) {
                            iterator.remove();
                            continue;
                        }
                    } else if (realValue instanceof PolyString) {
                        if (((PolyString)realValue).isEmpty()) {
                            iterator.remove();
                            continue;
                        }
                    }
                }
            }
        }
    }

    public PrismValueDeltaSetTriple<V> clone() {
        PrismValueDeltaSetTripleImpl<V> clone = new PrismValueDeltaSetTripleImpl<>();
        copyValues(clone);
        return clone;
    }

    protected void copyValues(PrismValueDeltaSetTripleImpl<V> clone) {
        super.copyValues(clone, original -> (V) original.clone());
    }

    public void checkConsistence() {
        Visitor visitor = visitable -> {
            if (visitable instanceof PrismValue) {
                if (((PrismValue)visitable).isEmpty()) {
                    throw new IllegalStateException("Empty value "+visitable+" in triple "+PrismValueDeltaSetTripleImpl.this);
                }
            }
        };
        accept(visitor);

        Processor<V> processor = pval -> {
            if (pval.getParent() != null) {
                if (pval instanceof PrismObjectValue) {
                    // Object values are exceptions from this rule. They could have a parent. TODO reconsider this.
                } else {
                    throw new IllegalStateException("Value " + pval + " in triple " + PrismValueDeltaSetTripleImpl.this + " has parent, looks like it was not cloned properly");
                }
            }
        };
        foreach(processor);
    }

    @Override
    public void accept(Visitor visitor) {
        acceptSet(zeroSet, visitor);
        acceptSet(plusSet, visitor);
        acceptSet(minusSet, visitor);
    }

    private void acceptSet(Collection<V> set, Visitor visitor) {
        if (set == null) {
            return;
        }
        for (V val: set) {
            val.accept(visitor);
        }
    }

    public void checkNoParent() {
        checkNoParent(zeroSet, "zero");
        checkNoParent(plusSet, "plus");
        checkNoParent(minusSet, "minus");
    }

    private void checkNoParent(Collection<V> set, String desc) {
        if (set == null) {
            return;
        }
        for (V val: set) {
            if (val.getParent() != null) {
                throw new IllegalStateException("Value "+val+" in "+desc+" triple set "+this+" has a parrent "+val.getParent()+". This is unexpected");
            }
        }
    }

    protected String debugName() {
        return "PVDeltaSetTriple";
    }

    @Override
    protected void toHumanReadableString(StringBuilder sb, V item) {
        sb.append(item.toHumanReadableString());
    }

}
