/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

/**
 * Names of projector/clockwork components invoked by medic.partialExecute method calls.
 */
@SuppressWarnings("WeakerAccess")
public class Components {

    public static final String LOAD = "load";
    public static final String FOCUS = "focus";
    public static final String INBOUND = "inbound";
    public static final String FOCUS_ACTIVATION = "focusActivation";
    public static final String OBJECT_TEMPLATE_BEFORE_ASSIGNMENTS = "objectTemplateBeforeAssignments";
    public static final String ASSIGNMENTS = "assignments";
    public static final String ASSIGNMENTS_ORG = "assignmentsOrg";
    public static final String ASSIGNMENTS_MEMBERSHIP_AND_DELEGATE = "assignmentsMembershipAndDelegate";
    public static final String ASSIGNMENTS_CONFLICTS = "assignmentsConflicts";
    public static final String FOCUS_LIFECYCLE = "focusLifecycle";
    public static final String OBJECT_TEMPLATE_AFTER_ASSIGNMENTS = "objectTemplateAfterAssignments";
    public static final String FOCUS_CREDENTIALS = "focusCredentials";
    public static final String FOCUS_POLICY_RULES = "focusPolicyRules";
    public static final String POLICY_RULE_COUNTERS = "policyRuleCounters";
    public static final String EXECUTION = "execution";
    public static final String PROJECTION = "projection";
    public static final String PROJECTION_VALUES = "projectionValues";
    public static final String PROJECTION_CREDENTIALS = "projectionCredentials";
    public static final String PROJECTION_RECONCILIATION = "projectionReconciliation";
    public static final String PROJECTION_VALUES_POST_RECON = "projectionValuesPostRecon";
    public static final String PROJECTION_LIFECYCLE = "projectionLifecycle";
    public static final String PROJECTION_POLICY_RULES = "projectionPolicyRules";
    public static final String OBJECT_TEMPLATE_AFTER_PROJECTIONS = "objectTemplateAfterProjections";
}
