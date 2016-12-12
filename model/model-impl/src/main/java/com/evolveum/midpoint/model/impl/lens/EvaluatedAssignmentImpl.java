/*
 * Copyright (c) 2010-2016 Evolveum
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

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.jetbrains.annotations.NotNull;

/**
 * Evaluated assignment that contains all constructions and authorizations from the assignment 
 * itself and all the applicable inducements from all the roles referenced from the assignment.
 * 
 * @author Radovan Semancik
 */
public class EvaluatedAssignmentImpl<F extends FocusType> implements EvaluatedAssignment<F> {
	
	private static final Trace LOGGER = TraceManager.getTrace(EvaluatedAssignmentImpl.class);

	private ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi;
	private DeltaSetTriple<Construction<F>> constructions;
	private DeltaSetTriple<EvaluatedAssignmentTargetImpl> roles;
	private Collection<PrismReferenceValue> orgRefVals;
	private Collection<PrismReferenceValue> membershipRefVals;
	private Collection<PrismReferenceValue> delegationRefVals;
	private Collection<Authorization> authorizations;
	private Collection<Mapping<? extends PrismPropertyValue<?>,? extends PrismPropertyDefinition<?>>> focusMappings;
	private Collection<AdminGuiConfigurationType> adminGuiConfigurations;
	@NotNull private final Collection<EvaluatedPolicyRule> focusPolicyRules;	// rules related to the focus itself
	@NotNull private final Collection<EvaluatedPolicyRule> targetPolicyRules;	// rules related to the target of this assignment
	// rules directly related to the target of this assignment - should be a subset of targetPolicyRules
	// (this means that if a reference is in thisTargetPolicyRules, then the same reference should
	// be in targetPolicyRules)
	@NotNull private final Collection<EvaluatedPolicyRule> thisTargetPolicyRules;
	private PrismObject<?> target;
	private boolean isValid;
	private boolean forceRecon;         // used also to force recomputation of parentOrgRefs
	private boolean presentInCurrentObject;
	private boolean presentInOldObject;
	private Collection<String> policySituations = new ArrayList<>();

	public EvaluatedAssignmentImpl() {
		constructions = new DeltaSetTriple<>();
		roles = new DeltaSetTriple<>();
		orgRefVals = new ArrayList<>();
		membershipRefVals = new ArrayList<>();
		delegationRefVals = new ArrayList<>();
		authorizations = new ArrayList<>();
		focusMappings = new ArrayList<>();
		adminGuiConfigurations = new ArrayList<>(); 
		focusPolicyRules = new ArrayList<>();
		targetPolicyRules = new ArrayList<>();
		thisTargetPolicyRules = new ArrayList<>();
	}

	public ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> getAssignmentIdi() {
		return assignmentIdi;
	}

