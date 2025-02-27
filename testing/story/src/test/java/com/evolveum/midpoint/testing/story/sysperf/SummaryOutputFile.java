package com.evolveum.midpoint.testing.story.sysperf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.*;

class SummaryOutputFile {

    private final PrintWriter writer;

    SummaryOutputFile() throws IOException {
        writer = new PrintWriter(new FileWriter(getFile()));
    }

    private File getFile() {
        return new File(TARGET_DIR, START + "-" + OTHER_PARAMETERS.label + "-summary.txt");
    }

    void logStart() {
        writer.println("Started: " + new Date(START) + " (" + START + ")");
        writer.println("Label: " + OTHER_PARAMETERS.label);
        writer.println();
        writer.printf("Schema: %s\n", SCHEMA_CONFIGURATION);
        writer.printf("Sources: %s\n", SOURCES_CONFIGURATION);
        writer.printf("Targets: %s\n", TARGETS_CONFIGURATION);
        writer.printf("Roles: %s\n", ROLES_CONFIGURATION);
        writer.printf("Import: %s\n", IMPORTS_CONFIGURATION);
        writer.printf("Reconciliation (with source): %s\n", RECONCILIATION_WITH_SOURCE_CONFIGURATION);
        writer.printf("Reconciliation (with target): %s\n", RECONCILIATION_WITH_TARGET_CONFIGURATION);
        writer.printf("Recomputation: %s\n", RECOMPUTATION_CONFIGURATION);
        writer.printf("Other: %s\n\n", OTHER_PARAMETERS);
        writer.flush();
    }

    void logTaskFinish(String desc, long executionTime, double timePerAccount) {
        writer.printf("********** FINISHED: %s **********\n\n", desc);
        writer.printf("Task execution time: %,d ms\n", executionTime);
        writer.printf("Time per account: %,.1f ms\n\n", timePerAccount);
        writer.flush();
    }

    void logFinish() {
        long end = System.currentTimeMillis();
        writer.printf("Finished at: %s\n", new Date(end));
        long millis = end - START;
        writer.printf("Took: %,.1f seconds = %.1f minutes\n", millis / 1000.0, millis / 60000.0);
        writer.printf("Accounts: %,d\n", SOURCES_CONFIGURATION.getNumberOfAccounts());
        writer.flush();
    }
}
