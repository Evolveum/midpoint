/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.IconComponent;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ReportParameterTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.CertDecisionsStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

public class CertificationItemsPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(CertificationItemsPanel.class);
    private static final String DOT_CLASS = CertificationItemsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CAMPAIGN = DOT_CLASS + "loadCampaign";


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
            protected @NotNull VisibleEnableBehaviour getNextVisibilityBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

//            @Override
//            protected Component createNextButton(String id, IModel<String> nextTitle) {
//                AjaxIconButton button = new AjaxIconButton(id, Model.of("fa fa-chart-pie"),
//                        createStringResource("PageCertDecisions.button.createReport")) {
//                    @Serial private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                        onNextPerformed(target);
//                    }
//                };
//                button.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getCampaignOid())));
//                button.showTitleAsLabel(true);
//                button.add(AttributeModifier.append("class", "btn btn-secondary"));
//                return button;
//            }

//            @Override
//            protected void onNextPerformed(AjaxRequestTarget target) {
//                runCertItemsReport(target);
//            }

        };
        add(navigationPanel);
    }

    private void initTablePanel() {
        MidpointForm<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        CertificationWorkItemTable table = new CertificationWorkItemTable(ID_DECISIONS_TABLE) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isMyCertItems() {
                return CertificationItemsPanel.this.isMyCertItems();
            }

            @Override
            protected boolean showOnlyNotDecidedItems() {
                return CertificationItemsPanel.this.showOnlyNotDecidedItems();
            }

            @Override
            protected String getCampaignOid() {
                return CertificationItemsPanel.this.getCampaignOid();
            }

        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    public CertDecisionsStorage getPageStorage() {
        return null;
    }


    protected boolean isMyCertItems() {
        return false;
    }

    protected boolean showOnlyNotDecidedItems() {
        return true;
    }


    private IModel<String> getNavigationPanelTitleModel() {
        return () -> getCampaignName();
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
                        DeadlinePanel deadlinePanel = new DeadlinePanel(id, new LoadableModel<>() {
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

            private LoadableDetachableModel<String> getStageModel(AccessCertificationCampaignType campaign) {
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
            XMLGregorianCalendar startDate = campaign.getStartTimestamp();
            return WebComponentUtil.getLocalizedDate(startDate, DateLabelComponent.SHORT_NOTIME_STYLE);
        };
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

}
