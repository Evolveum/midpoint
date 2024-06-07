/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.gui.api.component.ObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.table.ChartedHeaderDto;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDecisions;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.wicket.chartjs.ChartData;
import com.evolveum.wicket.chartjs.ChartDataset;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serial;
import java.util.List;

@PanelType(name = "dashboardCertItems")
public class DashboardCertCampaignsPanel extends ObjectListPanel<AccessCertificationCampaignType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = DashboardCertCampaignsPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(DashboardCertCampaignsPanel.class);
    private static final String OPERATION_COUNT_CERTIFICATION_ITEMS = DOT_CLASS + "countCertItems";
    private static final String OPERATION_LOAD_CAMPAIGNS = DOT_CLASS + "loadCampaigns";

    public DashboardCertCampaignsPanel(String id) {
        super(id, AccessCertificationCampaignType.class);
    }

    public DashboardCertCampaignsPanel(String id, ContainerPanelConfigurationType configurationType) {
        super(id, AccessCertificationCampaignType.class, configurationType);
    }

//    //todo cleanup; the same hack as for MyCaseWorkItemsPanel
    public DashboardCertCampaignsPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, AccessCertificationCampaignType.class, configurationType);
    }

    @Override
    protected LoadableModel<ChartedHeaderDto<DoughnutChartConfiguration>> getChartedHeaderDtoModel() {
        return new LoadableModel<>() {
            @Override
            protected ChartedHeaderDto<DoughnutChartConfiguration> load() {
                DoughnutChartConfiguration config = new DoughnutChartConfiguration();

                ChartData chartData = new ChartData();
                chartData.addDataset(createDataSet());

                config.setData(chartData);

                long notDecidedCertItemsCount = getCertItemsCountFromAllCampaigns(true);
                return new ChartedHeaderDto<>(config,
                        createStringResource("MyCertificationItemsPanel.chartTitle").getString(),
                        String.valueOf(notDecidedCertItemsCount));
            }
        };
    }

    private ChartDataset createDataSet() {
        ChartDataset dataset = new ChartDataset();
//        dataset.setLabel("Not decided");

        dataset.setFill(true);

        long notDecidedCertItemsCount = getCertItemsCountFromAllCampaigns(true);
        long allOpenCertItemsCount = getCertItemsCountFromAllCampaigns(false);
        long decidedCertItemsCount = allOpenCertItemsCount - notDecidedCertItemsCount;

        dataset.addData(decidedCertItemsCount);
        dataset.addBackgroudColor("blue");

        dataset.addData(notDecidedCertItemsCount);
        dataset.addBackgroudColor("grey");

        return dataset;
    }

    private long getCertItemsCountFromAllCampaigns(boolean notDecidedOnly) {
        long count = 0;

        try {
            ObjectQuery campaignsQuery = getDashboardCampaignsQuery();
            OperationResult result = new OperationResult(OPERATION_LOAD_CAMPAIGNS);
            List<PrismObject<AccessCertificationCampaignType>> campaigns = WebModelServiceUtils.searchObjects(
                    AccessCertificationCampaignType.class, campaignsQuery, null, result, getPageBase());
            List<String> oidList = campaigns.stream().map(PrismObject::getOid).toList();
            if (CollectionUtils.isEmpty(oidList)) {
                return 0;
            }
            ObjectQuery query = QueryUtils.createQueryForOpenWorkItemsForCampaigns(oidList, getPageBase().getPrincipal(),
                    notDecidedOnly);
            Task task = getPageBase().createSimpleTask(OPERATION_COUNT_CERTIFICATION_ITEMS);
            count = getPageBase().getModelService()
                    .countContainers(AccessCertificationWorkItemType.class, query, null, task, task.getResult());
        } catch (Exception ex) {
            LOGGER.error("Couldn't count certification work items", ex);
        }
        return count;
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
        return createSelectableBeanObjectDataProvider(this::getDashboardCampaignsQuery, null);
    }

    private ObjectQuery getDashboardCampaignsQuery() {
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

    @Override
    protected boolean isPreview() {
        return true;
    }

    @Override
    protected List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> createDefaultColumns() {
        return ColumnUtils.getPreviewCampaignColumns(getPageBase());
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
                    parameters.add(PageCertDecisions.CAMPAIGN_OID_PARAMETER, provider.getAvailableData().get(0).getValue().getOid());
                    return parameters;
                }
                return null;
            }
        };

    }

    @Override
    protected boolean isDataTableVisible() {
        return getDataProvider().size() > 1;
    }
}
