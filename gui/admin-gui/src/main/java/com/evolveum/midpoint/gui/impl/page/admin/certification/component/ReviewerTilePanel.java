/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.table.ChartedHeaderDto;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component.WidgetRmChartComponent;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.evolveum.wicket.chartjs.ChartData;
import com.evolveum.wicket.chartjs.ChartDataset;
import com.evolveum.wicket.chartjs.ChartOptions;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.IResource;

import java.io.Serial;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

public class ReviewerTilePanel extends TilePanel<Tile<UserType>, UserType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_REVIEWER_INFO_GRAPH = "reviewerInfoGraph";

    private ReviewerStatisticDto statisticDto;

    public ReviewerTilePanel(String id, IModel<Tile<UserType>> model, ReviewerStatisticDto statisticDto) {
        super(id, model);
        this.statisticDto = statisticDto;
        initLayout();
    }

    @Override
    protected Component createIconPanel(String idIcon) {
        UserType reviewer = getModelObject().getValue();
        IResource messageImageResource = WebComponentUtil.createJpegPhotoResource(reviewer);
        Component messageImagePanel = WebComponentUtil.createPhotoOrDefaultImagePanel(idIcon, messageImageResource,
                new IconType().cssClass("fa fa-user-circle"));
        messageImagePanel.add(AttributeAppender.append("style", "font-size: 50px;"));
        return messageImagePanel;
    }

    private void initLayout() {
        DoughnutChartConfiguration chartConfig = getReviewerProgressChartConfig();

        ChartedHeaderDto<DoughnutChartConfiguration> infoDto = new ChartedHeaderDto<>(chartConfig,
                createStatisticBoxLabel(), "", createPercentageLabel());
        WidgetRmChartComponent<DoughnutChartConfiguration> chartComponent = new WidgetRmChartComponent<>(ID_REVIEWER_INFO_GRAPH,
                Model.of(new DisplayType()), Model.of(infoDto));
        chartComponent.setOutputMarkupId(true);
        chartComponent.add(AttributeAppender.append(CLASS_CSS,"col-auto p-0"));
        add(chartComponent);

    }

    private DoughnutChartConfiguration getReviewerProgressChartConfig() {
        DoughnutChartConfiguration config = new DoughnutChartConfiguration();

        ChartOptions options = new ChartOptions();
        options.setMaintainAspectRatio(false);
        options.setResponsive(true);
        config.setOptions(options);

        ChartData chartData = new ChartData();
        ChartDataset dataset = new ChartDataset();
//        dataset.setLabel("Not decided");

        dataset.setFill(true);

        long notDecidedCertItemsCount = statisticDto.getNotDecidedItemsCount();
        long allOpenCertItemsCount = statisticDto.getAllItemsCount();
        long decidedCertItemsCount = allOpenCertItemsCount - notDecidedCertItemsCount;

        dataset.addData(decidedCertItemsCount);
        dataset.addBackgroudColor(getChartBackgroundColor(statisticDto.getOpenDecidedItemsPercentage()));

        dataset.addData(notDecidedCertItemsCount);
        dataset.addBackgroudColor("grey");

        chartData.addDataset(dataset);

        config.setData(chartData);
        return config;
    }

    private String getChartBackgroundColor(float decidedItemsPercentage) {
        if (decidedItemsPercentage == 100) {
            return "green";
        } else if (decidedItemsPercentage > 50) {
            return "blue";
        } if (decidedItemsPercentage > 25) {
            return "#ffc107";
        } else {
            return "red";
        }
    }

    private String createStatisticBoxLabel() {
        return getString("ReviewersStatisticsPanel.statistic.itemsWaitingForDecision",
                statisticDto.getNotDecidedItemsCount(), statisticDto.getAllItemsCount());
    }

    private String createPercentageLabel() {
        return String.format("%.0f", statisticDto.getOpenDecidedItemsPercentage()) + "%";
    }
}
