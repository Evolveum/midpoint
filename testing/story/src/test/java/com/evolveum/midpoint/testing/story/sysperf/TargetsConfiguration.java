/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import static com.evolveum.icf.dummy.resource.LinkClassDefinition.LinkClassDefinitionBuilder.aLinkClassDefinition;
import static com.evolveum.icf.dummy.resource.LinkClassDefinition.Participant.ParticipantBuilder.aParticipant;
import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;

class TargetsConfiguration {

    private static final String PROP = "targets";
    private static final String PROP_RESOURCES = PROP + ".resources";
    private static final String PROP_SINGLE_MAPPINGS = PROP + ".single-mappings";
    private static final String PROP_MULTI_MAPPINGS = PROP + ".multi-mappings";
    private static final String PROP_MAPPING_STRENGTH = PROP + ".mapping-strength";
    private static final String PROP_ASSOCIATIONS = PROP + ".associations";

    private static final String RESOURCE_INSTANCE_TEMPLATE = "target-%03d";
    private static final String A_SINGLE_NAME = "a-single-%04d";
    private static final String A_MULTI_NAME = "a-multi-%04d";
    static final String A_MEMBERSHIP = "membership";

    private static final File RESOURCE_TARGET_TEMPLATE_FILE = new File(TEST_DIR, "resource-target.vm.xml");
    private static final File ROLE_TARGETS_TEMPLATE_FILE = new File(TEST_DIR, "role-targets.vm.xml");

    private final int numberOfResources;
    private final int singleValuedMappings;
    private final int multiValuedMappings;
    private final String mappingStrength;
    private final ScriptingConfiguration scriptingConfiguration;
    @NotNull private final Associations associations;

    @NotNull private final OperationDelay operationDelay;

    private List<DummyTestResource> generatedResources;

    private TargetsConfiguration(ScriptingConfiguration scriptingConfiguration) {
        numberOfResources = Integer.parseInt(System.getProperty(PROP_RESOURCES, "0"));
        singleValuedMappings = Integer.parseInt(System.getProperty(PROP_SINGLE_MAPPINGS, "0"));
        multiValuedMappings = Integer.parseInt(System.getProperty(PROP_MULTI_MAPPINGS, "0"));
        mappingStrength = System.getProperty(PROP_MAPPING_STRENGTH, MappingStrengthType.STRONG.value());
        associations = Associations.fromValue(System.getProperty(PROP_ASSOCIATIONS));

        operationDelay = OperationDelay.fromSystemProperties(PROP);
        this.scriptingConfiguration = scriptingConfiguration;
    }

    int getSingleValuedMappings() {
        return singleValuedMappings;
    }

    int getMultiValuedMappings() {
        return multiValuedMappings;
    }

    int getNumberOfResources() {
        return numberOfResources;
    }

    public String getMappingStrength() {
        return mappingStrength;
    }

    @NotNull OperationDelay getOperationDelay() {
        return operationDelay;
    }

    public static TargetsConfiguration setup(ScriptingConfiguration scriptingConfiguration) {
        TargetsConfiguration configuration = new TargetsConfiguration(scriptingConfiguration);
        generateRoleTargets(configuration.generateDummyTestResources());
        System.out.println("Targets: " + configuration);
        return configuration;
    }

    @Override
    public String toString() {
        return "TargetsConfiguration{" +
                "numberOfResources=" + numberOfResources +
                ", singleValuedMappings=" + singleValuedMappings +
                ", multiValuedMappings=" + multiValuedMappings +
                ", mappingStrength=" + mappingStrength +
                ", associations=" + associations +
                ", operationDelay=" + operationDelay +
                '}';
    }

