/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;

import javax.xml.namespace.QName;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ComponentConstants {

    public static final String NS_COMPONENTS_PREFIX = SchemaConstants.NS_MIDPOINT_PUBLIC_PREFIX + "gui/component-3";
    public static final String NS_DASHBOARD_WIDGET = NS_COMPONENTS_PREFIX + "/dashboard/widget";

    // UI - Tabs for focus objects
    public static final QName UI_FOCUS_TAB_BASIC = new QName(NS_COMPONENTS_PREFIX, "focusTabBasic");
    public static final String UI_FOCUS_TAB_BASIC_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_BASIC);

    public static final QName UI_FOCUS_TAB_PROJECTIONS = new QName(NS_COMPONENTS_PREFIX, "focusTabProjections");
    public static final String UI_FOCUS_TAB_PROJECTIONS_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_PROJECTIONS);

    public static final QName UI_FOCUS_TAB_PERSONAS = new QName(NS_COMPONENTS_PREFIX, "focusTabPersonas");
    public static final String UI_FOCUS_TAB_PERSONAS_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_PERSONAS);

    public static final QName UI_FOCUS_TAB_ASSIGNMENTS = new QName(NS_COMPONENTS_PREFIX, "focusTabAssignments");
    public static final String UI_FOCUS_TAB_ASSIGNMENTS_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_ASSIGNMENTS);

    public static final QName UI_FOCUS_TAB_POLICY_RULES = new QName(NS_COMPONENTS_PREFIX, "focusTabPolicyRules");
    public static final String UI_FOCUS_TAB_POLICY_RULES_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_POLICY_RULES);

    public static final QName UI_CASE_TAB_WORKITEMS = new QName(NS_COMPONENTS_PREFIX, "caseTabWorkitems");
    public static final String UI_CASE_TAB_WORKITEMS_URL = QNameUtil.qNameToUri(UI_CASE_TAB_WORKITEMS);

    public static final QName UI_CASE_TAB_CHILD_CASES = new QName(NS_COMPONENTS_PREFIX, "caseTabChildCases");
    public static final String UI_CASE_TAB_CHILD_CASES_URL = QNameUtil.qNameToUri(UI_CASE_TAB_CHILD_CASES);

    public static final QName UI_CASE_TAB_APPROVAL = new QName(NS_COMPONENTS_PREFIX, "caseTabApproval");
    public static final String UI_CASE_TAB_APPROVAL_URL = QNameUtil.qNameToUri(UI_CASE_TAB_WORKITEMS);

    public static final QName UI_CASE_TAB_EVENTS = new QName(NS_COMPONENTS_PREFIX, "caseTabEvents");
    public static final String UI_CASE_TAB_EVENTS_URL = QNameUtil.qNameToUri(UI_CASE_TAB_EVENTS);

    public static final QName UI_FOCUS_TAB_APPLICABLE_POLICIES = new QName(NS_COMPONENTS_PREFIX, "focusTabApplicablePolicies");
    public static final String UI_FOCUS_TAB_APPLICABLE_POLICIES_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_APPLICABLE_POLICIES);

    public static final QName UI_FOCUS_TAB_CONSENTS = new QName(NS_COMPONENTS_PREFIX, "focusTabConsents");
    public static final String UI_FOCUS_TAB_CONSENTS_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_CONSENTS);

    public static final QName UI_FOCUS_TAB_TASKS = new QName(NS_COMPONENTS_PREFIX, "focusTabTasks");
    public static final String UI_FOCUS_TAB_TASKS_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_TASKS);

    public static final QName UI_FOCUS_TAB_OBJECT_HISTORY = new QName(NS_COMPONENTS_PREFIX, "focusTabObjectHistory");
    public static final String UI_FOCUS_TAB_OBJECT_HISTORY_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_OBJECT_HISTORY);

    public static final QName UI_FOCUS_TAB_INDUCEMENTS = new QName(NS_COMPONENTS_PREFIX, "focusTabInducements");
    public static final String UI_FOCUS_TAB_INDUCEMENTS_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_INDUCEMENTS);

    public static final QName UI_ROLE_TAB_INDUCED_ENTITLEMENTS = new QName(NS_COMPONENTS_PREFIX, "roleTabInducedEntitlements");
    public static final String UI_ROLE_TAB_INDUCED_ENTITLEMENTS_URL = QNameUtil.qNameToUri(UI_ROLE_TAB_INDUCED_ENTITLEMENTS);

    public static final QName UI_FOCUS_TAB_GOVERNANCE = new QName(NS_COMPONENTS_PREFIX, "focusTabGovernance");
    public static final String UI_FOCUS_TAB_GOVERNANCE_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_GOVERNANCE);

    public static final QName UI_FOCUS_TAB_DELEGATIONS = new QName(NS_COMPONENTS_PREFIX, "focusTabDelegations");
    public static final String UI_FOCUS_TAB_DELEGATIONS_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_DELEGATIONS);

    public static final QName UI_FOCUS_TAB_DELEGATED_TO_ME = new QName(NS_COMPONENTS_PREFIX, "focusTabDelegatedToMe");
    public static final String UI_FOCUS_TAB_DELEGATED_TO_ME_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_DELEGATED_TO_ME);

    public static final QName UI_FOCUS_TAB_POLICY_CONSTRAINTS = new QName(NS_COMPONENTS_PREFIX, "focusTabPolicyConstraints");
    public static final String UI_FOCUS_TAB_POLICY_CONSTRAINTS_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_POLICY_CONSTRAINTS);

    public static final QName UI_FOCUS_TAB_MEMBERS = new QName(NS_COMPONENTS_PREFIX, "focusTabMembers");
    public static final String UI_FOCUS_TAB_MEMBERS_URL = QNameUtil.qNameToUri(UI_FOCUS_TAB_MEMBERS);

    public static final QName UI_ARCHETYPE_TAB_ARCHETYPE_POLICY = new QName(NS_COMPONENTS_PREFIX, "archetypeTabArchetypePolicy");
    public static final String UI_ARCHTYPE_TAB_ARCHETYPE_POLICY_URL = QNameUtil.qNameToUri(UI_ARCHETYPE_TAB_ARCHETYPE_POLICY);

    public static final QName UI_CASE_TAB_OVERVIEW_MANUAL = new QName(NS_COMPONENTS_PREFIX, "caseTabOverviewManual");
    public static final String UI_CASE_TAB_OVERVIEW_MANUAL_URL = QNameUtil.qNameToUri(UI_CASE_TAB_OVERVIEW_MANUAL);

    public static final QName UI_CASE_TAB_OVERVIEW_REQUEST = new QName(NS_COMPONENTS_PREFIX, "caseTabOverviewRequest");
    public static final String UI_CASE_TAB_OVERVIEW_REQUEST_URL = QNameUtil.qNameToUri(UI_CASE_TAB_OVERVIEW_REQUEST);

    public static final QName UI_CASE_TAB_OVERVIEW_APPROVAL = new QName(NS_COMPONENTS_PREFIX, "caseTabOverviewApproval");
    public static final String UI_CASE_TAB_OVERVIEW_APPROVAL_URL = QNameUtil.qNameToUri(UI_CASE_TAB_OVERVIEW_APPROVAL);

    public static final QName UI_TASK_TAB_BASIC = new QName(NS_COMPONENTS_PREFIX, "taskTabBasic");
    public static final String UI_TASK_TAB_BASIC_URL = QNameUtil.qNameToUri(UI_TASK_TAB_BASIC);

    public static final QName UI_TASK_TAB_SCHEDULE = new QName(NS_COMPONENTS_PREFIX, "taskTabSchedule");
    public static final String UI_TASK_TAB_SCHEDULE_URL = QNameUtil.qNameToUri(UI_TASK_TAB_SCHEDULE);

    public static final QName UI_TASK_TAB_WORK_MANAGEMENT = new QName(NS_COMPONENTS_PREFIX, "taskTabWorkManagement");
    public static final String UI_TASK_TAB_WORK_MANAGEMENT_URL = QNameUtil.qNameToUri(UI_TASK_TAB_WORK_MANAGEMENT);

    public static final QName UI_TASK_TAB_CLEANUP_POLICIES = new QName(NS_COMPONENTS_PREFIX, "taskTabCleanupPolicies");
    public static final String UI_TASK_TAB_CLEANUP_POLICIES_URL = QNameUtil.qNameToUri(UI_TASK_TAB_CLEANUP_POLICIES);

    public static final QName UI_TASK_TAB_SUBTASKS = new QName(NS_COMPONENTS_PREFIX, "taskTabSubtasks");
    public static final String UI_TASK_TAB_SUBTASKS_URL = QNameUtil.qNameToUri(UI_TASK_TAB_SUBTASKS);

    public static final QName UI_TASK_TAB_OPERATION_STATISTICS = new QName(NS_COMPONENTS_PREFIX, "taskTabOperationStatistics");
    public static final String UI_TASK_TAB_OPERATION_STATISTICS_URL = QNameUtil.qNameToUri(UI_TASK_TAB_OPERATION_STATISTICS);

    public static final QName UI_TASK_TAB_ENVIRONMENTAL_PERFORMANCE = new QName(NS_COMPONENTS_PREFIX, "taskTabEnvironmentalPerformance");
    public static final String UI_TASK_TAB_ENVIRONMENTAL_PERFORMANCE_URL = QNameUtil.qNameToUri(UI_TASK_TAB_ENVIRONMENTAL_PERFORMANCE);

    public static final QName UI_TASK_TAB_OPERATION = new QName(NS_COMPONENTS_PREFIX, "taskTabOperation");
    public static final String UI_TASK_TAB_OPERATION_URL = QNameUtil.qNameToUri(UI_TASK_TAB_OPERATION);

    public static final QName UI_TASK_TAB_INTERNAL_PERFORMANCE = new QName(NS_COMPONENTS_PREFIX, "taskTabInternalPerformance");
    public static final String UI_TASK_TAB_INTERNAL_PERFORMANCE_URL = QNameUtil.qNameToUri(UI_TASK_TAB_INTERNAL_PERFORMANCE);

    public static final QName UI_TASK_TAB_RESULT = new QName(NS_COMPONENTS_PREFIX, "taskTabResult");
    public static final String UI_TASK_TAB_RESULT_URL = QNameUtil.qNameToUri(UI_TASK_TAB_RESULT);

    public static final QName UI_TASK_TAB_ERRORS = new QName(NS_COMPONENTS_PREFIX, "taskTabErrors");
    public static final String UI_TASK_TAB_ERRORS_URL = QNameUtil.qNameToUri(UI_TASK_TAB_ERRORS);

}
