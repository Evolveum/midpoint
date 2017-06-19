/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.*;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
@Component
public class PolicyRuleProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleProcessor.class);
	
	@Autowired private PrismContext prismContext;
	@Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
	@Autowired private MappingFactory mappingFactory;
	@Autowired private MappingEvaluator mappingEvaluator;
	@Autowired private MatchingRuleRegistry matchingRuleRegistry;

	private static final QName CONDITION_OUTPUT_NAME = new QName(SchemaConstants.NS_C, "condition");

	/**
	 * Evaluate the policies (policy rules, but also the legacy policies). Trigger the rules.
	 * But do not enforce anything and do not make any context changes.
	 */
	public <F extends FocusType> void processPolicies(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			OperationResult result) throws PolicyViolationException, SchemaException {
		checkAssignmentRules(context, evaluatedAssignmentTriple, result);
        checkExclusionsLegacy(context, evaluatedAssignmentTriple.getPlusSet(), evaluatedAssignmentTriple.getNonNegativeValues());
        // in policy based situations, the comparison is not symmetric
        checkExclusionsRuleBased(context, evaluatedAssignmentTriple.getPlusSet(), evaluatedAssignmentTriple.getPlusSet());
        checkExclusionsRuleBased(context, evaluatedAssignmentTriple.getPlusSet(), evaluatedAssignmentTriple.getZeroSet());
        checkExclusionsRuleBased(context, evaluatedAssignmentTriple.getZeroSet(), evaluatedAssignmentTriple.getPlusSet());
        checkExclusionsRuleBased(context, evaluatedAssignmentTriple.getZeroSet(), evaluatedAssignmentTriple.getZeroSet());
        checkAssigneeConstraints(context, evaluatedAssignmentTriple, result);
        checkSecondaryConstraints(context, evaluatedAssignmentTriple, result);
	}

	private <F extends FocusType> void checkAssignmentRules(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			OperationResult result) throws PolicyViolationException, SchemaException {
		checkAssignmentRules(context, evaluatedAssignmentTriple.getPlusSet(), PlusMinusZero.PLUS, result);
		checkAssignmentRules(context, evaluatedAssignmentTriple.getMinusSet(), PlusMinusZero.MINUS, result);
	}

	private <F extends FocusType> void checkAssignmentRules(LensContext<F> context,
			Collection<EvaluatedAssignmentImpl<F>> evaluatedAssignmentSet, PlusMinusZero whichSet,
			OperationResult result) throws PolicyViolationException, SchemaException {
		for (EvaluatedAssignmentImpl<F> evaluatedAssignment: evaluatedAssignmentSet) {
			Collection<EvaluatedPolicyRule> policyRules = evaluatedAssignment.getThisTargetPolicyRules();
			for (EvaluatedPolicyRule policyRule: policyRules) {
				PolicyConstraintsType policyConstraints = policyRule.getPolicyConstraints();
				if (policyConstraints == null) {
					continue;
				}
				for (AssignmentPolicyConstraintType assignmentConstraint: policyConstraints.getAssignment()) {
					if (matchesOperation(assignmentConstraint, whichSet)) {
						List<QName> relationsToCheck = assignmentConstraint.getRelation().isEmpty() ?
								Collections.singletonList(null) : assignmentConstraint.getRelation();
						for (QName constraintRelation : relationsToCheck) {
							if (MiscSchemaUtil.compareRelation(constraintRelation, evaluatedAssignment.getRelation())) {
								EvaluatedPolicyRuleTrigger trigger = new EvaluatedPolicyRuleTrigger<>(
										PolicyConstraintKindType.ASSIGNMENT,
										assignmentConstraint, "Assignment of " + evaluatedAssignment.getTarget());
								evaluatedAssignment.triggerConstraint(policyRule, trigger);
							}
						}
					}
				}
			}
		}
	}

	private boolean matchesOperation(AssignmentPolicyConstraintType constraint, PlusMinusZero whichSet) {
		List<ModificationTypeType> operations = constraint.getOperation();
		if (operations.isEmpty()) {
			return true;
		}
		switch (whichSet) {
			case PLUS: return operations.contains(ModificationTypeType.ADD);
			case MINUS: return operations.contains(ModificationTypeType.DELETE);
			case ZERO: return operations.contains(ModificationTypeType.REPLACE);
			default: throw new IllegalArgumentException("whichSet: " + whichSet);
		}
	}

	private <F extends FocusType> void checkExclusionsLegacy(LensContext<F> context, Collection<EvaluatedAssignmentImpl<F>> assignmentsA,
			Collection<EvaluatedAssignmentImpl<F>> assignmentsB) throws PolicyViolationException {
		for (EvaluatedAssignmentImpl<F> assignmentA: assignmentsA) {
			for (EvaluatedAssignmentImpl<F> assignmentB: assignmentsB) {
				if (assignmentA == assignmentB) {
					continue;	// Same thing, this cannot exclude itself
				}
				for (EvaluatedAssignmentTargetImpl eRoleA : assignmentA.getRoles().getAllValues()) {
					if (eRoleA.appliesToFocus()) {
						for (EvaluatedAssignmentTargetImpl eRoleB : assignmentB.getRoles().getAllValues()) {
							if (eRoleB.appliesToFocus()) {
								checkExclusionLegacy(assignmentA, assignmentB, eRoleA, eRoleB);
							}
						}
					}
				}
			}
		}
	}

	private <F extends FocusType> void checkExclusionLegacy(EvaluatedAssignmentImpl<F> assignmentA, EvaluatedAssignmentImpl<F> assignmentB,
			EvaluatedAssignmentTargetImpl roleA, EvaluatedAssignmentTargetImpl roleB) throws PolicyViolationException {
		checkExclusionOneWayLegacy(assignmentA, assignmentB, roleA, roleB);
		checkExclusionOneWayLegacy(assignmentB, assignmentA, roleB, roleA);
	}

	private <F extends FocusType> void checkExclusionOneWayLegacy(EvaluatedAssignmentImpl<F> assignmentA, EvaluatedAssignmentImpl<F> assignmentB,
			EvaluatedAssignmentTargetImpl roleA, EvaluatedAssignmentTargetImpl roleB) throws PolicyViolationException {
		for (ExclusionPolicyConstraintType exclusionA : roleA.getExclusions()) {
			checkAndTriggerExclusionConstraintViolationLegacy(assignmentA, assignmentB, roleA, roleB, exclusionA);
		}
	}

	private <F extends FocusType> void checkAndTriggerExclusionConstraintViolationLegacy(EvaluatedAssignmentImpl<F> assignmentA,
			@NotNull EvaluatedAssignmentImpl<F> assignmentB, EvaluatedAssignmentTargetImpl roleA, EvaluatedAssignmentTargetImpl roleB,
			ExclusionPolicyConstraintType constraint)
			throws PolicyViolationException {
		ObjectReferenceType targetRef = constraint.getTargetRef();
		if (roleB.getOid().equals(targetRef.getOid())) {
			EvaluatedExclusionTrigger trigger = new EvaluatedExclusionTrigger(
					constraint, "Violation of SoD policy: " + roleA.getTarget() + " excludes " + roleB.getTarget() +
					", they cannot be assigned at the same time", assignmentB,
					roleA.getTarget() != null ? roleA.getTarget().asObjectable() : null,
					roleB.getTarget() != null ? roleB.getTarget().asObjectable() : null,
					roleA.getAssignmentPath(), roleB.getAssignmentPath());
			assignmentA.triggerConstraint(null, trigger);
		}
	}

	private <F extends FocusType> void checkExclusionsRuleBased(LensContext<F> context,
			Collection<EvaluatedAssignmentImpl<F>> assignmentsA, Collection<EvaluatedAssignmentImpl<F>> assignmentsB)
			throws PolicyViolationException, SchemaException {
		for (EvaluatedAssignmentImpl<F> assignmentA : assignmentsA) {
			checkExclusionsRuleBased(context, assignmentA, assignmentsB);
		}
	}

	private <F extends FocusType> void checkExclusionsRuleBased(LensContext<F> context, EvaluatedAssignmentImpl<F> assignmentA,
			Collection<EvaluatedAssignmentImpl<F>> assignmentsB) throws PolicyViolationException, SchemaException {

		// We consider all policy rules, i.e. also from induced targets. (It is not possible to collect local
		// rules for individual targets in the chain - rules are computed only for directly evaluated assignments.)
		for (EvaluatedPolicyRule policyRule : assignmentA.getAllTargetsPolicyRules()) {
			if (policyRule.getPolicyConstraints() == null || policyRule.getPolicyConstraints().getExclusion().isEmpty()) {
				continue;
			}
			// In order to avoid false positives, we consider all targets from the current assignment as "allowed"
			Set<String> allowedTargetOids = assignmentA.getNonNegativeTargets().stream()
					.filter(t -> t.appliesToFocus())
					.map(t -> t.getOid())
					.collect(Collectors.toSet());

			for (EvaluatedAssignmentImpl<F> assignmentB : assignmentsB) {
				for (EvaluatedAssignmentTargetImpl targetB : assignmentB.getNonNegativeTargets()) {
					if (!targetB.appliesToFocus() || allowedTargetOids.contains(targetB.getOid())) {
						continue;
					}
					List<ExclusionPolicyConstraintType> exclusionConstraints = policyRule.getPolicyConstraints().getExclusion();
					// TODO: implement proper AND logic between multiple constraints (MID-3959)
					if (exclusionConstraints.size() > 1) {
						throw new UnsupportedOperationException("Multiple exclusion constraints in the same policy rules are not supported yet.");
					}
					if (!exclusionConstraints.isEmpty()) {
						ExclusionPolicyConstraintType exclusionConstraint = exclusionConstraints.iterator().next();
						if (excludes(exclusionConstraint, targetB)) {
							triggerExclusionConstraintViolation(assignmentA, assignmentB, targetB, exclusionConstraint, policyRule);
						}
					}
				}
			}
		}
	}

	private boolean excludes(ExclusionPolicyConstraintType constraint, EvaluatedAssignmentTargetImpl targetAssignment) throws SchemaException {
		if (constraint.getTargetRef() == null || targetAssignment.getOid() == null) {
			return false;		// shouldn't occur
			
		} else if (constraint.getTargetRef() != null) {
			ObjectReferenceType targetRef = constraint.getTargetRef();
			if (targetRef.getOid() != null) {
				return targetAssignment.getOid().equals(constraint.getTargetRef().getOid());
			} else {
				if (targetRef.getResolutionTime() == EvaluationTimeType.RUN) {
					SearchFilterType filterType = targetRef.getFilter();
					if (filterType == null) {
						throw new SchemaException("No filter in exclusion reference");
					}
					QName typeQName = targetRef.getType();
					@SuppressWarnings("rawtypes")
					PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(typeQName);
					ObjectFilter filter = QueryConvertor.parseFilter(filterType, objDef);
					PrismObject<? extends FocusType> target = targetAssignment.getTarget();
					return filter.match(target.getValue(), matchingRuleRegistry);
				} else {
					throw new SchemaException("No OID in exclusion reference");
				}
			}
			
		} else {
			throw new SchemaException("No target reference in exclusion");
		}
	}

	private <F extends FocusType> void triggerExclusionConstraintViolation(EvaluatedAssignmentImpl<F> assignmentA,
			@NotNull EvaluatedAssignmentImpl<F> assignmentB, EvaluatedAssignmentTargetImpl targetB,
			ExclusionPolicyConstraintType constraint, EvaluatedPolicyRule policyRule)
			throws PolicyViolationException {

		AssignmentPath pathA = policyRule.getAssignmentPath();
		AssignmentPath pathB = targetB.getAssignmentPath();
		String infoA = computeAssignmentInfo(pathA, assignmentA.getTarget());
		String infoB = computeAssignmentInfo(pathB, targetB.getTarget());
		ObjectType objectA = getConflictingObject(pathA, assignmentA.getTarget());
		ObjectType objectB = getConflictingObject(pathB, targetB.getTarget());
		EvaluatedExclusionTrigger trigger = new EvaluatedExclusionTrigger(
				constraint, "Violation of SoD policy: " + infoA + " excludes " + infoB +
				", they cannot be assigned at the same time", assignmentB, objectA, objectB, pathA, pathB);
		assignmentA.triggerConstraint(policyRule, trigger);
	}

	private ObjectType getConflictingObject(AssignmentPath path, PrismObject<?> defaultObject) {
		if (path == null) {
			return ObjectTypeUtil.toObjectable(defaultObject);
		}
		List<ObjectType> objects = path.getFirstOrderChain();
		return objects.isEmpty() ?
				ObjectTypeUtil.toObjectable(defaultObject) : objects.get(objects.size()-1);
	}

	private String computeAssignmentInfo(AssignmentPath path, PrismObject<?> defaultObject) {
		if (path == null) {
			return String.valueOf(defaultObject);	// shouldn't occur
		}
		List<ObjectType> objects = path.getFirstOrderChain();
		if (objects.isEmpty()) {		// shouldn't occur
			return String.valueOf(defaultObject);
		}
		ObjectType last = objects.get(objects.size()-1);
		StringBuilder sb = new StringBuilder();
		sb.append(last);
		if (objects.size() > 1) {
			sb.append(objects.stream()
					.map(o -> PolyString.getOrig(o.getName()))
					.collect(Collectors.joining("->", " (", ")")));
		}
		return sb.toString();
	}

	private <F extends FocusType> void checkAssigneeConstraints(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			OperationResult result) throws PolicyViolationException, SchemaException {
		for (EvaluatedAssignmentImpl<F> assignment: evaluatedAssignmentTriple.union()) {
			if (evaluatedAssignmentTriple.presentInPlusSet(assignment)) {
				if (!assignment.isPresentInCurrentObject()) {
					checkAssigneeConstraints(context, assignment, PlusMinusZero.PLUS, result);		// only really new assignments
				}
			} else if (evaluatedAssignmentTriple.presentInZeroSet(assignment)) {
				// No need to check anything here. Maintain status quo.
			} else {
				if (assignment.isPresentInCurrentObject()) {
					checkAssigneeConstraints(context, assignment, PlusMinusZero.MINUS, result);		// only assignments that are really deleted
				}
			}
		}
	}
	
	private <F extends FocusType> void checkAssigneeConstraints(LensContext<F> context, EvaluatedAssignment<F> assignment, PlusMinusZero plusMinus, OperationResult result) throws PolicyViolationException, SchemaException {
		PrismObject<?> target = assignment.getTarget();
		if (target == null || !(target.asObjectable() instanceof AbstractRoleType)) {
			return;
		}
		AbstractRoleType targetRole = (AbstractRoleType) target.asObjectable();
		QName relation = ObjectTypeUtil.normalizeRelation(assignment.getRelation());
		Collection<EvaluatedPolicyRule> policyRules = assignment.getThisTargetPolicyRules();
		for (EvaluatedPolicyRule policyRule: policyRules) {
			PolicyConstraintsType policyConstraints = policyRule.getPolicyConstraints();
			if (policyConstraints == null) {
				continue;
			}
			List<MultiplicityPolicyConstraintType> relevantMinAssignees = getForRelation(policyConstraints.getMinAssignees(), relation);
			List<MultiplicityPolicyConstraintType> relevantMaxAssignees = getForRelation(policyConstraints.getMaxAssignees(), relation);
			if (relevantMinAssignees.isEmpty() && relevantMaxAssignees.isEmpty()) {
				continue;
			}
			String focusOid = null;
			if (context.getFocusContext() != null) {
				focusOid = context.getFocusContext().getOid();
			}
			int currentAssigneesExceptMyself = getNumberOfAssigneesExceptMyself(targetRole, focusOid, relation, result);
			for (MultiplicityPolicyConstraintType constraint: relevantMinAssignees) {
				Integer requiredMultiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getMultiplicity());
				if (requiredMultiplicity <= 0) {
					continue;			// unbounded or 0
				}
				// Complain only if the situation is getting worse
				if (currentAssigneesExceptMyself < requiredMultiplicity && plusMinus == PlusMinusZero.MINUS) {
					EvaluatedPolicyRuleTrigger<MultiplicityPolicyConstraintType> trigger =
							new EvaluatedPolicyRuleTrigger<>(PolicyConstraintKindType.MIN_ASSIGNEES,
									constraint, target+" requires at least " + requiredMultiplicity
									+ " assignees with the relation of '" + relation.getLocalPart()
									+ "'. The operation would result in "+currentAssigneesExceptMyself+" assignees.");
					assignment.triggerConstraint(policyRule, trigger);
				}
			}
			for (MultiplicityPolicyConstraintType constraint: relevantMaxAssignees) {
				Integer requiredMultiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getMultiplicity());
				if (requiredMultiplicity < 0) {
					continue;			// unbounded
				}
				// Complain only if the situation is getting worse
				if (currentAssigneesExceptMyself >= requiredMultiplicity && plusMinus == PlusMinusZero.PLUS) {
					EvaluatedPolicyRuleTrigger<MultiplicityPolicyConstraintType> trigger =
							new EvaluatedPolicyRuleTrigger<>(PolicyConstraintKindType.MAX_ASSIGNEES,
									constraint, target + " requires at most " + requiredMultiplicity +
									" assignees with the relation of '" + relation.getLocalPart()
									+ "'. The operation would result in " + (currentAssigneesExceptMyself+1) + " assignees.");
					assignment.triggerConstraint(policyRule, trigger);
				}
			}
		}
	}

	private List<MultiplicityPolicyConstraintType> getForRelation(List<MultiplicityPolicyConstraintType> all, QName relation) {
		return all.stream()
				.filter(c -> containsRelation(c, relation))
				.collect(Collectors.toList());
	}

	private boolean containsRelation(MultiplicityPolicyConstraintType constraint, QName relation) {
		return getConstraintRelations(constraint).stream()
				.anyMatch(constraintRelation -> ObjectTypeUtil.relationMatches(constraintRelation, relation));
	}

	private List<QName> getConstraintRelations(MultiplicityPolicyConstraintType constraint) {
		return !constraint.getRelation().isEmpty() ?
				constraint.getRelation() :
				Collections.singletonList(SchemaConstants.ORG_DEFAULT);
	}

	/**
	 * Returns numbers of assignees with the given relation name.
	 */
	private int getNumberOfAssigneesExceptMyself(AbstractRoleType target, String selfOid, QName relation, OperationResult result)
			throws SchemaException {
		// TODO couldn't we simply use .relation(relation) ?
		S_AtomicFilterExit q = QueryBuilder.queryFor(FocusType.class, prismContext)
				.item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(
						new PrismReferenceValue(target.getOid()).relation(PrismConstants.Q_ANY));
		if (selfOid != null) {
			q = q.and().not().id(selfOid);
		}
		ObjectQuery query = q.build();
		List<PrismObject<FocusType>> assignees = repositoryService.searchObjects(FocusType.class, query, null, result);
		int count = 0;
		assignee: for (PrismObject<FocusType> assignee : assignees) {
			for (AssignmentType assignment : assignee.asObjectable().getAssignment()) {
				if (assignment.getTargetRef() != null
						&& ObjectTypeUtil.relationsEquivalent(relation, assignment.getTargetRef().getRelation())) {
					count++;
					continue assignee;
				}
			}
		}
		return count;
	}

	public <F extends FocusType> boolean processPruning(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			OperationResult result) throws PolicyViolationException, SchemaException {
		Collection<EvaluatedAssignmentImpl<F>> plusSet = evaluatedAssignmentTriple.getPlusSet();
		if (plusSet == null) {
			return false;
		}
		boolean needToReevaluateAssignments = false;
		for (EvaluatedAssignmentImpl<F> plusAssignment: plusSet) {
			for (EvaluatedPolicyRule targetPolicyRule: plusAssignment.getAllTargetsPolicyRules()) {
				for (EvaluatedPolicyRuleTrigger trigger: targetPolicyRule.getTriggers()) {
					if (!(trigger instanceof EvaluatedExclusionTrigger)) {
						continue;
					}
					EvaluatedExclusionTrigger exclTrigger = (EvaluatedExclusionTrigger) trigger;
					PolicyActionsType actions = targetPolicyRule.getActions();
					if (actions == null || actions.getPrune() == null) {
						continue;
					}
					EvaluatedAssignment<FocusType> conflictingAssignment = exclTrigger.getConflictingAssignment();
					if (conflictingAssignment == null) {
						throw new SystemException("Added assignment "+plusAssignment
								+", the exclusion prune rule was triggered but there is no conflicting assignment in the trigger");
					}
					LOGGER.debug("Pruning assignment {} because it conflicts with added assignment {}", conflictingAssignment, plusAssignment);
					
					PrismContainerValue<AssignmentType> assignmentValueToRemove = conflictingAssignment.getAssignmentType().asPrismContainerValue().clone();
					PrismObjectDefinition<F> focusDef = context.getFocusContext().getObjectDefinition();
					ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(FocusType.F_ASSIGNMENT, focusDef);
					assignmentDelta.addValuesToDelete(assignmentValueToRemove);
					context.getFocusContext().swallowToSecondaryDelta(assignmentDelta);
					
					needToReevaluateAssignments = true;
				}
			}
		}
		
		return needToReevaluateAssignments;
	}

	private <F extends FocusType> void checkSecondaryConstraints(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, OperationResult result)
			throws SchemaException, PolicyViolationException {
		checkSecondaryConstraints(context, evaluatedAssignmentTriple.getPlusSet(), result);
		checkSecondaryConstraints(context, evaluatedAssignmentTriple.getZeroSet(), result);
		checkSecondaryConstraints(context, evaluatedAssignmentTriple.getMinusSet(), result);
	}

	private <F extends FocusType> void checkSecondaryConstraints(LensContext<F> context,
			Collection<EvaluatedAssignmentImpl<F>> evaluatedAssignmentSet,
			OperationResult result) throws PolicyViolationException, SchemaException {
		for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignmentSet) {
			checkSecondaryConstraints(evaluatedAssignment, result);
		}
	}

	private <F extends FocusType> void checkSecondaryConstraints(EvaluatedAssignmentImpl<F> evaluatedAssignment,
			OperationResult result) throws PolicyViolationException, SchemaException {

		// Single pass only (for the time being)
		// We consider only directly attached "situation" policy rules. In the future, we might configure this.
		// So, if someone wants to report (forward) triggers from a target, he must ensure that a particular
		// "situation" constraint is present directly on it.
		for (EvaluatedPolicyRule policyRule: evaluatedAssignment.getThisTargetPolicyRules()) {
			if (policyRule.getPolicyConstraints() == null) {
				continue;
			}
			for (PolicySituationPolicyConstraintType situationConstraint : policyRule.getPolicyConstraints().getSituation()) {
				Collection<EvaluatedPolicyRule> sourceRules =
						selectTriggeredRules(evaluatedAssignment, situationConstraint.getSituation());
				if (sourceRules.isEmpty()) {
					continue;
				}
				String message =
						sourceRules.stream()
								.flatMap(r -> r.getTriggers().stream().map(EvaluatedPolicyRuleTrigger::getMessage))
								.distinct()
								.collect(Collectors.joining("; "));
				EvaluatedSituationTrigger trigger = new EvaluatedSituationTrigger(situationConstraint, message, sourceRules);
				evaluatedAssignment.triggerConstraint(policyRule, trigger);
			}
		}
	}

	private <F extends FocusType> Collection<EvaluatedPolicyRule> selectTriggeredRules(
			EvaluatedAssignmentImpl<F> evaluatedAssignment, List<String> situations) {
		// We consider all rules here, i.e. also those that are triggered on targets induced by this one.
		// Decision whether to trigger such rules lies on "primary" policy constraints. (E.g. approvals would
		// not trigger, whereas exclusions probably would.) Overall, our responsibility is simply to collect
		// all triggered rules.
		return evaluatedAssignment.getAllTargetsPolicyRules().stream()
				.filter(r -> !r.getTriggers().isEmpty() && situations.contains(r.getPolicySituation()))
				.collect(Collectors.toList());
	}

	@NotNull
	private <F extends FocusType> List<ItemDelta<?, ?>> getAssignmentModificationDelta(
			EvaluatedAssignmentImpl<F> evaluatedAssignment, List<EvaluatedPolicyRuleTriggerType> triggers) throws SchemaException {
		Long id = evaluatedAssignment.getAssignmentType().getId();
		if (id == null) {
			throw new IllegalArgumentException("Assignment with no ID: " + evaluatedAssignment);
		}
		List<ItemDelta<?, ?>> deltas = new ArrayList<>();
		Set<String> currentSituations = new HashSet<>(evaluatedAssignment.getAssignmentType().getPolicySituation());
		Set<String> newSituations = new HashSet<>(evaluatedAssignment.getPolicySituations());
		CollectionUtils.addIgnoreNull(deltas, createSituationDelta(
				new ItemPath(FocusType.F_ASSIGNMENT, id, AssignmentType.F_POLICY_SITUATION), currentSituations, newSituations));
		Set<EvaluatedPolicyRuleTriggerType> currentTriggers = new HashSet<>(evaluatedAssignment.getAssignmentType().getTrigger());
		Set<EvaluatedPolicyRuleTriggerType> newTriggers = new HashSet<>(triggers);
		CollectionUtils.addIgnoreNull(deltas, createTriggerDelta(
				new ItemPath(FocusType.F_ASSIGNMENT, id, AssignmentType.F_TRIGGER), currentTriggers, newTriggers));
		return deltas;
	}

	private <F extends FocusType> boolean shouldSituationBeUpdated(EvaluatedAssignment<F> evaluatedAssignment,
			List<EvaluatedPolicyRuleTriggerType> triggers) {
		Set<String> currentSituations = new HashSet<>(evaluatedAssignment.getAssignmentType().getPolicySituation());
		Set<EvaluatedPolicyRuleTriggerType> currentTriggers = new HashSet<>(evaluatedAssignment.getAssignmentType().getTrigger());
		// if the current situations different from the ones in the old assignment => update
		// (provided that the situations in the assignment were _not_ changed directly via a delta!!!) TODO check this
		if (!currentSituations.equals(new HashSet<>(evaluatedAssignment.getPolicySituations()))) {
			LOGGER.trace("computed policy situations are different from the current ones");
			return true;
		}
		if (!currentTriggers.equals(new HashSet<>(triggers))) {
			LOGGER.trace("computed policy rules triggers are different from the current ones");
			return true;
		}
		return false;
	}

	public <F extends FocusType> void storeFocusPolicySituation(LensContext<F> context, Task task, OperationResult result)
			throws SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return;
		}
		Set<String> currentSituations = focusContext.getObjectCurrent() != null ?
				new HashSet<>(focusContext.getObjectCurrent().asObjectable().getPolicySituation()) : Collections.emptySet();
		Set<String> newSituations = new HashSet<>(focusContext.getPolicySituations());
		PropertyDelta<String> situationsDelta = createSituationDelta(
				new ItemPath(FocusType.F_POLICY_SITUATION), currentSituations, newSituations);
		if (situationsDelta != null) {
			focusContext.swallowToProjectionWaveSecondaryDelta(situationsDelta);
		}
	}

	@Nullable
	private PropertyDelta<String> createSituationDelta(ItemPath path, Set<String> currentSituations, Set<String> newSituations)
			throws SchemaException {
		if (newSituations.equals(currentSituations)) {
			return null;
		}
		@SuppressWarnings({ "unchecked", "raw" })
		PropertyDelta<String> situationsDelta = (PropertyDelta<String>) DeltaBuilder.deltaFor(FocusType.class, prismContext)
				.item(path)
						.add(CollectionUtils.subtract(newSituations, currentSituations).toArray())
						.delete(CollectionUtils.subtract(currentSituations, newSituations).toArray())
				.asItemDelta();
		situationsDelta.setEstimatedOldValues(PrismPropertyValue.wrap(currentSituations));
		return situationsDelta;
	}

	private PropertyDelta<EvaluatedPolicyRuleTriggerType> createTriggerDelta(ItemPath path, Set<EvaluatedPolicyRuleTriggerType> currentTriggers, Set<EvaluatedPolicyRuleTriggerType> newTriggers)
			throws SchemaException {
		if (newTriggers.equals(currentTriggers)) {
			return null;
		}
		@SuppressWarnings({ "unchecked", "raw" })
		PropertyDelta<EvaluatedPolicyRuleTriggerType> triggersDelta = (PropertyDelta<EvaluatedPolicyRuleTriggerType>)
				DeltaBuilder.deltaFor(FocusType.class, prismContext)
						.item(path)
						.replace(newTriggers.toArray())			// TODO or add + delete?
						.asItemDelta();
		triggersDelta.setEstimatedOldValues(PrismPropertyValue.wrap(currentTriggers));
		return triggersDelta;
	}

	public <F extends FocusType> void addGlobalPoliciesToAssignments(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		PrismObject<SystemConfigurationType> systemConfiguration = context.getSystemConfiguration();
		if (systemConfiguration == null) {
			return;
		}
		// We need to consider object before modification here.
		LensFocusContext<F> focusContext = context.getFocusContext();
		PrismObject<F> focus = focusContext.getObjectCurrent();
		if (focus == null) {
			focus = focusContext.getObjectNew();		// only if it does not exist, let's try the new one
		}

		for (GlobalPolicyRuleType globalPolicyRule: systemConfiguration.asObjectable().getGlobalPolicyRule()) {
			ObjectSelectorType focusSelector = globalPolicyRule.getFocusSelector();
			if (!repositoryService.selectorMatches(focusSelector, focus, LOGGER,
					"Global policy rule "+globalPolicyRule.getName()+" focus selector: ")) {
				continue;
			}
			for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignmentTriple.getAllValues()) {
				for (EvaluatedAssignmentTargetImpl target : evaluatedAssignment.getRoles().getNonNegativeValues()) {
					if (!target.getAssignmentPath().last().isMatchingOrder() && !target.isDirectlyAssigned()) {
						// This is to be thought out well. It is of no use to include global policy rules
						// attached to meta-roles assigned to the role being assigned to the focus. But we certainly need to include rules
						// attached to all directly assigned roles (because they might be considered for assignment) as
						// well as all indirectly assigned roles but of the matching order (because of exclusion violation).
						continue;
					}
					if (!repositoryService.selectorMatches(globalPolicyRule.getTargetSelector(),
							target.getTarget(), LOGGER, "Global policy rule "+globalPolicyRule.getName()+" target selector: ")) {
						continue;
					}
					if (!isRuleConditionTrue(globalPolicyRule, focus, evaluatedAssignment, context, task, result)) {
						LOGGER.trace("Skipping global policy rule because the condition evaluated to false: {}", globalPolicyRule);
						continue;
					}
					EvaluatedPolicyRule evaluatedRule = new EvaluatedPolicyRuleImpl(globalPolicyRule, target.getAssignmentPath().clone());
					boolean direct = target.isDirectlyAssigned() || target.getAssignmentPath().getFirstOrderChain().size() == 1;
					if (direct) {
						evaluatedAssignment.addThisTargetPolicyRule(evaluatedRule);
					} else {
						evaluatedAssignment.addOtherTargetPolicyRule(evaluatedRule);
					}
				}
			}
		}
	}

	private <F extends FocusType> boolean isRuleConditionTrue(GlobalPolicyRuleType globalPolicyRule, PrismObject<F> focus,
			EvaluatedAssignmentImpl<F> evaluatedAssignment, LensContext<F> context, Task task, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		MappingType condition = globalPolicyRule.getCondition();
		if (condition == null) {
			return true;
		}

		Mapping.Builder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder = mappingFactory
				.createMappingBuilder();
		ObjectDeltaObject<F> focusOdo = new ObjectDeltaObject<>(focus, null, focus);
		builder = builder.mappingType(condition)
				.contextDescription("condition in global policy rule " + globalPolicyRule.getName())
				.sourceContext(focusOdo)
				.defaultTargetDefinition(
						new PrismPropertyDefinitionImpl<>(CONDITION_OUTPUT_NAME, DOMUtil.XSD_BOOLEAN, prismContext))
				.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo)
				.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo)
				.addVariableDefinition(ExpressionConstants.VAR_TARGET, evaluatedAssignment.getTarget())
				.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, evaluatedAssignment)                // TODO: ok?
				.rootNode(focusOdo);

		Mapping<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

		mappingEvaluator.evaluateMapping(mapping, context, task, result);

		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = mapping.getOutputTriple();
		return conditionTriple != null && ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());	// TODO: null -> true (in the method) - ok?
	}

	public <F extends FocusType, T extends FocusType> void applyAssignmentSituationOnAdd(LensContext<F> context,
			PrismObject<T> objectToAdd) throws SchemaException {
		if (context.getEvaluatedAssignmentTriple() == null) {
			return;
		}
		T focus = objectToAdd.asObjectable();
		for (EvaluatedAssignmentImpl<?> evaluatedAssignment : context.getEvaluatedAssignmentTriple().getNonNegativeValues()) {
			LOGGER.trace("Applying assignment situation on object ADD for {}", evaluatedAssignment);
			List<EvaluatedPolicyRuleTriggerType> triggers = getTriggers(evaluatedAssignment);
			if (!shouldSituationBeUpdated(evaluatedAssignment, triggers)) {
				continue;
			}
			AssignmentType assignment = evaluatedAssignment.getAssignmentType();
			if (assignment.getId() != null) {
				ItemDelta.applyTo(getAssignmentModificationDelta(evaluatedAssignment, triggers), objectToAdd);
			} else {
				int i = focus.getAssignment().indexOf(assignment);
				if (i < 0) {
					throw new IllegalStateException("Assignment to be replaced not found in an object to add: " + assignment + " / " + objectToAdd);
				}
				copyPolicyData(focus.getAssignment().get(i), evaluatedAssignment, triggers);
			}
		}
	}

	private List<EvaluatedPolicyRuleTriggerType> getTriggers(EvaluatedAssignmentImpl<?> evaluatedAssignment) {
		List<EvaluatedPolicyRuleTriggerType> rv = new ArrayList<>();
		for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getAllTargetsPolicyRules()) {
			for (EvaluatedPolicyRuleTrigger<?> trigger : policyRule.getTriggers()) {
				EvaluatedPolicyRuleTriggerType triggerType = trigger.toEvaluatedPolicyRuleTriggerType(policyRule).clone();
				simplifyTrigger(triggerType);
				rv.add(triggerType);
			}
		}
		return rv;
	}

	private void simplifyTrigger(EvaluatedPolicyRuleTriggerType trigger) {
		deleteAssignments(trigger.getAssignmentPath());
		if (trigger instanceof EvaluatedExclusionTriggerType) {
			EvaluatedExclusionTriggerType exclusionTrigger = (EvaluatedExclusionTriggerType) trigger;
			deleteAssignments(exclusionTrigger.getConflictingObjectPath());
			exclusionTrigger.setConflictingAssignment(null);
		} else if (trigger instanceof EvaluatedSituationTriggerType) {
			for (EvaluatedPolicyRuleType sourceRule : ((EvaluatedSituationTriggerType) trigger).getSourceRule()) {
				for (EvaluatedPolicyRuleTriggerType sourceTrigger : sourceRule.getTrigger()) {
					simplifyTrigger(sourceTrigger);
				}
			}
		}
	}

	private void deleteAssignments(AssignmentPathType path) {
		if (path == null) {
			return;
		}
		for (AssignmentPathSegmentType segment : path.getSegment()) {
			segment.setAssignment(null);
		}
	}

	public <F extends FocusType, T extends ObjectType> ObjectDelta<T> applyAssignmentSituationOnModify(LensContext<F> context,
			ObjectDelta<T> objectDelta) throws SchemaException {
		if (context.getEvaluatedAssignmentTriple() == null) {
			return objectDelta;
		}
		for (EvaluatedAssignmentImpl<?> evaluatedAssignment : context.getEvaluatedAssignmentTriple().getNonNegativeValues()) {
			LOGGER.trace("Applying assignment situation on object MODIFY for {}", evaluatedAssignment);
			List<EvaluatedPolicyRuleTriggerType> triggers = getTriggers(evaluatedAssignment);
			if (!shouldSituationBeUpdated(evaluatedAssignment, triggers)) {
				continue;
			}
			AssignmentType assignment = evaluatedAssignment.getAssignmentType();
			if (assignment.getId() != null) {
				objectDelta = swallow(context, objectDelta, getAssignmentModificationDelta(evaluatedAssignment, triggers));
			} else {
				if (objectDelta == null) {
					throw new IllegalStateException("No object delta!");
				}
				ContainerDelta<Containerable> assignmentDelta = objectDelta.findContainerDelta(FocusType.F_ASSIGNMENT);
				if (assignmentDelta == null) {
					throw new IllegalStateException("Unnumbered assignment (" + assignment
							+ ") couldn't be found in object delta (no assignment modification): " + objectDelta);
				}
				PrismContainerValue<AssignmentType> assignmentInDelta = assignmentDelta.findValueToAddOrReplace(assignment.asPrismContainerValue());
				if (assignmentInDelta == null) {
					throw new IllegalStateException("Unnumbered assignment (" + assignment
							+ ") couldn't be found in object delta (no corresponding assignment value): " + objectDelta);
				}
				copyPolicyData(assignmentInDelta.asContainerable(), evaluatedAssignment, triggers);
			}
		}
		return objectDelta;
	}

	private <T extends ObjectType, F extends FocusType> ObjectDelta<T> swallow(LensContext<F> context, ObjectDelta<T> objectDelta,
			List<ItemDelta<?,?>> deltas) throws SchemaException {
		if (deltas.isEmpty()) {
			return objectDelta;
		}
		if (objectDelta == null) {
			if (context.getFocusClass() == null) {
				throw new IllegalStateException("No focus class in " + context);
			}
			if (context.getFocusContext() == null) {
				throw new IllegalStateException("No focus context in " + context);
			}
			objectDelta = (ObjectDelta) new ObjectDelta<F>(context.getFocusClass(), ChangeType.MODIFY, prismContext);
			objectDelta.setOid(context.getFocusContext().getOid());
		}
		objectDelta.swallow(deltas);
		return objectDelta;
	}

	private void copyPolicyData(AssignmentType targetAssignment, EvaluatedAssignmentImpl<?> evaluatedAssignment,
			List<EvaluatedPolicyRuleTriggerType> triggers) {
		targetAssignment.getPolicySituation().clear();
		targetAssignment.getPolicySituation().addAll(evaluatedAssignment.getPolicySituations());
		targetAssignment.getTrigger().clear();
		targetAssignment.getTrigger().addAll(triggers);
	}

	public <O extends ObjectType> ObjectDelta<O> applyAssignmentSituation(LensContext<O> context, ObjectDelta<O> focusDelta)
			throws SchemaException {
		if (context.getFocusClass() == null || !FocusType.class.isAssignableFrom(context.getFocusClass())) {
			return focusDelta;
		}
		LensContext<? extends FocusType> contextOfFocus = (LensContext<FocusType>) context;
		if (focusDelta != null && focusDelta.isAdd()) {
			applyAssignmentSituationOnAdd(contextOfFocus, (PrismObject<? extends FocusType>) focusDelta.getObjectToAdd());
			return focusDelta;
		} else if (focusDelta == null || focusDelta.isModify()) {
			return applyAssignmentSituationOnModify(contextOfFocus, focusDelta);
		} else {
			return focusDelta;
		}
	}
}
