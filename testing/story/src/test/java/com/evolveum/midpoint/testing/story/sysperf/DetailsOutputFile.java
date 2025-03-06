package com.evolveum.midpoint.testing.story.sysperf;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.evolveum.midpoint.schema.util.task.ActivityPerformanceInformation;
import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;

class DetailsOutputFile {

    private final PrintWriter writer;

    DetailsOutputFile() throws IOException {
        writer = new PrintWriter(new FileWriter(getFile()));
    }

    private File getFile() {
        return new File(TARGET_DIR, START + "-" + OTHER_PARAMETERS.label + "-details.txt");
    }

    void logTaskFinish(
            String desc, TreeNode<ActivityPerformanceInformation> performanceInformation, OperationStatsType operationStats) {
        writer.printf("********** FINISHED: %s **********\n\n", desc);
        writer.println(TaskOperationStatsUtil.format(operationStats));
        writer.println();
        writer.println(performanceInformation.debugDump());
        writer.println();
        writer.flush();
    }
}
