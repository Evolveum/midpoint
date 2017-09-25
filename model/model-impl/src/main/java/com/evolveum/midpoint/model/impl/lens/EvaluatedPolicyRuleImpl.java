/*
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
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author semancik
 *
 */
public class EvaluatedPolicyRuleImpl implements EvaluatedPolicyRule {
	private static final long serialVersionUID = 1L;

	@NotNull private final PolicyRuleType policyRuleType;
	private final Collection<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();
	private final Collection<PolicyExceptionType> policyExceptions = new ArrayList<>();

	/**
	 * Information about exact place where the rule was found. This can be important for rules that are
	 * indirectly attached to an assignment.
	 *
	 * An example: Let Engineer induce Employee which conflicts with Contractor. The SoD rule is attached
	 * to Employee. But let the user have assignments for Engineer and Contractor only. When evaluating
	 * Engineer assignment, we find a (indirectly attached) SoD rule. But we need to know it came from Employee.
	 * This is what assignmentPath (Engineer->Employee->(maybe some metarole)->rule) and directOwner (Employee) are for.
	 *
	 * For global policy rules, assignmentPath is the path to the target object that matched global policy rule.
	 */
	@Nullable private final AssignmentPath assignmentPath;
	@Nullable private final ObjectType directOwner;
	private final transient PrismContext prismContext;     // only for debugDump - if null, nothing serious happens

	public EvaluatedPolicyRuleImpl(@NotNull PolicyRuleType policyRuleType, @Nullable AssignmentPath assignmentPath,
			PrismContext prismContext) {
		this.policyRuleType = policyRuleType;
		this.assignmentPath = assignmentPath;
		this.prismContext = prismContext;
		this.directOwner = computeDirectOwner();
	}

	private ObjectType computeDirectOwner() {
		if (assignmentPath == null) {
			return null;
		}
		List<ObjectType> roots = assignmentPath.getFirstOrderChain();
		return roots.isEmpty() ? null : roots.get(roots.size()-1);
	}

	@Override
	public String getName() {
		return policyRuleType.getName();
	}

	@Override
	public PolicyRuleType getPolicyRule() {
		return policyRuleType;
	}

	@Nullable
	@Override
	public AssignmentPath getAssignmentPath() {
		return assignmentPath;
	}

	@Nullable
	@Override
	public ObjectType getDirectOwner() {
		return directOwner;
	}

	@Override
	public PolicyConstraintsType getPolicyConstraints() {
		return policyRuleType.getPolicyConstraints();
	}

	@NotNull
	@Override
	public Collection<EvaluatedPolicyRuleTrigger<?>> getTriggers() {
		return triggers;
	}

