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
        writer.printf("Sources: %s\n", SOURCES_CONFIGURATION);
        writer.printf("Targets: %s\n", TARGETS_CONFIGURATION);
        writer.printf("Roles: %s\n", ROLES_CONFIGURATION);
        writer.printf("Imports: %s\n", IMPORTS_CONFIGURATION);
        writer.printf("Reconciliations: %s\n", RECONCILIATIONS_CONFIGURATION);
        writer.printf("Recomputation: %s\n", RECOMPUTATION_CONFIGURATION);
        writer.printf("Progress file: %s\n\n", ProgressOutputFile.FILE);
        writer.flush();
    }

    void logTaskFinish(String desc, long executionTime, double timePerAccount) {
        writer.printf("********** FINISHED: %s **********\n\n", desc);
        writer.printf("Task execution time: %,d ms\n", executionTime);
        writer.printf("Time per account: %,.1f ms\n\n", timePerAccount);
        writer.flush();
    }
}
