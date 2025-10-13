/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
@FunctionalInterface
public interface CollectorValuesConverter<V extends PrismValue> {

    List<Object> covertAttributeValuesToConnId(Collection<V> pvals, QName midPointAttributeName) throws SchemaException;

}
