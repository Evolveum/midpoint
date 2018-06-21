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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class ApprovalSchemaHelper {

	public void prepareSchema(ApprovalSchemaType schema, RelationResolver relationResolver, ReferenceResolver referenceResolver) {
		WfContextUtil.normalizeStages(schema);
		for (ApprovalStageDefinitionType stageDef : schema.getStage()) {
			prepareStage(stageDef, relationResolver, referenceResolver);
		}
	}

	public void prepareStage(ApprovalStageDefinitionType stageDef, RelationResolver relationResolver, ReferenceResolver referenceResolver) {
		try {
			// resolves filters in approvers
			List<ObjectReferenceType> resolvedApprovers = new ArrayList<>();
			for (ObjectReferenceType ref : stageDef.getApproverRef()) {
				resolvedApprovers.addAll(referenceResolver.resolveReference(ref, "approver ref"));
			}
			// resolves approver relations
			resolvedApprovers.addAll(relationResolver.getApprovers(stageDef.getApproverRelation()));
			stageDef.getApproverRef().clear();
			stageDef.getApproverRef().addAll(resolvedApprovers);
			stageDef.getApproverRelation().clear();

			// default values
			if (stageDef.getOutcomeIfNoApprovers() == null) {
				stageDef.setOutcomeIfNoApprovers(ApprovalLevelOutcomeType.REJECT);
			}
			if (stageDef.getGroupExpansion() == null) {
				stageDef.setGroupExpansion(GroupExpansionType.BY_CLAIMING_WORK_ITEMS);
			}
		} catch (ExpressionEvaluationException | ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException e) {
			throw new SystemException("Couldn't prepare approval schema for execution: " + e.getMessage(), e); // todo propagate these exceptions?
		}
	}

	// should be called only after preparation!
	public boolean shouldBeSkipped(ApprovalSchemaType schema) {
		return schema.getStage().stream().allMatch(this::shouldBeSkipped);
	}

	private boolean shouldBeSkipped(ApprovalStageDefinitionType stage) {
		if (!stage.getApproverRelation().isEmpty()) {
			throw new IllegalStateException("Schema stage was not prepared correctly; contains unresolved approver relations: " + stage);
		}
		return stage.getOutcomeIfNoApprovers() == ApprovalLevelOutcomeType.SKIP && stage.getApproverRef().isEmpty() && stage.getApproverExpression().isEmpty();
	}
}
