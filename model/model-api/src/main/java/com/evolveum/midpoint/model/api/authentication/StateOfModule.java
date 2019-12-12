/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

/**
 * @author skublik
 */

public enum StateOfModule {
    LOGIN_PROCESSING,
    FAILURE,
    SUCCESSFULLY,
    LOGOUT_PROCESSING,
    CALLED_OFF;
}
