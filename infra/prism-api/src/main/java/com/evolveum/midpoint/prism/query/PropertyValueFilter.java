/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;

/**
 *
 */
public interface PropertyValueFilter<T> extends ValueFilter<PrismPropertyValue<T>, PrismPropertyDefinition<T>> {

    // TODO cleanup this mess - how values are cloned, that expression is not cloned in LT/GT filter etc
    PropertyValueFilter clone();
}
