/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.SelectReportTemplatePanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.util.ReportParameterTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import com.evolveum.midpoint.gui.impl.page.admin.certification.component.CertCampaignSummaryPanel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CampaignStateHelper;

import org.apache.wicket.AttributeModifier;
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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

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

    private static final Trace LOGGER = TraceManager.getTrace(PageCertCampaign.class);
    private static final String DOT_CLASS = PageCertCampaign.class.getName() + ".";
    private static final String OPERATION_LOAD_REPORT = DOT_CLASS + "loadCertItemsReport";

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
                return createStringResource("PageCertCampaign.button.deleteCampaign");
            }

            @Override
            protected void addRightButtons(@NotNull RepeatingView rightButtonsView) {
                addCampaignManagementButton(rightButtonsView);
                addCreateReportButton(rightButtonsView);
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

    private void addCampaignManagementButton(RepeatingView rightButtonsView) {
        LoadableDetachableModel<String> buttonLabelModel = getActionButtonTitleModel();
        LoadableDetachableModel<String> buttonCssModel = getActionButtonCssModel();
        AjaxIconButton button = new AjaxIconButton(rightButtonsView.newChildId(), buttonCssModel, buttonLabelModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                AccessCertificationCampaignType campaign = getModelObjectType();

                getObjectDetailsModels().reloadPrismObjectModel();
                PageCertCampaign.this.getModel().detach();
                buttonCssModel.detach();
                buttonLabelModel.detach();

                CampaignProcessingHelper.campaignActionPerformed(campaign, PageCertCampaign.this, ajaxRequestTarget);
            }
        };
        button.showTitleAsLabel(true);
        button.add(AttributeAppender.append("class", "btn btn-primary"));
        rightButtonsView.add(button);
    }

    private void addCreateReportButton(RepeatingView rightButtonsView) {
        AjaxIconButton button = new AjaxIconButton(rightButtonsView.newChildId(), Model.of("fa fa-chart-pie"),
                createStringResource("PageCertDecisions.button.createReport")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createReportPerformed(target);
            }
        };
        button.showTitleAsLabel(true);
        button.add(AttributeModifier.append("class", "btn btn-secondary"));
        rightButtonsView.add(button);
    }

    private void createReportPerformed(AjaxRequestTarget target) {
        SelectReportTemplatePanel selectReportTemplatePanel = new SelectReportTemplatePanel(getMainPopupBodyId()) {

            @Serial private static final long serialVersionUID = 1L;

            //todo hardcoded for now. initial object report for certification items is used.
            //therefore we create parameters for this report
            //should be refactored to use generated form for report parameters
            @Override
            protected PrismContainer<ReportParameterType> createParameters(PrismObject<ReportType> report) {

                PrismContainerValue<ReportParameterType> reportParamValue;
                @NotNull PrismContainer<ReportParameterType> parameterContainer;
                try {
                    PrismContainerDefinition<ReportParameterType> paramContainerDef = getPrismContext().getSchemaRegistry()
                            .findContainerDefinitionByElementName(ReportConstants.REPORT_PARAMS_PROPERTY_NAME);
                    parameterContainer = paramContainerDef.instantiate();

                    ReportParameterType reportParam = new ReportParameterType();

                    AccessCertificationCampaignType campaign = getModelObjectType();
                    String campaignOid = campaign.getOid();
                    ObjectReferenceType campaignRef = new ObjectReferenceType()
                            .oid(campaignOid)
                            .type(AccessCertificationCampaignType.COMPLEX_TYPE);
                    ReportParameterTypeUtil.addParameter(reportParam, "campaignRef", campaignRef);

                    //todo default values (all stages and all iterations) are used for now
//                    if (isCertItemsReport(report)) {
//                        int stageNumber = campaign.getStageNumber();
//                        ReportParameterTypeUtil.addParameter(reportParam, "stageNumber", stageNumber);
//
//                        int iteration = campaign.getIteration();
//                        ReportParameterTypeUtil.addParameter(reportParam, "iteration", iteration);
//                    }
                    reportParamValue = reportParam.asPrismContainerValue();
                    reportParamValue.revive(getPrismContext());
                    parameterContainer.add(reportParamValue);
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create container for report parameters");
                    return null;
                }
                return parameterContainer;
            }

            private boolean isCertItemsReport(PrismObject<ReportType> report) {
                return report != null && report.asObjectable().getObjectCollection() != null
                        && report.asObjectable().getObjectCollection().getView() != null
                        && AccessCertificationWorkItemType.COMPLEX_TYPE.equals(report.asObjectable().getObjectCollection().getView().getType());
            }

        };
        showMainPopup(selectReportTemplatePanel, target);
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
