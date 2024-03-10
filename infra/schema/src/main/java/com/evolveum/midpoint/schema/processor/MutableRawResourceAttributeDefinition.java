/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;

/**
 * Mutable interface to (some of) {@link ResourceAttributeDefinition} implementations.
 *
 * TODO consider removing
 */
public interface MutableRawResourceAttributeDefinition<T>

        extends ResourceItemUcfDefinition.Mutable,
        ResourceItemPrismDefinition.Mutable,
        MutablePrismPropertyDefinition<T> {

}
