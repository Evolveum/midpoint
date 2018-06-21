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
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.prism.PrismContainerValue.asContainerable;
import static com.evolveum.midpoint.prism.delta.PlusMinusZero.MINUS;
import static com.evolveum.midpoint.prism.delta.PlusMinusZero.PLUS;
import static com.evolveum.midpoint.prism.delta.PlusMinusZero.ZERO;

/**
 * Evaluated assignment that contains all constructions and authorizations from the assignment
 * itself and all the applicable inducements from all the roles referenced from the assignment.
 *
 * @author Radovan Semancik
 */
public class EvaluatedAssignmentImpl<F extends FocusType> implements EvaluatedAssignment<F> {

	private static final Trace LOGGER = TraceManager.getTrace(EvaluatedAssignmentImpl.class);

	@NotNull private final ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi;
	private final boolean evaluatedOld;
	@NotNull private final DeltaSetTriple<Construction<F>> constructionTriple = new DeltaSetTriple<>();
	@NotNull private final DeltaSetTriple<PersonaConstruction<F>> personaConstructionTriple = new DeltaSetTriple<>();
	@NotNull private final DeltaSetTriple<EvaluatedAssignmentTargetImpl> roles = new DeltaSetTriple<>();
	@NotNull private final Collection<PrismReferenceValue> orgRefVals = new ArrayList<>();
	@NotNull private final Collection<PrismReferenceValue> membershipRefVals = new ArrayList<>();
	@NotNull private final Collection<PrismReferenceValue> delegationRefVals = new ArrayList<>();
	@NotNull private final Collection<Authorization> authorizations = new ArrayList<>();
	@NotNull private final Collection<MappingImpl<?,?>> focusMappings = new ArrayList<>();
	@NotNull private final Collection<AdminGuiConfigurationType> adminGuiConfigurations = new ArrayList<>();
	// rules related to the focal object (typically e.g. "forbid modifications")
	@NotNull private final Collection<EvaluatedPolicyRule> focusPolicyRules = new ArrayList<>();
	// rules related to the target of this assignment (typically e.g. "approve the assignment")
	@NotNull private final Collection<EvaluatedPolicyRule> thisTargetPolicyRules = new ArrayList<>();
	// rules related to other targets provided by this assignment (e.g. induced or obtained by delegation)
	// usually, these rules do not cause direct action (e.g. in the case of approvals);
	// however, there are situations in which they are used (e.g. for exclusion rules)
	@NotNull private final Collection<EvaluatedPolicyRule> otherTargetsPolicyRules = new ArrayList<>();

	private PrismObject<?> target;
	private boolean isValid;
	private boolean wasValid;
	private boolean forceRecon;         // used also to force recomputation of parentOrgRefs
	private boolean presentInCurrentObject;
	private boolean presentInOldObject;
	private Collection<String> policySituations = new HashSet<>();

	public EvaluatedAssignmentImpl(
			@NotNull ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi,
			boolean evaluatedOld) {
		this.assignmentIdi = assignmentIdi;
		this.evaluatedOld = evaluatedOld;
	}

