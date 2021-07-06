/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPerformanceInformation;
import com.evolveum.midpoint.schema.util.task.ActivityTreeUtil.QualifiedActivityState;

import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.wicket.chartjs.*;

import org.jetbrains.annotations.NotNull;

/**
 * "Items processing" statistics for a single activity.
 */
public class ActivityItemProcessingDto implements Serializable {

    public static final String F_SUCCESS_BOX = "successBox";
    public static final String F_FAILED_BOX = "failedBox";
    public static final String F_SKIP_BOX = "skipBox";
    public static final String F_CURRENT_ITEMS = "currentItems";
    public static final String F_CHART = "chart";
    public static final String F_TITLE = "title";
    public static final String F_WALL_CLOCK_THROUGHPUT = "wallClockThroughput";

    private ProcessedItemSetType successProcessedItemSet;
    private ProcessedItemSetType failureProcessedItemSet;
    private ProcessedItemSetType skippedProcessedItemSet;

    private final List<ProcessedItemDto> currentItems = new ArrayList<>();

    private PieChartConfiguration chart;

    private final ActivityPerformanceInformation performanceInformation;

    ActivityItemProcessingDto(@NotNull QualifiedActivityState qState) {
        parseItemProcessing(qState);
        createChartConfiguration();
        performanceInformation = createPerformanceInformation(qState);
    }

    private void parseItemProcessing(@NotNull QualifiedActivityState qState) {
        ActivityItemProcessingStatisticsType itemProcessing;
        if (qState.getWorkerStates() != null) {
            itemProcessing = ActivityItemProcessingStatisticsUtil.summarize(
                    ActivityItemProcessingStatisticsUtil.getItemProcessingStatisticsFromStates(
                            qState.getWorkerStates()));
        } else {
            itemProcessing = ActivityItemProcessingStatisticsUtil.getItemProcessingStatistics(qState.getActivityState());
        }
        if (itemProcessing == null) {
            return;
        }

        for (ProcessedItemSetType processedItem : itemProcessing.getProcessed()) {
            QualifiedItemProcessingOutcomeType outcome = processedItem.getOutcome();
            if (outcome == null) {
                continue;
            }
            parseItemForOutcome(outcome.getOutcome(), processedItem);
        }
        for (ProcessedItemType currentItem : itemProcessing.getCurrent()) {
            currentItems.add(new ProcessedItemDto(currentItem));
        }
    }

    private ActivityPerformanceInformation createPerformanceInformation(QualifiedActivityState qState) {
        if (qState.getWorkerStates() != null) {
            return ActivityPerformanceInformation.forCoordinator(qState.getActivityPath(), qState.getWorkerStates());
        } else {
            return ActivityPerformanceInformation.forRegularActivity(qState.getActivityPath(), qState.getActivityState());
        }
    }

    private void parseItemForOutcome(ItemProcessingOutcomeType outcome, ProcessedItemSetType processedItem) {
        switch (outcome) {
            case SUCCESS:
                this.successProcessedItemSet = processedItem;
                break;
            case FAILURE:
                this.failureProcessedItemSet = processedItem;
                break;
            case SKIP:
                this.skippedProcessedItemSet = processedItem;
                break;
        }
    }

    @SuppressWarnings("unused") // accessed dynamically
    public ActivityInfoBoxDto getSuccessBox() {
        return createInfoBoxType("success", successProcessedItemSet, "bg-green", "fa fa-check");
    }

    @SuppressWarnings("unused") // accessed dynamically
    public ActivityInfoBoxDto getFailedBox() {
        return createInfoBoxType("failure", failureProcessedItemSet, "bg-red", "fa fa-close");
    }

    @SuppressWarnings("unused") // accessed dynamically
    public ActivityInfoBoxDto getSkipBox() {
        return createInfoBoxType("skip", skippedProcessedItemSet, "bg-gray", "fe fe-skip-step-object");
    }

    private ActivityInfoBoxDto createInfoBoxType(String title, ProcessedItemSetType processedSet, String background, String icon) {
        if (processedSet == null || processedSet.getLastItem() == null) {
            return null;
        }
        ProcessedItemType processedItem = processedSet.getLastItem();
        return createInfoBoxType(createInfoBoxMessage(title, processedSet), processedItem, background, icon);
    }

    private String createInfoBoxMessage(String result, ProcessedItemSetType processedItemSetType) {
        return getString("TaskIterativeProgress.box.title." + result, getFormattedDate(processedItemSetType));
    }

