/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.PageCertItems;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.List;

/**
 * Panel is used as an active campaigns preview on the Certification items page.
 * Campaign tiles are navigating to the certification items view, not to the campaign details.
 */
public class ActiveCampaignsPanel extends CampaignsPanel {
    @Serial private static final long serialVersionUID = 1L;

    public ActiveCampaignsPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    protected ObjectQuery getCustomCampaignsQuery() {
        return getPrismContext().queryFor(AccessCertificationCampaignType.class)
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
            protected LoadableDetachableModel<List<ProgressBar>> createCampaignProgressModel() {
                return CertMiscUtil.createCampaignWorkItemsProgressBarModel(getCampaign(), getPrincipalOid(), getPageBase());
            }

            @Override
            protected String getPrincipalOid() {
                return ActiveCampaignsPanel.this.getPrincipalOid();
            }

            @Override
            protected IModel<String> getDetailsButtonLabelModel() {
                return createStringResource("CampaignTilePanel.showItemsLabel");
            }

            @Override
            protected void detailsButtonClickPerformed(AjaxRequestTarget target) {
                showCertItems(getCampaign().getOid(), target);
            }
        };
    }

    @Override
    protected WebMarkupContainer createNavigationPanel(String id) {
        return new NavigationPanel(id) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createTitleModel() {
                return getActiveCampaignsPanelTitleModel();
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                ActiveCampaignsPanel.this.getPageBase().redirectBack();
            }

            @Override
            protected Component createNextButton(String id, IModel<String> nextTitle) {
                AjaxIconButton showAll = new AjaxIconButton(id, Model.of("fa fa-list"),
                        createStringResource("MyCampaignsPanel.showAll")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        showCertItems(null, ajaxRequestTarget);
                    }
                };
                showAll.showTitleAsLabel(true);
                showAll.add(AttributeAppender.append("class", "btn btn-default"));

                return showAll;
            }

        };
    }

    protected void showCertItems(String campaignOid, AjaxRequestTarget target) {
    }

    protected String getPrincipalOid() {
        return null;
    }

    protected IModel<String> getActiveCampaignsPanelTitleModel() {
        return createStringResource("ActiveCampaignsPanel.title");
    }

    @Override
    protected String getCampaignTileCssStyle() {
        return " ";
    }

    @Override
    protected void nameColumnLinkClickPerformed(AjaxRequestTarget target,
            IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
        if (rowModel == null || rowModel.getObject() == null || rowModel.getObject().getValue() == null) {
            return;
        }
        showCertItems(rowModel.getObject().getValue().getOid(), target);
    }

    @Override
    protected boolean isNameColumnLinkEnabled(IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
        return WebComponentUtil.isAuthorizedForPage(getCertItemsPage());
    }

    protected Class<? extends PageCertItems> getCertItemsPage() {
        return PageCertItems.class;
    }

}

