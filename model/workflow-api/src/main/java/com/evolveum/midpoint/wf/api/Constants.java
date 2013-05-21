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
 * @author mederly
 */
public class Constants {
    public static final String WORKFLOW_EXTENSION_NS = "http://midpoint.evolveum.com/model/workflow/extension-2";
    public static final QName WFPROCESS_INSTANCE_FINISHED_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "processInstanceFinished");
    public static final QName WFRESULTING_DELTA_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "resultingDelta");
    public static final QName WFDELTA_TO_PROCESS_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "deltaToProcess");
    public static final QName WFPROCESSID_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "processInstanceId");
    public static final QName WFCHANGE_PROCESSOR_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "changeProcessor");
    public static final QName WFPROCESS_WRAPPER_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "processWrapper");
    public static final QName WFLASTVARIABLES_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "lastVariables");
    public static final QName WFLAST_DETAILS_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "lastDetails");
    public static final QName WFSTATUS_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "status");
}
