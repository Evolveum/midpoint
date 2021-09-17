package com.evolveum.midpoint.testing.story.sysperf;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.evolveum.midpoint.task.api.Task;

class ProgressOutputFile {

    private final PrintWriter writer;

    ProgressOutputFile() throws IOException {
        writer = new PrintWriter(new FileWriter(getFile()));
        writer.println("test;time;progress");
    }

    private File getFile() {
        return new File(TARGET_DIR, START + "-" + OTHER_PARAMETERS.label + "-progress.csv");
    }

    void recordProgress(String label, Task task) {
        long start = task.getLastRunStartTimestamp();
        Long lastFinish = task.getLastRunFinishTimestamp();
        long thisFinish = lastFinish != null && lastFinish > start ? lastFinish : System.currentTimeMillis();
        long running = thisFinish - start;
        long progress = task.getLegacyProgress();
        writer.println(label + task.getName() + ";" + running + ";" + progress);
        writer.flush();
    }
}
