/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Utilities and constants related to extension item processing, especially as JSONB.
 */
public class ExtUtils {

    /**
     * Supported types for extension properties - without references and enums treated differently.
     */
    private static final Map<String, SupportedExtensionTypeInfo> SUPPORTED_INDEXED_EXTENSION_TYPES = new HashMap<>();

    static {
        addType(DOMUtil.XSD_BOOLEAN, Boolean.class);
        addType(DOMUtil.XSD_INT, Integer.class);
        addType(DOMUtil.XSD_LONG, Long.class);
        addType(DOMUtil.XSD_SHORT, Short.class);
        addType(DOMUtil.XSD_INTEGER, BigInteger.class);
        addType(DOMUtil.XSD_DECIMAL, BigDecimal.class);
        addType(DOMUtil.XSD_STRING, String.class);
        addType(DOMUtil.XSD_FLOAT, Float.class);
        addType(DOMUtil.XSD_DOUBLE, Double.class);
        addType(DOMUtil.XSD_DATETIME, XMLGregorianCalendar.class);
        addType(PolyStringType.COMPLEX_TYPE, PolyString.class);
    }

    private static void addType(QName typeName, Class<?> valueClass) {
        String uri = QNameUtil.qNameToUri(typeName);
        SUPPORTED_INDEXED_EXTENSION_TYPES.put(uri,
                new SupportedExtensionTypeInfo(uri, typeName, valueClass));
    }

    /**
     * Returns expected class for real values for registered types, or `null`.
     */
    public static @Nullable Class<?> getRealValueClass(String typeUri) {
        SupportedExtensionTypeInfo info = SUPPORTED_INDEXED_EXTENSION_TYPES.get(typeUri);
        return info != null ? info.realValueClass : null;
    }

    public static boolean isRegisteredType(QName typeName) {
        return SUPPORTED_INDEXED_EXTENSION_TYPES.containsKey(QNameUtil.qNameToUri(typeName));
    }

    public static QName getSupportedTypeName(String typeUri) {
        return SUPPORTED_INDEXED_EXTENSION_TYPES.get(typeUri).typeName;
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

    /** Creates definition from {@link MExtItem}. */
    public static ItemDefinition<?> createDefinition(QName name, MExtItem itemInfo, boolean indexOnly) {
        QName typeName = ExtUtils.getSupportedTypeName(itemInfo.valueType);
        final MutableItemDefinition<?> def;
        if (ObjectReferenceType.COMPLEX_TYPE.equals(typeName)) {
            def = PrismContext.get().definitionFactory().createReferenceDefinition(name, typeName);
        } else {
            def = PrismContext.get().definitionFactory().createPropertyDefinition(name, typeName);
        }
        def.setMinOccurs(0);
        def.setMaxOccurs(-1);
        def.setRuntimeSchema(true);
        def.setDynamic(true);
        def.setIndexOnly(indexOnly);
        return def;
    }

    public static class SupportedExtensionTypeInfo {
        public final String uri; // see QNameUtil.qNameToUri()
        public final QName typeName;
        public final Class<?> realValueClass;

        public SupportedExtensionTypeInfo(String uri, QName typeName, Class<?> realValueClass) {
            this.uri = uri;
            this.typeName = typeName;
            this.realValueClass = realValueClass;
        }
    }
}
