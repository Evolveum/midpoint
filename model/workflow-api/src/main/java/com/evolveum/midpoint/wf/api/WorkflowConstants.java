/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

/**
 * @author mederly
 */
public class WorkflowConstants {

	public static final String NS_WORKFLOW_TRIGGER_PREFIX = SchemaConstants.NS_WORKFLOW + "/trigger";

	// don't forget to add audit property names into localization properties file
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
