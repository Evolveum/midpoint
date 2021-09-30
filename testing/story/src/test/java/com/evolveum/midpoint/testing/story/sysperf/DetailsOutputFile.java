package com.evolveum.midpoint.testing.story.sysperf;

import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.*;

class DetailsOutputFile {

    private final PrintWriter writer;

    DetailsOutputFile() throws IOException {
        writer = new PrintWriter(new FileWriter(getFile()));
    }

    private File getFile() {
        return new File(TARGET_DIR, START + "-" + OTHER_PARAMETERS.label + "-details.txt");
    }

    void logTaskFinish(String testName, TaskType taskAfter) {
        writer.printf("********** %s FINISHED **********\n\n", testName);
        writer.println(StatisticsUtil.format(taskAfter.getOperationStats()));
        writer.println();
        writer.flush();
    }
}
