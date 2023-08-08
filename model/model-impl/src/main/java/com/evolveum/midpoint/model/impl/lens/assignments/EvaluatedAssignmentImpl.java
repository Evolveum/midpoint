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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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

    private static final Trace LOGGER = TraceManager.getTrace(EvaluatedAssignmentImpl.class);

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

    /** Evaluated focus mappings. (These result from evaluation of {@link #focusMappingEvaluationRequests}.) */
    @NotNull private final Collection<MappingImpl<?,?>> focusMappings = new ArrayList<>();

    @NotNull private final Collection<AdminGuiConfigurationType> adminGuiConfigurations = new ArrayList<>();

    /**
     * Policy rules assigned to the focus/projections directly or indirectly through this assignment.
     * They may or may not be really applicable to the focus/projections. This is not our responsibility.
     * We only check that the rule was induced to the focus object.
     *
     * A typical focus-assigned policy rule is e.g. "forbid focus modifications".
     * Shadow-related rules can be present here as well.
     */
    @NotNull private final Collection<EvaluatedPolicyRuleImpl> objectPolicyRules = new ArrayList<>();

    /**
     * Policy rules assigned to the target of this assignment, directly or indirectly.
     * Typically e.g. "approve the creation or modification of the assignment".
     * They may or may not be really applicable during the current clockwork operation.
     */
    @NotNull private final List<EvaluatedPolicyRuleImpl> targetPolicyRules = new ArrayList<>();

    /**
     * Policy rules from other assignments relevant to this one, typically those with an exclusion constraint.
     * Used to implement one-way-defined exclusion policy rules, see e.g. MID-8269.
     *
     * Some of them may be the same as in {@link #targetPolicyRules} (but wrapped).
     *
     * Any given policy rule is here only once.
     * But beware of non-determinism, see {@link #registerAsForeignRule(EvaluatedPolicyRuleImpl)}.
     *
     * Set up during policy rules evaluation.
     */
    @NotNull private final List<ForeignPolicyRuleImpl> foreignPolicyRules = new ArrayList<>();

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
            boolean evaluatedOld, @NotNull AssignmentOrigin origin) {
        var prismContext = PrismContext.get();
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
        return switch (whichSet) {
            case ZERO -> getConstructionTriple().getZeroSet();
            case PLUS -> getConstructionTriple().getPlusSet();
            case MINUS -> getConstructionTriple().getMinusSet();
        };
    }

    void addConstruction(AssignedResourceObjectConstruction<AH> construction, PlusMinusZero whichSet) {
        addToTriple(construction, constructionTriple, whichSet);
    }

    private <T> void addToTriple(T construction, @NotNull DeltaSetTriple<T> triple, PlusMinusZero whichSet) {
        switch (whichSet) {
            case ZERO -> triple.addToZeroSet(construction);
            case PLUS -> triple.addToPlusSet(construction);
            case MINUS -> triple.addToMinusSet(construction);
            default -> throw new IllegalArgumentException("whichSet: " + whichSet);
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

    @Override
    public @NotNull Collection<Authorization> getAuthorizations() {
        return authorizations;
    }

    void addAuthorization(@NotNull Authorization authorization) {
        if (!authorizations.contains(authorization)) {
            authorizations.add(authorization);
        }
    }

    @Override
    @NotNull
    public Collection<AdminGuiConfigurationType> getAdminGuiConfigurations() {
        return adminGuiConfigurations;
    }

    void addAdminGuiConfiguration(AdminGuiConfigurationType adminGuiConfiguration) {
        if (!adminGuiConfigurations.contains(adminGuiConfiguration)) {
            adminGuiConfigurations.add(adminGuiConfiguration);
        }
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
    public Collection<EvaluatedPolicyRuleImpl> getObjectPolicyRules() {
        return objectPolicyRules;
    }

    void addObjectPolicyRule(EvaluatedPolicyRuleImpl policyRule) {
        objectPolicyRules.add(policyRule);
    }

    @Override
    @NotNull
    public Collection<EvaluatedPolicyRuleImpl> getAllTargetsPolicyRules() {
        return Collections.unmodifiableList(targetPolicyRules);
    }

    @Override
    public @NotNull Collection<EvaluatedPolicyRuleImpl> getThisTargetPolicyRules() {
        return getAllTargetsPolicyRules().stream()
                .filter(r -> r.getTargetType() == EvaluatedPolicyRule.TargetType.DIRECT_ASSIGNMENT_TARGET)
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull Collection<EvaluatedPolicyRuleImpl> getOtherTargetsPolicyRules() {
        return getAllTargetsPolicyRules().stream()
                .filter(r -> r.getTargetType() == EvaluatedPolicyRule.TargetType.INDIRECT_ASSIGNMENT_TARGET)
                .collect(Collectors.toList());
    }

    public void addTargetPolicyRule(EvaluatedPolicyRuleImpl policyRule) {
        targetPolicyRules.add(policyRule);
    }

    @Override
    public int getAllTargetsPolicyRulesCount() {
        return targetPolicyRules.size();
    }

    public @NotNull List<ForeignPolicyRuleImpl> getForeignPolicyRules() {
        return Collections.unmodifiableList(foreignPolicyRules);
    }

    @Override
    public @NotNull Collection<AssociatedPolicyRule> getAllAssociatedPolicyRules() {
        ArrayList<AssociatedPolicyRule> allRules = new ArrayList<>(targetPolicyRules);
        for (AssociatedPolicyRule foreignRule : foreignPolicyRules) {
            if (!AssociatedPolicyRule.contains(allRules, foreignRule)) {
                allRules.add(foreignRule);
            }
        }
        return allRules;
    }

    /**
     * Registers a foreign policy rule.
     *
     * In theory, the result of this operation may be non-deterministic. Let us assume we have the same policy rule
     * with multiple occurrences pointing to this assignment (as foreign rules). Whatever is evaluated first, ends
     * in this collection. This may result in phantom updates if full policy situation recording is enabled. However,
     * that is an experimental feature anyway.
     */
    public void registerAsForeignRule(EvaluatedPolicyRuleImpl rule) {
        if (!AssociatedPolicyRule.contains(foreignPolicyRules, rule)) {
            foreignPolicyRules.add(
                    ForeignPolicyRuleImpl.of(rule, this));
        }
    }

    public boolean hasPolicyRuleException(
            @NotNull EvaluatedPolicyRuleImpl rule, @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {

        if (hasDirectPolicyRuleException(rule, triggers)) {
            return true;
        }

        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
            if (trigger instanceof EvaluatedExclusionTrigger exclusionTrigger) {
                EvaluatedAssignmentImpl<?> conflictingAssignment =
                        (EvaluatedAssignmentImpl<?>) exclusionTrigger.getConflictingAssignment();
                if (conflictingAssignment.hasDirectPolicyRuleException(rule, triggers)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasDirectPolicyRuleException(
            @NotNull EvaluatedPolicyRule rule,
            @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        for (PolicyExceptionType policyException: getAssignment().getPolicyException()) {
            String ruleName = rule.getName();
            if (policyException.getRuleName().equals(ruleName)) {
                LOGGER.debug("Policy rule {} would be triggered, but there is an exception for it. Not triggering.", ruleName);
                LOGGER.trace("Policy rule {} would be triggered, but there is an exception for it:\nTriggers:\n{}\nException:\n{}",
                        ruleName, DebugUtil.debugDumpLazily(triggers, 1), policyException);
                return true;
            }
        }
        return false;
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
        DebugUtil.debugDumpWithLabelLn(
                sb, "objectPolicyRules " + ruleCountInfo(objectPolicyRules), objectPolicyRules, indent+1);
        Collection<EvaluatedPolicyRuleImpl> thisTargetRules = getThisTargetPolicyRules();
        DebugUtil.debugDumpWithLabelLn(
                sb, "thisTargetPolicyRules " + ruleCountInfo(thisTargetRules), thisTargetRules, indent+1);
        Collection<EvaluatedPolicyRuleImpl> otherTargetsRules = getOtherTargetsPolicyRules();
        DebugUtil.debugDumpWithLabelLn(
                sb, "otherTargetsRules " + ruleCountInfo(otherTargetsRules), otherTargetsRules, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "origin", origin.toString(), indent+1);
        return sb.toString();
    }

    private String ruleCountInfo(Collection<? extends AssociatedPolicyRule> rules) {
        return "(" + rules.size() + ", triggered " + AssociatedPolicyRule.getTriggeredRulesCount(rules) + ")";
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
                + "; " + objectPolicyRules.size() + " rules"
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
