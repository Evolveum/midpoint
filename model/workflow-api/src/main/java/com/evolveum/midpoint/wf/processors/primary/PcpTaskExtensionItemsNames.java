/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.processors.primary;

import com.evolveum.midpoint.wf.api.WfTaskExtensionItemsNames;

import javax.xml.namespace.QName;

/**
 * Names of workflow-related items that are stored in task extensions,
 * specific to primary change processor.
 *
 * @author mederly
 */
public class PcpTaskExtensionItemsNames {

    // Contains deltaIn(i), i.e. delta(s) that has to be approved.
    public static final QName WFDELTAS_TO_PROCESS_PROPERTY_NAME = new QName(WfTaskExtensionItemsNames.WORKFLOW_EXTENSION_NS, "deltasToProcess");

    // Contains deltaOut(i), i.e. delta(s) that are the result of the approval process (most common situation is
    // that deltaOut(i) = either deltaIn(i) (if approved), or null/empty delta (if rejected).
    public static final QName WFRESULTING_DELTAS_PROPERTY_NAME = new QName(WfTaskExtensionItemsNames.WORKFLOW_EXTENSION_NS, "resultingDeltas");

    // Contains a set of approvers who approved the delta(s). The change aspect should put here those approvers that
    // will be stored into approvers list in metadata when the operation is really executed.
    public static final QName WFAPPROVED_BY_REFERENCE_NAME = new QName(WfTaskExtensionItemsNames.WORKFLOW_EXTENSION_NS, "approvedBy");

    // Name of class that provides an interface between midPoint and activiti process.
    public static final QName WFPRIMARY_CHANGE_ASPECT_NAME = new QName(WfTaskExtensionItemsNames.WORKFLOW_EXTENSION_NS, "primaryChangeAspect");
}
