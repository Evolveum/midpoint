/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;

/**
 *
 */
public interface RefFilter extends ValueFilter<PrismReferenceValue, PrismReferenceDefinition> {

    @Override
    RefFilter clone();

    void setOidNullAsAny(boolean oidNullAsAny);

    void setTargetTypeNullAsAny(boolean targetTypeNullAsAny);

    void setRelationNullAsAny(boolean relationNullAsAny);

    boolean isOidNullAsAny();

    boolean isTargetTypeNullAsAny();

    boolean isRelationNullAsAny();
}
