/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.table.ChartedHeaderDto;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.wicket.chartjs.*;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

@PanelType(name = "myCertItems")
public class MyCertificationItemsPanel extends CertificationItemsPanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = MyCertificationItemsPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(MyCertificationItemsPanel.class);
    private static final String OPERATION_COUNT_ALL_CERTIFICATION_ITEMS = DOT_CLASS + "loadCertItems";
    private static final String OPERATION_COUNT_NOT_DECIDED_CERTIFICATION_ITEMS = DOT_CLASS + "loadNotDecidedCertItems";

    public MyCertificationItemsPanel(String id) {
        super(id);
    }

    public MyCertificationItemsPanel(String id, ContainerPanelConfigurationType configurationType) {
        super(id, configurationType);
    }

    //todo cleanup; the same hack as for MyCaseWorkItemsPanel
    public MyCertificationItemsPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, null, configurationType);
    }

    @Override
    protected boolean isMyCertItems() {
        return true;
    }

    @Override
    protected LoadableModel<ChartedHeaderDto<DoughnutChartConfiguration>> getChartedHeaderDtoModel() {
        return new LoadableModel<>() {
            @Override
            protected ChartedHeaderDto<DoughnutChartConfiguration> load() {
                DoughnutChartConfiguration config = new DoughnutChartConfiguration();

                ChartData chartData = new ChartData();
                chartData.addDataset(createDataSet());
//                chartData.addDataset(createAllItemsDataset());

                config.setData(chartData);
//                config.setOptions(createChartOptions());

                long notDecidedCertItemsCount = getNotDecidedCertItemsCount();
                return new ChartedHeaderDto<>(config, "Pending certification items", String.valueOf(notDecidedCertItemsCount));
            }
        };
    }

    private @NotNull ChartOptions createChartOptions() {
        ChartOptions options = new ChartOptions();
//        options.setLegend(createLegendOptions());
        options.setIndexAxis(IndexAxis.AXIS_X.getValue());
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);

        ChartInteractionOption interaction = new ChartInteractionOption();
        interaction.setMode("index");
        interaction.setIntersect(false);
        options.setInteraction(interaction);

        ChartScaleAxisOption chartScaleXAxisOption = new ChartScaleAxisOption();
        chartScaleXAxisOption.setDisplay(true);
        ChartTitleOption chartTitleXOption =
                new ChartTitleOption();
        chartTitleXOption.setDisplay(true);
        chartTitleXOption.setText("some text");

        chartScaleXAxisOption.setTitle(chartTitleXOption);

        ChartScaleAxisOption chartScaleYAxisOption = new ChartScaleAxisOption();
        chartScaleYAxisOption.setDisplay(true);
        ChartTitleOption chartTitleYOption =
                new ChartTitleOption();
        chartTitleYOption.setDisplay(true);
        chartTitleYOption.setText("some text1");
        chartScaleYAxisOption.setTitle(chartTitleYOption);

        ChartScaleOption scales = new ChartScaleOption();
        scales.setX(chartScaleXAxisOption);
        scales.setY(chartScaleYAxisOption);
        options.setScales(scales);

        return options;
    }

    private ChartDataset createDataSet() {
        ChartDataset dataset = new ChartDataset();
        dataset.setLabel("Not decided");
        dataset.setFill(true);
        dataset.addBackgroudColor("blue");
        long notDecidedCertItemsCount = getNotDecidedCertItemsCount();
        dataset.addData(notDecidedCertItemsCount);


        long allWorkItemsCount = 0;

        try {
            ObjectQuery allWorkItemsQuery = getOpenCertWorkItemsQuery(false);
            Task task = getPageBase().createSimpleTask(OPERATION_COUNT_ALL_CERTIFICATION_ITEMS);
            allWorkItemsCount = getPageBase().getModelService()
                    .countContainers(AccessCertificationWorkItemType.class, allWorkItemsQuery, null, task, task.getResult());
        } catch (Exception ex) {
            LOGGER.error("Couldn't count all certification work items", ex);
            return null;
        }


        dataset.addData(allWorkItemsCount - notDecidedCertItemsCount);
        dataset.addBackgroudColor("grey");
        return dataset;
    }

    private long getNotDecidedCertItemsCount() {
        long count = 0;
        try {
            ObjectQuery query = getOpenCertWorkItemsQuery(true);
            Task task = getPageBase().createSimpleTask(OPERATION_COUNT_NOT_DECIDED_CERTIFICATION_ITEMS);
            count = getPageBase().getModelService()
                    .countContainers(AccessCertificationWorkItemType.class, query, null, task, task.getResult());
        } catch (Exception ex) {
            LOGGER.error("Couldn't count certification work items", ex);
        }
        return count;
    }

    private ChartDataset createAllItemsDataset() {
        long allWorkItemsCount = 0;

        try {
            ObjectQuery allWorkItemsQuery = getOpenCertWorkItemsQuery(false);
            Task task = getPageBase().createSimpleTask(OPERATION_COUNT_ALL_CERTIFICATION_ITEMS);
            allWorkItemsCount = getPageBase().getModelService()
                    .countContainers(AccessCertificationWorkItemType.class, allWorkItemsQuery, null, task, task.getResult());
        } catch (Exception ex) {
            LOGGER.error("Couldn't count all certification work items", ex);
            return null;
        }
        ChartDataset dataset = new ChartDataset();
        dataset.setLabel("All items");
        dataset.setFill(true);
        dataset.addBackgroudColor("grey");
        dataset.addData(allWorkItemsCount);
        return dataset;
    }

}
