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

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author mederly
 */
public class EvaluatedExclusionTrigger extends EvaluatedPolicyRuleTrigger<ExclusionPolicyConstraintType> {

	private final EvaluatedAssignment conflictingAssignment;

	public EvaluatedExclusionTrigger(@NotNull ExclusionPolicyConstraintType constraint,
			String message, EvaluatedAssignment conflictingAssignment) {
		super(PolicyConstraintKindType.EXCLUSION, constraint, message);
		this.conflictingAssignment = conflictingAssignment;
	}

	public <F extends FocusType> EvaluatedAssignment<F> getConflictingAssignment() {
		return conflictingAssignment;
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
		DebugUtil.debugDumpWithLabelToString(sb, "conflictingAssignment", conflictingAssignment, indent);
	}
}
