/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import org.apache.wicket.model.IModel;

import java.io.Serial;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/myActiveCampaigns", matchUrlForSecurity = "/admin/certification/myActiveCampaigns")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_MY_ACTIVE_CERT_CAMPAIGNS_URL,
                        label = PageAdminCertification.AUTH_MY_ACTIVE_CERT_CAMPAIGNS_LABEL,
                        description = PageAdminCertification.AUTH_MY_ACTIVE_CERT_CAMPAIGNS_DESCRIPTION)})
public class PageMyActiveCampaigns extends PageActiveCampaigns {

    @Serial private static final long serialVersionUID = 1L;

    public PageMyActiveCampaigns() {
    }

    @Override
    boolean isDisplayingAllItems() {
        return false;
    }

    @Override
    protected Class<? extends PageCertItems> getCertItemsPage() {
        return PageMyCertItems.class;
    }

    @Override
    protected IModel<String> getActiveCampaignsPanelTitleModel() {
        return createStringResource("MyActiveCampaignsPanel.title");
    }

}
