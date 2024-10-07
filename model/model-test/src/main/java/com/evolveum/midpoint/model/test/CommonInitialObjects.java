/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test;

import java.io.IOException;
import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestReport;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Definition of commonly used initial objects be used in tests on or above the `model` level.
 *
 * TODO Should this class be limited to marks? Or should it cover more initial objects? Is this a good idea, after all?
 */
@Experimental
public interface CommonInitialObjects {

    String INITIAL_OBJECTS = "initial-objects";

    String MARKS = INITIAL_OBJECTS + "/mark";
    String REPORTS = INITIAL_OBJECTS + "/report";
    String OBJECT_COLLECTION = INITIAL_OBJECTS + "/object-collection";
    String POLICIES = INITIAL_OBJECTS + "/policy";

    String ARCHETYPES = INITIAL_OBJECTS + "/archetype";

    String FUNCTION_LIBRARY = INITIAL_OBJECTS + "/function-library";

    String SERVICES = INITIAL_OBJECTS + "/service";

    TestObject<ArchetypeType> ARCHETYPE_APPLICATION_ROLE = TestObject.classPath(
            ARCHETYPES, "028-archetype-application-role.xml", SystemObjectsType.ARCHETYPE_APPLICATION_ROLE.value());

    TestObject<ArchetypeType> ARCHETYPE_REPORT = TestObject.classPath(
            ARCHETYPES, "059-archetype-report.xml", SystemObjectsType.ARCHETYPE_REPORT.value());

    TestObject<ArchetypeType> ARCHETYPE_COLLECTION_REPORT = TestObject.classPath(
            ARCHETYPES, "061-archetype-report-collection.xml", SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value());

    TestObject<ArchetypeType> ARCHETYPE_EVENT_MARK = TestObject.classPath(
            ARCHETYPES, "700-archetype-event-mark.xml", SystemObjectsType.ARCHETYPE_EVENT_MARK.value());

    TestObject<ArchetypeType> ARCHETYPE_OBJECT_MARK = TestObject.classPath(
            ARCHETYPES, "701-archetype-object-mark.xml", SystemObjectsType.ARCHETYPE_OBJECT_MARK.value());

    TestObject<ArchetypeType> ARCHETYPE_SHADOW_POLICY_MARK = TestObject.classPath(
            ARCHETYPES, "705-archetype-shadow-policy-mark.xml", SystemObjectsType.ARCHETYPE_SHADOW_POLICY_MARK.value());

    TestObject<ArchetypeType> ARCHETYPE_RECONCILIATION_TASK = TestObject.classPath(
            ARCHETYPES, "501-archetype-task-reconciliation.xml",
            SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value());

    TestObject<ArchetypeType> ARCHETYPE_IMPORT_TASK = TestObject.classPath(
            ARCHETYPES, "503-archetype-task-import.xml",
            SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());

    TestObject<ArchetypeType> ARCHETYPE_ITERATIVE_BULK_ACTION_TASK = TestObject.classPath(
            ARCHETYPES, "509-archetype-task-iterative-bulk-action.xml",
            SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value());

    TestObject<ArchetypeType> ARCHETYPE_CLASSIFICATION = TestObject.classPath(
            ARCHETYPES, "062-archetype-classification.xml",
            SystemObjectsType.ARCHETYPE_CLASSIFICATION.value());

    TestObject<ArchetypeType> ARCHETYPE_BUSINESS_ROLE = TestObject.classPath(
            ARCHETYPES, "022-archetype-business-role.xml",
            SystemObjectsType.ARCHETYPE_BUSINESS_ROLE.value());

    //Certification tasks
    TestObject<ArchetypeType> ARCHETYPE_CERTIFICATION_TASK_PARENT = TestObject.classPath(
            ARCHETYPES, "520-archetype-task-certification.xml",
            SystemObjectsType.ARCHETYPE_CERTIFICATION_TASK.value());

    TestObject<ArchetypeType> ARCHETYPE_CERTIFICATION_TASK_OPEN_NEXT_STAGE = TestObject.classPath(
            ARCHETYPES, "534-archetype-task-certification-open-next-stage.xml",
            SystemObjectsType.ARCHETYPE_CERTIFICATION_OPEN_NEXT_STAGE_TASK.value());

