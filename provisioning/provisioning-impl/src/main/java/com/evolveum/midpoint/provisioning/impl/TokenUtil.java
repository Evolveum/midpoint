/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.provisioning.api.LiveSyncToken;
import com.evolveum.midpoint.provisioning.ucf.api.UcfSyncToken;

public class TokenUtil {

    public static UcfSyncToken toUcf(LiveSyncToken provisioningToken) {
        return provisioningToken != null ? UcfSyncToken.of(provisioningToken.getValue()) : null;
    }

    public static LiveSyncToken fromUcf(UcfSyncToken ucfToken) {
        return ucfToken != null ? LiveSyncToken.of(ucfToken.getValue()) : null;
    }
}
