/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.audit.impl;

import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
@Service
public class AuditServiceImpl implements AuditService {
	
	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private static Logger AUDIT_LOGGER = org.slf4j.LoggerFactory.getLogger(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
	
	private static final Trace LOGGER = TraceManager.getTrace(AuditServiceImpl.class);

	@Autowired
	private LightweightIdentifierGenerator lightweightIdentifierGenerator;
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.audit.AuditService#audit(com.evolveum.midpoint.common.audit.AuditEventRecord)
	 */
	@Override
	public void audit(AuditEventRecord record, Task task) {
		
		assertCorrectness(record, task);
		completeRecord(record, task);
		recordRecord(record);

	}

	private void assertCorrectness(AuditEventRecord record, Task task) {
		if (task == null) {
			LOGGER.warn("Task is null in a call to audit service");
		} else {
			if (task.getOwner() == null) {
				LOGGER.warn("Task has no owner in a call to audit service");
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
				", st=" + record.getEventStage() + 
				", sid=" + record.getSessionIdentifier() + 
				", tid=" + record.getTaskIdentifier() +
				", toid=" + record.getTaskOID() + 
				", hid=" + record.getHostIdentifier() +
				", I=" + formatObject(record.getInitiator()) +
				", T=" + formatObject(record.getTarget()) + 
				", O=" + formatObject(record.getTargetOwner()) + 
				", D=" + formatDeltaSummary(record.getDelta()) + 
				", o=" + record.getOutcome();
	}
	

	private String toDetails(AuditEventRecord record) {
		StringBuilder sb = new StringBuilder("Details of event ");
		sb.append(record.getEventIdentifier()).append(" stage ").append(record.getEventStage()).append("\n");
		ObjectDelta<?> delta = record.getDelta();
		sb.append("Delta: ");
		if (delta == null) {
			sb.append("null");
		} else {
			sb.append("\n");
			sb.append(delta.debugDump(1));
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
	
	private static String formatObject(ObjectType object) {
		if (object == null) {
			return "null";
		}
		return ObjectTypeUtil.getShortTypeName(object)+":"+object.getOid()+"("+object.getName()+")";
	}

	private static String formatUser(UserType user) {
		if (user == null) {
			return "null";
		}
		return user.getOid()+"("+user.getName()+")";
	}

	private String formatDeltaSummary(ObjectDelta<?> delta) {
		if (delta == null) {
			return "null";
		}
		return delta.getOid()+":"+delta.getChangeType();
	}

}
