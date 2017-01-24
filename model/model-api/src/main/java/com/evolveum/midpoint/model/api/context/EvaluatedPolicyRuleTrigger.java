/**
 * Copyright (c) 2016-2017 Evolveum
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

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Description of a situation that caused a trigger of the policy rule.
 * 
 * @author semancik
 */
public class EvaluatedPolicyRuleTrigger implements DebugDumpable {
	
	@NotNull private final PolicyConstraintKindType constraintKind;
	@NotNull private final AbstractPolicyConstraintType constraint;
	private final String message;
	private final EvaluatedAssignment conflictingAssignment;
	@NotNull private final Collection<EvaluatedPolicyRule> sourceRules;
	
	public EvaluatedPolicyRuleTrigger(@NotNull PolicyConstraintKindType constraintKind, @NotNull AbstractPolicyConstraintType constraint, String message) {
		this.constraintKind = constraintKind;
		this.constraint = constraint;
		this.message = message;
		this.conflictingAssignment = null;
		this.sourceRules = Collections.emptyList();
	}
	
	public EvaluatedPolicyRuleTrigger(@NotNull PolicyConstraintKindType constraintKind, @NotNull AbstractPolicyConstraintType constraint, 
			String message, EvaluatedAssignment conflictingAssignment) {
		this.constraintKind = constraintKind;
		this.constraint = constraint;
		this.message = message;
		this.conflictingAssignment = conflictingAssignment;
		this.sourceRules = Collections.emptyList();
	}

	public EvaluatedPolicyRuleTrigger(@NotNull PolicyConstraintKindType constraintKind, @NotNull AbstractPolicyConstraintType constraint,
			String message, @NotNull Collection<EvaluatedPolicyRule> sourceRules) {
		this.constraintKind = constraintKind;
		this.constraint = constraint;
		this.message = message;
		this.conflictingAssignment = null;
		this.sourceRules = sourceRules;
	}

	/**
	 * The kind of constraint that caused the trigger.
	 * @return
	 */
	@NotNull
	public PolicyConstraintKindType getConstraintKind() {
		return constraintKind;
	}

	@NotNull
	public AbstractPolicyConstraintType getConstraint() {
		return constraint;
	}

	/**
	 * Human-readable message associated with this trigger. The message
	 * explain why the rule was triggered. It can be used
	 * in the logs, as an error message, in the audit trail
	 * and so on.
	 */
	public String getMessage() {
		return message;
	}

	public <F extends FocusType> EvaluatedAssignment<F> getConflictingAssignment() {
		return conflictingAssignment;
	}

	@NotNull
	public Collection<EvaluatedPolicyRule> getSourceRules() {
		return sourceRules;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof EvaluatedPolicyRuleTrigger))
			return false;
		EvaluatedPolicyRuleTrigger that = (EvaluatedPolicyRuleTrigger) o;
		return constraintKind == that.constraintKind &&
				Objects.equals(constraint, that.constraint) &&
				Objects.equals(message, that.message) &&
				Objects.equals(conflictingAssignment, that.conflictingAssignment) &&
				Objects.equals(sourceRules, that.sourceRules);
	}

	@Override
	public int hashCode() {
		return Objects.hash(constraintKind, constraint, message, conflictingAssignment, sourceRules);
	}

	@Override
	public String toString() {
		return "EvaluatedPolicyRuleTrigger(" + constraintKind + ": " + message + ")";
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "EvaluatedPolicyRuleTrigger", indent);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "constraintKind", constraintKind, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "constraint", constraint, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "message", message, indent + 1);
		// cannot debug dump conflicting assignment in detail, as we might go into infinite loop
		// (the assignment could have evaluated rule that would point to another conflicting assignment, which
		// could point back to this rule)
		DebugUtil.debugDumpWithLabelToStringLn(sb, "conflictingAssignment", conflictingAssignment, indent + 1);
		// the same here
		DebugUtil.debugDumpWithLabel(sb, "sourceRules", sourceRules.stream().map(Object::toString).collect(Collectors.toList()), indent + 1);
		return sb.toString();
	}

}