    TestObject<ArchetypeType> ARCHETYPE_CERTIFICATION_TASK_REMEDIATION = TestObject.classPath(
            ARCHETYPES, "535-archetype-task-certification-remediation.xml",
            SystemObjectsType.ARCHETYPE_CERTIFICATION_REMEDIATION_TASK.value());

    TestObject<ArchetypeType> ARCHETYPE_CERTIFICATION_TASK_START_CAMPAIGN = TestObject.classPath(
            ARCHETYPES, "536-archetype-task-certification-start-campaign.xml",
            SystemObjectsType.ARCHETYPE_CERTIFICATION_START_CAMPAIGN_TASK.value());

    TestObject<ArchetypeType> ARCHETYPE_CERTIFICATION_TASK_CLOSE_CURRENT_STAGE = TestObject.classPath(
            ARCHETYPES, "537-archetype-task-certification-close-current-stage.xml",
            SystemObjectsType.ARCHETYPE_CERTIFICATION_START_CAMPAIGN_TASK.value());

    TestObject<ArchetypeType> ARCHETYPE_CERTIFICATION_TASK_REITERATE_CAMPAIGN = TestObject.classPath(
            ARCHETYPES, "538-archetype-task-certification-reiterate-campaign.xml",
            SystemObjectsType.ARCHETYPE_CERTIFICATION_REITERATE_CAMPAIGN_TASK.value());

    TestObject<MarkType> MARK_FOCUS_ACTIVATED = TestObject.classPath(
            MARKS, "710-mark-focus-activated.xml", SystemObjectsType.MARK_FOCUS_ACTIVATED.value());

    TestObject<MarkType> MARK_FOCUS_DEACTIVATED = TestObject.classPath(
            MARKS, "711-mark-focus-deactivated.xml", SystemObjectsType.MARK_FOCUS_DEACTIVATED.value());

    TestObject<MarkType> MARK_FOCUS_RENAMED = TestObject.classPath(
            MARKS, "712-mark-focus-renamed.xml", SystemObjectsType.MARK_FOCUS_RENAMED.value());

    TestObject<MarkType> MARK_FOCUS_ASSIGNMENT_CHANGED = TestObject.classPath(
            MARKS, "713-mark-focus-assignment-changed.xml", SystemObjectsType.MARK_FOCUS_ASSIGNMENT_CHANGED.value());

    TestObject<MarkType> MARK_FOCUS_ARCHETYPE_CHANGED = TestObject.classPath(
            MARKS, "714-mark-focus-archetype-changed.xml", SystemObjectsType.MARK_FOCUS_ARCHETYPE_CHANGED.value());

    TestObject<MarkType> MARK_FOCUS_PARENT_ORG_REFERENCE_CHANGED = TestObject.classPath(
            MARKS, "715-mark-focus-parent-org-reference-changed.xml",
            SystemObjectsType.MARK_FOCUS_PARENT_ORG_REFERENCE_CHANGED.value());

    TestObject<MarkType> MARK_FOCUS_ROLE_MEMBERSHIP_CHANGED = TestObject.classPath(
            MARKS, "716-mark-focus-role-membership-changed.xml",
            SystemObjectsType.MARK_FOCUS_ROLE_MEMBERSHIP_CHANGED.value());

    TestObject<MarkType> MARK_PROJECTION_ACTIVATED = TestObject.classPath(
            MARKS, "730-mark-projection-activated.xml", SystemObjectsType.MARK_PROJECTION_ACTIVATED.value());

    TestObject<MarkType> MARK_PROJECTION_DEACTIVATED = TestObject.classPath(
            MARKS, "731-mark-projection-deactivated.xml", SystemObjectsType.MARK_PROJECTION_DEACTIVATED.value());

    TestObject<MarkType> MARK_PROJECTION_RENAMED = TestObject.classPath(
            MARKS, "732-mark-projection-renamed.xml", SystemObjectsType.MARK_PROJECTION_RENAMED.value());

    TestObject<MarkType> MARK_PROJECTION_IDENTIFIER_CHANGED = TestObject.classPath(
            MARKS, "733-mark-projection-identifier-changed.xml",
            SystemObjectsType.MARK_PROJECTION_IDENTIFIER_CHANGED.value());

    TestObject<MarkType> MARK_PROJECTION_ENTITLEMENT_CHANGED = TestObject.classPath(
            MARKS, "734-mark-projection-entitlement-changed.xml",
            SystemObjectsType.MARK_PROJECTION_ENTITLEMENT_CHANGED.value());

