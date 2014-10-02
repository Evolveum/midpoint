/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

/**
 * @author lazyman
 */
public class PageAdminConfiguration extends PageAdmin {

    public static final String AUTH_CONFIGURATION_ALL = AuthorizationConstants.NS_AUTHORIZATION + "#configurationAll";
    public static final String AUTH_CONFIGURATION_ALL_LABEL = "PageAdminConfiguration.auth.configurationAll.label";
    public static final String AUTH_CONFIGURATION_ALL_DESCRIPTION = "PageAdminConfiguration.auth.configurationAll.description";

    protected static final int CONFIGURATION_TAB_BASIC = 0;
    protected static final int CONFIGURATION_TAB_LOGGING = 1;
}
