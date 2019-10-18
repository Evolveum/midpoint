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
 * Denotes parent object or container.
 */
public class ParentPathSegment extends ReferencePathSegment {

    public static final String SYMBOL = "..";
    public static final QName QNAME = PrismConstants.T_PARENT;

    @Override
    public boolean equivalent(Object obj) {
        return equals(obj);
    }

    @Override
    public ItemPathSegment clone() {
        return new ParentPathSegment();
    }

    @Override
    public String toString() {
        return SYMBOL;
    }
}
