/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

class AttributeEqualsCheckerFactory {

    /** Creates a checker for simple/reference attribute. */
    static <V extends PrismValue> @NotNull EqualsChecker<V> checkerFor(ShadowAttributeDefinition<V, ?, ?, ?> attrDef)
            throws SchemaException {
        if (attrDef instanceof ShadowSimpleAttributeDefinition<?> simpleDef) {
            //noinspection unchecked
            return (EqualsChecker<V>) PropertyValueMatcher.createMatcher(simpleDef, SchemaService.get().matchingRuleRegistry());
        } else if (attrDef instanceof ShadowReferenceAttributeDefinition) {
            //noinspection unchecked
            return (EqualsChecker<V>) ShadowReferenceAttributeValue.semanticEqualsChecker();
        } else {
            throw new UnsupportedOperationException("Unsupported attribute definition: " + attrDef);
        }
    }
}
