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

package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;

/**
 * @author bpowers
 */
@PageDescriptor(url = "/admin/caseWorkItemsAllocatedToMe", action = {
        @AuthorizationAction(actionUri = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALLOCATED_TO_ME,
                label = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALLOCATED_TO_ME_LABEL,
                description = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALLOCATED_TO_ME_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASE_WORK_ITEMS_ALLOCATED_TO_ME_URL,
                label = "PageCaseWorkItems.auth.caseWorkItemsAllocatedToMe.label",
                description = "PageCaseWorkItems.auth.caseWorkItemsAllocatedToMe.description")})
public class PageCaseWorkItemsAllocatedToMe extends PageCaseWorkItems {

    public PageCaseWorkItemsAllocatedToMe() {
        super(false);
    }

}
