/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 * <p>
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.prism.xml.ns._public.types_3;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.JaxbVisitor;

/**
 * This class was originally generated. But it was heavily modified by hand.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProtectedStringType")
public class ProtectedStringType extends ProtectedDataType<String> implements Cloneable {

    public static final QName COMPLEX_TYPE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "ProtectedStringType");

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    public ProtectedStringType() {
        content = new ContentList();
    }

    @Override
    public byte[] getClearBytes() {
        String clearValue = getClearValue();
        if (clearValue == null) {
            return null;
        }
        // We want fixed charset here, independent of locale. We want consistent and portable encryption/decryption.
        return clearValue.getBytes(CHARSET);
    }

    @Override
    public void setClearBytes(byte[] bytes) {
        if (bytes != null) {
            setClearValue(bytesToString(bytes));
        }
    }

    @Override
    public boolean canSupportType(Class<?> type) {
        return String.class.isAssignableFrom(type);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public ProtectedStringType clone() {
        ProtectedStringType cloned = new ProtectedStringType();
        cloneTo(cloned);
        return cloned;
    }

    public static String bytesToString(byte[] clearBytes) {
        // We want fixed charset here, independent of locale. We want consistent and portable encryption/decryption.
        return new String(clearBytes, CHARSET);
    }

    @Override
    public void accept(JaxbVisitor visitor) {
        visitor.visit(this);
    }
}
