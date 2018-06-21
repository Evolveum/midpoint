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

package com.evolveum.midpoint.web.page.admin.cases;


import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

/**
 * @author bpowers
 */
public class PageAdminCaseWorkItems extends PageAdmin {

    public static final String AUTH_CASE_WORK_ITEMS_ALL = AuthorizationConstants.AUTZ_UI_CASE_WORK_ITEMS_ALL_URL;
    public static final String AUTH_CASE_WORK_ITEMS_ALL_LABEL = "PageAdminCaseWorkItems.auth.caseWorkItemsAll.label";
    public static final String AUTH_CASE_WORK_ITEMS_ALL_DESCRIPTION = "PageAdminCaseWorkItems.auth.caseWorkItemsAll.description";
    public static final String AUTH_CASE_WORK_ITEMS_ALLOCATED_TO_ME = AuthorizationConstants.AUTZ_UI_CASE_WORK_ITEMS_ALLOCATED_TO_ME_URL;
    public static final String AUTH_CASE_WORK_ITEMS_ALLOCATED_TO_ME_LABEL = "PageAdminCaseWorkItems.auth.caseWorkItemsAllocatedToMe.label";
    public static final String AUTH_CASE_WORK_ITEMS_ALLOCATED_TO_ME_DESCRIPTION = "PageAdminCaseWorkItems.auth.caseWorkItemsAllocatedToMe.description";
}
