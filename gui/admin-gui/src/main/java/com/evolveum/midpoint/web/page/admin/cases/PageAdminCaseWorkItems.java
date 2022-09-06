/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.web.page.admin.PageAdmin;

import com.evolveum.midpoint.web.page.admin.workflow.PageAttorneySelection;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

/**
 * @author bpowers
 */
public class PageAdminCaseWorkItems extends PageAdmin {

    public static final String AUTH_CASE_WORK_ITEMS_ALL_LABEL = "PageAdminCaseWorkItems.auth.caseWorkItemsAll.label";
    public static final String AUTH_CASE_WORK_ITEMS_ALL_DESCRIPTION = "PageAdminCaseWorkItems.auth.caseWorkItemsAll.description";
    public static final String AUTH_CASE_WORK_ITEMS_ALLOCATED_TO_ME_LABEL = "PageAdminCaseWorkItems.auth.caseWorkItemsAllocatedToMe.label";
    public static final String AUTH_CASE_WORK_ITEMS_ALLOCATED_TO_ME_DESCRIPTION = "PageAdminCaseWorkItems.auth.caseWorkItemsAllocatedToMe.description";

    public PageAdminCaseWorkItems(PageParameters pageParameters) {
        super(pageParameters);
    }

    protected String getPowerDonorOid() {
        PageParameters parameters = getPageParameters();
        if (parameters == null) {
            return null;
        }

        StringValue attorneyParam = parameters.get(PageAttorneySelection.PARAMETER_DONOR_OID);
        if (attorneyParam.isNull() || attorneyParam.isEmpty()) {
            return null;
        }

        return attorneyParam.toString();
    }

}
