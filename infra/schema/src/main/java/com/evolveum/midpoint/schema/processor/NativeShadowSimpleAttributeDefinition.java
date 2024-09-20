/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
