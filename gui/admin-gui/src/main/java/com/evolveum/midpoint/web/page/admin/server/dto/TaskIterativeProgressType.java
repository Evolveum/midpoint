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
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.page.admin.server.TaskDisplayUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.wicket.chartjs.*;

public class TaskIterativeProgressType implements Serializable {

    public static final String F_SUCCESS_BOX = "successBox";
    public static final String F_FAILED_BOX = "failedBox";
    public static final String F_SKIP_BOX = "skipBox";
    public static final String F_CURRENT_ITEMS = "currentItems";
    public static final String F_PROGRESS = "progress";
    public static final String F_TITLE = "title";

    private ProcessedItemSetType successProcessedItemSetType;
    private ProcessedItemSetType failureProcessedItemSetType;
    private ProcessedItemSetType skippedProcessedItemSetType;

    private List<TaskInfoBoxType> currentItems = new ArrayList<>();

    private PieChartConfiguration progress;
    private String title = "";

    public TaskIterativeProgressType(IterativeTaskPartItemsProcessingInformationType processingInfoType, TaskType taskType, PageBase pageBase) {
        for (ProcessedItemSetType processedItem : processingInfoType.getProcessed()) {
            QualifiedItemProcessingOutcomeType outcome = processedItem.getOutcome();
            if (outcome == null) {
                continue;
            }
            parseItemForOutcome(outcome.getOutcome(), processedItem);
        }
        for (ProcessedItemType currentItem : processingInfoType.getCurrent()) {
            currentItems.add(createInfoBoxType("Processing now", currentItem, "bg-aqua", "fa fa-question"));
        }

        createChartConfiguration();
        createTitle(processingInfoType.getPartUri(), taskType, pageBase);
    }

    private void parseItemForOutcome(ItemProcessingOutcomeType outcome, ProcessedItemSetType processedItem) {
        switch (outcome) {
            case SUCCESS:
                this.successProcessedItemSetType = processedItem;
                break;
            case FAILURE:
                this.failureProcessedItemSetType = processedItem;
                break;
            case SKIP:
                this.skippedProcessedItemSetType = processedItem;
                break;
        }
    }

    public TaskInfoBoxType getSuccessBox() {
        return createInfoBoxType("success", successProcessedItemSetType, "bg-green", "fa fa-check");
    }

    public TaskInfoBoxType getFailedBox() {
        return createInfoBoxType("failure", failureProcessedItemSetType, "bg-red", "fa fa-close");
    }

    public TaskInfoBoxType getSkipBox() {
        return createInfoBoxType("skip", skippedProcessedItemSetType, "bg-gray", "fa fa-ban");
    }

    private String createInfoBoxMessage(String result, ProcessedItemSetType processedItemSetType) {
        return "Last " + result + " on " + getFormattedDate(processedItemSetType);
    }

    private TaskInfoBoxType createInfoBoxType(String title, ProcessedItemSetType processedsetType, String background, String icon) {
        if (processedsetType == null || processedsetType.getLastItem() == null) {
            return null;
        }
        ProcessedItemType processedItem = processedsetType.getLastItem();
        TaskInfoBoxType taskInfoBoxType = createInfoBoxType(createInfoBoxMessage(title, processedsetType), processedItem, background, icon);
        taskInfoBoxType.setProgress(getProgress(processedsetType));
        taskInfoBoxType.setDescription("All: " + processedsetType.getCount());
        return taskInfoBoxType;
    }

    private int getProgress(ProcessedItemSetType processedItemSetType) {
        int count = getCount(processedItemSetType);

        int totalCount = getTotalCount();
        if (totalCount == 0) {
            return 0;
        }

        return Math.round(((float) count / totalCount) * 100);
    }

    private TaskInfoBoxType createInfoBoxType(String title, ProcessedItemType processedItem, String background, String icon) {
        TaskInfoBoxType infoBoxType = new TaskInfoBoxType(background, icon, title);
        infoBoxType.setNumber(processedItem.getName());

        Long end = getTimestampAsLong(processedItem.getEndTimestamp());
        Long start = getTimestampAsLong(processedItem.getStartTimestamp());

        infoBoxType.setDuration(end - start);

        infoBoxType.setErrorMessage(processedItem.getMessage());
        return infoBoxType;
    }

    private String getFormattedDate(ProcessedItemSetType processedSetItem) {
        if (processedSetItem == null) {
            return null;
        }
        ProcessedItemType processedItem = processedSetItem.getLastItem();
        Long end = getTimestampAsLong(processedItem.getEndTimestamp());
        return WebComponentUtil.formatDate(end == 0 ? processedItem.getStartTimestamp() : processedItem.getEndTimestamp());
    }

    private Long getTimestampAsLong(XMLGregorianCalendar cal) {
        Long calAsLong = MiscUtil.asLong(cal);
        if (calAsLong == null) {
            return System.currentTimeMillis();
        }
        return calAsLong;
    }

    private void createTitle(String partUri, TaskType taskType, PageBase pageBase) {
        int success = getCount(successProcessedItemSetType);
        int failure = getCount(failureProcessedItemSetType);
        int skipped = getCount(skippedProcessedItemSetType);

        int objectsTotal = success + failure + skipped;
        Long wallClock = computeWallClock(objectsTotal, taskType);
        long throughput = computeThroughput(wallClock);
        if (partUri != null) {
            title = pageBase.getString("TaskIterativeProgress.part." + partUri);
        } else {
            title = pageBase.getString("TaskOperationStatisticsPanel.processingInfo");
        }
        title += pageBase.getString("TaskStatePanel.message.objectsTotal",
                objectsTotal, wallClock, throughput);
    }

    private int getTotalCount() {
        int success = getCount(successProcessedItemSetType);
        int failure = getCount(failureProcessedItemSetType);
        int skipped = getCount(skippedProcessedItemSetType);

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

    private Long computeWallClock(int totalCount, TaskType taskType) {
        if (totalCount == 0) {
            return 0L;
        }
        Long executionTime = TaskDisplayUtil.getExecutionTime(taskType);
        return executionTime != null ? executionTime/totalCount : 0;
    }

    private long computeThroughput(Long avg) {
        return avg != 0 ? 60000 / avg : 0;
    }

    private Long getDuration(ProcessedItemSetType item) {
        if (item == null) {
            return 0L;
        }

        Long duration = item.getDuration();
        if (duration == null) {
            return 0L;
        }

        return duration;
    }

    private void createChartConfiguration() {
        progress = new PieChartConfiguration();

        ChartData chartData = new ChartData();
        chartData.addDataset(createDataset());

        chartData.addLabel("Success (" + getCount(successProcessedItemSetType) + ")");
        chartData.addLabel("Failure (" + getCount(failureProcessedItemSetType) + ")");
        chartData.addLabel("Skip (" + getCount(skippedProcessedItemSetType) + ")");

        progress.setData(chartData);

        progress.setOptions(createChartOptions());
    }

    private ChartDataset createDataset() {
        ChartDataset dataset = new ChartDataset();
        dataset.addData(getCount(successProcessedItemSetType));
        dataset.addData(getCount(failureProcessedItemSetType));
        dataset.addData(getCount(skippedProcessedItemSetType));

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


}
