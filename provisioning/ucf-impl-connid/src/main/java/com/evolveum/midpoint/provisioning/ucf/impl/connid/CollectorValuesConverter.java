/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
