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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.HeteroComparator;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType.*;

/**
 * @author mederly
 */
public class PolicyRuleTypeUtil {

	private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleTypeUtil.class);

	private static final char JOIN_AND = '*';
	private static final char JOIN_OR = '|';

	private static Map<String, String> CONSTRAINT_NAMES = new HashMap<>();

	private static final String SYMBOL_MIN = "min";
	private static final String SYMBOL_MAX = "max";
	private static final String SYMBOL_ASSIGNMENT_MODIFICATION = "amod";
	private static final String SYMBOL_OBJECT_MODIFICATION = "omod";
	private static final String SYMBOL_OBJECT_STATE = "ostate";
	private static final String SYMBOL_ASSIGNMENT_STATE = "astate";
	private static final String SYMBOL_EXCLUSION = "exc";
	private static final String SYMBOL_HAS_ASSIGNMENT = "hasass";
	private static final String SYMBOL_NO_ASSIGNMENT = "noass";
	private static final String SYMBOL_TIME_VALIDITY = "time";
	private static final String SYMBOL_SITUATION = "sit";
	private static final String SYMBOL_TRANSITION = "trans";

	static {
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_MIN_ASSIGNEES.getLocalPart(), SYMBOL_MIN);
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_MAX_ASSIGNEES.getLocalPart(), SYMBOL_MAX);
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_ASSIGNMENT.getLocalPart(), SYMBOL_ASSIGNMENT_MODIFICATION);
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_MODIFICATION.getLocalPart(), SYMBOL_OBJECT_MODIFICATION);
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_OBJECT_STATE.getLocalPart(), SYMBOL_OBJECT_STATE);
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_ASSIGNMENT_STATE.getLocalPart(), SYMBOL_ASSIGNMENT_STATE);
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_EXCLUSION.getLocalPart(), SYMBOL_EXCLUSION);
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_HAS_ASSIGNMENT.getLocalPart(), SYMBOL_HAS_ASSIGNMENT);
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_HAS_NO_ASSIGNMENT.getLocalPart(), SYMBOL_NO_ASSIGNMENT);
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_TIME_VALIDITY.getLocalPart(), SYMBOL_TIME_VALIDITY);
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_SITUATION.getLocalPart(), SYMBOL_SITUATION);
		CONSTRAINT_NAMES.put(PolicyConstraintsType.F_TRANSITION.getLocalPart(), SYMBOL_TRANSITION);
	}

	public static String toShortString(PolicyConstraintsType constraints) {
		return toShortString(constraints, JOIN_AND);
	}

	public static String toShortString(PolicyConstraintsType constraints, char join) {
		if (constraints == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		for (JAXBElement<AbstractPolicyConstraintType> constraint : toConstraintsList(constraints, false)) {
			QName name = constraint.getName();
			String abbreviation = CONSTRAINT_NAMES.get(name.getLocalPart());
			if (sb.length() > 0) {
				sb.append(join);
			}
			if (QNameUtil.match(name, PolicyConstraintsType.F_AND)) {
				sb.append('(');
				sb.append(toShortString((PolicyConstraintsType) constraint.getValue(), JOIN_AND));
				sb.append(')');
			} else if (QNameUtil.match(name, PolicyConstraintsType.F_OR)) {
				sb.append('(');
				sb.append(toShortString((PolicyConstraintsType) constraint.getValue(), JOIN_OR));
				sb.append(')');
			} else if (QNameUtil.match(name, PolicyConstraintsType.F_NOT)) {
				sb.append('(');
				sb.append(toShortString((PolicyConstraintsType) constraint.getValue(), JOIN_AND));
				sb.append(')');
			} else if (QNameUtil.match(name, PolicyConstraintsType.F_TRANSITION)) {
				TransitionPolicyConstraintType trans = (TransitionPolicyConstraintType) constraint.getValue();
				sb.append(SYMBOL_TRANSITION);
				sb.append(toTransSymbol(trans.isStateBefore()));
				sb.append(toTransSymbol(trans.isStateAfter()));
				sb.append('(');
				sb.append(toShortString(trans.getConstraints(), JOIN_AND));
				sb.append(')');
			} else {
				sb.append(abbreviation != null ? abbreviation : name.getLocalPart());
			}
		}
		return sb.toString();
	}

	private static String toTransSymbol(Boolean state) {
		if (state != null) {
			return state ? "1" : "0";
		} else {
			return "x";
		}
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
			case ASSIGNMENT_MODIFICATION: return SYMBOL_ASSIGNMENT_MODIFICATION;
			case OBJECT_MODIFICATION: return SYMBOL_OBJECT_MODIFICATION;
			case EXCLUSION: return SYMBOL_EXCLUSION;
			case MAX_ASSIGNEES_VIOLATION: return SYMBOL_MAX;
			case MIN_ASSIGNEES_VIOLATION: return SYMBOL_MIN;
			case OBJECT_STATE: return SYMBOL_OBJECT_STATE;
			case ASSIGNMENT_STATE: return SYMBOL_ASSIGNMENT_STATE;
			case HAS_ASSIGNMENT: return SYMBOL_HAS_ASSIGNMENT;
			case HAS_NO_ASSIGNMENT: return SYMBOL_NO_ASSIGNMENT;
			case TIME_VALIDITY: return SYMBOL_TIME_VALIDITY;
			case SITUATION: return SYMBOL_SITUATION;
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
	interface ConstraintVisitor {
		/**
		 * Returns false if the process is to be finished.
		 */
		boolean visit(QName name, AbstractPolicyConstraintType constraint);
	}

	/**
	 * Returns false if the process was stopped by the consumer.
	 * All references should be resolved.
	 */
	public static boolean accept(PolicyConstraintsType pc, ConstraintVisitor visitor, boolean deep, boolean alsoRoots, boolean ignoreRefs) {
		if (pc == null) {
			return true;
		}
		boolean rv;
		if (alsoRoots) {
			rv = visit(Collections.singletonList(pc), F_AND, visitor);
		} else {
			rv = true;
		}
		rv = rv && visit(pc.getMinAssignees(), F_MIN_ASSIGNEES, visitor)
				&& visit(pc.getMaxAssignees(), F_MAX_ASSIGNEES, visitor)
				&& visit(pc.getExclusion(), F_EXCLUSION, visitor)
				&& visit(pc.getAssignment(), F_ASSIGNMENT, visitor)
				&& visit(pc.getHasAssignment(), F_HAS_ASSIGNMENT, visitor)
				&& visit(pc.getHasNoAssignment(), F_HAS_NO_ASSIGNMENT, visitor)
				&& visit(pc.getModification(), F_MODIFICATION, visitor)
				&& visit(pc.getTimeValidity(), F_TIME_VALIDITY, visitor)
				&& visit(pc.getAssignmentState(), F_ASSIGNMENT_STATE, visitor)
				&& visit(pc.getObjectState(), F_OBJECT_STATE, visitor)
				&& visit(pc.getSituation(), F_SITUATION, visitor)
				&& visit(pc.getTransition(), F_TRANSITION, visitor)
				&& visit(pc.getAnd(), F_AND, visitor)
				&& visit(pc.getOr(), F_OR, visitor)
				&& visit(pc.getNot(), F_NOT, visitor);

		if (!ignoreRefs && !pc.getRef().isEmpty()) {
			throw new IllegalStateException("Unresolved constraint reference (" + pc.getRef() + ").");
		}

		if (deep) {
			for (TransitionPolicyConstraintType transitionConstraint : pc.getTransition()) {
				rv = rv && accept(transitionConstraint.getConstraints(), visitor, true, alsoRoots, ignoreRefs);
			}
			rv = rv
					&& accept(pc.getAnd(), visitor, alsoRoots, ignoreRefs)
					&& accept(pc.getOr(), visitor, alsoRoots, ignoreRefs)
					&& accept(pc.getNot(), visitor, alsoRoots, ignoreRefs);
		}
		return rv;
	}

	private static boolean visit(List<? extends AbstractPolicyConstraintType> constraints, QName name, ConstraintVisitor visitor) {
		for (AbstractPolicyConstraintType constraint : constraints) {
			if (!visitor.visit(name, constraint)) {
				return false;
			}
		}
		return true;
	}

	private static boolean accept(List<PolicyConstraintsType> constraintsList, ConstraintVisitor matcher, boolean alsoRoots, boolean ignoreRefs) {
		for (PolicyConstraintsType constraints : constraintsList) {
			if (!accept(constraints, matcher, true, alsoRoots, ignoreRefs)) {
				return false;
			}
		}
		return true;
	}

	public static List<JAXBElement<AbstractPolicyConstraintType>> toConstraintsList(PolicyConstraintsType pc, boolean deep) {
		List<JAXBElement<AbstractPolicyConstraintType>> rv = new ArrayList<>();
		accept(pc, (name, c) -> { rv.add(toConstraintJaxbElement(name, c)); return true; }, deep, false, false);
		return rv;
	}

	@NotNull
	private static JAXBElement<AbstractPolicyConstraintType> toConstraintJaxbElement(QName name, AbstractPolicyConstraintType c) {
		return new JAXBElement<>(name, AbstractPolicyConstraintType.class, c);
	}

	/**
	 * Returns true if this policy rule can be applied to an assignment.
	 * By default, rules that have only object-related constraints, are said to be applicable to objects only
	 * (even if technically they could be applied to assignments as well).
	 */
	public static boolean isApplicableToAssignment(PolicyRuleType rule) {
		if (rule.getEvaluationTarget() != null) {
			return rule.getEvaluationTarget() == PolicyRuleEvaluationTargetType.ASSIGNMENT;
		} else {
			return hasAssignmentOnlyConstraint(rule) || !hasObjectRelatedConstraint(rule);
		}
	}

	/**
	 * Returns true if this policy rule can be applied to an object as a whole.
	 */
	public static boolean isApplicableToObject(PolicyRuleType rule) {
		if (rule.getEvaluationTarget() != null) {
			return rule.getEvaluationTarget() == PolicyRuleEvaluationTargetType.FOCUS;
		} else {
			return !hasAssignmentOnlyConstraint(rule);
		}
	}

	private static boolean hasAssignmentOnlyConstraint(PolicyRuleType rule) {
		// 'accept' continues until isNotAssignmentOnly is false; and returns false then --> so we return true in that case (i.e. we have found assignmentOnly-constraint)
		return !accept(rule.getPolicyConstraints(), PolicyRuleTypeUtil::isNotAssignmentOnly, true, true, false);
	}

	// do we have a constraint that indicates a use against object?
	private static boolean hasObjectRelatedConstraint(PolicyRuleType rule) {
		// 'accept' continues until isNotObjectRelated is false; and returns false then --> so we return true in that case (i.e. we have found object-related constraint)
		return !accept(rule.getPolicyConstraints(), PolicyRuleTypeUtil::isNotObjectRelated, true, true, false);
	}

	private static final Set<Class<? extends AbstractPolicyConstraintType>> ASSIGNMENTS_ONLY_CONSTRAINTS_CLASSES =
			new HashSet<>(Arrays.asList(AssignmentPolicyConstraintType.class, ExclusionPolicyConstraintType.class, MultiplicityPolicyConstraintType.class));

	private static boolean isNotAssignmentOnly(QName name, AbstractPolicyConstraintType c) {
		boolean rv = ASSIGNMENTS_ONLY_CONSTRAINTS_CLASSES.contains(c.getClass())
				|| QNameUtil.match(name, PolicyConstraintsType.F_ASSIGNMENT_STATE)
				|| c instanceof TimeValidityPolicyConstraintType && Boolean.TRUE.equals(((TimeValidityPolicyConstraintType) c).isAssignment());
		//System.out.println("isAssignmentOnly: " + name.getLocalPart() + "/" + c.getClass().getSimpleName() + " -> " + rv);
		return !rv;
	}

	private static final Set<Class<? extends AbstractPolicyConstraintType>> OBJECT_RELATED_CONSTRAINTS_CLASSES =
			new HashSet<>(Arrays.asList(HasAssignmentPolicyConstraintType.class, ModificationPolicyConstraintType.class));

	private static boolean isNotObjectRelated(QName name, AbstractPolicyConstraintType c) {
		boolean rv = OBJECT_RELATED_CONSTRAINTS_CLASSES.contains(c.getClass())
				|| QNameUtil.match(name, PolicyConstraintsType.F_OBJECT_STATE)
				|| c instanceof TimeValidityPolicyConstraintType && !Boolean.TRUE.equals(((TimeValidityPolicyConstraintType) c).isAssignment());
		//System.out.println("isObjectRelated: " + name.getLocalPart() + "/" + c.getClass().getSimpleName() + " -> " + rv);
		return !rv;
	}

	interface ConstraintResolver {
		@NotNull
		JAXBElement<? extends AbstractPolicyConstraintType> resolve(@NotNull String name) throws ObjectNotFoundException;
	}

	public static class LazyMapConstraintsResolver implements ConstraintResolver {
		@NotNull private final List<Supplier<Map<String, JAXBElement<? extends AbstractPolicyConstraintType>>>> constraintsSuppliers;
		@NotNull private final Map<String, JAXBElement<? extends AbstractPolicyConstraintType>> constraintsMap = new HashMap<>();
		private int usedSuppliers = 0;

		@SafeVarargs
		public LazyMapConstraintsResolver(
				@NotNull Supplier<Map<String, JAXBElement<? extends AbstractPolicyConstraintType>>>... constraintsSuppliers) {
			this.constraintsSuppliers = Arrays.asList(constraintsSuppliers);
		}

		@NotNull
		@Override
		public JAXBElement<? extends AbstractPolicyConstraintType> resolve(@NotNull String name) throws ObjectNotFoundException {
			for (;;) {
				JAXBElement<? extends AbstractPolicyConstraintType> rv = constraintsMap.get(name);
				if (rv != null) {
					return rv;
				}
				if (usedSuppliers >= constraintsSuppliers.size()) {
					throw new ObjectNotFoundException("No policy constraint named '" + name + "' could be found. Known constraints: " + constraintsMap.keySet());
				}
				constraintsMap.putAll(constraintsSuppliers.get(usedSuppliers++).get());
			}
		}
	}

	public static void resolveReferences(PolicyConstraintsType pc, ConstraintResolver resolver)
			throws ObjectNotFoundException, SchemaException {
		accept(pc, (name, c) -> {
			if (c instanceof PolicyConstraintsType) {
				try {
					resolveLocalReferences((PolicyConstraintsType) c, resolver);
				} catch (ObjectNotFoundException | SchemaException e) {
					MiscUtil.throwExceptionAsUnchecked(e);
				}
			}
			return true;
		}, true, true, true);
	}

	@SuppressWarnings("unchecked")
	private static void resolveLocalReferences(PolicyConstraintsType pc, ConstraintResolver resolver)
			throws ObjectNotFoundException, SchemaException {
		for (String ref : pc.getRef()) {
			JAXBElement<? extends AbstractPolicyConstraintType> resolved = resolver.resolve(ref);
			QName constraintName = resolved.getName();
			AbstractPolicyConstraintType constraintValue = resolved.getValue();
			PrismContainer<? extends AbstractPolicyConstraintType> container = pc.asPrismContainerValue().findOrCreateContainer(constraintName);
			container.add(constraintValue.asPrismContainerValue().clone());
		}
		pc.getRef().clear();
	}

	public static void resolveReferences(List<PolicyRuleType> rules, Collection<? extends PolicyRuleType> otherRules) throws SchemaException, ObjectNotFoundException {
		LazyMapConstraintsResolver resolver = new LazyMapConstraintsResolver(createConstraintsSupplier(rules),
				createConstraintsSupplier(otherRules));
		for (PolicyRuleType rule : rules) {
			resolveReferences(rule.getPolicyConstraints(), resolver);
//			// check resolution
//			try {
//				toConstraintsList(rule.getPolicyConstraints(), true);
//			} catch (Throwable t) {
//				System.out.println("Hi!");
//			}
		}
	}

	@NotNull
	private static Supplier<Map<String, JAXBElement<? extends AbstractPolicyConstraintType>>> createConstraintsSupplier(
			Collection<? extends PolicyRuleType> rules) {
		return () -> {
			Map<String, JAXBElement<? extends AbstractPolicyConstraintType>> constraints = new HashMap<>();
			for (PolicyRuleType rule : rules) {
				accept(rule.getPolicyConstraints(), (elementName, c) -> {
					if (c.getName() != null) {
						constraints.put(c.getName(), toConstraintJaxbElement(elementName, c));
					}
					return true;
				}, true, true,true);
			}
			return constraints;
		};
	}

}
