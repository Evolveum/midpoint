/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.valuePolicy;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

/**
 * Created by matus on 9/8/2017.
 */
public class PageAdminValuePolicies extends PageAdmin {
    public static final String AUTH_VALUE_POLICIES_ALL = AuthorizationConstants.AUTZ_UI_VALUE_POLICIES_ALL_URL;
    public static final String AUTH_VALUE_POLICIES_ALL_LABEL = "PageAdminValuePolicies.auth.valuePoliciesAll.label";
    public static final String AUTH_VALUE_POLICIES_ALL_DESCRIPTION = "PageAdminValuePolicies.auth.valuePoliciesAll.description";
}
