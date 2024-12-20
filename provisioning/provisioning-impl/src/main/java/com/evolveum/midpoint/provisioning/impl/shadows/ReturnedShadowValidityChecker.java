/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.impl.Shadow;
import com.evolveum.midpoint.schema.*;

import com.evolveum.midpoint.schema.processor.ShadowReferenceAttribute;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.schema.util.AbstractShadow;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Checks the validity of shadows returned by non-raw `get` and `search` operations.
 */
public class ReturnedShadowValidityChecker {

    /** When asking about the future shadow state, pending operations may induce unresolved references. TODO decide on this. */
    private final boolean allowUnresolvedReferenceAttributeValues;

    private ReturnedShadowValidityChecker(@Nullable Collection<SelectorOptions<GetOperationOptions>> options) {
        var rootOptions = SelectorOptions.findRootOptions(options);
        assert !GetOperationOptions.isRaw(rootOptions);
        this.allowUnresolvedReferenceAttributeValues =
                GetOperationOptions.getPointInTimeType(rootOptions) == PointInTimeType.FUTURE;
    }

    public static void check(Shadow shadow, @Nullable Collection<SelectorOptions<GetOperationOptions>> options) {
        new ReturnedShadowValidityChecker(options)
                .check(shadow);
    }

    static @NotNull ResultHandler<ShadowType> createCheckingHandler(
            @NotNull ResultHandler<ShadowType> handler, Collection<SelectorOptions<GetOperationOptions>> options) {
        if (GetOperationOptions.isRaw(options)) {
            return handler;
        } else {
            var checker = new ReturnedShadowValidityChecker(options);
            return (object, parentResult) -> {
                checker.check(AbstractShadow.of(object));
                return handler.handle(object, parentResult);
            };
        }
    }

    public static void check(
            @NotNull SearchResultList<PrismObject<ShadowType>> shadows, Collection<SelectorOptions<GetOperationOptions>> options) {
        if (!GetOperationOptions.isRaw(options)) {
            var checker = new ReturnedShadowValidityChecker(options);
            shadows.forEach(shadow -> checker.check(AbstractShadow.of(shadow)));
        }
    }

    public void check(@NotNull AbstractShadow shadow) {

        if (shadow.isIdentificationOnly()) {
            return; // Nothing to check here.
        }

        if (shadow.isError()) {
            return; // May not be fully correct.
        }

        // Here are the checks
        stateNonNull(shadow.getBean().getEffectiveOperationPolicy(), "No effective operation policy in %s", shadow);

        // And this is the recursion
        for (var refAttr : shadow.getReferenceAttributes()) {
            checkReferenceAttributeValues(refAttr);
        }
        for (var assoc : shadow.getAssociations()) {
            for (var assocVal : assoc.getAssociationValues()) {
                try {
                    for (var refAttr : assocVal.getObjectReferences()) {
                        checkReferenceAttributeValues(refAttr);
                    }
                    if (assoc.getDefinition().isComplex()) {
                        try {
                            check(assocVal.getAssociationDataObject());
                        } catch (IllegalStateException e) {
                            throw in("association object", e);
                        }
                    }
                } catch (IllegalStateException e) {
                    throw in(assoc.getElementName() + " value " + assocVal, e);
                }
            }
        }
    }

    private void checkReferenceAttributeValues(ShadowReferenceAttribute refAttr) {
        for (var refAttrVal : refAttr.getAttributeValues()) {
            try {
                var embeddedShadow = refAttrVal.getShadowIfPresent();
                if (isPresent(embeddedShadow, refAttrVal)) {
                    check(Objects.requireNonNull(embeddedShadow));
                }
            } catch (IllegalStateException e) {
                throw in(refAttr.getElementName() + " value " + refAttrVal, e);
            }
        }
    }

    @Contract("!null, _ -> true")
    private boolean isPresent(AbstractShadow embeddedShadow, ShadowReferenceAttributeValue refAttrVal) {
        if (embeddedShadow != null) {
            return true;
        } else if (allowUnresolvedReferenceAttributeValues) {
            return false; // OK
        } else {
            throw new IllegalStateException("Unresolved reference attribute value: " + refAttrVal);
        }
    }

    private static @NotNull IllegalStateException in(String where, IllegalStateException innerException) {
        return new IllegalStateException(innerException.getMessage() + " in " + where, innerException);
    }
}
