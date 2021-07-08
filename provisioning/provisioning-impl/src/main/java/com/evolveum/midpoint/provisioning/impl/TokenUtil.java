/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
