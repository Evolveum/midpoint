/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.util;

import com.evolveum.midpoint.security.api.AuthorizationConstants;

public class AuthConstants {
    public static final String DEFAULT_PATH_AFTER_LOGIN = "/self/dashboard";
    public static final String DEFAULT_PATH_AFTER_LOGOUT = "/";

    public static final String AUTH_CONFIGURATION_ALL = AuthorizationConstants.AUTZ_UI_CONFIGURATION_ALL_URL;
    public static final String AUTH_CONFIGURATION_ALL_LABEL = "PageAdminConfiguration.auth.configurationAll.label";
    public static final String AUTH_CONFIGURATION_ALL_DESCRIPTION = "PageAdminConfiguration.auth.configurationAll.description";

    public static final String SEC_QUESTION_J_QID = "qid";
    public static final String SEC_QUESTION_J_QANS = "qans";
    public static final String SEC_QUESTION_J_QTXT = "qtxt";
    public static final String ATTR_VERIFICATION_J_PATH = "path";
    public static final String ATTR_VERIFICATION_J_VALUE = "value";
}
