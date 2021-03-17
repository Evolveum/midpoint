/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.query.lang;

import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

class FilterNames {

    public static final String QUERY_NS = "http://prism.evolveum.com/xml/ns/public/query-3";
    public static final String MATCHING_RULE_NS = "http://prism.evolveum.com/xml/ns/public/matching-rule-3";

    public static final QName AND = queryName("and");
    public static final QName OR = queryName("or");
    public static final QName EQUAL = queryName("equal");
    public static final QName LESS = queryName("less");
    public static final QName GREATER = queryName("greater");
    public static final QName LESS_OR_EQUAL = queryName("lessOrEqual");
    public static final QName GREATER_OR_EQUAL = queryName("greaterOrEqual");
    public static final QName CONTAINS = queryName("contains");
    public static final QName STARTS_WITH = queryName("startsWith");
    public static final QName ENDS_WITH = queryName("endsWith");
    public static final QName MATCHES = queryName("matches");
    public static final QName EXISTS = queryName("exists");
    public static final QName FULL_TEXT = queryName("fullText");
    public static final QName IN_OID = queryName("inOid");
    public static final QName OWNED_BY_OID = queryName("ownedByOid");
    public static final QName IN_ORG = queryName("inOrg");
    public static final QName IS_ROOT = queryName("isRoot");
    public static final QName NOT = queryName("not");
    public static final QName NOT_EQUAL = queryName("notEqual");
    public static final QName TYPE = queryName("type");

    static final BiMap<String, QName> ALIAS_TO_NAME = ImmutableBiMap.<String, QName>builder()
            .put("=", EQUAL)
            .put("<", LESS)
            .put(">", GREATER)
            .put("<=", LESS_OR_EQUAL)
            .put(">=", GREATER_OR_EQUAL)
            .put("!=", NOT_EQUAL)
            .build();

    static final Map<QName, String> NAME_TO_ALIAS = ALIAS_TO_NAME.inverse();

    private FilterNames() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static QName queryName(String localName) {
        return new QName(QUERY_NS, localName);
    }

    static Optional<QName> fromAlias(String alias) {
        return Optional.ofNullable(ALIAS_TO_NAME.get(alias));
    }

    static Optional<String> aliasFor(QName name) {
        return Optional.ofNullable(NAME_TO_ALIAS.get(name));
    }

}
