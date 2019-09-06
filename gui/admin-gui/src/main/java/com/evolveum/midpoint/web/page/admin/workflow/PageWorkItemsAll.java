/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/workItemsAll", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_APPROVALS_ALL_URL,
                    label = PageAdminWorkItems.AUTH_APPROVALS_ALL_LABEL,
                    description = PageAdminWorkItems.AUTH_APPROVALS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ALL_WORK_ITEMS_URL,
                label = "PageWorkItemsAll.auth.allWorkItems.label",
                description = "PageWorkItemsAll.auth.allWorkItems.description")})
public class PageWorkItemsAll extends PageWorkItems {

    public PageWorkItemsAll() {
        super(WorkItemsPageType.ALL);
    }

}
