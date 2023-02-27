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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
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

    String ARCHETYPES = INITIAL_OBJECTS + "/archetype";

    String FUNCTION_LIBRARY = INITIAL_OBJECTS + "/function-library";

    TestObject<ArchetypeType> STANDARD_FUNCTIONS = TestObject.classPath(
            FUNCTION_LIBRARY, "005-standard-functions.xml", SystemObjectsType.STANDARD_FUNCTIONS.value());

    TestObject<ArchetypeType> ARCHETYPE_EVENT_MARK = TestObject.classPath(
            ARCHETYPES, "700-archetype-event-mark.xml", SystemObjectsType.ARCHETYPE_EVENT_MARK.value());

    TestObject<ArchetypeType> ARCHETYPE_OBJECT_MARK = TestObject.classPath(
            ARCHETYPES, "701-archetype-object-mark.xml", SystemObjectsType.ARCHETYPE_OBJECT_MARK.value());

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

    TestObject<MarkType> MARK_PROTECTED_SHADOW = TestObject.classPath(
            MARKS, "800-mark-protected-shadow.xml",
            SystemObjectsType.MARK_PROTECTED.value());

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

    /** To be used when needed. */
    static void addMarks(AbstractModelIntegrationTest test, Task task, OperationResult result)
            throws CommonException, IOException {
        if (!test.areMarksSupported()) {
            return;
        }
        test.addObject(ARCHETYPE_EVENT_MARK, task, result);
        test.addObject(ARCHETYPE_OBJECT_MARK, task, result);
        test.addObject(MARK_FOCUS_ACTIVATED, task, result);
        test.addObject(MARK_FOCUS_DEACTIVATED, task, result);
        test.addObject(MARK_FOCUS_RENAMED, task, result);
        test.addObject(MARK_FOCUS_ASSIGNMENT_CHANGED, task, result);
        test.addObject(MARK_FOCUS_ARCHETYPE_CHANGED, task, result);
        test.addObject(MARK_FOCUS_PARENT_ORG_REFERENCE_CHANGED, task, result);
        test.addObject(MARK_FOCUS_ROLE_MEMBERSHIP_CHANGED, task, result);
        test.addObject(MARK_PROJECTION_ACTIVATED, task, result);
        test.addObject(MARK_PROJECTION_DEACTIVATED, task, result);
        test.addObject(MARK_PROJECTION_RENAMED, task, result);
        test.addObject(MARK_PROJECTION_IDENTIFIER_CHANGED, task, result);
        test.addObject(MARK_PROJECTION_ENTITLEMENT_CHANGED, task, result);
        test.addObject(MARK_PROJECTION_PASSWORD_CHANGED, task, result);
        test.addObject(MARK_SHADOW_CLASSIFICATION_CHANGED, task, result);
        test.addObject(MARK_SHADOW_CORRELATION_STATE_CHANGED, task, result);
        test.addObject(MARK_PROTECTED_SHADOW, task, result);
    }
}
