/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.cases;

import java.util.List;

import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectOwnerOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectOwnerOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class CorrelationCaseUtil {

    public static @Nullable ResourceObjectOwnerOptionsType getOwnerOptions(@NotNull CaseType aCase) {
        ShadowType shadow = (ShadowType) ObjectTypeUtil.getObjectFromReference(aCase.getObjectRef());
        if (shadow != null && shadow.getCorrelation() != null) {
            return shadow.getCorrelation().getOwnerOptions();
        } else {
            return null;
        }
    }

    public static @NotNull List<ResourceObjectOwnerOptionType> getOwnerOptionsList(@NotNull CaseType aCase) {
        var info = getOwnerOptions(aCase);
        return info != null ? info.getOption() : List.of();
    }

    public static @NotNull String getShadowOidRequired(@NotNull CaseType aCase) throws SchemaException {
        return MiscUtil.requireNonNull(
                MiscUtil.requireNonNull(
                                aCase.getObjectRef(), () -> "No objectRef in " + aCase)
                        .getOid(), () -> "No shadow OID in " + aCase);

    }
}
