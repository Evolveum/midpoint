/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.provisioning.ucf.api.UcfSyncToken;

import org.identityconnectors.framework.common.objects.SyncToken;
import org.jetbrains.annotations.Contract;

/**
 * Utility methods to work with the token extension item and SyncToken objects.
 */
public class TokenUtil {

    static SyncToken toConnId(UcfSyncToken ucfToken) {
        if (ucfToken != null) {
            return new SyncToken(ucfToken.getValue());
        } else {
            return null;
        }
    }

    @Contract("!null -> !null; null -> null")
    static UcfSyncToken toUcf(SyncToken connIdToken) {
        if (connIdToken != null) {
            return UcfSyncToken.of(connIdToken.getValue());
        } else {
            return null;
        }
    }
}
