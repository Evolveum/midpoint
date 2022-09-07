/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.workflow;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CREATE_TIMESTAMP;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.page.admin.cases.PageCaseWorkItems;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.util.string.StringValue;

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
public class PageWorkItemsAttorney extends PageCaseWorkItems {

    public PageWorkItemsAttorney(PageParameters pageParameters) {
        super(pageParameters);
    }

    @Override
    protected ObjectFilter getCaseWorkItemsFilter() {
        String attorneyUserOid = getPowerDonorOid();
        if (StringUtils.isEmpty(attorneyUserOid) || attorneyUserOid.equals("null")) {
            return super.getCaseWorkItemsFilter();
        }
        return getPrismContext().queryFor(CaseWorkItemType.class)
                .item(CaseWorkItemType.F_ASSIGNEE_REF)
                .ref(attorneyUserOid, UserType.COMPLEX_TYPE)
                .and()
                .item(CaseWorkItemType.F_CLOSE_TIMESTAMP)
                .isNull()
                .desc(F_CREATE_TIMESTAMP)
                .buildFilter();
    }
}
