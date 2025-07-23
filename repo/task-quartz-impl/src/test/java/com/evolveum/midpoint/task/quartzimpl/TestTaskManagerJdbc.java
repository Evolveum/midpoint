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
