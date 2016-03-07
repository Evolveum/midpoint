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

package com.evolveum.midpoint.wf.api;

import javax.xml.namespace.QName;

/**
 * Names of workflow-related items that are stored in task extensions.
 * Items in this class are common for all wf-related tasks.
 * Specific items are provided by similar classes in the "processors" package.
 *
 * @author mederly
 */
public class WfTaskExtensionItemsNames {

    public static final String WORKFLOW_EXTENSION_NS = "http://midpoint.evolveum.com/model/workflow/extension-3";

    // Contains relevant activiti process instance ID (when known).
    public static final QName WFPROCESSID_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "processInstanceId");

    // Just a flag whether the process instance related to the task has already finished (used e.g. to determine if the shadowing handler should end or not).
    public static final QName WFPROCESS_INSTANCE_FINISHED_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "processInstanceFinished");

    // Contains name of class that manages this particular model operation ("change", thus "change processor").
    public static final QName WFCHANGE_PROCESSOR_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "changeProcessor");

    // Contains history of process status messages. Such messages can contain any process-related messages that
    // should be visible to the user (e.g. "your request was approved by engineering group, and is being sent
    // to the management"). For simple processes, the status has mostly the value "Workflow process instance
    // has proceeded". In order to be able to sort these status messages chronologically, each is prefixed
    // by a timestamp (long value + formatted string value). Storing of these wfStatus values can be simply
    // turned off in WfTaskUtil class.
    public static final QName WFSTATUS_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "status");

    // String representation of recent process instance information (for diagnostic purposes).
    public static final QName WFLAST_DETAILS_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "lastDetails");

    /**
     * This property is put into model task (i.e. not wf root task). It points to the
     * wf root task - that's important e.g. in cases when wf root task is not a subtask
     * of the model task.
     */
    public static final QName WFROOT_TASK_OID_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "rootTaskOid");
}
