package com.evolveum.midpoint.testing.story.sysperf;

import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.task.api.Task;
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

    void logTestFinish(String testName, TaskType taskAfter) {
        writer.printf("********** %s FINISHED **********\n\n", testName);
        writer.println(StatisticsUtil.format(taskAfter.getOperationStats()));
        writer.println();
        writer.flush();
    }
}
