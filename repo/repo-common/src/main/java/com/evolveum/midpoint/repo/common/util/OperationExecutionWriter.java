/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.util;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionRecordTypeType.SIMPLE;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Writes provided OperationExecutionType records into objects.
 * Responsibilities:
 *
 * 1. writes provided operation execution record to the specified object
 * 2. deletes superfluous operation execution records from that object
 */
@Experimental
@Component
public class OperationExecutionWriter implements SystemConfigurationChangeListener {

    private static final Trace LOGGER = TraceManager.getTrace(OperationExecutionWriter.class);

    @Autowired private SchemaService schemaService;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    public static final int DEFAULT_NUMBER_OF_RESULTS_TO_KEEP = 5;

    public static final int DEFAUL_NUMBER_OF_RESULTS_TO_KEEP_PER_TASK = 3;

    private static final String OP_WRITE = OperationExecutionWriter.class.getName() + ".write";

    /** Extracted recorded strategy for simple OpExec records (from system configuration). */
    private volatile OperationExecutionRecordingStrategyType simpleExecsRecordingStrategy;

    /** Extracted recorded strategy for complex OpExec records (from system configuration). */
    private volatile OperationExecutionRecordingStrategyType complexExecsRecordingStrategy;

    /** Extracted cleanup policy for simple OpExec records (from system configuration). */
    private volatile OperationExecutionCleanupPolicyType simpleExecsCleanupPolicy;

    /** Extracted cleanup policy for complex OpExec records (from system configuration). */
    private volatile OperationExecutionCleanupPolicyType complexExecsCleanupPolicy;

