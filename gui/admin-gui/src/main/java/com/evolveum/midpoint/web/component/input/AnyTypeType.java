/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DOMUtil;

import org.jetbrains.annotations.Nullable;

public enum AnyTypeType {

    STRING(DOMUtil.XSD_STRING, String.class),

    INTEGER(DOMUtil.XSD_INTEGER, Integer.class),

    INT(DOMUtil.XSD_INT, Integer.class),

    BOOLEAN(DOMUtil.XSD_BOOLEAN, Boolean.class),

    BYTE(DOMUtil.XSD_BASE64BINARY, byte[].class),

    DATE_TIME(DOMUtil.XSD_DATETIME, XMLGregorianCalendar.class),

    LONG(DOMUtil.XSD_LONG, Long.class);

    public final QName xsdType;

    public final Class<?> type;

    AnyTypeType(QName xsdType, Class<?> type) {
        this.xsdType = xsdType;
        this.type = type;
    }

    public static @Nullable AnyTypeType fromObject(@Nullable Object object) {
        if (object == null) {
            return STRING;
        }

        Class<?> type = object.getClass();
        for (AnyTypeType anyTypeType : values()) {
            if (anyTypeType.type.isAssignableFrom(type)) {
                return anyTypeType;
            }
        }

        return null;
    }
}
