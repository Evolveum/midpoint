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
package com.evolveum.midpoint.audit.api;

import java.text.SimpleDateFormat;

import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
public class AuditEventRecord {
	
	/**
	 * Timestamp in millis.
	 */
	private Long timestamp;
	
	/**
	 * Unique identification of the event.
	 */
	private String eventIdentifier;
	
	// session ID
	private String sessionIdentifier;
	
	// channel???? (e.g. web gui, web service, ...)
	
	// task ID (not OID!)
	private String taskIdentifier;
	private String taskOID;
	
	// host ID
	private String hostIdentifier;
	
	// initiator (subject, event "owner"): store OID, type(implicit?), name
	private UserType initiator;
	
	// (primary) target (object, the thing acted on): store OID, type, name
	// OPTIONAL
	private ObjectType target;
	
	// user that the target "belongs to"????
	private UserType targetOwner;
		
	// event type
	private AuditEventType eventType;
	
	// event stage (request, execution)
	
	// delta
	private ObjectDelta<?> delta;
	
	// delta order (primary, secondary)
	
	// outcome (success, failure)
	private OperationResultStatus outcome;

	// result (e.g. number of entries, returned object???)

	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	
	public AuditEventRecord() {
	}
	
	public AuditEventRecord(AuditEventType eventType) {
		this.eventType = eventType;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public String getEventIdentifier() {
		return eventIdentifier;
	}

	public void setEventIdentifier(String eventIdentifier) {
		this.eventIdentifier = eventIdentifier;
	}

	public String getSessionIdentifier() {
		return sessionIdentifier;
	}

	public void setSessionIdentifier(String sessionIdentifier) {
		this.sessionIdentifier = sessionIdentifier;
	}

	public String getTaskIdentifier() {
		return taskIdentifier;
	}

	public void setTaskIdentifier(String taskIdentifier) {
		this.taskIdentifier = taskIdentifier;
	}

	public String getTaskOID() {
		return taskOID;
	}

	public void setTaskOID(String taskOID) {
		this.taskOID = taskOID;
	}

	public String getHostIdentifier() {
		return hostIdentifier;
	}

	public void setHostIdentifier(String hostIdentifier) {
		this.hostIdentifier = hostIdentifier;
	}

	public UserType getInitiator() {
		return initiator;
	}

	public void setInitiator(UserType initiator) {
		this.initiator = initiator;
	}

	public ObjectType getTarget() {
		return target;
	}

	public void setTarget(ObjectType target) {
		this.target = target;
	}

	public UserType getTargetOwner() {
		return targetOwner;
	}

	public void setTargetOwner(UserType targetOwner) {
		this.targetOwner = targetOwner;
	}

	public AuditEventType getEventType() {
		return eventType;
	}

	public void setEventType(AuditEventType eventType) {
		this.eventType = eventType;
	}

	public ObjectDelta<?> getDelta() {
		return delta;
	}

	public void setDelta(ObjectDelta<?> delta) {
		this.delta = delta;
	}

	public OperationResultStatus getOutcome() {
		return outcome;
	}

	public void setOutcome(OperationResultStatus outcome) {
		this.outcome = outcome;
	}

	@Override
	public String toString() {
		return "AUDIT[" + formatTimestamp(timestamp) + " " + eventIdentifier
				+ " s=" + sessionIdentifier + " t=" + taskIdentifier
				+ " toid=" + taskOID + " h=" + hostIdentifier + " I=" + formatObject(initiator)
				+ " T=" + formatObject(target) + ", O=" + formatObject(targetOwner) + ", e=" + eventType
				+ ", D=" + delta + ", o=" + outcome + "]";
	}

	private String formatTimestamp(Long timestamp) {
		if (timestamp == null) {
			return "null";
		}
		return TIMESTAMP_FORMAT.format(new java.util.Date(timestamp));
	}
	
	private String formatObject(ObjectType object) {
		if (object == null) {
			return "null";
		}
		return ObjectTypeUtil.toShortString(object);
	}
		
}
