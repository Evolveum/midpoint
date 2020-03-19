/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import org.apache.commons.lang.StringUtils;

/**
 * @author katka
 *
 */
public class PrismPropertyValueWrapper<T> extends PrismValueWrapperImpl<T, PrismPropertyValue<T>> {

    /**
     * @param parent
     * @param value
     * @param status
     */
    public PrismPropertyValueWrapper(ItemWrapper<?, ?, ?, ?> parent, PrismPropertyValue<T> value, ValueStatus status) {
        super(parent, value, status);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public void setRealValue(T newRealValue) {

        newRealValue = trimValueIfNeeded(newRealValue);

        if (newRealValue == null) {
            getNewValue().setValue(null);
            setStatus(ValueStatus.DELETED);
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
            if (realValue instanceof  String) {
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
        if (typeName == null) {
            return getRealValue().toString();
        }

        if (QNameUtil.match(DOMUtil.XSD_QNAME, typeName)) {
            return ((QName)getRealValue()).getLocalPart();
        }

        return getRealValue().toString();
    }

}
