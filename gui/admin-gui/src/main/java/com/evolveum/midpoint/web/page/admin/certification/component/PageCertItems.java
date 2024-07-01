/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.IconComponent;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.CertMiscUtil;
import com.evolveum.midpoint.web.page.admin.certification.PageAdminCertification;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.web.session.CertDecisionsStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/decisionsAll", matchUrlForSecurity = "/admin/certification/decisionsAll")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_DECISIONS,
                        label = PageAdminCertification.AUTH_CERTIFICATION_DECISIONS_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_DECISIONS_DESCRIPTION)})
public class PageCertItems extends PageAdminCertification {

    @Serial private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PageCertItems.class);

    private static final String DOT_CLASS = PageCertItems.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordAction";
    private static final String OPERATION_RECORD_ACTION_SELECTED = DOT_CLASS + "recordActionSelected";
    private static final String OPERATION_LOAD_CAMPAIGN = DOT_CLASS + "loadCampaign";
    private static final String OPERATION_LOAD_REPORT = DOT_CLASS + "loadCertItemsReport";
    private static final String OPERATION_RUN_REPORT = DOT_CLASS + "runCertItemsReport";
    private static final String OPERATION_LOAD_ACCESS_CERT_DEFINITION = DOT_CLASS + "loadAccessCertificationDefinition";


    LoadableDetachableModel<AccessCertificationCampaignType> singleCampaignModel;
    private boolean isCampaignView = true;

    private static final String ID_CAMPAIGNS_PANEL = "campaignsPanel";
    private static final String ID_CERT_ITEMS_PANEL = "certItemsPanel";

    String campaignOid = null;

    public PageCertItems() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        singleCampaignModel = initSingleCampaignModel();

        initLayout();
    }

    private void initLayout() {
        ActiveCampaignsPanel campaignsPanel = new ActiveCampaignsPanel(ID_CAMPAIGNS_PANEL) {

            @Override
            protected void showCertItems(String campaignOid, AjaxRequestTarget target) {
                isCampaignView = false;
                PageCertItems.this.campaignOid = campaignOid;
                addOrReplaceCertItemsPanel();
                target.add(PageCertItems.this);
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
                return PageCertItems.this.getPrincipalAsReviewer();
            }

        };
        campaignsPanel.setOutputMarkupId(true);
        campaignsPanel.add(new VisibleBehaviour(() -> isCampaignView));
        add(campaignsPanel);

        addOrReplaceCertItemsPanel();
    }

    private void addOrReplaceCertItemsPanel() {
        CertificationItemsPanel table = new CertificationItemsPanel(ID_CERT_ITEMS_PANEL) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isMyCertItems() {
                return false;
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                isCampaignView = true;
                PageCertItems.this.campaignOid = null;
                target.add(PageCertItems.this);
            }

            @Override
            protected String getCampaignOid() {
                return campaignOid;
            }

        };
        table.add(new VisibleBehaviour(() -> !isCampaignView));
        table.setOutputMarkupId(true);
        addOrReplace(table);
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
        var definitionObj = WebModelServiceUtils.loadObject(definitionRef, PageCertItems.this, task, result);
        if (definitionObj == null) {
            return null;
        }
        AccessCertificationDefinitionType definition = (AccessCertificationDefinitionType) definitionObj.asObjectable();
        return definition.getView();
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
                        .oid(campaignOid)
                        .type(AccessCertificationCampaignType.COMPLEX_TYPE);
        return WebModelServiceUtils.resolveReferenceName(campaignRef, PageCertItems.this, true);
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
                                principal, PageCertItems.this);
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
                        return new VisibleBehaviour(PageCertItems.this::isSingleCampaignView);
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
                                        singleCampaign, PageCertItems.this) : null;
                            }
                        });
                        deadlinePanel.add(AttributeAppender.append("title",
                                createStringResource("PageCertDecisions.table.deadline")));
                        return deadlinePanel;
                    }

                    @Override
                    public VisibleBehaviour isVisible() {
                        return new VisibleBehaviour(PageCertItems.this::isSingleCampaignView);
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
                        return new VisibleBehaviour(PageCertItems.this::isSingleCampaignView);
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
                        return new VisibleBehaviour(PageCertItems.this::isSingleCampaignView);
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
                                getPrincipalAsReviewer(), true, PageCertItems.this);
                        float allOpenItemCount = CertMiscUtil.countOpenCertItems(campaignsOids,
                                getPrincipalAsReviewer(), false, PageCertItems.this);
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
        if (StringUtils.isNotEmpty(campaignOid)) {
            return Collections.singletonList(campaignOid);
        } else {
            return CertMiscUtil.getActiveCampaignsOids(!isDisplayingAllItems(), PageCertItems.this);
        }
    }

    private LoadableDetachableModel<AccessCertificationCampaignType> initSingleCampaignModel() {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected AccessCertificationCampaignType load() {
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
                AccessCertificationCampaignType.class, campaignOid, PageCertItems.this, task, task.getResult());
        if (campaign != null) {
            return campaign.asObjectable();
        }
        return null;
    }

    private MidPointPrincipal getPrincipalAsReviewer() {
        return isDisplayingAllItems() ? null : getPrincipal();
    }

    private boolean isSingleCampaignView() {
        return StringUtils.isNotEmpty(campaignOid) || getCampaignOidsList().size() == 1;
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
