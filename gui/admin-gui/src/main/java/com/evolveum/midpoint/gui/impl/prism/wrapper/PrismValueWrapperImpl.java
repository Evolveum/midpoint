/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationAttemptDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author katka
 *
 */
public abstract class PrismValueWrapperImpl<T> implements PrismValueWrapper<T> {

    private static final long serialVersionUID = 1L;

    private ItemWrapper<?,?> parent;

    private PrismValue oldValue;
    private PrismValue newValue;

    private ValueStatus status;
    private ValueMetadataWrapperImpl valueMetadata;
    private boolean showMetadata;


    PrismValueWrapperImpl(ItemWrapper<?, ?> parent, PrismValue value, ValueStatus status) {
        this.parent = parent;
        this.newValue = value;
        if (value != null) {
            this.oldValue = value.clone();
        }
        this.status = status;
    }

    @Override
    public <D extends ItemDelta<PrismValue, ? extends ItemDefinition>> void addToDelta(D delta) throws SchemaException {
        switch (status) {
            case ADDED:
                if (newValue.isEmpty()) {
                    break;
                }
                if (parent.isSingleValue()) {
                    delta.addValueToReplace(getNewValueWithMetadataApplied());
                } else {
                    delta.addValueToAdd(getNewValueWithMetadataApplied());
                }
                break;
            case NOT_CHANGED:
                if (!isChanged()) {
                    break;
                }
            case MODIFIED:

                if (parent.isSingleValue()) {
                    if (newValue.isEmpty())  {
                        // if old value is empty, nothing to do.
                        if (!oldValue.isEmpty()) {
                            delta.addValueToDelete(oldValue.clone());
                        }
                    } else {
                        delta.addValueToReplace(getNewValueWithMetadataApplied());
                    }
                    break;
                }

                if (!newValue.isEmpty()) {
                    delta.addValueToAdd(getNewValueWithMetadataApplied());
                }
                if (!oldValue.isEmpty()) {
                    delta.addValueToDelete(oldValue.clone());
                }
                break;
            case DELETED:
                if (oldValue != null && !oldValue.isEmpty()) {
                    delta.addValueToDelete(oldValue.clone());
                }
                break;
            default:
                break;
        }

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
    public <V extends PrismValue> V getNewValue() {
        return (V) newValue;
    }

    protected <V extends PrismValue> V getNewValueWithMetadataApplied() throws SchemaException {
        if (getParent() != null && getParent().isProcessProvenanceMetadata()) {
            PrismContainerValue<ValueMetadataType> newYieldValue = WebPrismUtil.getNewYieldValue();

            MidPointApplication app = MidPointApplication.get();
            ValueMetadata newValueMetadata = app.getPrismContext().getValueMetadataFactory().createEmpty();
            newValueMetadata.addMetadataValue(newYieldValue);

            newValue.setValueMetadata(newValueMetadata.clone());
        }

        return (V) newValue.clone();
    }

    @Override
    public <V extends PrismValue> V getOldValue() {
        return (V) oldValue;
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
        return sb.toString();
    }

    public boolean isVisible() {
        return !ValueStatus.DELETED.equals(getStatus());
    }

    @Override
    public void setValueMetadata(ValueMetadataWrapperImpl valueMetadata) {
        this.valueMetadata = valueMetadata;
    }

    @Override
    public ValueMetadataWrapperImpl getValueMetadata() {
        return valueMetadata;
    }

    @Override
    public boolean isShowMetadata() {
        return showMetadata;
    }

    public void setShowMetadata(boolean showMetadata) {
        this.showMetadata = showMetadata;
    }

    @Override
    public String toShortString() {
        if (getRealValue() == null) {
            return "";
        }
        return getRealValue().toString();
    }

    @Override
    public <C extends Containerable> PrismContainerValueWrapper<C> getParentContainerValue(@NotNull Class<? extends C> parentClass) {
        ItemWrapper parent = getParent();
        if (parent == null || parent instanceof PrismObjectWrapper) {
            return null;
        }
        if (parent instanceof PrismContainerWrapper && parentClass.equals(parent.getTypeClass())) {
            return (PrismContainerValueWrapper<C>) this;
        }
        return parent.getParentContainerValue(parentClass);
    }
}
