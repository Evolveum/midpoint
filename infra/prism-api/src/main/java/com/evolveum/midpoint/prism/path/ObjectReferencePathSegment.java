/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.prism.PrismConstants;

import javax.xml.namespace.QName;

/**
 * Denotes referenced object, like "assignment/targetRef/@/name" (name of assignment's target object)
 *
 * @author mederly
 */
public class ObjectReferencePathSegment extends ReferencePathSegment {

    public static final String SYMBOL = "@";
    public static final QName QNAME = PrismConstants.T_OBJECT_REFERENCE;

    @Override
    public boolean equivalent(Object obj) {
        return equals(obj);
    }

    @Override
    public ItemPathSegment clone() {
        return new ObjectReferencePathSegment();
    }

    @Override
    public String toString() {
        return SYMBOL;
    }
}
