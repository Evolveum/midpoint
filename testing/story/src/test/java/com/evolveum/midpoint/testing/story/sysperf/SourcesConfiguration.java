/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import static java.util.Collections.emptyList;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TARGET_DIR;
import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TEST_DIR;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;

class SourcesConfiguration {

    private static final String PROP = "sources";
    private static final String PROP_RESOURCES = PROP + ".resources";
    private static final String PROP_ACCOUNTS = PROP + ".accounts";
    private static final String PROP_SINGLE_MAPPINGS = PROP + ".single-mappings";
    private static final String PROP_MULTI_MAPPINGS = PROP + ".multi-mappings";
    private static final String PROP_MULTI_ATTR_VALUES = PROP + ".multi-attr-values";

    private static final String RESOURCE_INSTANCE_TEMPLATE = "source-%03d";
    static final String A_SINGLE_NAME = "a-single-%04d";
    static final String A_MULTI_NAME = "a-multi-%04d";
    static final String A_ROLE = "role";

    private static final File RESOURCE_TEMPLATE_FILE = new File(TEST_DIR, "resource-source.vm.xml");

    private final int numberOfResources;
    private final int numberOfAccounts;
    private final int singleValuedMappings;
    private final int multiValuedMappings;
    private final int attributeValues;

    @NotNull private final OperationDelay operationDelay;

    private final List<DummyTestResource> generatedResources;

    private SourcesConfiguration() {
        numberOfResources = Integer.parseInt(System.getProperty(PROP_RESOURCES, "1"));
        numberOfAccounts = Integer.parseInt(System.getProperty(PROP_ACCOUNTS, "10"));
        singleValuedMappings = Integer.parseInt(System.getProperty(PROP_SINGLE_MAPPINGS, "1"));
        multiValuedMappings = Integer.parseInt(System.getProperty(PROP_MULTI_MAPPINGS, "1"));
        attributeValues = Integer.parseInt(System.getProperty(PROP_MULTI_ATTR_VALUES, "5"));

        operationDelay = OperationDelay.fromSystemProperties(PROP);

        generatedResources = generateDummyTestResources();
    }

    int getNumberOfResources() {
        return numberOfResources;
    }

    int getNumberOfAccounts() {
        return numberOfAccounts;
    }

    int getSingleValuedMappings() {
        return singleValuedMappings;
    }

    int getMultiValuedMappings() {
        return multiValuedMappings;
    }

    int getAttributeValues() {
        return attributeValues;
    }

    @NotNull OperationDelay getOperationDelay() {
        return operationDelay;
    }

    List<DummyTestResource> getGeneratedResources() {
        return generatedResources;
    }

    @Override
    public String toString() {
        return "SourcesConfiguration{" +
                "numberOfResources=" + numberOfResources +
                ", numberOfAccounts=" + numberOfAccounts +
                ", singleValuedMappings=" + singleValuedMappings +
                ", multiValuedMappings=" + multiValuedMappings +
                ", attributeValues=" + attributeValues +
                ", operationDelay=" + operationDelay +
                '}';
    }

    public static SourcesConfiguration setup() {
        SourcesConfiguration configuration = new SourcesConfiguration();
        System.out.println("Sources: " + configuration);
        return configuration;
    }

    private List<DummyTestResource> generateDummyTestResources() {
        List<DummyTestResource> resources = new ArrayList<>();
        for (int i = 0; i < numberOfResources; i++) {
            boolean primary = i == 0;
            String oid = RandomSource.randomUUID().toString();
            String resourceDefinitionFile = createResourceDefinition(i, oid, primary);
            resources.add(new DummyTestResource(TARGET_DIR, resourceDefinitionFile, oid, getResourceInstance(i),
                    controller -> {
                        if (primary) {
                            createAttributes(controller, A_SINGLE_NAME, singleValuedMappings, false);
                            controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                                    A_ROLE, String.class, false, true);
                        }
                        createAttributes(controller, A_MULTI_NAME, multiValuedMappings, true);
                    }));
        }
        return resources;
    }

    private String createResourceDefinition(int index, String oid, boolean primary) {
        String generatedFileName = String.format("generated-resource-source-%03d.xml", index);

        File generated = new File(TARGET_DIR, generatedFileName);
        VelocityGenerator.generate(RESOURCE_TEMPLATE_FILE, generated,
                Map.of("resourceOid", oid,
                        "resourceInstance", getResourceInstance(index),
                        "multiValuedIndexList", Util.createIndexList(multiValuedMappings),
                        "singleValuedIndexList", primary ?
                                Util.createIndexList(singleValuedMappings) : emptyList(),
                        "primary", primary));
        return generatedFileName;
    }

    @NotNull
    private String getResourceInstance(int i) {
        return String.format(RESOURCE_INSTANCE_TEMPLATE, i);
    }

    private void createAttributes(DummyResourceContoller controller, String name, int number, boolean multi)
            throws ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        DummyObjectClass objectClass = controller.getDummyResource().getAccountObjectClass();
        for (int i = 0; i < number; i++) {
            controller.addAttrDef(objectClass, String.format(name, i), String.class, false, multi);
        }
    }
}
