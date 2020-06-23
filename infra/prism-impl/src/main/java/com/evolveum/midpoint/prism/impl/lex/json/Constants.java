/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import javax.xml.namespace.QName;

/**
 * TODO
 */
public class Constants {
    static final String PROP_NAMESPACE = "@ns";
    public static final QName PROP_NAMESPACE_QNAME = new QName(PROP_NAMESPACE);
    static final String PROP_TYPE = "@type";
    public static final QName PROP_TYPE_QNAME = new QName(PROP_TYPE);
    public static final String PROP_INCOMPLETE = "@incomplete";
    public static final QName PROP_INCOMPLETE_QNAME = new QName(PROP_INCOMPLETE);
    static final String PROP_ELEMENT = "@element";
    public static final QName PROP_ELEMENT_QNAME = new QName(PROP_ELEMENT);
    public static final String PROP_VALUE = "@value";
    public static final QName PROP_VALUE_QNAME = new QName(PROP_VALUE);
    public static final String PROP_METADATA = "@metadata";
    public static final QName PROP_METADATA_QNAME = new QName(PROP_METADATA);
}
