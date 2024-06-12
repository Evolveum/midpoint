/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDecisions;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CampaignStateHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serial;

public class MyCampaignsPanel extends CampaignsPanel {

    @Serial private static final long serialVersionUID = 1L;

    public MyCampaignsPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initMyCampaignsLayout();
    }

    private void initMyCampaignsLayout() {
    }

    protected ObjectQuery getCustomCampaignsQuery() {
        FocusType principal = getPageBase().getPrincipalFocus();

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

    protected Component createCampaignTile(String id,
            IModel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> model) {
        return new CampaignTilePanel(id, model) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isAuthorizedForCampaignActions() {
                return false;
            }

            @Override
            protected MidPointPrincipal getPrincipal() {
                return MyCampaignsPanel.this.getPageBase().getPrincipal();
            }

            @Override
            protected IModel<String> getDetailsButtonLabelModel() {
                return createStringResource("CampaignTilePanel.showItems");
            }

            @Override
            protected void detailsButtonClickPerformed(AjaxRequestTarget target) {
                navigateToDecisionsPanel(getCampaign().getOid());
            }
        };
    }

    @Override
    protected WebMarkupContainer createNavigationPanel(String id) {
        return new NavigationPanel(id) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createTitleModel() {
                return createStringResource("PageMyCertCampaigns.title", "");
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                MyCampaignsPanel.this.getPageBase().redirectBack();
            }

            @Override
            protected Component createNextButton(String id, IModel<String> nextTitle) {
                AjaxIconButton showAll = new AjaxIconButton(id, Model.of("fa fa-list"),
                        createStringResource("MyCampaignsPanel.showAll")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        MyCampaignsPanel.this.getPageBase().navigateToNext(PageCertDecisions.class);
                    }
                };
                showAll.showTitleAsLabel(true);
                showAll.add(AttributeAppender.append("class", "btn btn-sm btn-secondary"));

                return showAll;
            }

        };
    }

    private void navigateToDecisionsPanel(String campaignOid) {
        PageParameters parameters = new PageParameters();
        parameters.set(PageCertDecisions.CAMPAIGN_OID_PARAMETER, campaignOid);
        getPageBase().navigateToNext(PageCertDecisions.class, parameters);
    }
}
