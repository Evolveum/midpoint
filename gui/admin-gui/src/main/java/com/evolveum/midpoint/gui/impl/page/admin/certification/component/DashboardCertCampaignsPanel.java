/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.ObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.table.ChartedHeaderDto;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.CampaignSummary;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

@PanelType(name = "myCertificationItems")
public class DashboardCertCampaignsPanel extends ObjectListPanel<AccessCertificationCampaignType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(DashboardCertCampaignsPanel.class);

    private static final String DOT_CLASS = DashboardCertCampaignsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CAMPAIGNS_SUMMARY = DOT_CLASS + "loadCampaignsSummary";

    private IModel<List<CampaignSummary>> campaignsSummaryModel;

    public DashboardCertCampaignsPanel(String id) {
        super(id, AccessCertificationCampaignType.class);

        initModels();
    }

    public DashboardCertCampaignsPanel(String id, ContainerPanelConfigurationType configurationType) {
        super(id, AccessCertificationCampaignType.class, configurationType);

        initModels();
    }

    //    //todo cleanup; the same hack as for MyCaseWorkItemsPanel
    public DashboardCertCampaignsPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, AccessCertificationCampaignType.class, configurationType);

        initModels();
    }

    private void initModels() {
        // todo this model logic might be reusable outside of this panel to load campaign summaries
        this.campaignsSummaryModel = new LoadableModel<>(false) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<CampaignSummary> load() {
                LOGGER.debug("Loading active campaigns summary");

                PageBase page = getPageBase();

                Task task = page.createSimpleTask(OPERATION_LOAD_CAMPAIGNS_SUMMARY);
                OperationResult result = task.getResult();

                ObjectQuery query = CertMiscUtil.getPrincipalActiveCampaignsQuery(page);
                List<PrismObject<AccessCertificationCampaignType>> campaigns = WebModelServiceUtils.searchObjects(
                        AccessCertificationCampaignType.class, query, null, result, page);

                List<CampaignSummary> summary = new ArrayList<>();

                for (PrismObject<AccessCertificationCampaignType> campaignObj : campaigns) {
                    AccessCertificationCampaignType campaign = campaignObj.asObjectable();

                    ObjectQuery workItems = CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaign.getOid());

                    try {
                        int openNotDecided = page.getCertificationService().countOpenWorkItems(workItems, true,
                                false, null, task, task.getResult());
                        int allOpen = page.getCertificationService().countOpenWorkItems(workItems, false,
                                false, null, task, task.getResult());
                        int openDecided = allOpen - openNotDecided;

                        summary.add(new CampaignSummary(campaign, openDecided, openNotDecided));
                    } catch (CommonException ex) {
                        LOGGER.error("Couldn't load certification campaign work items count for campaign {}: {}",
                                campaign.getName(), ex.getMessage(), ex);
                    }
                }

                LOGGER.debug("Loaded {} active campaigns summary", summary.size());

                return summary;
            }
        };
    }

    @Override
    protected LoadableModel<ChartedHeaderDto<DoughnutChartConfiguration>> getChartedHeaderDtoModel() {
        return new LoadableModel<>(false) {

            @Override
            protected ChartedHeaderDto<DoughnutChartConfiguration> load() {
                LOGGER.debug("Loading charted header model");

                List<CampaignSummary> campaignSummaries = campaignsSummaryModel.getObject();
                DoughnutChartConfiguration chartConfig =
                        CertMiscUtil.createDoughnutChartConfigForCampaigns(campaignSummaries);

                long notDecidedCertItemsCount = campaignSummaries.stream()
                        .map(s -> s.openNotDecidedCount()).reduce(0L, Long::sum);

                return new ChartedHeaderDto<>(chartConfig,
                        createStringResource("MyCertificationItemsPanel.chartTitle").getString(),
                        String.valueOf(notDecidedCertItemsCount));
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return null;
    }

    @Override
    protected IColumn<SelectableBean<AccessCertificationCampaignType>, String> createIconColumn() {
        return null;
    }

    @Override
    protected final ISelectableDataProvider<SelectableBean<AccessCertificationCampaignType>> createProvider() {
        return createSelectableBeanObjectDataProvider(() -> CertMiscUtil.getPrincipalActiveCampaignsQuery(getPageBase()),
                null);
    }

    @Override
    protected boolean isPreview() {
        return true;
    }

    @Override
    protected List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> createDefaultColumns() {
        return ColumnUtils.getPreviewCampaignColumns(getPageBase(), campaignsSummaryModel);
    }

    @Override
    protected LoadableModel<PageParameters> getNavigationParametersModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected PageParameters load() {
                SelectableBeanObjectDataProvider<AccessCertificationCampaignType> provider =
                        (SelectableBeanObjectDataProvider) getDataProvider();
                if (provider != null && provider.getAvailableData().size() == 1) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, provider.getAvailableData().get(0).getValue().getOid());
                    return parameters;
                }
                return null;
            }
        };

    }

    @Override
    protected boolean isDataTableVisible() {
        return getDataProvider().size() > 0;
    }
}
