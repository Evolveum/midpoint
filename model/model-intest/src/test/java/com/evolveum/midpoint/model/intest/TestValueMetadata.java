/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Optional;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.test.DummyTestResource;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Tests the value metadata handling.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestValueMetadata extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/metadata");

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
    private static final TestResource<TaskType> TASK_HR_IMPORT = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "task-hr-import.xml", "da0a48bb-1294-475e-bcd3-51bfb9813885");
    private static final TestResource<TaskType> TASK_HR_RECONCILIATION = new TestResource<>(
            PROVENANCE_METADATA_RECORDING_DIR, "task-hr-reconciliation.xml", "0c1f0434-6409-47b8-b7f5-2f44510385c2");

    private static final String ATTR_FIRSTNAME = "firstname";
    private static final String ATTR_LASTNAME = "lastname";
    private static final DummyTestResource RESOURCE_HR = new DummyTestResource(
            PROVENANCE_METADATA_RECORDING_DIR, "resource-hr.xml", "9a34c3b6-aca5-4f9b-aae4-24f3f2d98ce9", "hr", controller -> {
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_FIRSTNAME, String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                ATTR_LASTNAME, String.class, false, false);
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
    private static final TestResource<ObjectTemplateType> TEMPLATE_REGULAR_USER = new TestResource<>(TEST_DIR, "template-regular-user.xml", "b1005d3d-6ef4-4347-b235-313666824ed8");
    private static final TestResource<UserType> USER_ALICE = new TestResource<>(TEST_DIR, "user-alice.xml", "9fc389be-5b47-4e9d-90b5-33fffd87b3ca");
    private static final TestResource<UserType> USER_BOB = new TestResource<>(TEST_DIR, "user-bob.xml", "cab2344d-06c0-4881-98ee-7075bf5d1309");
    private static final TestResource<UserType> USER_CHUCK = new TestResource<>(TEST_DIR, "user-chuck.xml", "3eb9ca6b-49b8-4602-943a-992d8eb9adad");
    private static final String USER_BLAISE_NAME = "blaise";

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
        addObject(ORIGIN_ADMIN_ENTRY, initTask, initResult);
        addObject(ORIGIN_SELF_SERVICE_APP, initTask, initResult);
        addObject(ORIGIN_HR_FEED, initTask, initResult);
        addObject(TASK_HR_IMPORT, initTask, initResult);
        addObject(TASK_HR_RECONCILIATION, initTask, initResult);
        addObject(USER_LEONHARD, initTask, initResult);

        addObject(TEMPLATE_REGULAR_USER, initTask, initResult);
        addObject(USER_ALICE, initTask, initResult);

//        predefinedTestMethodTracing = PredefinedTestMethodTracing.MODEL_LOGGING;
        setGlobalTracingOverride(createModelLoggingTracingProfile());
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

        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        ValueMetadataType realMetadataValue = (ValueMetadataType) metadata.createNewValue().asContainerable();
        realMetadataValue.setProvisioning(new ProvisioningMetadataType(prismContext));
        realMetadataValue.getProvisioning().setLastProvisioningTimestamp(now);

        then();
        @NotNull ValueMetadata metadataAfter = nameValue.getValueMetadata();
        assertThat(metadataAfter.isEmpty()).isFalse();

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
                    .singleValue()
                        .containerSingle(ValueMetadataType.F_PROCESS)
                            .assertItems(ProcessMetadataType.F_REQUEST_TIMESTAMP)
                            .end()
                        .end()
                    .end()
                .valueMetadata(UserType.F_NAME)
                    .singleValue()
                        .containerSingle(ValueMetadataType.F_TRANSFORMATION)
                            .container(TransformationMetadataType.F_SOURCE)
                                .assertSize(1)
                                .singleValue()
                                    .assertPropertyEquals(ValueSourceType.F_KIND, "http://midpoint.evolveum.com/data-provenance/source#resource")
                                    .end()
                                .end()
                            .end()
                        .end()
                    .end()
                .valueMetadata(UserType.F_GIVEN_NAME)
                    .singleValue()
                        .display()
                        .end()
                    .end()
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .singleValue()
                        .display()
                        .end()
                    .end()
                .valueMetadata(UserType.F_FULL_NAME)
                    .singleValue()
                        .display()
                        .end()
                    .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, ppv -> "Development".equals(PolyString.getOrig((PolyString) ppv.getRealValue())))
                    .singleValue()
                        .display()
                        .end()
                    .end()
                .valueMetadata(ItemPath.create(UserType.F_ASSIGNMENT, 111L))
                    .singleValue()
                        .display()
                        .end()
                    .end()
                .valueMetadata(ItemPath.create(UserType.F_ASSIGNMENT, 111L, AssignmentType.F_SUBTYPE), ppv -> "manual".equals(ppv.getRealValue()))
                    .singleValue()
                        .display()
                        .end()
                    .end()
                .valueMetadata(ItemPath.create(UserType.F_ASSIGNMENT, 111L, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS))
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
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, "low")
                        .end()
                    .end()
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, "high")
                        .end()
                    .end()
                .assertFullName("Bob Green")
                .valueMetadata(UserType.F_FULL_NAME)
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, "low");
    }

    /**
     * Now we change givenName: value stays the same but LoA is increased.
     */
    @Test
    public void test055SimpleMetadataMappingUserModify() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismPropertyValue<PolyString> bob = prismContext.itemFactory().createPropertyValue();
        bob.setValue(PolyString.fromOrig("Bob"));
        bob.getValueMetadata().createNewValue().findOrCreateProperty(LOA_PATH).setRealValue("normal");

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
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, "normal")
                        .end()
                    .end()
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, "high")
                        .end()
                    .end()
                .assertFullName("Bob Green")
                .valueMetadata(UserType.F_FULL_NAME)
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, "normal");
    }

    @Test
    public void test070SimpleMetadataMappingPreview() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        repoAdd(USER_CHUCK, result); // Full name is not present because this is raw addition.
        ModelContext<UserType> modelContext =
                previewChanges(prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, USER_CHUCK.oid),
                        ModelExecuteOptions.create(prismContext)
                                .reconcile(),
                        task, result);

        then();
        PrismObject<UserType> userAfter = modelContext.getFocusContext().getObjectNew();
        assertUser(userAfter, "after")
                .display()
                .displayXml()
                .assertFullName("Chuck White")
                .valueMetadata(UserType.F_FULL_NAME)
                    .singleValue()
                        .display()
                        .assertPropertyValuesEqual(LOA_PATH, "low")
                        .end();
    }
    //endregion

    //region Scenario 0: Creation metadata recording
    @Test
    public void test080AddPaul() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        addObject(USER_PAUL, task, result);

        then();
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

        assertThat(fullNameMetadata2.asContainerable().getStorage().getCreateTimestamp())
                .as("create time after recompute")
                .isEqualTo(fullNameMetadata1.asContainerable().getStorage().getCreateTimestamp());
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
    @Test
    public void test900ProvideNamesByAdmin() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        setGlobalTracingOverride(createModelLoggingTracingProfile());

        // @formatter:off
        PrismPropertyValue<PolyString> leonhard = prismContext.itemFactory().createPropertyValue();
        leonhard.setValue(PolyString.fromOrig("Leonhard"));
        leonhard.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginYield()
                                .beginAcquisition()
                                    .timestamp(now)
                                    .channel(SchemaConstants.CHANNEL_GUI_USER_URI)
                                    .originRef(ORIGIN_ADMIN_ENTRY.ref())
                                .<ProvenanceYieldType>end()
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        // @formatter:on

        // @formatter:off
        PrismPropertyValue<PolyString> euler = prismContext.itemFactory().createPropertyValue();
        euler.setValue(PolyString.fromOrig("Euler"));
        euler.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginYield()
                                .beginAcquisition()
                                    .timestamp(now)
                                    .channel(SchemaConstants.CHANNEL_GUI_USER_URI)
                                    .originRef(ORIGIN_ADMIN_ENTRY.ref())
                                .<ProvenanceYieldType>end()
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
        assertUserAfter(USER_LEONHARD.oid)
                .displayXml()
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .display()
                    .end()

                .valueMetadataSingle(UserType.F_FAMILY_NAME)
                    .display()
                    .end()

                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .display()
                    .end();
    }

    @Test
    public void test910AddSameFamilyNameByRest() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        // @formatter:off
        PrismPropertyValue<PolyString> euler = prismContext.itemFactory().createPropertyValue();
        euler.setValue(PolyString.fromOrig("Euler"));
        euler.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginYield()
                                .beginAcquisition()
                                    .timestamp(now)
                                    .channel(SchemaConstants.CHANNEL_REST_URI)
                                    .originRef(ORIGIN_SELF_SERVICE_APP.ref())
                                .<ProvenanceYieldType>end()
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        // @formatter:on

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).add(euler)
                .asObjectDelta(USER_LEONHARD.oid);

        when();
        executeChanges(delta, null, task, result);

        then();
        assertUserAfter(USER_LEONHARD.oid)
                .displayXml()
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .display()
                    .end()

                .valueMetadata(UserType.F_FAMILY_NAME)
                    .display()
                    .end()

                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .display()
                    .end();
    }

    @Test
    public void test920ImportBlaise() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, TEMPLATE_PROVENANCE_METADATA_RECORDING.oid);

        DummyAccount pascal = RESOURCE_HR.controller.addAccount(USER_BLAISE_NAME);
        pascal.addAttributeValue(ATTR_FIRSTNAME, "Blaise");
        pascal.addAttributeValue(ATTR_LASTNAME, "Pascal");

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
                        .singleYield()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertOriginRef(ORIGIN_HR_FEED.oid)
                                .assertTimestampBetween(start, end)
                            .end()
                        .end()
                    .end()
                .end()

                .assertGivenName("Blaise")
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .display()
                    .provenance()
                        .singleYield()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertOriginRef(ORIGIN_HR_FEED.oid)
                                .assertTimestampBetween(start, end)
                            .end()
                        .end()
                    .end()
                .end()

                .assertFamilyName("Pascal")
                .valueMetadataSingle(UserType.F_FAMILY_NAME)
                    .display()
                    .provenance()
                        .singleYield()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertOriginRef(ORIGIN_HR_FEED.oid)
                                .assertTimestampBetween(start, end)
                            .end()
                        .end()
                    .end()
                .end()

                .assertFullName("Blaise Pascal")
                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .display()
                    .provenance()
                        .singleYield()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertOriginRef(ORIGIN_HR_FEED.oid)
                                .assertTimestampBetween(start, end)
                            .end()
                        .end()
                    .end()
                .end();
    }

    @Test
    public void test930ReinforceFamilyNameByManualEntry() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        PrismObject<UserType> blaise = findUserByUsername(USER_BLAISE_NAME);

        setGlobalTracingOverride(addRepositoryAndSqlLogging(createModelLoggingTracingProfile()));

        // @formatter:off
        PrismPropertyValue<PolyString> pascal = prismContext.itemFactory().createPropertyValue();
        pascal.setValue(PolyString.fromOrig("Pascal"));
        pascal.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginYield()
                                .beginAcquisition()
                                    .timestamp(now)
                                    .channel(SchemaConstants.CHANNEL_GUI_USER_URI)
                                    .originRef(ORIGIN_ADMIN_ENTRY.ref())
                                    .actorRef(USER_ADMINISTRATOR_OID, UserType.COMPLEX_TYPE)
                                .<ProvenanceYieldType>end()
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
        // @formatter:on

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).add(pascal)
                .asObjectDelta(blaise.getOid());

        when();

        // Reconcile option is to engage inbound processing.
        executeChanges(delta, ModelExecuteOptions.create(prismContext).reconcile(), task, result);

        then();
        assertUserAfterByUsername(USER_BLAISE_NAME)
                .displayXml()
                .assertName("blaise")
                .valueMetadataSingle(UserType.F_NAME)
                    .display()
                    .provenance()
                        .singleYield()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertOriginRef(ORIGIN_HR_FEED.oid)
                                .assertTimestampBetween(null, now)
                            .end()
                        .end()
                    .end()
                .end()

                .assertGivenName("Blaise")
                .valueMetadataSingle(UserType.F_GIVEN_NAME)
                    .display()
                    .provenance()
                        .singleYield()
                            .singleAcquisition()
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertOriginRef(ORIGIN_HR_FEED.oid)
                                .assertTimestampBetween(null, now)
                            .end()
                        .end()
                    .end()
                .end()

                .assertFamilyName("Pascal")
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .display()
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleYield()
                                .singleAcquisition()
                                    .assertResourceRef(RESOURCE_HR.oid)
                                    .assertOriginRef(ORIGIN_HR_FEED.oid)
                                    .assertTimestampBetween(null, now)
                                .end()
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleYield()
                                .singleAcquisition()
                                    .assertTimestampBetween(now, now)
                                .end()
                            .end()
                        .end()
                    .end()
                .end()

                .assertFullName("Blaise Pascal")
                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .display()
                    .provenance()
                        .singleYield(ORIGIN_ADMIN_ENTRY.oid)
                            .assertMappingSpec(TEMPLATE_PROVENANCE_METADATA_RECORDING.oid)
                            .singleAcquisition(ORIGIN_HR_FEED.oid)
                                .assertResourceRef(RESOURCE_HR.oid)
                                .assertOriginRef(ORIGIN_HR_FEED.oid)
                                .assertTimestampBetween(null, now)
                            .end()
                            .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
                                .assertTimestampBetween(now, now)
                            .end()
                        .end()
                        //.assertSize(2)
                    .end()
                .end();
    }

    @Test
    public void test940ReinforceGivenNameByManualEntry() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();

        PrismObject<UserType> user = findUserByUsername(USER_BLAISE_NAME);

        // @formatter:off
        PrismPropertyValue<PolyString> blaise = prismContext.itemFactory().createPropertyValue();
        blaise.setValue(PolyString.fromOrig("Blaise"));
        blaise.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginProvenance()
                            .beginYield()
                                .beginAcquisition()
                                    .timestamp(now)
                                    .channel(SchemaConstants.CHANNEL_GUI_USER_URI)
                                    .originRef(ORIGIN_ADMIN_ENTRY.ref())
                                    .actorRef(USER_JIM.oid, UserType.COMPLEX_TYPE)
                                .<ProvenanceYieldType>end()
                            .<ProvenanceMetadataType>end()
                        .<ValueMetadataType>end());
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
                .valueMetadata(UserType.F_NAME)
                    .display()
