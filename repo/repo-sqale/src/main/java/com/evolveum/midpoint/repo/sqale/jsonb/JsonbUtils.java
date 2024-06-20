/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.jsonb;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

public enum JsonbUtils {
    ;

    public static final String JSONB_POLY_ORIG_KEY = "o";
    public static final String JSONB_POLY_NORM_KEY = "n";
    public static final String JSONB_REF_TARGET_OID_KEY = "o";
    public static final String JSONB_REF_TARGET_TYPE_KEY = "t";
    public static final String JSONB_REF_RELATION_KEY = "r";

    private static Map<Class<?>, Function<Number, Number>> numberConverters;

    static {

        numberConverters = ImmutableMap.<Class<?>, Function<Number, Number>>builder()
                .put(numberConverter(Byte.class, Number::byteValue))
                .put(numberConverter(Double.class, Number::doubleValue))
                .put(numberConverter(Float.class, Number::floatValue))
                .put(numberConverter(Long.class, Number::longValue))
                .put(numberConverter(Short.class, Number::shortValue))
                .put(numberConverter(byte.class, Number::byteValue))
                .put(numberConverter(double.class, Number::doubleValue))
                .put(numberConverter(float.class, Number::floatValue))
                .put(numberConverter(long.class, Number::longValue))
                .put(numberConverter(short.class, Number::shortValue))
                .build();
            //numberConverter(BigInteger.class, BigInteger);
            //numberConverter(BigDecimal.class, DOMUtil.XSD_DECIMAL, true);
        }

    @SuppressWarnings("rawtypes")
    private static <T extends Number> Map.Entry<Class<?>,Function<Number, Number>> numberConverter(Class<T> targetClass, Function<Number, T> function) {
        return new AbstractMap.SimpleEntry(targetClass, function);


    }

    public static Jsonb polyStringTypesToJsonb(Collection<PolyStringType> polys) {
        if (polys == null || polys.isEmpty()) {
            return null;
        }
        return Jsonb.fromList(polys.stream()
                .map(JsonbUtils::polyStringToMap)
                .collect(Collectors.toList()));
    }

    public static Jsonb polyStringsToJsonb(Collection<PolyString> polys) {
        if (polys == null || polys.isEmpty()) {
            return null;
        }
        return Jsonb.fromList(polys.stream()
                .map(JsonbUtils::polyStringToMap)
                .collect(Collectors.toList()));
    }

    @NotNull
    public static Map<String, String> polyStringToMap(@NotNull PolyString poly) {
        return Map.of(JSONB_POLY_ORIG_KEY, poly.getOrig(),
                JSONB_POLY_NORM_KEY, poly.getNorm());
    }

    @NotNull
    public static Map<String, String> polyStringToMap(@NotNull PolyStringType poly) {
        return Map.of(JSONB_POLY_ORIG_KEY, poly.getOrig(),
                JSONB_POLY_NORM_KEY, poly.getNorm());
    }

    public static Object toRealValue(Object jsonValue, @NotNull QName type, SqaleRepoContext repositoryContext) {
        if (jsonValue instanceof Map<?,?> map) {
            if (PolyStringType.COMPLEX_TYPE.equals(type)) {
                String orig = (String) map.get(JSONB_POLY_ORIG_KEY);
                String norm = (String) map.get(JSONB_POLY_NORM_KEY);
                return new PolyString(orig, norm);
            }
            if (ObjectReferenceType.COMPLEX_TYPE.equals(type)) {
                var relation = QNameUtil.uriToQName(
                        repositoryContext.resolveIdToUri(
                                (Integer) map.get(JSONB_REF_RELATION_KEY)));
                var ret = new ObjectReferenceType();
                ret.setOid((String) map.get(JSONB_REF_TARGET_OID_KEY));
                ret.setType(MObjectType.valueOf((String) map.get(JSONB_REF_TARGET_TYPE_KEY)).getTypeName());
                ret.setRelation(relation);
                return ret;
            }
        }
        if (jsonValue instanceof String str) {
            // Use standard codecs
            if (XmlTypeConverter.canConvert(type)) {
                return XmlTypeConverter.toJavaValue(str, type);
            }
        }
        if (jsonValue instanceof Number num) {
            return convertNumber(num, type);

        }
        // FIXME: Add other more specific handlers
        return jsonValue;
    }

    private static Number convertNumber(Number num, QName typeName) {
        var type = XsdTypeMapper.getXsdToJavaMapping(typeName);
        if (type.isInstance(num)) {
            return num;
        }
        var converter = numberConverters.get(type);
        if (converter != null) {
            return converter.apply(num);
        }

        return num;
    }
}
