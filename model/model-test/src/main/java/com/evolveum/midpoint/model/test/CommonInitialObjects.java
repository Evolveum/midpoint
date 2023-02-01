/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test;

import java.io.IOException;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractTestResource;
import com.evolveum.midpoint.test.ClassPathTestResource;
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

    String ARCHETYPES = INITIAL_OBJECTS + "/archetype";

    String FUNCTION_LIBRARY = INITIAL_OBJECTS + "/function-library";

    AbstractTestResource<ArchetypeType> STANDARD_FUNCTIONS = new ClassPathTestResource<>(
            FUNCTION_LIBRARY, "005-standard-functions.xml",
            SystemObjectsType.STANDARD_FUNCTIONS.value());

    AbstractTestResource<ArchetypeType> ARCHETYPE_EVENT_MARK = new ClassPathTestResource<>(
            ARCHETYPES, "700-archetype-event-mark.xml",
            SystemObjectsType.ARCHETYPE_EVENT_MARK.value());

    AbstractTestResource<ArchetypeType> ARCHETYPE_OBJECT_MARK = new ClassPathTestResource<>(
            ARCHETYPES, "701-archetype-object-mark.xml",
            SystemObjectsType.ARCHETYPE_OBJECT_MARK.value());

    AbstractTestResource<MarkType> MARK_FOCUS_ACTIVATED = new ClassPathTestResource<>(
            MARKS, "710-mark-focus-activated.xml",
            SystemObjectsType.MARK_FOCUS_ACTIVATED.value());

    AbstractTestResource<MarkType> MARK_FOCUS_DEACTIVATED = new ClassPathTestResource<>(
            MARKS, "711-mark-focus-deactivated.xml",
            SystemObjectsType.MARK_FOCUS_DEACTIVATED.value());

    AbstractTestResource<MarkType> MARK_FOCUS_RENAMED = new ClassPathTestResource<>(
            MARKS, "712-mark-focus-renamed.xml",
            SystemObjectsType.MARK_FOCUS_RENAMED.value());

    AbstractTestResource<MarkType> MARK_FOCUS_ASSIGNMENT_CHANGED = new ClassPathTestResource<>(
            MARKS, "713-mark-focus-assignment-changed.xml",
            SystemObjectsType.MARK_FOCUS_ASSIGNMENT_CHANGED.value());

    AbstractTestResource<MarkType> MARK_FOCUS_ARCHETYPE_CHANGED = new ClassPathTestResource<>(
            MARKS, "714-mark-focus-archetype-changed.xml",
            SystemObjectsType.MARK_FOCUS_ARCHETYPE_CHANGED.value());

    AbstractTestResource<MarkType> MARK_FOCUS_PARENT_ORG_REFERENCE_CHANGED = new ClassPathTestResource<>(
            MARKS, "715-mark-focus-parent-org-reference-changed.xml",
            SystemObjectsType.MARK_FOCUS_PARENT_ORG_REFERENCE_CHANGED.value());

    AbstractTestResource<MarkType> MARK_FOCUS_ROLE_MEMBERSHIP_CHANGED = new ClassPathTestResource<>(
            MARKS, "716-mark-focus-role-membership-changed.xml",
            SystemObjectsType.MARK_FOCUS_ROLE_MEMBERSHIP_CHANGED.value());

    AbstractTestResource<MarkType> MARK_PROJECTION_ACTIVATED = new ClassPathTestResource<>(
            MARKS, "730-mark-projection-activated.xml",
            SystemObjectsType.MARK_PROJECTION_ACTIVATED.value());

    AbstractTestResource<MarkType> MARK_PROJECTION_DEACTIVATED = new ClassPathTestResource<>(
            MARKS, "731-mark-projection-deactivated.xml",
            SystemObjectsType.MARK_PROJECTION_DEACTIVATED.value());

    AbstractTestResource<MarkType> MARK_PROJECTION_RENAMED = new ClassPathTestResource<>(
            MARKS, "732-mark-projection-renamed.xml",
            SystemObjectsType.MARK_PROJECTION_RENAMED.value());

    AbstractTestResource<MarkType> MARK_PROJECTION_IDENTIFIER_CHANGED = new ClassPathTestResource<>(
            MARKS, "733-mark-projection-identifier-changed.xml",
            SystemObjectsType.MARK_PROJECTION_IDENTIFIER_CHANGED.value());

    AbstractTestResource<MarkType> MARK_PROJECTION_ENTITLEMENT_CHANGED = new ClassPathTestResource<>(
            MARKS, "734-mark-projection-entitlement-changed.xml",
            SystemObjectsType.MARK_PROJECTION_ENTITLEMENT_CHANGED.value());

    AbstractTestResource<MarkType> MARK_PROJECTION_PASSWORD_CHANGED = new ClassPathTestResource<>(
            MARKS, "735-mark-projection-password-changed.xml",
            SystemObjectsType.MARK_PROJECTION_PASSWORD_CHANGED.value());

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
    }
}
