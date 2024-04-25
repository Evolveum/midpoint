/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.path;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.DefinitionResolver;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

/**
 * Simplified resolution context holding a single prism value.
 */
class ValueResolutionContext extends ResolutionContext {

    @NotNull private final PrismValue value;
    private final String contextDescription;

    ValueResolutionContext(@NotNull PrismValue value, String contextDescription) {
        this.value = value;
        this.contextDescription = contextDescription;
    }

    <V extends PrismValue> PrismValueDeltaSetTriple<V> createOutputTriple() {
        PrismValueDeltaSetTriple<V> outputTriple = PrismContext.get().deltaFactory().createPrismValueDeltaSetTriple();
        //noinspection unchecked
        outputTriple.addToZeroSet((V) CloneUtil.clone(value));
        return outputTriple;
    }

    @Override
    boolean isContainer() {
        return value instanceof PrismContainerValue;
    }

    @Override
    ResolutionContext stepInto(ItemName step, DefinitionResolver<?, ?> defResolver) throws SchemaException {
        assert isContainer();
        Item<?, ?> item = ((PrismContainerValue<?>) value).findItem(step);
        if (item != null) {
            if (item.size() > 1) {
                throw new SchemaException("Cannot resolve " + step + " in " + item +
                        " because there is more than one value. In " + contextDescription);
            } else if (item.hasNoValues()) {
                return null;
            } else {
                return new ValueResolutionContext(item.getAnyValue(), contextDescription);
            }
        } else {
            return null;
        }
    }

    @Override
    boolean isStructuredProperty() {
        return value instanceof PrismPropertyValue && value.getRealValue() instanceof Structured;
    }

    @Override
    ResolutionContext resolveStructuredProperty(ItemPath pathToResolve, PrismPropertyDefinition<?> outputDefinition) {
        assert isStructuredProperty();
        //noinspection ConstantConditions
        Object resolvedRealValue = ((Structured) value.getRealValue()).resolve(pathToResolve);
        if (resolvedRealValue != null) {
            PrismPropertyValue<Object> resolvedPrismValue = PrismContext.get().itemFactory().createPropertyValue(resolvedRealValue);
            return new ValueResolutionContext(resolvedPrismValue, contextDescription);
        } else {
            return null;
        }
    }

    @Override
    boolean isNull() {
        return false;
    }

    static ResolutionContext fromRealValue(Object variableValue, String contextDescription) {
        PrismValue prismValue;
        if (variableValue instanceof Referencable){
            prismValue = ((Referencable) variableValue).asReferenceValue();
        } else if (variableValue instanceof Containerable){
            prismValue = ((Containerable) variableValue).asPrismContainerValue();
        } else {
            prismValue = PrismContext.get().itemFactory().createPropertyValue(variableValue);
        }
        return new ValueResolutionContext(prismValue, contextDescription);
    }
}
