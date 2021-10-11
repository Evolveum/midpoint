/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.wf.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CREATE_TIMESTAMP;

/**
 * @author bpowers
 */
@PageDescriptor(url = "/admin/myWorkItems", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MY_WORK_ITEMS_URL,
                label = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALLOCATED_TO_ME_LABEL,
                description = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALLOCATED_TO_ME_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                label = "PageCaseWorkItems.auth.caseWorkItemsAllocatedToMe.label",
                description = "PageCaseWorkItems.auth.caseWorkItemsAllocatedToMe.description")})
public class PageCaseWorkItemsAllocatedToMe extends PageCaseWorkItems {
    private static final long serialVersionUID = 1L;

    public PageCaseWorkItemsAllocatedToMe() {
        super();
    }

    @Override
    protected ObjectFilter getCaseWorkItemsFilter(){
        return QueryUtils.filterForNotClosedStateAndAssignees(getPrismContext().queryFor(CaseWorkItemType.class),
                SecurityUtils.getPrincipalUser(),
                OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS, getRelationRegistry())
                .desc(F_CREATE_TIMESTAMP)
                .buildFilter();
    }

    @Override
    public Breadcrumb redirectBack() {
        return redirectBack(1);
    }
}
