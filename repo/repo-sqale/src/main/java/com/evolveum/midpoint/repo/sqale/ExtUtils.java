/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Utilities and constants related to extension item processing, especially as JSONB.
 */
public class ExtUtils {

    public static final String EXT_POLY_ORIG_KEY = "o";
    public static final String EXT_POLY_NORM_KEY = "n";

    public static final String EXT_REF_TARGET_OID_KEY = "o";
    public static final String EXT_REF_TARGET_TYPE_KEY = "t";
    public static final String EXT_REF_RELATION_KEY = "r";

    /**
     * Supported types for extension properties - without references and enums treated differently.
     */
    static final Set<QName> SUPPORTED_INDEXED_EXTENSION_TYPES = Set.of(
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