    private List<DummyTestResource> generateDummyTestResources() {
        if (generatedResources != null) {
            return generatedResources;
        }
        generatedResources = new ArrayList<>();
        for (int i = 0; i < numberOfResources; i++) {
            String oid = RandomSource.randomUUID().toString();
            String resourceDefinitionFile = createResourceDefinition(i, oid);
            generatedResources.add(new DummyTestResource(TARGET_DIR, resourceDefinitionFile, oid, getResourceInstance(i),
                    controller -> {
                        DummyResource dummyResource = controller.getDummyResource();
                        createAttributes(controller, A_SINGLE_NAME, singleValuedMappings, false);
                        createAttributes(controller, A_MULTI_NAME, multiValuedMappings, true);
                        controller.addAttrDef(dummyResource.getAccountObjectClass(),
                                A_MEMBERSHIP, String.class, false, true);
                        controller.addAttrDef(dummyResource.getGroupObjectClass(),
                                DummyGroup.ATTR_MEMBERS_NAME, String.class, false, true);

                        if (associations.isNativeReferences()) {
                            // The membership is intentionally defined only for accounts, not for groups.
                            // The reason is that the current implementation of links in the dummy resource is quite slow:
                            // iterating through all existing links. (Similar to the implementation of the "memberOf", which
                            // iterates through all the groups to find the membership.) However, unfortunately, for native
                            // references there's an extra problem in that membership of the groups an account is a member of
                            // is checked as well, resulting in O(l^2) complexity, where l is the number of links. This is
                            // observably worse than the complexity of the "memberOf" attribute determination, which is O(g),
                            // where g is the number of groups. Both should be optimized in the future.
                            controller.addLinkClassDefinition(
                                    aLinkClassDefinition()
                                            .withName("groupMembership")
                                            .withFirstParticipant(aParticipant()
                                                    .withObjectClassNames(DummyAccount.OBJECT_CLASS_NAME)
                                                    .withLinkAttributeName("group")
                                                    .withMaxOccurs(-1)
                                                    .withReturnedByDefault(true)
                                                    .withExpandedByDefault(false)
                                                    .build())
                                            .withSecondParticipant(aParticipant()
                                                    .withObjectClassNames(DummyGroup.OBJECT_CLASS_NAME)
                                                    .build())
                                            .build());
                        }
                    }));
        }
        return generatedResources;
    }

    private String createResourceDefinition(int index, String oid) {
        String generatedFileName = String.format("generated-resource-target-%03d.xml", index);

        File generated = new File(TARGET_DIR, generatedFileName);
        VelocityGenerator.generate(RESOURCE_TARGET_TEMPLATE_FILE, generated,
                Map.of("resourceOid", oid,
                        "resourceInstance", getResourceInstance(index),
                        "multiValuedIndexList", Util.createIndexList(multiValuedMappings),
                        "singleValuedIndexList", Util.createIndexList(singleValuedMappings),
                        "mappingStrength", mappingStrength,
                        "associationShortcut", associations.isAssociationShortcut(),
                        "nativeReferences", associations.isNativeReferences(),
                        "scriptingLanguage", scriptingConfiguration.language()));

        return generatedFileName;
    }

    @NotNull
    private String getResourceInstance(int i) {
        return String.format(RESOURCE_INSTANCE_TEMPLATE, i);
    }

    private void createAttributes(DummyResourceContoller controller, String name, int number, boolean multi) {
        DummyObjectClass objectClass = controller.getDummyResource().getAccountObjectClass();
        for (int i = 0; i < number; i++) {
            controller.addAttrDef(objectClass, String.format(name, i), String.class, false, multi);
        }
    }

    List<DummyTestResource> getGeneratedResources() {
        return generatedResources;
    }

    private static void generateRoleTargets(List<DummyTestResource> testResources) {
        List<String> targetOidList = testResources.stream()
                .map(r -> r.oid)
                .toList();
        VelocityGenerator.generate(
                ROLE_TARGETS_TEMPLATE_FILE, ((TestObject.FileBasedTestObjectSource) ROLE_TARGETS.source).getFile(),
                Map.of("oidList", targetOidList));
    }

}
