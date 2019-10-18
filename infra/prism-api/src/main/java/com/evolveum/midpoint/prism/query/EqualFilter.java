/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismConstants;

import javax.xml.namespace.QName;

/**
 *
 */
public interface EqualFilter<T> extends PropertyValueFilter<T> {

    QName ELEMENT_NAME = new QName(PrismConstants.NS_QUERY, "equal");

    @Override
    EqualFilter<T> clone();

}
