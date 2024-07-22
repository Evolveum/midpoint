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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.ActiveCampaignsPanel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;

//TODO better name
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/myActiveCampaigns", matchUrlForSecurity = "/admin/certification/myActiveCampaigns")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = PageAdminCertification.AUTZ_MY_CERTIFICATION_CAMPAIGN_DECISIONS_URL,
                        label = PageAdminCertification.AUTH_MY_CERTIFICATION_CAMPAIGN_DECISIONS_LABEL,
                        description = PageAdminCertification.AUTH_MY_CERTIFICATION_CAMPAIGN_DECISIONS_DESCRIPTION)})
public class PageMyActiveCampaigns extends PageActiveCampaigns {

    @Serial private static final long serialVersionUID = 1L;

    public PageMyActiveCampaigns() {
    }

    @Override
    boolean isDisplayingAllItems() {
        return true;
    }

}
