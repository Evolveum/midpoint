/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.spi.AuditServiceRegistry;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.*;

import static com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil.resolveNames;

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

    @Autowired private PrismContext prismContext;
    @Autowired private SchemaHelper schemaHelper;

    private final List<AuditService> services = new Vector<>();

    @Override
    public void audit(AuditEventRecord record, Task task) {

        if (services.isEmpty()) {
            LOGGER.warn("Audit event will not be recorded. No audit services registered.");
            return;
        }

        assertCorrectness(record, task);
        completeRecord(record, task);

        for (AuditService service : services) {
            service.audit(record, task);
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

    private void assertCorrectness(AuditEventRecord record, Task task) {
        if (task == null) {
            LOGGER.warn("Task is null in a call to audit service");
        }
    }

    /**
     * Complete the record with data that can be computed or discovered from the
     * environment
     */
    private void completeRecord(AuditEventRecord record, Task task) {
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
        if (record.getTaskIdentifier() == null && task != null) {
            record.setTaskIdentifier(task.getTaskIdentifier());
        }
        if (record.getTaskOid() == null && task != null) {
            record.setTaskOid(task.getOid());
        }
        if (record.getChannel() == null && task != null) {
            record.setChannel(task.getChannel());
        }
        if (record.getInitiator() == null && task != null) {
            record.setInitiator(task.getOwner());
        }

        if (record.getNodeIdentifier() == null && taskManager != null) {
            record.setNodeIdentifier(taskManager.getNodeId());
        }

        HttpConnectionInformation connInfo = SecurityUtil.getCurrentConnectionInformation();
        if (connInfo == null && securityContextManager != null) {
            connInfo = securityContextManager.getStoredConnectionInformation();
        }
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

        if (record.getDeltas() != null) {
            for (ObjectDeltaOperation<? extends ObjectType> objectDeltaOperation : record.getDeltas()) {
                ObjectDelta<? extends ObjectType> delta = objectDeltaOperation.getObjectDelta();

                // currently this does not work as expected (retrieves all default items)
                Collection<SelectorOptions<GetOperationOptions>> nameOnlyOptions = schemaHelper.getOperationOptionsBuilder()
                        .item(ObjectType.F_NAME).retrieve()
                        .build();
                ObjectDeltaSchemaLevelUtil.NameResolver nameResolver = (objectClass, oid) -> {
                    if (record.getNonExistingReferencedObjects().contains(oid)) {
                        return null;    // save a useless getObject call plus associated warning (MID-5378)
                    }
                    if (repositoryService == null) {
                        LOGGER.warn("No repository, no OID resolution (for {})", oid);
                        return null;
                    }
                    LOGGER.warn("Unresolved object reference in delta being audited (for {}: {}) -- this might indicate "
                            + "a performance problem, as these references are normally resolved using repository cache",
                            objectClass.getSimpleName(), oid);
                    PrismObject<? extends ObjectType> object = repositoryService.getObject(objectClass, oid, nameOnlyOptions,
                            new OperationResult(AuditServiceProxy.class.getName() + ".completeRecord.resolveName"));
                    return object.getName();
                };
                resolveNames(delta, nameResolver, prismContext);
            }
        }
    }

    @Override
    public List<AuditEventRecord> listRecords(String query, Map<String, Object> params, OperationResult parentResult) {
        List<AuditEventRecord> result = new ArrayList<>();
        for (AuditService service : services) {
            if (service.supportsRetrieval()) {
                List<AuditEventRecord> records = service.listRecords(query, params, parentResult);
                if (records != null && !records.isEmpty()) {
                    result.addAll(records);
                }
            }
        }
        return result;
    }

    @Override
    public void listRecordsIterative(String query, Map<String, Object> params, AuditResultHandler handler, OperationResult result) {
        for (AuditService service : services) {
            if (service.supportsRetrieval()) {
                service.listRecordsIterative(query, params, handler, result);
            }
        }
    }

    @Override
    public void reindexEntry(AuditEventRecord record) {
        for (AuditService service : services) {
            if (service.supportsRetrieval()) {
                service.reindexEntry(record);
            }
        }
    }

    @Override
    public long countObjects(String query, Map<String, Object> params) {
        long count = 0;
        for (AuditService service : services) {
            if (service.supportsRetrieval()) {
                long c = service.countObjects(query, params);
                count += c;
            }
        }
        return count;
    }

    @Override
    public boolean supportsRetrieval() {
        for (AuditService service : services) {
            if (service.supportsRetrieval()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void applyAuditConfiguration(SystemConfigurationAuditType configuration) {
        services.forEach(service -> service.applyAuditConfiguration(configuration));
    }
}
