/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.PatternMatcher;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a report on internal operations. Currently supports only operations during item processing, i.e. not during
 * pre-processing phase.
 */
public class InternalOperationsReport extends AbstractReport {

    private static final ItemPath STATE_ITEM_PATH =
            ItemPath.create(ActivityStateType.F_REPORTS, ActivityReportsType.F_INTERNAL_OPERATIONS);

    @NotNull private final List<FastFilter> fastFilters = new ArrayList<>();

    public InternalOperationsReport(InternalOperationsReportDefinitionType definition,
            @NotNull CurrentActivityState<?> activityState) {
        super(definition, InternalOperationRecordType.COMPLEX_TYPE, activityState);
        if (definition != null) {
            definition.getFastFilter()
                    .forEach(bean -> fastFilters.add(FastFilter.create(bean)));
        }
    }

    @Override
    String getReportType() {
        return "Internal operations";
    }

    @Override
    @NotNull ItemPath getStateItemPath() {
        return STATE_ITEM_PATH;
    }

    public void add(@NotNull ItemProcessingRequest<?> request, @NotNull WorkBucketType bucket,
            @NotNull OperationResult reportedResult, @NotNull RunningTask task, @NotNull OperationResult result) {

        List<InternalOperationRecordType> records = new ArrayList<>();

        createRecords(request, bucket, reportedResult, null, task, result, records);

        if (!records.isEmpty()) {

            // Can be called from multiple LATs at once.
            synchronized (this) {
                openIfClosed(result);
                writeRecords(records);
            }
        }
    }

    private void createRecords(@NotNull ItemProcessingRequest<?> request, @NotNull WorkBucketType bucket,
            @NotNull OperationResult reportedResult, @Nullable Long parent,
            @NotNull RunningTask task, @NotNull OperationResult result,
            @NotNull List<InternalOperationRecordType> records) {
        createRecord(request, bucket, reportedResult, parent, task, result, records);
        reportedResult.getSubresults().forEach(
                sub -> createRecords(request, bucket, sub, reportedResult.getToken(), task, result, records));
    }

    private void createRecord(@NotNull ItemProcessingRequest<?> request, @NotNull WorkBucketType bucket,
            @NotNull OperationResult reportedResult, @Nullable Long parent,
            @NotNull RunningTask task, @NotNull OperationResult result,
            @NotNull List<InternalOperationRecordType> records) {

        Lazy<String> concatenatedQualifiersLazy = Lazy.from(() -> concatQualifiers(reportedResult));
        if (fastFiltersReject(reportedResult, concatenatedQualifiersLazy)) {
            return;
        }

        InternalOperationRecordType record = new InternalOperationRecordType()
                .operation(reportedResult.getOperation())
                .qualifiers(concatenatedQualifiersLazy.get())
                .operationKind(reportedResult.getOperationKind())
                .status(reportedResult.getStatusBean())
                .importance(reportedResult.getImportance())
                .asynchronousOperationReference(reportedResult.getAsynchronousOperationReference())
                .start(XmlTypeConverter.createXMLGregorianCalendar(reportedResult.getStart()))
                .end(XmlTypeConverter.createXMLGregorianCalendar(reportedResult.getEnd()))
                .microseconds(reportedResult.getMicrosecondsNullable())
                .cpuMicroseconds(reportedResult.getCpuMicrosecondsNullable())
                .invocationId(reportedResult.getInvocationIdNullable())
                .params(reportedResult.getParamsBean())
                .context(reportedResult.getContextBean())
                .returns(reportedResult.getReturnsBean())
                .token(reportedResult.getToken())
                .messageCode(reportedResult.getMessageCode())
                .message(reportedResult.getMessage())
                .parent(parent);
        ActivityReportUtil.addItemInformation(record, request, bucket);

        if (isRejected(record, task, result)) {
            return;
        }

        records.add(record);
    }

    private boolean fastFiltersReject(@NotNull OperationResult result, Lazy<String> concatenatedQualifiersLazy) {
        return fastFilters.stream()
                .allMatch(f -> f.rejects(result, concatenatedQualifiersLazy));
    }

    @NotNull
    private String concatQualifiers(OperationResult result) {
        return String.join(".", result.getQualifiers());
    }

    private static class FastFilter {
        @NotNull private final PatternMatcher operationNameInclusionMatcher;
        @NotNull private final PatternMatcher operationNameExclusionMatcher;
        @NotNull private final PatternMatcher individualQualifierMatcher;
        @NotNull private final PatternMatcher concatenatedQualifiersMatcher;

        private FastFilter(@NotNull InternalOperationRecordFastFilterType bean) {
            operationNameInclusionMatcher = PatternMatcher.create(bean.getOperationInclude());
            operationNameExclusionMatcher = PatternMatcher.create(bean.getOperationExclude());
            individualQualifierMatcher = PatternMatcher.create(bean.getIndividualQualifier());
            concatenatedQualifiersMatcher = PatternMatcher.create(bean.getConcatenatedQualifiers());
        }

        private static FastFilter create(@NotNull InternalOperationRecordFastFilterType bean) {
            return new FastFilter(bean);
        }

        private boolean rejects(OperationResult result, Lazy<String> concatenatedQualifiersLazy) {
            return !operationNameInclusionMatcher.isEmpty() &&
                        !operationNameInclusionMatcher.match(result.getOperation()) ||
                    operationNameExclusionMatcher.match(result.getOperation()) ||
                    !individualQualifierMatcher.isEmpty() &&
                            result.getQualifiers().stream().noneMatch(individualQualifierMatcher::match) ||
                    !concatenatedQualifiersMatcher.isEmpty() &&
                            !concatenatedQualifiersMatcher.match(concatenatedQualifiersLazy.get());
        }
    }
}