	@NotNull
	public ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> getAssignmentIdi() {
		return assignmentIdi;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.impl.lens.EvaluatedAssignment#getAssignmentType()
	 */
	@Override
	public AssignmentType getAssignmentType() {
		return asContainerable(assignmentIdi.getSingleValue(evaluatedOld));
	}

	@Override
	public Long getAssignmentId() {
		Item<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> any = assignmentIdi.getAnyItem();
		return any != null && !any.getValues().isEmpty() ? any.getValue(0).getId() : null;
	}

	@Override
	public AssignmentType getAssignmentType(boolean old) {
		return asContainerable(assignmentIdi.getSingleValue(old));
	}

	@Override
	public QName getRelation() {
		AssignmentType assignmentType = getAssignmentType();
		if (assignmentType == null) {
			return null;
		}
		ObjectReferenceType targetRef = assignmentType.getTargetRef();
		if (targetRef == null) {
			return null;
		}
		return ObjectTypeUtil.normalizeRelation(targetRef.getRelation());
	}

	@NotNull
	public DeltaSetTriple<Construction<F>> getConstructionTriple() {
		return constructionTriple;
	}

	/**
	 * Construction is not a part of model-api. To avoid heavy refactoring at present time, there is not a classical
	 * Construction-ConstructionImpl separation, but we use artificial (simplified) EvaluatedConstruction
	 * API class instead.
	 *
	 * @return
	 */
	@Override
	public DeltaSetTriple<EvaluatedConstruction> getEvaluatedConstructions(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		DeltaSetTriple<EvaluatedConstruction> rv = new DeltaSetTriple<>();
		for (PlusMinusZero whichSet : PlusMinusZero.values()) {
			Collection<Construction<F>> constructionSet = constructionTriple.getSet(whichSet);
			if (constructionSet != null) {
				for (Construction<F> construction : constructionSet) {
					rv.addToSet(whichSet, new EvaluatedConstructionImpl(construction, task, result));
				}
			}
		}
		return rv;
	}


	public Collection<Construction<F>> getConstructionSet(PlusMinusZero whichSet) {
        switch (whichSet) {
            case ZERO: return getConstructionTriple().getZeroSet();
            case PLUS: return getConstructionTriple().getPlusSet();
            case MINUS: return getConstructionTriple().getMinusSet();
            default: throw new IllegalArgumentException("whichSet: " + whichSet);
        }
    }

	public void addConstruction(Construction<F> contruction, PlusMinusZero whichSet) {
		switch (whichSet) {
            case ZERO:
            	constructionTriple.addToZeroSet(contruction);
            	break;
            case PLUS:
            	constructionTriple.addToPlusSet(contruction);
            	break;
            case MINUS:
            	constructionTriple.addToMinusSet(contruction);
            	break;
            default:
            	throw new IllegalArgumentException("whichSet: " + whichSet);
        }
	}

	@NotNull
	public DeltaSetTriple<PersonaConstruction<F>> getPersonaConstructionTriple() {
		return personaConstructionTriple;
	}

	public void addPersonaConstruction(PersonaConstruction<F> personaContruction, PlusMinusZero whichSet) {
		switch (whichSet) {
            case ZERO:
            	personaConstructionTriple.addToZeroSet(personaContruction);
            	break;
            case PLUS:
            	personaConstructionTriple.addToPlusSet(personaContruction);
            	break;
            case MINUS:
            	personaConstructionTriple.addToMinusSet(personaContruction);
            	break;
            default:
            	throw new IllegalArgumentException("whichSet: " + whichSet);
        }
	}

	@NotNull
	@Override
	public DeltaSetTriple<EvaluatedAssignmentTargetImpl> getRoles() {
		return roles;
	}

	public void addRole(EvaluatedAssignmentTargetImpl role, PlusMinusZero mode) {
		roles.addToSet(mode, role);
	}

	@NotNull
	public Collection<PrismReferenceValue> getOrgRefVals() {
		return orgRefVals;
	}

	public void addOrgRefVal(PrismReferenceValue org) {
		orgRefVals.add(org);
	}

	@NotNull
	public Collection<PrismReferenceValue> getMembershipRefVals() {
		return membershipRefVals;
	}

	public void addMembershipRefVal(PrismReferenceValue org) {
		membershipRefVals.add(org);
	}

	@NotNull
	public Collection<PrismReferenceValue> getDelegationRefVals() {
		return delegationRefVals;
	}

	public void addDelegationRefVal(PrismReferenceValue org) {
		delegationRefVals.add(org);
	}

	@NotNull
	@Override
	public Collection<Authorization> getAuthorizations() {
		return authorizations;
	}

	public void addAuthorization(Authorization authorization) {
		authorizations.add(authorization);
	}

	@NotNull
	public Collection<AdminGuiConfigurationType> getAdminGuiConfigurations() {
		return adminGuiConfigurations;
	}

	public void addAdminGuiConfiguration(AdminGuiConfigurationType adminGuiConfiguration) {
		adminGuiConfigurations.add(adminGuiConfiguration);
	}

	@NotNull
	public Collection<MappingImpl<?,?>> getFocusMappings() {
		return focusMappings;
	}

	public void addFocusMapping(MappingImpl<? extends PrismPropertyValue<?>,? extends PrismPropertyDefinition<?>> focusMapping) {
		this.focusMappings.add(focusMapping);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.impl.lens.EvaluatedAssignment#getTarget()
	 */
	@Override
	public PrismObject<?> getTarget() {
		return target;
	}

	public void setTarget(PrismObject<?> target) {
		this.target = target;
	}

	/* (non-Javadoc)
         * @see com.evolveum.midpoint.model.impl.lens.EvaluatedAssignment#isValid()
         */
	@Override
	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public boolean getWasValid() {
		return wasValid;
	}

	public void setWasValid(boolean wasValid) {
		this.wasValid = wasValid;
	}

	public boolean isForceRecon() {
		return forceRecon;
	}

	public void setForceRecon(boolean forceRecon) {
		this.forceRecon = forceRecon;
	}

	public Collection<ResourceType> getResources(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		Collection<ResourceType> resources = new ArrayList<>();
		for (Construction<F> acctConstr: constructionTriple.getAllValues()) {
			resources.add(acctConstr.getResource(task, result));
		}
		return resources;
	}

	// System configuration is used only to provide $configuration script variable (MID-2372)
	public void evaluateConstructions(ObjectDeltaObject<F> focusOdo, PrismObject<SystemConfigurationType> systemConfiguration, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
		for (Construction<F> construction :constructionTriple.getAllValues()) {
			construction.setFocusOdo(focusOdo);
			construction.setSystemConfiguration(systemConfiguration);
			construction.setWasValid(wasValid);
			LOGGER.trace("Evaluating construction '{}' in {}", construction, construction.getSource());
			construction.evaluate(task, result);
		}
	}

	public void evaluateConstructions(ObjectDeltaObject<F> focusOdo, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
		evaluateConstructions(focusOdo, null, task, result);
	}

	public void setPresentInCurrentObject(boolean presentInCurrentObject) {
		this.presentInCurrentObject = presentInCurrentObject;
	}

	public void setPresentInOldObject(boolean presentInOldObject) {
		this.presentInOldObject = presentInOldObject;
	}

	@Override
	public boolean isPresentInCurrentObject() {
		return presentInCurrentObject;
	}

	@Override
	public boolean isPresentInOldObject() {
		return presentInOldObject;
	}

	@NotNull
	public Collection<EvaluatedPolicyRule> getFocusPolicyRules() {
		return focusPolicyRules;
	}

	public void addFocusPolicyRule(EvaluatedPolicyRule policyRule) {
		focusPolicyRules.add(policyRule);
	}

	@NotNull
	public Collection<EvaluatedPolicyRule> getThisTargetPolicyRules() {
		return thisTargetPolicyRules;
	}

	public void addThisTargetPolicyRule(EvaluatedPolicyRule policyRule) {
		thisTargetPolicyRules.add(policyRule);
	}

	@NotNull
	public Collection<EvaluatedPolicyRule> getOtherTargetsPolicyRules() {
		return otherTargetsPolicyRules;
	}

	public void addOtherTargetPolicyRule(EvaluatedPolicyRule policyRule) {
		otherTargetsPolicyRules.add(policyRule);
	}

	@NotNull
	public Collection<EvaluatedPolicyRule> getAllTargetsPolicyRules() {
		return Stream.concat(thisTargetPolicyRules.stream(), otherTargetsPolicyRules.stream()).collect(Collectors.toList());
	}

	public void addLegacyPolicyConstraints(PolicyConstraintsType constraints, AssignmentPath assignmentPath,
			FocusType directOwner, PrismContext prismContext) {
		// approximate solution - just add the constraints to all the places; hopefully any misplaced ones would be simply ignored
		if (constraints == null) {
			return;
		}
		otherTargetsPolicyRules.add(toEvaluatedPolicyRule(constraints, assignmentPath, directOwner, prismContext));
		thisTargetPolicyRules.add(toEvaluatedPolicyRule(constraints, assignmentPath, directOwner, prismContext));
		focusPolicyRules.add(toEvaluatedPolicyRule(constraints, assignmentPath, directOwner, prismContext));
	}

	@NotNull
	private EvaluatedPolicyRule toEvaluatedPolicyRule(PolicyConstraintsType constraints, AssignmentPath assignmentPath,
			FocusType directOwner, PrismContext prismContext) {
		PolicyRuleType policyRuleType = new PolicyRuleType();
		policyRuleType.setPolicyConstraints(constraints);
		PolicyActionsType policyActionsType = new PolicyActionsType();
		policyActionsType.setEnforcement(new EnforcementPolicyActionType());
		policyRuleType.setPolicyActions(policyActionsType);
		return new EvaluatedPolicyRuleImpl(policyRuleType, assignmentPath, prismContext);
	}

	@Override
	public Collection<String> getPolicySituations() {
		return policySituations;
	}

	@Override
	public void triggerRule(@NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
		boolean hasException = processRuleExceptions(this, rule, triggers);

		for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
			if (trigger instanceof EvaluatedExclusionTrigger) {
				EvaluatedExclusionTrigger exclTrigger = (EvaluatedExclusionTrigger) trigger;
				if (exclTrigger.getConflictingAssignment() != null) {
					hasException =
							hasException || processRuleExceptions((EvaluatedAssignmentImpl<F>) exclTrigger.getConflictingAssignment(),
									rule, triggers);
				}
			}
		}

		if (!hasException) {
			LensUtil.triggerRule(rule, triggers, policySituations);
		}
	}

	@Override
	public void triggerConstraintLegacy(EvaluatedPolicyRuleTrigger trigger,
			LocalizationService localizationService) throws PolicyViolationException {
		LensUtil.triggerConstraintLegacy(trigger, policySituations, localizationService);
	}

	private boolean processRuleExceptions(EvaluatedAssignmentImpl<F> evaluatedAssignment, @NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
		boolean hasException = false;
		for (PolicyExceptionType policyException: evaluatedAssignment.getAssignmentType().getPolicyException()) {
			if (policyException.getRuleName().equals(rule.getName())) {
				LensUtil.processRuleWithException(rule, triggers, policyException);
				hasException = true;
			}
		}
		return hasException;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "EvaluatedAssignment", indent);
		DebugUtil.debugDumpWithLabelLn(sb, "assignment old", String.valueOf(assignmentIdi.getItemOld()), indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "assignment delta", String.valueOf(assignmentIdi.getDelta()), indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "assignment new", String.valueOf(assignmentIdi.getItemNew()), indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "evaluatedOld", evaluatedOld, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "target", String.valueOf(target), indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "isValid", isValid, indent + 1);
        if (forceRecon) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "forceRecon", forceRecon, indent + 1);
        }
        sb.append("\n");
		if (constructionTriple.isEmpty()) {
			DebugUtil.debugDumpWithLabel(sb, "Constructions", "(empty)", indent+1);
		} else {
			DebugUtil.debugDumpWithLabel(sb, "Constructions", constructionTriple, indent+1);
		}
		if (!personaConstructionTriple.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "Persona constructions", personaConstructionTriple, indent+1);
		}
		if (!roles.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "Roles", roles, indent+1);
		}
		dumpRefList(indent, sb, "Orgs", orgRefVals);
		dumpRefList(indent, sb, "Membership", membershipRefVals);
		dumpRefList(indent, sb, "Delegation", delegationRefVals);
		if (!authorizations.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Authorizations", indent+1);
			for (Authorization autz: authorizations) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+2);
				sb.append(autz.toString());
			}
		}
		if (!focusMappings.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Focus Mappings", indent+1);
			for (PrismValueDeltaSetTripleProducer<?,?> mapping: focusMappings) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+2);
				sb.append(mapping.toString());
			}
		}
		if (target != null) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "Target", target.toString(), indent+1);
		}
		sb.append("\n");
		DebugUtil.debugDumpWithLabelLn(sb, "focusPolicyRules " + ruleCountInfo(focusPolicyRules), focusPolicyRules, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "thisTargetPolicyRules " + ruleCountInfo(thisTargetPolicyRules), thisTargetPolicyRules, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "otherTargetsPolicyRules " + ruleCountInfo(otherTargetsPolicyRules), otherTargetsPolicyRules, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Present in old object", isPresentInOldObject(), indent+1);
		DebugUtil.debugDumpWithLabel(sb, "Present in current object", isPresentInCurrentObject(), indent+1);
		return sb.toString();
	}

	private String ruleCountInfo(Collection<EvaluatedPolicyRule> rules) {
		return "(" + rules.size() + ", triggered " + LensContext.getTriggeredRulesCount(rules) + ")";
	}

	private void dumpRefList(int indent, StringBuilder sb, String label, Collection<PrismReferenceValue> referenceValues) {
		if (!referenceValues.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, label, indent+1);
			for (PrismReferenceValue refVal: referenceValues) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+2);
				sb.append(refVal.toString());
			}
		}
	}

	@Override
	public String toString() {
		return "EvaluatedAssignment(target=" + target + "; constr=" + constructionTriple + "; org="+orgRefVals+"; autz="+authorizations+"; "+focusMappings.size()+" focus mappings; "+ focusPolicyRules
				.size()+" rules)";
	}

	public String toHumanReadableString() {
		if (target != null) {
			return "EvaluatedAssignment(" + target + ")";
		} else if (!constructionTriple.isEmpty()) {
			return "EvaluatedAssignment(" + constructionTriple + ")";
		} else if (!personaConstructionTriple.isEmpty()) {
			return "EvaluatedAssignment(" + personaConstructionTriple + ")";
		} else {
			return toString();
		}
	}

	public List<EvaluatedAssignmentTargetImpl> getNonNegativeTargets() {
		List<EvaluatedAssignmentTargetImpl> rv = new ArrayList<>();
		rv.addAll(roles.getZeroSet());
		rv.addAll(roles.getPlusSet());
		return rv;
	}

	/**
	 * @return mode (adding, deleting, keeping) with respect to the *current* object (not the old one)
	 */
	@NotNull
	public PlusMinusZero getMode() {
		if (assignmentIdi.getItemNew() == null || assignmentIdi.getItemNew().isEmpty()) {
			return MINUS;
		} else if (presentInCurrentObject) {
			return ZERO;
		} else {
			return PLUS;
		}
	}
}
