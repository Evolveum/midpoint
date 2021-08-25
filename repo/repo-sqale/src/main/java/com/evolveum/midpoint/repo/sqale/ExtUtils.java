/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.google.common.collect.ImmutableMap;

/**
 * Utilities and constants related to extension item processing, especially as JSONB.
 */
public class ExtUtils {

    /**
     * Supported types for extension properties - without references and enums treated differently.
     */
    public static final Set<QName> SUPPORTED_INDEXED_EXTENSION_TYPES = Set.of(
            DOMUtil.XSD_BOOLEAN,
            DOMUtil.XSD_INT,
            DOMUtil.XSD_LONG,
            DOMUtil.XSD_SHORT,
            DOMUtil.XSD_INTEGER,
            DOMUtil.XSD_DECIMAL,
            DOMUtil.XSD_STRING,
            DOMUtil.XSD_DOUBLE,
            DOMUtil.XSD_FLOAT,
            DOMUtil.XSD_DATETIME,
            PolyStringType.COMPLEX_TYPE);

    public static final Map<String,QName> SUPPORTED_TYPE_URI_TO_QNAME;
    public static final Map<String, Class<?>> SUPPORTED_TYPE_URI_TO_REAL_CLASS = ImmutableMap.<String,Class<?>>builder()
            .put(QNameUtil.qNameToUri(DOMUtil.XSD_INT), Integer.class)
            .put(QNameUtil.qNameToUri(DOMUtil.XSD_LONG), Long.class)
            .put(QNameUtil.qNameToUri(DOMUtil.XSD_SHORT), Short.class)
            .put(QNameUtil.qNameToUri(DOMUtil.XSD_DECIMAL), BigDecimal.class)
            .put(QNameUtil.qNameToUri(DOMUtil.XSD_INTEGER), BigInteger.class)
            .put(QNameUtil.qNameToUri(DOMUtil.XSD_STRING), String.class)
            .put(QNameUtil.qNameToUri(DOMUtil.XSD_FLOAT), Float.class)
            .put(QNameUtil.qNameToUri(DOMUtil.XSD_DOUBLE), Double.class)
            .put(QNameUtil.qNameToUri(DOMUtil.XSD_DATETIME), XMLGregorianCalendar.class)
            .put(QNameUtil.qNameToUri(PolyStringType.COMPLEX_TYPE), PolyString.class)

            .build();

    static {
        HashMap<String, QName> uriMap = new HashMap<>();
        for (QName name : SUPPORTED_INDEXED_EXTENSION_TYPES) {
            uriMap.put(QNameUtil.qNameToUri(name), name);

        }



        SUPPORTED_TYPE_URI_TO_QNAME = Collections.unmodifiableMap(uriMap);
    }

    public static boolean isEnumDefinition(PrismPropertyDefinition<?> definition) {
        Collection<? extends DisplayableValue<?>> allowedValues = definition.getAllowedValues();
        return allowedValues != null && !allowedValues.isEmpty();
    }

    public static String extensionDateTime(@NotNull XMLGregorianCalendar dateTime) {
        //noinspection ConstantConditions
        return MiscUtil.asInstant(dateTime)
                .truncatedTo(ChronoUnit.MILLIS)
                .toString();
    }
}
