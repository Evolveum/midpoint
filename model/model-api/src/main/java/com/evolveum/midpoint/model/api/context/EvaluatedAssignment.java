/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import org.jetbrains.annotations.NotNull;

public interface EvaluatedAssignment<AH extends AssignmentHolderType> extends DebugDumpable {

    AssignmentType getAssignmentType();

    Long getAssignmentId();

    Collection<Authorization> getAuthorizations();

    Collection<AdminGuiConfigurationType> getAdminGuiConfigurations();

    DeltaSetTriple<? extends EvaluatedAssignmentTarget> getRoles();

    DeltaSetTriple<EvaluatedConstruction> getEvaluatedConstructions(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

    PrismObject<?> getTarget();

    AssignmentType getAssignmentType(boolean old);

    // return value of null is ambiguous: either targetRef is null or targetRef.relation is null
    QName getRelation();

    QName getNormalizedRelation(RelationRegistry relationRegistry);

    /**
     * TODO Define this concept. It looks like it mixes ideas of validity (activation, lifecycle state)
     *  and relativity mode (condition).
     */
    boolean isValid();

    boolean isPresentInCurrentObject();

    boolean isPresentInOldObject();

    /**
     * Returns all policy rules that apply to the focal object and are derived from this assignment
     * - even those that were not triggered. The policy rules are compiled from all the applicable
     * sources (target, meta-roles, etc.)
     */
    @NotNull
    Collection<EvaluatedPolicyRule> getFocusPolicyRules();

    /**
     * Returns all policy rules that directly apply to the target object of this assignment
     * (and are derived from this assignment) - even those that were not triggered. The policy rules
     * are compiled from all the applicable sources (target, meta-roles, etc.)
     */
    @NotNull
    Collection<EvaluatedPolicyRule> getThisTargetPolicyRules();

    /**
     * Returns all policy rules that apply to some other target object of this assignment
     * (and are derived from this assignment) - even those that were not triggered. The policy rules
     * are compiled from all the applicable sources (target, meta-roles, etc.)
     */
    @NotNull
    Collection<EvaluatedPolicyRule> getOtherTargetsPolicyRules();

    /**
     * Returns all policy rules that apply to any of the target objects provided by this assignment
     * (and are derived from this assignment) - even those that were not triggered. The policy rules
     * are compiled from all the applicable sources (target, meta-roles, etc.)
     *
     * The difference to getThisTargetPolicyRules is that if e.g.
     * jack is a Pirate, and Pirate induces Sailor, then
     *  - getThisTargetPolicyRules will show rules that are attached to Pirate
     *  - getAllTargetsPolicyRules will show rules that are attached to Pirate and Sailor
     *  - getOtherTargetsPolicyRules will show rules that are attached to Sailor
     */
    @NotNull
    Collection<EvaluatedPolicyRule> getAllTargetsPolicyRules();

    /**
     * How many target policy rules are there. This is more efficient than getAllTargetsPolicyRules().size(), as the
     * collection of all targets policy rules is computed on demand.
     */
    int getAllTargetsPolicyRulesCount();

    Collection<String> getPolicySituations();

    void triggerRule(@NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger<?>> triggers);


    /**
     * These are evaluated focus mappings. Since 4.0.1 the evaluation is carried out not during assignment evaluation
     * but afterwards.
     */
    Collection<? extends Mapping<?,?>> getFocusMappings();

}
