/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.service;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusDetails;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.authentication.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.PageDescriptor;
import com.evolveum.midpoint.authentication.api.Url;
import com.evolveum.midpoint.web.page.admin.users.component.ServiceSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/serviceNew")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL,
                label = "PageAdminServices.auth.servicesAll.label",
                description = "PageAdminServices.auth.servicesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICE_URL,
                label = "PageService.auth.role.label",
                description = "PageService.auth.role.description") })
public class PageService extends PageFocusDetails<ServiceType, FocusDetailsModels<ServiceType>> {

    public PageService() {
        super();
    }

    public PageService(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageService(PrismObject<ServiceType> service) {
        super(service);
    }

    @Override
    public Class<ServiceType> getType() {
        return ServiceType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, LoadableModel<ServiceType> summaryModel) {
        return new ServiceSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }
}