    TestObject<MarkType> MARK_PROJECTION_PASSWORD_CHANGED = TestObject.classPath(
            MARKS, "735-mark-projection-password-changed.xml",
            SystemObjectsType.MARK_PROJECTION_PASSWORD_CHANGED.value());

    TestObject<MarkType> MARK_SHADOW_CLASSIFICATION_CHANGED = TestObject.classPath(
            MARKS, "736-mark-shadow-classification-changed.xml",
            SystemObjectsType.MARK_SHADOW_CLASSIFICATION_CHANGED.value());

    TestObject<MarkType> MARK_SHADOW_CORRELATION_STATE_CHANGED = TestObject.classPath(
            MARKS, "737-mark-shadow-correlation-state-changed.xml",
            SystemObjectsType.MARK_SHADOW_CORRELATION_STATE_CHANGED.value());

    TestObject<MarkType> MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED = TestObject.classPath(
            MARKS, "738-mark-projection-resource-object-affected.xml",
            SystemObjectsType.MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED.value());

    TestObject<MarkType> MARK_PROTECTED = TestObject.classPath(
            MARKS, "800-mark-protected.xml", SystemObjectsType.MARK_PROTECTED.value());

    TestObject<MarkType> MARK_DECOMMISSION_LATER = TestObject.classPath(
            MARKS, "801-mark-decommission-later.xml", SystemObjectsType.MARK_DECOMMISSION_LATER.value());

    TestObject<MarkType> MARK_CORRELATE_LATER = TestObject.classPath(
            MARKS, "802-mark-correlate-later.xml", SystemObjectsType.MARK_CORRELATE_LATER.value());

    TestObject<MarkType> MARK_DO_NOT_TOUCH = TestObject.classPath(
            MARKS, "803-mark-do-not-touch.xml", SystemObjectsType.MARK_DO_NOT_TOUCH.value());

    TestObject<MarkType> MARK_INVALID_DATA = TestObject.classPath(
            MARKS, "804-mark-invalid-data.xml", SystemObjectsType.MARK_INVALID_DATA.value());

    TestObject<MarkType> MARK_UNMANAGED = TestObject.classPath(
            MARKS, "805-mark-unmanaged.xml", SystemObjectsType.MARK_UNMANAGED.value());

    TestObject<MarkType> MARK_MANAGED = TestObject.classPath(
            MARKS, "806-mark-managed.xml", SystemObjectsType.MARK_MANAGED.value());

    TestObject<MarkType> MARK_EXCLUSION_VIOLATION = TestObject.classPath(
            MARKS, "811-exclusion-violation.xml", SystemObjectsType.MARK_EXCLUSION_VIOLATION.value());

    TestObject<MarkType> MARK_REQUIREMENT_VIOLATION = TestObject.classPath(
            MARKS, "812-requirement-violation.xml", SystemObjectsType.MARK_REQUIREMENT_VIOLATION.value());

    TestObject<MarkType> MARK_UNDERASSIGNED = TestObject.classPath(
            MARKS, "813-underassigned.xml", SystemObjectsType.MARK_UNDERASSIGNED.value());

    TestObject<MarkType> MARK_OVERASSIGNED = TestObject.classPath(
            MARKS, "814-overassigned.xml", SystemObjectsType.MARK_OVERASSIGNED.value());

    TestObject<MarkType> MARK_OBJECT_MODIFIED = TestObject.classPath(
            MARKS, "815-object-modified.xml", SystemObjectsType.MARK_OBJECT_MODIFIED.value());

    TestObject<MarkType> MARK_ASSIGNMENT_MODIFIED = TestObject.classPath(
            MARKS, "816-assignment-modified.xml", SystemObjectsType.MARK_ASSIGNMENT_MODIFIED.value());

    TestObject<MarkType> MARK_HAS_ASSIGNMENT = TestObject.classPath(
            MARKS, "817-has-assignment.xml", SystemObjectsType.MARK_HAS_ASSIGNMENT.value());

    TestObject<MarkType> MARK_HAS_NO_ASSIGNMENT = TestObject.classPath(
            MARKS, "818-has-no-assignment.xml", SystemObjectsType.MARK_HAS_NO_ASSIGNMENT.value());

