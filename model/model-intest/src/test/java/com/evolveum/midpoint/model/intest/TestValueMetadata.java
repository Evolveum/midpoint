/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.MetadataItemProcessingSpec;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests the value metadata handling.
 *
 * TODO add asserts for single focus modification per clockwork run
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestValueMetadata extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/metadata");

    //region Constants for level of assurance recording scenario
    private static final File LEVEL_OF_ASSURANCE_DIR = new File(TEST_DIR, "level-of-assurance");

    private static final TestResource<UserType> USER_BOB = new TestResource<>(LEVEL_OF_ASSURANCE_DIR, "user-bob.xml", "cab2344d-06c0-4881-98ee-7075bf5d1309");
    private static final TestResource<UserType> USER_CHUCK = new TestResource<>(LEVEL_OF_ASSURANCE_DIR, "user-chuck.xml", "3eb9ca6b-49b8-4602-943a-992d8eb9adad");

    private static final TestResource<ObjectTemplateType> TEMPLATE_LOA_USER = new TestResource<>(LEVEL_OF_ASSURANCE_DIR, "template-loa-user.xml", "b1005d3d-6ef4-4347-b235-313666824ed8");
    //endregion

    //region Constants for sensitivity propagation scenario
    private static final File SENSITIVITY_PROPAGATION_DIR = new File(TEST_DIR, "sensitivity-propagation");

    private static final TestResource<ArchetypeType> ARCHETYPE_USER_SENSITIVITY_PROPAGATION = new TestResource<>(
            SENSITIVITY_PROPAGATION_DIR, "archetype-user-sensitivity-propagation.xml", "4231f36d-4e57-4597-8b6d-a7ce3c709616");
    private static final TestResource<ObjectTemplateType> TEMPLATE_USER_SENSITIVITY_PROPAGATION = new TestResource<>(
            SENSITIVITY_PROPAGATION_DIR, "template-user-sensitivity-propagation.xml", "60b83ded-57ea-4987-9d88-af13d2862649");
    private static final TestResource<OrgType> ORG_EMPLOYEES = new TestResource<>(
            SENSITIVITY_PROPAGATION_DIR, "org-employees.xml", "e1d97086-d1a1-4541-bd0b-fe694ecf767e");
    private static final TestResource<OrgType> ORG_SPECIAL_MEDICAL_SERVICES = new TestResource<>(
            SENSITIVITY_PROPAGATION_DIR, "org-special-medical-services.xml", "29963fc9-f494-4911-af3c-9e73fd64617f");
    private static final TestResource<UserType> USER_JIM = new TestResource<>(
            SENSITIVITY_PROPAGATION_DIR, "user-jim.xml", "8d162a31-00a8-48dc-b96f-08d3a85ada1d");
    //endregion

    //region Constants for creation metadata recording scenario
    private static final File CREATION_METADATA_RECORDING_DIR = new File(TEST_DIR, "creation-metadata-recording");

    private static final TestResource<ArchetypeType> ARCHETYPE_CREATION_METADATA_RECORDING = new TestResource<>(
            CREATION_METADATA_RECORDING_DIR, "archetype-creation-metadata-recording.xml", "5fb59a01-e5b9-4531-931d-923c94f341aa");
    private static final TestResource<ObjectTemplateType> TEMPLATE_CREATION_METADATA_RECORDING = new TestResource<>(
            CREATION_METADATA_RECORDING_DIR, "template-creation-metadata-recording.xml", "00301846-fe73-476a-83be-6bfe13251b4a");
    private static final TestResource<UserType> USER_PAUL = new TestResource<>(
            CREATION_METADATA_RECORDING_DIR, "user-paul.xml", "7c8e736b-b195-4ca1-bce4-12f86ff1bc71");
    //endregion

    //region Constants for provenance metadata recording scenario
    private static final File PROVENANCE_METADATA_RECORDING_DIR = new File(TEST_DIR, "provenance-metadata-recording");

    private static final TestResource<ArchetypeType> ARCHETYPE_PROVENANCE_METADATA_RECORDING = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "archetype-provenance-metadata-recording.xml", "b2117a51-a516-4151-9168-30f8baa78ec2");
    private static final TestResource<ObjectTemplateType> TEMPLATE_PROVENANCE_METADATA_RECORDING = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "template-provenance-metadata-recording.xml", "9ff4dcad-8f7e-4a28-8515-83cf50daec22");

    private static final TestResource<ServiceType> ORIGIN_ADMIN_ENTRY = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "origin-admin-entry.xml", "6c0a7a75-0551-4842-807d-424e279a257f");
    private static final TestResource<ServiceType> ORIGIN_SELF_SERVICE_APP = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "origin-self-service-app.xml", "760fda34-846f-4aac-a5ac-881c0ff23653");
    private static final TestResource<ServiceType> ORIGIN_HR_FEED = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "origin-hr-feed.xml", "f43bd824-e07e-4a41-950e-00de06881555");
    private static final TestResource<ServiceType> ORIGIN_CRM_FEED = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "origin-crm-feed.xml", "28c230b1-7209-4aa0-a5b9-776b22e14960");
    private static final TestResource<TaskType> TASK_HR_IMPORT = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "task-hr-import.xml", "da0a48bb-1294-475e-bcd3-51bfb9813885");
    private static final TestResource<TaskType> TASK_HR_RECONCILIATION = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "task-hr-reconciliation.xml", "0c1f0434-6409-47b8-b7f5-2f44510385c2");
    private static final TestResource<TaskType> TASK_CRM_IMPORT = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "task-crm-import.xml", "59a74606-0550-470b-856d-50890c31f3a4");

    private static final String ATTR_FIRSTNAME = "firstname";
    private static final String ATTR_LASTNAME = "lastname";
    private static final String ATTR_ORGANIZATION = "organization";
    private static final String ATTR_LOA = "loa";

    private static final DummyTestResource RESOURCE_HR = new DummyTestResource(
            PROVENANCE_METADATA_RECORDING_DIR, "resource-hr.xml", "9a34c3b6-aca5-4f9b-aae4-24f3f2d98ce9", "hr", controller -> {
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_FIRSTNAME, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_LASTNAME, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_ORGANIZATION, String.class, false, true);
    });
    private static final DummyTestResource RESOURCE_CRM = new DummyTestResource(
            PROVENANCE_METADATA_RECORDING_DIR, "resource-crm.xml", "74a73b98-3372-465b-a247-3d695b794170", "crm", controller -> {
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_FIRSTNAME, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_LASTNAME, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_ORGANIZATION, String.class, false, true);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_LOA, Integer.class, false, false);
    });

    private static final TestResource<UserType> USER_LEONHARD = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "user-leonhard.xml", "31984da7-e162-4e22-a437-6f60d80092c4");
    //endregion

    private static final String NS_EXT_METADATA = "http://midpoint.evolveum.com/xml/ns/samples/metadata";
    private static final ItemName LOA_NAME = new ItemName(NS_EXT_METADATA, "loa");
    private static final ItemPath LOA_PATH = ItemPath.create(ObjectType.F_EXTENSION, LOA_NAME);
    private static final ItemName SENSITIVITY_NAME = new ItemName(NS_EXT_METADATA, "sensitivity");
    private static final ItemPath SENSITIVITY_PATH = ItemPath.create(ObjectType.F_EXTENSION, SENSITIVITY_NAME);

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");
    private static final TestResource<UserType> USER_ALICE = new TestResource<>(TEST_DIR, "user-alice.xml", "9fc389be-5b47-4e9d-90b5-33fffd87b3ca");
    private static final String USER_BLAISE_NAME = "blaise";

    private static final ItemPath PATH_ALIAS = ItemPath.create(UserType.F_EXTENSION, new ItemName("alias")); // TODO namespace
    private static final ItemPath PATH_ASSURED_ORGANIZATION = ItemPath.create(UserType.F_EXTENSION, new ItemName("assuredOrganization")); // TODO namespace

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(ARCHETYPE_USER_SENSITIVITY_PROPAGATION, initTask, initResult);
        addObject(TEMPLATE_USER_SENSITIVITY_PROPAGATION, initTask, initResult);
        addObject(ORG_EMPLOYEES, initTask, initResult);
        addObject(ORG_SPECIAL_MEDICAL_SERVICES, initTask, initResult);

        addObject(ARCHETYPE_CREATION_METADATA_RECORDING, initTask, initResult);
        addObject(TEMPLATE_CREATION_METADATA_RECORDING, initTask, initResult);

        addObject(ARCHETYPE_PROVENANCE_METADATA_RECORDING, initTask, initResult);
        addObject(TEMPLATE_PROVENANCE_METADATA_RECORDING, initTask, initResult);

        initDummyResource(RESOURCE_HR, initTask, initResult);
        initDummyResource(RESOURCE_CRM, initTask, initResult);

        addObject(ORIGIN_ADMIN_ENTRY, initTask, initResult);
        addObject(ORIGIN_SELF_SERVICE_APP, initTask, initResult);
        addObject(ORIGIN_HR_FEED, initTask, initResult);
        addObject(ORIGIN_CRM_FEED, initTask, initResult);

        addObject(TASK_HR_IMPORT, initTask, initResult);
        addObject(TASK_HR_RECONCILIATION, initTask, initResult);
        addObject(TASK_CRM_IMPORT, initTask, initResult);

        addObject(USER_LEONHARD, initTask, initResult);

        addObject(TEMPLATE_LOA_USER, initTask, initResult);
        addObject(USER_ALICE, initTask, initResult);

