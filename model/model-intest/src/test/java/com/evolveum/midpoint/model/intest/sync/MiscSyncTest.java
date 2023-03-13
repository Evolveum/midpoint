/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Date;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Various synchronization-related tests.
 *
 * For example:
 *
 * . mapping chaining (MID-2149)
 * . synchronization reactions (MID-8369)
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class MiscSyncTest extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/sync");

    private static final DummyTestResource RESOURCE_DUMMY_BYZANTINE = new DummyTestResource(
            TEST_DIR, "resource-dummy-byzantine.xml", "10000000-0000-0000-0000-00000000f904", "byzantine",
            c -> {
                c.extendSchemaPirate();
                c.setSyncStyle(DummySyncStyle.SMART);
            });

    private static final TestTask TASK_LIVE_SYNC_DUMMY_BYZANTINE =
            new TestTask(TEST_DIR, "task-dummy-byzantine-livesync.xml", "10000000-0000-0000-5555-55550000f904");

    private static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
    private static final Date ACCOUNT_MANCOMB_VALID_FROM = MiscUtil.asDate(2011, 2, 3, 4, 5, 6);
    private static final Date ACCOUNT_MANCOMB_VALID_TO = MiscUtil.asDate(2066, 5, 4, 3, 2, 1);

    private static final String ORG_F0001_OID = "00000000-8888-6666-0000-100000000001";

    private static final TestObject<ObjectTemplateType> USER_TEMPLATE_BYZANTINE =
            TestObject.file(TEST_DIR, "user-template-byzantine.xml", "c287436b-1269-4a5f-83cd-a7ba1ef1c048");
    private static final TestObject<ObjectTemplateType> USER_TEMPLATE_BYZANTINE_DELETED =
            TestObject.file(TEST_DIR, "user-template-byzantine-deleted.xml", "9384e63f-9a68-4890-ae92-695aed8b4af3");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        if (areMarksSupported()) {
            repoAdd(CommonInitialObjects.ARCHETYPE_OBJECT_MARK, initResult);
            repoAdd(CommonInitialObjects.MARK_PROTECTED, initResult);
        }

        USER_TEMPLATE_BYZANTINE.init(this, initTask, initResult);
        USER_TEMPLATE_BYZANTINE_DELETED.init(this, initTask, initResult);
        RESOURCE_DUMMY_BYZANTINE.initAndTest(this, initTask, initResult);

        TASK_LIVE_SYNC_DUMMY_BYZANTINE.init(this, initTask, initResult);
        TASK_LIVE_SYNC_DUMMY_BYZANTINE.rerun(initResult); // to get the token
    }

    /** MID-2149 */
    @Test
    public void test100ImportWithChainedMappings() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_BYZANTINE.oid);
        try {
            assertUsers(6);

            given("mancomb account is on the resource");
            DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
            account.setEnabled(true);
            account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
            account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
            long loot = ACCOUNT_MANCOMB_VALID_FROM.getTime();
            account.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, loot);
            String gossip = XmlTypeConverter.createXMLGregorianCalendar(ACCOUNT_MANCOMB_VALID_TO).toXMLFormat();
            account.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, gossip);

            displayValue("Adding dummy account", account.debugDump());
            RESOURCE_DUMMY_BYZANTINE.addAccount(account);

            when("LS is run");
            long timeBeforeSync = System.currentTimeMillis();
            TASK_LIVE_SYNC_DUMMY_BYZANTINE.rerun(result);

            then("account is linked");
            PrismObject<ShadowType> accountMancomb =
                    findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, RESOURCE_DUMMY_BYZANTINE.get());
            display("Account mancomb", accountMancomb);
            assertNotNull("No mancomb account shadow", accountMancomb);
            assertEquals("Wrong resourceRef in mancomb account",
                    RESOURCE_DUMMY_BYZANTINE.oid,
                    accountMancomb.asObjectable().getResourceRef().getOid());
            assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, timeBeforeSync);

            and("user is OK");
            PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
            display("User mancomb", userMancomb);
            assertNotNull("User mancomb was not created", userMancomb);
            assertLiveLinks(userMancomb, 1);
            assertAdministrativeStatusEnabled(userMancomb);

            assertLinked(userMancomb, accountMancomb);

            and("e-mail address and orgs are correctly set");
            assertEquals("Wrong e-mail address for mancomb", "mancomb.Mr@test.com", userMancomb.asObjectable().getEmailAddress());
            assertAssignedOrg(userMancomb, ORG_F0001_OID);
            assertHasOrg(userMancomb, ORG_F0001_OID);

            and("no other users were created");
            assertUsers(7);
        } finally {
            setDefaultObjectTemplate(UserType.COMPLEX_TYPE, null);
        }
    }

    /** MID-8369 */
    @Test
    public void test110CheckDeletionReactions() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String name = "test110";

        given("there's account on the resource synced with midPoint");
        RESOURCE_DUMMY_BYZANTINE.addAccount(name);
        TASK_LIVE_SYNC_DUMMY_BYZANTINE.rerun(result);
        var shadowOid = assertUserByUsername(name, "after initial import")
                .display()
                .links()
                .singleLive()
                .getOid();

        when("the account is deleted and resource is synced");
        RESOURCE_DUMMY_BYZANTINE.getDummyResource().deleteAccountByName(name);
        TASK_LIVE_SYNC_DUMMY_BYZANTINE.rerun(result);

        then("shadow is unlinked and template was run");
        assertUserAfterByUsername(name)
                .assertDescription("deleted")
                .assertLinks(0, 0); // the shadow may or may not be really deleted (but it seems so)
        assertNoShadow(shadowOid); // see above - if the shadow would not be deleted, just check if it's dead instead
    }
}
