package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component;

import org.jetbrains.annotations.NotNull;

import com.evolveum.wicket.chartjs.ChartData;
import com.evolveum.wicket.chartjs.ChartDataset;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

public class RoleAnalysisDonutChartUtils {

    public static @NotNull DoughnutChartConfiguration createDoughnutChartConfigFor(
            int all,
            int part,
            String colorAll,
            String colorPart) {

        if(all == 0){
            all = 1;
        }
        DoughnutChartConfiguration config = new DoughnutChartConfiguration();
        ChartData chartData = new ChartData();
        chartData.addDataset(createDataSet(all, part, colorAll, colorPart));
        config.setData(chartData);
        DonuthChartOptions options = new DonuthChartOptions();
        options.setResponsive(true);
        options.setMaintainAspectRatio(true);
        config.setOptions(options);

        return config;
    }

    private static @NotNull ChartDataset createDataSet(int all, int part, String colorAll, String colorPart) {
        ChartDataset dataset = new ChartDataset();
        dataset.setFill(true);

        dataset.addData(all);
        dataset.addBackgroudColor(colorAll);

        dataset.addData(part);
        dataset.addBackgroudColor(colorPart);

        return dataset;
    }

}
