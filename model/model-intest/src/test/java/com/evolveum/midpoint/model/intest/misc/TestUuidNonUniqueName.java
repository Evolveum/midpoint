/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.misc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Test with a resource that has unique primary identifier (ConnId UID), but non-unique secondary
 * identifier (ConnId NAME).
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUuidNonUniqueName extends AbstractMiscTest {

    protected static final File RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_FILE = new File(TEST_DIR, "resource-dummy-uuid-nonunique-name.xml");
    protected static final String RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID = "4027de14-2473-11e9-bd83-5f54b071e14f";
    protected static final String RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_NAME = "uuid-nonunique-name";

    String accountJackSparrowUid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(
                RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_NAME, RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_FILE,
                RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID, initTask, initResult);

        importObjectFromFile(USER_SKELLINGTON_FILE);
    }

    @Test
    public void test010TestResourceConnection() throws Exception {
        // GIVEN
        Task task = getTestTask();

        // WHEN
        when();
        OperationResult result = modelService.testResource(RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID, task);

        // THEN
        then();
        assertSuccess(result);

        assertResourceAfter(RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID)
                .displayXml()
                .assertHasSchema();
    }

    @Test
    public void test020RefinedSchema() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertResourceAfter(RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID)
                .displayXml()
                .assertHasSchema();

        // @formatter:off
        assertRefinedResourceSchema(resource, "after")
            .assertNamespace(MidPointConstants.NS_RI)
            .defaultAccountDefinition()
                .attribute(SchemaConstants.ICFS_UID)
                    .assertIsPrimaryIdentifier()
                    .assertNotSecondaryIdentifier()
                    .end()
                .attribute(SchemaConstants.ICFS_NAME)
                    .assertNotPrimaryIdentifier()
                    .assertNotSecondaryIdentifier()
                    .end();
        // @formatter:on
    }

    @Test
    public void test100AssignAccountToJackSparrow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        accountJackSparrowUid = assertUserAfter(USER_JACK_OID)
            .singleLink()
                .target()
                    .assertName(USER_JACK_GIVEN_NAME)
                    .attributes()
                        .assertValue(SchemaConstants.ICFS_NAME, USER_JACK_GIVEN_NAME)
                        .getValue(SchemaConstants.ICFS_UID);
        // @formatter:on

        assertDummyAccountById(RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_NAME, accountJackSparrowUid)
                .assertName(USER_JACK_GIVEN_NAME)
                .assertId(accountJackSparrowUid)
                .assertFullName(USER_JACK_FULL_NAME);

        assertThat(accountJackSparrowUid).withFailMessage("Same sparrow's name and uid")
                .isNotEqualTo(USER_JACK_GIVEN_NAME);
    }

    @Test
    public void test102GetAccountJackSparrow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountJackSparrowOid = assertUserBefore(USER_JACK_OID)
                .singleLink()
                .getOid();

        // WHEN
        when();
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountJackSparrowOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertShadow(shadow, "getObject")
            .assertName(USER_JACK_GIVEN_NAME)
            .attributes()
                .assertValue(SchemaConstants.ICFS_NAME, USER_JACK_GIVEN_NAME)
                .assertValue(SchemaConstants.ICFS_UID, accountJackSparrowUid)
                .assertHasPrimaryIdentifier()
                .assertNoSecondaryIdentifier();
        // @formatter:on
    }

    /**
     * MID-5077
     */
    @Test
    public void test110AssignAccountToJackSkellington() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertEquals(USER_SKELLINGTON_GIVEN_NAME, USER_JACK_GIVEN_NAME);

        // WHEN
        when();
        assignAccountToUser(USER_SKELLINGTON_OID, RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        String accountJackSkellingtonUid = assertUserAfter(USER_SKELLINGTON_OID)
            .singleLink()
                .target()
                    .assertName(USER_SKELLINGTON_GIVEN_NAME)
                    .attributes()
                        .assertValue(SchemaConstants.ICFS_NAME, USER_SKELLINGTON_GIVEN_NAME)
                        .getValue(SchemaConstants.ICFS_UID);
        // @formatter:on

        assertDummyAccountById(RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_NAME, accountJackSkellingtonUid)
                .assertName(USER_SKELLINGTON_GIVEN_NAME)
                .assertId(accountJackSkellingtonUid)
                .assertFullName(USER_SKELLINGTON_FULL_NAME);

        assertThat(accountJackSkellingtonUid).withFailMessage("Same skellington's name and uid")
                .isNotEqualTo(USER_SKELLINGTON_GIVEN_NAME);
        assertThat(accountJackSkellingtonUid).withFailMessage("Same skellington's and sparow's uid")
                .isNotEqualTo(accountJackSparrowUid);
    }
}
