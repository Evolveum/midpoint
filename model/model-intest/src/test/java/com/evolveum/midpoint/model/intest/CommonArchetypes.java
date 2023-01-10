/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.test.TestResource;
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

    TestResource<ArchetypeType> ARCHETYPE_TASK_ITERATIVE_BULK_ACTION = new TestResource<>(
            COMMON_DIR, "archetype-task-iterative-bulk-action.xml",
            SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value());

    TestResource<ArchetypeType> ARCHETYPE_TASK_SINGLE_BULK_ACTION = new TestResource<>(
            COMMON_DIR, "archetype-task-single-bulk-action.xml",
            SystemObjectsType.ARCHETYPE_SINGLE_BULK_ACTION_TASK.value());
}
