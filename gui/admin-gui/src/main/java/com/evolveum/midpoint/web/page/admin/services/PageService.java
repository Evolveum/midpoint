/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.services;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractRoleMainPanel;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.page.admin.PageAdminAbstractRole;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.users.component.ServiceSummaryPanel;
import com.evolveum.midpoint.web.security.GuiAuthorizationConstants;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

@PageDescriptor(url = "/admin/service", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL,
                label = "PageAdminServices.auth.servicesAll.label",
                description = "PageAdminServices.auth.servicesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICE_URL,
                label = "PageService.auth.role.label",
                description = "PageService.auth.role.description") })
public class PageService extends PageAdminAbstractRole<ServiceType> implements ProgressReportingAwarePage{

    private static final long serialVersionUID = 1L;

    public PageService() {
        super();
    }

    public PageService(PageParameters parameters) {
        super(parameters);
    }

    public PageService(final PrismObject<ServiceType> role) {
        super(role);
    }

    public PageService(final PrismObject<ServiceType> userToEdit, boolean isNewObject) {
        super(userToEdit, isNewObject);
    }

    public PageService(final PrismObject<ServiceType> abstractRole, boolean isNewObject, boolean isReadonly) {
        super(abstractRole, isNewObject, isReadonly);
    }

    @Override
    protected ServiceType createNewObject() {
        return new ServiceType();
    }

    @Override
    public Class<ServiceType> getCompileTimeClass() {
        return ServiceType.class;
    }

    @Override
    protected Class getRestartResponsePage() {
        return PageServices.class;
    }

    @Override
    protected FocusSummaryPanel<ServiceType> createSummaryPanel() {
        return new ServiceSummaryPanel(ID_SUMMARY_PANEL, Model.of(getObjectModel().getObject().getObject().asObjectable()), this);
    }

    @Override
    protected AbstractObjectMainPanel<ServiceType> createMainPanel(String id) {
        return new AbstractRoleMainPanel<ServiceType>(id, getObjectModel(),
                getProjectionModel(), this) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void viewObjectHistoricalDataPerformed(AjaxRequestTarget target, PrismObject<ServiceType> object, String date){
                PageService.this.navigateToNext(new PageServiceHistory(object, date));
            }

            @Override
            protected boolean isFocusHistoryPage(){
                return PageService.this.isFocusHistoryPage();
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
