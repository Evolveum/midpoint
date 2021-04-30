package com.evolveum.midpoint.testing.story.sysperf;

public class OtherParameters {

    private static final String PROP_TASK_TIMEOUT = "taskTimeout";

    final int taskTimeout;

    private OtherParameters(int taskTimeout) {
        this.taskTimeout = taskTimeout;
    }

    static OtherParameters setup() {
        return new OtherParameters(
                Integer.parseInt(System.getProperty(PROP_TASK_TIMEOUT, "1800000")) // 30 minutes
        );
    }
}
