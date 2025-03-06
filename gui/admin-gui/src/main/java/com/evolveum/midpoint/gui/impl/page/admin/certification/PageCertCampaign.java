/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification;

import java.io.Serial;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.CampaignActionButton;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.CertCampaignSummaryPanel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.SelectReportTemplatePanel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CampaignStateHelper;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ReportParameterTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    private static final String OPERATION_LOAD_RUNNING_TASK = DOT_CLASS + "loadRunningTask";

    private LoadableDetachableModel<String> buttonLabelModel;
    private LoadableDetachableModel<AccessCertificationCampaignType> campaignModel;

    private String runningTaskOid;

    public PageCertCampaign() {
        this(new PageParameters());
    }

    public PageCertCampaign(PageParameters parameters) {
        super(parameters);
        initRunningTaskOid();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    protected void initLayout() {
        initCampaignModel();
        super.initLayout();
    }

    private void initRunningTaskOid() {
        String campaignOid = getModelObjectType().getOid();
        OperationResult result = new OperationResult(OPERATION_LOAD_RUNNING_TASK);
        List<PrismObject<TaskType>> tasks = CertMiscUtil.loadRunningCertTask(campaignOid, result, PageCertCampaign.this);
        if (!tasks.isEmpty()) {
            runningTaskOid = tasks.get(0).getOid();
        }
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
                addRunningTaskButton(rightButtonsView);
                addCreateReportButton(rightButtonsView);
                addReloadButton(rightButtonsView);
                addCampaignManagementButton(rightButtonsView);
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
        buttonLabelModel = getActionButtonTitleModel();

        CampaignActionButton actionButton = new CampaignActionButton(rightButtonsView.newChildId(), PageCertCampaign.this,
                campaignModel, buttonLabelModel, runningTaskOid) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void refresh(AjaxRequestTarget target) {
                runningTaskOid = getRunningTaskOid();
                PageCertCampaign.this.refresh(target, !isEmptyTaskOid());
                buttonLabelModel.detach();
            }

//            @Override
//            protected boolean isEmptyTaskOid() {
//                return StringUtils.isEmpty(runningTaskOid);
//            }

        };
        actionButton.setOutputMarkupPlaceholderTag(true);
        actionButton.add(AttributeModifier.append("class", "btn btn-primary"));
//        actionButton.add(new EnableBehaviour(() -> StringUtils.isEmpty(runningTaskOid)));
        rightButtonsView.add(actionButton);
    }

    private void initCampaignModel() {
        campaignModel = new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected AccessCertificationCampaignType load() {
                return getModelPrismObject().asObjectable();
            }
        };
    }

    private void addRunningTaskButton(RepeatingView rightButtonsView) {
        AjaxIconButton button = new AjaxIconButton(rightButtonsView.newChildId(), Model.of("fa fa-tasks"),
                createStringResource("PageCertCampaign.button.showRunningTask")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(runningTaskOid, ObjectTypes.TASK);
                DetailsPageUtil.dispatchToObjectDetailsPage(ref, PageCertCampaign.this, false);
            }
        };
        button.showTitleAsLabel(true);
        button.add(AttributeModifier.append("class", "btn btn-default"));
        button.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(runningTaskOid)));
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
        button.add(AttributeModifier.append("class", "btn btn-default"));
        rightButtonsView.add(button);
    }

    private void addReloadButton(RepeatingView rightButtonsView) {
        AjaxIconButton button = new AjaxIconButton(rightButtonsView.newChildId(), Model.of("fa fa-sync-alt"),
                createStringResource("ReloadableButton.reload")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                target.add(PageCertCampaign.this);
            }
        };
        button.showTitleAsLabel(true);
        button.add(AttributeModifier.append("class", "btn btn-default"));
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

    @Override
    protected void onDetach() {
        getObjectDetailsModels().reset();
        if (buttonLabelModel != null) {
            buttonLabelModel.detach();
        }
        if (campaignModel != null) {
            campaignModel.detach();
        }
        super.onDetach();
    }

    public void refresh(AjaxRequestTarget target, boolean soft) {
        getObjectDetailsModels().reset();
        campaignModel.detach();
        if (getSummaryPanel() != null) {
            target.add(getSummaryPanel());
        }
        if (getOperationalButtonsPanel() != null) {
            target.add(getOperationalButtonsPanel());
        }
        if (getFeedbackPanel() != null) {
            target.add(getFeedbackPanel());
        }
        if (get(ID_DETAILS_VIEW) != null) {
            target.add(get(ID_DETAILS_VIEW));
        }
        refreshTitle(target);
        replacePanel(findDefaultConfiguration(), target);
    }
}
