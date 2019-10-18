/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;

import javax.xml.namespace.QName;

/**
 *
 */
public interface MutablePrismReferenceDefinition extends PrismReferenceDefinition, MutableItemDefinition<PrismReference> {

    void setTargetTypeName(QName typeName);

    void setComposite(boolean value);
}
