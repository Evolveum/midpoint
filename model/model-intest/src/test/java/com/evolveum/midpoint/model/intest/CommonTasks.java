/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import static com.evolveum.midpoint.test.AbstractIntegrationTest.COMMON_DIR;

/**
 * Common tasks, useful for empty model integration tests.
 *
 * Only tasks that are present also in standard system operation (or tasks that are strongly related to them)
 * should be defined here.
 */
@Experimental
public interface CommonTasks {

    TestObject<TaskType> TASK_TRIGGER_SCANNER_ON_DEMAND = TestObject.file(
            COMMON_DIR, "task-trigger-scanner-on-demand.xml", "4a574260-ae0c-4e00-8dfc-f833b703e45a");
}