//        setGlobalTracingOverride(createModelLoggingTracingProfile());
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    //region Basic tests
    @Test
    public void test010KeepingLiveMetadata() {
        given();
        UserType mark = new UserType(prismContext)
                .name("mark");
        PrismPropertyValue<PolyString> nameValue = mark.asPrismObject()
                .findProperty(UserType.F_NAME)
                .getValue(PolyString.class);

        // Creates empty value metadata
        nameValue.getValueMetadata();

        when();
        ValueMetadata metadata = nameValue.getValueMetadata();
        assertThat(metadata.isEmpty()).isTrue();
        assertThat(metadata.getDefinition()).isNotNull();

        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        ValueMetadataType realMetadataValue = (ValueMetadataType) metadata.createNewValue().asContainerable();
        realMetadataValue.setProvisioning(new ProvisioningMetadataType(prismContext));
        realMetadataValue.getProvisioning().setLastProvisioningTimestamp(now);

        then();
        @NotNull ValueMetadata metadataAfter = nameValue.getValueMetadata();
        assertThat(metadataAfter.isEmpty()).isFalse();
        assertThat(metadataAfter.getDefinition()).isNotNull();

        ValueMetadataType realMetadataValueAfter = (ValueMetadataType) metadataAfter.getRealValue();
        assertThat(realMetadataValueAfter.getProvisioning().getLastProvisioningTimestamp()).isEqualTo(now);
    }

    @Test
    public void test020ParsingMetadata() throws Exception {
        given();

        when();

        then();
        assertUserAfter(USER_ALICE.oid)
                .valueMetadata(ItemPath.EMPTY_PATH)
                    .assertHasDefinition()
                    .singleValue()
                        .containerSingle(ValueMetadataType.F_PROCESS)
                            .assertItemsExactly(ProcessMetadataType.F_REQUEST_TIMESTAMP)
                            .end()
                        .end()
                    .end()
                .valueMetadata(UserType.F_NAME)
                    .assertHasDefinition()
                    .singleValue()
//                        .containerSingle(ValueMetadataType.F_TRANSFORMATION)
//                            .container(TransformationMetadataType.F_SOURCE)
//                                .assertSize(1)
//                                .singleValue()
//                                    .assertPropertyEquals(ValueSourceType.F_KIND, "http://midpoint.evolveum.com/data-provenance/source#resource")
//                                    .end()
//                                .end()
//                            .end()
//                        .assertPropertyValuesEqual(LOA_PATH, 10)
//                        .end()
                    .end()
                .end()
                .valueMetadata(UserType.F_GIVEN_NAME)
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, 7)
                        .end()
                    .end()
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, 7)
                        .end()
                    .end()
                .valueMetadata(UserType.F_FULL_NAME)
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, 5)
                        .end()
                    .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, ppv -> "Development".equals(PolyString.getOrig((PolyString) ppv.getRealValue())))
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, 7)
                        .end()
                    .end()
                .valueMetadata(ItemPath.create(UserType.F_ASSIGNMENT, 111L))
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, 5)
                        .end()
                    .end()
                .valueMetadata(ItemPath.create(UserType.F_ASSIGNMENT, 111L, AssignmentType.F_SUBTYPE), ppv -> "manual".equals(ppv.getRealValue()))
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .end()
                    .end()
                .valueMetadata(ItemPath.create(UserType.F_ASSIGNMENT, 111L, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS))
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .end()
                    .end();
    }

    /**
     * Let us add a user with some metadata for givenName (loa=low) and familyName (loa=high).
     * Metadata for fullName should be computed (loa=low).
     */
    @Test
    public void test050SimpleMetadataMappingUserAdd() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        addObject(USER_BOB, task, result);

        then();
        assertUserAfter(USER_BOB.oid)
                .display()
                .displayXml()
                .valueMetadata(UserType.F_GIVEN_NAME)
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, 1)
                        .end()
                    .end()
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, 3)
                        .end()
                    .end()
                .assertFullName("Bob Green")
                .valueMetadata(UserType.F_FULL_NAME)
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, 1);
    }

    /**
     * Now we change givenName: value stays the same but LoA is increased to normal.
     * Resulting LoA of fullName should be also normal.
     */
    @Test
    public void test055SimpleMetadataMappingUserModify() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismPropertyValue<PolyString> bob = prismContext.itemFactory().createPropertyValue();
        bob.setValue(PolyString.fromOrig("Bob"));
        bob.getValueMetadata().createNewValue().findOrCreateProperty(LOA_PATH).setRealValue(2);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(bob)
                .asObjectDelta(USER_BOB.oid);

        when();
        executeChanges(delta, null, task, result);

        then();
        assertUserAfter(USER_BOB.oid)
                .display()
                .displayXml()
                .valueMetadata(UserType.F_GIVEN_NAME)
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, 2)
                        .end()
                    .end()
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, 3)
                        .end()
                    .end()
                .assertFullName("Bob Green")
                .valueMetadata(UserType.F_FULL_NAME)
                    .assertHasDefinition()
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, 2);
    }

    /**
     * Now we change givenName to a different value (with different LoA of high).
     * Resulting LoA of fullName should be now high.
     *
     * The difference in this case is also that we use ADD instead of REPLACE.
     * (DELETE of old value is automatically added by conversion to delta set triple.)
     */
    @Test
    public void test057SimpleMetadataMappingUserModifyDifferentValue() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismPropertyValue<PolyString> robert = prismContext.itemFactory().createPropertyValue();
        robert.setValue(PolyString.fromOrig("Robert"));
        robert.getValueMetadata().createNewValue().findOrCreateProperty(LOA_PATH).setRealValue(3);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).add(robert)
                .asObjectDelta(USER_BOB.oid);

        when();
        executeChanges(delta, null, task, result);

        then();
        assertUserAfter(USER_BOB.oid)
                .displayXml()
                .valueMetadata(UserType.F_GIVEN_NAME)
                    .singleValue()
                        .assertPropertyValuesEqual(LOA_PATH, 3)
                        .end()
                    .end()
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .singleValue()
                        .assertPropertyValuesEqual(LOA_PATH, 3)
                        .end()
                    .end()
                .assertFullName("Robert Green")
                .valueMetadata(UserType.F_FULL_NAME)
                    .singleValue()
                        .assertPropertyValuesEqual(LOA_PATH, 3);
    }

    /**
     * Here we simply recompute a user and observe if the metadata (LoA) is correctly computed on fullName.
     */
    @Test
    public void test070SimpleMetadataMappingPreview() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        repoAdd(USER_CHUCK, result); // Full name is not present because this is raw addition.
        ModelContext<UserType> modelContext =
                previewChanges(prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, USER_CHUCK.oid),
                        ModelExecuteOptions.create()
                                .reconcile(),
                        task, result);

        then();
        PrismObject<UserType> userAfter = modelContext.getFocusContext().getObjectNew();
        assertUser(userAfter, "after")
                .displayXml()
                .assertFullName("Chuck White")
                .valueMetadata(UserType.F_FULL_NAME)
                    .assertHasDefinition()
                    .singleValue()
                        .assertPropertyValuesEqual(LOA_PATH, 1)
                        .end();
    }
    //endregion

    //region Scenario 0: Creation metadata recording

    /**
     * Adding user Paul. We store creation metadata for fulLName.
     * This test checks that the metadata is stored correctly and is not changed on recomputation.
     */
    @Test
    public void test080AddPaul() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        XMLGregorianCalendar before = XmlTypeConverter.createXMLGregorianCalendar();

        when();
        addObject(USER_PAUL, task, result);

        then();
        XMLGregorianCalendar after = XmlTypeConverter.createXMLGregorianCalendar();

        PrismContainerValue<ValueMetadataType> fullNameMetadata1 = assertUserAfter(USER_PAUL.oid)
                .display()
                .displayXml()
                .assertFullName("Paul Morphy")
                .assertDescription("Paul")
                .valueMetadata(UserType.F_DESCRIPTION)
                    .assertSize(0)
                    .end()
                .valueMetadata(UserType.F_FULL_NAME)
                    .singleValue()
                        .display()
                        .assertSize(1)
                        .getPrismValue();

        when("recompute");
        recomputeUser(USER_PAUL.oid, task, result);

        then("after recompute");
        PrismContainerValue<ValueMetadataType> fullNameMetadata2 = assertUserAfter(USER_PAUL.oid)
                .display()
                .displayXml()
                .assertFullName("Paul Morphy")
                .assertDescription("Paul")
                .valueMetadata(UserType.F_DESCRIPTION)
                    .assertSize(0)
                    .end()
                .valueMetadata(UserType.F_FULL_NAME)
                    .singleValue()
                        .display()
                        .assertSize(1)
                        .getPrismValue();

        displayDumpable("full name metadata after add", fullNameMetadata1);
        displayDumpable("full name metadata after recompute", fullNameMetadata2);

        XMLGregorianCalendar createTimestamp1 = fullNameMetadata1.asContainerable().getStorage().getCreateTimestamp();
        XMLGregorianCalendar createTimestamp2 = fullNameMetadata2.asContainerable().getStorage().getCreateTimestamp();

        assertThat(createTimestamp1).isNotNull();
        assertThat(createTimestamp2).isNotNull();
        assertThat(createTimestamp2)
                .as("create time after recompute")
                .isEqualTo(createTimestamp1);

        assertBetween("Create timestamp is out of expected range", before, after, createTimestamp1);
    }
    //endregion

    //region Scenario 1: Sensitivity propagation
    @Test
    public void test100AddJim() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        addObject(USER_JIM, task, result);

        then();
        assertUserAfter(USER_JIM.oid)
                .display()
                .displayXml()
                .assignments()
                    .forOrg(ORG_SPECIAL_MEDICAL_SERVICES.oid)
                    .valueMetadataSingle()
                        .display()
                        .assertPropertyValuesEqual(SENSITIVITY_PATH, "high")
                        .end()
                    .end()
                    .forOrg(ORG_EMPLOYEES.oid)
                    .valueMetadataSingle()
                        .display()
                        .assertPropertyValuesEqual(SENSITIVITY_PATH, "low")
                        .end()
                    .end();
        // TODO for roleMembershipRef (when implemented)
    }
    //endregion

    //region Scenario 9: Provenance metadata

    /**
     * Leonhard Euler (imported without any information) gets both given name and family name
     * with provenance of "admin entry".
     *
     * Delta:
     *  - add Leonhard (admin)
     *  - add Euler (admin)
     *
     * After:
     *  - Leonhard (admin)
     *  - Euler (admin)
     *  - Leonhard Euler (m:admin)
     */
    @Test
    public void test200ProvideNamesByAdmin() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        // @formatter:off
        PrismPropertyValue<PolyString> leonhard = prismContext.itemFactory().createPropertyValue();
        leonhard.setValue(PolyString.fromOrig("Leonhard"));
        leonhard.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginAcquisition()
                                .originRef(ORIGIN_ADMIN_ENTRY.ref())
                                .channel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .timestamp(now)
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        // @formatter:on

        // @formatter:off
        PrismPropertyValue<PolyString> euler = prismContext.itemFactory().createPropertyValue();
        euler.setValue(PolyString.fromOrig("Euler"));
        euler.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginAcquisition()
                                .originRef(ORIGIN_ADMIN_ENTRY.ref())
                                .channel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .timestamp(now)
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        // @formatter:on

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).add(leonhard)
                .item(UserType.F_FAMILY_NAME).add(euler)
                .asObjectDelta(USER_LEONHARD.oid);

        when();
        executeChanges(delta, null, task, result);

        then();

        // @formatter:off
        assertUserAfter(USER_LEONHARD.oid)
                .displayXml()
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            .assertTimestampBetween(now, now)
                        .end()
                    .end()
                .end()

                .valueMetadataSingle(UserType.F_FAMILY_NAME)
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            .assertTimestampBetween(now, now)
                        .end()
                    .end()
                .end()

                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .provenance()
                        .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            .assertTimestampBetween(now, now)
                        .end()
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * Now we add the same family name of Euler but with different origin/channel.
     *
     * Before:
     *  - Leonhard (admin)
     *  - Euler (admin)
     *  - Leonhard Euler (m:admin)
     *
     * Delta:
     *  - add Euler (self)
     *
     * After:
     *  - Leonhard (admin)
     *  - Euler (admin, self)
     *  - Leonhard Euler (m:admin+self)
     */
    @Test
    public void test210AddSameFamilyNameByRest() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar before = XmlTypeConverter.createXMLGregorianCalendar();
        Thread.sleep(10);
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        // @formatter:off
        PrismPropertyValue<PolyString> euler = prismContext.itemFactory().createPropertyValue();
        euler.setValue(PolyString.fromOrig("Euler"));
        euler.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginAcquisition()
                                .originRef(ORIGIN_SELF_SERVICE_APP.ref())
                                .channel(SchemaConstants.CHANNEL_REST_LOCAL)
                                .timestamp(now)
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        // @formatter:on

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).add(euler)
                .asObjectDelta(USER_LEONHARD.oid);

        when();
        executeChanges(delta, null, task, result);

        then();
        // @formatter:off
        assertUserAfter(USER_LEONHARD.oid)
                .displayXml()
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                .end()

                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_SELF_SERVICE_APP.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_SELF_SERVICE_APP.ref())
                                .assertChannel(SchemaConstants.CHANNEL_REST_LOCAL)
                                .assertTimestampBetween(now, now)
                            .end()
                        .end()
                    .end()
                .end()

                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .provenance()
                        .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                        .assertAcquisitions(2)
                        .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                            .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            .assertTimestampBefore(before)
                        .end()
                        .singleAcquisition(ORIGIN_SELF_SERVICE_APP.oid)
                            .assertOriginRef(ORIGIN_SELF_SERVICE_APP.ref())
                            .assertChannel(SchemaConstants.CHANNEL_REST_LOCAL)
                            .assertTimestampBetween(now, now)
                        .end()
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * Adding fullName (the same) manually by HR (simulated).
     *
     * Before:
     *  - Leonhard (admin)
     *  - Euler (admin, self)
     *  - Leonhard Euler (m:admin+self)
     *
     * Delta:
     *  - add Leonhard Euler (hr)
     *
     * After:
     *  - Leonhard (admin)
     *  - Euler (admin, self)
     *  - Leonhard Euler (m:admin+self, hr)
     */
    @Test
    public void test220ReinforceFullNameByHr() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar before = XmlTypeConverter.createXMLGregorianCalendar();
        Thread.sleep(10);
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        // @formatter:off
        PrismPropertyValue<PolyString> leonhardEuler = prismContext.itemFactory().createPropertyValue();
        leonhardEuler.setValue(PolyString.fromOrig("Leonhard Euler"));
        leonhardEuler.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginAcquisition()
                                .originRef(ORIGIN_HR_FEED.ref())
                                .timestamp(now)
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        // @formatter:on

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME).add(leonhardEuler)
                .asObjectDelta(USER_LEONHARD.oid);

        when();
        executeChanges(delta, null, task, result);

        then();
        // @formatter:off
        assertUserAfter(USER_LEONHARD.oid)
                .displayXml()
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                .end()

                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_SELF_SERVICE_APP.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_SELF_SERVICE_APP.ref())
                                .assertChannel(SchemaConstants.CHANNEL_REST_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                .end()

                .valueMetadata(UserType.F_FULL_NAME)
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                            .assertAcquisitions(2)
                            .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                            .singleAcquisition(ORIGIN_SELF_SERVICE_APP.oid)
                                .assertOriginRef(ORIGIN_SELF_SERVICE_APP.ref())
                                .assertChannel(SchemaConstants.CHANNEL_REST_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .assertNoMappingSpec()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_HR_FEED.ref())
                                .assertChannel(null)
                                .assertTimestampBetween(now, now);
        // @formatter:on
    }

    /**
     * Adding alias of Leonhard Euler manually by self-service app.
     *
     * Before:
     *  - alias = Leonhard Euler (m:admin+self)
     *
     * Delta:
     *  - add Leonhard Euler (self)
     *
     * After:
     *  - alias = Leonhard Euler (m:admin+self, self)
     */
    @Test
    public void test225ReinforceAliasBySelf() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar before = XmlTypeConverter.createXMLGregorianCalendar();
        Thread.sleep(10);
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        // @formatter:off
        PrismPropertyValue<PolyString> leonhardEuler = prismContext.itemFactory().createPropertyValue();
        leonhardEuler.setValue(PolyString.fromOrig("Leonhard Euler"));
        leonhardEuler.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginAcquisition()
                                .originRef(ORIGIN_SELF_SERVICE_APP.ref())
                                .channel(SchemaConstants.CHANNEL_REST_LOCAL)
                                .timestamp(now)
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        // @formatter:on

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(PATH_ALIAS).add(leonhardEuler)
                .asObjectDelta(USER_LEONHARD.oid);

        when();
        executeChanges(delta, null, task, result);

        then();
        // @formatter:off
        assertUserAfter(USER_LEONHARD.oid)
                .displayXml()
                .valueMetadata(PATH_ALIAS)
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                            .assertAcquisitions(3)
                            .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                            .singleAcquisition(ORIGIN_SELF_SERVICE_APP.oid)
                                .assertOriginRef(ORIGIN_SELF_SERVICE_APP.ref())
                                .assertChannel(SchemaConstants.CHANNEL_REST_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                            .singleAcquisition(ORIGIN_HR_FEED.oid)
                                .assertOriginRef(ORIGIN_HR_FEED.ref())
                                .assertChannel(null)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                    .value(pcv -> pcv.asContainerable().getProvenance().getMappingSpecification() == null)
                        .provenance()
                            .singleAcquisition()
                                .assertChannel(SchemaConstants.CHANNEL_REST_LOCAL)
                                .assertTimestampBetween(now, now);
        // @formatter:on
    }

    /**
     * Delete family name REST/self yield. Full name provenance should be updated accordingly.
     *
     * Before:
     *  - Leonhard (admin)
     *  - Euler (admin, self)
     *  - Leonhard Euler (m:admin+self, hr)
     *
     * Delta:
     *  - delete Euler (self)
     *
     * After:
     *  - Leonhard (admin)
     *  - Euler (admin)
     *  - Leonhard Euler (m:admin, hr)
     */
    @Test
    public void test230DeleteFamilyNameRestYield() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar before = XmlTypeConverter.createXMLGregorianCalendar();
        Thread.sleep(10);

        // @formatter:off
        PrismPropertyValue<PolyString> euler = prismContext.itemFactory().createPropertyValue();
        euler.setValue(PolyString.fromOrig("Euler"));
        euler.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginAcquisition()
                                .originRef(ORIGIN_SELF_SERVICE_APP.ref())
                                // intentionally not specifying non-key items
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        // @formatter:on

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).delete(euler)
                .asObjectDelta(USER_LEONHARD.oid);

        when();

        executeChanges(delta, null, task, result);

        then();
        // @formatter:off
        assertUserAfter(USER_LEONHARD.oid)
                .displayXml()
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                .end()

                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertSize(1)
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                .end()

                .valueMetadata(UserType.F_FULL_NAME)
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                            .assertAcquisitions(1)
                            .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .assertNoMappingSpec()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_HR_FEED.ref())
                                .assertChannel(null)
                                .assertTimestampBefore(before);
        // @formatter:on
    }

    /**
     * Changes given name from Leonhard to Leo. All fullName metadata will be gone with the old value.
     *
     * Before:
     *  - Leonhard (admin)
     *  - Euler (admin)
     *  - Leonhard Euler (m:admin, hr)
     *  - alias: Leonhard Euler (m:admin+hr, self) TODO
     *
     * Delta:
     *  - replace givenName: Leo (self)
     *
     * After:
     *  - givenName: Leo (self)
     *  - familyName: Euler (admin)
     *  - fullName: Leo Euler (m:admin+self)
     *  - alias:
     *     - Leo Euler (m:admin+self)
     *     - Leonhard Euler (self)
     */
    @Test
    public void test240ChangeGivenName() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar before = XmlTypeConverter.createXMLGregorianCalendar();
        Thread.sleep(10);
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        // @formatter:off
        PrismPropertyValue<PolyString> leo = prismContext.itemFactory().createPropertyValue();
        leo.setValue(PolyString.fromOrig("Leo"));
        leo.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginAcquisition()
                                .originRef(ORIGIN_SELF_SERVICE_APP.ref())
                                .channel(SchemaConstants.CHANNEL_REST_LOCAL)
                                .timestamp(now)
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        // @formatter:on

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(leo)
                .asObjectDelta(USER_LEONHARD.oid);

        when();

        executeChanges(delta, null, task, result);

        then();
        // @formatter:off
        assertUserAfter(USER_LEONHARD.oid)
                .displayXml()
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_SELF_SERVICE_APP.ref())
                            .assertChannel(SchemaConstants.CHANNEL_REST_LOCAL)
                            .assertTimestampBetween(now, now)
                        .end()
                    .end()
                .end()

                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertSize(1)
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                .end()

                .valueMetadata(UserType.F_FULL_NAME)
                    .assertSize(1)
                    .singleValue()
                        .provenance()
                            .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                            .assertAcquisitions(2)
                            .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                            .singleAcquisition(ORIGIN_SELF_SERVICE_APP.oid)
                                .assertOriginRef(ORIGIN_SELF_SERVICE_APP.ref())
                                .assertChannel(SchemaConstants.CHANNEL_REST_LOCAL)
                                .assertTimestampBetween(now, now)
                            .end()
                        .end()
                    .end()
                .end()

                .valueMetadataSingle(PATH_ALIAS, ValueSelector.origEquals("Leonhard Euler"))
                    .provenance()
                        .assertNoMappingSpec()
                        .singleAcquisition()
                            .assertChannel(SchemaConstants.CHANNEL_REST_LOCAL)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                .end()
                .valueMetadataSingle(PATH_ALIAS, ValueSelector.origEquals("Leo Euler"))
                    .provenance()
                        .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                        .assertAcquisitions(2)
                            .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                            .singleAcquisition(ORIGIN_SELF_SERVICE_APP.oid)
                                .assertOriginRef(ORIGIN_SELF_SERVICE_APP.ref())
                                .assertChannel(SchemaConstants.CHANNEL_REST_LOCAL)
                                .assertTimestampBetween(now, now)
                            .end()
                        .end()
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * Replace givenName provenance
     *
     * Before:
     *  - givenName: Leo (self)
     *  - familyName: Euler (admin)
     *  - fullName: Leo Euler (m:admin+self)
     *  - alias:
     *     - Leo Euler (m:admin+self)
     *     - Leonhard Euler (self)
     *
     * Delta:
     *  - replace givenName: Leo (admin)
     *
     * After:
     *  - givenName: Leo (admin)
     *  - familyName: Euler (admin)
     *  - fullName: Leo Euler (m:admin)
     *  - alias:
     *     - Leo Euler (m:admin)
     *     - Leonhard Euler (self)
     */
    @Test
    public void test250ReplaceGivenNameProvenance() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar before = XmlTypeConverter.createXMLGregorianCalendar();
        Thread.sleep(10);
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        // @formatter:off
        PrismPropertyValue<PolyString> leoAdmin = prismContext.itemFactory().createPropertyValue();
        leoAdmin.setValue(PolyString.fromOrig("Leo"));
        leoAdmin.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginAcquisition()
                                .originRef(ORIGIN_ADMIN_ENTRY.ref())
                                .channel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .timestamp(now)
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        // @formatter:on

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(leoAdmin)
                .asObjectDelta(USER_LEONHARD.oid);

        when();

        executeChanges(delta, null, task, result);

        then();
        // @formatter:off
        assertUserAfter(USER_LEONHARD.oid)
                .displayXml()
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            .assertTimestamp(now)
                        .end()
                    .end()
                .end()

                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertSize(1)
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                .end()

                .valueMetadata(UserType.F_FULL_NAME)
                    .assertSize(1)
                    .singleValue()
                        .provenance()
                            .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                            .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            .end()
                        .end()
                    .end()
                .end()

                .valueMetadataSingle(PATH_ALIAS, ValueSelector.origEquals("Leonhard Euler"))
                    .provenance()
                        .assertNoMappingSpec()
                        .singleAcquisition()
                            .assertChannel(SchemaConstants.CHANNEL_REST_LOCAL)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                .end()
                .valueMetadataSingle(PATH_ALIAS, ValueSelector.origEquals("Leo Euler"))
                    .provenance()
                        .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                        .end()
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * Blaise is imported from HR anew.
     *
     * After:
     *  - name:       blaise (m:hr/2)
     *  - givenName:  Blaise (m:hr/2)
     *  - familyName: Pascal (m:hr/2)
     *  - fullName:   Blaise Pascal (m:hr/2)
     *  - org:        Department of Hydrostatics (m:hr/2)
     *  - org:        Binomial Club (m:hr/2)
     *
     * Note: /2 means LoA of 2.
     */
    @Test
    public void test300ImportBlaiseFromHr() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // This is necessary because when inbounds start the user has no archetype, therefore no template.
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, TEMPLATE_PROVENANCE_METADATA_RECORDING.oid);

        DummyAccount pascal = RESOURCE_HR.controller.addAccount(USER_BLAISE_NAME);
        pascal.addAttributeValue(ATTR_FIRSTNAME, "Blaise");
        pascal.addAttributeValue(ATTR_LASTNAME, "Pascal");
        pascal.addAttributeValue(ATTR_ORGANIZATION, "Department of Hydrostatics");
        pascal.addAttributeValue(ATTR_ORGANIZATION, "Binomial Club");

        when();
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        rerunTask(TASK_HR_IMPORT.oid, result);
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();

        then();
        assertUserAfterByUsername(USER_BLAISE_NAME)
                .displayXml()
                .assertName("blaise")
                .valueMetadataSingle(UserType.F_NAME)
                    .display()
                    .provenance()
                        .assertMappingSpec(RESOURCE_HR.oid)
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBetween(start, end)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()

                .assertGivenName("Blaise")
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .provenance()
                        .assertMappingSpec(RESOURCE_HR.oid)
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBetween(start, end)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()

                .assertFamilyName("Pascal")
                .valueMetadataSingle(UserType.F_FAMILY_NAME)
                    .provenance()
                        .assertMappingSpec(RESOURCE_HR.oid)
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBetween(start, end)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()

                .assertFullName("Blaise Pascal")
                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .provenance()
                        .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBetween(start, end)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()

                .assertOrganizations("Department of Hydrostatics", "Binomial Club")
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Department of Hydrostatics"))
                    .provenance()
                        .assertMappingSpec(RESOURCE_HR.oid)
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBetween(start, end)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Binomial Club"))
                    .provenance()
                        .assertMappingSpec(RESOURCE_HR.oid)
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBetween(start, end)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()

                .assertCostCenter("CC000")
                .valueMetadata(UserType.F_COST_CENTER)
                    .assertSize(0) // there should be none, because the processing of MD for costCenter is disabled
                .end()

                .assertLocality("Bratislava")
                .valueMetadataSingle(UserType.F_LOCALITY)
                    .assertInternalOrigin()
                    .provenance()
                        .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                    .end()
                    .assertNoItem(LOA_PATH) // LoA handling is turned off for "locality" mapping
                .end()

                .valueMetadataSingle(UserType.F_TITLE)
                    .assertInternalOrigin() // generated
                .end()

                .valueMetadataSingle(UserType.F_ASSIGNMENT) // there should be only a single assignment
                    .display()
                    .provenance()
                        .assertMappingSpec(RESOURCE_HR.oid)
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBetween(start, end)
                        .end()
                    .end()
                    .assertNoItem(LOA_PATH) // LoA handling is turned off for assignments
                .end();
    }

    /**
     * Before:
     *  - name:       blaise (m:hr/2)
     *  - givenName:  Blaise (m:hr/2)
     *  - familyName: Pascal (m:hr/2)
     *  - fullName:   Blaise Pascal (m:hr/2)
     *  - org:        Department of Hydrostatics (m:hr/2)
     *  - org:        Binomial Club (m:hr/2)
     *
     * Delta:
     *  - add familyName: Pascal (admin/5)
     *
     * After:
     *  - name:       blaise (hr/2)
     *  - givenName:  Blaise (hr/2)
     *  - familyName: Pascal (hr/2, admin/5)
     *  - fullName:   Blaise Pascal (m:hr+admin/2)
     *  - org:        Department of Hydrostatics (hr/2)
     *  - org:        Binomial Club (hr/2)
     */
    @Test
    public void test310ReinforceFamilyNameByManualEntry() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar before = XmlTypeConverter.createXMLGregorianCalendar();
        Thread.sleep(10);
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        PrismObject<UserType> blaise = findUserByUsername(USER_BLAISE_NAME);

        // @formatter:off
        PrismPropertyValue<PolyString> pascal = prismContext.itemFactory().createPropertyValue();
        pascal.setValue(PolyString.fromOrig("Pascal"));
        pascal.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginAcquisition()
                                .originRef(ORIGIN_ADMIN_ENTRY.ref())
                                .channel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .actorRef(USER_ADMINISTRATOR_OID, UserType.COMPLEX_TYPE)
                                .timestamp(now)
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        setLoA(pascal, 5);
        // @formatter:on

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).add(pascal)
                .asObjectDelta(blaise.getOid());

        when();

        // Reconcile option is to engage inbound processing.
        executeChanges(delta, ModelExecuteOptions.create().reconcile(), task, result);

        then();
        assertUserAfterByUsername(USER_BLAISE_NAME)
                .displayXml()
                .assertName("blaise")
                .valueMetadataSingle(UserType.F_NAME)
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.ref())
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()

                .assertGivenName("Blaise")
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.ref())
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()

                .assertFamilyName("Pascal")
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_HR_FEED.ref())
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 2)
                    .end()
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertActorRef(USER_ADMINISTRATOR_OID)
                                .assertTimestampBetween(now, now)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 5)
                    .end()
                .end()

                .assertFullName("Blaise Pascal")
                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .display()
                    .provenance()
                        .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                        .singleAcquisition(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertTimestampBefore(before)
                        .end()
                        .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                            .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            .assertActorRef(USER_ADMINISTRATOR_OID)
                            .assertTimestampBetween(now, now)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2) // because givenName has LoA of 2
                .end()

                .assertOrganizations("Department of Hydrostatics", "Binomial Club")
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Department of Hydrostatics"))
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Binomial Club"))
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end();
    }

    /**
     * Before:
     *  - name:       blaise (m:hr/2)
     *  - givenName:  Blaise (m:hr/2)
     *  - familyName: Pascal (m:hr/2, admin/5)
     *  - fullName:   Blaise Pascal (m:hr+admin/2)
     *  - org:        Department of Hydrostatics (m:hr/2)
     *  - org:        Binomial Club (m:hr/2)
     *
     * Delta:
     *  - add givenName: Blaise (admin/jim:4)
     *
     * After:
     *  - name:       blaise (m:hr/2)
     *  - givenName:  Blaise (m:hr/2, admin/jim/4)
     *  - familyName: Pascal (m:hr/2, admin/5)
     *  - fullName:   Blaise Pascal (m:hr+admin/4) [not sure about actor/timestamp]
     *  - org:        Department of Hydrostatics (m:hr/2)
     *  - org:        Binomial Club (m:hr/2)
     */
    @Test
    public void test320ReinforceGivenNameByManualEntry() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar before = XmlTypeConverter.createXMLGregorianCalendar();
        Thread.sleep(10);
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        PrismObject<UserType> user = findUserByUsername(USER_BLAISE_NAME);

        // @formatter:off
        PrismPropertyValue<PolyString> blaise = prismContext.itemFactory().createPropertyValue();
        blaise.setValue(PolyString.fromOrig("Blaise"));
        blaise.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginAcquisition()
                                .originRef(ORIGIN_ADMIN_ENTRY.ref())
                                .channel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .actorRef(USER_JIM.oid, UserType.COMPLEX_TYPE)
                                .timestamp(now)
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        setLoA(blaise, 4);
        // @formatter:on

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).add(blaise)
                .asObjectDelta(user.getOid());

        when();
        executeChanges(delta, null, task, result);

        then();
        assertUserAfterByUsername(USER_BLAISE_NAME)
                .displayXml()
                .assertName("blaise")
                .valueMetadataSingle(UserType.F_NAME)
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.ref())
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()

                .assertGivenName("Blaise")
                .valueMetadata(UserType.F_GIVEN_NAME)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_HR_FEED.ref())
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 2)
                    .end()
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertActorRef(USER_JIM.oid)
                                .assertTimestampBetween(now, now)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 4)
                    .end()
                .end()

                .assertFamilyName("Pascal")
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_HR_FEED.ref())
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 2)
                    .end()
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertActorRef(USER_ADMINISTRATOR_OID)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 5)
                    .end()
                .end()

                .assertFullName("Blaise Pascal")
                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .display()
                    .provenance()
                        .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                        .singleAcquisition(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertTimestampBefore(before)
                        .end()
                        .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                            .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            // not sure about actor (administrator vs. jim) nor timestamp
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 4) // Blaise: 4, Pascal: 5
                .end()

                .assertOrganizations("Department of Hydrostatics", "Binomial Club")
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Department of Hydrostatics"))
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Binomial Club"))
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end();
    }

    /**
     * Now importing Blaise from CRM. Values are not conflicting. One organization is added.
     *
     * After:
     *  - name:       blaise (m:hr/2)
     *  - givenName:  Blaise (m:hr/2, admin/jim/4)
     *  - familyName: Pascal (m:hr/2, admin/5)
     *  - fullName:   Blaise Pascal (m:hr+admin/4)
     *  - org:        Department of Hydrostatics (m:hr/2)
     *  - org:        Binomial Club (m:hr/2)
     *
     * After:
     *  - name:       blaise (m:hr/2, m:crm/3)
     *  - givenName:  Blaise (m:hr/2, admin/jim/4, m:crm/3)
     *  - familyName: Pascal (m:hr/2, admin/5, m:crm/3)
     *  - fullName:   Blaise Pascal (m:hr+admin+crm/4)
     *  - org:        Department of Hydrostatics (m:hr/2, m:crm/3)
     *  - org:        Binomial Club (m:hr/2)
     *  - org:        Gases (m:crm/3)
     */
    @Test
    public void test330ImportBlaiseFromCrm() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount crmAccount = RESOURCE_CRM.controller.addAccount(USER_BLAISE_NAME);
        crmAccount.addAttributeValue(ATTR_FIRSTNAME, "Blaise");
        crmAccount.addAttributeValue(ATTR_LASTNAME, "Pascal");
        crmAccount.addAttributeValue(ATTR_ORGANIZATION, "Department of Hydrostatics");
        crmAccount.addAttributeValue(ATTR_ORGANIZATION, "Gases");
        crmAccount.addAttributeValue(ATTR_LOA, 3);

        when();
        XMLGregorianCalendar before = clock.currentTimeXMLGregorianCalendar();
        rerunTask(TASK_CRM_IMPORT.oid, result);
        XMLGregorianCalendar after = clock.currentTimeXMLGregorianCalendar();

        then();
        assertUserAfterByUsername(USER_BLAISE_NAME)
                .displayXml()
                .assertName("blaise")
                .valueMetadata(UserType.F_NAME)
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 2)
                    .end()
                    .valueForOrigin(ORIGIN_CRM_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_CRM.oid)
                                .assertTimestampBetween(before, after)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 3)
                    .end()
                .end()

                .assertGivenName("Blaise")
                .valueMetadata(UserType.F_GIVEN_NAME)
                    .assertSize(3)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_HR_FEED.ref())
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 2)
                    .end()
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertActorRef(USER_JIM.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 4)
                    .end()
                    .valueForOrigin(ORIGIN_CRM_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_CRM.oid)
                                .assertTimestampBetween(before, after)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 3)
                    .end()
                .end()

                .assertFamilyName("Pascal")
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertSize(3)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_HR_FEED.ref())
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 2)
                    .end()
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertActorRef(USER_ADMINISTRATOR_OID)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 5)
                    .end()
                    .valueForOrigin(ORIGIN_CRM_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_CRM.oid)
                                .assertTimestampBetween(before, after)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 3)
                    .end()
                .end()

                .assertFullName("Blaise Pascal")
                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .provenance()
                        .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                        .assertAcquisitions(3)
                        .singleAcquisition(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                        .singleAcquisition(ORIGIN_CRM_FEED.oid)
                            .assertResourceRef(RESOURCE_CRM.oid)
                            .assertTimestampBetween(before, after)
                        .end()
                        .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            // not sure about actor (administrator vs. jim) nor timestamp
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 4)
                .end()

                .assertOrganizations("Department of Hydrostatics", "Binomial Club", "Gases")
                .valueMetadata(UserType.F_ORGANIZATION, ValueSelector.origEquals("Department of Hydrostatics"))
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_HR_FEED.oid)
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 2)
                    .end()
                    .valueForOrigin(ORIGIN_CRM_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_CRM_FEED.oid)
                                .assertResourceRef(RESOURCE_CRM.oid)
                                .assertTimestampBetween(before, after)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 3)
                    .end()
                .end()
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Binomial Club"))
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 2)
                .end()
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Gases"))
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_CRM_FEED.oid)
                            .assertResourceRef(RESOURCE_CRM.oid)
                            .assertTimestampBetween(before, after)
                        .end()
                    .end()
                    .assertPropertyValuesEqual(LOA_PATH, 3)
                .end();
    }

    /**
     * Deleting Blaise from Gases (in CRM).
     *
     * Before:
     *  - name:       blaise (hr, crm)
     *  - givenName:  Blaise (hr, admin/jim, crm)
     *  - familyName: Pascal (hr, admin, crm)
     *  - fullName:   Blaise Pascal (m:hr+admin+crm)
     *  - org:        Department of Hydrostatics (hr, crm)
     *  - org:        Binomial Club (hr)
     *  - org:        Gases (crm)
     *
     * Delta: org should lose Gases value.
     *
     * After:
     *  - name:       blaise (hr, crm)
     *  - givenName:  Blaise (hr, admin/jim, crm)
     *  - familyName: Pascal (hr, admin, crm)
     *  - fullName:   Blaise Pascal (m:hr+admin+crm)
     *  - org:        Department of Hydrostatics (hr, crm)
     *  - org:        Binomial Club (hr)
     */
    @Test
    public void test340DeleteBlaiseFromGases() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount crmAccount = Objects.requireNonNull(
                RESOURCE_CRM.controller.getDummyResource().getAccountByUsername(USER_BLAISE_NAME));
        crmAccount.removeAttributeValue(ATTR_ORGANIZATION, "Gases");

        when();
        XMLGregorianCalendar before = clock.currentTimeXMLGregorianCalendar();
        rerunTask(TASK_CRM_IMPORT.oid, result);

        then();
        assertUserAfterByUsername(USER_BLAISE_NAME)
                .displayXml()
                .assertName("blaise")
                .valueMetadata(UserType.F_NAME)
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_CRM_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_CRM.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                .end()

                .assertGivenName("Blaise")
                .valueMetadata(UserType.F_GIVEN_NAME)
                    .assertSize(3)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_HR_FEED.ref())
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertActorRef(USER_JIM.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_CRM_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_CRM.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                .end()

                .assertFamilyName("Pascal")
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .assertSize(3)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_HR_FEED.ref())
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_ADMIN_ENTRY.ref())
                                .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .assertActorRef(USER_ADMINISTRATOR_OID)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_CRM_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_CRM.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                .end()

                .assertFullName("Blaise Pascal")
                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .provenance()
                        .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                        .assertAcquisitions(3)
                        .singleAcquisition(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                        .singleAcquisition(ORIGIN_CRM_FEED.oid)
                            .assertResourceRef(RESOURCE_CRM.oid)
                            .assertTimestampBefore(before)
                        .end()
                        .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                            .assertChannel(SchemaConstants.CHANNEL_USER_LOCAL)
                            // not sure about actor (administrator vs. jim) nor timestamp
                        .end()
                    .end()
                .end()

                .assertOrganizations("Department of Hydrostatics", "Binomial Club")
                .valueMetadata(UserType.F_ORGANIZATION, ValueSelector.origEquals("Department of Hydrostatics"))
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_HR_FEED.oid)
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_CRM_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertOriginRef(ORIGIN_CRM_FEED.oid)
                                .assertResourceRef(RESOURCE_CRM.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                    .end()
                .end()
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Binomial Club"))
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                .end();
    }

    /**
     * Before:
     *  - name:       blaise (hr, crm)
     *  - givenName:  Blaise (hr, admin/jim, crm)
     *  - familyName: Pascal (hr, admin, crm)
     *  - fullName:   Blaise Pascal (m:hr+admin+crm)
     *  - org:        Department of Hydrostatics (hr, crm)
     *  - org:        Binomial Club (hr)
     *  - org:        Gases (crm)
     *
     * Delta: org should lose Department of Hydrostatics value (via HR).
     *
     * After:
     *  - name:       blaise (hr/2, crm/3)
     *  - givenName:  Blaise (hr/2, admin/jim/4, crm/3)
     *  - familyName: Pascal (hr/2, admin/5, crm/3)
     *  - fullName:   Blaise Pascal (m:hr+admin+crm/4)
     *  - org:        Department of Hydrostatics (crm/3) <---- We should keep the CRM yield of this value.
     *  - org:        Binomial Club (hr/2)
     */
    @Test
    public void test350DeleteBlaiseFromHydrostatics() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount hrAccount = Objects.requireNonNull(
                RESOURCE_HR.controller.getDummyResource().getAccountByUsername(USER_BLAISE_NAME));
        hrAccount.removeAttributeValue(ATTR_ORGANIZATION, "Department of Hydrostatics");

        when();
        XMLGregorianCalendar before = clock.currentTimeXMLGregorianCalendar();
        rerunTask(TASK_HR_IMPORT.oid, result);

        then();
        assertUserAfterByUsername(USER_BLAISE_NAME)
                .displayXml()

                // skip all those lengthy asserts
                .assertOrganizations("Department of Hydrostatics", "Binomial Club")
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Department of Hydrostatics"))
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_CRM_FEED.oid)
                            .assertResourceRef(RESOURCE_CRM.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                .end()
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Binomial Club"))
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_HR_FEED.oid)
                            .assertResourceRef(RESOURCE_HR.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                .end()

                .assertValues(PATH_ASSURED_ORGANIZATION, poly("Department of Hydrostatics"));
    }

    /**
     * Before:
     *  - name:       blaise (hr/2, crm/3)
     *  - givenName:  Blaise (hr/2, admin/jim/4, crm/3)
     *  - familyName: Pascal (hr/2, admin/5, crm/3)
     *  - fullName:   Blaise Pascal (m:hr+admin+crm/4)
     *  - org:        Department of Hydrostatics (crm/3) <---- We should keep the CRM yield of this value.
     *  - org:        Binomial Club (hr/2)
     *  - assuredOrg: Department of Hydrostatics
     *
     * Delta: admin enters its own value for Binomial Club with loa of 5
     *
     * After:
     *  - name:       blaise (hr/2, crm/3)
     *  - givenName:  Blaise (hr/2, admin/jim/4, crm/3)
     *  - familyName: Pascal (hr/2, admin/5, crm/3)
     *  - fullName:   Blaise Pascal (m:hr+admin+crm/4)
     *  - org:        Department of Hydrostatics (crm/3) <---- We should keep the CRM yield of this value.
     *  - org:        Binomial Club (hr/2)
     *  - assuredOrg: Department of Hydrostatics
     *  - assuredOrg: Binomial Club
     */
    @Test
    public void test360ReinforceBinomialClub() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> blaise = findUserByUsername(USER_BLAISE_NAME);

        XMLGregorianCalendar before = clock.currentTimeXMLGregorianCalendar();
        Thread.sleep(10);
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        PrismPropertyValue<PolyString> binomialClubAdmin = prismContext.itemFactory().createPropertyValue();
        binomialClubAdmin.setValue(PolyString.fromOrig("Binomial Club"));
        binomialClubAdmin.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginAcquisition()
                                .originRef(ORIGIN_ADMIN_ENTRY.ref())
                                .channel(SchemaConstants.CHANNEL_USER_LOCAL)
                                .timestamp(now)
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        setLoA(binomialClubAdmin, 5);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ORGANIZATION).add(binomialClubAdmin)
                .asObjectDelta(blaise.getOid());

        when();
        executeChanges(delta, null, task, result);

        then();
        assertUserAfterByUsername(USER_BLAISE_NAME)
                .displayXml()

                // skip all those lengthy asserts
                .assertOrganizations("Department of Hydrostatics", "Binomial Club")
                .valueMetadataSingle(UserType.F_ORGANIZATION, ValueSelector.origEquals("Department of Hydrostatics"))
                    .provenance()
                        .singleAcquisition()
                            .assertOriginRef(ORIGIN_CRM_FEED.oid)
                            .assertResourceRef(RESOURCE_CRM.oid)
                            .assertTimestampBefore(before)
                        .end()
                    .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATION, ValueSelector.origEquals("Binomial Club"))
                    .assertSize(2)
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertTimestampBefore(before)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 2)
                    .end()
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleAcquisition()
                                .assertTimestamp(now)
                            .end()
                        .end()
                        .assertPropertyValuesEqual(LOA_PATH, 5)
                    .end()
                .end()

                .assertValues(PATH_ASSURED_ORGANIZATION, poly("Department of Hydrostatics"), poly("Binomial Club"));
    }

    @Test
    public void test370ProvenanceSupport() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> blaise = findUserByUsername(USER_BLAISE_NAME);

        when();
        MetadataItemProcessingSpec provenanceSupportSpec = modelInteractionService.getMetadataItemProcessingSpec(
                ValueMetadataType.F_PROVENANCE, blaise, task, result);

        then();
        displayDumpable("provenance support spec", provenanceSupportSpec);

        assertThat(provenanceSupportSpec.isFullProcessing(UserType.F_GIVEN_NAME)).as("giveName provenance support").isTrue();
        assertThat(provenanceSupportSpec.isFullProcessing(UserType.F_FULL_NAME)).as("fullName provenance support").isTrue();
        assertThat(provenanceSupportSpec.isFullProcessing(ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)))
                .as("activation/administrativeStatus provenance support").isTrue();
        assertThat(provenanceSupportSpec.isFullProcessing(UserType.F_ASSIGNMENT)).as("assignment provenance support").isTrue();
        assertThat(provenanceSupportSpec.isFullProcessing(UserType.F_COST_CENTER)).as("costCenter provenance support").isFalse();
    }

    @Test
    public void test390DeleteBlaiseAndReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        RESOURCE_HR.controller.deleteAccount(USER_BLAISE_NAME);

        when();
//        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        rerunTask(TASK_HR_RECONCILIATION.oid, result);
//        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();

        then();
        assertUserAfterByUsername(USER_BLAISE_NAME)
                .displayXml();

        // TODO decide on how this should look like
        //  Currently inbounds are not processed at all, so metadata keeps record on HR acquisition
    }

    //endregion

    private void setLoA(PrismValue value, int loa) throws SchemaException {
        PrismContainer<Containerable> valueMetadata = value.getValueMetadataAsContainer();
        assertThat(valueMetadata.size()).isEqualTo(1);
        valueMetadata.getValue().findOrCreateProperty(LOA_PATH).setRealValue(loa);
    }

    private PolyString poly(String orig) {
        PolyString polyString = PolyString.fromOrig(orig);
        polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
        return polyString;
    }


}
