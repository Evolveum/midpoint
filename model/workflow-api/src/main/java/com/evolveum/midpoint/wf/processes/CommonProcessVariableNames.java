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

package com.evolveum.midpoint.wf.processes;

/**
 * @author Pavol
 */
public class CommonProcessVariableNames {

    // Process instance name, e.g. "Approving adding Webmaster to JoeDoe". [String]
    public static final String VARIABLE_PROCESS_INSTANCE_NAME = "processInstanceName";

    // When the process instance was started. [java.util.Date]
    public static final String VARIABLE_START_TIME = "startTime";

    // OID of task related to the process instance. [String]
    public static final String VARIABLE_MIDPOINT_TASK_OID = "midPointTaskOid";

    // Java class name of the change processor (the same as wf:changeProcessor task property) [String]
    public static final String VARIABLE_MIDPOINT_CHANGE_PROCESSOR = "midPointChangeProcessor";

    // Java class name of the process wrapper (the same as wf:processWrapper task property) [String]
    public static final String VARIABLE_MIDPOINT_PROCESS_WRAPPER = "midPointProcessWrapper";

    // OID of the user who requested the particular operation (e.g. adding of a role to another user).
    // Used e.g. for searching for process instances requested by particular user. [String]
    public static final String VARIABLE_MIDPOINT_REQUESTER_OID = "midPointRequesterOid";

    // OID of the object (typically, a user) that is being changed within the operation. [String]
    public static final String VARIABLE_MIDPOINT_OBJECT_OID = "midPointObjectOid";

    // Object that is about to be added (for ADD operation). [ObjectType]
    public static final String VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED = "midPointObjectToBeAdded";

    // XML representation of the delta to be approved. (Note that technically a process
    // can approve more deltas; if necessary, this variable would have to be changed.)
    // [StringHolder]
    public static final String VARIABLE_MIDPOINT_DELTA = "midPointDelta";

    // Variable reflecting the process status, like "your request was approved by
    // engineering group, and is being sent to the management". Stored into wfStatus task extension property.
    // [String]
    public static final String VARIABLE_MIDPOINT_STATE = "midPointState";

    // Object that provides various utility methods for use in processes, e.g. getApprover(RoleType r). [ActivitiUtil]
    public static final String VARIABLE_UTIL = "util";

    // Basic decision returned from a workflow process.
    // for most work items it is simple __APPROVED__ or __REJECTED__, but in principle this can be any string value
    public static final String VARIABLE_WF_ANSWER = "wfAnswer";

    // Basic decision returned from a work item.
    // for most work items it is simple __APPROVED__ or __REJECTED__, but in principle this can be any string value
    public static final String FORM_FIELD_DECISION = "[H]decision";

    public static final String DECISION_APPROVED = "__APPROVED__";
    public static final String DECISION_REJECTED = "__REJECTED__";

    public static String approvalStringValue(Boolean approved) {
        if (approved == null) {
            return null;
        } else {
            return approved ? DECISION_APPROVED : DECISION_REJECTED;
        }
    }

    public static Boolean approvalBooleanValue(String decision) {
        if (DECISION_APPROVED.equals(decision)) {
            return true;
        } else if (DECISION_REJECTED.equals(decision)) {
            return false;
        } else {
            return null;
        }
    }

    public static boolean isApproved(String decision) {
        return DECISION_APPROVED.equals(decision);
    }

    // Comment related to that decision - set by user task (form). [String]
    // this value is put into audit record, so its advisable to use this particular name
    public static final String FORM_FIELD_COMMENT = "comment";

    public static final String FORM_BUTTON_PREFIX = "[B]";

    public static final String VARIABLE_MODEL_CONTEXT = "modelContext";

    // A signal that the process instance is being stopped. Used e.g. to suppress propagation of exceptions
    // occurring in the process instance end listener.
    // [Boolean]
    public static final String VARIABLE_MIDPOINT_IS_PROCESS_INSTANCE_STOPPING = "midPointIsProcessInstanceStopping";

}
