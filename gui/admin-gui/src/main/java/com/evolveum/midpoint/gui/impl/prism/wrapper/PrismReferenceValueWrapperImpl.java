/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

/**
 * @author katka
 *
 */
public class PrismReferenceValueWrapperImpl<T extends Referencable> extends PrismValueWrapperImpl<T> {

    private static final long serialVersionUID = 1L;

    public PrismReferenceValueWrapperImpl(PrismReferenceWrapper<T> parent, PrismReferenceValue value, ValueStatus status) {
        super(parent, value, status);
    }

    private boolean editEnabled = true;
    private boolean isLink = false;

    @Override
    public void setRealValue(T realValueReferencable) {
        PrismReferenceValue value = getNewValue();
        if (realValueReferencable == null) {
            value.setOid(null);
            value.setOriginType(null);
            value.setOriginObject(null);
            value.setTargetName((PolyStringType) null);
            value.setTargetType(null);
            value.setRelation(null);
            value.setFilter(null);

            setStatus(ValueStatus.MODIFIED);
            return;
        }
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

    @Override
    public PrismReferenceValue getNewValue() {
        return super.getNewValue();
    }

    @Override
    public String toShortString() {
        T referencable = getRealValue();
        if (referencable == null) {
            return "";
        }

        return getRefName(referencable) + " (" + getTargetType(referencable) + ")";
    }

    private String getRefName(T referencable) {
        return referencable.getTargetName() != null ? WebComponentUtil.getOrigStringFromPoly(referencable.getTargetName()) : referencable.getOid();
    }

    private String getTargetType(T referencable) {
        QName type = referencable.getType();
        return type != null ? type.getLocalPart() : "";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createIndentedStringBuilder(indent);
        sb.append(getNewValue().debugDump())
                .append(" (").append(getStatus()).append(", old: ").append(getOldValue().debugDump()).append(")");
        return sb.toString();
    }
}
