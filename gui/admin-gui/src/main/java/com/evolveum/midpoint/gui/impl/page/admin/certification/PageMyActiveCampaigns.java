/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serial;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/myActiveCampaigns", matchUrlForSecurity = "/admin/certification/myActiveCampaigns")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_MY_CERTIFICATION_CAMPAIGN_DECISIONS_URL,
                        label = PageAdminCertification.AUTH_MY_CERTIFICATION_CAMPAIGN_DECISIONS_LABEL,
                        description = PageAdminCertification.AUTH_MY_CERTIFICATION_CAMPAIGN_DECISIONS_DESCRIPTION)})
public class PageMyActiveCampaigns extends PageActiveCampaigns {

    @Serial private static final long serialVersionUID = 1L;

    public PageMyActiveCampaigns() {
    }

    @Override
    boolean isDisplayingAllItems() {
        return false;
    }

    protected Class<? extends PageCertItems> getCertItemsPage() {
        return PageMyCertItems.class;
    }

}
