/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.page.admin.certification.PageAdminCertification;
import com.evolveum.midpoint.web.page.admin.certification.PageCertCampaigns;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/myCampaigns", matchUrlForSecurity = "/admin/certification/myCampaigns")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_MY_CERTIFICATION_CAMPAIGNS,
                label = PageAdminCertification.AUTH_MY_CERTIFICATION_CAMPAIGNS_LABEL,
                description = PageAdminCertification.AUTH_MY_CERTIFICATION_CAMPAIGNS_DESCRIPTION) })
public class PageMyCertCampaigns extends PageCertCampaigns {

    public PageMyCertCampaigns() {
        super(new PageParameters());
    }

    @Override
    protected CampaignsPanel createCampaignsPanel(String id) {
        return new MyCampaignsPanel(id);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return Model.of("");
    }
}
