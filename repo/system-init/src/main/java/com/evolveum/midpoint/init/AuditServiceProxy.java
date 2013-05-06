/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.spi.AuditServiceRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Vector;

/**
 * @author lazyman
 */
public class AuditServiceProxy implements AuditService, AuditServiceRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(AuditServiceProxy.class);
    @Autowired
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    private List<AuditService> services = new Vector<AuditService>();

    @Override
    public void audit(AuditEventRecord record, Task task) {
        assertCorrectness(record, task);
        completeRecord(record, task);

        if (services.isEmpty()) {
            LOGGER.warn("Audit event will not be recorded. No audit services registered.");
        }

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
        } else {
            if (task.getOwner() == null) {
                LOGGER.warn("Task '{}' has no owner in a call to audit service", new Object[]{task.getName()});
            }
        }
    }

    /**
     * Complete the record with data that can be computed or discovered from the environment
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
        if (record.getTaskOID() == null && task != null) {
            record.setTaskOID(task.getOid());
        }
        if (record.getTaskOID() == null && task != null) {
            record.setTaskOID(task.getOid());
        }
        if (record.getSessionIdentifier() == null && task != null) {
            // TODO
        }
        if (record.getInitiator() == null && task != null) {
            record.setInitiator(task.getOwner());
        }

        if (record.getHostIdentifier() == null) {
            // TODO
        }
    }
}
