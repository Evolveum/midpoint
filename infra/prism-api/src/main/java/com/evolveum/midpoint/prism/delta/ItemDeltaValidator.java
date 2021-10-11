/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.PrismValue;

/**
 * @author semancik
 *
 */
@FunctionalInterface
public interface ItemDeltaValidator<V extends PrismValue> {

    void validate(PlusMinusZero plusMinusZero, V itemValue);

}
