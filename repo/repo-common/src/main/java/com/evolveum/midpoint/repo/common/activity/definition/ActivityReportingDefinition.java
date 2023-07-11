/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import static java.util.Comparator.*;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Defines reporting features of the activity, like logging, tracing, profiling, and reports.
 */
public class ActivityReportingDefinition implements DebugDumpable, Cloneable {

    /**
     * This bean is detached copy dedicated for this definition. It is therefore freely modifiable.
     */
    @NotNull private ActivityReportingDefinitionType bean;

    /** Default values for various reporting options. Specified for concrete activity run. */
    @NotNull private Lazy<ActivityReportingCharacteristics> reportingCharacteristics =
            Lazy.from(ActivityReportingCharacteristics::new);

    private ActivityReportingDefinition(@NotNull ActivityReportingDefinitionType bean) {
        this.bean = bean;
    }

    public ActivityReportingDefinition(@NotNull ActivityReportingDefinitionType bean,
            @NotNull Lazy<ActivityReportingCharacteristics> reportingCharacteristics) {
        this.bean = bean;
        this.reportingCharacteristics = reportingCharacteristics;
    }

    /**
     * The task can be null for children of custom composite activities.
     */
    @NotNull
    public static ActivityReportingDefinition create(@Nullable ActivityDefinitionType definitionBean) {
        ActivityReportingDefinitionType bean =
                definitionBean != null && definitionBean.getReporting() != null ?
                        definitionBean.getReporting().clone() :
                        new ActivityReportingDefinitionType();

        return new ActivityReportingDefinition(bean);
    }

    public @NotNull List<ActivityTracingDefinitionType> getTracingConfigurationsSorted() {
        var sorted = new ArrayList<>(bean.getTracing());
        sorted.sort(
                comparing(
                        ActivityTracingDefinitionType::getOrder,
                        nullsLast(naturalOrder())));
        return sorted;
    }

    public @Nullable ActivityProfilingDefinitionType getProfilingConfiguration() {
        return bean.getProfiling();
    }

    void applyChangeTailoring(@NotNull ActivityTailoringType tailoring) {
        if (tailoring.getReporting() != null) {
            bean = TailoringUtil.getTailoredBean(bean, tailoring.getReporting());
        } else {
            // null means we do not want it to change.
        }
    }

    @Override
    public String toString() {
        return "logging: " + size(bean.getLogging()) + " item(s), "
                + "tracing: " + bean.getTracing().size() + " configuration(s), "
                + "profiling: " + (bean.getProfiling() != null ? "present" : "absent")
                + "reports: " + size(bean.getReports()) + " item(s), "
                + "state overview: " + size(bean.getStateOverview()) + " item(s), "
                + "item counting: " + size(bean.getItemCounting()) + " item(s)";
    }

    private int size(Containerable containerable) {
        return containerable != null ? containerable.asPrismContainerValue().size() : 0;
    }

    @Override
    public String debugDump(int indent) {
        return bean.debugDump();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ActivityReportingDefinition clone() {
        return new ActivityReportingDefinition(
                bean.clone(),
                Lazy.instant(reportingCharacteristics.get().clone())); // This clone is not strictly necessary.
    }

    public void applyDefaults(Lazy<ActivityReportingCharacteristics> lazyDefaults) {
        reportingCharacteristics = lazyDefaults;
    }

    public @NotNull ActivityReportingDefinitionType getBean() {
        return bean;
    }

    public BucketsProcessingReportDefinitionType getBucketsReportDefinition() {
        ActivityReportsDefinitionType reports = bean.getReports();
        return reports != null ? reports.getBuckets() : null;
    }

    public ItemsProcessingReportDefinitionType getItemsReportDefinition() {
        ActivityReportsDefinitionType reports = bean.getReports();
        return reports != null ? reports.getItems() : null;
    }

    public ConnIdOperationsReportDefinitionType getConnIdOperationsReportDefinition() {
        ActivityReportsDefinitionType reports = bean.getReports();
        return reports != null ? reports.getConnIdOperations() : null;
    }

    public InternalOperationsReportDefinitionType getInternalOperationsReportDefinition() {
        ActivityReportsDefinitionType reports = bean.getReports();
        return reports != null ? reports.getInternalOperations() : null;
    }

    public @Nullable Long getStateOverviewProgressUpdateInterval() {
        return bean.getStateOverview() != null ?
                bean.getStateOverview().getProgressUpdateInterval() : null;
    }

    public @NotNull ActivityStateOverviewProgressUpdateModeType getStateOverviewProgressUpdateMode() {
        return MoreObjects.firstNonNull(
                getStateOverviewProgressUpdateModeRaw(),
                ActivityStateOverviewProgressUpdateModeType.FOR_NON_LOCAL_ACTIVITIES);
    }

    private @Nullable ActivityStateOverviewProgressUpdateModeType getStateOverviewProgressUpdateModeRaw() {
        return bean.getStateOverview() != null ?
                bean.getStateOverview().getProgressUpdateMode() : null;
    }

    /** How should be bucket completion logged? (none/brief/full) */
    public @NotNull ActivityEventLoggingOptionType getBucketCompletionLogging() {
        ActivityLoggingOptionsType logging = bean.getLogging();
        if (logging != null && logging.getBucketCompletion() != null) {
            return logging.getBucketCompletion();
        } else {
            return reportingCharacteristics.get().getBucketCompletionLoggingDefault();
        }
    }

    /** How should be item completion logged? (none/brief/full) */
    public @NotNull ActivityEventLoggingOptionType getItemCompletionLogging() {
        ActivityLoggingOptionsType logging = bean.getLogging();
        if (logging != null && logging.getItemCompletion() != null) {
            return logging.getItemCompletion();
        } else {
            return reportingCharacteristics.get().getItemCompletionLoggingDefault();
        }
    }

    public @NotNull ActivityItemCountingOptionType getDetermineBucketSize() {
        ActivityItemCountingDefinitionType itemCounting = bean.getItemCounting();
        if (itemCounting != null && itemCounting.getDetermineBucketSize() != null) {
            return itemCounting.getDetermineBucketSize();
        } else {
            return reportingCharacteristics.get().getDetermineBucketSizeDefault();
        }
    }

    public @NotNull ActivityOverallItemCountingOptionType getDetermineOverallSize() {
        ActivityItemCountingDefinitionType itemCounting = bean.getItemCounting();
        if (itemCounting != null && itemCounting.getDetermineOverallSize() != null) {
            return itemCounting.getDetermineOverallSize();
        } else {
            return reportingCharacteristics.get().getDetermineOverallSizeDefault();
        }
    }

    /** Whether we should use the "expected total" (overall size) information if already present. */
    public boolean isCacheOverallSize() {
        ActivityItemCountingDefinitionType itemCounting = bean.getItemCounting();
        if (itemCounting != null && itemCounting.isCacheOverallSize() != null) {
            return itemCounting.isCacheOverallSize();
        } else {
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean shouldCreateSimulationResult() {
        return bean.getSimulationResult() != null;
    }

    public SimulationDefinitionType getSimulationDefinition() {
        ActivitySimulationResultDefinitionType simResultPart = bean.getSimulationResult();
        return simResultPart != null ? simResultPart.getDefinition() : null;
    }
}
