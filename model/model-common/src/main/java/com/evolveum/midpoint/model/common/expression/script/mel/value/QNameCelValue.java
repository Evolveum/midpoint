/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.util.*;

import com.evolveum.midpoint.model.common.expression.script.mel.MelComparable;

import com.evolveum.midpoint.util.QNameUtil;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructType;
import dev.cel.common.values.CelValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 */
public class QNameCelValue extends CelValue implements Map<String,String>, MidPointValueProducer<QName>, MelComparable {

    public static final String QNAME_PACKAGE_NAME = QName.class.getTypeName();
    private static final String F_LOCAL_PART = "localPart";
    private static final String F_NAMESPACE_URI = "namespaceURI";
    public static final CelType CEL_TYPE = createQNameType();

    private static final String FUNCTION_STRING_EQUALS_QNAME_ID = "string-equals-qname";
    private static final String FUNCTION_QNAME_EQUALS_STRING_ID = "qname-equals-string";

    private final QName qname;

    QNameCelValue(QName qname) {
        this.qname = qname;
    }

    public static QNameCelValue create(QName qname) {
        return new QNameCelValue(qname);
    }

    public static QNameCelValue create(String namespace, String localPart) {
        return new QNameCelValue(new QName(namespace, localPart));
    }

    public static QNameCelValue create(String localPart) {
        return new QNameCelValue(new QName(localPart));
    }

    public Map<String, String> value() {
        return Map.of(F_LOCAL_PART, qname.getLocalPart(),
                F_NAMESPACE_URI, qname.getNamespaceURI());
    }

    @Override
    public QName getJavaValue() {
        return qname;
    }

    @Override
    public boolean isZeroValue() {
        return isEmpty();
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    public QName getQName() {
        return qname;
    }

    public String getLocalPart() {
        return qname.getLocalPart();
    }

    public String getNamespaceURI() {
        return qname.getNamespaceURI();
    }

    @Override
    public int size() {
        return value().size();
    }

    @Override
    public boolean isEmpty() {
        return value().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return value().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return value().containsValue(value);
    }

    @Override
    public String get(Object key) {
        return value().get(key);
    }

    @Override
    public @Nullable String put(String key, String value) {
        return value().put(key,value);
    }

    @Override
    public String remove(Object key) {
        return value().remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends String> m) {
        value().putAll(m);
    }

    @Override
    public void clear() {
        value().clear();
    }

    @Override
    public @NotNull Set<String> keySet() {
        return value().keySet();
    }

    @Override
    public @NotNull Collection<String> values() {
        return value().values();
    }

    @Override
    public @NotNull Set<Entry<String, String>> entrySet() {
        return value().entrySet();
    }

    private static CelType createQNameType() {
        ImmutableSet<String> fieldNames = ImmutableSet.of(F_LOCAL_PART, F_NAMESPACE_URI);
        StructType.FieldResolver fieldResolver = fieldName -> {
            if (F_LOCAL_PART.equals(fieldName) || F_NAMESPACE_URI.equals(fieldName)) {
                return Optional.of(SimpleType.STRING);
            } else {
                throw new IllegalStateException("Illegal request for qname field " + fieldName);
            }
        };
        return StructType.create(QNAME_PACKAGE_NAME, fieldNames, fieldResolver);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        QNameCelValue that = (QNameCelValue) o;
        return Objects.equals(qname, that.qname);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(qname);
    }

    @Override
    public boolean melEquals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof String s) {
            return s.equals(getLocalPart());
        }
        if (other instanceof QNameCelValue q) {
            return QNameUtil.match(getQName(), q.getQName());
        }
        return false;
    }
}

