/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification;

import java.io.Serial;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

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

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/activeCampaigns", matchUrlForSecurity = "/admin/certification/activeCampaigns")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = PageAdminCertification.AUTZ_CERTIFICATION_CAMPAIGN_DECISIONS_URL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGN_DECISIONS_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGN_DECISIONS_DESCRIPTION)})
public class PageActiveCampaigns extends PageAdminCertification {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CAMPAIGNS_PANEL = "campaignsPanel";

    public PageActiveCampaigns() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    //TODO back button behavior
    private void initLayout() {
        ActiveCampaignsPanel campaignsPanel = new ActiveCampaignsPanel(ID_CAMPAIGNS_PANEL) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void showCertItems(String campaignOid, AjaxRequestTarget target) {
                PageParameters params = new PageParameters();
                params.add(OnePageParameterEncoder.PARAMETER, campaignOid);
                navigateToNext(PageCertItems.class, params);
                target.add(PageActiveCampaigns.this);
            }

            @Override
            protected ObjectQuery getCustomCampaignsQuery() {
                if (isDisplayingAllItems()) {
                    return getPrismContext().queryFor(AccessCertificationCampaignType.class)
                            .item(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM,
                                    AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP)
                            .isNull()
                            .build();
                } else {
                    MidPointPrincipal principal = getPrincipal();

                    return getPrismContext().queryFor(AccessCertificationCampaignType.class)
                            .item(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM,
                                    AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                            .ref(principal.getOid())
                            .and()
                            .item(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM,
                                    AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP)
                            .isNull()
                            .build();
                }
            }

            @Override
            protected MidPointPrincipal getPrincipal() {
                return PageActiveCampaigns.this.getPrincipalAsReviewer();
            }

        };
        campaignsPanel.setOutputMarkupId(true);
        add(campaignsPanel);
    }

    private MidPointPrincipal getPrincipalAsReviewer() {
        return isDisplayingAllItems() ? null : getPrincipal();
    }


    public IModel<String> getCampaignStartDateModel(AccessCertificationCampaignType campaign) {
        return () -> {
            if (campaign == null) {
                return "";
            }
            XMLGregorianCalendar startDate = campaign.getStartTimestamp();
            return WebComponentUtil.getLocalizedDate(startDate, DateLabelComponent.SHORT_NOTIME_STYLE);
        };
    }


    boolean isDisplayingAllItems() {
        return false;
    }

}
