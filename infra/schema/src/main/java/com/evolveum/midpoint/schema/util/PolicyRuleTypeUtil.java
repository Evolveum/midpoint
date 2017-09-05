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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.HeteroComparator;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.Objects;
import java.util.function.Consumer;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType.*;

/**
 * @author mederly
 */
public class PolicyRuleTypeUtil {

	private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleTypeUtil.class);

	private static final char JOIN_AND = '*';
	private static final char JOIN_OR = '|';

	private static Map<String, String> CONSTRAINT_NAMES = new HashMap<>();
	static {
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_MIN_ASSIGNEES.getLocalPart(), "min");
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_MAX_ASSIGNEES.getLocalPart(), "max");
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_ASSIGNMENT.getLocalPart(), "amod");
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_MODIFICATION.getLocalPart(), "omod");
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_OBJECT_STATE.getLocalPart(), "ostate");
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_ASSIGNMENT_STATE.getLocalPart(), "astate");
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_EXCLUSION.getLocalPart(), "exc");
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_HAS_ASSIGNMENT.getLocalPart(), "hasass");
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_HAS_NO_ASSIGNMENT.getLocalPart(), "noass");
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_TIME_VALIDITY.getLocalPart(), "time");
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_SITUATION.getLocalPart(), "sit");
	}

	public static String toShortString(PolicyConstraintsType constraints) {
		return toShortString(constraints, JOIN_AND);
	}

	public static String toShortString(PolicyConstraintsType constraints, char join) {
		if (constraints == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		for (JAXBElement<AbstractPolicyConstraintType> primitive : toPrimitiveConstraintsList(constraints, false)) {
			QName name = primitive.getName();
			String abbreviation = CONSTRAINT_NAMES.get(name.getLocalPart());
			if (sb.length() > 0) {
				sb.append(join);
			}
			sb.append(abbreviation != null ? abbreviation : name.getLocalPart());
		}
		for (PolicyConstraintsType component : constraints.getAnd()) {
			if (sb.length() > 0) {
				sb.append(join);
			}
			sb.append('(');
			sb.append(toShortString(component, JOIN_AND));
			sb.append(')');
		}
		for (PolicyConstraintsType component : constraints.getOr()) {
			if (sb.length() > 0) {
				sb.append(join);
			}
			sb.append('(');
			sb.append(toShortString(component, JOIN_OR));
			sb.append(')');
		}
		for (PolicyConstraintsType component : constraints.getNot()) {
			if (sb.length() > 0) {
				sb.append(join);
			}
			sb.append("!(");
			sb.append(toShortString(component, JOIN_AND));
			sb.append(')');
		}
		return sb.toString();
	}

	public static String toShortString(PolicyActionsType actions) {
		if (actions == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		if (actions.getEnforcement() != null) {
			sb.append("enforce ");
		}
		if (!actions.getApproval().isEmpty()) {
			sb.append("approve ");
		}
		if (actions.getRemediation() != null) {
			sb.append("remedy ");
		}
		if (actions.getCertification() != null) {
			sb.append("certify ");
		}
		if (!actions.getNotification().isEmpty()) {
			sb.append("notify ");
		}
		return sb.toString().trim();
	}

//	public static String toShortString(AbstractPolicyConstraintType constraint) {
//		if (constraint == null) {
//			return "null";
//		}
//		StringBuilder sb = new StringBuilder();
//		sb.append(getConstraintClassShortcut(constraint.getClass()));
//		if (constraint.getName() != null) {
//			sb.append(":").append(constraint.getName());
//		}
//		return sb.toString();
//	}

	public static String toDiagShortcut(PolicyConstraintKindType constraintKind) {
		if (constraintKind == null) {
			return "null";
		}
		switch (constraintKind) {
			case ASSIGNMENT_MODIFICATION: return "a-mod";
			case OBJECT_MODIFICATION: return "o-mod";
			case EXCLUSION: return "exc";
			case MAX_ASSIGNEES_VIOLATION: return "max";
			case MIN_ASSIGNEES_VIOLATION: return "min";
			case OBJECT_STATE: return "o-state";
			case ASSIGNMENT_STATE: return "a-state";
			case HAS_ASSIGNMENT: return "has-a";
			case HAS_NO_ASSIGNMENT: return "no-a";
			case TIME_VALIDITY: return "time";
			case SITUATION: return "sit";
			default: return constraintKind.toString();
		}
	}

	public static void visit(List<EvaluatedPolicyRuleTriggerType> triggers, Consumer<EvaluatedPolicyRuleTriggerType> visitor) {
		for (EvaluatedPolicyRuleTriggerType trigger : triggers) {
			visitor.accept(trigger);
			if (trigger instanceof EvaluatedSituationTriggerType) {
				EvaluatedSituationTriggerType situationTrigger = (EvaluatedSituationTriggerType) trigger;
				for (EvaluatedPolicyRuleType sourceRule : situationTrigger.getSourceRule()) {
					visit(sourceRule.getTrigger(), visitor);
				}
			}
		}
	}

	// Generates identifiers and references for triggers.
	// Cannot modify original objects!
	public static List<EvaluatedPolicyRuleTriggerType> pack(List<EvaluatedPolicyRuleTriggerType> baseTriggers) {
		List<EvaluatedPolicyRuleTriggerType> rv = new ArrayList<>();
		Map<EvaluatedPolicyRuleTriggerType, Integer> identifierMap = new HashMap<>();
		int id = 1;
		for (EvaluatedPolicyRuleTriggerType trigger : baseTriggers) {
			EvaluatedPolicyRuleTriggerType clone = trigger.clone();
			clone.setTriggerId(id);
			rv.add(clone);
			identifierMap.put(trigger, id);
			id++;
		}
		for (EvaluatedPolicyRuleTriggerType trigger : rv) {
			if (trigger instanceof EvaluatedSituationTriggerType) {
				EvaluatedSituationTriggerType situationTrigger = (EvaluatedSituationTriggerType) trigger;
				for (EvaluatedPolicyRuleType sourceRule : situationTrigger.getSourceRule()) {
					pack(sourceRule.getTrigger(), identifierMap);
				}
			}
		}
		return rv;
	}

	private static void pack(List<EvaluatedPolicyRuleTriggerType> triggers, Map<EvaluatedPolicyRuleTriggerType, Integer> identifierMap) {
		List<EvaluatedPolicyRuleTriggerType> packed = new ArrayList<>();
		for (EvaluatedPolicyRuleTriggerType trigger : triggers) {
			packed.add(pack(trigger, identifierMap));
			if (trigger instanceof EvaluatedSituationTriggerType) {
				EvaluatedSituationTriggerType situationTrigger = (EvaluatedSituationTriggerType) trigger;
				for (EvaluatedPolicyRuleType sourceRule : situationTrigger.getSourceRule()) {
					pack(sourceRule.getTrigger(), identifierMap);
				}
			}
		}
		triggers.clear();
		triggers.addAll(packed);
	}

	private static EvaluatedPolicyRuleTriggerType pack(EvaluatedPolicyRuleTriggerType trigger, Map<EvaluatedPolicyRuleTriggerType, Integer> identifierMap) {
		Integer identifier = identifierMap.get(trigger);
		if (identifier != null) {
			return new EvaluatedPolicyRuleTriggerType().ref(identifier);
		} else {
			LOGGER.warn("Problem while packing evaluated trigger {}: it is not present within base triggers", trigger);
			return trigger;
		}
	}

	// Replaces referenced triggers by actual values. Deletes all identifiers, so the triggers could be compared.
	// Cannot modify original objects!
	public static List<EvaluatedPolicyRuleTriggerType> unpack(List<EvaluatedPolicyRuleTriggerType> triggers) {
		List<EvaluatedPolicyRuleTriggerType> rv = CloneUtil.cloneCollectionMembers(triggers);
		unpack(rv, triggers);
		visit(rv, t -> t.triggerId(null));
		return rv;
	}

	private static void unpack(List<EvaluatedPolicyRuleTriggerType> triggers,
			List<EvaluatedPolicyRuleTriggerType> baseTriggers) {
		List<EvaluatedPolicyRuleTriggerType> unpacked = new ArrayList<>();
		for (EvaluatedPolicyRuleTriggerType trigger : triggers) {
			unpacked.add(unpack(trigger, baseTriggers));
			if (trigger instanceof EvaluatedSituationTriggerType) {
				EvaluatedSituationTriggerType situationTrigger = (EvaluatedSituationTriggerType) trigger;
				for (EvaluatedPolicyRuleType sourceRule : situationTrigger.getSourceRule()) {
					unpack(sourceRule.getTrigger(), baseTriggers);
				}
			}
		}
		triggers.clear();
		triggers.addAll(unpacked);
	}

	private static EvaluatedPolicyRuleTriggerType unpack(EvaluatedPolicyRuleTriggerType trigger,
			List<EvaluatedPolicyRuleTriggerType> baseTriggers) {
		if (trigger.getRef() == null) {
			return trigger;
		}
		EvaluatedPolicyRuleTriggerType found = findByIdentifier(baseTriggers, trigger.getRef());
		if (found == null) {
			LOGGER.warn("Problem while unpacking evaluated trigger {}: referenced trigger with id {} is not present among base triggers",
					trigger, trigger.getRef());
			return trigger;
		} else {
			return found;
		}
	}

	public static boolean canBePacked(List<EvaluatedPolicyRuleTriggerType> triggers) {
		for (EvaluatedPolicyRuleTriggerType trigger : triggers) {
			if (trigger instanceof EvaluatedSituationTriggerType) {
				EvaluatedSituationTriggerType situationTrigger = (EvaluatedSituationTriggerType) trigger;
				for (EvaluatedPolicyRuleType sourceRule : situationTrigger.getSourceRule()) {
					if (sourceRule.getTrigger().stream().anyMatch(t -> t.getRef() == null)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static EvaluatedPolicyRuleTriggerType findByIdentifier(List<EvaluatedPolicyRuleTriggerType> triggers, int identifier) {
		return triggers.stream().filter(t -> t.getTriggerId() != null && t.getTriggerId() == identifier).findFirst().orElse(null);
	}

	public static boolean triggerCollectionsEqual(Collection<EvaluatedPolicyRuleTriggerType> triggers,
			Collection<EvaluatedPolicyRuleTriggerType> currentTriggersUnpacked) {
		HeteroComparator<EvaluatedPolicyRuleTriggerType, EvaluatedPolicyRuleTriggerType> comparator = (t1, t2) -> {
			if (!(t1 instanceof EvaluatedSituationTriggerType) || !(t2 instanceof EvaluatedSituationTriggerType)) {
				return Objects.equals(t1, t2);
			}
			EvaluatedSituationTriggerType st1 = (EvaluatedSituationTriggerType) t1;
			EvaluatedSituationTriggerType st2 = (EvaluatedSituationTriggerType) t2;
			// TODO deduplicate this (against standard equals) somehow
			// Until that is done, update after each change to triggers beans structure
			return Objects.equals(st1.getRef(), st2.getRef())
					&& Objects.equals(st1.getTriggerId(), st2.getTriggerId())
					&& Objects.equals(st1.getRuleName(), st2.getRuleName())
					&& Objects.equals(st1.getConstraintKind(), st2.getConstraintKind())
					&& Objects.equals(st1.getConstraint(), st2.getConstraint())
					&& Objects.equals(st1.getMessage(), st2.getMessage())
					&& Objects.equals(st1.getAssignmentPath(), st2.getAssignmentPath())
					&& Objects.equals(st1.getDirectOwnerRef(), st2.getDirectOwnerRef())
					&& Objects.equals(st1.getDirectOwnerDisplayName(), st2.getDirectOwnerDisplayName())
					&& MiscUtil.unorderedCollectionEquals(st1.getSourceRule(), st2.getSourceRule());
		};
		return MiscUtil.unorderedCollectionEquals(currentTriggersUnpacked, triggers, comparator);
	}
	
	@FunctionalInterface
	interface PrimitiveConstraintMatcher {
		boolean matches(QName name, AbstractPolicyConstraintType constraint);
	}
	
	public static boolean hasMatchingPrimitiveConstraint(PolicyConstraintsType pc, PrimitiveConstraintMatcher matcher, boolean recursive) {
		if (pc == null) {
			return false;
		}
		boolean rv = hasMatchingPrimitiveConstraint(pc.getMinAssignees(), F_MIN_ASSIGNEES, matcher)
				|| hasMatchingPrimitiveConstraint(pc.getMaxAssignees(), F_MAX_ASSIGNEES, matcher)
				|| hasMatchingPrimitiveConstraint(pc.getExclusion(), F_EXCLUSION, matcher)
				|| hasMatchingPrimitiveConstraint(pc.getAssignment(), F_ASSIGNMENT, matcher)
				|| hasMatchingPrimitiveConstraint(pc.getHasAssignment(), F_HAS_ASSIGNMENT, matcher)
				|| hasMatchingPrimitiveConstraint(pc.getHasNoAssignment(), F_HAS_NO_ASSIGNMENT, matcher)
				|| hasMatchingPrimitiveConstraint(pc.getModification(), F_MODIFICATION, matcher)
				|| hasMatchingPrimitiveConstraint(pc.getTimeValidity(), F_TIME_VALIDITY, matcher)
				|| hasMatchingPrimitiveConstraint(pc.getAssignmentState(), F_ASSIGNMENT_STATE, matcher)
				|| hasMatchingPrimitiveConstraint(pc.getObjectState(), F_OBJECT_STATE, matcher)
				|| hasMatchingPrimitiveConstraint(pc.getSituation(), F_SITUATION, matcher);
		if (recursive) {
			rv = rv
					|| hasMatchingPrimitiveConstraint(pc.getAnd(), matcher)
					|| hasMatchingPrimitiveConstraint(pc.getOr(), matcher)
					|| hasMatchingPrimitiveConstraint(pc.getNot(), matcher);
		}
		return rv;
	}

	private static boolean hasMatchingPrimitiveConstraint(List<? extends AbstractPolicyConstraintType> constraints, QName name, PrimitiveConstraintMatcher matcher) {
		for (AbstractPolicyConstraintType constraint : constraints) {
			if (matcher.matches(name, constraint)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasMatchingPrimitiveConstraint(List<PolicyConstraintsType> constraintsList, PrimitiveConstraintMatcher matcher) {
		for (PolicyConstraintsType constraints : constraintsList) {
			if (hasMatchingPrimitiveConstraint(constraints, matcher, true)) {
				return true;
			}
		}
		return false;
	}

	// we (mis)use the matcher to collect all the constraints
	public static List<JAXBElement<AbstractPolicyConstraintType>> toPrimitiveConstraintsList(PolicyConstraintsType pc, boolean recursive) {
		List<JAXBElement<AbstractPolicyConstraintType>> rv = new ArrayList<>();
		hasMatchingPrimitiveConstraint(pc, (name, c) -> { rv.add(new JAXBElement<>(name, AbstractPolicyConstraintType.class, c)); return false; }, recursive);
		return rv;
	}

	// e.g. situation-only constraints are applicable both to focus and assignments
	public static boolean isApplicableToAssignment(PolicyRuleType rule) {
		if (rule.getEvaluationTarget() != null) {
			return rule.getEvaluationTarget() == PolicyRuleEvaluationTargetType.ASSIGNMENT;
		} else {
			return hasAssignmentOnlyConstraint(rule) || !hasObjectRelatedConstraint(rule);
		}
	}

	public static boolean isApplicableToObject(PolicyRuleType rule) {
		if (rule.getEvaluationTarget() != null) {
			return rule.getEvaluationTarget() == PolicyRuleEvaluationTargetType.FOCUS;
		} else {
			return !hasAssignmentOnlyConstraint(rule);
		}
	}

	private static boolean hasAssignmentOnlyConstraint(PolicyRuleType rule) {
		return hasMatchingPrimitiveConstraint(rule.getPolicyConstraints(), PolicyRuleTypeUtil::isAssignmentOnly, true);
	}

	// do we have a constraint that indicates a use against object?
	private static boolean hasObjectRelatedConstraint(PolicyRuleType rule) {
		return hasMatchingPrimitiveConstraint(rule.getPolicyConstraints(), PolicyRuleTypeUtil::isObjectRelated, true);
	}

	private static final Set<Class<? extends AbstractPolicyConstraintType>> ASSIGNMENTS_ONLY_CONSTRAINTS_CLASSES =
			new HashSet<>(Arrays.asList(AssignmentPolicyConstraintType.class, ExclusionPolicyConstraintType.class, MultiplicityPolicyConstraintType.class));

	private static boolean isAssignmentOnly(QName name, AbstractPolicyConstraintType c) {
		return ASSIGNMENTS_ONLY_CONSTRAINTS_CLASSES.contains(c.getClass())
				|| QNameUtil.match(name, PolicyConstraintsType.F_ASSIGNMENT_STATE)
				|| c instanceof TimeValidityPolicyConstraintType && Boolean.TRUE.equals(((TimeValidityPolicyConstraintType) c).isAssignment());
	}

	private static final Set<Class<? extends AbstractPolicyConstraintType>> FOCUS_RELATED_CONSTRAINTS_CLASSES =
			new HashSet<>(Arrays.asList(HasAssignmentPolicyConstraintType.class, ModificationPolicyConstraintType.class));

	private static boolean isObjectRelated(QName name, AbstractPolicyConstraintType c) {
		return FOCUS_RELATED_CONSTRAINTS_CLASSES.contains(c.getClass())
				|| QNameUtil.match(name, PolicyConstraintsType.F_OBJECT_STATE)
				|| c instanceof TimeValidityPolicyConstraintType && !Boolean.TRUE.equals(((TimeValidityPolicyConstraintType) c).isAssignment());
	}
}
