/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.audit.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public class LoggerAuditServiceImpl implements AuditService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static final Logger AUDIT_LOGGER =
            LoggerFactory.getLogger(LoggingConfigurationManager.AUDIT_LOGGER_NAME);

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.audit.AuditService#audit(com.evolveum.midpoint.common.audit.AuditEventRecord)
     */
    @Override
    public void audit(AuditEventRecord record, Task task) {
        recordRecord(record);
    }

    @Override
    public void cleanupAudit(CleanupPolicyType policy, OperationResult parentResult) {
        //nothing to cleanup
    }

    private void recordRecord(AuditEventRecord record) {
        // FIXME: hardcoded auditing to a system log
        if (AUDIT_LOGGER.isInfoEnabled()) {
            AUDIT_LOGGER.info("{}", toSummary(record));
        }
        if (AUDIT_LOGGER.isDebugEnabled()) {
            AUDIT_LOGGER.debug("{}", toDetails(record));
        }
    }

    private String toSummary(AuditEventRecord record) {
        return formatTimestamp(record.getTimestamp()) +
                " eid=" + record.getEventIdentifier() +
                ", et=" + record.getEventType() +
                ", es=" + record.getEventStage() +
                ", sid=" + record.getSessionIdentifier() +
                ", rid=" + record.getRequestIdentifier() +
                ", tid=" + record.getTaskIdentifier() +
                ", toid=" + record.getTaskOid() +
                ", hid=" + record.getHostIdentifier() +
                ", nid=" + record.getNodeIdentifier() +
                ", raddr=" + record.getRemoteHostAddress() +
                ", I=" + formatReference(record.getInitiatorRef()) +
                ", T=" + formatReference(record.getTargetRef()) +
                ", TO=" + formatReference(record.getTargetOwnerRef()) +
                ", D=" + formatDeltaSummary(record.getDeltas()) +
                ", ch=" + record.getChannel() +
                ", o=" + record.getOutcome() +
                ", p=" + record.getParameter() +
                ", m=" + record.getMessage();
    }

    private String toDetails(AuditEventRecord record) {
        StringBuilder sb = new StringBuilder("Details of event ");
        sb.append(record.getEventIdentifier()).append(" stage ").append(record.getEventStage()).append("\n");

        sb.append("Deltas:");
        for (ObjectDeltaOperation<?> delta : record.getDeltas()) {
            sb.append("\n");
            if (delta == null) {
                sb.append("null");
            } else {
                sb.append(delta.debugDump(1));
            }
        }

        // TODO: target?
        return sb.toString();
    }

    private static String formatTimestamp(Long timestamp) {
        if (timestamp == null) {
            return "null";
        }
        return TIMESTAMP_FORMAT.format(
                Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
    }

    private static String formatObject(PrismObject<? extends ObjectType> object) {
        if (object == null) {
            return "null";
        }
        return object.asObjectable().toDebugType() + ":" + object.getOid() + "(" + object.getElementName() + ")";
    }

    private String formatReference(PrismReferenceValue refVal) {
        if (refVal == null) {
            return "null";
        }
        if (refVal.getObject() != null) {
            //noinspection unchecked
            return formatObject(refVal.getObject());
        }
        return refVal.toString();
    }

    private String formatDeltaSummary(Collection<ObjectDeltaOperation<? extends ObjectType>> collection) {
        if (collection == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");

        Iterator<ObjectDeltaOperation<? extends ObjectType>> iterator = collection.iterator();
        while (iterator.hasNext()) {
            ObjectDeltaOperation<?> delta = iterator.next();
            sb.append(delta.getObjectDelta().getOid()).append(":").append(delta.getObjectDelta().getChangeType());
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public List<AuditEventRecord> listRecords(String query, Map<String, Object> params, OperationResult result) {
        throw new UnsupportedOperationException("Object retrieval not supported");
    }

    @Override
    public long countObjects(String query, Map<String, Object> params) {
        throw new UnsupportedOperationException("Object retrieval not supported");
    }

    @Override
    public boolean supportsRetrieval() {
        return false;
    }

    @Override
    public void reindexEntry(AuditEventRecord record) {
        throw new UnsupportedOperationException("Reindex entry not supported");
    }

    @Override
    public int countObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) {
        throw new UnsupportedOperationException("countObjects not supported");
    }

    @Override
    @NotNull
    public SearchResultList<AuditEventRecordType> searchObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) {
        throw new UnsupportedOperationException("searchObjects not supported");
    }
}
