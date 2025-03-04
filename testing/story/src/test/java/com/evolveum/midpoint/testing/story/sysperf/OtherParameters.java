package com.evolveum.midpoint.testing.story.sysperf;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.*;

public class OtherParameters {

    private static final String PROP_LABEL = "label";
    private static final String PROP_TASK_TIMEOUT = "taskTimeout";
    private static final String PROP_DISABLE_DEFAULT_MULTIVALUE_PROVENANCE = "disableDefaultMultivalueProvenance";

    private static final File SYSTEM_CONFIGURATION_TEMPLATE_FILE = new File(TEST_DIR, "system-configuration.vm.xml");

    final String label;
    final int taskTimeout;
    final boolean disableDefaultMultivalueProvenance;

    private OtherParameters() {
        this.label = System.getProperty(PROP_LABEL, createDefaultLabel());
        this.taskTimeout = Integer.parseInt(System.getProperty(PROP_TASK_TIMEOUT, "1800000")); // 30 minutes
        this.disableDefaultMultivalueProvenance = Boolean.parseBoolean(System.getProperty(PROP_DISABLE_DEFAULT_MULTIVALUE_PROVENANCE, "false"));

        createSystemConfigurationFile();
    }

    private String createDefaultLabel() {
        return SOURCES_CONFIGURATION.getNumberOfResources() + "s-" +
                getSourceMappingsLabel() + "-" +
                TARGETS_CONFIGURATION.getNumberOfResources() + "t-" +
                getTargetMappingsLabel() + "-" +
                getAssignmentsLabel() + "-" +
                getUsersLabel()
                ;
    }

    @NotNull
    private String getSourceMappingsLabel() {
        int single = SOURCES_CONFIGURATION.getSingleValuedMappings();
        int multi = SOURCES_CONFIGURATION.getMultiValuedMappings();
        if (single == multi) {
            return single + "m";
        } else {
            return single + "sm-" + multi + "mm";
        }
    }

    @NotNull
    private String getTargetMappingsLabel() {
        int single = TARGETS_CONFIGURATION.getSingleValuedMappings();
        int multi = TARGETS_CONFIGURATION.getMultiValuedMappings();
        if (single == multi) {
            return single + "m";
        } else {
            return single + "sm" + multi + "mm";
        }
    }

    private String getAssignmentsLabel() {
        int min = ROLES_CONFIGURATION.getNumberOfAssignmentsMin();
        int max = ROLES_CONFIGURATION.getNumberOfAssignmentsMax();
        if (min == max) {
            return min + "a";
        } else {
            return min + "-" + max + "a";
        }
    }

    private String getUsersLabel() {
        int users = SOURCES_CONFIGURATION.getNumberOfAccounts();

        int number;
        String suffix;

        if (users >= 1_000_000 && users % 1_000_000 == 0) {
            number = users / 1_000_000;
            suffix = "M";
        } else if (users >= 1_000 && users % 1_000 == 0) {
            number = users / 1_000;
            suffix = "k";
        } else {
            number = users;
            suffix = "";
        }
        return number + suffix + "u";
    }

    static OtherParameters setup() {
        return new OtherParameters();
    }

    private void createSystemConfigurationFile() {
        VelocityGenerator.generate(
                SYSTEM_CONFIGURATION_TEMPLATE_FILE,
                GENERATED_SYSTEM_CONFIGURATION_FILE,
                Map.of("disableDefaultMultivalueProvenance", disableDefaultMultivalueProvenance));
    }

    @Override
    public String toString() {
        return "OtherParameters{" +
                "label=" + label +
                ", taskTimeout=" + taskTimeout +
                ", disableDefaultMultivalueProvenance=" + disableDefaultMultivalueProvenance +
                '}';
    }
}
