/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.orgs;

import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractRoleMainPanel;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.page.admin.PageAdminAbstractRole;
import com.evolveum.midpoint.web.page.admin.roles.AvailableRelationDto;
import com.evolveum.midpoint.web.page.admin.users.component.OrgMemberPanel;
import com.evolveum.midpoint.web.page.admin.users.component.OrgSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/org/unit", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL,
                label = "PageAdminUsers.auth.orgAll.label",
                description = "PageAdminUsers.auth.orgAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_UNIT_URL,
                label = "PageOrgUnit.auth.orgUnit.label",
                description = "PageOrgUnit.auth.orgUnit.description") })
public class PageOrgUnit extends PageAdminAbstractRole<OrgType> implements ProgressReportingAwarePage {

    private static final Trace LOGGER = TraceManager.getTrace(PageOrgUnit.class);

    public PageOrgUnit() {
        super();
    }

    public PageOrgUnit(PageParameters parameters) {
        super(parameters);
    }

    public PageOrgUnit(final PrismObject<OrgType> role) {
        super(role);
    }

    public PageOrgUnit(final PrismObject<OrgType> userToEdit, boolean isNewObject) {
        super(userToEdit, isNewObject);
    }

    public PageOrgUnit(final PrismObject<OrgType> abstractRole, boolean isNewObject, boolean isReadonly) {
        super(abstractRole, isNewObject, isReadonly);
    }

    @Override
    protected OrgType createNewObject() {
        return new OrgType();
    }

    @Override
    public Class<OrgType> getCompileTimeClass() {
        return OrgType.class;
    }

    @Override
    protected Class getRestartResponsePage() {
        return PageOrgTree.class;
    }

    @Override
    protected FocusSummaryPanel<OrgType> createSummaryPanel(IModel<OrgType> summaryModel) {
        return new OrgSummaryPanel(ID_SUMMARY_PANEL, summaryModel, this);
    }

    @Override
    protected AbstractObjectMainPanel<OrgType> createMainPanel(String id) {
        return new AbstractRoleMainPanel<OrgType>(id, getObjectModel(),
                getProjectionModel(), this) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isFocusHistoryPage(){
                return PageOrgUnit.this.isFocusHistoryPage();
            }

            @Override
            protected void viewObjectHistoricalDataPerformed(AjaxRequestTarget target, PrismObject<OrgType> object, String date){
                PageOrgUnit.this.navigateToNext(new PageOrgUnitHistory(object, date));
            }

            @Override
            public OrgMemberPanel createMemberPanel(String panelId, PageBase parentPage) {

                return new OrgMemberPanel(panelId, new Model<>(getObject().asObjectable()), parentPage) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected AvailableRelationDto getSupportedRelations() {
                        return getSupportedMembersTabRelations(getDefaultRelationConfiguration());
                    }

                    @Override
                    protected String getStorageKeyTabSuffix() {
                        return "orgMembers";
                    }

                };
            }

            @Override
            public OrgMemberPanel createGovernancePanel(String panelId, PageBase parentPage) {

                return new OrgMemberPanel(panelId, new Model<>(getObject().asObjectable()), parentPage) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected AvailableRelationDto getSupportedRelations() {
                        return getSupportedGovernanceTabRelations(getDefaultRelationConfiguration());
                    }

                    @Override
                    protected Map<String, String> getAuthorizations(QName complexType) {
                        return getGovernanceTabAuthorizations();
                    }

                    @Override
                    protected String getStorageKeyTabSuffix() {
                        return "orgGovernance";
                    }

                };
            }

            @Override
            protected boolean getOptionsPanelVisibility() {
                if (isSelfProfile()){
                    return false;
                } else {
                    return super.getOptionsPanelVisibility();
                }
            }


        };
    }

}
