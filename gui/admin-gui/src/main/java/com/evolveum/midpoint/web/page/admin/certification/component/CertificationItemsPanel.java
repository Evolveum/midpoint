/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.IconComponent;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.action.*;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.CertMiscUtil;
import com.evolveum.midpoint.web.page.admin.certification.helpers.AvailableResponses;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.web.session.CertDecisionsStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;

public class CertificationItemsPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(CertificationItemsPanel.class);
    private static final String DOT_CLASS = CertificationItemsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CAMPAIGN = DOT_CLASS + "loadCampaign";
    private static final String OPERATION_LOAD_REPORT = DOT_CLASS + "loadCertItemsReport";
    private static final String OPERATION_RUN_REPORT = DOT_CLASS + "runCertItemsReport";
    private static final String OPERATION_LOAD_ACCESS_CERT_DEFINITION = DOT_CLASS + "loadAccessCertificationDefinition";
    private static final String OPERATION_RECORD_COMMENT = DOT_CLASS + "recordComment";

    private static final String ID_NAVIGATION_PANEL = "navigationPanel";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_DECISIONS_TABLE = "decisionsTable";

    IModel<AccessCertificationCampaignType> campaignModel;

    public CertificationItemsPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initCampaignModel();
        initLayout();
    }

    private void initCampaignModel() {
        campaignModel = initSingleCampaignModel();
    }

    private void initLayout() {
        initNavigationPanel();

        initTablePanel();
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
                CertificationItemsPanel.this.onBackPerformed(target);
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

    private void initTablePanel() {
        MidpointForm mainForm = new MidpointForm(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        ContainerableListPanel<AccessCertificationWorkItemType, PrismContainerValueWrapper<AccessCertificationWorkItemType>> table =
                new ContainerableListPanel<>(ID_DECISIONS_TABLE, AccessCertificationWorkItemType.class) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected List<IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String>> createDefaultColumns() {
                        return createColumns();
                    }

                    @Override
                    protected ISelectableDataProvider<PrismContainerValueWrapper<AccessCertificationWorkItemType>> createProvider() {
                        return CertificationItemsPanel.this.createProvider(getSearchModel());
                    }

                    @Override
                    public List<AccessCertificationWorkItemType> getSelectedRealObjects() {
                        List<PrismContainerValueWrapper<AccessCertificationWorkItemType>> selectedObjects = getSelectedObjects();
                        return selectedObjects.stream().map(PrismValueWrapper::getRealValue).collect(Collectors.toList());
                    }

                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return UserProfileStorage.TableId.PAGE_CERT_DECISIONS_PANEL;
                    }

                    @Override
                    protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createCheckboxColumn() {
                        if (!isPreview()) {
                            return new CheckBoxHeaderColumn<>();
                        }
                        return null;
                    }

                    @Override
                    protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createIconColumn() {
                        return new IconColumn<>(Model.of("")) {

                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                                return GuiDisplayTypeUtil.createDisplayType(
                                        IconAndStylesUtil.createDefaultBlackIcon(AccessCertificationWorkItemType.COMPLEX_TYPE));
                            }

                        };
                    }

                    private List<IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String>> createColumns() {
                        return ColumnUtils.getDefaultCertWorkItemColumns(!isMyCertItems(), showOnlyNotDecidedItems());
                    }

                    private List<AbstractGuiAction<AccessCertificationWorkItemType>> getCertItemActions() {
                        List<AccessCertificationResponseType> availableResponses = new AvailableResponses(getPageBase()).getResponseValues();   //from sys config
                        if (CollectionUtils.isEmpty(availableResponses)) {
                            availableResponses = Arrays.stream(values()).filter(r -> r != DELEGATE).collect(Collectors.toList());
                        }
                        List<GuiActionType> actions = getCertItemsViewActions();
                        return CertMiscUtil.mergeCertItemsResponses(availableResponses, actions, getPageBase());
                    }

                    private List<GuiActionType> getCertItemsViewActions() {
                        CompiledObjectCollectionView collectionView = getObjectCollectionView();
                        return collectionView == null ? new ArrayList<>() : collectionView.getActions();
                    }

                    @Override
                    public CompiledObjectCollectionView getObjectCollectionView() {
                        return loadCampaignView();
                     }

                     @Override
                     protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createActionsColumn() {
                         List<AbstractGuiAction<AccessCertificationWorkItemType>> actions = getCertItemActions();
                         if (CollectionUtils.isNotEmpty(actions)) {
                             return new GuiActionColumn<>(actions, getPageBase()) {
                                 @Serial private static final long serialVersionUID = 1L;

                                 @Override
                                 protected AccessCertificationWorkItemType unwrapRowModelObject(
                                         PrismContainerValueWrapper<AccessCertificationWorkItemType> rowModelObject) {
                                     return rowModelObject.getRealValue();
                                 }

                                 @Override
                                 protected List<AccessCertificationWorkItemType> getSelectedItems() {
                                     return getSelectedRealObjects();
                                 }
                             };
                         }
                         return null;
                     }

                     @Override
                     protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createCustomExportableColumn(
                            IModel<String> displayModel, GuiObjectColumnType guiObjectColumn, ExpressionType expression) {
                        ItemPath path = WebComponentUtil.getPath(guiObjectColumn);

                        if (ItemPath.create(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_COMMENT)
                                .equivalent(path)) {
                            String propertyExpression = "realValue" + "." + AccessCertificationWorkItemType.F_OUTPUT.getLocalPart() + "."
                                    + AbstractWorkItemOutputType.F_COMMENT.getLocalPart();
                            return new DirectlyEditablePropertyColumn<>(
                                    createStringResource("PageCertDecisions.table.comment"), propertyExpression) {
                                @Serial private static final long serialVersionUID = 1L;

                                @Override
                                public void onBlur(AjaxRequestTarget target,
                                        IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> model) {
                                    recordCommentPerformed(target, model.getObject());
                                }
                            };
                        }
                        return super.createCustomExportableColumn(displayModel, guiObjectColumn, expression);
                    }
                };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    public CertDecisionsStorage getPageStorage() {
        return null;
    }

    private ContainerListDataProvider<AccessCertificationWorkItemType> createProvider(IModel<Search<AccessCertificationWorkItemType>> searchModel) {
        Collection<SelectorOptions<GetOperationOptions>> options = CertificationItemsPanel.this.getPageBase()
                .getOperationOptionsBuilder()
                .resolveNames()
                .build();
        ContainerListDataProvider<AccessCertificationWorkItemType> provider = new ContainerListDataProvider<>(this,
                searchModel, options) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return null;
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getOpenCertWorkItemsQuery();
            }

        };
