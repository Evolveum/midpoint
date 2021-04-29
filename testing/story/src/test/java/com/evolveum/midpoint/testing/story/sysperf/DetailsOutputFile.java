package com.evolveum.midpoint.testing.story.sysperf;

import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.schema.util.task.TaskPerformanceInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.START;
import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TARGET_DIR;

class DetailsOutputFile {

    private static final File FILE = new File(TARGET_DIR, START + "-details.txt");
    private final PrintWriter writer;

    DetailsOutputFile() throws IOException {
        writer = new PrintWriter(new FileWriter(FILE));
    }

    void logTaskFinish(String desc, TaskType taskAfter, TaskPerformanceInformation performanceInformation) {
        writer.printf("********** FINISHED: %s **********\n\n", desc);
        writer.println(TaskOperationStatsUtil.format(taskAfter.getOperationStats()));
        writer.println();
        writer.println(performanceInformation.debugDump());
        writer.println();
        writer.flush();
    }
}
