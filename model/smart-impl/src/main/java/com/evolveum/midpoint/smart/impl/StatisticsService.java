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
import java.util.Optional;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.task.api.Task;
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

    /** Default time-to-live for statistics objects if not configured. */
    private static final Duration DEFAULT_STATISTICS_TTL = XmlTypeConverter.createDuration("P1D");

    private final RepositoryService repositoryService;
    private final SystemObjectCache systemObjectCache;

    public StatisticsService(
            @Qualifier("cacheRepositoryService") RepositoryService repositoryService,
            SystemObjectCache systemObjectCache) {
        this.repositoryService = repositoryService;
        this.systemObjectCache = systemObjectCache;
    }

    /**
     * Returns the object holding last known statistics for the given resource and object class.
     * Automatically deletes expired statistics based on configured TTL (default: 24 hours).
     */
    public GenericObjectType getLatestStatistics(String resourceOid, QName objectClassName, Task task, OperationResult parentResult)
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
                if (isStatisticsExpired(statistics.getTimestamp(), task, result)) {
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
     * Returns the object holding last known statistics for the given resource, kind and intent.
     * Automatically deletes expired statistics based on configured TTL (default: 24 hours).
     */
    public GenericObjectType getLatestObjectTypeStatistics(String resourceOid, String kind, String intent, Task task, OperationResult parentResult)
            throws SchemaException {
        var result = parentResult.subresult(OP_GET_LATEST_OBJECT_TYPE_STATISTICS)
                .addParam("resourceOid", resourceOid)
                .addParam("kind", kind)
                .addParam("intent", intent)
                .build();
        try {
            var objects = repositoryService.searchObjects(
                    GenericObjectType.class,
                    PrismContext.get().queryFor(GenericObjectType.class)
                            .item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_RESOURCE_OID)
                            .eq(resourceOid)
                            .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_KIND_NAME)
                            .eq(kind)
                            .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_INTENT_NAME)
                            .eq(intent)
                            .build(),
                    null,
                    result);

            var latestStatistics = objects.stream()
                    .filter(o -> ObjectTypeUtil.getExtensionItemRealValue(
                                    o.asObjectable().getExtension(), MODEL_EXTENSION_OBJECT_TYPE_STATISTICS) != null)
                    .max(Comparator.comparing(
                            o -> toMillis(ShadowObjectTypeStatisticsTypeUtil.getObjectTypeStatisticsRequired(o).getTimestamp())))
                    .orElse(null);

            if (latestStatistics != null) {
                var statistics = ShadowObjectTypeStatisticsTypeUtil.getObjectTypeStatisticsRequired(latestStatistics);
                if (isStatisticsExpired(statistics.getTimestamp(), task, result)) {
                    LOGGER.info("Object type statistics {} for resource {}/{}/{} expired, deleting",
                            latestStatistics.getOid(), resourceOid, kind, intent);
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

    public void deleteStatisticsForResource(
            String resourceOid,
            QName objectClassName,
            Task task,
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

    public void deleteObjectTypeStatistics(
            String resourceOid,
            String kind,
            String intent,
            Task task,
            OperationResult parentResult) throws SchemaException {
        var result = parentResult.subresult("deleteObjectTypeStatistics")
                .addParam("resourceOid", resourceOid)
                .addParam("kind", kind)
                .addParam("intent", intent)
                .build();
        try {
            var objects = repositoryService.searchObjects(
                    GenericObjectType.class,
                    PrismContext.get().queryFor(GenericObjectType.class)
                            .item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_RESOURCE_OID)
                            .eq(resourceOid)
                            .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_KIND_NAME)
                            .eq(kind)
                            .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_INTENT_NAME)
                            .eq(intent)
                            .build(),
                    null,
                    result);

            for (var obj : objects) {
                deleteStatistics(obj.getOid(), result);
            }

            LOGGER.info("Manually deleted {} object type statistics for resource {}/{}/{}",
                    objects.size(), resourceOid, kind, intent);
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
    private Duration getConfiguredTTL(Task task, OperationResult result) {
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
    private boolean isStatisticsExpired(XMLGregorianCalendar timestamp, Task task, OperationResult result) {
        if (timestamp == null) {
            return true;
        }
        Duration ttl = getConfiguredTTL(task, result);
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
