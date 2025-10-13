/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
