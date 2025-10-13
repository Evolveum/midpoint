/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import static com.evolveum.midpoint.test.AbstractIntegrationTest.COMMON_DIR;

/**
 * Definition of common archetypes to be used in model integration tests.
 *
 * Only archetypes that are present also in standard system operation should be defined here.
 */
@Experimental
public interface CommonArchetypes {

    TestObject<ArchetypeType> ARCHETYPE_TASK_ITERATIVE_BULK_ACTION = TestObject.file(
            COMMON_DIR, "archetype-task-iterative-bulk-action.xml",
            SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value());

    TestObject<ArchetypeType> ARCHETYPE_TASK_SINGLE_BULK_ACTION = TestObject.file(
            COMMON_DIR, "archetype-task-single-bulk-action.xml",
            SystemObjectsType.ARCHETYPE_SINGLE_BULK_ACTION_TASK.value());
}
