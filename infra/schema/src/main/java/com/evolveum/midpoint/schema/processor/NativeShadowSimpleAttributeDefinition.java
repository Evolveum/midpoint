/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismItemMatchingDefinition;
import com.evolveum.midpoint.prism.PrismItemValuesDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition.PrismPropertyLikeDefinitionBuilder;

public interface NativeShadowSimpleAttributeDefinition<T>
        extends NativeShadowAttributeDefinition,
        PrismItemValuesDefinition<T>,
        PrismItemMatchingDefinition<T> {

    interface NativeShadowAttributeDefinitionBuilder<T>
            extends PrismPropertyLikeDefinitionBuilder<T>, NativeShadowAttributeDefinition.NativeShadowAttributeDefinitionBuilder {

    }
}
