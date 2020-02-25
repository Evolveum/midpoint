/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.misc;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.List;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import com.evolveum.midpoint.prism.PrismContext;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test with a resource that has unique primary identifier (ConnId UID), but non-unique secondary
 * identifier (ConnId NAME).
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUuidNonUniqueName extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/misc");

    protected static final File RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_FILE = new File(TEST_DIR, "resource-dummy-uuid-nonunique-name.xml");
    protected static final String RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID = "4027de14-2473-11e9-bd83-5f54b071e14f";
    protected static final String RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_NAME = "uuid-nonunique-name";

    protected static final File USER_SKELLINGTON_FILE = new File(TEST_DIR, "user-skellington.xml");
    protected static final String USER_SKELLINGTON_OID = "637fbaf0-2476-11e9-a181-f73466184d45";
    protected static final String USER_SKELLINGTON_NAME = "skellington";
    protected static final String USER_SKELLINGTON_GIVEN_NAME = "Jack";
    protected static final String USER_SKELLINGTON_FAMILY_NAME = "Skellington";
    protected static final String USER_SKELLINGTON_FULL_NAME = "Jack Skellington";

    String accountJackSparrowUid;
    String accountJackSkellingtonUid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_NAME,
                RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_FILE, RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID, initTask, initResult);

        importObjectFromFile(USER_SKELLINGTON_FILE);
    }

    @Test
    public void test010TestResourceConnection() throws Exception {
        final String TEST_NAME = "test010TestResourceConnection";

        // GIVEN
        Task task = createTask(TEST_NAME);

        // WHEN
        displayWhen(TEST_NAME);
        OperationResult result = modelService.testResource(RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID, task);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertResourceAfter(RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID)
            .displayXml()
            .assertHasSchema();
    }

    @Test
    public void test020RefinedSchema() throws Exception {
        final String TEST_NAME = "test020RefinedSchema";

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertResourceAfter(RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID)
            .displayXml()
            .assertHasSchema();

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

    }

    @Test
    public void test100AssignAccountToJackSparrow() throws Exception {
        final String TEST_NAME = "test100AssignAccountToJackSparrow";

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        accountJackSparrowUid = assertUserAfter(USER_JACK_OID)
            .singleLink()
                .target()
                    .assertName(USER_JACK_GIVEN_NAME)
                    .attributes()
                        .assertValue(SchemaConstants.ICFS_NAME, USER_JACK_GIVEN_NAME)
                        .getValue(SchemaConstants.ICFS_UID);

        assertDummyAccountById(RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_NAME, accountJackSparrowUid)
            .assertName(USER_JACK_GIVEN_NAME)
            .assertId(accountJackSparrowUid)
            .assertFullName(USER_JACK_FULL_NAME);

        assertFalse("Same sparrow's name and uid", USER_JACK_GIVEN_NAME.equals(accountJackSparrowUid));
    }

    @Test
    public void test102GetAccountJackSparrow() throws Exception {
        final String TEST_NAME = "test102GetAccountJackSparrow";

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        String accountJackSparrowOid = assertUserBefore(USER_JACK_OID)
                .singleLink()
                    .getOid();

        // WHEN
        displayWhen(TEST_NAME);
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountJackSparrowOid, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertShadow(shadow, "getObject")
                    .assertName(USER_JACK_GIVEN_NAME)
                    .attributes()
                        .assertValue(SchemaConstants.ICFS_NAME, USER_JACK_GIVEN_NAME)
                        .assertValue(SchemaConstants.ICFS_UID, accountJackSparrowUid)
                        .assertHasPrimaryIdentifier()
                        .assertNoSecondaryIdentifier();
    }

    /**
     * MID-5077
     */
    @Test
    public void test110AssignAccountToJackSkellington() throws Exception {
        final String TEST_NAME = "test110AssignAccountToJackSkellington";

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assertEquals(USER_SKELLINGTON_GIVEN_NAME, USER_JACK_GIVEN_NAME);

        // WHEN
        displayWhen(TEST_NAME);
        assignAccountToUser(USER_SKELLINGTON_OID, RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        accountJackSkellingtonUid = assertUserAfter(USER_SKELLINGTON_OID)
            .singleLink()
                .target()
                    .assertName(USER_SKELLINGTON_GIVEN_NAME)
                    .attributes()
                        .assertValue(SchemaConstants.ICFS_NAME, USER_SKELLINGTON_GIVEN_NAME)
                        .getValue(SchemaConstants.ICFS_UID);

        assertDummyAccountById(RESOURCE_DUMMY_UUID_NONUNIQUE_NAME_NAME, accountJackSkellingtonUid)
            .assertName(USER_SKELLINGTON_GIVEN_NAME)
            .assertId(accountJackSkellingtonUid)
            .assertFullName(USER_SKELLINGTON_FULL_NAME);

        assertFalse("Same skellington's name and uid", USER_SKELLINGTON_GIVEN_NAME.equals(accountJackSkellingtonUid));
        assertFalse("Same skellington's and sparow's uid", accountJackSparrowUid.equals(accountJackSkellingtonUid));
    }

}
