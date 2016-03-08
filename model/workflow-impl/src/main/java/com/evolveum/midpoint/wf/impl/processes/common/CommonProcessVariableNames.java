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

package com.evolveum.midpoint.wf.impl.processes.common;

/**
 * @author mederly
 */
public class CommonProcessVariableNames {

    // [String]
    // Process instance name, e.g. "Approving adding Webmaster to JoeDoe".
	// Used for diagnostic purposes.
    public static final String VARIABLE_PROCESS_INSTANCE_NAME = "processInstanceName";

    // When the process instance was started. [java.util.Date]
    public static final String VARIABLE_START_TIME = "startTime";

    // OID of task related to the process instance. [String]
    public static final String VARIABLE_MIDPOINT_TASK_OID = "midPointTaskOid";

    // Java class name of the change processor (the same as wf:changeProcessor task property) [String]
    public static final String VARIABLE_MIDPOINT_CHANGE_PROCESSOR = "midPointChangeProcessor";

    // OID of the user who requested the particular operation.
    // Used e.g. for searching for process instances requested by particular user. [String]
    public static final String VARIABLE_MIDPOINT_REQUESTER_OID = "requesterOid";

    // Requester - OID + name + perhaps additional information [LightweightObjectRef]
    // Pure OID is kept in order to allow searching.
    public static final String VARIABLE_REQUESTER_REF = "requesterRef";


    // OID of the object (typically, a user) that is being changed within the operation. [String]
    // In some cases (e.g. for PrimaryChangeProcessor) the OID is determined clearly.
    // In other situations, e.g. for GeneralChangeProcessor there must be a code that provides
    // this information. In some cases, there may be no OID - e.g. when an object is yet to be created.
    //
    // TODO think about storing also object class (currently we fetch an object from the repo as "ObjectType.class" but that's far from ideal).
    public static final String VARIABLE_MIDPOINT_OBJECT_OID = "midPointObjectOid";

    // Object that provides various utility methods for use in processes, e.g. getApprover(RoleType r). [ActivitiUtil]
    public static final String VARIABLE_UTIL = "util";

    // Basic decision returned from a work item.
    // for most work items it is simple __APPROVED__ or __REJECTED__, but in principle this can be any string value
    public static final String FORM_FIELD_DECISION = "[H]decision";

    // Comment related to that decision - set by user task (form). [String]
    // this value is put into audit record, so its advisable to use this particular name
    public static final String FORM_FIELD_COMMENT = "comment";

    public static final String FORM_BUTTON_PREFIX = "[B]";

    // A signal that the process instance is being stopped. Used e.g. to suppress propagation of exceptions
    // occurring in the process instance end listener.
    // [Boolean]
    public static final String VARIABLE_MIDPOINT_IS_PROCESS_INSTANCE_STOPPING = "midPointIsProcessInstanceStopping";

    // Name of process interface bean (ProcessMidPointInterface implementation) that is related to this process [String]
    public static final String VARIABLE_MIDPOINT_PROCESS_INTERFACE_BEAN_NAME = "midPointProcessInterfaceBeanName";

    // Object of the operation - if can be specified like this [LightweightObjectRef]
    public static final String VARIABLE_OBJECT_REF = "objectRef";

    // Target of the operation - if any [LightweightObjectRef]
    public static final String VARIABLE_TARGET_REF = "targetRef";

}
