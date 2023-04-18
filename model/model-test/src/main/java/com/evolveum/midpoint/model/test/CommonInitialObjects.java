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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

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

    String ARCHETYPES = INITIAL_OBJECTS + "/archetype";

    String FUNCTION_LIBRARY = INITIAL_OBJECTS + "/function-library";

    TestObject<ArchetypeType> ARCHETYPE_REPORT = TestObject.classPath(
            ARCHETYPES, "059-archetype-report.xml", SystemObjectsType.ARCHETYPE_REPORT.value());

    TestObject<ArchetypeType> ARCHETYPE_COLLECTION_REPORT = TestObject.classPath(
            ARCHETYPES, "061-archetype-report-collection.xml", SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value());

    TestObject<ArchetypeType> ARCHETYPE_EVENT_MARK = TestObject.classPath(
            ARCHETYPES, "700-archetype-event-mark.xml", SystemObjectsType.ARCHETYPE_EVENT_MARK.value());

    TestObject<ArchetypeType> ARCHETYPE_OBJECT_MARK = TestObject.classPath(
            ARCHETYPES, "701-archetype-object-mark.xml", SystemObjectsType.ARCHETYPE_OBJECT_MARK.value());

    TestObject<ArchetypeType> ARCHETYPE_RECONCILIATION_TASK = TestObject.classPath(
            ARCHETYPES, "501-archetype-task-reconciliation.xml",
            SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value());

    TestObject<ArchetypeType> ARCHETYPE_IMPORT_TASK = TestObject.classPath(
            ARCHETYPES, "503-archetype-task-import.xml",
            SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());

    TestObject<ArchetypeType> ARCHETYPE_ITERATIVE_BULK_ACTION_TASK = TestObject.classPath(
            ARCHETYPES, "509-archetype-task-iterative-bulk-action.xml",
            SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value());

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
                    MARK_INVALID_DATA);
        } catch (CommonException | IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw SystemException.unexpected(e);
        }
    }
}