    TestObject<MarkType> MARK_OBJECT_STATE = TestObject.classPath(
            MARKS, "819-object-state.xml", SystemObjectsType.MARK_OBJECT_STATE.value());

    TestObject<MarkType> MARK_ASSIGNMENT_STATE = TestObject.classPath(
            MARKS, "820-assignment-state.xml", SystemObjectsType.MARK_ASSIGNMENT_STATE.value());

    TestObject<MarkType> MARK_OBJECT_TIME_VALIDITY = TestObject.classPath(
            MARKS, "821-object-time-validity.xml", SystemObjectsType.MARK_OBJECT_TIME_VALIDITY.value());

    TestObject<MarkType> MARK_ASSIGNMENT_TIME_VALIDITY = TestObject.classPath(
            MARKS, "822-assignment-time-validity.xml", SystemObjectsType.MARK_ASSIGNMENT_TIME_VALIDITY.value());

    TestObject<MarkType> MARK_SUSPICIOUS = TestObject.classPath(
            MARKS, "830-suspicious.xml", SystemObjectsType.MARK_SUSPICIOUS.value());

//    TestObject<MarkType> MARK_UNDERSTAFFED_SECURITY = TestObject.classPath(
//            MARKS, "831-understaffed-security.xml", SystemObjectsType.MARK_UNDERSTAFFED_SECURITY.value());
//
//    TestObject<MarkType> MARK_ORPHANED = TestObject.classPath(
//            MARKS, "832-orphaned.xml", SystemObjectsType.MARK_ORPHANED.value());
//
//    TestObject<MarkType> MARK_NEGLECTED = TestObject.classPath(
//            MARKS, "833-neglected.xml", SystemObjectsType.MARK_NEGLECTED.value());
//
//    TestObject<PolicyType> POLICY_INFORMATION_SECURITY_RESPONSIBILITY = TestObject.classPath(
//            POLICIES, "333-classification-information-security-responsibility.xml", SystemObjectsType.CLASSIFICATION_INFORMATION_SECURITY_RESPONSIBILITY.value());


    String PARAM_SIMULATION_RESULT_REF = "simulationResultRef";
    String PARAM_PATHS_TO_INCLUDE = "pathsToInclude";
    String PARAM_PATHS_TO_EXCLUDE = "pathsToExclude";
    String PARAM_INCLUDE_OPERATIONAL_ITEMS = "includeOperationalItems";
    String PARAM_SHOW_IF_NO_DETAILS = "showIfNoDetails";

    TestReport REPORT_SIMULATION_OBJECTS = TestReport.classPath(
            REPORTS,
            "170-report-simulation-objects.xml",
            "00000000-0000-0000-0000-286d76cea7c5",
            List.of(PARAM_SIMULATION_RESULT_REF));

    TestReport REPORT_SIMULATION_OBJECTS_WITH_METRICS = TestReport.classPath(
            REPORTS,
            "171-report-simulation-objects-with-metrics.xml",
            "00000000-0000-0000-0000-616a5c5dbca8",
            List.of(PARAM_SIMULATION_RESULT_REF));

    TestReport REPORT_SIMULATION_ITEMS_CHANGED = TestReport.classPath(
            REPORTS,
            "172-report-simulation-items-changed.xml",
            "00000000-0000-0000-0000-ea32deff43df",
            List.of(PARAM_SIMULATION_RESULT_REF));

    TestReport REPORT_SIMULATION_VALUES_CHANGED = TestReport.classPath(
            REPORTS,
            "173-report-simulation-values-changed.xml",
            "00000000-0000-0000-0000-61bc8211947c",
            List.of(PARAM_SIMULATION_RESULT_REF));

    TestReport REPORT_SIMULATION_RESULTS = TestReport.classPath(
            REPORTS,
            "180-report-simulation-results.xml",
            "00000000-0000-0000-0000-97631b84fde7");

    TestObject<ObjectCollectionType> OBJECT_COLLECTION_CERTIFICATION_CAMPAIGNS_ALL = TestObject.classPath(
            OBJECT_COLLECTION,
            "280-object-collection-certification-campaign-all.xml",
            "00000000-0000-0000-0001-000000000280");

    TestReport REPORT_CERTIFICATION_DEFINITIONS = TestReport.classPath(
            REPORTS,
            "130-report-certification-definitions.xml",
            "00000000-0000-0000-0000-000000000130");

