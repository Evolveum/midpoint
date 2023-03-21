/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

public interface EvaluatedAssignment extends ShortDumpable, DebugDumpable, Serializable {

    AssignmentType getAssignment();

    AssignmentType getAssignment(boolean old);

    @Deprecated // use getAssignment
    default AssignmentType getAssignmentType() {
        return getAssignment();
    }

    @Deprecated // use getAssignment
    default AssignmentType getAssignmentType(boolean old) {
        return getAssignment(old);
    }

    Long getAssignmentId();

    Collection<Authorization> getAuthorizations();

    Collection<AdminGuiConfigurationType> getAdminGuiConfigurations();

    DeltaSetTriple<? extends EvaluatedAssignmentTarget> getRoles();

    @NotNull DeltaSetTriple<EvaluatedResourceObjectConstruction> getEvaluatedConstructions(@NotNull Task task, @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException;

    PrismObject<?> getTarget();

    // return value of null is ambiguous: either targetRef is null or targetRef.relation is null
    QName getRelation();

    QName getNormalizedRelation();

    /**
     * TODO Define this concept. It looks like it mixes ideas of validity (activation, lifecycle state)
     *  and relativity mode (condition).
     */
    boolean isValid();

    boolean isPresentInCurrentObject();

    boolean isPresentInOldObject();

    /**
     * Returns all policy rules that apply to the focal/projections objects and are derived from this assignment
     * - even those that were not triggered. The policy rules are compiled from all the applicable
     * sources (target, meta-roles, etc.)
     */
    @NotNull Collection<? extends EvaluatedPolicyRule> getObjectPolicyRules();

    /**
     * Returns all policy rules that directly apply to the target object of this assignment
     * (and are derived from this assignment) - even those that were not triggered. The policy rules
     * are compiled from all the applicable sources (target, meta-roles, etc.)
     */
    @NotNull Collection<? extends EvaluatedPolicyRule> getThisTargetPolicyRules();

    /**
     * Returns all policy rules that apply to some other target object of this assignment
     * (and are derived from this assignment) - even those that were not triggered. The policy rules
     * are compiled from all the applicable sources (target, meta-roles, etc.)
     */
    @NotNull Collection<? extends EvaluatedPolicyRule> getOtherTargetsPolicyRules();

    /**
     * Returns all policy rules that apply to any of the target objects provided by this assignment
     * (and are derived from this assignment) - even those that were not triggered. The policy rules
     * are compiled from all the applicable sources (target, meta-roles, etc.)
     *
     * The difference to getThisTargetPolicyRules is that if e.g.
     * jack is a Pirate, and Pirate induces Sailor, then
     *
     * - `getThisTargetPolicyRules` will show rules that are attached to Pirate
     * - `getAllTargetsPolicyRules` will show rules that are attached to Pirate and Sailor
     * - `getOtherTargetsPolicyRules` will show rules that are attached to Sailor
     */
    @NotNull
    Collection<? extends EvaluatedPolicyRule> getAllTargetsPolicyRules();

    /**
     * Returns {@link #getAllTargetsPolicyRules()} plus so-called "foreign policy rules". Those are rules that are related
     * to this assignment because they contain an exclusion trigger pointing to this assignment as the conflicting one.
     * This is necessary to implement "declare once, use twice" approach where it should be sufficient to declare an exclusion
     * constraint at one of the targets only. See e.g. MID-8269.
     *
     * There are important things to be aware of, though. Please see {@link AssociatedPolicyRule} for more information.
     */
    @NotNull Collection<AssociatedPolicyRule> getAllAssociatedPolicyRules();

    /**
     * How many target policy rules are there.
     */
    int getAllTargetsPolicyRulesCount();

    /**
     * These are evaluated focus mappings. Since 4.0.1 the evaluation is carried out not during assignment evaluation
     * but afterwards.
     */
    Collection<? extends Mapping<?, ?>> getFocusMappings();

    String toHumanReadableString();

    /**
     * Assignment is either being added in the current wave or was added in some of the previous waves.
     */
    boolean isBeingAdded();

    /**
     * Assignment is either being deleted in the current wave or was deleted in some of the previous waves.
     */
    boolean isBeingDeleted();

    /**
     * Assignment was present at the beginning and is not being deleted.
     */
    boolean isBeingKept();

    /**
     * Set of abstract role OIDs considered for addition of admin gui configuration.
     */
    Set<String> getAdminGuiDependencies();
}
