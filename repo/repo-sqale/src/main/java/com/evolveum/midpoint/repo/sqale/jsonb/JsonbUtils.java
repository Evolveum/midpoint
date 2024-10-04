/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.jsonb;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.prism.xml.ns._public.types_3.*;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.polystring.PolyString;

import javax.xml.namespace.QName;

public enum JsonbUtils {
    ;

    public static final String JSONB_POLY_ORIG_KEY = "o";
    public static final String JSONB_POLY_NORM_KEY = "n";
    public static final String JSONB_REF_TARGET_OID_KEY = "o";
    public static final String JSONB_REF_TARGET_TYPE_KEY = "t";
    public static final String JSONB_REF_RELATION_KEY = "r";
    private static final String PROTECTED_CLEARTEXT_KEY = "ct";


    private static final String PROTECTED_HASHED_DIGEST_SALT_KEY = "hs";
    private static final String PROTECTED_HASHED_DIGEST_ALGORHITM_KEY = "ha";
    private static final String PROTECTED_HASHED_DIGEST_WORK_FACTOR_KEY = "hw";
    private static final String PROTECTED_HASHED_DIGEST_VALUE_KEY = "hv";
    private static final String PROTECTED_ENCRYPTED_KEYNAME_KEY = "ek";
    private static final String PROTECTED_ENCRYPTED_ALGORHITM_KEY = "ea";
    private static final String PROTECTED_ENCRYPTED_CIPHER_DATA_KEY = "ed";
    private static final String PROTECTED_EXTERNAL_PROVIDER_KEY = "exp";
    private static final String PROTECTED_EXTERNAL_KEY_KEY = "exk";

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
                var schemaType = MObjectType.valueOf((String) map.get(JSONB_REF_TARGET_TYPE_KEY)).getSchemaType();
                ret.setType(ObjectTypes.getObjectType(schemaType).getTypeQName());
                ret.setRelation(relation);
                return ret;
            }
            if (ProtectedStringType.COMPLEX_TYPE.equals(type)) {
                return protectedStringType(map);
            }
            if (ProtectedByteArrayType.COMPLEX_TYPE.equals(type)) {
                return protectedByteType(map);
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

    private static ProtectedStringType protectedStringType(Map<?,?> map) {
        var ret = new ProtectedStringType();
        readProtectedData(map, ret, String.class);
        return ret;
    }

    private static ProtectedByteArrayType protectedByteType(Map<?,?> map) {
        var ret = new ProtectedByteArrayType();
        readProtectedData(map, ret, Byte[].class);
        return ret;
    }

    private static <T> void readProtectedData(Map<?,?> map, ProtectedDataType<T> ret, Class<T> clearType) {
        ret.setClearValue(readFromMap(map, clearType, PROTECTED_CLEARTEXT_KEY));
        ret.setHashedData(readHashData(map));
        ret.setEncryptedData(readEncryptedData(map));
    }

    private static EncryptedDataType readEncryptedData(Map<?,?> map) {
        var encryptedKeyname = readFromMap(map, String.class, PROTECTED_ENCRYPTED_KEYNAME_KEY);
        var encryptedAlgorhitm = readFromMap(map, String.class, PROTECTED_ENCRYPTED_ALGORHITM_KEY);
        var encryptedCipherData = readFromMap(map, byte[].class, PROTECTED_ENCRYPTED_CIPHER_DATA_KEY);

        if (encryptedKeyname != null || encryptedAlgorhitm != null || encryptedCipherData != null) {
            var encrypted = new EncryptedDataType();
            if (encryptedCipherData != null) {
                var cipherData = new CipherDataType();
                cipherData.setCipherValue(encryptedCipherData);
                encrypted.setCipherData(cipherData);
            }

            var keyInfo = new KeyInfoType();
            keyInfo.setKeyName(encryptedKeyname);
            encrypted.setKeyInfo(keyInfo);

            var method = new EncryptionMethodType();
            method.setAlgorithm(encryptedAlgorhitm);
            encrypted.setEncryptionMethod(method);
            return encrypted;
        }
        return null;
    }

    private static HashedDataType readHashData(Map<?,?> map) {
        var hashedValue = readFromMap(map, byte[].class, PROTECTED_HASHED_DIGEST_VALUE_KEY);
        var hashedSalt= readFromMap(map, byte[].class, PROTECTED_HASHED_DIGEST_SALT_KEY);
        var hashedAlgorithm= readFromMap(map, String.class, PROTECTED_HASHED_DIGEST_ALGORHITM_KEY);
        var hashedWorkFactor= readFromMap(map, Integer.class, PROTECTED_HASHED_DIGEST_WORK_FACTOR_KEY);

        if (hashedValue != null || hashedSalt != null || hashedAlgorithm != null || hashedWorkFactor != null) {
            var hashed = new HashedDataType();

            hashed.setDigestValue(hashedValue);
            var method = new DigestMethodType();
            method.setAlgorithm(hashedAlgorithm);
            method.setSalt(hashedSalt);
            method.setWorkFactor(hashedWorkFactor);
            hashed.setDigestMethod(method);
            return hashed;
        }
        return null;
    }



    public static Object protectedDataToMap(ProtectedDataType<?> data) {
        var obj = new HashMap<String, Object>();
        writeToMap(obj, data.getClearValue(), PROTECTED_CLEARTEXT_KEY);
        var hashed = data.getHashedDataType();
        if (hashed != null) {
            writeToMap(obj, hashed.getDigestValue(), PROTECTED_HASHED_DIGEST_VALUE_KEY);
            var method = hashed.getDigestMethod();
            if (method != null) {
                writeToMap(obj, method.getSalt(), PROTECTED_HASHED_DIGEST_SALT_KEY);
                writeToMap(obj, method.getAlgorithm(), PROTECTED_HASHED_DIGEST_ALGORHITM_KEY);
                writeToMap(obj, method.getWorkFactor(), PROTECTED_HASHED_DIGEST_WORK_FACTOR_KEY);

            }
        }
        var encrypted = data.getEncryptedDataType();
        if (encrypted != null) {
            if (encrypted.getKeyInfo() != null) {
                writeToMap(obj, encrypted.getKeyInfo().getKeyName(), PROTECTED_ENCRYPTED_KEYNAME_KEY);
            }
            if (encrypted.getEncryptionMethod() != null) {
                writeToMap(obj, encrypted.getEncryptionMethod().getAlgorithm(), PROTECTED_ENCRYPTED_ALGORHITM_KEY);
            }
            if (encrypted.getCipherData() != null) {
                writeToMap(obj,encrypted.getCipherData().getCipherValue(), PROTECTED_ENCRYPTED_CIPHER_DATA_KEY);
            }
        }
        return obj;

    }


    private static <T> T readFromMap(Map<?,?> map, Class<T> valueType, String... keyPath) {

        for (int i = 0; i < keyPath.length - 1; i++) {
            map = (Map<String, Object>) map.get(keyPath[i]);
            if (map == null) {
                return null;
            }
        }
        var finalKey =  keyPath[keyPath.length - 1];
        var value = map.get(finalKey);
        return readValue(value, valueType);
    }

    private static <T> T readValue(Object value, Class<T> valueType) {
        if (value == null) {
            return null;
        }
        if (valueType.isInstance(value)) {
            return valueType.cast(value);
        }
        if (byte[].class.equals(valueType) && value instanceof String strValue) {
            return valueType.cast(Base64.decodeBase64(strValue));
        }

        throw new UnsupportedOperationException("Unsupported type combo requested " + valueType + " and deserialized " + value.getClass());
    }

    private static void writeToMap(@NotNull Map<String, Object> targetMap, Object clearValue, String... keyPath) {
        if (clearValue == null) {
            return;
        }
        // Consume path up to last
        for (int i = 0; i < keyPath.length - 1; i++) {
            targetMap = (Map<String, Object>) targetMap.computeIfAbsent(keyPath[i], k -> new HashMap<>());
        }
        var finalKey =  keyPath[keyPath.length - 1];
        targetMap.put(finalKey, writeValue(clearValue));
    }

    private static Object writeValue(Object clearValue) {
        if (clearValue instanceof Number || clearValue instanceof String) {
            return clearValue;
        }
        if (clearValue instanceof byte[] data) {
            return Base64.encodeBase64String(data);
        }
        throw new UnsupportedOperationException("Type " + clearValue.getClass() + " is not currently supported.");
    }
}
