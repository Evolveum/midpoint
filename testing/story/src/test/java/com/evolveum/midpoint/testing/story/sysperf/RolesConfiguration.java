/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.*;

import static java.util.Collections.emptyList;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.jetbrains.annotations.NotNull;

class RolesConfiguration {

    private static final String PROP = "roles";
    private static final String PROP_BUSINESS = PROP + ".business";
    private static final String PROP_BUSINESS_COUNT = PROP_BUSINESS + ".count";
    private static final String PROP_TECHNICAL = PROP + ".technical";
    private static final String PROP_TECHNICAL_COUNT = PROP_TECHNICAL + ".count";
    private static final String PROP_ASSIGNMENTS = PROP + ".assignments";
    private static final String PROP_ASSIGNMENTS_COUNT = PROP_ASSIGNMENTS + ".count";
    private static final String PROP_ASSIGNMENTS_MIN = PROP_ASSIGNMENTS + ".min";
    private static final String PROP_ASSIGNMENTS_MAX = PROP_ASSIGNMENTS + ".max";
    private static final String PROP_INDUCEMENTS = PROP + ".inducements";
    private static final String PROP_INDUCEMENTS_COUNT = PROP_INDUCEMENTS + ".count";
    private static final String PROP_INDUCEMENTS_MIN = PROP_INDUCEMENTS + ".min";
    private static final String PROP_INDUCEMENTS_MAX = PROP_INDUCEMENTS + ".max";
    private static final String PROP_MEMBER_OF_COMPUTATION = PROP + ".memberOfComputation";

    private static final File BUSINESS_ROLE_TEMPLATE_FILE = new File(TEST_DIR, "role-business.vm.xml");
    private static final File TECHNICAL_ROLE_TEMPLATE_FILE = new File(TEST_DIR, "role-technical.vm.xml");

    private final int numberOfBusinessRoles;
    private final int numberOfTechnicalRoles;
    private final int numberOfAssignmentsMin;
    private final int numberOfAssignmentsMax;
    private final int numberOfInducementsMin;
    private final int numberOfInducementsMax;

    private final boolean memberOfComputation;

    private final List<TestObject<RoleType>> generatedBusinessRoles;
    private final List<TestObject<RoleType>> generatedTechnicalRoles;

    private RolesConfiguration() {
        numberOfBusinessRoles = Integer.parseInt(System.getProperty(PROP_BUSINESS_COUNT, "2"));
        numberOfTechnicalRoles = Integer.parseInt(System.getProperty(PROP_TECHNICAL_COUNT, "2"));
        String assignmentsCount = System.getProperty(PROP_ASSIGNMENTS_COUNT);
        if (assignmentsCount != null) {
            numberOfAssignmentsMax = numberOfAssignmentsMin = Integer.parseInt(assignmentsCount);
        } else {
            numberOfAssignmentsMin = Integer.parseInt(System.getProperty(PROP_ASSIGNMENTS_MIN, "1"));
            numberOfAssignmentsMax = Integer.parseInt(System.getProperty(PROP_ASSIGNMENTS_MAX, String.valueOf(numberOfAssignmentsMin)));
        }
        String inducementsCount = System.getProperty(PROP_INDUCEMENTS_COUNT);
        if (inducementsCount != null) {
            numberOfInducementsMax = numberOfInducementsMin = Integer.parseInt(inducementsCount);
        } else {
            numberOfInducementsMin = Integer.parseInt(System.getProperty(PROP_INDUCEMENTS_MIN, "1"));
            numberOfInducementsMax = Integer.parseInt(System.getProperty(PROP_INDUCEMENTS_MAX, String.valueOf(numberOfInducementsMin)));
        }
        memberOfComputation = Boolean.parseBoolean(System.getProperty(PROP_MEMBER_OF_COMPUTATION, "false"));

        generatedTechnicalRoles = generateTechnicalRoles();
        generatedBusinessRoles = generateBusinessRoles();
    }

    int getNumberOfBusinessRoles() {
        return numberOfBusinessRoles;
    }

    int getNumberOfTechnicalRoles() {
        return numberOfTechnicalRoles;
    }

    int getNumberOfAssignmentsMin() {
        return numberOfAssignmentsMin;
    }

    int getNumberOfAssignmentsMax() {
        return numberOfAssignmentsMax;
    }

    int getNumberOfInducementsMin() {
        return numberOfInducementsMin;
    }

    int getNumberOfInducementsMax() {
        return numberOfInducementsMax;
    }

    List<TestObject<RoleType>> getGeneratedBusinessRoles() {
        return generatedBusinessRoles;
    }

