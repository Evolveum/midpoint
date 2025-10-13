/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentitySourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

public class FocusIdentitySourceTypeUtil {

    @VisibleForTesting
    public static FocusIdentitySourceType defaultAccount(@NotNull String resourceOid) {
        return resourceObject(resourceOid, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
    }

    public static FocusIdentitySourceType resourceObject(String resourceOid, ShadowKindType kind, String intent) {
        return new FocusIdentitySourceType()
                .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                .kind(kind)
                .intent(intent);
    }
}
