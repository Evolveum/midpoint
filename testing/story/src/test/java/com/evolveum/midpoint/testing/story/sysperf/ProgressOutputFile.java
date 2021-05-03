package com.evolveum.midpoint.testing.story.sysperf;

import com.evolveum.midpoint.task.api.Task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TARGET_DIR;
import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.START;

class ProgressOutputFile {

    static final File FILE = new File(TARGET_DIR, START + "-progress.csv");
    private final PrintWriter writer;

    ProgressOutputFile() throws IOException {
        writer = new PrintWriter(new FileWriter(FILE));
        writer.println("test;time;progress");
    }

    void recordProgress(String label, Task task) {
        long start = task.getLastRunStartTimestamp();
        Long lastFinish = task.getLastRunFinishTimestamp();
        long thisFinish = lastFinish != null && lastFinish > start ? lastFinish : System.currentTimeMillis();
        long running = thisFinish - start;
        long progress = task.getProgress();
        writer.println(label + task.getName() + ";" + running + ";" + progress);
        writer.flush();
    }
}
