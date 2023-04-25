/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.api.PositiveNegativeItemPaths;

/**
 * Helper class to {@link SecurityEnforcerImpl}, used to evaluate item authorizations.
 *
 * @author semancik
 */
class AutzItemPaths extends PositiveNegativeItemPaths {

    void collectItems(Authorization autz) {
        collectItemPaths(autz.getItems(), autz.getExceptItems());
    }
}
