/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.org;

import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Created by honchar.
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/orgHistory", matchUrlForSecurity = "/admin/orgHistory")
        },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL,
                label = "PageAdminUsers.auth.orgAll.label",
                description = "PageAdminUsers.auth.orgAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_UNIT_HISTORY_URL,
                label = "PageOrgHistory.auth.orgUnitHistory.label",
                description = "PageOrgHistory.auth.orgUnitHistory.description")})
public class PageOrgHistory extends PageOrg {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageOrgHistory.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageOrgHistory.class);


    private static final String OPERATION_RESTRUCT_OBJECT = DOT_CLASS + "restructObject";
    private static final String OID_PARAMETER_LABEL = "oid";
    private static final String EID_PARAMETER_LABEL = "eventIdentifier";
    private static final String DATE_PARAMETER_LABEL = "date";

    public PageOrgHistory(PageParameters pageParameters) {
        super(pageParameters);
    }

    @Override
    protected AbstractRoleDetailsModel<OrgType> createObjectDetailsModels(PrismObject<OrgType> object) {
        return new AbstractRoleDetailsModel<>(createPrismObjectModel(getReconstructedObject()), true, this);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                String name = null;
                if (getModelObjectType() != null) {
                    name = WebComponentUtil.getName(getModelObjectType());
                }
                return createStringResource("PageUserHistory.title", name, getDate()).getObject();
            }
        };
    }

    private PrismObject<OrgType> getReconstructedObject() {
        OperationResult result = new OperationResult(OPERATION_RESTRUCT_OBJECT);
        try {
            Task task = createSimpleTask(OPERATION_RESTRUCT_OBJECT);
            return getModelAuditService().reconstructObject(OrgType.class, getObjectId(), getEventIdentifier(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(
                    createStringResource("ObjectHistoryTabPanel.message.getReconstructedObject.fatalError")
                            .getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't restruct object", ex);
        }
        return null;
    }

    public String getDate() {
        return String.valueOf(getPageParameters().get(DATE_PARAMETER_LABEL));
    }

    public String getObjectId() {
        return String.valueOf(getPageParameters().get(OID_PARAMETER_LABEL));
    }

    public String getEventIdentifier() {
        return String.valueOf(getPageParameters().get(EID_PARAMETER_LABEL));
    }

}
