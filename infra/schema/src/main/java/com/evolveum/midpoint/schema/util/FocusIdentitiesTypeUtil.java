/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentitySourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FocusIdentitiesTypeUtil {

    public static FocusIdentityType getMatchingIdentity(
            @NotNull FocusIdentitiesType identities, FocusIdentitySourceType source) {
        return getMatchingIdentity(identities.getIdentity(), source);
    }

    public static FocusIdentityType getMatchingIdentity(
            @NotNull Collection<FocusIdentityType> identities, FocusIdentitySourceType source) {
        List<FocusIdentityType> matchingList = identities.stream()
                .filter(i -> FocusIdentityTypeUtil.matches(i, source))
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(
                matchingList,
                () -> new IllegalStateException("Multiple identities matching " + source + ": " + matchingList));
    }
}
