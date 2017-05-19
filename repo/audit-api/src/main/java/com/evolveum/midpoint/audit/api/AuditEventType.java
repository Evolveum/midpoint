/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.audit.api;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;

/**
 * @author semancik
 *
 */
public enum AuditEventType {

	GET_OBJECT,

	ADD_OBJECT,

	MODIFY_OBJECT,

	DELETE_OBJECT,

	EXECUTE_CHANGES_RAW,

	SYNCHRONIZATION,
	//  ....

	/**
	 * E.g. login
	 */
	CREATE_SESSION,

	/**
	 * E.g. logout
	 */
	TERMINATE_SESSION,

    /**
     * Workflow actions
     */
    WORK_ITEM,

    WORKFLOW_PROCESS_INSTANCE,

    RECONCILIATION;

    /*
     * Query session, modify session
     */

	/*
	 * Task states???
	 */

	/*
	 * Startup, shutdown, critical failure (whole system)
	 */

	// backup, restores

    public static AuditEventType toAuditEventType(AuditEventTypeType event) {
    	if (event == null) {
    		return null;
    	}
    	switch (event) {
			case ADD_OBJECT:
				return AuditEventType.ADD_OBJECT;
			case CREATE_SESSION:
				return AuditEventType.CREATE_SESSION;
			case DELETE_OBJECT:
				return AuditEventType.DELETE_OBJECT;
			case EXECUTE_CHANGES_RAW:
				return AuditEventType.EXECUTE_CHANGES_RAW;
			case GET_OBJECT:
				return AuditEventType.GET_OBJECT;
			case MODIFY_OBJECT:
				return AuditEventType.MODIFY_OBJECT;
			case RECONCILIATION:
				return AuditEventType.RECONCILIATION;
			case SYNCHRONIZATION:
				return AuditEventType.SYNCHRONIZATION;
			case TERMINATE_SESSION:
				return AuditEventType.TERMINATE_SESSION;
			case WORK_ITEM:
				return AuditEventType.WORK_ITEM;
			case WORKFLOW_PROCESS_INSTANCE:
				return AuditEventType.WORKFLOW_PROCESS_INSTANCE;
			default:
				throw new IllegalArgumentException("Unknown audit event type: " + event);
    	}
    }

    public static AuditEventTypeType fromAuditEventType(AuditEventType event) {
    	if (event == null) {
    		return null;
    	}
    	switch (event) {
			case ADD_OBJECT:
				return AuditEventTypeType.ADD_OBJECT;
			case CREATE_SESSION:
				return AuditEventTypeType.CREATE_SESSION;
			case DELETE_OBJECT:
				return AuditEventTypeType.DELETE_OBJECT;
			case EXECUTE_CHANGES_RAW:
				return AuditEventTypeType.EXECUTE_CHANGES_RAW;
			case GET_OBJECT:
				return AuditEventTypeType.GET_OBJECT;
			case MODIFY_OBJECT:
				return AuditEventTypeType.MODIFY_OBJECT;
			case RECONCILIATION:
				return AuditEventTypeType.RECONCILIATION;
			case SYNCHRONIZATION:
				return AuditEventTypeType.SYNCHRONIZATION;
			case TERMINATE_SESSION:
				return AuditEventTypeType.TERMINATE_SESSION;
			case WORK_ITEM:
				return AuditEventTypeType.WORK_ITEM;
			case WORKFLOW_PROCESS_INSTANCE:
				return AuditEventTypeType.WORKFLOW_PROCESS_INSTANCE;
			default:
				throw new IllegalArgumentException("Unknown audit event type: " + event);

    	}

    }

}
