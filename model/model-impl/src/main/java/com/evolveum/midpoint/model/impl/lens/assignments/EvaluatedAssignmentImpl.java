/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.assignments;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.construction.*;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.AssignedFocusMappingEvaluationRequest;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.annotations.VisibleForTesting;
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
public class EvaluatedAssignmentImpl<AH extends AssignmentHolderType> implements EvaluatedAssignment {

    @NotNull private final ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi;
    private final boolean evaluatedOld;

    /**
     * Constructions collected from this assignment. They are categorized to plus/minus/zero sets
     * according to holding assignment relativity mode (absolute w.r.t. focus, relative w.r.t. primary assignment).
     */
    @NotNull private final DeltaSetTriple<AssignedResourceObjectConstruction<AH>> constructionTriple;

    @NotNull private final DeltaSetTriple<PersonaConstruction<AH>> personaConstructionTriple;
    @NotNull private final DeltaSetTriple<EvaluatedAssignmentTargetImpl> roles;
    @NotNull private final Collection<PrismReferenceValue> orgRefVals = new ArrayList<>();
    @NotNull private final Collection<PrismReferenceValue> archetypeRefVals = new ArrayList<>();
    @NotNull private final Collection<PrismReferenceValue> membershipRefVals = new ArrayList<>();
    @NotNull private final Collection<PrismReferenceValue> delegationRefVals = new ArrayList<>();
    @NotNull private final Collection<Authorization> authorizations = new ArrayList<>();

    /**
     * Requests to evaluate focus mappings. These are collected during assignment evaluation, but executed afterwards.
     * This is to implement proper mapping chaining.
     *
     * @since 4.0.1
     */
    @NotNull private final Collection<AssignedFocusMappingEvaluationRequest> focusMappingEvaluationRequests = new ArrayList<>();

    @NotNull private final Collection<MappingImpl<?,?>> focusMappings = new ArrayList<>();

    @NotNull private final Collection<AdminGuiConfigurationType> adminGuiConfigurations = new ArrayList<>();
    // rules related to the focal object (typically e.g. "forbid modifications")
    @NotNull private final Collection<EvaluatedPolicyRuleImpl> focusPolicyRules = new ArrayList<>();
    // rules related to the target of this assignment (typically e.g. "approve the assignment")
    @NotNull private final Collection<EvaluatedPolicyRuleImpl> thisTargetPolicyRules = new ArrayList<>();
    // rules related to other targets provided by this assignment (e.g. induced or obtained by delegation)
    // usually, these rules do not cause direct action (e.g. in the case of approvals);
    // however, there are situations in which they are used (e.g. for exclusion rules)
    @NotNull private final Collection<EvaluatedPolicyRuleImpl> otherTargetsPolicyRules = new ArrayList<>();
    private String tenantOid;

    private PrismObject<?> target;

    /**
     * Is the primary (evaluated) assignment valid in the new focus state, i.e.
     *  - active according to the activation and lifecycle state,
     *  - condition evaluated to true in the new state.
     */
    private boolean valid;

    /**
     * Was the first (evaluated) assignment valid in the old focus state, i.e.
     *  - was it present in the old object at all,
     *  - was it active according to the activation and lifecycle state,
     *  - was condition evaluated to true in the old state (FIXME CURRENTLY NOT IMPLEMENTED)
     *
     * TODO verify and fix the algorithms - MID-6404
     */
    private boolean wasValid;

    /**
     * TODO describe MID-6404
     *
     * (used also to force recomputation of parentOrgRefs)
     */
    private boolean forceRecon;

    /**
     * Origin of the primary assignment.
     */
    @NotNull private final AssignmentOrigin origin;

    private final Set<String> adminGuiDependencies = new HashSet<>();

    public EvaluatedAssignmentImpl(
            @NotNull ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi,
            boolean evaluatedOld, @NotNull AssignmentOrigin origin, PrismContext prismContext) {
        this.assignmentIdi = assignmentIdi;
        this.evaluatedOld = evaluatedOld;
        this.constructionTriple = prismContext.deltaFactory().createDeltaSetTriple();
        this.personaConstructionTriple = prismContext.deltaFactory().createDeltaSetTriple();
        this.roles = prismContext.deltaFactory().createDeltaSetTriple();
        this.origin = origin;
    }

