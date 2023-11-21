/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.impl.PrismPropertyImpl;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.normalization.Normalizer;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.Checks;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.ClassUtils;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResourceAttributeImpl<T> extends PrismPropertyImpl<T> implements ResourceAttribute<T> {

    @Serial private static final long serialVersionUID = -6149194956029296486L;

    ResourceAttributeImpl(QName name, ResourceAttributeDefinition<T> definition) {
        super(name, definition, PrismContext.get());
    }

    @Override
    public ResourceAttributeDefinition<T> getDefinition() {
        return (ResourceAttributeDefinition<T>) super.getDefinition();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ResourceAttribute<T> clone() {
        return cloneComplex(CloneStrategy.LITERAL);
    }

    @Override
    public ResourceAttribute<T> cloneComplex(CloneStrategy strategy) {
        ResourceAttributeImpl<T> clone = new ResourceAttributeImpl<>(getElementName(), getDefinition());
        copyValues(strategy, clone);
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, ResourceAttributeImpl<T> clone) {
        super.copyValues(strategy, clone);
        // Nothing to copy
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "RA";
    }

    @Override
    public void applyDefinition(PrismPropertyDefinition<T> definition, boolean force) throws SchemaException {
        if (definition != null) {
            Checks.checkSchema(
                    definition instanceof ResourceAttributeDefinition, "Definition should be %s not %s" ,
                    ResourceAttributeDefinition.class.getSimpleName(), definition.getClass().getName());
        }
        super.applyDefinition(definition, force);
    }

    @Override
    public <T2> @NotNull ResourceAttribute<T2> forceDefinitionWithNormalization(
            @NotNull ResourceAttributeDefinition<T2> newDefinition)
            throws SchemaException {

        Normalizer<?> normalizer = newDefinition.getNormalizer();
        ResourceAttributeDefinition<T> oldDefinition = getDefinition();
        Class<?> oldJavaType = oldDefinition != null ? oldDefinition.getTypeClass() : null;
        Class<?> newJavaType = ClassUtils.resolvePrimitiveIfNecessary(newDefinition.getTypeClass());

        List<PrismPropertyValue<T2>> newValues = new ArrayList<>();
        for (PrismPropertyValue<T> oldValue : values) {
            newValues.add(
                    convertAndNormalize(oldValue, newJavaType, normalizer));
        }

        ResourceAttribute<T2> newAttribute;
        if (newJavaType.equals(oldJavaType) && isMutable()) {
            clear();
            //noinspection unchecked
            newAttribute = (ResourceAttribute<T2>) this;
            newAttribute.applyDefinition(newDefinition, true);
            newAttribute.addAll(newValues);
        } else {
            newAttribute = newDefinition.instantiate();
            for (PrismPropertyValue<T2> newValue : newValues) {
                if (newValue.getParent() == null) {
                    newAttribute.add(newValue);
                } else {
                    newAttribute.add(newValue.clone());
                }
            }
        }

        return newAttribute;
    }

    private <T2> PrismPropertyValue<T2> convertAndNormalize(
            @NotNull PrismPropertyValue<T> oldValue,
            @NotNull Class<?> newJavaType,
            @NotNull Normalizer<?> normalizer) throws SchemaException {
        var oldRealValue = Objects.requireNonNull(oldValue.getRealValue());
        Class<?> oldJavaType = oldRealValue.getClass();

        // Only 3 scenarios are supported here:
        //  - String to PolyString
        //  - PolyString to PolyString
        //  - X to X (where X is any other type); with "no-op" normalizer

        // TODO which of these two (PolyString/PolyStringType)?
        if (PolyString.class.equals(newJavaType) || PolyStringType.class.equals(newJavaType)) {
            String oldStringValue;
            if (oldRealValue instanceof String string) {
                oldStringValue = string;
            } else if (oldRealValue instanceof PolyString polyString) {
                oldStringValue = polyString.getOrig();
            } else if (oldRealValue instanceof PolyStringType polyString) {
                oldStringValue = polyString.getOrig();
            } else {
                throw new UnsupportedOperationException(
                        "Cannot convert from %s to %s".formatted(oldRealValue.getClass(), newJavaType));
            }
            // TODO what if the original attribute is a "rich" polystring with (e.g.) translations?
            //noinspection unchecked
            return (PrismPropertyValue<T2>) new PrismPropertyValueImpl<>(new PolyString(
                    oldStringValue,
                    normalizer.normalizeString(oldStringValue)));
        } else if (newJavaType.isInstance(oldRealValue)) {
            if (normalizer.isIdentity()) {
                //noinspection unchecked
                return (PrismPropertyValue<T2>) oldValue;
            } else if (oldRealValue instanceof String stringOldValue) {
                //noinspection unchecked
                return (PrismPropertyValue<T2>) new PrismPropertyValueImpl<>(
                        normalizer.normalizeString(stringOldValue));
            }
        }

        throw new UnsupportedOperationException(
                "Cannot convert from %s to %s with %s: %s".formatted(
                        oldJavaType, newJavaType, normalizer, MiscUtil.getDiagInfo(oldRealValue)));
    }
}