	@NotNull
	@Override
	public Collection<EvaluatedPolicyRuleTrigger<?>> getAllTriggers() {
		List<EvaluatedPolicyRuleTrigger<?>> rv = new ArrayList<>();
		for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
			if (trigger instanceof EvaluatedSituationTrigger) {
				rv.addAll(((EvaluatedSituationTrigger) trigger).getAllTriggers());
			} else {
				rv.add(trigger);
			}
		}
		return rv;
	}

	void addTriggers(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
		this.triggers.addAll(triggers);
	}

	@NotNull
	@Override
	public Collection<PolicyExceptionType> getPolicyExceptions() {
		return policyExceptions;
	}

	void addPolicyException(PolicyExceptionType exception) {
		policyExceptions.add(exception);
	}

	@Override
	public PolicyActionsType getActions() {
		return policyRuleType.getPolicyActions();
	}

	// TODO rewrite this method
	@Override
	public String getPolicySituation() {
		// TODO default situations depending on getTriggeredConstraintKinds
		if (policyRuleType.getPolicySituation() != null) {
			return policyRuleType.getPolicySituation();
		}

		if (!triggers.isEmpty()) {
			EvaluatedPolicyRuleTrigger firstTrigger = triggers.iterator().next();
			if (firstTrigger instanceof EvaluatedSituationTrigger) {
				Collection<EvaluatedPolicyRule> sourceRules = ((EvaluatedSituationTrigger) firstTrigger).getSourceRules();
				if (!sourceRules.isEmpty()) {	// should be always the case
					return sourceRules.iterator().next().getPolicySituation();
				}
			}
			PolicyConstraintKindType constraintKind = firstTrigger.getConstraintKind();
			PredefinedPolicySituation predefinedSituation = PredefinedPolicySituation.get(constraintKind);
			if (predefinedSituation != null) {
				return predefinedSituation.getUrl();
			}
		}

		PolicyConstraintsType policyConstraints = getPolicyConstraints();
		return getSituationFromConstraints(policyConstraints);
	}

	@Nullable
	private String getSituationFromConstraints(PolicyConstraintsType policyConstraints) {
		if (!policyConstraints.getExclusion().isEmpty()) {
			return PredefinedPolicySituation.EXCLUSION_VIOLATION.getUrl();
		} else if (!policyConstraints.getMinAssignees().isEmpty()) {
			return PredefinedPolicySituation.UNDERASSIGNED.getUrl();
		} else if (!policyConstraints.getMaxAssignees().isEmpty()) {
			return PredefinedPolicySituation.OVERASSIGNED.getUrl();
		} else if (!policyConstraints.getModification().isEmpty()) {
			return PredefinedPolicySituation.MODIFIED.getUrl();
		} else if (!policyConstraints.getAssignment().isEmpty()) {
			return PredefinedPolicySituation.ASSIGNMENT_MODIFIED.getUrl();
		} else if (!policyConstraints.getObjectTimeValidity().isEmpty()) {
			return PredefinedPolicySituation.OBJECT_TIME_VALIDITY.getUrl();
		} else if (!policyConstraints.getAssignmentTimeValidity().isEmpty()) {
			return PredefinedPolicySituation.ASSIGNMENT_TIME_VALIDITY.getUrl();
		} else if (!policyConstraints.getHasAssignment().isEmpty()) {
			return PredefinedPolicySituation.HAS_ASSIGNMENT.getUrl();
		} else if (!policyConstraints.getHasNoAssignment().isEmpty()) {
			return PredefinedPolicySituation.HAS_NO_ASSIGNMENT.getUrl();
		} else if (!policyConstraints.getObjectState().isEmpty()) {
			return PredefinedPolicySituation.OBJECT_STATE.getUrl();
		} else if (!policyConstraints.getAssignmentState().isEmpty()) {
			return PredefinedPolicySituation.ASSIGNMENT_STATE.getUrl();
		}
		for (TransitionPolicyConstraintType tc : policyConstraints.getTransition()) {
			String s = getSituationFromConstraints(tc.getConstraints());
			if (s != null) {
				return s;
			}
		}
		for (PolicyConstraintsType subconstraints : policyConstraints.getAnd()) {
			String s = getSituationFromConstraints(subconstraints);
			if (s != null) {
				return s;
			}
		}
		// desperate attempt (might be altogether wrong)
		for (PolicyConstraintsType subconstraints : policyConstraints.getOr()) {
			String s = getSituationFromConstraints(subconstraints);
			if (s != null) {
				return s;
			}
		}
		// "not" will not be used
		return null;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "EvaluatedPolicyRule " + (getName() != null ? getName() + " " : "") + "(triggers: " + triggers.size() + ")", indent);
		DebugUtil.debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
		DebugUtil.debugDumpLabelLn(sb, "policyRuleType", indent + 1);
		DebugUtil.indentDebugDump(sb, indent + 2);
		PrismPrettyPrinter.debugDumpValue(sb, indent + 2, policyRuleType, prismContext, PolicyRuleType.COMPLEX_TYPE, PrismContext.LANG_XML);
		DebugUtil.debugDumpWithLabelLn(sb, "assignmentPath", assignmentPath, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "triggers", triggers, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "directOwner", ObjectTypeUtil.toShortString(directOwner), indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "rootObjects", assignmentPath != null ? String.valueOf(assignmentPath.getFirstOrderChain()) : null, indent + 1);
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof EvaluatedPolicyRuleImpl))
			return false;
		EvaluatedPolicyRuleImpl that = (EvaluatedPolicyRuleImpl) o;
		return java.util.Objects.equals(policyRuleType, that.policyRuleType) &&
				Objects.equals(assignmentPath, that.assignmentPath) &&
				Objects.equals(triggers, that.triggers) &&
				Objects.equals(policyExceptions, that.policyExceptions) &&
				Objects.equals(directOwner, that.directOwner);
	}

	@Override
	public int hashCode() {
		return Objects.hash(policyRuleType, assignmentPath, triggers, policyExceptions, directOwner);
	}

	@Override
	public String toString() {
		return "EvaluatedPolicyRuleImpl(" + getName() + ")";
	}

	@Override
	public boolean isGlobal() {
		// in the future we might employ special flag for this (if needed)
		return policyRuleType instanceof GlobalPolicyRuleType;
	}

	@Override
	public String toShortString() {
		StringBuilder sb = new StringBuilder();
		if (isGlobal()) {
			sb.append("G:");
		}
		if (getName() != null) {
			sb.append(getName()).append(":");
		}
		sb.append("(").append(PolicyRuleTypeUtil.toShortString(getPolicyConstraints())).append(")");
		sb.append("->");
		sb.append("(").append(PolicyRuleTypeUtil.toShortString(getActions())).append(")");
		if (!getTriggers().isEmpty()) {
			sb.append(" # {T:");
			sb.append(getTriggers().stream().map(EvaluatedPolicyRuleTrigger::toDiagShortcut)
					.collect(Collectors.joining(", ")));
			sb.append("}");
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<TreeNode<LocalizableMessage>> extractMessages() {
		TreeNode<LocalizableMessage> root = new TreeNode<>();
		for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
			createMessageTreeNode(root, trigger);
		}
		return root.getChildren();
	}

	private void createMessageTreeNode(TreeNode<LocalizableMessage> root, EvaluatedPolicyRuleTrigger<?> trigger) {
		PolicyConstraintPresentationType presentation = trigger.getConstraint().getPresentation();
		boolean hidden = presentation != null && Boolean.TRUE.equals(presentation.isHidden());
		boolean isFinal = presentation != null && Boolean.TRUE.equals(presentation.isFinal());
		if (!hidden) {
			TreeNode<LocalizableMessage> newNode = new TreeNode<>();
			newNode.setUserObject(trigger.getMessage());
			root.add(newNode);
			root = newNode;
		}
		if (!isFinal) {
			for (EvaluatedPolicyRuleTrigger<?> innerTrigger : trigger.getInnerTriggers()) {
				createMessageTreeNode(root, innerTrigger);
			}
		}
	}

	/**
	 * Honors "final" but not "hidden" flag.
	 */

	@Override
	public EvaluatedPolicyRuleType toEvaluatedPolicyRuleType(boolean includeAssignmentsContent, boolean respectFinalFlag) {
		EvaluatedPolicyRuleType rv = new EvaluatedPolicyRuleType();
		//rv.setPolicyRule(policyRuleType);			// DO NOT use this, in order to avoid large data in assignments
		rv.setRuleName(getName());
		if (assignmentPath != null) {
			rv.setAssignmentPath(assignmentPath.toAssignmentPathType(includeAssignmentsContent));
		}
		if (directOwner != null) {
			rv.setDirectOwnerRef(ObjectTypeUtil.createObjectRef(directOwner));
			rv.setDirectOwnerDisplayName(ObjectTypeUtil.getDisplayName(directOwner));
		}
		triggers.forEach(t -> rv.getTrigger().add(t.toEvaluatedPolicyRuleTriggerType(includeAssignmentsContent, respectFinalFlag)));
		return rv;
	}

}
