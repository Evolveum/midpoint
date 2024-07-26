/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.icf.dummy.resource;

import static com.evolveum.icf.dummy.resource.LinkClassDefinition.LinkClassDefinitionBuilder.aLinkClassDefinition;
import static com.evolveum.icf.dummy.resource.LinkClassDefinition.Participant.ParticipantBuilder.aParticipant;

import static org.assertj.core.api.Assertions.assertThat;

import com.evolveum.midpoint.util.DebugUtil;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Low-level tests for the dummy resource itself.
 */
public class TestDummyResource {

    private static final String OC_PERSON = "person";
    private static final String ATTR_FIRST_NAME = "firstName";
    private static final String ATTR_LAST_NAME = "lastName";
    private static final String ATTR_CONTRACT = "contract";

    private static final String OC_CONTRACT = "contract";
    private static final String ATTR_ORG = "org";

    private static final String LC_PERSON_CONTRACT = "personContract";
    private static final String LC_CONTRACT_ORG = "contractOrg";

    private final DummyResource dummyResource = DummyResource.getInstance();

    @BeforeClass
    public void setup() throws Exception {
        createSchema();
    }

    private void createSchema() {
        var personOC = DummyObjectClass.standard();
        addAttrDef(personOC, ATTR_FIRST_NAME, String.class, false, false);
        addAttrDef(personOC, ATTR_LAST_NAME, String.class, false, false);
        dummyResource.addStructuralObjectClass(OC_PERSON, personOC);

        var contractOC = DummyObjectClass.embedded();
        dummyResource.addStructuralObjectClass(OC_CONTRACT, contractOC);

        dummyResource.addLinkClassDef(
                aLinkClassDefinition()
                        .withName(LC_PERSON_CONTRACT)
                        .withFirstParticipant(aParticipant()
                                .withObjectClassNames(OC_PERSON)
                                .withLinkAttributeName(ATTR_CONTRACT)
                                .withMaxOccurs(-1)
                                .build())
                        .withSecondParticipant(aParticipant()
                                .withObjectClassNames(OC_CONTRACT)
                                .withMinOccurs(1) // necessary for cascading deletion operations
                                .withMaxOccurs(1)
                                .build())
                        .build());
        dummyResource.addLinkClassDef(
                aLinkClassDefinition()
                        .withName(LC_CONTRACT_ORG)
                        .withFirstParticipant(aParticipant()
                                .withObjectClassNames(OC_CONTRACT)
                                .withLinkAttributeName(ATTR_ORG)
                                .withMinOccurs(1)
                                .withMaxOccurs(1)
                                .build())
                        .withSecondParticipant(aParticipant()
                                .withObjectClassNames(DummyOrg.OBJECT_CLASS_NAME)
                                .withLinkAttributeName(ATTR_CONTRACT)
                                .withMaxOccurs(-1)
                                .build())
                        .build());
    }

    @SuppressWarnings("SameParameterValue")
    private void addAttrDef(
            DummyObjectClass objectClass, String attrName, Class<?> type, boolean isRequired, boolean isMulti) {
        objectClass.add(
                new DummyAttributeDefinition(attrName, type, isRequired, isMulti));
    }

    @Test
    void test100CreateAndRetrieveLinks() throws Exception {

        // GIVEN

        // Faculties
        DummyOrg facultyOfScience = new DummyOrg("Faculty of science");
        dummyResource.addObject(facultyOfScience);

        DummyOrg facultyOfLaw = new DummyOrg("Faculty of law");
        dummyResource.addObject(facultyOfLaw);

        DummyOrg facultyOfMedicine = new DummyOrg("Faculty of medicine");
        dummyResource.addObject(facultyOfMedicine);

        // Persons & contracts

        // John works at three faculties
        DummyObject p100000 = new DummyGenericObject(OC_PERSON, "100000");
        p100000.addAttributeValue(ATTR_FIRST_NAME, "John");
        p100000.addAttributeValue(ATTR_LAST_NAME, "Doe");
        dummyResource.addObject(p100000);

        DummyObject c200000 = new DummyGenericObject(OC_CONTRACT, "200000");
        dummyResource.addObject(c200000);
        dummyResource.addLinkValue(LC_PERSON_CONTRACT, p100000, c200000);
        dummyResource.addLinkValue(LC_CONTRACT_ORG, c200000, facultyOfScience);

        DummyObject c200001 = new DummyGenericObject(OC_CONTRACT, "200001");
        dummyResource.addObject(c200001);
        dummyResource.addLinkValue(LC_PERSON_CONTRACT, p100000, c200001);
        dummyResource.addLinkValue(LC_CONTRACT_ORG, c200001, facultyOfLaw);

        DummyObject c200002 = new DummyGenericObject(OC_CONTRACT, "200002");
        dummyResource.addObject(c200002);
        dummyResource.addLinkValue(LC_PERSON_CONTRACT, p100000, c200002);
        dummyResource.addLinkValue(LC_CONTRACT_ORG, c200002, facultyOfMedicine);

        // Mark only at one
        DummyObject p100001 = new DummyGenericObject(OC_PERSON, "100001");
        p100001.addAttributeValue(ATTR_FIRST_NAME, "Mark");
        p100001.addAttributeValue(ATTR_LAST_NAME, "Smith");
        dummyResource.addObject(p100001);

        DummyObject c200003 = new DummyGenericObject(OC_CONTRACT, "200003");
        dummyResource.addObject(c200003);
        dummyResource.addLinkValue(LC_PERSON_CONTRACT, p100001, c200003);
        dummyResource.addLinkValue(LC_CONTRACT_ORG, c200003, facultyOfScience);

        System.out.println(dummyResource.debugDump());

        // WHEN + THEN

        var contractsForP100000 = p100000.getLinkedObjects(ATTR_CONTRACT);
        System.out.printf("Contracts for %s:\n%s%n", p100000, DebugUtil.debugDump(contractsForP100000, 1));
        assertThat(contractsForP100000).as("contracts for John").hasSize(3);

        var contractsForP100001 = p100001.getLinkedObjects(ATTR_CONTRACT);
        System.out.printf("Contracts for %s:\n%s%n", p100001, DebugUtil.debugDump(contractsForP100001, 1));
        assertThat(contractsForP100001).as("contracts for Mark").hasSize(1);

        var contractsForScience = facultyOfScience.getLinkedObjects(ATTR_CONTRACT);
        System.out.printf("Contracts for %s:\n%s%n", facultyOfScience, DebugUtil.debugDump(contractsForScience, 1));
        assertThat(contractsForScience).as("contracts for Faculty of science").hasSize(2);

        // WHEN deleting John
        dummyResource.deleteObjectByName(OC_PERSON, "100000");

        // THEN John's contracts should be deleted
        var contractsForScienceUpdated = facultyOfScience.getLinkedObjects(ATTR_CONTRACT);
        System.out.printf("Contracts for %s:\n%s%n", facultyOfScience, DebugUtil.debugDump(contractsForScienceUpdated, 1));
        assertThat(contractsForScienceUpdated).as("contracts for Faculty of science").hasSize(1);
        var contract = contractsForScienceUpdated.iterator().next();
        assertThat(contract).isEqualTo(contractsForP100001.iterator().next());
    }
}
