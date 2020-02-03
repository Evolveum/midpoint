/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static com.evolveum.midpoint.util.DOMUtil.NS_W3C_XML_SCHEMA_PREFIX;

import javax.xml.namespace.QName;

/**
 * @author semancik
 */
public enum PrimitiveType {

    STRING("string"),
    DECIMAL("decimal"),
    INTEGER("integer"),
    INT("int"),
    LONG("long"),
    SHORT("short"),
    FLOAT("float"),
    DOUBLE("double"),
    BOOLEAN("boolean"),
    BASE64BINARY("base64Binary"),
    DATETIME("dateTime"),
    DURATION("duration"),
    BYTE("byte"),
    QNAME("qname"),
    ANYURI("anyURI");

    private final String localName;
    private final QName qname;

    PrimitiveType(String localName) {
        this.localName = localName;
        this.qname = new QName(W3C_XML_SCHEMA_NS_URI, localName, NS_W3C_XML_SCHEMA_PREFIX);
    }

    public QName getQname() {
        return qname;
    }

    public static final QName XSD_STRING = STRING.getQname();
    public static final QName XSD_DECIMAL = DECIMAL.getQname();
    public static final QName XSD_INTEGER = INTEGER.getQname();
    public static final QName XSD_INT = INT.getQname();
    public static final QName XSD_LONG = LONG.getQname();
    public static final QName XSD_SHORT = SHORT.getQname();
    public static final QName XSD_FLOAT = FLOAT.getQname();
    public static final QName XSD_DOUBLE = DOUBLE.getQname();
    public static final QName XSD_BOOLEAN = BOOLEAN.getQname();
    public static final QName XSD_BASE64BINARY = BASE64BINARY.getQname();
    public static final QName XSD_DATETIME = DATETIME.getQname();
    public static final QName XSD_DURATION = DURATION.getQname();
    public static final QName XSD_BYTE = BYTE.getQname();
    public static final QName XSD_QNAME = QNAME.getQname();
    public static final QName XSD_ANYURI = ANYURI.getQname();

}