    @NotNull
    public ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> getAssignmentIdi() {
        return assignmentIdi;
    }

    @Override
    public AssignmentType getAssignment() {
        return asContainerable(assignmentIdi.getSingleValue(evaluatedOld));
    }

    @Override
    public Long getAssignmentId() {
        Item<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> any = assignmentIdi.getAnyItem();
        return any != null && !any.getValues().isEmpty() ? any.getAnyValue().getId() : null;
    }

    @Override
    public AssignmentType getAssignment(boolean old) {
        return asContainerable(assignmentIdi.getSingleValue(old));
    }

    private ObjectReferenceType getTargetRef() {
        AssignmentType assignment = getAssignment();
        return assignment != null ? assignment.getTargetRef() : null;
    }

    @Override
    public QName getRelation() {
        ObjectReferenceType targetRef = getTargetRef();
        return targetRef != null ? targetRef.getRelation() : null;
    }

    @Override
    public QName getNormalizedRelation() {
        ObjectReferenceType targetRef = getTargetRef();
        return targetRef != null ? SchemaService.get().normalizeRelation(targetRef.getRelation()) : null;
    }

    @NotNull
    public DeltaSetTriple<AssignedResourceObjectConstruction<AH>> getConstructionTriple() {
        return constructionTriple;
    }

    /**
     * Construction is not a part of model-api. To avoid heavy refactoring at present time, there is not a classical
     * Construction-ConstructionImpl separation, but we use artificial (simplified) {@link EvaluatedResourceObjectConstruction}
     * API class instead.
     */
    @Override
    @NotNull
    public DeltaSetTriple<EvaluatedResourceObjectConstruction> getEvaluatedConstructions(@NotNull Task task, @NotNull OperationResult result) {
        DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<AH>> rv =
                PrismContext.get().deltaFactory().createDeltaSetTriple();
        for (AssignedResourceObjectConstruction<AH> construction : constructionTriple.getPlusSet()) {
            for (EvaluatedAssignedResourceObjectConstructionImpl<AH> evaluatedConstruction : construction.getEvaluatedConstructionTriple().getNonNegativeValues()) {
                rv.addToPlusSet(evaluatedConstruction);
            }
        }
        for (AssignedResourceObjectConstruction<AH> construction : constructionTriple.getZeroSet()) {
            rv.merge(construction.getEvaluatedConstructionTriple());
        }
        for (AssignedResourceObjectConstruction<AH> construction : constructionTriple.getMinusSet()) {
            for (EvaluatedAssignedResourceObjectConstructionImpl<AH> evaluatedConstruction : construction.getEvaluatedConstructionTriple().getNonPositiveValues()) {
                rv.addToPlusSet(evaluatedConstruction);
            }
        }
        //noinspection unchecked,rawtypes
        return (DeltaSetTriple)rv;
    }

    @VisibleForTesting
    public Collection<AssignedResourceObjectConstruction<AH>> getConstructionSet(PlusMinusZero whichSet) {
        switch (whichSet) {
            case ZERO: return getConstructionTriple().getZeroSet();
            case PLUS: return getConstructionTriple().getPlusSet();
            case MINUS: return getConstructionTriple().getMinusSet();
            default: throw new IllegalArgumentException("whichSet: " + whichSet);
        }
    }

    void addConstruction(AssignedResourceObjectConstruction<AH> construction, PlusMinusZero whichSet) {
        addToTriple(construction, constructionTriple, whichSet);
    }

    private <T> void addToTriple(T construction, @NotNull DeltaSetTriple<T> triple, PlusMinusZero whichSet) {
        switch (whichSet) {
            case ZERO:
                triple.addToZeroSet(construction);
                break;
            case PLUS:
                triple.addToPlusSet(construction);
                break;
            case MINUS:
                triple.addToMinusSet(construction);
                break;
            default:
                throw new IllegalArgumentException("whichSet: " + whichSet);
        }
    }

    @NotNull
    public DeltaSetTriple<PersonaConstruction<AH>> getPersonaConstructionTriple() {
        return personaConstructionTriple;
    }

    void addPersonaConstruction(PersonaConstruction<AH> personaConstruction, PlusMinusZero whichSet) {
        addToTriple(personaConstruction, personaConstructionTriple, whichSet);
    }

    @NotNull
    @Override
    public DeltaSetTriple<EvaluatedAssignmentTargetImpl> getRoles() {
        return roles;
    }

