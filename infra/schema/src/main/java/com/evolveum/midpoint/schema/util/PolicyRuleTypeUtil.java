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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author mederly
 */
public class PolicyRuleTypeUtil {

	private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleTypeUtil.class);

	private static Map<String, String> CONSTRAINT_NAMES = new HashMap<>();
	static {
		CONSTRAINT_NAMES.put(ExclusionPolicyConstraintType.class.getName(), "exc");
		CONSTRAINT_NAMES.put(MultiplicityPolicyConstraintType.class.getName(), "multi");
		CONSTRAINT_NAMES.put(ModificationPolicyConstraintType.class.getName(), "mod");
		CONSTRAINT_NAMES.put(AssignmentPolicyConstraintType.class.getName(), "assign");
		CONSTRAINT_NAMES.put(PolicySituationPolicyConstraintType.class.getName(), "sit");
	}

	public static String getConstraintClassShortcut(Class<?> clazz) {
		String shortcut = CONSTRAINT_NAMES.get(clazz.getName());
		return shortcut != null ? shortcut : clazz.getSimpleName();
	}

	public static String toShortString(PolicyConstraintsType constraints) {
		if (constraints == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		constraints.getExclusion().forEach(excl -> sb.append("exc "));
		constraints.getMinAssignees().forEach(min -> sb.append("min "));
		constraints.getMaxAssignees().forEach(max -> sb.append("max "));
		constraints.getModification().forEach(mod -> sb.append("mod "));
		constraints.getAssignment().forEach(assign -> sb.append("assign "));
		constraints.getSituation().forEach(assign -> sb.append("sit "));
		return sb.toString().trim();
	}

	public static String toShortString(PolicyActionsType actions) {
		if (actions == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		if (actions.getEnforcement() != null) {
			sb.append("enforce ");
		}
		if (actions.getApproval() != null) {
			sb.append("approve ");
		}
		if (actions.getRemediation() != null) {
			sb.append("remedy ");
		}
		if (actions.getCertification() != null) {
			sb.append("certify ");
		}
		if (actions.getNotification() != null) {
			sb.append("notify ");
		}
		return sb.toString().trim();
	}

	public static String toShortString(AbstractPolicyConstraintType constraint) {
		if (constraint == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(getConstraintClassShortcut(constraint.getClass()));
		if (constraint.getName() != null) {
			sb.append(":").append(constraint.getName());
		}
		return sb.toString();
	}

	public static String toDiagShortcut(PolicyConstraintKindType constraintKind) {
		if (constraintKind == null) {
			return "null";
		}
		switch (constraintKind) {
			case ASSIGNMENT: return "assign";
			case EXCLUSION: return "exc";
			case MAX_ASSIGNEES: return "max";
			case MIN_ASSIGNEES: return "min";
			case MODIFICATION: return "mod";
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
}
