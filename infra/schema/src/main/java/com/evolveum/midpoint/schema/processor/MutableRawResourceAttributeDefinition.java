/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;

/**
 * Mutable interface to (some of) {@link ShadowSimpleAttributeDefinition} implementations.
 *
 * TODO remove this
 */
public interface MutableRawResourceAttributeDefinition<T>

        extends ShadowAttributeUcfDefinition.Mutable,
        ResourceItemPrismDefinition.Mutable,
        PrismPropertyDefinition.PrismPropertyDefinitionMutator<T> {

}
