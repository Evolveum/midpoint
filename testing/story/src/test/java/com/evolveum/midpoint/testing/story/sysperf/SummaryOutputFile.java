package com.evolveum.midpoint.testing.story.sysperf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.*;

class SummaryOutputFile {

    private static final File FILE = new File(TARGET_DIR, START + "-summary.txt");
    private final PrintWriter writer;

    SummaryOutputFile() throws IOException {
        writer = new PrintWriter(new FileWriter(FILE));
    }

    void logStart() {
        writer.println("Started: " + new Date(START) + " (" + START + ")");
        writer.printf("Extension schema variant: %s\n", EXTENSION_SCHEMA_VARIANT);
        writer.printf("Source variant: %s\n", SOURCE_VARIANT);
        writer.printf("Threading variant: %s\n", THREADING_VARIANT);
        writer.printf("Population variant: %s\n", POPULATION_VARIANT);
        writer.printf("Progress file: %s\n\n", ProgressOutputFile.FILE);
        writer.flush();
    }

    void logTestFinish(String testName, long executionTime, double timePerAccount) {
        writer.printf("********** %s FINISHED **********\n\n", testName);
        writer.printf("Task execution time: %,d ms\n", executionTime);
        writer.printf("Time per account: %,.1f ms\n\n", timePerAccount);
        writer.flush();
    }
}
