/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.util.*;

import com.evolveum.midpoint.model.common.expression.script.mel.MelComparable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.CelType;
import dev.cel.common.types.NullableType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructType;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 */
public class QNameCelValue extends AbstractStructuredCelValue<String> implements MidPointValueProducer<QName>, MelComparable {

    public static final String QNAME_PACKAGE_NAME = QName.class.getTypeName();
    private static final String F_LOCAL_PART = "localPart";
    private static final String F_NAMESPACE_URI = "namespaceURI";
    public static final CelType CEL_TYPE = createCelType();

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

    protected Map<String, String> createMapValue() {
        return Map.of(
                F_LOCAL_PART, qname.getLocalPart(),
                F_NAMESPACE_URI, qname.getNamespaceURI()
        );
    }

    @Override
    public QName getJavaValue() {
        return qname;
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

    private static CelType createCelType() {
        ImmutableSet<String> fieldNames = ImmutableSet.of(F_LOCAL_PART, F_NAMESPACE_URI);
        StructType.FieldResolver fieldResolver = fieldName -> {
            if (F_LOCAL_PART.equals(fieldName) || F_NAMESPACE_URI.equals(fieldName)) {
                return Optional.of(NullableType.create(SimpleType.STRING));
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
        if (other instanceof ItemPathCelValue p) {
            return p.getJavaValue().equivalent(ItemPath.create(qname));
        }
        return false;
    }
}

