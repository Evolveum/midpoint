/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author lazyman
 */
public class PageAdminReports extends PageAdmin {

    public static final String AUTH_REPORTS_ALL = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL;
    public static final String AUTH_REPORTS_ALL_LABEL = "PageAdminReports.auth.reportsAll.label";
    public static final String AUTH_REPORTS_ALL_DESCRIPTION = "PageAdminReports.auth.reportsAll.description";

    public PageAdminReports(){
        this(null);
    }

    public PageAdminReports(PageParameters parameters){
        super(parameters);
    }
}
