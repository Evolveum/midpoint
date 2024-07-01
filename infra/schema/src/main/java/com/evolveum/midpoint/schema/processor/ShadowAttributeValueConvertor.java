/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.normalization.Normalizer;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.util.PrismUtil;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

/**
 * Central point of conversion of attribute values.
 * Perhaps temporary, to clean-up all the mess in this area.
 */
class ShadowAttributeValueConvertor {

    // TODO find the correct place for this method
    static <S, T> PrismPropertyValue<T> convertPropertyValue(
            @NotNull PrismPropertyValue<S> srcValue, @NotNull PrismPropertyDefinition<T> targetDef) {
        return PrismUtil.convertPropertyValue(srcValue, targetDef);
    }

    static <T> PrismPropertyValue<T> createPrismPropertyValueFromRealValue(
            @NotNull Object realValue, @NotNull ShadowSimpleAttributeDefinition<T> targetDef) throws SchemaException {
        return convertAndNormalize(
                realValue, targetDef.getTypeClass(), targetDef.getNormalizer(), targetDef.getStringNormalizerIfApplicable());
    }

    static <T2> @NotNull PrismPropertyValue<T2> convertAndNormalize(
            @NotNull Object oldRealValue,
            @NotNull Class<T2> newJavaType,
            @NotNull Normalizer<T2> normalizer,
            Normalizer<String> polyStringNormalizer) throws SchemaException {

        Preconditions.checkArgument(!(oldRealValue instanceof PrismValue),
                "real value is required here: %s", oldRealValue);

        // Only these scenarios are supported here:
        //  - String to PolyString
        //  - PolyString to PolyString
        //  - PolyString to String
        //  - X to X (where X is any other type); with "no-op" normalizer

        if (PolyString.class.equals(newJavaType)) {

            if (oldRealValue instanceof PolyString polyString) {
                // This is a special case; we want to normalize the polystring norm, but keep other parts intact.
                assert polyStringNormalizer != null;
                //noinspection unchecked
                return (PrismPropertyValue<T2>) new PrismPropertyValueImpl<>(
                        polyString.copyApplyingNormalization(polyStringNormalizer));
            }

            String oldStringValue;
            if (oldRealValue instanceof RawType raw) {
                oldStringValue = raw.getParsedRealValue(String.class);
            } else if (oldRealValue instanceof String string) {
                oldStringValue = string;
            } else {
                throw new UnsupportedOperationException(
                        "Cannot convert from %s to %s".formatted(oldRealValue.getClass(), newJavaType));
            }
            // TODO what if the original attribute is a "rich" polystring with (e.g.) translations?
            //noinspection unchecked
            return (PrismPropertyValue<T2>) new PrismPropertyValueImpl<>(new PolyString(
                    oldStringValue,
                    normalizer.normalizeString(oldStringValue)));
        }

        if (String.class.equals(newJavaType)) {

            if (oldRealValue instanceof PolyString polyString) {
                // "Downgrading" from PolyString to String when going back to raw definition
                //noinspection unchecked
                return (PrismPropertyValue<T2>) new PrismPropertyValueImpl<>(polyString.getOrig());
            } else if (oldRealValue instanceof String string) {
                // We intentionally do not want to normalize strings. This could lead to a loss of information.
                // Moreover, the refined schema parser makes sure that the expected type for such cases is PolyString.
                //noinspection unchecked
                return (PrismPropertyValue<T2>) new PrismPropertyValueImpl<>(string);
            } else if (oldRealValue instanceof RawType raw) {
                //noinspection unchecked
                return (PrismPropertyValue<T2>) new PrismPropertyValueImpl<>(raw.getParsedRealValue(String.class));
            } else {
                // this will probably fail below
            }
        }

        if (normalizer.isIdentity()) {
            if (newJavaType.isInstance(oldRealValue)) {
                //noinspection unchecked
                return (PrismPropertyValue<T2>) new PrismPropertyValueImpl<>(oldRealValue);
            } else if (oldRealValue instanceof RawType raw) {
                return new PrismPropertyValueImpl<>(
                        raw.getParsedRealValue(newJavaType));
            } else {
                return new PrismPropertyValueImpl<>(
                        JavaTypeConverter.convert(newJavaType, oldRealValue));
            }
        }

        throw new UnsupportedOperationException(
                "Cannot convert %s to %s with %s".formatted(
                        MiscUtil.getDiagInfo(oldRealValue), newJavaType, normalizer));
    }
}
