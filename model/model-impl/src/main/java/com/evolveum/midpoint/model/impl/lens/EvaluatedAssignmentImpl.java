/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
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
public class EvaluatedAssignmentImpl<AH extends AssignmentHolderType> implements EvaluatedAssignment<AH>, ShortDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(EvaluatedAssignmentImpl.class);

    @NotNull private final ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi;
    private final boolean evaluatedOld;
    @NotNull private final DeltaSetTriple<Construction<AH>> constructionTriple;
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

    /**
     * These are evaluated focus mappings. Since 4.0.1 the evaluation is carried out not during assignment evaluation
     * but afterwards.
     */
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
    private String tenantOid;

    private PrismObject<?> target;
    private boolean isValid;
    private boolean wasValid;
    private boolean forceRecon;         // used also to force recomputation of parentOrgRefs
    @NotNull private final AssignmentOrigin origin;
    private Collection<String> policySituations = new HashSet<>();

    private PrismContext prismContext;

    public EvaluatedAssignmentImpl(
            @NotNull ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi,
            boolean evaluatedOld, @NotNull AssignmentOrigin origin, PrismContext prismContext) {
        this.assignmentIdi = assignmentIdi;
        this.evaluatedOld = evaluatedOld;
        this.constructionTriple = prismContext.deltaFactory().createDeltaSetTriple();
        this.personaConstructionTriple = prismContext.deltaFactory().createDeltaSetTriple();
        this.roles = prismContext.deltaFactory().createDeltaSetTriple();
        this.prismContext = prismContext;
        this.origin = origin;
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
        return any != null && !any.getValues().isEmpty() ? any.getAnyValue().getId() : null;
    }

    @Override
    public AssignmentType getAssignmentType(boolean old) {
        return asContainerable(assignmentIdi.getSingleValue(old));
    }

    private ObjectReferenceType getTargetRef() {
        AssignmentType assignmentType = getAssignmentType();
        if (assignmentType == null) {
            return null;
        }
        return assignmentType.getTargetRef();
    }

    @Override
    public QName getRelation() {
        ObjectReferenceType targetRef = getTargetRef();
        return targetRef != null ? targetRef.getRelation() : null;
    }

    @Override
    public QName getNormalizedRelation(RelationRegistry relationRegistry) {
        ObjectReferenceType targetRef = getTargetRef();
        return targetRef != null ? relationRegistry.normalizeRelation(targetRef.getRelation()) : null;
    }

    @NotNull
    public DeltaSetTriple<Construction<AH>> getConstructionTriple() {
        return constructionTriple;
    }

    /**
     * Construction is not a part of model-api. To avoid heavy refactoring at present time, there is not a classical
     * Construction-ConstructionImpl separation, but we use artificial (simplified) EvaluatedConstruction
     * API class instead.
     */
    @Override
    public DeltaSetTriple<EvaluatedConstruction> getEvaluatedConstructions(Task task, OperationResult result) {
        DeltaSetTriple<EvaluatedConstruction> rv = prismContext.deltaFactory().createDeltaSetTriple();
        for (PlusMinusZero whichSet : PlusMinusZero.values()) {
            Collection<Construction<AH>> constructionSet = constructionTriple.getSet(whichSet);
            if (constructionSet != null) {
                for (Construction<AH> construction : constructionSet) {
                    if (!construction.isIgnored()) {
                        rv.addToSet(whichSet, new EvaluatedConstructionImpl(construction));
                    }
                }
            }
        }
        return rv;
    }

    Collection<Construction<AH>> getConstructionSet(PlusMinusZero whichSet) {
        switch (whichSet) {
            case ZERO: return getConstructionTriple().getZeroSet();
            case PLUS: return getConstructionTriple().getPlusSet();
            case MINUS: return getConstructionTriple().getMinusSet();
            default: throw new IllegalArgumentException("whichSet: " + whichSet);
        }
    }

    void addConstruction(Construction<AH> construction, PlusMinusZero whichSet) {
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
    DeltaSetTriple<PersonaConstruction<AH>> getPersonaConstructionTriple() {
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

    @NotNull
    public Collection<AdminGuiConfigurationType> getAdminGuiConfigurations() {
        return adminGuiConfigurations;
    }

    void addAdminGuiConfiguration(AdminGuiConfigurationType adminGuiConfiguration) {
        adminGuiConfigurations.add(adminGuiConfiguration);
    }

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

    public boolean isVirtual() {
        return origin.isVirtual();
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

    public void setWasValid(boolean wasValid) {
        this.wasValid = wasValid;
    }

    public boolean isForceRecon() {
        return forceRecon;
    }

    public void setForceRecon(boolean forceRecon) {
        this.forceRecon = forceRecon;
    }

    // System configuration is used only to provide $configuration script variable (MID-2372)
    public void evaluateConstructions(ObjectDeltaObject<AH> focusOdo, PrismObject<SystemConfigurationType> systemConfiguration,
            Consumer<ResourceType> resourceConsumer, Task task, OperationResult result) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException,
            CommunicationException {
        for (Construction<AH> construction : constructionTriple.getAllValues()) {
            construction.setFocusOdo(focusOdo);
            construction.setSystemConfiguration(systemConfiguration);
            construction.setWasValid(wasValid);
            LOGGER.trace("Evaluating construction '{}' in {}", construction, construction.getSource());
            construction.evaluate(task, result);
            if (resourceConsumer != null && construction.getResource() != null) {
                resourceConsumer.accept(construction.getResource());
            }
        }
    }

    void evaluateConstructions(ObjectDeltaObject<AH> focusOdo, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
        evaluateConstructions(focusOdo, null, null, task, result);
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

    @NotNull
    public Collection<EvaluatedPolicyRule> getFocusPolicyRules() {
        return focusPolicyRules;
    }

    void addFocusPolicyRule(EvaluatedPolicyRule policyRule) {
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

    @Override
    public int getAllTargetsPolicyRulesCount() {
        return thisTargetPolicyRules.size() + otherTargetsPolicyRules.size();
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
                            hasException || processRuleExceptions((EvaluatedAssignmentImpl<AH>) exclTrigger.getConflictingAssignment(),
                                    rule, triggers);
                }
            }
        }

        if (!hasException) {
            LensUtil.triggerRule(rule, triggers, policySituations);
        }
    }

    private boolean processRuleExceptions(EvaluatedAssignmentImpl<AH> evaluatedAssignment, @NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
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
        DebugUtil.debugDumpWithLabelLn(sb, "isValid", isValid, indent + 1);
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
        return "EvaluatedAssignment(target=" + target
                + "; constr=" + constructionTriple
                + "; org=" + orgRefVals
                + "; autz=" + authorizations
                + "; " + focusMappingEvaluationRequests.size() + " focus mappings eval requests"
                + "; " + focusMappings.size() + " focus mappings"
                + "; " + focusPolicyRules.size()+" rules)";
    }

    String toHumanReadableString() {
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
            sb.append(toString());
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
    public PlusMinusZero getMode() {
        if (assignmentIdi.getItemNew() == null || assignmentIdi.getItemNew().isEmpty()) {
            return MINUS;
        } else if (origin.isCurrent()) {
            return ZERO;
        } else {
            return PLUS;
        }
    }

}
