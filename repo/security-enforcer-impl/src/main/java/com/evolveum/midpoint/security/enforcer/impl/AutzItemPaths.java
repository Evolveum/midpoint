/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.api.PositiveNegativeItemPaths;

/**
 * An extension of {@link PositiveNegativeItemPaths} providing a helper method to collect them from an authorization.
 *
 * @author semancik
 */
class AutzItemPaths extends PositiveNegativeItemPaths {

    void collectItems(Authorization autz) {
        collectItemPaths(autz.getItems(), autz.getExceptItems());
    }
}
