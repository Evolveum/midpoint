/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.async;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

/**
 *  Tests model.notifyChange using manually constructed ResourceObjectShadowChangeDescriptionType objects.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestNotifyChange extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "async/notify-change");

    private static final File RESOURCE_GROUPER_FILE = new File(TEST_DIR, "resource-grouper.xml");
    private static final String RESOURCE_GROUPER_ID = "Grouper";
    private static final String RESOURCE_GROUPER_OID = "bbb9900a-b53d-4453-b60b-908725e3950e";

    private static final String BANDERSON_USERNAME = "banderson";
    private static final String JLEWIS685_USERNAME = "jlewis685";
    private static final String ALUMNI_NAME = "ref:alumni";
    private static final String STAFF_NAME = "ref:staff";

    private static final String GROUPER_USER_INTENT = "subject";
    private static final String GROUPER_GROUP_INTENT = "group";

    private PrismObject<ResourceType> resourceDummyGrouper;

    private static final File SHADOW_BANDERSON_FILE = new File(TEST_DIR, "shadow-banderson.xml");
    private static final File SHADOW_BANDERSON_WITH_GROUPS_FILE = new File(TEST_DIR, "shadow-banderson-with-groups.xml");
    private static final File SHADOW_JLEWIS685_FILE = new File(TEST_DIR, "shadow-jlewis685.xml");
    private static final File SHADOW_ALUMNI_FILE = new File(TEST_DIR, "shadow-alumni.xml");
    private static final File SHADOW_STAFF_FILE = new File(TEST_DIR, "shadow-staff.xml");

    private String lewisShadowOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Resources
        DummyResourceContoller dummyResourceCtlGrouper = DummyResourceContoller.create(RESOURCE_GROUPER_ID, resourceDummyGrouper);
        DummyResource dummyResourceGrouper = dummyResourceCtlGrouper.getDummyResource();
        dummyResourceGrouper.setSyncStyle(DummySyncStyle.SMART);
        dummyResourceGrouper.populateWithDefaultSchema();

        resourceDummyGrouper = importAndGetObjectFromFile(ResourceType.class, RESOURCE_GROUPER_FILE, RESOURCE_GROUPER_OID, initTask, initResult);
        dummyResourceCtlGrouper.setResource(resourceDummyGrouper);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultGrouper = modelService.testResource(RESOURCE_GROUPER_OID, task, task.getResult());
        TestUtil.assertSuccess(testResultGrouper);
    }

    /**
     * MEMBER_ADD event for banderson.
     */
    @Test
    public void test100AddAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        PrismObject<ShadowType> bandersonShadow = prismContext.parseObject(SHADOW_BANDERSON_FILE);
        ResourceObjectShadowChangeDescriptionType change = new ResourceObjectShadowChangeDescriptionType();
        ObjectDelta<ShadowType> addDelta = DeltaFactory.Object.createAddDelta(bandersonShadow);
        change.setObjectDelta(DeltaConvertor.toObjectDeltaType(addDelta));
        change.setChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN

        modelService.notifyChange(change, task, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .displayWithProjections()
                .links()
                    .singleAny()
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(GROUPER_USER_INTENT)
                        .assertResource(RESOURCE_GROUPER_OID);
    }

    /**
     * MEMBER_ADD event for jlewis685.
     */
    @Test
    public void test105AddLewis() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        PrismObject<ShadowType> lewisShadow = prismContext.parseObject(SHADOW_JLEWIS685_FILE);
        ResourceObjectShadowChangeDescriptionType change = new ResourceObjectShadowChangeDescriptionType();
        ObjectDelta<ShadowType> addDelta = DeltaFactory.Object.createAddDelta(lewisShadow);
        change.setObjectDelta(DeltaConvertor.toObjectDeltaType(addDelta));
        change.setChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN

        modelService.notifyChange(change, task, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        lewisShadowOid = assertUserAfterByUsername(JLEWIS685_USERNAME)
                .displayWithProjections()
                .links()
                    .singleAny()
                    .resolveTarget()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(GROUPER_USER_INTENT)
                        .assertResource(RESOURCE_GROUPER_OID)
                        .display()
                    .end()
                    .getOid();
        System.out.println("lewis shadow OID = " + lewisShadowOid);
    }

    /**
     * GROUP_ADD event for ref:alumni.
     */
    @Test
    public void test110AddAlumni() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        PrismObject<ShadowType> alumniShadow = prismContext.parseObject(SHADOW_ALUMNI_FILE);
        ResourceObjectShadowChangeDescriptionType change = new ResourceObjectShadowChangeDescriptionType();
        ObjectDelta<ShadowType> addDelta = DeltaFactory.Object.createAddDelta(alumniShadow);
        change.setObjectDelta(DeltaConvertor.toObjectDeltaType(addDelta));
        change.setChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN

        modelService.notifyChange(change, task, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertOrgByName(ALUMNI_NAME, "after")
                .displayWithProjections()
                .links()
                    .singleAny()
                    .resolveTarget()
                        .assertKind(ShadowKindType.ENTITLEMENT)
                        .assertIntent(GROUPER_GROUP_INTENT)
                        .assertResource(RESOURCE_GROUPER_OID)
                        .display();
    }

    /**
     * GROUP_ADD event for ref:staff.
     */
    @Test
    public void test120AddStaff() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        PrismObject<ShadowType> staffShadow = prismContext.parseObject(SHADOW_STAFF_FILE);
        ResourceObjectShadowChangeDescriptionType change = new ResourceObjectShadowChangeDescriptionType();
        ObjectDelta<ShadowType> addDelta = DeltaFactory.Object.createAddDelta(staffShadow);
        change.setObjectDelta(DeltaConvertor.toObjectDeltaType(addDelta));
        change.setChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN

        modelService.notifyChange(change, task, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertOrgByName(STAFF_NAME, "after")
                .displayWithProjections()
                .links()
                    .singleAny()
                    .resolveTarget()
                        .assertKind(ShadowKindType.ENTITLEMENT)
                        .assertIntent(GROUPER_GROUP_INTENT)
                        .assertResource(RESOURCE_GROUPER_OID)
                        .display();
    }

    /**
     * Adding ref:alumni and ref:staff membership for banderson "the old way" (i.e. by providing full current shadow).
     */
    @Test
    public void test200AddGroupsForAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        PrismObject<ShadowType> bandersonShadow = prismContext.parseObject(SHADOW_BANDERSON_WITH_GROUPS_FILE);
        ResourceObjectShadowChangeDescriptionType change = new ResourceObjectShadowChangeDescriptionType();
        change.setCurrentShadow(bandersonShadow.asObjectable());
        change.setChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN

        modelService.notifyChange(change, task, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .displayWithProjections()
                .assertOrganizationalUnits(ALUMNI_NAME, STAFF_NAME)
                .links()
                    .singleAny()
                        .resolveTarget()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(GROUPER_USER_INTENT)
                            .assertResource(RESOURCE_GROUPER_OID)
                            .display("shadow after");
    }

    /**
     * Adding ref:alumni membership for jlewis685 "the new way" (i.e. by a delta).
     */
    @Test
    public void test210AddGroupsForLewis() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        ResourceSchema schema = ResourceSchemaFactory.getRawSchema(resourceDummyGrouper);
        assert schema != null;
        ResourceAttributeDefinition<?> privilegeDefinition =
                schema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS)
                .findAttributeDefinition(DummyResourceContoller.DUMMY_ENTITLEMENT_PRIVILEGE_NAME);
        ObjectDelta<ShadowType> delta = prismContext.deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, DummyResourceContoller.DUMMY_ENTITLEMENT_PRIVILEGE_NAME), privilegeDefinition)
                .add(ALUMNI_NAME)
                .asObjectDelta(lewisShadowOid);

        ResourceObjectShadowChangeDescriptionType change = new ResourceObjectShadowChangeDescriptionType();
        change.setObjectDelta(DeltaConvertor.toObjectDeltaType(delta));
        change.setOldShadowOid(lewisShadowOid);
        change.setChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN

        modelService.notifyChange(change, task, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(JLEWIS685_USERNAME)
                .displayWithProjections()
                .assertOrganizationalUnits(ALUMNI_NAME)
                .links()
                    .singleAny()
                        .resolveTarget()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(GROUPER_USER_INTENT)
                            .assertResource(RESOURCE_GROUPER_OID)
                            .display("shadow after");
    }

}