//                    .provenance()
//                        .singleYield()
//                            .singleAcquisition()
//                                .assertResourceRef(RESOURCE_HR.oid)
//                                .assertOriginRef(ORIGIN_HR_FEED.oid)
//                                .assertTimestampBetween(null, now)
//                            .end()
//                        .end()
//                    .end()
                .end()

                .assertGivenName("Blaise")
                .valueMetadata(UserType.F_GIVEN_NAME)
                    .display()
                    .valueForOrigin(ORIGIN_HR_FEED.oid)
                        .provenance()
                            .singleYield()
                                .singleAcquisition()
                                    .assertResourceRef(RESOURCE_HR.oid)
                                    .assertOriginRef(ORIGIN_HR_FEED.oid)
                                    .assertTimestampBetween(null, now)
                                .end()
                            .end()
                        .end()
                    .end()
                    .valueForOrigin(ORIGIN_ADMIN_ENTRY.oid)
                        .provenance()
                            .singleYield()
                                .singleAcquisition()
                                    .assertTimestampBetween(now, now)
                                .end()
                            .end()
                        .end()
                    .end()
                .end()

                .assertFamilyName("Pascal")
                .valueMetadata(UserType.F_FAMILY_NAME)
                    .display()
//                    .provenance()
//                        .singleYield(ORIGIN_HR_FEED.oid)
//                            .singleAcquisition()
//                                .assertResourceRef(RESOURCE_HR.oid)
//                                .assertOriginRef(ORIGIN_HR_FEED.oid)
//                                .assertTimestampBetween(null, now)
//                            .end()
//                        .end()
//                        .singleYield(ORIGIN_ADMIN_ENTRY.oid)
//                            .singleAcquisition()
//                                .assertTimestampBetween(null, now)
//                            .end()
//                        .end()
//                    .end()
                .end()

                .assertFullName("Blaise Pascal")
                .valueMetadataSingle(UserType.F_FULL_NAME)
                    .display()
//                    .provenance()
//                        .singleYield(ORIGIN_ADMIN_ENTRY.oid)
//                            .singleAcquisition(ORIGIN_HR_FEED.oid)
//                                .assertResourceRef(RESOURCE_HR.oid)
//                                .assertOriginRef(ORIGIN_HR_FEED.oid)
//                                .assertTimestampBetween(null, now)
//                            .end()
//                            .singleAcquisition(ORIGIN_ADMIN_ENTRY.oid)
//                                .assertTimestampBetween(now, now)
//                            .end()
//                        .end()
//                        //.assertSize(2)
//                    .end()
                .end();
    }

    @Test
    public void test950DeleteBlaiseAndReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        RESOURCE_HR.controller.deleteAccount(USER_BLAISE_NAME);

        when();
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        rerunTask(TASK_HR_RECONCILIATION.oid, result);
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();

        then();
        assertUserAfterByUsername(USER_BLAISE_NAME)
                .displayXml();

        // TODO decide on how this should look like
        //  Currently inbounds are not processed at all, so metadata keeps record on HR acquisition
    }

    //endregion
}
