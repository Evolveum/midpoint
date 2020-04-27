/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.match;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Interface for generic matching rules. The responsibility of a matching rule is to decide if
 * two objects of the same type match. This may seem a simple thing to do but the details may get
 * quite complex. E.g. comparing string in case sensitive or insensitive manner, comparing PolyStrings, etc.
 *
 * @author Radovan Semancik
 *
 */
public interface MatchingRule<T> {

    /**
     * QName that identifies the rule. This QName may be used to refer to this specific matching rule,
     * it is an matching rule identifier.
     */
    QName getName();

    /**
     * Returns true if the rule can be applied to the specified XSD type.
     */
    boolean supports(QName xsdType);

    /**
     * Matches two objects.
     */
    boolean match(T a, T b) throws SchemaException;

    /**
     * Matches value against given regex.
     */
    boolean matchRegex(T a, String regex) throws SchemaException;

    /**
     * Returns a normalized version of the value.
     * For normalized version the following holds:
     * if A matches B then normalize(A) == normalize(B)
     */
    T normalize(T original) throws SchemaException;
}