    private ActivityInfoBoxDto createInfoBoxType(String title, ProcessedItemType processedItem, String background, String icon) {
        ActivityInfoBoxDto infoBoxType = new ActivityInfoBoxDto(background, icon, title);
        infoBoxType.setNumber(processedItem.getName());

        Long end = WebComponentUtil.getTimestampAsLong(processedItem.getEndTimestamp(), true);
        Long start = WebComponentUtil.getTimestampAsLong(processedItem.getStartTimestamp(), true);

        infoBoxType.setDuration(end - start);

        infoBoxType.setErrorMessage(processedItem.getMessage());
        return infoBoxType;
    }

    private String getFormattedDate(ProcessedItemSetType processedSetItem) {
        if (processedSetItem == null) {
            return null;
        }
        ProcessedItemType processedItem = processedSetItem.getLastItem();
        Long end = WebComponentUtil.getTimestampAsLong(processedItem.getEndTimestamp(), true);
        return WebComponentUtil.formatDate(end == 0 ? processedItem.getStartTimestamp() : processedItem.getEndTimestamp());
    }

    public String getTitle() {
        if (performanceInformation.getActivityPath().isEmpty()) {
            return getString("TaskOperationStatisticsPanel.processingInfo", performanceInformation.getItemsProcessed());
        } else {
            return getString("TaskIterativeProgress.part." + performanceInformation.getActivityPath(), performanceInformation.getItemsProcessed());
        }
    }

    // TODO correct locale
    @SuppressWarnings("unused") // accessed dynamically
    public String getWallClockThroughput() {
        if (containsPerfInfo()) {
            return getString("TaskIterativeProgress.wallClock.throughput",
                    String.format(Locale.US, "%,.1f", performanceInformation.getAverageWallClockTime()),
                    String.format(Locale.US, "%,.1f", performanceInformation.getThroughput()));
        }
        return null;
    }

    private boolean containsPerfInfo() {
        return performanceInformation.getAverageWallClockTime() != null && performanceInformation.getThroughput() != null;
    }

    public int getTotalCount() {
        int success = getCount(successProcessedItemSet);
        int failure = getCount(failureProcessedItemSet);
        int skipped = getCount(skippedProcessedItemSet);

        return success + failure + skipped;
    }

    private int getCount(ProcessedItemSetType item) {
        if (item == null) {
            return 0;
        }

        Integer count = item.getCount();
        if (count == null) {
            return 0;
        }

        return count;
    }

    private void createChartConfiguration() {
        chart = new PieChartConfiguration();

        ChartData chartData = new ChartData();
        chartData.addDataset(createDataset());

        chartData.addLabel(getString("TaskIterativeProgress.success", getCount(successProcessedItemSet)));
        chartData.addLabel(getString("TaskIterativeProgress.failure", getCount(failureProcessedItemSet)));
        chartData.addLabel(getString("TaskIterativeProgress.skip", getCount(skippedProcessedItemSet)));

        chart.setData(chartData);

        chart.setOptions(createChartOptions());
    }

    private String getString(String key, Object... params) {
        StringResourceModel stringModel = new StringResourceModel(key).setDefaultValue(key).setParameters(params);
        return stringModel.getString();
    }

    private ChartDataset createDataset() {
        ChartDataset dataset = new ChartDataset();
        dataset.addData(getCount(successProcessedItemSet));
        dataset.addData(getCount(failureProcessedItemSet));
        dataset.addData(getCount(skippedProcessedItemSet));

        dataset.addBackgroudColor("rgba(73, 171, 101)");
        dataset.addBackgroudColor("rgba(168, 44, 44)");
        dataset.addBackgroudColor("rgba(145, 145, 145)");
        return dataset;
    }

    private ChartOptions createChartOptions() {
        ChartOptions options = new ChartOptions();
        options.setAnimation(createAnimation());
        options.setLegend(createChartLegend());
        return options;
    }

    private ChartAnimationOption createAnimation() {
        ChartAnimationOption animationOption = new ChartAnimationOption();
        animationOption.setDuration(0);
        return animationOption;
    }

    private ChartLegendOption createChartLegend() {
        ChartLegendOption legend = new ChartLegendOption();
        legend.setPosition("right");
        ChartLegendLabel label = new ChartLegendLabel();
        label.setBoxWidth(15);
        legend.setLabels(label);
        return legend;
    }

    public PieChartConfiguration getChart() {
        return chart;
    }

    public List<ProcessedItemDto> getCurrentItems() {
        return currentItems;
    }
}
