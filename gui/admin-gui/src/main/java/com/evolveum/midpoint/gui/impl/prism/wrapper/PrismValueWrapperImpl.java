/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.util.ExecutedDeltaPostProcessor;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.binding.AbstractPlainStructured;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationAttemptDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

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
                if (getNewValue().isEmpty()) {
                    break;
                }
                Object realValue = getNewValue().getRealValueIfExists();
                if (realValue instanceof AbstractPlainStructured && !isChanged()) {
                    // if empty AbstractPlainStructured value was used as placeholder, e.g. old and new values equal and
                    // state is ADDED then we don't really want to add it
                    // In MID-9564 case was that existing value was removed (marked as DELETED) and new value (with null real
                    // value) was added (state ADDED) as placeholder. Panel then had to set real value placeholder,
                    // e.g. ExpressionType object which changed state of wrapper to MODIFIED which is not correct since it's
                    // not modification of existing value
                    //
                    // Really not sure whether this is correct solution, since real value placeholders are needed (for UI panels),
                    // therefore having empty PPV with null real value is not possible.
                    // Drawback of this solution is that empty AbstractPlainStructured value will not be added to delta - if
                    // it's not done "manually" via PrismPropertyValueWrapper.setRealValue()
                    //
                    // Also see comment in ItemWrapperImpl.remove().

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
                    if (getNewValue().isEmpty())  {
                        // if old value is empty, nothing to do.
                        if (!getOldValue().isEmpty()) {
                            delta.addValueToDelete(getOldValue().clone());
                        }
                    } else {
                        delta.addValueToReplace(getNewValueWithMetadataApplied());
                    }
                    break;
                }

                if (!getNewValue().isEmpty()) {
                    delta.addValueToAdd(getNewValueWithMetadataApplied());
                }
                if (!getOldValue().isEmpty()) {
                    delta.addValueToDelete(getOldValue().clone());
                }
                break;
            case DELETED:
                if (getOldValue() != null && !getOldValue().isEmpty()) {
                    delta.addValueToDelete(getOldValue().clone());
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

            newValue.setValueMetadata(newValueMetadata.clone()); //TODO possible NPE here
        }

        return (V) getNewValue().clone();
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

    @Override
    public Collection<ExecutedDeltaPostProcessor> getPreconditionDeltas(ModelServiceLocator serviceLocator, OperationResult result) throws CommonException {
        return null;
    }

    protected final void setNewValue(PrismValue newValue) {
        this.newValue = newValue;
    }
}
