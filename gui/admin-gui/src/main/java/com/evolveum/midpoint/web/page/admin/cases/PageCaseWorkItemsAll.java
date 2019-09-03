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

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.wf.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CREATE_TIMESTAMP;

/**
 * @author bpowers
 */
@PageDescriptor(url = "/admin/caseWorkItemsAll", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASE_WORK_ITEMS_ALL_URL,
                label = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALL_LABEL,
                description = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASE_WORK_ITEMS_ALL_URL,
                label = "PageCaseWorkItems.auth.caseWorkItemsAll.label",
                description = "PageCaseWorkItems.auth.caseWorkItemsAll.description")})
public class PageCaseWorkItemsAll extends PageCaseWorkItems {
    private static final long serialVersionUID = 1L;

    public PageCaseWorkItemsAll() {
        super();
    }

    @Override
    protected ObjectFilter getCaseWorkItemsFilter(){
        return getPrismContext().queryFor(CaseWorkItemType.class)
                .not()
                .item(PrismConstants.T_PARENT, CaseType.F_STATE)
                .eq(SchemaConstants.CASE_STATE_CLOSED)
                .buildFilter();
    }
}