    /**
     * Writes operation execution record and deletes the one(s) that have to be deleted,
     * according to the current cleanup policy.
     */
    public <O extends ObjectType> void write(Request<O> request, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        OperationExecutionRecordTypeType currentRecordType = toNotNull(request.recordToAdd.getRecordType());

        OperationResult result = parentResult.subresult(OP_WRITE)
                .setMinor()
                .build();
        try {
            if (shouldSkipAllOperationExecutionRecording(currentRecordType)) {
                LOGGER.trace("Skipping operation execution recording because it's turned off.");
                return;
            }

            CleaningSpecification cleaningSpec = CleaningSpecification.createFrom(selectCleanupPolicy(currentRecordType));

            boolean addingRecord;
            if (cleaningSpec.isKeepNone()) {
                LOGGER.trace("Will skip operation execution recording because it's turned off (recordsToKeep is set to 0).");
                addingRecord = false;
            } else if (request.recordToAdd.getStatus() == OperationResultStatusType.SUCCESS
                    && shouldSkipOperationExecutionRecordingWhenSuccess(currentRecordType)) {
                LOGGER.trace("Will skip operation execution recording because it's turned off for successful processing.");
                addingRecord = false; // we may still delete old records
            } else {
                addingRecord = true;
            }

            try {
                List<OperationExecutionType> recordsToAdd = addingRecord ? singletonList(request.recordToAdd) : emptyList();
                List<OperationExecutionType> recordsToDelete = getRecordsToDelete(request, cleaningSpec, addingRecord, result);
                if (!recordsToAdd.isEmpty() || !recordsToDelete.isEmpty()) {
                    executeChanges(request, recordsToAdd, recordsToDelete, result);
                }
            } catch (ObjectNotFoundException e) {
                if (!request.deletedOk) {
                    throw e;
                } else {
                    LOGGER.trace("Object {} deleted but this was expected.", request.oid);
                    result.muteLastSubresultError();
                }
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
            result.switchHandledErrorToSuccess(); // Error while deleting is expected and should not be propagated upwards.
        }
    }

    private OperationExecutionRecordTypeType toNotNull(OperationExecutionRecordTypeType recordType) {
        return MoreObjects.firstNonNull(recordType, SIMPLE);
    }

    private static String getTaskOid(OperationExecutionType execution) {
        ObjectReferenceType ref = execution.getTaskRef();
        return ref != null ? ref.getOid() : null;
    }

    private static String getTaskRunIdentifier(OperationExecutionType execution) {
        return execution.getTaskRunIdentifier() != null ? execution.getTaskRunIdentifier() : null;
    }

    private OperationExecutionCleanupPolicyType selectCleanupPolicy(OperationExecutionRecordTypeType recordType) {
        return select(recordType, simpleExecsCleanupPolicy, complexExecsCleanupPolicy);
    }

    private OperationExecutionRecordingStrategyType selectRecordingStrategy(OperationExecutionRecordTypeType recordType) {
        return select(recordType, simpleExecsRecordingStrategy, complexExecsRecordingStrategy);
    }

    private <T> T select(OperationExecutionRecordTypeType recordType, T simpleVersion, T complexVersion) {
        return toNotNull(recordType) == SIMPLE ? simpleVersion : complexVersion;
    }

    private <O extends ObjectType> void executeChanges(Request<O> request,
            List<OperationExecutionType> recordsToAdd, List<OperationExecutionType> recordsToDelete, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        // Note that even recordToAdd can have a parent: e.g. if the write to primary owner fails, and we try
        // the secondary one - the recordToAdd would be already inserted in the respective delta! Hence always cloning it.
        List<ItemDelta<?, ?>> deltas = prismContext.deltaFor(request.objectType)
                .item(ObjectType.F_OPERATION_EXECUTION)
                .delete(PrismContainerValue.toPcvList(CloneUtil.cloneCollectionMembers(recordsToDelete)))
                .add(PrismContainerValue.toPcvList(CloneUtil.cloneCollectionMembers(recordsToAdd)))
                .asItemDeltas();
        LOGGER.trace("Operation execution delta:\n{}", DebugUtil.debugDumpLazily(deltas));
        repositoryService.modifyObject(request.objectType, request.oid, deltas, result);
    }

    private <O extends ObjectType> List<OperationExecutionType> getRecordsToDelete(Request<O> request,
            CleaningSpecification cleaningSpecification, boolean addingRecord, OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        if (cleaningSpecification.isKeepAll() && request.getTaskOid() == null) {
            return emptyList();
        } else {
            Collection<OperationExecutionType> loadedExistingRecords = request.existingRecords != null ?
                    request.existingRecords : loadExistingRecords(request.objectType, request.oid, result);
            return selectRecordsToDelete(request, loadedExistingRecords, cleaningSpecification, addingRecord);
        }
    }

    private <O extends ObjectType> Collection<OperationExecutionType> loadExistingRecords(Class<O> objectType, String oid,
            OperationResult result) throws SchemaException, ObjectNotFoundException {
        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                .readOnly()
                .build();
        return repositoryService.getObject(objectType, oid, options, result)
                .asObjectable()
                .getOperationExecution();
    }

    @NotNull
    private List<OperationExecutionType> selectRecordsToDelete(Request<?> request,
            Collection<OperationExecutionType> existingRecords, CleaningSpecification cleaningSpecification,
            boolean addingRecord) {

        OperationExecutionRecordTypeType currentRecordType = toNotNull(request.recordToAdd.getRecordType());
        String taskOid = request.getTaskOid();
        String taskRunIdentifier = request.getTaskRunIdentifier();
        ActivityPathType activityPath = request.getActivityPath();

        List<OperationExecutionType> recordsToDelete = new ArrayList<>();
        List<OperationExecutionType> recordsToConsider = new ArrayList<>(existingRecords);

        // Let us continue with matching record type only.
        recordsToConsider.removeIf(record -> !recordTypeMatches(record, currentRecordType));

        // Group records by task oid and delete the oldest ones if maxRecordsPerTask is exceeded
        Map<String, List<OperationExecutionType>> recordsByTask = recordsToConsider.stream()
                .filter(r -> r.getTaskRef() != null && r.getTaskRef().getOid() != null)
                .collect(Collectors.groupingBy(r -> r.getTaskRef().getOid()));

        int maxRecordsPerTask = cleaningSpecification.recordsToKeepPerTask;
        for (List<OperationExecutionType> mapValue : recordsByTask.values()) {
            if (mapValue.size() <= maxRecordsPerTask) {
                // If the number of records for this task is less than or equal to the maximum, we do not delete any.
                continue;
            }

            mapValue.sort(
                    Comparator.nullsFirst(
                            Comparator.comparing(
                                    oe -> oe.getTimestamp() != null ? oe.getTimestamp().toGregorianCalendar().getTimeInMillis() : null)));

            recordsToDelete.addAll(mapValue.subList(0, mapValue.size() - maxRecordsPerTask));
        }

        // Delete all records related to the current task (regardless of whether we add something or not)
        // It is questionable if we should remove task-related records from ALL or from those with matching type.
        // If the former is preferable, then please move this block of code just above the filtering on record type.
        for (Iterator<OperationExecutionType> iterator = recordsToConsider.iterator(); iterator.hasNext(); ) {
            OperationExecutionType record = iterator.next();
            if (shouldDeleteBecauseOfTheSameTaskAndActivity(record, taskOid, taskRunIdentifier, activityPath)) {
                recordsToDelete.add(record);
                iterator.remove();
            }
        }

        // Delete old records
        for (Iterator<OperationExecutionType> iterator = recordsToConsider.iterator(); iterator.hasNext(); ) {
            OperationExecutionType execution = iterator.next();
            if (cleaningSpecification.isOld(execution)) {
                recordsToDelete.add(execution);
                iterator.remove();
            }
        }

        // delete surplus records
        // keeping = how much we want to keep FROM EXISTING RECORDS
        int keeping = Math.max(cleaningSpecification.recordsToKeep - (addingRecord ? 1 : 0), 0);
        if (recordsToConsider.size() >= keeping) {
            if (keeping == 0) {
                recordsToDelete.addAll(recordsToConsider); // avoid unnecessary sorting
            } else {
                recordsToConsider.sort(Comparator.nullsFirst(Comparator.comparing(e -> XmlTypeConverter.toDate(e.getTimestamp()))));
                recordsToDelete.addAll(recordsToConsider.subList(0, recordsToConsider.size() - keeping));
            }
        }

        return recordsToDelete;
    }

    private boolean recordTypeMatches(@NotNull OperationExecutionType record,
            @NotNull OperationExecutionRecordTypeType recordType) {
        return toNotNull(record.getRecordType()) == recordType;
    }

    private boolean shouldDeleteBecauseOfTheSameTaskAndActivity(
            OperationExecutionType execution,
            String taskOid,
            String taskRunIdentifier,
            ActivityPathType activityPath) {
        return taskOid != null
                && taskOid.equals(getTaskOid(execution))
                && taskRunIdentifier != null
                && taskRunIdentifier.equals(getTaskRunIdentifier(execution))
                && Objects.equals(activityPath, execution.getActivityPath());
    }

    @PostConstruct
    public void init() {
        systemConfigurationChangeDispatcher.registerListener(this);
    }

    @PreDestroy
    public void destroy() {
        systemConfigurationChangeDispatcher.unregisterListener(this);
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        updateRecordingStrategies(value);
        updateCleanupPolicies(value);
    }

    private void updateRecordingStrategies(@Nullable SystemConfigurationType value) {
        if (value != null && value.getInternals() != null) {
            simpleExecsRecordingStrategy = value.getInternals().getSimpleOperationExecutionRecording();
            complexExecsRecordingStrategy = value.getInternals().getComplexOperationExecutionRecording();
            if (simpleExecsRecordingStrategy == null) {
                simpleExecsRecordingStrategy = value.getInternals().getOperationExecutionRecording();
            }
            if (complexExecsRecordingStrategy == null) {
                complexExecsRecordingStrategy = value.getInternals().getOperationExecutionRecording();
            }
        } else {
            simpleExecsRecordingStrategy = null;
            complexExecsRecordingStrategy = null;
        }
    }

    private void updateCleanupPolicies(@Nullable SystemConfigurationType value) {
        if (value != null && value.getCleanupPolicy() != null) {
            simpleExecsCleanupPolicy = value.getCleanupPolicy().getSimpleOperationExecutions();
            complexExecsCleanupPolicy = value.getCleanupPolicy().getComplexOperationExecutions();
        } else {
            simpleExecsCleanupPolicy = null;
            complexExecsCleanupPolicy = null;
        }
    }

    public boolean shouldSkipAllOperationExecutionRecording(OperationExecutionRecordTypeType recordType) {
        var recordingStrategy = selectRecordingStrategy(recordType);
        return recordingStrategy != null && Boolean.TRUE.equals(recordingStrategy.isSkip());
    }

    private boolean shouldSkipOperationExecutionRecordingWhenSuccess(OperationExecutionRecordTypeType recordType) {
        var recordingStrategy = selectRecordingStrategy(recordType);
        return recordingStrategy != null && Boolean.TRUE.equals(recordingStrategy.isSkipWhenSuccess());
    }

    public int getMaximumRecordsPerTask() {
        Integer maxRecordsPerTask = null;

        for (OperationExecutionRecordTypeType type : OperationExecutionRecordTypeType.values()) {
            OperationExecutionCleanupPolicyType policy = selectCleanupPolicy(type);
            if (policy == null || policy.getMaxRecordsPerTask() == null) {
                continue;
            }

            if (maxRecordsPerTask == null) {
                maxRecordsPerTask = policy.getMaxRecordsPerTask();
                continue;
            }

            maxRecordsPerTask = Math.max(maxRecordsPerTask, policy.getMaxRecordsPerTask());
        }

        return maxRecordsPerTask != null ? maxRecordsPerTask : DEFAUL_NUMBER_OF_RESULTS_TO_KEEP_PER_TASK;
    }

    /** An easy-to-process extract from the cleanup policy bean, along with a couple of methods. */
    private static class CleaningSpecification {
        private final int recordsToKeep;
        private final int recordsToKeepPerTask;
        private final long deleteBefore;

        private CleaningSpecification(int recordsToKeep, int recordsToKeepPerTask, long deleteBefore) {
            this.recordsToKeep = recordsToKeep;
            this.recordsToKeepPerTask = recordsToKeepPerTask;
            this.deleteBefore = deleteBefore;
        }

        private static CleaningSpecification createFrom(OperationExecutionCleanupPolicyType policy) {
            if (policy != null) {
                return new CleaningSpecification(
                        ObjectUtils.defaultIfNull(policy.getMaxRecords(), Integer.MAX_VALUE),
                        ObjectUtils.defaultIfNull(policy.getMaxRecordsPerTask(), Integer.MAX_VALUE),
                        computeDeleteBefore(policy));
            } else {
                return new CleaningSpecification(DEFAULT_NUMBER_OF_RESULTS_TO_KEEP, DEFAUL_NUMBER_OF_RESULTS_TO_KEEP_PER_TASK, 0L);
            }
        }

        private static long computeDeleteBefore(CleanupPolicyType cleanupPolicy) {
            if (cleanupPolicy.getMaxAge() != null) {
                XMLGregorianCalendar limit = XmlTypeConverter.addDuration(
                        XmlTypeConverter.createXMLGregorianCalendar(new Date()), cleanupPolicy.getMaxAge().negate());
                return XmlTypeConverter.toMillis(limit);
            } else {
                return 0;
            }
        }

        private boolean isKeepAll() {
            return recordsToKeep == Integer.MAX_VALUE && deleteBefore == 0L;
        }

        private boolean isKeepNone() {
            return recordsToKeep == 0;
        }

        private boolean isOld(OperationExecutionType execution) {
            return deleteBefore > 0 && // to avoid useless conversions
                    XmlTypeConverter.toMillis(execution.getTimestamp()) < deleteBefore;
        }
    }

    /**
     * A request to write an operation execution record.
     */
    public static class Request<O extends ObjectType> {

        /** Type of the object to write execution record to. */
        @NotNull private final Class<O> objectType;

        /** OID of the object. */
        @NotNull private final String oid;

        /** Operation execution record to be added. */
        @NotNull private final OperationExecutionType recordToAdd;

        /**
         * Existing execution records, used to generate delete deltas.
         * They can be a bit out-of-date (let us say less than a second), but not much.
         * So, therefore, if called from the clockwork, it is OK to use objectNew, assuming the
         * object is not that old. If the value is null, existing execution records will be fetched anew
         * (if cleanup is necessary).
         */
        @Nullable private final Collection<OperationExecutionType> existingRecords;

        /**
         * Is it OK if the holding object was deleted in the meanwhile? If so, and if the object
         * does not exist anymore, no exception is thrown, and the error is marked as handled.
         */
        private final boolean deletedOk;

        public Request(@NotNull Class<O> objectType, @NotNull String oid, @NotNull OperationExecutionType recordToAdd,
                @Nullable Collection<OperationExecutionType> existingRecords, boolean deletedOk) {
            this.objectType = objectType;
            this.oid = oid;
            this.recordToAdd = recordToAdd;
            this.existingRecords = existingRecords;
            this.deletedOk = deletedOk;
        }

        private String getTaskOid() {
            return OperationExecutionWriter.getTaskOid(recordToAdd);
        }

        private String getTaskRunIdentifier() {
            return OperationExecutionWriter.getTaskRunIdentifier(recordToAdd);
        }

        private ActivityPathType getActivityPath() {
            return recordToAdd.getActivityPath();
        }

        @Override
        public String toString() {
            return "Request{" +
                    "objectType=" + objectType +
                    ", oid='" + oid + '\'' +
                    ", recordToAdd=" + recordToAdd +
                    ", existingRecords=" + existingRecords +
                    ", deletedOk=" + deletedOk +
                    '}';
        }
    }
}
