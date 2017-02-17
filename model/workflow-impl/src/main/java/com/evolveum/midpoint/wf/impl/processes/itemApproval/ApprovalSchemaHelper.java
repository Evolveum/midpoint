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
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
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
		WfContextUtil.orderAndRenumberLevels(schema);
		for (ApprovalLevelType level : schema.getLevel()) {
			prepareLevel(level, relationResolver, referenceResolver);
		}
	}

	public void prepareLevel(ApprovalLevelType level, RelationResolver relationResolver, ReferenceResolver referenceResolver) {
		try {
			// resolves filters in approvers
			List<ObjectReferenceType> resolvedApprovers = new ArrayList<>();
			for (ObjectReferenceType ref : level.getApproverRef()) {
				resolvedApprovers.addAll(referenceResolver.resolveReference(ref, "approver ref"));
			}
			// resolves approver relations
			resolvedApprovers.addAll(relationResolver.getApprovers(level.getApproverRelation()));
			level.getApproverRef().clear();
			level.getApproverRef().addAll(resolvedApprovers);
			level.getApproverRelation().clear();

			// default values
			if (level.getOutcomeIfNoApprovers() == null) {
				level.setOutcomeIfNoApprovers(ApprovalLevelOutcomeType.REJECT);
			}
			if (level.getGroupExpansion() == null) {
				level.setGroupExpansion(GroupExpansionType.BY_CLAIMING_WORK_ITEMS);
			}
		} catch (ExpressionEvaluationException | ObjectNotFoundException | SchemaException e) {
			throw new SystemException("Couldn't prepare approval schema for execution: " + e.getMessage(), e); // todo propagate these exceptions?
		}
	}

	// should be called only after preparation!
	public boolean shouldBeSkipped(ApprovalSchemaType schema) {
		return schema.getLevel().stream().allMatch(this::shouldBeSkipped);
	}

	private boolean shouldBeSkipped(ApprovalLevelType level) {
		if (!level.getApproverRelation().isEmpty()) {
			throw new IllegalStateException("Schema level was not prepared correctly; contains unresolved approver relations: " + level);
		}
		return level.getOutcomeIfNoApprovers() == ApprovalLevelOutcomeType.SKIP && level.getApproverRef().isEmpty() && level.getApproverExpression().isEmpty();
	}
}
