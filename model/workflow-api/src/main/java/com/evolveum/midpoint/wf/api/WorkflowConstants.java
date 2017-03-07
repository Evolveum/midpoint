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

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

/**
 * @author mederly
 */
public class WorkflowConstants {

	public static final String NS_WORKFLOW_TRIGGER_PREFIX = SchemaConstants.NS_WORKFLOW + "/trigger";
	
	public static final String AUDIT_COMMENT = "wf.comment";
	public static final String AUDIT_OBJECT = "wf.object";
	public static final String AUDIT_TARGET = "wf.target";
	public static final String AUDIT_ORIGINAL_ASSIGNEE = "wf.originalAssignee";
	public static final String AUDIT_CURRENT_ASSIGNEE = "wf.currentAssignee";
	public static final String AUDIT_STAGE_NUMBER = "wf.stageNumber";
	public static final String AUDIT_STAGE_COUNT = "wf.stageCount";
	public static final String AUDIT_STAGE_NAME = "wf.stageName";
	public static final String AUDIT_STAGE_DISPLAY_NAME = "wf.stageDisplayName";
	public static final String AUDIT_ESCALATION_LEVEL_NUMBER = "wf.escalationLevelNumber";
	public static final String AUDIT_ESCALATION_LEVEL_NAME = "wf.escalationLevelName";
	public static final String AUDIT_ESCALATION_LEVEL_DISPLAY_NAME = "wf.escalationLevelDisplayName";
	public static final String AUDIT_WORK_ITEM_ID = "wf.workItemId";
	public static final String AUDIT_PROCESS_INSTANCE_ID = "wf.processInstanceId";
	public static final String AUDIT_REQUESTER_COMMENT = "wf.requesterComment";
	public static final String AUDIT_CAUSE_TYPE = "wf.causeType";
	public static final String AUDIT_CAUSE_NAME = "wf.causeName";
	public static final String AUDIT_CAUSE_DISPLAY_NAME = "wf.causeDisplayName";
}