    void addRole(EvaluatedAssignmentTargetImpl role, PlusMinusZero mode) {
        roles.addToSet(mode, role);
    }

    @NotNull
    public Collection<PrismReferenceValue> getOrgRefVals() {
        return orgRefVals;
    }

    @NotNull
    public Collection<PrismReferenceValue> getArchetypeRefVals() {
        return archetypeRefVals;
    }

    @NotNull
    public Collection<PrismReferenceValue> getMembershipRefVals() {
        return membershipRefVals;
    }

    @NotNull
    public Collection<PrismReferenceValue> getDelegationRefVals() {
        return delegationRefVals;
    }

    public String getTenantOid() {
        return tenantOid;
    }

    void setTenantOid(String tenantOid) {
        this.tenantOid = tenantOid;
    }

    @NotNull
    @Override
    public Collection<Authorization> getAuthorizations() {
        return authorizations;
    }

    void addAuthorization(Authorization authorization) {
        authorizations.add(authorization);
    }

    @Override
    @NotNull
    public Collection<AdminGuiConfigurationType> getAdminGuiConfigurations() {
        return adminGuiConfigurations;
    }

    void addAdminGuiConfiguration(AdminGuiConfigurationType adminGuiConfiguration) {
        adminGuiConfigurations.add(adminGuiConfiguration);
    }

    @Override
    @NotNull
    public Collection<MappingImpl<?,?>> getFocusMappings() {
        return focusMappings;
    }

    @NotNull
    public Collection<AssignedFocusMappingEvaluationRequest> getFocusMappingEvaluationRequests() {
        return focusMappingEvaluationRequests;
    }

    public void addFocusMapping(MappingImpl<?,?> focusMapping) {
        this.focusMappings.add(focusMapping);
    }

    void addFocusMappingEvaluationRequest(AssignedFocusMappingEvaluationRequest request) {
        this.focusMappingEvaluationRequests.add(request);
    }

    @Override
    public PrismObject<?> getTarget() {
        return target;
    }

    public void setTarget(PrismObject<?> target) {
        this.target = target;
    }