	public void setAssignmentIdi(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi) {
		this.assignmentIdi = assignmentIdi;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.impl.lens.EvaluatedAssignment#getAssignmentType()
	 */
	@Override
	public AssignmentType getAssignmentType() {
		return assignmentIdi.getItemNew().getValue(0).asContainerable();
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
		return targetRef.getRelation();
	}

	public DeltaSetTriple<Construction<F>> getConstructions() {
		return constructions;
	}

	/**
	 * Construction is not a part of model-api. To avoid heavy refactoring at present time, there is not a classical
	 * Construction-ConstructionImpl separation, but we use artificial (simplified) EvaluatedConstruction
	 * API class instead.
	 *
	 * @return
	 */
	public DeltaSetTriple<EvaluatedConstruction> getEvaluatedConstructions(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		DeltaSetTriple<EvaluatedConstruction> rv = new DeltaSetTriple<>();
		for (PlusMinusZero whichSet : PlusMinusZero.values()) {
			Collection<Construction<F>> constructionSet = constructions.getSet(whichSet);
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
            case ZERO: return getConstructions().getZeroSet();
            case PLUS: return getConstructions().getPlusSet();
            case MINUS: return getConstructions().getMinusSet();
            default: throw new IllegalArgumentException("whichSet: " + whichSet);
        }
    }

	public void addConstructionZero(Construction<F> contruction) {
		constructions.addToZeroSet(contruction);
	}
	
	public void addConstructionPlus(Construction<F> contruction) {
		constructions.addToPlusSet(contruction);
	}
	
	public void addConstructionMinus(Construction<F> contruction) {
		constructions.addToMinusSet(contruction);
	}
	
	@Override
	public DeltaSetTriple<EvaluatedAssignmentTargetImpl> getRoles() {
		return roles;
	}
	
	public void addRole(EvaluatedAssignmentTargetImpl role, PlusMinusZero mode) {
		roles.addToSet(mode, role);
	}

	public Collection<PrismReferenceValue> getOrgRefVals() {
		return orgRefVals;
	}

	public void addOrgRefVal(PrismReferenceValue org) {
		orgRefVals.add(org);
	}
	
	public Collection<PrismReferenceValue> getMembershipRefVals() {
		return membershipRefVals;
	}

	public void addMembershipRefVal(PrismReferenceValue org) {
		membershipRefVals.add(org);
	}

	public Collection<PrismReferenceValue> getDelegationRefVals() {
		return delegationRefVals;
	}

	public void addDelegationRefVal(PrismReferenceValue org) {
		delegationRefVals.add(org);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.impl.lens.EvaluatedAssignment#getAuthorizations()
	 */
	@Override
	public Collection<Authorization> getAuthorizations() {
		return authorizations;
	}
	
	public void addAuthorization(Authorization authorization) {
		authorizations.add(authorization);
	}
	
	public Collection<AdminGuiConfigurationType> getAdminGuiConfigurations() {
		return adminGuiConfigurations;
	}
	
	public void addAdminGuiConfiguration(AdminGuiConfigurationType adminGuiConfiguration) {
		adminGuiConfigurations.add(adminGuiConfiguration);
	}

	public Collection<Mapping<? extends PrismPropertyValue<?>,? extends PrismPropertyDefinition<?>>> getFocusMappings() {
		return focusMappings;
	}

	public void addFocusMapping(Mapping<? extends PrismPropertyValue<?>,? extends PrismPropertyDefinition<?>> focusMapping) {
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

	public boolean isForceRecon() {
		return forceRecon;
	}

	public void setForceRecon(boolean forceRecon) {
		this.forceRecon = forceRecon;
	}

	public Collection<ResourceType> getResources(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		Collection<ResourceType> resources = new ArrayList<ResourceType>();
		for (Construction<F> acctConstr: constructions.getAllValues()) {
			resources.add(acctConstr.getResource(task, result));
		}
		return resources;
	}

	// System configuration is used only to provide $configuration script variable (MID-2372)
	public void evaluateConstructions(ObjectDeltaObject<F> focusOdo, PrismObject<SystemConfigurationType> systemConfiguration, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		for (Construction<F> construction :constructions.getAllValues()) {
			construction.setFocusOdo(focusOdo);
			construction.setSystemConfiguration(systemConfiguration);
			LOGGER.trace("Evaluating construction '{}' in {}", construction, construction.getSource());
			construction.evaluate(task, result);
		}
	}

	public void evaluateConstructions(ObjectDeltaObject<F> focusOdo, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
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
	public Collection<EvaluatedPolicyRule> getTargetPolicyRules() {
		return targetPolicyRules;
	}

	public void addTargetPolicyRule(EvaluatedPolicyRule policyRule) {
		targetPolicyRules.add(policyRule);
	}

	@NotNull
	public Collection<EvaluatedPolicyRule> getThisTargetPolicyRules() {
		return thisTargetPolicyRules;
	}

	public void addThisTargetPolicyRule(EvaluatedPolicyRule policyRule) {
		thisTargetPolicyRules.add(policyRule);
	}

	public void addLegacyPolicyConstraints(PolicyConstraintsType constraints) {
		if (!constraints.getModification().isEmpty()) {
			PolicyConstraintsType focusConstraints = constraints.clone();
			focusConstraints.getAssignment().clear();
			focusConstraints.getMaxAssignees().clear();
			focusConstraints.getMinAssignees().clear();
			focusConstraints.getExclusion().clear();
			focusPolicyRules.add(toEvaluatedPolicyRule(focusConstraints));
		}
		if (!constraints.getMinAssignees().isEmpty() || !constraints.getMaxAssignees().isEmpty()
				|| !constraints.getAssignment().isEmpty() || !constraints.getExclusion().isEmpty()) {
			PolicyConstraintsType targetConstraints = constraints.clone();
			targetConstraints.getModification().clear();
			EvaluatedPolicyRule evaluatedPolicyRule = toEvaluatedPolicyRule(targetConstraints);
			targetPolicyRules.add(evaluatedPolicyRule);
			thisTargetPolicyRules.add(evaluatedPolicyRule);
		}
	}

	@NotNull
	private EvaluatedPolicyRule toEvaluatedPolicyRule(PolicyConstraintsType constraints) {
		PolicyRuleType policyRuleType = new PolicyRuleType();
		policyRuleType.setPolicyConstraints(constraints);
		return new EvaluatedPolicyRuleImpl(policyRuleType, null);
	}

	@Override
	public Collection<String> getPolicySituations() {
		return policySituations;
	}

	@Override
	public void triggerConstraint(EvaluatedPolicyRule rule, EvaluatedPolicyRuleTrigger trigger) throws PolicyViolationException {
		LensUtil.triggerConstraint(rule, trigger, policySituations);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "EvaluatedAssignment", indent);
		DebugUtil.debugDumpWithLabel(sb, "isValid", isValid, indent + 1);
        if (forceRecon) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "forceRecon", forceRecon, indent + 1);
        }
		if (!constructions.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "Constructions", constructions, indent+1);
		}
		if (!roles.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "Roles", roles, indent+1);
		}
		if (!orgRefVals.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Orgs", indent+1);
			for (PrismReferenceValue org: orgRefVals) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+2);
				sb.append(org.toString());
			}
		}
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
			for (PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> mapping: focusMappings) {
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
		DebugUtil.debugDumpWithLabelLn(sb, "focusPolicyRules", focusPolicyRules, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "targetPolicyRules", targetPolicyRules, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Present in old object", isPresentInOldObject(), indent+1);
		DebugUtil.debugDumpWithLabel(sb, "Present in current object", isPresentInCurrentObject(), indent+1);
		return sb.toString();
	}

	@Override
	public String toString() {
		return "EvaluatedAssignment(constr=" + constructions + "; org="+orgRefVals+"; autz="+authorizations+"; "+focusMappings.size()+" focus mappings; "+ focusPolicyRules
				.size()+" rules)";
	}
}
