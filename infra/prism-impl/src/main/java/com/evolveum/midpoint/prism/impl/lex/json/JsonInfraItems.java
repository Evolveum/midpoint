/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import javax.xml.namespace.QName;

/**
 *
 */
public class JsonInfraItems {

    /**
     * Default namespace declaration, deprecated in favour of global prefixes
     * and {@link #PROP_CONTEXT}, which also allows definition of local prefixes.
     *
     * @see #PROP_CONTEXT
     */
    @Deprecated
    public static final String PROP_NAMESPACE = "@ns";
    @Deprecated
    public static final QName PROP_NAMESPACE_QNAME = new QName(PROP_NAMESPACE);
    /**
     * Explicit type information
     *
     */
    public static final String PROP_TYPE = "@type";
    public static final QName PROP_TYPE_QNAME = new QName(PROP_TYPE);
    /**
     * Marks object as incomplete
     */
    public static final String PROP_INCOMPLETE = "@incomplete";
    public static final QName PROP_INCOMPLETE_QNAME = new QName(PROP_INCOMPLETE);
    /**
     * Explicit item name definition for heterolists
     *
     */
    public static final String PROP_ELEMENT = "@element";
    public static final QName PROP_ELEMENT_QNAME = new QName(PROP_ELEMENT);
    /**
     * Alias for value, usually used when other infra item is needed.
     *
     */
    public static final String PROP_VALUE = "@value";
    public static final QName PROP_VALUE_QNAME = new QName(PROP_VALUE);
    /**
     * Metadata item
     */
    public static final String PROP_METADATA = "@metadata";
    public static final QName PROP_METADATA_QNAME = new QName(PROP_METADATA);

    /**
     * Namespace context definition.
     */
    public static final String PROP_CONTEXT = "@context";
    public static final QName PROP_CONTEXT_QNAME = new QName(PROP_CONTEXT);

}
