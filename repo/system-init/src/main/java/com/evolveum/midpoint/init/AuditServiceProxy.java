/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import static com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil.resolveNames;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.spi.AuditServiceRegistry;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;

/**
 * @author lazyman
 */
public class AuditServiceProxy implements AuditService, AuditServiceRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(AuditServiceProxy.class);

    @Autowired
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    @Nullable
    @Autowired(required = false) // missing in some tests
    private RepositoryService repositoryService;

    @Nullable
    @Autowired(required = false) // missing in some tests (maybe)
    private TaskManager taskManager;

    @Nullable
    @Autowired(required = false) // missing in some tests (maybe)
    @Qualifier("securityContextManager")
    private SecurityContextManager securityContextManager;

    @Autowired private SchemaService schemaService;

    private final List<AuditService> services = new ArrayList<>();

    @Override
    public void audit(AuditEventRecord record, Task task, OperationResult result) {
        if (services.isEmpty()) {
            LOGGER.warn("Audit event will not be recorded. No audit services registered.");
            return;
        }

        assertCorrectness(task);
        completeRecord(record, task, result);

        for (AuditService service : services) {
            service.audit(record, task, result);
        }
    }

    @Override
    public void audit(AuditEventRecordType record, OperationResult result) {
        if (services.isEmpty()) {
            LOGGER.warn("Audit event will not be recorded. No audit services registered.");
            return;
        }

        for (AuditService service : services) {
            service.audit(record, result);
        }
    }

    @Override
    public void cleanupAudit(CleanupPolicyType policy, OperationResult parentResult) {
        Validate.notNull(policy, "Cleanup policy must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        for (AuditService service : services) {
            service.cleanupAudit(policy, parentResult);
        }
    }

    @Override
    public void registerService(AuditService service) {
        Validate.notNull(service, "Audit service must not be null.");
        if (services.contains(service)) {
            return;
        }

        services.add(service);
    }

    @Override
    public void unregisterService(AuditService service) {
        Validate.notNull(service, "Audit service must not be null.");
        services.remove(service);
    }

    private void assertCorrectness(Task task) {
        if (task == null) {
            LOGGER.warn("Task is null in a call to audit service");
        }
    }

    /**
     * Complete the record with data that can be computed or discovered from the
     * environment
     */
    private void completeRecord(AuditEventRecord record, Task task, OperationResult result) {
        LightweightIdentifier id = null;
        if (record.getEventIdentifier() == null) {
            id = lightweightIdentifierGenerator.generate();
            record.setEventIdentifier(id.toString());
        }
        if (record.getTimestamp() == null) {
            if (id == null) {
                record.setTimestamp(System.currentTimeMillis());
            } else {
                // To be consistent with the ID
                record.setTimestamp(id.getTimestamp());
            }
        }
        if (task != null) {
            if (record.getTaskIdentifier() == null) {
                record.setTaskIdentifier(task.getTaskIdentifier());
            }
            if (record.getTaskOid() == null) {
                if (task instanceof RunningTask) {
                    record.setTaskOid(((RunningTask) task).getRootTaskOid());
                } else {
                    record.setTaskOid(task.getOid());
                }
            }
            if (record.getChannel() == null) {
                record.setChannel(task.getChannel());
            }
            if (record.getInitiatorRef() == null) {
                record.setInitiator(
                        task.getOwner(result));
            }
        }

        var principal = SecurityUtil.getPrincipalIfExists();
        if (principal != null) {
            if (record.getEffectivePrincipalRef() == null) {
                record.setEffectivePrincipal(principal.getFocusPrismObject());
            }
            if (record.getEffectivePrivilegesModification() == null) {
                record.setEffectivePrivilegesModification(principal.getEffectivePrivilegesModification());
            }
            if (record.getAttorneyRef() == null) {
                record.setAttorney(principal.getAttorneyPrismObject());
            }
        }

        if (record.getNodeIdentifier() == null && taskManager != null) {
            record.setNodeIdentifier(taskManager.getNodeId());
        }

        HttpConnectionInformation connInfo = determineHttpConnectionInformation();
        if (connInfo != null) {
            if (record.getSessionIdentifier() == null) {
                record.setSessionIdentifier(connInfo.getSessionId());
            }
            if (record.getRemoteHostAddress() == null) {
                record.setRemoteHostAddress(connInfo.getRemoteHostAddress());
            }
            if (record.getHostIdentifier() == null) {
                record.setHostIdentifier(connInfo.getLocalHostName());
            }
        }

        if (record.getSessionIdentifier() == null && task != null) {
            record.setSessionIdentifier(task.getTaskIdentifier());
        }

        for (ObjectDeltaOperation<? extends ObjectType> objectDeltaOperation : record.getDeltas()) {
            ObjectDelta<? extends ObjectType> delta = objectDeltaOperation.getObjectDelta();

            Collection<SelectorOptions<GetOperationOptions>> nameOnlyOptions = schemaService.getOperationOptionsBuilder()
                    .item(ObjectType.F_NAME).retrieve() // currently this does not work as expected (retrieves all default items)
                    .readOnly() // to avoid cloning when returning from the cache
                    .build();
            ObjectDeltaSchemaLevelUtil.NameResolver nameResolver = (objectClass, oid, lResult) -> {
                if (record.getNonExistingReferencedObjects().contains(oid)) {
                    return null; // save a useless getObject call plus associated warning (MID-5378)
                }
                if (repositoryService == null) {
                    LOGGER.warn("No repository, no OID resolution (for {})", oid);
                    return null;
                }
                LOGGER.warn("Unresolved object reference in delta being audited (for {}: {}) -- this might indicate "
                                + "a performance problem, as these references are normally resolved using repository cache",
                        objectClass.getSimpleName(), oid);
                return repositoryService
                        .getObject(objectClass, oid, nameOnlyOptions, lResult)
                        .getName();
            };
            resolveNames(delta, nameResolver, result);
        }
    }

    @Nullable
    private HttpConnectionInformation determineHttpConnectionInformation() {
        HttpConnectionInformation threadLocalConnInfo = SecurityUtil.getCurrentConnectionInformation();
        if (threadLocalConnInfo != null) {
            return threadLocalConnInfo;
        } else if (securityContextManager != null) {
            return securityContextManager.getStoredConnectionInformation();
        } else {
            return null;
        }
    }

    @Override
    public boolean supportsRetrieval() {
        return services.stream().anyMatch(s -> s.supportsRetrieval());
    }

    @Override
    public void applyAuditConfiguration(SystemConfigurationAuditType configuration) {
        services.forEach(service -> service.applyAuditConfiguration(configuration));
    }

    @Override
    public int countObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) {
        int count = 0;
        for (AuditService service : services) {
            if (service.supportsRetrieval()) {
                long c = service.countObjects(query, options, parentResult);
                count += (int) c;
            }
        }
        return count;
    }

    @Override
    @NotNull
    public SearchResultList<AuditEventRecordType> searchObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult)
            throws SchemaException {
        for (AuditService service : services) {
            if (service.supportsRetrieval()) {
                return service.searchObjects(query, options, parentResult);
            }
        }

        return new SearchResultList<>();
    }

    @Override
    @NotNull
    public SearchResultMetadata searchObjectsIterative(
            @Nullable ObjectQuery query,
            @NotNull AuditResultHandler handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException {
        for (AuditService service : services) {
            if (service.supportsRetrieval()) {
                return service.searchObjectsIterative(query, handler, options, parentResult);
            }
        }

        return new SearchResultMetadata();
    }

    @Override
    public @NotNull RepositoryDiag getRepositoryDiag() {
        RepositoryDiag diag = new RepositoryDiag();
        diag.setImplementationShortName(getClass().getSimpleName());

        return diag;
    }

    /** Support public (but non-API) method to obtain concrete implementation of audit service. */
    public <T extends AuditService> T getImplementation(Class<T> implementationType) {
        for (AuditService service : services) {
            if (implementationType.isInstance(service)) {
                return implementationType.cast(service);
            }
        }

        return null;
    }
}
