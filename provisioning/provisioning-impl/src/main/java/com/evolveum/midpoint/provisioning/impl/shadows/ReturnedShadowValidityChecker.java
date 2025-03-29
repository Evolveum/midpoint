/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.impl.Shadow;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
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

    /**
     * 1. When asking about the future shadow state, pending operations may induce unresolved references. TODO decide on this.
     * 2. But also when ignoring associations (retrieve = EXCLUDE), we may leave unresolved references.
     */
    private final boolean allowUnresolvedReferenceAttributeValues;

    /** We ignore all checks on reference attribute and association values. */
    private final boolean ignoreReferenceAttributeValues;

    private ReturnedShadowValidityChecker(@Nullable Collection<SelectorOptions<GetOperationOptions>> options) {
        var rootOptions = SelectorOptions.findRootOptions(options);
        assert !GetOperationOptions.isRaw(rootOptions);
        this.ignoreReferenceAttributeValues = !ProvisioningUtil.isFetchAssociations(options);
        this.allowUnresolvedReferenceAttributeValues =
                ignoreReferenceAttributeValues
                        || GetOperationOptions.getPointInTimeType(rootOptions) == PointInTimeType.FUTURE;
    }

    public static void check(Shadow shadow, @Nullable Collection<SelectorOptions<GetOperationOptions>> options) {
        new ReturnedShadowValidityChecker(options)
                .check(shadow, true);
    }

    static @NotNull ResultHandler<ShadowType> createCheckingHandler(
            @NotNull ResultHandler<ShadowType> handler, Collection<SelectorOptions<GetOperationOptions>> options) {
        if (GetOperationOptions.isRaw(options)) {
            return handler;
        } else {
            var checker = new ReturnedShadowValidityChecker(options);
            return (object, parentResult) -> {
                checker.check(AbstractShadow.of(object), true);
                return handler.handle(object, parentResult);
            };
        }
    }

    public static void check(
            @NotNull SearchResultList<PrismObject<ShadowType>> shadows, Collection<SelectorOptions<GetOperationOptions>> options) {
        if (!GetOperationOptions.isRaw(options)) {
            var checker = new ReturnedShadowValidityChecker(options);
            shadows.forEach(shadow -> checker.check(AbstractShadow.of(shadow), true));
        }
    }

    /**
     * isTopLevel: Either the shadow returned, or its association values. The idea is that referenced shadows need not fulfill
     * all the checks.
     */
    private void check(@NotNull AbstractShadow shadow, boolean isTopLevel) {

        if (shadow.isIdentificationOnly()) {
            return; // Nothing to check here.
        }

        if (shadow.isError()) {
            return; // May not be fully correct.
        }

        // Here are the checks
        stateNonNull(shadow.getBean().getEffectiveOperationPolicy(), "No effective operation policy in %s", shadow);

        if (ignoreReferenceAttributeValues) {
            return;
        }

        // And this is the recursion
        for (var refAttr : shadow.getReferenceAttributes()) {
            checkReferenceAttributeValues(refAttr, isTopLevel);
        }
        for (var assoc : shadow.getAssociations()) {
            for (var assocVal : assoc.getAssociationValues()) {
                try {
                    if (assoc.getDefinition().isComplex()) {
                        try {
                            check(assocVal.getAssociationDataObject(), isTopLevel);
                        } catch (IllegalStateException e) {
                            throw in("association object", e);
                        }
                    } else {
                        // simple association
                        for (var refAttr : assocVal.getObjectReferences()) {
                            checkReferenceAttributeValues(refAttr, false);
                        }
                    }
                } catch (IllegalStateException e) {
                    throw in(assoc.getElementName() + " value " + assocVal, e);
                }
            }
        }
    }

    private void checkReferenceAttributeValues(ShadowReferenceAttribute refAttr, boolean isTopLevel) {
        for (var refAttrVal : refAttr.getAttributeValues()) {
            try {
                var embeddedShadow = refAttrVal.getShadowIfPresent();
                if (isPresent(embeddedShadow, refAttrVal, isTopLevel)) {
                    check(Objects.requireNonNull(embeddedShadow), false);
                }
            } catch (IllegalStateException e) {
                throw in(refAttr.getElementName() + " value " + refAttrVal, e);
            }
        }
    }

    @Contract("!null, _, _ -> true")
    private boolean isPresent(AbstractShadow embeddedShadow, ShadowReferenceAttributeValue refAttrVal, boolean isTopLevel) {
        if (embeddedShadow != null) {
            return true;
        } else if (!isTopLevel) {
            return false; // references in referenced shadows need not be resolved
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