    public boolean isVirtual() {
        return origin.isVirtual();
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean isValid) {
        this.valid = isValid;
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

    /**
     * Evaluates constructions in this assignment.
     *
     * @param focusOdoAbsolute Absolute focus ODO. It must not be relative one, because constructions are applied on resource
     *                         objects. And resource objects' old state is related to focus object old state. (These projections
     *                         are _not_ changed iteratively, perhaps except for wave restarting.)
     */
    public void evaluateConstructions(ObjectDeltaObject<AH> focusOdoAbsolute, Consumer<ResourceType> resourceConsumer,
            Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            SecurityViolationException, ConfigurationException, CommunicationException {
        for (AssignedResourceObjectConstruction<AH> construction : constructionTriple.getAllValues()) {
            construction.setFocusOdoAbsolute(focusOdoAbsolute);
            construction.setWasValid(wasValid);
            construction.evaluate(task, result);
            if (resourceConsumer != null && construction.getResource() != null) {
                resourceConsumer.accept(construction.getResource());
            }
        }
    }

    @NotNull
    public AssignmentOrigin getOrigin() {
        return origin;
    }

    @Override
    public boolean isPresentInCurrentObject() {
        return origin.isCurrent();
    }

    @Override
    public boolean isPresentInOldObject() {
        return origin.isOld();
    }

    @Override
    @NotNull
    public Collection<EvaluatedPolicyRuleImpl> getFocusPolicyRules() {
        return focusPolicyRules;
    }

    void addFocusPolicyRule(EvaluatedPolicyRuleImpl policyRule) {
        focusPolicyRules.add(policyRule);
    }

    @Override
    @NotNull
    public Collection<EvaluatedPolicyRuleImpl> getThisTargetPolicyRules() {
        return thisTargetPolicyRules;
    }

    public void addThisTargetPolicyRule(EvaluatedPolicyRuleImpl policyRule) {
        thisTargetPolicyRules.add(policyRule);
    }

    @Override
    @NotNull
    public Collection<EvaluatedPolicyRuleImpl> getOtherTargetsPolicyRules() {
        return otherTargetsPolicyRules;
    }

    public void addOtherTargetPolicyRule(EvaluatedPolicyRuleImpl policyRule) {
        otherTargetsPolicyRules.add(policyRule);
    }

    @Override
    @NotNull
    public Collection<EvaluatedPolicyRuleImpl> getAllTargetsPolicyRules() {
        return Stream.concat(thisTargetPolicyRules.stream(), otherTargetsPolicyRules.stream()).collect(Collectors.toList());
    }

    @Override
    public int getAllTargetsPolicyRulesCount() {
        return thisTargetPolicyRules.size() + otherTargetsPolicyRules.size();
    }

    @Override
    public void triggerRule(@NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        boolean hasException = processRuleExceptions(this, rule, triggers);

        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
            if (trigger instanceof EvaluatedExclusionTrigger) {
                EvaluatedExclusionTrigger exclTrigger = (EvaluatedExclusionTrigger) trigger;
                if (exclTrigger.getConflictingAssignment() != null) {
                    hasException = hasException ||
                            processRuleExceptions((EvaluatedAssignmentImpl<AH>) exclTrigger.getConflictingAssignment(),
                                    rule, triggers);
                }
            }
        }

        if (!hasException) {
            LensUtil.triggerRule(rule, triggers);
        }
    }

    private boolean processRuleExceptions(EvaluatedAssignmentImpl<AH> evaluatedAssignment, @NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        boolean hasException = false;
        for (PolicyExceptionType policyException: evaluatedAssignment.getAssignment().getPolicyException()) {
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
        DebugUtil.debugDumpWithLabelLn(sb, "valid", valid, indent + 1);
        if (forceRecon) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "forceRecon", true, indent + 1);
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
        if (!focusMappingEvaluationRequests.isEmpty()) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "Focus mappings evaluation requests", indent+1);
            for (AssignedFocusMappingEvaluationRequest request : focusMappingEvaluationRequests) {
                sb.append("\n");
                DebugUtil.indentDebugDump(sb, indent+2);
                sb.append(request.shortDump());
            }
        }
        if (!focusMappings.isEmpty()) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "Focus mappings", indent+1);
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
        DebugUtil.debugDumpWithLabelLn(sb, "origin", origin.toString(), indent+1);
        return sb.toString();
    }

    private String ruleCountInfo(Collection<EvaluatedPolicyRuleImpl> rules) {
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
        return "EvaluatedAssignment(target=" + target
                + "; constr=" + constructionTriple
                + "; org=" + orgRefVals
                + "; autz=" + authorizations
                + "; " + focusMappingEvaluationRequests.size() + " focus mappings eval requests"
                + "; " + focusMappings.size() + " focus mappings"
                + "; " + focusPolicyRules.size() + " rules"
                + "; refs=" + membershipRefVals.size() + "m, " + delegationRefVals.size() + "d, " + archetypeRefVals.size() + "a"
                +")";
    }

    @Override
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

    @Override
    public void shortDump(StringBuilder sb) {
        if (target != null) {
            sb.append(target);
        } else if (!constructionTriple.isEmpty()) {
            sb.append("construction(");
            constructionTriple.shortDump(sb);
            sb.append(")");
        } else if (!personaConstructionTriple.isEmpty()) {
            sb.append("personaConstruction(");
            personaConstructionTriple.shortDump(sb);
            sb.append(")");
        } else {
            sb.append(this);
            return;
        }
        if (!isValid()) {
            sb.append(" invalid ");
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
    @Experimental
    public PlusMinusZero getMode() {
        if (assignmentIdi.getItemNew() == null || assignmentIdi.getItemNew().isEmpty()) {
            return MINUS;
        } else if (origin.isCurrent()) {
            return ZERO;
        } else {
            return PLUS;
        }
    }

    /**
     * Returns absolute mode of this assignment with regard to focus old state.
     */
    @NotNull
    public PlusMinusZero getAbsoluteMode() {
        return origin.getAbsoluteMode();
    }

    @Override
    public boolean isBeingAdded() {
        return origin.isBeingAdded();
    }

    @Override
    public boolean isBeingDeleted() {
        return origin.isBeingDeleted();
    }

    @Override
    public boolean isBeingKept() {
        return origin.isBeingKept();
    }

    @Override
    public Set<String> getAdminGuiDependencies() {
        return adminGuiDependencies;
    }

    public void addAdminGuiDependency(String oid) {
        adminGuiDependencies.add(oid);
    }


}
