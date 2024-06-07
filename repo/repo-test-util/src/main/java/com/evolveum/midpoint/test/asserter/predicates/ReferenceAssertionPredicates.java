/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter.predicates;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Methods for construction of various assertion predicates related to {@link ObjectReferenceType}s.
 */
@Experimental
public class ReferenceAssertionPredicates {

    public static AssertionPredicate<ObjectReferenceType> references(@NotNull String oid, @NotNull QName typeName) {
        return new GenericAssertionPredicate<>(
                value -> value != null && oid.equals(value.getOid()) && QNameUtil.match(typeName, value.getType()),
                value -> "value of " + value + " does not point to " + typeName + " with OID " + oid);
    }

    public static AssertionPredicate<ObjectReferenceType> references(@NotNull String oid, @NotNull QName typeName, QName relation) {
        return new GenericAssertionPredicate<>(
                value ->
                        value != null
                                && oid.equals(value.getOid())
                                && QNameUtil.match(typeName, value.getType())
                                && QNameUtil.match(relation, value.getRelation()),
                value -> "value of " + value + " does not point to " + typeName + " with OID " + oid + " and relation " + relation);
    }
}
