/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.toMillis;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.controller.ModelInteractionServiceImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowObjectTypeStatisticsTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Service for managing statistics objects lifecycle including retrieval, validation, and deletion.
 */
@Service
public class StatisticsService {

    private static final Trace LOGGER = TraceManager.getTrace(StatisticsService.class);

    private static final String OP_GET_LATEST_STATISTICS = "getLatestStatistics";
    private static final String OP_GET_LATEST_OBJECT_TYPE_STATISTICS = "getLatestObjectTypeStatistics";
    private static final String OP_SUBMIT_OBJECT_CLASS_STATISTICS_COMPUTATION = "submitObjectClassStatisticsComputation";

    /** Default time-to-live for statistics objects if not configured. */
    private static final Duration DEFAULT_STATISTICS_TTL = XmlTypeConverter.createDuration("P1D");

    private final RepositoryService repositoryService;
    private final ModelService modelService;
    private final ModelInteractionServiceImpl modelInteractionService;
    private final SystemObjectCache systemObjectCache;

    public StatisticsService(
            @Qualifier("cacheRepositoryService") RepositoryService repositoryService,
            ModelService modelService,
            ModelInteractionServiceImpl modelInteractionService,
            SystemObjectCache systemObjectCache) {
        this.repositoryService = repositoryService;
        this.modelService = modelService;
        this.modelInteractionService = modelInteractionService;
        this.systemObjectCache = systemObjectCache;
    }

    /**
     * Returns the object holding last known statistics for the given resource and object class.
     * Automatically deletes expired statistics based on configured TTL (default: 24 hours).
     */
    public GenericObjectType getLatestStatistics(String resourceOid, QName objectClassName, OperationResult parentResult)
            throws SchemaException {
        var result = parentResult.subresult(OP_GET_LATEST_STATISTICS)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();
        try {
            var objects = repositoryService.searchObjects(
                    GenericObjectType.class,
                    PrismContext.get().queryFor(GenericObjectType.class)
                            .item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_RESOURCE_OID)
                            .eq(resourceOid)
                            .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME)
                            .eq(objectClassName.getLocalPart())
                            .build(),
                    null,
                    result);

            var latestStatistics = objects.stream()
                    .filter(o -> ObjectTypeUtil.getExtensionItemRealValue(
                                    o.asObjectable().getExtension(), MODEL_EXTENSION_STATISTICS) != null)
                    .max(Comparator.comparing(
                            o -> toMillis(ShadowObjectClassStatisticsTypeUtil.getStatisticsRequired(o).getTimestamp())))
                    .orElse(null);

            if (latestStatistics != null) {
                var statistics = ShadowObjectClassStatisticsTypeUtil.getStatisticsRequired(latestStatistics);
                if (isStatisticsExpired(statistics.getTimestamp(), result)) {
                    LOGGER.info("Statistics {} for resource {} and class {} expired, deleting",
                            latestStatistics.getOid(), resourceOid, objectClassName);
                    deleteStatistics(latestStatistics.getOid(), result);
                    return null;
                }
            }

