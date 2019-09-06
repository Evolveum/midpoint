/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/workItemsAttorney")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_APPROVALS_ALL_URL,
                        label = PageAdminWorkItems.AUTH_APPROVALS_ALL_LABEL,
                        description = PageAdminWorkItems.AUTH_APPROVALS_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ATTORNEY_WORK_ITEMS_URL,
                        label = "PageAttorneySelection.auth.workItems.attorney.label",
                        description = "PageAttorneySelection.auth.workItems.attorney.description")
        })
public class PageWorkItemsAttorney extends PageWorkItems {

    private static final Trace LOGGER = TraceManager.getTrace(PageWorkItemsAttorney.class);

    private static final String DOT_CLASS = PageWorkItems.class.getName() + ".";


    public PageWorkItemsAttorney() {
        super(WorkItemsPageType.ATTORNEY);
    }
}
