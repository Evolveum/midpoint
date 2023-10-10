/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Define actual state of authentication module between requests
 *
 * @author skublik
 */
@Experimental
public enum AuthenticationModuleState {
    LOGIN_PROCESSING,
    FAILURE,
    FAILURE_CONFIGURATION,
    SUCCESSFULLY,
    LOGOUT_PROCESSING,
    CALLED_OFF
}
