/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.cases;

import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

public class CorrelationCaseUtil {

    public static @Nullable ResourceObjectOwnerOptionsType getOwnerOptions(@NotNull CaseType aCase) {
        ShadowType shadow = (ShadowType) ObjectTypeUtil.getObjectFromReference(aCase.getTargetRef());
        if (shadow != null && shadow.getCorrelation() != null) {
            return shadow.getCorrelation().getOwnerOptions();
        } else {
            return null;
        }
    }

    public static @NotNull List<ResourceObjectOwnerOptionType> getOwnerOptionsList(@NotNull CaseType aCase) {
        ResourceObjectOwnerOptionsType info = getOwnerOptions(aCase);
        return info != null ? info.getOption() : List.of();
    }

    public static @NotNull String getShadowOidRequired(@NotNull CaseType aCase) throws SchemaException {
        return MiscUtil.requireNonNull(
                MiscUtil.requireNonNull(
                                aCase.getTargetRef(), () -> "No targetRef in " + aCase)
                        .getOid(), () -> "No shadow OID in " + aCase);

    }

    public static AbstractWorkItemOutputType createDefaultOutput(@NotNull OwnerOptionIdentifier identifier) {
        return new AbstractWorkItemOutputType()
                .outcome(identifier.getStringValue());
    }

    public static AbstractWorkItemOutputType createDefaultOutput(String identifier) {
        return new AbstractWorkItemOutputType()
                .outcome(identifier != null ?
                        OwnerOptionIdentifier.forExistingOwner(identifier).getStringValue() :
                        OwnerOptionIdentifier.forNoOwner().getStringValue());
    }

    // Throws an exception if there's a problem.
    public static @Nullable ObjectReferenceType getResultingOwnerRef(@NotNull CaseType aCase) throws SchemaException {
        String outcomeUri = MiscUtil.requireNonNull(
                aCase.getOutcome(), () -> "No outcome in " + aCase);
        List<ResourceObjectOwnerOptionType> matchingOptions = getOwnerOptionsList(aCase).stream()
                .filter(option -> outcomeUri.equals(option.getIdentifier()))
                .collect(Collectors.toList());
        ResourceObjectOwnerOptionType matchingOption = MiscUtil.extractSingletonRequired(matchingOptions,
                () -> new SchemaException("Multiple matching options for outcome " + outcomeUri + ": "
                        + matchingOptions + " in " + aCase),
                () -> new SchemaException("No matching option for outcome " + outcomeUri + " in " + aCase));
        return matchingOption.getCandidateOwnerRef();
    }

    public static @NotNull CaseCorrelationContextType getCorrelationContextRequired(@NotNull CaseType aCase) {
        return MiscUtil.requireNonNull(
                aCase.getCorrelationContext(),
                () -> new IllegalStateException("No correlation context in " + aCase));
    }

    public static @NotNull ObjectType getPreFocusRequired(CaseType aCase) {
        ObjectReferenceType ref = MiscUtil.requireNonNull(
                getCorrelationContextRequired(aCase).getPreFocusRef(),
                () -> new IllegalStateException("No pre-focus ref in " + aCase));
        return (ObjectType) MiscUtil.requireNonNull(
                ObjectTypeUtil.getObjectFromReference(ref),
                () -> new IllegalStateException("No pre-focus in " + aCase));
    }
}