    TestReport REPORT_CERTIFICATION_CAMPAIGNS = TestReport.classPath(
            REPORTS,
            "140-report-certification-campaigns.xml",
            "00000000-0000-0000-0000-000000000140");

    TestReport REPORT_CERTIFICATION_CASES = TestReport.classPath(
            REPORTS,
            "150-report-certification-cases.xml",
            "00000000-0000-0000-0000-000000000150");

    TestReport REPORT_CERTIFICATION_WORK_ITEMS = TestReport.classPath(
            REPORTS,
            "160-report-certification-work-items.xml",
            "00000000-0000-0000-0000-000000000160");

    TestObject<ServiceType> SERVICE_ORIGIN_INTERNAL = TestObject.classPath(
            SERVICES,
            "600-origin-internal.xml",
            "00000000-0000-0000-0000-000000000600"
    );

    static void addCertificationTasks(AbstractModelIntegrationTest test, Task task, OperationResult result) throws CommonException, IOException{
        try {
            test.initTestObjects(
                    task, result,
                    ARCHETYPE_CERTIFICATION_TASK_PARENT,
                    ARCHETYPE_CERTIFICATION_TASK_OPEN_NEXT_STAGE,
                    ARCHETYPE_CERTIFICATION_TASK_REMEDIATION,
                    ARCHETYPE_CERTIFICATION_TASK_START_CAMPAIGN,
                    ARCHETYPE_CERTIFICATION_TASK_CLOSE_CURRENT_STAGE,
                    ARCHETYPE_CERTIFICATION_TASK_REITERATE_CAMPAIGN);
        } catch (CommonException | IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw SystemException.unexpected(e);
        }
    }

    /** To be used when needed. */
    static void addMarks(AbstractModelIntegrationTest test, Task task, OperationResult result)
            throws CommonException, IOException {
        if (!test.areMarksSupported()) {
            return;
        }
        try {
            test.initTestObjects(
                    task, result,
                    ARCHETYPE_EVENT_MARK,
                    ARCHETYPE_OBJECT_MARK,
                    ARCHETYPE_SHADOW_POLICY_MARK,
                    MARK_FOCUS_ACTIVATED,
                    MARK_FOCUS_DEACTIVATED,
                    MARK_FOCUS_RENAMED,
                    MARK_FOCUS_ASSIGNMENT_CHANGED,
                    MARK_FOCUS_ARCHETYPE_CHANGED,
                    MARK_FOCUS_PARENT_ORG_REFERENCE_CHANGED,
                    MARK_FOCUS_ROLE_MEMBERSHIP_CHANGED,
                    MARK_PROJECTION_ACTIVATED,
                    MARK_PROJECTION_DEACTIVATED,
                    MARK_PROJECTION_RENAMED,
                    MARK_PROJECTION_IDENTIFIER_CHANGED,
                    MARK_PROJECTION_ENTITLEMENT_CHANGED,
                    MARK_PROJECTION_PASSWORD_CHANGED,
                    MARK_SHADOW_CLASSIFICATION_CHANGED,
                    MARK_SHADOW_CORRELATION_STATE_CHANGED,
                    MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED,
                    MARK_PROTECTED,
                    MARK_DECOMMISSION_LATER,
                    MARK_CORRELATE_LATER,
                    MARK_DO_NOT_TOUCH,
                    MARK_INVALID_DATA,
                    MARK_UNMANAGED,
                    MARK_MANAGED,
                    MARK_EXCLUSION_VIOLATION,
                    MARK_REQUIREMENT_VIOLATION,
                    MARK_UNDERASSIGNED,
                    MARK_OVERASSIGNED,
                    MARK_OBJECT_MODIFIED,
                    MARK_ASSIGNMENT_MODIFIED,
                    MARK_HAS_ASSIGNMENT,
                    MARK_HAS_NO_ASSIGNMENT,
                    MARK_OBJECT_STATE,
                    MARK_ASSIGNMENT_STATE,
                    MARK_OBJECT_TIME_VALIDITY,
                    MARK_ASSIGNMENT_TIME_VALIDITY,
                    MARK_SUSPICIOUS
//                    MARK_UNDERSTAFFED_SECURITY,
//                    MARK_ORPHANED,
//                    MARK_NEGLECTED
                    );
        } catch (CommonException | IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw SystemException.unexpected(e);
        }
    }
}
