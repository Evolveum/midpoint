/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
public class PrismReferenceValueWrapperImpl<T extends Referencable> extends PrismValueWrapperImpl<T, PrismReferenceValue> {

    private static final long serialVersionUID = 1L;

    public PrismReferenceValueWrapperImpl(PrismReferenceWrapper<T> parent, PrismReferenceValue value, ValueStatus status) {
        super(parent, value, status);
    }

    private boolean editEnabled = true;
    private boolean isLink = false;

    @Override
    public void setRealValue(T realValueReferencable) {
        PrismReferenceValue value = getNewValue();
        PrismReferenceValue realValue = realValueReferencable.asReferenceValue();
        value.setOid(realValue.getOid());
        value.setOriginType(realValue.getOriginType());
        value.setOriginObject(realValue.getOriginObject());
        value.setTargetName(realValue.getTargetName());
        value.setTargetType(realValue.getTargetType());
        value.setRelation(realValue.getRelation());
        value.setFilter(realValue.getFilter());

        setStatus(ValueStatus.MODIFIED);
    }

    public boolean isEditEnabled() {
        return editEnabled;
    }

    public void setEditEnabled(boolean editEnabled) {
        this.editEnabled = editEnabled;
    }

    public boolean isLink() {
        return isLink;
    }

    public void setLink(boolean link) {
        isLink = link;
    }
}
