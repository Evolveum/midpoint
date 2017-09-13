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

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author mederly
 */
public class EvaluatedExclusionTrigger extends EvaluatedPolicyRuleTrigger<ExclusionPolicyConstraintType> {

	@NotNull private final EvaluatedAssignment conflictingAssignment;
	private final ObjectType conflictingTarget;
	private final AssignmentPath conflictingPath;

	public EvaluatedExclusionTrigger(@NotNull ExclusionPolicyConstraintType constraint,
			LocalizableMessage message, @NotNull EvaluatedAssignment conflictingAssignment,
			ObjectType thisTarget, ObjectType conflictingTarget, AssignmentPath thisPath, AssignmentPath conflictingPath) {
		super(PolicyConstraintKindType.EXCLUSION, constraint, message);
		this.conflictingAssignment = conflictingAssignment;
		this.conflictingTarget = conflictingTarget;
		this.conflictingPath = conflictingPath;
	}

	public <F extends FocusType> EvaluatedAssignment<F> getConflictingAssignment() {
		return conflictingAssignment;
	}

	public AssignmentPath getConflictingPath() {
		return conflictingPath;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof EvaluatedExclusionTrigger))
			return false;
		if (!super.equals(o))
			return false;
		EvaluatedExclusionTrigger that = (EvaluatedExclusionTrigger) o;
		return Objects.equals(conflictingAssignment, that.conflictingAssignment);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), conflictingAssignment);
	}

	@Override
	protected void debugDumpSpecific(StringBuilder sb, int indent) {
		// cannot debug dump conflicting assignment in detail, as we might go into infinite loop
		// (the assignment could have evaluated rule that would point to another conflicting assignment, which
		// could point back to this rule)
		DebugUtil.debugDumpWithLabelToStringLn(sb, "conflictingAssignment", conflictingAssignment, indent);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "conflictingPath", conflictingPath, indent);
	}

	@Override
	public EvaluatedExclusionTriggerType toEvaluatedPolicyRuleTriggerType(EvaluatedPolicyRule owningRule) {
		EvaluatedExclusionTriggerType rv = new EvaluatedExclusionTriggerType();
		fillCommonContent(rv, owningRule);
		rv.setConflictingObjectRef(ObjectTypeUtil.createObjectRef(conflictingTarget));
		rv.setConflictingObjectDisplayName(ObjectTypeUtil.getDisplayName(conflictingTarget));
		if (conflictingPath != null) {
			rv.setConflictingObjectPath(conflictingPath.toAssignmentPathType());
		}
		rv.setConflictingAssignment(conflictingAssignment.getAssignmentType());
		return rv;
	}
}
