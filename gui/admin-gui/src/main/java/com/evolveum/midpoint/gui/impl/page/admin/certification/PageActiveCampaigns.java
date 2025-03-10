/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.self.dashboard.PageSelfDashboard;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.ActiveCampaignsPanel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
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
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_ACTIVE_CERT_CAMPAIGNS_URL,
                        label = PageAdminCertification.AUTH_ACTIVE_CERT_CAMPAIGNS_LABEL,
                        description = PageAdminCertification.AUTH_ACTIVE_CERT_CAMPAIGNS_DESCRIPTION)})
public class PageActiveCampaigns extends PageAdminCertification {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageActiveCampaigns.class);

    private static final String DOT_CLASS = PageActiveCampaigns.class.getName() + ".";
    private static final String OPERATION_LOAD_ACTIVE_CAMPAIGNS = DOT_CLASS + "loadActiveCampaigns";


    private static final String ID_CAMPAIGNS_PANEL = "campaignsPanel";

    public PageActiveCampaigns() {
    }

    @Override
    protected void onAfterRender() {
        super.onAfterRender();
        List<PrismObject<AccessCertificationCampaignType>> campaigns = loadActiveCampaigns();
        boolean isSingleActiveCampaign = campaigns.size() == 1;
        if (isSingleActiveCampaign && isRedirectedFromDashboardPage()) {
            showCertItems(campaigns.get(0).getOid());
        }
    }

    private boolean isRedirectedFromDashboardPage() {
        Breadcrumb previousPage = getPreviousBreadcrumb();
        return previousPage != null &&  PageSelfDashboard.class.equals(previousPage.getPageClass());
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
                PageActiveCampaigns.this.showCertItems(campaignOid);
            }

            @Override
            protected ObjectQuery getCustomCampaignsQuery() {
                return getActiveCampaignsQuery();
            }

            @Override
            protected String getPrincipalOid() {
                return PageActiveCampaigns.this.getReviewerOid();
            }

            @Override
            protected IModel<String> getActiveCampaignsPanelTitleModel() {
                return PageActiveCampaigns.this.getActiveCampaignsPanelTitleModel();
            }

            @Override
            protected Class<? extends PageCertItems> getCertItemsPage() {
                return PageActiveCampaigns.this.getCertItemsPage();
            }

        };
        campaignsPanel.setOutputMarkupId(true);
        add(campaignsPanel);
    }

    protected IModel<String> getActiveCampaignsPanelTitleModel() {
        return createStringResource("ActiveCampaignsPanel.title");
    }

    private List<PrismObject<AccessCertificationCampaignType>> loadActiveCampaigns() {
        Task task = createSimpleTask(OPERATION_LOAD_ACTIVE_CAMPAIGNS);
        OperationResult result = task.getResult();
        ObjectQuery query = getActiveCampaignsQuery();
        try {
            return WebModelServiceUtils.searchObjects(AccessCertificationCampaignType.class, query, null, result, PageActiveCampaigns.this);
        } catch (Exception ex) {
            LOGGER.error("Couldn't load active certification campaigns", ex);
            return new ArrayList<>();
        }
    }

    private ObjectQuery getActiveCampaignsQuery() {
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

    private String getReviewerOid() {
        return getPrincipalAsReviewer() != null ? getPrincipalAsReviewer().getOid() : null;
    }


    private MidPointPrincipal getPrincipalAsReviewer() {
        return isDisplayingAllItems() ? null : getPrincipal();
    }


    boolean isDisplayingAllItems() {
        return true;
    }

    private void showCertItems(String campaignOid) {
        PageParameters params = new PageParameters();
        if (StringUtils.isNotEmpty(campaignOid)) {
            params.add(OnePageParameterEncoder.PARAMETER, campaignOid);
        }
        navigateToNext(getCertItemsPage(), params);
    }

    protected Class<? extends PageCertItems> getCertItemsPage() {
        return PageCertItems.class;
    }
}
