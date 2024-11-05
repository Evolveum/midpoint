/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.IconComponent;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.certification.PageAdminCertification;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.*;
import com.evolveum.midpoint.web.component.data.MultiButtonPanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn.ButtonColorClass;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.CertificationItemsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.DeadlinePanel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.HorizontalCampaignDetailsPanel;
import com.evolveum.midpoint.web.page.admin.certification.dto.*;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.AvailableResponses;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.CertDecisionsStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.wicket.chartjs.ChartConfiguration;

import com.evolveum.wicket.chartjs.ChartJsPanel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.web.page.admin.certification.CertDecisionHelper.WhichObject.OBJECT;
import static com.evolveum.midpoint.web.page.admin.certification.CertDecisionHelper.WhichObject.TARGET;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/decisionsAllOld", matchUrlForSecurity = "/admin/certification/decisionsAll")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_DECISIONS,
                        label = PageAdminCertification.AUTH_CERTIFICATION_DECISIONS_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_DECISIONS_DESCRIPTION)})
@CollectionInstance(identifier = "allCertDecisions", applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.certification.decisions", singularLabel = ""))
public class PageCertDecisions extends PageAdminCertification {

    private static final Trace LOGGER = TraceManager.getTrace(PageCertDecisions.class);

    private static final String DOT_CLASS = PageCertDecisions.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordAction";
    private static final String OPERATION_RECORD_ACTION_SELECTED = DOT_CLASS + "recordActionSelected";
    private static final String OPERATION_LOAD_CAMPAIGN = DOT_CLASS + "loadCampaign";
    private static final String OPERATION_LOAD_REPORT = DOT_CLASS + "loadCertItemsReport";
    private static final String OPERATION_RUN_REPORT = DOT_CLASS + "runCertItemsReport";
    private static final String OPERATION_LOAD_ACCESS_CERT_DEFINITION = DOT_CLASS + "loadAccessCertificationDefinition";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_DECISIONS_TABLE = "decisionsTable";
    private static final String ID_NAVIGATION_PANEL = "navigationPanel";

    public static final String CAMPAIGN_OID_PARAMETER = "campaignOid";

    private final CertDecisionHelper helper = new CertDecisionHelper();

    //model is initialized with a campaign object in case when campaign oid
    // comes as a parameter or in case the user has only one active campaign
    LoadableDetachableModel<AccessCertificationCampaignType> singleCampaignModel;

    public PageCertDecisions() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        singleCampaignModel = initSingleCampaignModel();
    }

    private void initLayout() {
        initNavigationPanel();

        Form<?> mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);
        CertificationItemsPanel table = new CertificationItemsPanel(ID_DECISIONS_TABLE) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isMyCertItems() {
                return !isDisplayingAllItems();
            }

            @Override
            protected String getCampaignOid() {
                return getCampaignOidParameter();
            }

        };
        table.setOutputMarkupId(true);
        mainForm.add(table);

        // adding this on outer feedback panel prevents displaying the error messages
        //addVisibleOnWarningBehavior(getMainFeedbackPanel());
        //addVisibleOnWarningBehavior(getTempFeedbackPanel());
    }

    private ContainerPanelConfigurationType createPanelConfig() {
        ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
        config.setListView(loadCampaignView());
        return config;
    }

    private GuiObjectListViewType loadCampaignView() {
        AccessCertificationCampaignType campaign = singleCampaignModel.getObject();
        if (campaign == null) {
            return null;
        }
        var definitionRef = campaign.getDefinitionRef();
        if (definitionRef == null) {
            return null;
        }
        Task task = createSimpleTask(OPERATION_LOAD_ACCESS_CERT_DEFINITION);
        OperationResult result = task.getResult();
        var definitionObj = WebModelServiceUtils.loadObject(definitionRef, PageCertDecisions.this, task, result);
        if (definitionObj == null) {
            return null;
        }
        AccessCertificationDefinitionType definition = (AccessCertificationDefinitionType) definitionObj.asObjectable();
        return definition.getView();
    }

    private void initNavigationPanel() {
        NavigationPanel navigationPanel = new NavigationPanel(ID_NAVIGATION_PANEL) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createTitleModel() {
                return getNavigationPanelTitleModel();
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageCertDecisions.this.redirectBack();
            }

            @Override
            protected Component createHeaderContent(String id) {
                HorizontalCampaignDetailsPanel detailsPanel = new HorizontalCampaignDetailsPanel(id, createCampaignDetailsModel()) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onComponentTag(ComponentTag tag) {
                        tag.setName("div");
                        super.onComponentTag(tag);
                    }
                };
                detailsPanel.setOutputMarkupId(true);
                detailsPanel.add(AttributeAppender.append("class", "d-flex justify-content-end"));
                return detailsPanel;
            }

            @Override
            protected Component createNextButton(String id, IModel<String> nextTitle) {
                AjaxIconButton button = new AjaxIconButton(id, Model.of("fa fa-chart-pie"),
                        createStringResource("PageCertDecisions.button.createReport")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onNextPerformed(target);
                    }
                };
                button.showTitleAsLabel(true);
                button.add(AttributeModifier.append("class", "btn btn-secondary"));
                return button;
            }

            @Override
            protected void onNextPerformed(AjaxRequestTarget target) {
                runCertItemsReport(target);
            }

        };
        add(navigationPanel);
    }

    protected String getCampaignOidParameter() {
        PageParameters pageParameters = getPageParameters();
        if (pageParameters != null && pageParameters.get(CAMPAIGN_OID_PARAMETER) != null) {
            return pageParameters.get(CAMPAIGN_OID_PARAMETER).toString();
        }
        return null;
    }

    private List<IColumn<CertWorkItemDto, String>> initColumns() {
        List<IColumn<CertWorkItemDto, String>> columns = new ArrayList<>();

        IColumn<CertWorkItemDto, String> column;

        column = new CheckBoxHeaderColumn<>();
        columns.add(column);

        column = helper.createTypeColumn(OBJECT, this);
        columns.add(column);

        column = helper.createObjectNameColumn(this, "PageCertDecisions.table.objectName");
        columns.add(column);

        column = helper.createTypeColumn(TARGET, this);
        columns.add(column);

        column = helper.createTargetNameColumn(this, "PageCertDecisions.table.targetName");
        columns.add(column);

        if (isDisplayingAllItems()) {
            column = helper.createReviewerNameColumn(this, "PageCertDecisions.table.reviewer");
            columns.add(column);
        }

        column = helper.createDetailedInfoColumn(this);
        columns.add(column);

        column = helper.createConflictingNameColumn(this, "PageCertDecisions.table.conflictingTargetName");
        columns.add(column);

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CERTIFICATION_ALL_URL,
                AuthorizationConstants.AUTZ_UI_CERTIFICATION_CAMPAIGN_URL)) {

            column = new AjaxLinkColumn<CertWorkItemDto>(
                    createStringResource("PageCertDecisions.table.campaignName"),
                    SearchingUtils.CAMPAIGN_NAME, CertWorkItemDto.F_CAMPAIGN_NAME) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId, IModel<CertWorkItemDto> rowModel) {
                    super.populateItem(item, componentId, rowModel);
                    AccessCertificationCampaignType campaign = rowModel.getObject().getCampaign();
                    if (campaign != null && campaign.getDescription() != null) {
                        item.add(AttributeModifier.replace("title", campaign.getDescription()));
                        item.add(new TooltipBehavior());
                    }
                }

                @Override
                public void onClick(AjaxRequestTarget target, IModel<CertWorkItemDto> rowModel) {
                    CertWorkItemDto dto = rowModel.getObject();
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, dto.getCampaignRef().getOid());
                    navigateToNext(PageCertCampaign.class, parameters);
                }
            };
        } else {
            column = new AbstractColumn<CertWorkItemDto, String>(createStringResource("PageCertDecisions.table.campaignName"), SearchingUtils.CAMPAIGN_NAME) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId,
                        final IModel<CertWorkItemDto> rowModel) {
                    item.add(new Label(componentId, new IModel<Object>() {
                        @Override
                        public Object getObject() {
                            return rowModel.getObject().getCampaignName();
                        }
                    }));
                }
            };
        }
        columns.add(column);

        column = new PropertyColumn<CertWorkItemDto, String>(createStringResource("PageCertDecisions.table.iteration"),
                CertCaseOrWorkItemDto.F_ITERATION) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return "countLabel";
            }

        };
        columns.add(column);

        column = new AbstractColumn<CertWorkItemDto, String>(
                createStringResource("PageCertDecisions.table.campaignStage")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId, final IModel<CertWorkItemDto> rowModel) {
                item.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        CertWorkItemDto dto = rowModel.getObject();
                        return dto.getCampaignStageNumber() + "/" + dto.getCampaignStageCount();
                    }
                }));
                String stageName = rowModel.getObject().getCurrentStageName();
                if (stageName != null) {
                    item.add(AttributeModifier.replace("title", stageName));
                    item.add(new TooltipBehavior());
                }
            }

            @Override
            public String getCssClass() {
                return "countLabel";
            }
        };
        columns.add(column);

        column = new AbstractColumn<CertWorkItemDto, String>(
                createStringResource("PageCertDecisions.table.escalation")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId, final IModel<CertWorkItemDto> rowModel) {
                item.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        CertWorkItemDto dto = rowModel.getObject();
                        Integer n = dto.getEscalationLevelNumber();
                        return n != null ? String.valueOf(n) : null;
                    }
                }));
                String info = rowModel.getObject().getEscalationLevelInfo();
                if (info != null) {
                    item.add(AttributeModifier.replace("title", info));
                    item.add(new TooltipBehavior());
                }
            }

            @Override
            public String getCssClass() {
                return "countLabel";
            }
        };
        columns.add(column);

        column = new PropertyColumn<CertWorkItemDto, String>(
                createStringResource("PageCertDecisions.table.requested"),
                SearchingUtils.CURRENT_REVIEW_REQUESTED_TIMESTAMP, CertWorkItemDto.F_REVIEW_REQUESTED) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId, IModel<CertWorkItemDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                CertWorkItemDto dto = rowModel.getObject();
                Date started = dto.getStageStarted();
                if (started != null) {
                    item.add(AttributeModifier.replace("title", WebComponentUtil.getShortDateTimeFormattedValue(started, PageCertDecisions.this)));
                    item.add(new TooltipBehavior());
                }
            }
        };
        columns.add(column);

        column = new PropertyColumn<CertWorkItemDto, String>(createStringResource("PageCertDecisions.table.deadline"),
                SearchingUtils.CURRENT_REVIEW_DEADLINE, CertWorkItemDto.F_DEADLINE_AS_STRING) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<CertWorkItemDto>> item, String componentId, final IModel<CertWorkItemDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                XMLGregorianCalendar deadline = rowModel.getObject().getCertCase().getCurrentStageDeadline();
                if (deadline != null) {
                    item.add(AttributeModifier.replace("title", WebComponentUtil.formatDate(deadline)));
                    item.add(new TooltipBehavior());
                }
            }
        };
        columns.add(column);

        final AvailableResponses availableResponses = new AvailableResponses(this);
        final int responses = availableResponses.getResponseKeys().size();

        column = new AbstractColumn<CertWorkItemDto, String>(new Model<>()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<CertWorkItemDto>> cellItem, String componentId,
                    IModel<CertWorkItemDto> rowModel) {

                cellItem.add(new MultiButtonPanel<CertWorkItemDto>(componentId, rowModel, responses + 1) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Component createButton(int index, String componentId, IModel<CertWorkItemDto> model) {
                        AjaxIconButton btn;
                        if (index < responses) {
                            btn = buildDefaultButton(componentId, null, new Model(availableResponses.getTitle(index)),
                                    new Model<>("btn btn-sm " + getDecisionButtonColor(model, availableResponses.getResponseValues().get(index))),
                                    target ->
                                            recordActionPerformed(target, model.getObject(), availableResponses.getResponseValues().get(index)));
                            btn.add(new EnableBehaviour(() -> !decisionEquals(model, availableResponses.getResponseValues().get(index))));
                        } else {
                            btn = buildDefaultButton(componentId, null, new Model(availableResponses.getTitle(index)),
                                    new Model<>("btn btn-sm " + ButtonColorClass.DANGER), null);
                            btn.setEnabled(false);
                            btn.add(new VisibleBehaviour(() -> !availableResponses.isAvailable(model.getObject().getResponse())));
                        }

                        return btn;
                    }
                });
            }
        };
        columns.add(column);

        column = new DirectlyEditablePropertyColumn<CertWorkItemDto>(
                createStringResource("PageCertDecisions.table.comment"),
                CertWorkItemDto.F_COMMENT) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onBlur(AjaxRequestTarget target, IModel model) {
                // TODO determine somehow if the model.comment was really changed
                recordActionPerformed(target, (CertWorkItemDto) model.getObject(), null);
            }
        };
        columns.add(column);

        columns.add(new InlineMenuHeaderColumn<>(createInlineMenu(availableResponses)));

        return columns;
    }

    private List<InlineMenuItem> createInlineMenu(AvailableResponses availableResponses) {
        List<InlineMenuItem> items = new ArrayList<>();
        if (availableResponses.isAvailable(ACCEPT)) {
            items.add(createMenu("PageCertDecisions.menu.acceptSelected", ACCEPT));
        }
        if (availableResponses.isAvailable(REVOKE)) {
            items.add(createMenu("PageCertDecisions.menu.revokeSelected", REVOKE));
        }
        if (availableResponses.isAvailable(REDUCE)) {
            items.add(createMenu("PageCertDecisions.menu.reduceSelected", REDUCE));
        }
        if (availableResponses.isAvailable(NOT_DECIDED)) {
            items.add(createMenu("PageCertDecisions.menu.notDecidedSelected", NOT_DECIDED));
        }
        if (availableResponses.isAvailable(NO_RESPONSE)) {
            items.add(createMenu("PageCertDecisions.menu.noResponseSelected", NO_RESPONSE));
        }
        return items;
    }

    private InlineMenuItem createMenu(String titleKey, final AccessCertificationResponseType response) {
        return new InlineMenuItem(createStringResource(titleKey)) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageCertDecisions.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        recordActionOnSelected(response, target);
                    }
                };
            }
        };
    }

    private String getDecisionButtonColor(IModel<CertWorkItemDto> model, AccessCertificationResponseType response) {
        if (decisionEquals(model, response)) {
            return ButtonColorClass.PRIMARY.toString();
        } else {
            return ButtonColorClass.DEFAULT.toString();
        }
    }

    private boolean decisionEquals(IModel<CertWorkItemDto> model, AccessCertificationResponseType response) {
        return model.getObject().getResponse() == response;
    }

    private Table getDecisionsTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_DECISIONS_TABLE));
    }
    //endregion

    //region Actions

    private void recordActionOnSelected(AccessCertificationResponseType response, AjaxRequestTarget target) {
        List<CertWorkItemDto> workItemDtoList = WebComponentUtil.getSelectedData(getDecisionsTable());
        if (workItemDtoList.isEmpty()) {
            warn(getString("PageCertDecisions.message.noItemSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION_SELECTED);
        Task task = createSimpleTask(OPERATION_RECORD_ACTION_SELECTED);
        for (CertWorkItemDto workItemDto : workItemDtoList) {
            OperationResult resultOne = result.createSubresult(OPERATION_RECORD_ACTION);
            try {
                getCertificationService().recordDecision(
                        workItemDto.getCampaignRef().getOid(),
                        workItemDto.getCaseId(), workItemDto.getWorkItemId(),
                        response, workItemDto.getComment(), task, resultOne);
            } catch (Exception ex) {
                resultOne.recordFatalError(ex);
            } finally {
                resultOne.computeStatusIfUnknown();
            }
        }
        result.computeStatus();

        if (!result.isSuccess()) {
            showResult(result);
        }
        target.add(getFeedbackPanel());
        target.add((Component) getDecisionsTable());
    }

    // if response is null this means keep the current one in workItemDto
    private void recordActionPerformed(AjaxRequestTarget target, CertWorkItemDto workItemDto, AccessCertificationResponseType response) {
        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION);
        try {
            Task task = createSimpleTask(OPERATION_RECORD_ACTION);
            if (response == null) {
                response = workItemDto.getResponse();
            }
            // TODO work item ID
            getCertificationService().recordDecision(
                    workItemDto.getCampaignRef().getOid(),
                    workItemDto.getCaseId(), workItemDto.getWorkItemId(),
                    response, workItemDto.getComment(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!result.isSuccess()) {
            showResult(result);
        }
//        resetCertWorkItemCountModel();
        if (response != null) {
            target.add(this);
        }
    }

    private CertDecisionsStorage getCertDecisionsStorage() {
        return getSessionStorage().getCertDecisions();
    }

    private IModel<String> getNavigationPanelTitleModel() {
        String campaignName = getCampaignName();
        return createStringResource("PageMyCertCampaigns.title",
                StringUtils.isNotEmpty(campaignName) ? "/" + campaignName : "");
    }

    private String getCampaignName() {
        ObjectReferenceType campaignRef =
                new ObjectReferenceType()
                        .oid(getCampaignOidParameter())
                        .type(AccessCertificationCampaignType.COMPLEX_TYPE);
        return WebModelServiceUtils.resolveReferenceName(campaignRef, PageCertDecisions.this, true);
    }

    private LoadableDetachableModel<List<DetailsTableItem>> createCampaignDetailsModel() {
        return new LoadableDetachableModel<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<DetailsTableItem> load() {
                return getCampaignDetails();
            }

            private List<DetailsTableItem> getCampaignDetails() {
                List<String> campaignsOids = getCampaignOidsList();
                MidPointPrincipal principal = getPrincipalAsReviewer();

                List<DetailsTableItem> items = new ArrayList<>();
                AccessCertificationCampaignType singleCampaign = singleCampaignModel.getObject();

                DetailsTableItem chartPanelItem = new DetailsTableItem(getCompletedItemsPercentageModel(campaignsOids)) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        ChartConfiguration config = CertMiscUtil.createDoughnutChartConfigForCampaigns(campaignsOids,
                                principal, PageCertDecisions.this);
                        ChartJsPanel<ChartConfiguration> chartPanel = new ChartJsPanel<>(id, Model.of(config)) {

                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected void onComponentTag(ComponentTag tag) {
                                tag.setName("canvas");
                                super.onComponentTag(tag);
                            }
                        };
                        chartPanel.setOutputMarkupId(true);
                        chartPanel.add(AttributeModifier.append("style", "margin-top: -5px !important;"));
                        chartPanel.add(AttributeAppender.append("title",
                                createStringResource("PageCertCampaign.table.completedItemsPercentage")));
                        return chartPanel;
                    }
                };
                chartPanelItem.setValueComponentBeforeLabel(true);
                items.add(chartPanelItem);

                DetailsTableItem startDateItem = new DetailsTableItem(getCampaignStartDateModel(singleCampaign)) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        IconComponent iconPanel = new IconComponent(id, Model.of(GuiStyleConstants.CLASS_TRIGGER_ICON)) {

                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public void onComponentTag(ComponentTag tag) {
                                tag.setName("i");
                                super.onComponentTag(tag);
                            }
                        };
                        iconPanel.add(AttributeAppender.append("title",
                                createStringResource("PageCertDecisions.campaignStartDate")));
                        return iconPanel;
                    }

                    @Override
                    public VisibleBehaviour isVisible() {
                        return new VisibleBehaviour(PageCertDecisions.this::isSingleCampaignView);
                    }

                };
                startDateItem.setValueComponentBeforeLabel(true);
                items.add(startDateItem);

                DetailsTableItem deadlineItem = new DetailsTableItem(createStringResource("")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        DeadlinePanel deadlinePanel = new DeadlinePanel(id, new LoadableModel<XMLGregorianCalendar>() {
                            @Override
                            protected XMLGregorianCalendar load() {
                                return singleCampaign != null ? CampaignProcessingHelper.computeDeadline(
                                        singleCampaign, PageCertDecisions.this) : null;
                            }
                        });
                        deadlinePanel.add(AttributeAppender.append("title",
                                createStringResource("PageCertDecisions.table.deadline")));
                        return deadlinePanel;
                    }

                    @Override
                    public VisibleBehaviour isVisible() {
                        return new VisibleBehaviour(PageCertDecisions.this::isSingleCampaignView);
                    }
                };
                items.add(deadlineItem);

                DetailsTableItem stageItem = new DetailsTableItem(getStageModel(singleCampaign)) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        IconComponent iconPanel = new IconComponent(id, Model.of("fa fa-circle-half-stroke mt-1 mr-1")) {

                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public void onComponentTag(ComponentTag tag) {
                                tag.setName("i");
                                super.onComponentTag(tag);
                            }
                        };
                        iconPanel.add(AttributeAppender.append("title",
                                createStringResource("PageCertCampaigns.table.stage")));
                        return iconPanel;
                    }

                    @Override
                    public VisibleBehaviour isVisible() {
                        return new VisibleBehaviour(PageCertDecisions.this::isSingleCampaignView);
                    }
                };
                stageItem.setValueComponentBeforeLabel(true);
                items.add(stageItem);

                DetailsTableItem iterationItem = new DetailsTableItem(getIterationLabelModel(singleCampaign)) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        IconComponent iconPanel = new IconComponent(id, Model.of("fa fa-rotate-right mt-1 mr-1")) {

                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public void onComponentTag(ComponentTag tag) {
                                tag.setName("i");
                                super.onComponentTag(tag);
                            }
                        };
                        iconPanel.add(AttributeAppender.append("title",
                                createStringResource("PageCertCampaign.iteration")));
                        return iconPanel;
                    }

                    @Override
                    public VisibleBehaviour isVisible() {
                        return new VisibleBehaviour(PageCertDecisions.this::isSingleCampaignView);
                    }
                };
                iterationItem.setValueComponentBeforeLabel(true);
                items.add(iterationItem);

                return items;
            }

            private LoadableDetachableModel<String> getStageModel(AccessCertificationCampaignType campaign) {
                return CertMiscUtil.getCampaignStageLoadableModel(campaign);
            }

            private LoadableDetachableModel<String> getCompletedItemsPercentageModel(List<String> campaignsOids) {
                return new LoadableDetachableModel<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected String load() {
                        float openNotDecidedItemCount = CertMiscUtil.countOpenCertItems(campaignsOids,
                                getPrincipalAsReviewer(), true, PageCertDecisions.this);
                        float allOpenItemCount = CertMiscUtil.countOpenCertItems(campaignsOids,
                                getPrincipalAsReviewer(), false, PageCertDecisions.this);
                        float decidedOpenItems = allOpenItemCount - openNotDecidedItemCount;
                        if (allOpenItemCount == 0 || decidedOpenItems == 0) {
                            return "0%";
                        }

                        float percentage = (decidedOpenItems / allOpenItemCount) * 100;
                        return String.format("%.0f%%", percentage);
                    }
                };
            }

            private IModel<String> getIterationLabelModel(AccessCertificationCampaignType campaign) {
                if (campaign == null) {
                    return () -> "";
                }
                return CertMiscUtil.getCampaignIterationLoadableModel(campaign);
            }
        };
    }

    private List<String> getCampaignOidsList() {
        if (StringUtils.isNotEmpty(getCampaignOidParameter())) {
            return Collections.singletonList(getCampaignOidParameter());
        } else {
            return CertMiscUtil.getActiveCampaignsOids(!isDisplayingAllItems(), PageCertDecisions.this);
        }
    }

    private LoadableDetachableModel<AccessCertificationCampaignType> initSingleCampaignModel() {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected AccessCertificationCampaignType load() {
                String campaignOid = getCampaignOidParameter();
                if (StringUtils.isEmpty(campaignOid)) {
                    List<String> campaignsOids = getCampaignOidsList();
                    if (campaignsOids.size() == 1) {
                        return loadCampaign(campaignsOids.get(0));
                    }
                }
                if (StringUtils.isEmpty(campaignOid)) {
                    return null;
                }
                return loadCampaign(campaignOid);
            }
        };
    }

    private AccessCertificationCampaignType loadCampaign(String campaignOid) {
        if (StringUtils.isEmpty(campaignOid)) {
            return null;
        }
        Task task = createSimpleTask(OPERATION_LOAD_CAMPAIGN);
        PrismObject<AccessCertificationCampaignType> campaign = WebModelServiceUtils.loadObject(
                AccessCertificationCampaignType.class, campaignOid, PageCertDecisions.this, task, task.getResult());
        if (campaign != null) {
            return campaign.asObjectable();
        }
        return null;
    }

    private MidPointPrincipal getPrincipalAsReviewer() {
        return isDisplayingAllItems() ? null : getPrincipal();
    }

    private boolean isSingleCampaignView() {
        return StringUtils.isNotEmpty(getCampaignOidParameter()) || getCampaignOidsList().size() == 1;
    }

    public IModel<String> getCampaignStartDateModel(AccessCertificationCampaignType campaign) {
        return () -> {
            if (campaign == null) {
                return "";
            }
            XMLGregorianCalendar startDate = campaign != null ? campaign.getStartTimestamp() : null;
            return WebComponentUtil.getLocalizedDate(startDate, DateLabelComponent.SHORT_NOTIME_STYLE);
        };
    }

    //todo set campaign oid, assigneeRef as parameters
    private void runCertItemsReport(AjaxRequestTarget target) {
        Task task = createSimpleTask(OPERATION_RUN_REPORT);
        OperationResult result = task.getResult();

        try {
            PrismObject<ReportType> report = loadCertItemsReport();
            getReportManager().runReport(report, null, task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }

    private PrismObject<ReportType> loadCertItemsReport() {
        Task task = createSimpleTask(OPERATION_LOAD_REPORT);
        OperationResult result = task.getResult();
        PrismObject<ReportType> report = null;
        try {
            String certItemsReportOid = "00000000-0000-0000-0000-000000000160";
            report = getModelService().getObject(ReportType.class, certItemsReportOid, null, task, result);
        } catch (Exception ex) {
            LOGGER.error("Couldn't load certification work items report", ex);
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }
        return report;
    }

    boolean isDisplayingAllItems() {
        return true;
    }
}
