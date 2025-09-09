/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import java.io.Serial;

/**
 * @author katka
 */
public class PrismPropertyValueWrapper<T> extends PrismValueWrapperImpl<T> {

    /**
     * @param parent
     * @param value
     * @param status
     */
    public PrismPropertyValueWrapper(PrismPropertyWrapper<T> parent, PrismPropertyValue<T> value, ValueStatus status) {
        super(parent, value, status);
    }

    @Serial private static final long serialVersionUID = 1L;

    @Override
    protected <V extends PrismValue> V getNewValueWithMetadataApplied() throws SchemaException {
        V value = super.getNewValueWithMetadataApplied();
        WebPrismUtil.cleanupValueMetadata(value);
        return value;
    }

    @Override
    public void setRealValue(T newRealValue) {

        newRealValue = trimValueIfNeeded(newRealValue);

        if (newRealValue == null) {
            if (getRealValue() == null) {
                //nothing to do, value vas not changed
                return;
            }

            getNewValue().setValue(null);
            return;
        }

        if (newRealValue instanceof QName) {
            if (QNameUtil.match((QName) newRealValue, (QName) getRealValue())) {
                return;
            }
        } else if (newRealValue instanceof ItemPath) {
            if (((ItemPath) newRealValue).equivalent((ItemPath) getRealValue())) {
                return;
            }
        } else {
            if (newRealValue.equals(getRealValue())) {
                return;
            }
        }

        getNewValue().setValue(newRealValue);
        setStatus(ValueStatus.MODIFIED);
    }

    private T trimValueIfNeeded(T realValue) {
        if (ValueStatus.ADDED == getStatus() || ValueStatus.MODIFIED == getStatus()) {
            if (realValue instanceof String) {
                return (T) ((String) realValue).trim();
            }

            if (realValue instanceof PolyString) {

                PolyString polyString = (PolyString) realValue;
                String polyStringOrig = polyString.getOrig();
                if (StringUtils.isEmpty(polyStringOrig)) {
                    return realValue;
                }

                String trimmed = polyStringOrig.trim();
                PolyString newPolyString = new PolyString(trimmed);
                newPolyString.setLang(polyString.getLang());
                newPolyString.setTranslation(polyString.getTranslation());
                return (T) newPolyString;

            }
        }

        return realValue;
    }

    public String toShortString() {
        if (getRealValue() == null) {
            return null;
        }

        if (getParent() == null) {
            return getRealValue().toString();
        }

        QName typeName = getParent().getTypeName();

        if (QNameUtil.match(DOMUtil.XSD_QNAME, typeName)) {
            return ((QName) getRealValue()).getLocalPart();
        }

        if (QNameUtil.match(DOMUtil.XSD_DATETIME, typeName)) {
            return WebComponentUtil.getLocalizedDate((XMLGregorianCalendar) getRealValue(), DateLabelComponent.FULL_MEDIUM_STYLE);
        }

        return getRealValue().toString();
    }

    @Override
    public PrismPropertyValue<T> getNewValue() {
        return super.getNewValue();
    }

    @Override
    public PrismPropertyValue<T> getOldValue() {
        return super.getOldValue();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createIndentedStringBuilder(indent);
        sb.append(getNewValue().debugDump())
                .append(" (").append(getStatus()).append(", old: ").append(getOldValue().debugDump()).append(")");
        return sb.toString();
    }
}