//        provider.setSort(CaseWorkItemType.F_DEADLINE.getLocalPart(), SortOrder.DESCENDING);
        return provider;
    }


    protected ObjectQuery getOpenCertWorkItemsQuery() {
        ObjectQuery query;
        if (StringUtils.isNotEmpty(getCampaignOid())) {
            query = QueryUtils.createQueryForOpenWorkItemsForCampaigns(Collections.singletonList(getCampaignOid()),
                    getPageBase().getPrincipal(), false);
        } else {
            query = PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                    .build();
        }
        MidPointPrincipal principal = null;
        if (isMyCertItems()) {
            principal = getPageBase().getPrincipal();
        }
        return QueryUtils.createQueryForOpenWorkItems(query, principal, false);
    }

    protected boolean isMyCertItems() {
        return true;
    }

    protected boolean showOnlyNotDecidedItems() {
        return false;
    }


    private IModel<String> getNavigationPanelTitleModel() {
        String campaignName = getCampaignName();
        return createStringResource("PageMyCertCampaigns.title",
                StringUtils.isNotEmpty(campaignName) ? "/" + campaignName : "");
    }

    private String getCampaignName() {
        ObjectReferenceType campaignRef =
                new ObjectReferenceType()
                        .oid(getCampaignOid())
                        .type(AccessCertificationCampaignType.COMPLEX_TYPE);
        return WebModelServiceUtils.resolveReferenceName(campaignRef, getPageBase(), true);
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
                AccessCertificationCampaignType campaign = loadCampaign(getCampaignOid());

                DetailsTableItem chartPanelItem = new DetailsTableItem(getCompletedItemsPercentageModel(campaignsOids)) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        ChartConfiguration config = CertMiscUtil.createDoughnutChartConfigForCampaigns(campaignsOids,
                                principal, getPageBase());
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

                DetailsTableItem startDateItem = new DetailsTableItem(getCampaignStartDateModel(campaign)) {
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
                        return new VisibleBehaviour(CertificationItemsPanel.this::isSingleCampaignView);
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
                                return campaign != null ? CampaignProcessingHelper.computeDeadline(
                                        campaign, getPageBase()) : null;
                            }
                        });
                        deadlinePanel.add(AttributeAppender.append("title",
                                createStringResource("PageCertDecisions.table.deadline")));
                        return deadlinePanel;
                    }

                    @Override
                    public VisibleBehaviour isVisible() {
                        return new VisibleBehaviour(CertificationItemsPanel.this::isSingleCampaignView);
                    }
                };
                items.add(deadlineItem);

                DetailsTableItem stageItem = new DetailsTableItem(getStageModel(campaign)) {
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
                        return new VisibleBehaviour(CertificationItemsPanel.this::isSingleCampaignView);
                    }
                };
                stageItem.setValueComponentBeforeLabel(true);
                items.add(stageItem);

                DetailsTableItem iterationItem = new DetailsTableItem(getIterationLabelModel(campaign)) {
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
                        return new VisibleBehaviour(CertificationItemsPanel.this::isSingleCampaignView);
                    }
                };
                iterationItem.setValueComponentBeforeLabel(true);
                items.add(iterationItem);

                return items;
            }

            private LoadableModel<String> getStageModel(AccessCertificationCampaignType campaign) {
                return CertMiscUtil.getCampaignStageLoadableModel(campaign);
            }

            private LoadableDetachableModel<String> getCompletedItemsPercentageModel(List<String> campaignsOids) {
                return new LoadableDetachableModel<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected String load() {
                        float openNotDecidedItemCount = CertMiscUtil.countOpenCertItems(campaignsOids,
                                getPrincipalAsReviewer(), true, getPageBase());
                        float allOpenItemCount = CertMiscUtil.countOpenCertItems(campaignsOids,
                                getPrincipalAsReviewer(), false, getPageBase());
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
        if (StringUtils.isNotEmpty(getCampaignOid())) {
            return Collections.singletonList(getCampaignOid());
        } else {
            return CertMiscUtil.getActiveCampaignsOids(!isDisplayingAllItems(), getPageBase());
        }
    }

    private LoadableDetachableModel<AccessCertificationCampaignType> initSingleCampaignModel() {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected AccessCertificationCampaignType load() {
                if (StringUtils.isEmpty(getCampaignOid())) {
                    List<String> campaignsOids = getCampaignOidsList();
                    if (campaignsOids.size() == 1) {
                        return loadCampaign(campaignsOids.get(0));
                    }
                }
                if (StringUtils.isEmpty(getCampaignOid())) {
                    return null;
                }
                return loadCampaign(getCampaignOid());
            }
        };
    }

    private AccessCertificationCampaignType loadCampaign(String campaignToLoad) {
        if (StringUtils.isEmpty(campaignToLoad)) {
            return null;
        }
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_CAMPAIGN);
        PrismObject<AccessCertificationCampaignType> campaign = WebModelServiceUtils.loadObject(
                AccessCertificationCampaignType.class, campaignToLoad, getPageBase(), task, task.getResult());
        if (campaign != null) {
            return campaign.asObjectable();
        }
        return null;
    }

    private MidPointPrincipal getPrincipalAsReviewer() {
        return isDisplayingAllItems() ? null : getPageBase().getPrincipal();
    }

    private boolean isSingleCampaignView() {
        return StringUtils.isNotEmpty(getCampaignOid()) || getCampaignOidsList().size() == 1;
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
        Task task = getPageBase().createSimpleTask(OPERATION_RUN_REPORT);
        OperationResult result = task.getResult();

        try {
            PrismObject<ReportType> report = loadCertItemsReport();
            getPageBase().getReportManager().runReport(report, null, task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }

    private PrismObject<ReportType> loadCertItemsReport() {
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_REPORT);
        OperationResult result = task.getResult();
        PrismObject<ReportType> report = null;
        try {
            String certItemsReportOid = "00000000-0000-0000-0000-000000000160";
            report = getPageBase().getModelService().getObject(ReportType.class, certItemsReportOid, null, task, result);
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

    protected String getCampaignOid() {
        return null;
    }

    protected void onBackPerformed(AjaxRequestTarget target) {
        getPageBase().redirectBack();
    }

    private CompiledObjectCollectionView loadCampaignView() {
        AccessCertificationCampaignType campaign = campaignModel.getObject();
        if (campaign == null) {
            return null;
        }
        var definitionRef = campaign.getDefinitionRef();
        if (definitionRef == null) {
            return null;
        }
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ACCESS_CERT_DEFINITION);
        OperationResult result = task.getResult();
        var definitionObj = WebModelServiceUtils.loadObject(definitionRef, getPageBase(), task, result);
        if (definitionObj == null) {
            return null;
        }
        AccessCertificationDefinitionType definition = (AccessCertificationDefinitionType) definitionObj.asObjectable();
        GuiObjectListViewType view = definition.getView();
        return WebComponentUtil.getCompiledObjectCollectionView(view, new ContainerPanelConfigurationType(), getPageBase());
    }

    private void recordCommentPerformed(AjaxRequestTarget target, PrismContainerValueWrapper<AccessCertificationWorkItemType> certItemWrapper) {
        if (certItemWrapper == null) {
            return;
        }
        OperationResult result = new OperationResult(OPERATION_RECORD_COMMENT);
        try {
            AccessCertificationWorkItemType certItem = certItemWrapper.getRealValue();
            if (certItem == null) {
                return;
            }
            //todo check if comment was really changed
//            Collection<ItemDelta> containerDelta = certItemWrapper.getDeltas();
//            if (containerDelta == null || containerDelta.isEmpty()) {
//                return;
//            }
            Task task = getPageBase().createSimpleTask(OPERATION_RECORD_COMMENT);
            String comment = certItem.getOutput() != null ? certItem.getOutput().getComment() : null;
            CertMiscUtil.recordCertItemResponse(
                    certItem, null, comment, result, task, getPageBase());

        } catch (Exception ex) {
            LOGGER.error("Couldn't record comment for certification work item", ex);
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!result.isSuccess()) {
            showResult(result);
        }
        target.add(this);
    }

}
