/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.PageAdminCertification;

import com.evolveum.midpoint.web.page.admin.certification.component.CertCampaignSummaryPanel;
import com.evolveum.midpoint.web.page.admin.certification.component.CertificationItemsTabbedPanel;
import com.evolveum.midpoint.web.page.admin.certification.component.StatisticBoxDto;
import com.evolveum.midpoint.web.page.admin.certification.component.StatisticListBoxPanel;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CampaignStateHelper;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CertificationItemResponseHelper;

import com.evolveum.midpoint.web.page.admin.reports.ReportDownloadHelper;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/campaign",
                        matchUrlForSecurity = "/admin/certification/campaign")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS,
                        label = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS_DESCRIPTION) }
)
public class PageCertCampaign extends PageAssignmentHolderDetails<AccessCertificationCampaignType, AssignmentHolderDetailsModel<AccessCertificationCampaignType>> {

    @Serial private static final long serialVersionUID = 1L;

    public PageCertCampaign() {
        this(new PageParameters());
    }

    public PageCertCampaign(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected boolean supportNewDetailsLook() {
        return true;
    }

    @Override
    protected InlineOperationalButtonsPanel<AccessCertificationCampaignType> createInlineButtonsPanel(String idButtons, LoadableModel<PrismObjectWrapper<AccessCertificationCampaignType>> objectWrapperModel) {
        return new InlineOperationalButtonsPanel<>(idButtons, objectWrapperModel) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isSaveButtonVisible() {
                return false;
            }

            @Override
            protected boolean isDeleteButtonVisible() {
                return true;
            }

            @Override
            protected IModel<String> getDeleteButtonLabelModel(PrismObjectWrapper<AccessCertificationCampaignType> modelObject) {
                return Model.of("PageCertCampaign.button.deleteCampaign");
            }

            @Override
            protected void addRightButtons(@NotNull RepeatingView rightButtonsView) {
                addCampaignManagementButtons(rightButtonsView);
            }

            @Override
            protected IModel<String> createSubmitButtonLabelModel(PrismObjectWrapper<AccessCertificationCampaignType> modelObject) {
                return Model.of();
            }

            @Override
            protected IModel<String> getTitle() {
                return createStringResource("PageCertCampaign.campaignView");
            }
        };

    }

    @Override
    public Class<AccessCertificationCampaignType> getType() {
        return AccessCertificationCampaignType.class;
    }

    @Override
    protected Panel createVerticalSummaryPanel(String id, IModel<AccessCertificationCampaignType> summaryModel) {
        return new CertCampaignSummaryPanel(id, summaryModel);
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<AccessCertificationCampaignType> summaryModel) {
        return null;
    }

    //TODO ??
    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> WebComponentUtil.getDisplayNameOrName(getModelPrismObject());
    }

    @Override
    protected CertificationDetailsModel createObjectDetailsModels(PrismObject<AccessCertificationCampaignType> object) {
        return new CertificationDetailsModel(createPrismObjectModel(object), this);
    }

    private void addCampaignManagementButtons(RepeatingView rightButtonsView) {
        LoadableDetachableModel<String> buttonLabelModel = getActionButtonTitleModel();
        LoadableDetachableModel<String> buttonCssModel = getActionButtonCssModel();
        AjaxIconButton button = new AjaxIconButton(rightButtonsView.newChildId(), buttonCssModel, buttonLabelModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                CampaignProcessingHelper.campaignActionPerformed(getModelObjectType(), PageCertCampaign.this, ajaxRequestTarget);

                //todo fix reloading of the page
                getObjectDetailsModels().reloadPrismObjectModel();
                PageCertCampaign.this.getModel().detach();

                buttonCssModel.detach();
                buttonLabelModel.detach();

                ajaxRequestTarget.add(PageCertCampaign.this);
            }
        };
        button.showTitleAsLabel(true);
        button.add(AttributeAppender.append("class", "btn btn-primary"));
        rightButtonsView.add(button);
    }

    private LoadableDetachableModel<String> getActionButtonCssModel() {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                CampaignStateHelper campaignStateHelper = new CampaignStateHelper(getModelObjectType());
                return campaignStateHelper.getNextAction().getActionIcon().getCssClass();
            }
        };
    }

    private LoadableDetachableModel<String> getActionButtonTitleModel() {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                CampaignStateHelper campaignStateHelper = new CampaignStateHelper(getModelObjectType());
                return createStringResource(campaignStateHelper.getNextAction().getActionLabelKey()).getString();
            }
        };
    }
}
