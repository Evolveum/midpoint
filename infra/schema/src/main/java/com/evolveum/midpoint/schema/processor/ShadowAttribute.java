/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Access to both {@link ShadowSimpleAttribute} and {@link ShadowReferenceAttribute}.
 *
 * Currently, it cannot extend {@link Item} because of the clash on many methods, like {@link Item#getValue()}.
 * (To be researched further.)
 */
@Experimental
public interface ShadowAttribute<PV, RV> {

    ShadowAttribute<PV, RV> clone();

    void setIncomplete(boolean incomplete);

    boolean isIncomplete();

    boolean hasNoValues();

    void addValueSkipUniquenessCheck(PV value) throws SchemaException;
}
