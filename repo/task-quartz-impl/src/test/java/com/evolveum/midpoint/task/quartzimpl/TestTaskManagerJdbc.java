package com.evolveum.midpoint.task.quartzimpl;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "midpoint.taskManager.clustered=true",
        "midpoint.nodeId=node1",
        "midpoint.taskManager.jdbcJobStore=true"
})
public class TestTaskManagerJdbc extends TestTaskManagerBasic {

}
