/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import javax.xml.namespace.QName;

/**
 * @author katka
 *
 */
public abstract class PrismValueWrapperImpl<T, V extends PrismValue> implements PrismValueWrapper<T, V> {

    private static final long serialVersionUID = 1L;

    private ItemWrapper<?,?,?,?> parent;

    private V oldValue;
    private V newValue;

    private ValueStatus status;


    PrismValueWrapperImpl(ItemWrapper<?, ?, ?, ?> parent, V value, ValueStatus status) {
        this.parent = parent;
        this.newValue = value;
        this.oldValue = (V) value.clone();
        this.status = status;
    }

    @Override
    public <D extends ItemDelta<V, ID>, ID extends ItemDefinition> void addToDelta(D delta) throws SchemaException {
        switch (status) {
            case ADDED:
                if (newValue.isEmpty()) {
                    break;
                }
                if (parent.isSingleValue()) {
                    delta.addValueToReplace((V) newValue.clone());
                } else {
                    delta.addValueToAdd((V) newValue.clone());
                }
                break;
            case NOT_CHANGED:
                if (!isChanged()) {
                    break;
                }
            case MODIFIED:

                if (parent.isSingleValue()) {
                    if (newValue.isEmpty())  {
                        delta.addValueToDelete((V) oldValue.clone());
                    } else {
                        delta.addValueToReplace((V) newValue.clone());
                    }
                    break;
                }

                if (!newValue.isEmpty()) {
                    delta.addValueToAdd((V) newValue.clone());
                }
                if (!oldValue.isEmpty()) {
                    delta.addValueToDelete((V) oldValue.clone());
                }
                break;
            case DELETED:
                if (oldValue != null && !oldValue.isEmpty()) {
                    delta.addValueToDelete((V) oldValue.clone());
                }
                break;
            default:
                break;
        }

//        parent.applyDelta((ItemDelta) delta);
    }


    protected boolean isChanged() {
        if (QNameUtil.match(DOMUtil.XSD_QNAME, getParent().getTypeName())) {
            QName newValueQname = newValue != null ? (QName) newValue.getRealValue() : null;
            QName oldValueQName = oldValue != null ? (QName) oldValue.getRealValue() : null;
            return !QNameUtil.match(newValueQname, oldValueQName);
        }
        return !oldValue.equals(newValue, EquivalenceStrategy.REAL_VALUE);
    }

    @Override
    public T getRealValue() {
        return newValue.getRealValue();
    }


    @Override
    public V getNewValue() {
        return newValue;
    }

    @Override
    public V getOldValue() {
        return oldValue;
    }

    @Override
    public <IW extends ItemWrapper> IW getParent() {
        return (IW) parent;
    }

    @Override
    public ValueStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(ValueStatus status) {
        this.status = status;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createIndentedStringBuilder(indent);
        sb.append("Status: ").append(status).append("\n");
        sb.append("New value: ").append(newValue.debugDump()).append("\n");
        sb.append("Old value: ").append(oldValue.debugDump()).append("\n");
        return sb.toString();
    }

}