            return latestStatistics != null ? latestStatistics.asObjectable() : null;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Starts regeneration of statistics for the given resource object class.
     *
     * <p>If a statistics computation task for the same resource and object class
     * is already running, its task OID is returned and no new task is started.
     * Otherwise, existing statistics are deleted and a new computation task
     * is submitted.</p>
     *
     * @return OID of the running or newly created statistics computation task
     */
    public @NotNull String regenerateObjectClassStatistics(String resourceOid, QName objectClassName,
            Task task, OperationResult parentResult) throws CommonException {
        String runningTaskOid = findRunningStatisticsComputationTaskOid(resourceOid, objectClassName, parentResult, task);
        if (runningTaskOid != null) {
            LOGGER.debug("There is already a running statistics computation task (OID {}) for resourceOid {}, objectClassName {};"
                            + " will not start another one",
                    runningTaskOid, resourceOid, objectClassName);
            return runningTaskOid;
        }

        var result = parentResult.subresult(OP_SUBMIT_OBJECT_CLASS_STATISTICS_COMPUTATION)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();

        try {
            deleteStatisticsForResource(resourceOid, objectClassName, parentResult);

            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(new WorkDefinitionsType()
                                    .objectClassStatisticsComputation(new ObjectClassStatisticsComputationWorkDefinitionType()
                                            .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                                            .objectClassName(objectClassName))),
                    ActivitySubmissionOptions.create().withTaskTemplate(new TaskType()
                            .name("Regenerate statistics for " + objectClassName.getLocalPart() + " on " + resourceOid)
                            .cleanupAfterCompletion(DEFAULT_STATISTICS_TTL)),
                    task, result);

            LOGGER.debug("Submitted regenerate statistics operation for resourceOid {}, objectClassName {}: {}",
                    resourceOid, objectClassName, oid);
            return oid;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /** Returns OID of running statistics computation task for given resource object class, or null if there is none. */
    private @Nullable String findRunningStatisticsComputationTaskOid(String resourceOid, QName objectClassName,
            OperationResult result, Task task) throws CommonException {

        var query = PrismContext.get().queryFor(TaskType.class)
                .item(ItemPath.create(
                        TaskType.F_AFFECTED_OBJECTS,
                        TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_RESOURCE_OBJECTS,
                        BasicResourceObjectSetType.F_RESOURCE_REF))
                .ref(resourceOid)
                .and()
                .item(TaskType.F_EXECUTION_STATE).eq(TaskExecutionStateType.RUNNING)
                .build();

        var foundOidRef = new AtomicReference<String>();

        ResultHandler<TaskType> handler = (taskPrism, lResult) -> {
            if (foundOidRef.get() != null) {
                return false; // stop iterating
            }

            TaskType taskBean = taskPrism.asObjectable();
            ActivityDefinitionType activity = taskBean.getActivity();
            if (activity == null || activity.getWork() == null) {
                return true;
            }

            WorkDefinitionsType work = activity.getWork();
            ObjectClassStatisticsComputationWorkDefinitionType def = work.getObjectClassStatisticsComputation();
            if (def == null) {
                return true;
            }

            if (objectClassName.equals(def.getObjectClassName())) {
                foundOidRef.set(taskBean.getOid());
                return false; // stop iterating, we found one
            }

            return true;
        };

        modelService.searchObjectsIterative(TaskType.class, query, handler, null, task, result);

        return foundOidRef.get();
    }

    public void deleteStatisticsForResource(
            String resourceOid,
            QName objectClassName,
            OperationResult parentResult) throws SchemaException {
        var result = parentResult.subresult("deleteStatisticsForResource")
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();
        try {
            var objects = repositoryService.searchObjects(
                    GenericObjectType.class,
                    PrismContext.get().queryFor(GenericObjectType.class)
                            .item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_RESOURCE_OID)
                            .eq(resourceOid)
                            .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME)
                            .eq(objectClassName.getLocalPart())
                            .build(),
                    null,
                    result);

            for (var obj : objects) {
                deleteStatistics(obj.getOid(), result);
            }

            LOGGER.info("Manually deleted {} statistics objects for resource {} and class {}",
                    objects.size(), resourceOid, objectClassName);
            result.recordSuccess();
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Retrieves the configured TTL for statistics from system configuration.
     * Falls back to default 24 hours if not configured.
     */
    private Duration getConfiguredTTL(OperationResult result) {
        try {
            SystemConfigurationType systemConfig = Objects.requireNonNull(systemObjectCache.getSystemConfiguration(result))
                    .asObjectable();
            if (systemConfig.getInternals() != null && systemConfig.getInternals().getSmartIntegrationStatisticsTtl() != null) {
                Duration configuredTtl = systemConfig.getInternals().getSmartIntegrationStatisticsTtl();
                LOGGER.debug("Using configured TTL for statistics: {}", configuredTtl);
                return configuredTtl;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to retrieve configured statistics TTL, using default: {}", e.getMessage());
        }
        return DEFAULT_STATISTICS_TTL;
    }

    /**
     * Checks if statistics have expired based on the configured TTL.
     */
    private boolean isStatisticsExpired(XMLGregorianCalendar timestamp, OperationResult result) {
        if (timestamp == null) {
            return true;
        }
        Duration ttl = getConfiguredTTL(result);
        XMLGregorianCalendar expirationTime = XmlTypeConverter.addDuration(timestamp, ttl);
        return XmlTypeConverter.isBeforeNow(expirationTime);
    }

    /**
     * Deletes a statistics object from the repository.
     */
    private void deleteStatistics(String statisticsOid, OperationResult result) {
        try {
            repositoryService.deleteObject(GenericObjectType.class, statisticsOid, result);
            LOGGER.debug("Deleted expired statistics object {}", statisticsOid);
        } catch (Exception e) {
            LOGGER.warn("Failed to delete statistics object {}: {}", statisticsOid, e.getMessage(), e);
        }
    }
}
