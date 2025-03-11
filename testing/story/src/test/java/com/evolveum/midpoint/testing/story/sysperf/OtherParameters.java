package com.evolveum.midpoint.testing.story.sysperf;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.MiscUtil;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.*;

public class OtherParameters {

    private static final String PROP_LABEL = "label";
    private static final String PROP_TASK_TIMEOUT = "taskTimeout";
    private static final String PROP_CACHED = "cached";
    private static final String PROP_NOT_CACHED = "not-cached";
    private static final String PROP_CLEAR_REPO_CACHE_BEFORE_TASK_RUN = "clearRepoCacheBeforeTaskRun";

    private static final File SYSTEM_CONFIGURATION_TEMPLATE_FILE = new File(TEST_DIR, "system-configuration.vm.xml");

    private static final Set<ObjectTypes> CACHED_BY_DEFAULT = Set.of(
            ObjectTypes.SYSTEM_CONFIGURATION,
            ObjectTypes.ARCHETYPE,
            ObjectTypes.OBJECT_TEMPLATE,
            ObjectTypes.SECURITY_POLICY,
            ObjectTypes.PASSWORD_POLICY, // ValuePolicyType
            ObjectTypes.RESOURCE,
            ObjectTypes.ROLE,
            ObjectTypes.SHADOW,
            ObjectTypes.MARK,
            ObjectTypes.CONNECTOR);

    final String label;
    final int taskTimeout;
    private final Set<ObjectTypes> cached;
    private final Set<ObjectTypes> notCached;
    private final boolean clearRepoCacheBeforeTaskRun;

    private OtherParameters() {
        this.label = System.getProperty(PROP_LABEL, createDefaultLabel());
        this.taskTimeout = Integer.parseInt(System.getProperty(PROP_TASK_TIMEOUT, "1800000")); // 30 minutes
        this.cached = parseObjectTypeNames(System.getProperty(PROP_CACHED));
        this.notCached = parseObjectTypeNames(System.getProperty(PROP_NOT_CACHED));
        this.clearRepoCacheBeforeTaskRun = Boolean.parseBoolean(System.getProperty(PROP_CLEAR_REPO_CACHE_BEFORE_TASK_RUN, "false"));

        createSystemConfigurationFile();
    }

    private Set<ObjectTypes> parseObjectTypeNames(String stringValue) {
        return Arrays.stream(MiscUtil.emptyIfNull(stringValue).split(","))
                .filter(s -> !s.isBlank())
                .map(name -> ObjectTypes.getObjectType(name.trim()))
                .collect(Collectors.toUnmodifiableSet());
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

    boolean isClearRepoCacheBeforeTaskRun() {
        return clearRepoCacheBeforeTaskRun;
    }

    static OtherParameters setup() {
        return new OtherParameters();
    }

    private void createSystemConfigurationFile() {
        VelocityGenerator.generate(
                SYSTEM_CONFIGURATION_TEMPLATE_FILE,
                GENERATED_SYSTEM_CONFIGURATION_FILE,
                Map.of("cachedTypeNames", computeCachedTypeNames()));
    }

    private Collection<String> computeCachedTypeNames() {
        var effective = Sets.difference(
                Sets.union(CACHED_BY_DEFAULT, cached),
                notCached);
        return effective.stream()
                .map(ObjectTypes::getValue)
                .toList();
    }

    @Override
    public String toString() {
        return "OtherParameters{" +
                "label=" + label +
                ", taskTimeout=" + taskTimeout +
                ", cached=" + cached +
                ", notCached=" + notCached +
                ", clearRepoCacheBeforeTaskRun=" + clearRepoCacheBeforeTaskRun +
                '}';
    }
}