    List<TestObject<RoleType>> getGeneratedTechnicalRoles() {
        return generatedTechnicalRoles;
    }

    boolean isMemberOfComputation() {
        return memberOfComputation;
    }

    @Override
    public String toString() {
        return "RolesConfiguration{" +
                "numberOfBusinessRoles=" + numberOfBusinessRoles +
                ", numberOfTechnicalRoles=" + numberOfTechnicalRoles +
                ", numberOfAssignmentsMin=" + numberOfAssignmentsMin +
                ", numberOfAssignmentsMax=" + numberOfAssignmentsMax +
                ", numberOfInducementsMin=" + numberOfInducementsMin +
                ", numberOfInducementsMax=" + numberOfInducementsMax +
                ", memberOfComputation=" + memberOfComputation +
                '}';
    }

    public static RolesConfiguration setup() {
        RolesConfiguration configuration = new RolesConfiguration();
        System.out.println("Roles: " + configuration);
        return configuration;
    }

    private List<TestObject<RoleType>> generateTechnicalRoles() {
        List<TestObject<RoleType>> roles = new ArrayList<>();
        for (int i = 0; i < numberOfTechnicalRoles; i++) {
            String oid = RandomSource.randomUUID().toString();
            String fileName = createTechnicalRoleDefinition(i, oid);
            roles.add(TestObject.file(TARGET_DIR, fileName, oid));
        }
        return roles;
    }

    private String createTechnicalRoleDefinition(int index, String oid) {
        String fileName = String.format("generated-technical-role-%04d.xml", index);
        String resourceOid;
        if (TARGETS_CONFIGURATION.getNumberOfResources() > 0) {
            resourceOid = TARGETS_CONFIGURATION.getGeneratedResources()
                    .get(index % TARGETS_CONFIGURATION.getNumberOfResources())
                    .oid;
        } else {
            resourceOid = "";
        }

        File generated = new File(TARGET_DIR, fileName);
        VelocityGenerator.generate(TECHNICAL_ROLE_TEMPLATE_FILE, generated,
                Map.of("oid", oid,
                        "index", String.format("%04d", index),
                        "resourceOid", resourceOid,
                        "metarole", memberOfComputation));

        return fileName;
    }

    private List<TestObject<RoleType>> generateBusinessRoles() {
        List<TestObject<RoleType>> roles = new ArrayList<>();
        for (int i = 0; i < numberOfBusinessRoles; i++) {
            String oid = RandomSource.randomUUID().toString();
            List<String> inducedOidList = createInducedOidList();
            String fileName = createBusinessRoleDefinition(i, oid, inducedOidList);
            roles.add(TestObject.file(TARGET_DIR, fileName, oid));
        }
        return roles;
    }

    private List<String> createInducedOidList() {
        int size = randomFromInterval(numberOfInducementsMin, numberOfInducementsMax);
        if (size <= 0) {
            return emptyList();
        } else {
            List<String> technicalRolesOidList = generatedTechnicalRoles.stream()
                    .map(r -> r.oid)
                    .collect(Collectors.toList());
            return randomFromList(technicalRolesOidList, size);
        }
    }

    @NotNull
    private List<String> randomFromList(List<String> technicalRolesOidList, int size) {
        Collections.shuffle(technicalRolesOidList, RandomSource.FIXED_RANDOM);
        return technicalRolesOidList.subList(0, size);
    }

    private int randomFromInterval(int min, int max) {
        //noinspection OptionalGetWithoutIsPresent
        return RandomSource.FIXED_RANDOM
                .ints(min, max + 1)
                .findFirst().getAsInt();
    }

    private String createBusinessRoleDefinition(int index, String oid, List<String> inducedOidList) {
        String fileName = String.format("generated-business-role-%04d.xml", index);

        File generated = new File(TARGET_DIR, fileName);
        VelocityGenerator.generate(BUSINESS_ROLE_TEMPLATE_FILE, generated,
                Map.of("oid", oid,
                        "name", getBusinessRoleName(index),
                        "inducedOidList", inducedOidList));

        return fileName;
    }

    private String getBusinessRoleName(int index) {
        return String.format("business-%04d", index);
    }

    /**
     * Generates random names of business roles for an account.
     */
    List<String> getRolesForAccount() {
        int assignedRoles = randomFromInterval(numberOfAssignmentsMin, numberOfAssignmentsMax);
        if (assignedRoles == 0) {
            return List.of();
        } else {
            List<String> businessRolesNames = IntStream.range(0, numberOfBusinessRoles)
                    .mapToObj(this::getBusinessRoleName)
                    .collect(Collectors.toList());
            return randomFromList(businessRolesNames, assignedRoles);
        }
    }
}
