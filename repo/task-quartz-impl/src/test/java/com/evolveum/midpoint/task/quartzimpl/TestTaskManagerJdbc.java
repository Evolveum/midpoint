/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.quartzimpl;

import org.springframework.test.context.TestPropertySource;

/**
 * Test class that uses a real database store for Quartz jobs
 */
@TestPropertySource(properties = {
        "midpoint.taskManager.clustered=true",
        "midpoint.nodeId=node1",
        "midpoint.taskManager.jdbcJobStore=true"
})
public class TestTaskManagerJdbc extends TestTaskManagerBasic {

}
