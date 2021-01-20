/*
 * Copyright (c) 2020 Evolveum and contributors
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
import com.evolveum.midpoint.schema.SchemaHelper;
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

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static java.util.Collections.emptyList;

/**
 * Writes provided OperationExecutionType records into objects.
 */
@Experimental
@Component
public class OperationExecutionWriter implements SystemConfigurationChangeListener {

    private static final Trace LOGGER = TraceManager.getTrace(OperationExecutionWriter.class);

    @Autowired private SchemaHelper schemaHelper;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    private static final int DEFAULT_NUMBER_OF_RESULTS_TO_KEEP = 5;

    private static final String OP_WRITE = OperationExecutionWriter.class.getName() + ".write";

    private volatile OperationExecutionRecordingStrategyType recordingStrategy;

    private volatile CleanupPolicyType cleanupPolicy;

    /**
     * Writes operation execution record and deletes the one(s) that have to be deleted,
     * according to the current cleanup policy.
     *
     * @param objectType Type of the object to write execution record to.
     * @param oid OID of the object.
     * @param executionToAdd Operation execution record to be added.
     * @param existingExecutions Existing execution records, used to generate delete deltas.
     *        They can be a little bit out-of-date (let us say less than a second), but not much.
     *        So, therefore, if called from the clockwork, it is OK to use objectNew, assuming the
     *        object is not that old. If the value is null, existing executions will be fetched anew
     *        (if cleanup is necessary).
     * @param deletedOk Is it OK if the holding object was deleted in the meanwhile?
     */
    public <O extends ObjectType> void write(@NotNull Class<O> objectType, @NotNull String oid,
            @NotNull OperationExecutionType executionToAdd, @Nullable Collection<OperationExecutionType> existingExecutions,
            boolean deletedOk, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        OperationResult result = parentResult.subresult(OP_WRITE)
                .setMinor()
                .build();
        try {
            CleaningSpecification cleaningSpec = CleaningSpecification.createFrom(cleanupPolicy);
            if (cleaningSpec.isKeepNone()) {
                // Unlike in 4.2 and before, we do not delete old records if we are not going to write anything.
                LOGGER.trace("Skipping operation execution recording because it's turned off (recordsToKeep is set to 0).");
                return;
            }

            if (shouldSkipOperationExecutionRecording()) {
                LOGGER.trace("Skipping operation execution recording because it's turned off.");
                return;
            }

            if (shouldSkipOperationExecutionRecordingWhenSuccess() &&
                    executionToAdd.getStatus() == OperationResultStatusType.SUCCESS) {
                LOGGER.trace("Skipping operation execution recording because it's turned off for successful processing.");
                return;
            }

            try {
                List<OperationExecutionType> executionsToDelete = getExecutionsToDelete(objectType, oid,
                        existingExecutions, cleaningSpec, result);
                executeChanges(objectType, oid, executionToAdd, executionsToDelete, result);
            } catch (ObjectNotFoundException e) {
                if (!deletedOk) {
                    throw e;
                } else {
                    LOGGER.trace("Object {} deleted but this was expected.", oid);
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

    private <O extends ObjectType> void executeChanges(Class<O> objectType, String oid, OperationExecutionType executionToAdd,
            List<OperationExecutionType> executionsToDelete, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        List<ItemDelta<?, ?>> deltas = prismContext.deltaFor(objectType)
                .item(ObjectType.F_OPERATION_EXECUTION)
                .add(executionToAdd)
                .delete(PrismContainerValue.toPcvList(CloneUtil.cloneCollectionMembers(executionsToDelete)))
                .asItemDeltas();
        LOGGER.trace("Operation execution delta:\n{}", DebugUtil.debugDumpLazily(deltas));
        repositoryService.modifyObject(objectType, oid, deltas, result);
    }

    private <O extends ObjectType> List<OperationExecutionType> getExecutionsToDelete(Class<O> objectType,
            String oid, @Nullable Collection<OperationExecutionType> existingExecutions,
            CleaningSpecification cleaningSpecification, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (cleaningSpecification.isKeepAll()) {
            return emptyList();
        } else {
            Collection<OperationExecutionType> loadedExistingExecutions = existingExecutions != null ?
                    existingExecutions : loadExistingExecutions(objectType, oid, result);
            return selectExecutionsToDelete(loadedExistingExecutions, cleaningSpecification);
        }
    }

    private <O extends ObjectType> Collection<OperationExecutionType> loadExistingExecutions(Class<O> objectType, String oid,
            OperationResult result) throws SchemaException, ObjectNotFoundException {
        Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                .readOnly()
                .build();
        return repositoryService.getObject(objectType, oid, options, result)
                .asObjectable()
                .getOperationExecution();
    }

    @NotNull
    private List<OperationExecutionType> selectExecutionsToDelete(Collection<OperationExecutionType> existingExecutions,
            CleaningSpecification cleaningSpecification) {
        List<OperationExecutionType> executionsToDelete = new ArrayList<>();
        List<OperationExecutionType> executions = new ArrayList<>(existingExecutions);

        // delete old executions
        for (Iterator<OperationExecutionType> iterator = executions.iterator(); iterator.hasNext(); ) {
            OperationExecutionType execution = iterator.next();
            if (cleaningSpecification.isOld(execution)) {
                executionsToDelete.add(execution);
                iterator.remove();
            }
        }

        // delete surplus executions
        // keeping = how much we want to keep FROM EXISTING RECORDS; assuming we are going to add new record
        int keeping = Math.max(cleaningSpecification.recordsToKeep - 1, 0);
        if (executions.size() >= keeping) {
            if (keeping == 0) {
                executionsToDelete.addAll(executions); // avoid unnecessary sorting
            } else {
                executions.sort(Comparator.nullsFirst(Comparator.comparing(e -> XmlTypeConverter.toDate(e.getTimestamp()))));
                executionsToDelete.addAll(executions.subList(0, executions.size() - keeping));
            }
        }

        return executionsToDelete;
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
        this.recordingStrategy = value != null && value.getInternals() != null ?
                value.getInternals().getOperationExecutionRecording() : null;
        this.cleanupPolicy = value != null && value.getCleanupPolicy() != null ?
                value.getCleanupPolicy().getObjectResults() : null;
    }

    public boolean shouldSkipOperationExecutionRecording() {
        return recordingStrategy != null && Boolean.TRUE.equals(recordingStrategy.isSkip());
    }

    private boolean shouldSkipOperationExecutionRecordingWhenSuccess() {
        return recordingStrategy != null && Boolean.TRUE.equals(recordingStrategy.isSkipWhenSuccess());
    }

    /** An easy-to-process extract from the cleanup policy bean, along with a couple of methods. */
    private static class CleaningSpecification {
        private final int recordsToKeep;
        private final long deleteBefore;

        private CleaningSpecification(int recordsToKeep, long deleteBefore) {
            this.recordsToKeep = recordsToKeep;
            this.deleteBefore = deleteBefore;
        }

        private static CleaningSpecification createFrom(CleanupPolicyType policy) {
            if (policy != null) {
                return new CleaningSpecification(
                        ObjectUtils.defaultIfNull(policy.getMaxRecords(), Integer.MAX_VALUE),
                        computeDeleteBefore(policy));
            } else {
                return new CleaningSpecification(DEFAULT_NUMBER_OF_RESULTS_TO_KEEP, 0L);
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
}
