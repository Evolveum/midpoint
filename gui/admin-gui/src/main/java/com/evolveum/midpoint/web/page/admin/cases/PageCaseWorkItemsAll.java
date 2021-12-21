/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * @author bpowers
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/allWorkItems", matchUrlForSecurity = "/admin/allWorkItems")
        },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ALL_WORK_ITEMS_URL,
                label = PageAdminWorkItems.AUTH_APPROVALS_ALL_LABEL,
                description = PageAdminWorkItems.AUTH_APPROVALS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                label = "PageWorkItemsAll.auth.allWorkItems.label",
                description = "PageWorkItemsAll.auth.allWorkItems.description")})
public class PageCaseWorkItemsAll extends PageCaseWorkItems {
    private static final long serialVersionUID = 1L;

    public PageCaseWorkItemsAll() {
        super();
    }

    @Override
    protected ObjectFilter getCaseWorkItemsFilter(){
        return getPrismContext().queryFor(CaseWorkItemType.class)
                .item(CaseWorkItemType.F_CLOSE_TIMESTAMP)
                .isNull()
                .buildFilter();
    }
}
