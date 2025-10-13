/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
