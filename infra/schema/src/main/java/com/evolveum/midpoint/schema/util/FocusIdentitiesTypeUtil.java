/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;

import java.util.List;
import java.util.stream.Collectors;

public class FocusIdentitiesTypeUtil {

    public static FocusIdentityType getOwnIdentity(FocusIdentitiesType identities) {
        List<FocusIdentityType> ownList = identities.getIdentity().stream()
                .filter(FocusIdentityTypeUtil::isOwn)
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(
                ownList,
                () -> new IllegalStateException("Multiple own identities: " + ownList));
    }
}
