/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.audit.impl;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.schema.result.OperationResult;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class LoggerAuditServiceImpl implements AuditService {

	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private static Logger AUDIT_LOGGER = org.slf4j.LoggerFactory.getLogger(LoggingConfigurationManager.AUDIT_LOGGER_NAME);

	private static final Trace LOGGER = TraceManager.getTrace(LoggerAuditServiceImpl.class);

	@Autowired
	private LightweightIdentifierGenerator lightweightIdentifierGenerator;

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
			AUDIT_LOGGER.info("{}",toSummary(record));
		}
		if (AUDIT_LOGGER.isDebugEnabled()) {
			AUDIT_LOGGER.debug("{}",toDetails(record));
		}
	}

	private String toSummary(AuditEventRecord record) {
		return formatTimestamp(record.getTimestamp()) +
				" eid=" + record.getEventIdentifier() +
				", et=" + record.getEventType() +
				", es=" + record.getEventStage() +
				", sid=" + record.getSessionIdentifier() +
				", tid=" + record.getTaskIdentifier() +
				", toid=" + record.getTaskOID() +
				", hid=" + record.getHostIdentifier() +
				", nid=" + record.getNodeIdentifier() +
				", raddr=" + record.getRemoteHostAddress() +
				", I=" + formatObject(record.getInitiator()) +
				", T=" + formatReference(record.getTarget()) +
				", TO=" + formatObject(record.getTargetOwner()) +
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
		for (ObjectDeltaOperation<?> delta: record.getDeltas()) {
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
		return TIMESTAMP_FORMAT.format(new java.util.Date(timestamp));
	}

	private static String formatObject(PrismObject<? extends ObjectType> object) {
		if (object == null) {
			return "null";
		}
		return object.asObjectable().toDebugType()+":"+object.getOid()+"("+object.getElementName()+")";
	}

	private String formatReference(PrismReferenceValue refVal) {
    	if (refVal == null) {
    		return "null";
    	}
		if (refVal.getObject() != null) {
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
	public List<AuditEventRecord> listRecords(String query, Map<String, Object> params) {
		throw new UnsupportedOperationException("Object retrieval not supported");
	}

    @Override
    public long countObjects(String query, Map<String, Object> params){
    	throw new UnsupportedOperationException("Object retrieval not supported");
    }

	@Override
	public boolean supportsRetrieval() {
		return false;
	}

	// This method is never used. It is here only for maven dependency plugin to properly detect common component usage.
	@SuppressWarnings("unused")
	private void fakeMethod() {
		LoggingConfigurationManager.getCurrentlyUsedVersion();
	}

	@Override
	public void listRecordsIterative(String query, Map<String, Object> params,
			AuditResultHandler auditResultHandler) {
		throw new UnsupportedOperationException("Object retrieval not supported");

	}

	@Override
	public void reindexEntry(AuditEventRecord record) {
		throw new UnsupportedOperationException("Reindex entry not supported");

	}
}
