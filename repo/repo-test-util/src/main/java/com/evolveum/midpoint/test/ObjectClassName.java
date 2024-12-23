/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

/**
 * The internal/ConnId/midPoint form of object class or reference type name.
 *
 * TODO consider renaming this class
 */
public class ObjectClassName {

    private final String localName;
    private final String connIdName;
    private final QName xsdName;

    private ObjectClassName(String localName, String connIdName, QName xsdName) {
        this.localName = localName;
        this.connIdName = connIdName;
        this.xsdName = xsdName;
    }

    public static ObjectClassName legacyAccount(String localName) {
        return new ObjectClassName(localName, "__ACCOUNT__", RI_ACCOUNT_OBJECT_CLASS);
    }

    public static ObjectClassName legacyGroup(String localName) {
        return new ObjectClassName(localName, "__GROUP__", RI_GROUP_OBJECT_CLASS);
    }

    public static ObjectClassName legacyCustom(String localName) {
        return new ObjectClassName(localName, localName, new QName(NS_RI, "Custom" + localName + "ObjectClass"));
    }

    /** "Modern" style of writing class names: the native and midPoint names are the same. */
    public static ObjectClassName custom(String localName) {
        return new ObjectClassName(localName, localName, new QName(NS_RI, localName));
    }

    public String local() {
        return localName;
    }

    public String connId() {
        return connIdName;
    }

    public QName xsd() {
        return xsdName;
    }

    @Override
    public String toString() {
        return localName;
    }
}
