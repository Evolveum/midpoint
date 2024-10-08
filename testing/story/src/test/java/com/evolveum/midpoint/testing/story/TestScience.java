package com.evolveum.midpoint.testing.story;
/*
 * Copyright (c) 2013-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestScience extends AbstractStoryTest {

    private static final String TEST_DIR = "src/test/resources/science";

    public static final String NS_SCIENCE_EXT = "http://midpoint.evolveum.com/xml/ns/science/user/ext";
    private static final QName SCIENCE_EXTENSION_UID_QNAME = new QName(NS_SCIENCE_EXT, "aixUserId");

    private static final File ROLE_STATISTICS_FILE = new File(TEST_DIR, "role-statistics.xml");
    private static final String ROLE_STATISTICS_OID = "23d90f70-1924-419e-9beb-78a8bde6d261";

    private static final File GROUP_STATS_USERS_LDIF_FILE = new File(TEST_DIR, "group-stats.ldif");

    private static final File RESOURCE_OPENDJ_AD_SIMULATION_FILE = new File(TEST_DIR, "resource-opendj-ad-simulation.xml");
    private static final String RESOURCE_OPENDJ_AD_SIMULATION_OID = "10000000-0000-0000-0000-0000000001ad";

    private static final File RESOURCE_DUMMY_STATS_FILE = new File(TEST_DIR, "resource-dummy-stats.xml");
    private static final String RESOURCE_DUMMY_STATS_OID = "10000000-2000-0000-0000-0000000005aa";
    protected static final String RESOURCE_DUMMY_STATS_ID = "stats";

    private static final File RESOURCE_DUMMY_UNIX_FILE = new File(TEST_DIR, "resource-dummy-unix.xml");
    private static final String RESOURCE_DUMMY_UNIX_OID = "10000000-1000-0000-0000-0000000004a0";
    protected static final String RESOURCE_DUMMY_UNIX_ID = "unix";

    private static final String DUMMY_ACCOUNT_ATTRIBUTE_UNIX_SHELL_NAME = "Shell";
    private static final String DUMMY_ACCOUNT_ATTRIBUTE_UNIX_SYSTEM_NAME = "SYSTEM";
    private static final String DUMMY_ACCOUNT_ATTRIBUTE_UNIX_DIR_NAME = "Dir";
    private static final String DUMMY_ACCOUNT_ATTRIBUTE_UNIX_UID_NAME = "Uid";
    private static final String DUMMY_ACCOUNT_ATTRIBUTE_UNIX_DESCRIPTION_NAME = "Description";

    private static final String DUMMY_ACCOUNT_ATTRIBUTE_STATS_DESC_NAME = "Desc";

    private static final String NS_RESOURCE_INSTANCE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";

    private static final QName UNIX_INTERNAL_ID = new QName(NS_RESOURCE_INSTANCE, DummyAccount.ATTR_INTERNAL_ID);


    protected static DummyResource dummyResourceUnix;
    protected static DummyResourceContoller dummyResourceCtlUnix;
    protected ResourceType resourceDummyUnixType;
    protected PrismObject<ResourceType> resourceDummyUnix;

    protected static DummyResource dummyResourceStats;
    protected static DummyResourceContoller dummyResourceCtlStats;
    protected ResourceType resourceDummyStatsType;
    protected PrismObject<ResourceType> resourceDummyStats;

    protected ResourceType resourceOpenDjType;
    protected PrismObject<ResourceType> resourceOpenDj;

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Roles
        repoAddObjectFromFile(ROLE_STATISTICS_FILE, initResult);
//        repoAddObjectFromFile(ROLE_MATH_FILE, RoleType.class, initResult);

        resourceOpenDj = importAndGetObjectFromFileIgnoreWarnings(ResourceType.class, RESOURCE_OPENDJ_AD_SIMULATION_FILE, RESOURCE_OPENDJ_AD_SIMULATION_OID, initTask, initResult);
        resourceOpenDjType = resourceOpenDj.asObjectable();
        openDJController.setResource(resourceOpenDj);

        openDJController.addEntryFromLdifFile(GROUP_STATS_USERS_LDIF_FILE);

        // Resources
        dummyResourceCtlUnix = DummyResourceContoller.create(RESOURCE_DUMMY_UNIX_ID, resourceDummyUnix);
        dummyResourceCtlUnix.populateWithDefaultSchema();
        DummyObjectClass dummyUnixAccountObjectClass = dummyResourceCtlUnix.getDummyResource().getAccountObjectClass();
        dummyResourceCtlUnix.addAttrDef(dummyUnixAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_UNIX_DESCRIPTION_NAME, String.class, false, false);
        dummyResourceCtlUnix.addAttrDef(dummyUnixAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_UNIX_DIR_NAME, String.class, false, false);
        dummyResourceCtlUnix.addAttrDef(dummyUnixAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_UNIX_SHELL_NAME, String.class, false, false);
        dummyResourceCtlUnix.addAttrDef(dummyUnixAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_UNIX_SYSTEM_NAME, String.class, false, false);
        dummyResourceCtlUnix.addAttrDef(dummyUnixAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_UNIX_UID_NAME, Integer.class, false, false);
//        dummyResourceCtlUnix.extendSchemaAd();;
        dummyResourceUnix = dummyResourceCtlUnix.getDummyResource();
        resourceDummyUnix = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_UNIX_FILE, RESOURCE_DUMMY_UNIX_OID, initTask, initResult);
        resourceDummyUnixType = resourceDummyUnix.asObjectable();
        dummyResourceCtlUnix.setResource(resourceDummyUnix);

        dummyResourceCtlStats = DummyResourceContoller.create(RESOURCE_DUMMY_STATS_ID, resourceDummyStats);
        dummyResourceCtlStats.populateWithDefaultSchema();
        DummyObjectClass dummyStatsAccountObjectClass = dummyResourceCtlStats.getDummyResource().getAccountObjectClass();
        dummyResourceCtlStats.addAttrDef(dummyStatsAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_STATS_DESC_NAME, String.class, false, false);

        dummyResourceStats = dummyResourceCtlStats.getDummyResource();
        resourceDummyStats = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_STATS_FILE, RESOURCE_DUMMY_STATS_OID, initTask, initResult);
        resourceDummyStatsType = resourceDummyStats.asObjectable();
        dummyResourceCtlStats.setResource(resourceDummyStats);

    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultStats = modelService.testResource(RESOURCE_DUMMY_STATS_OID, task, task.getResult());
        TestUtil.assertSuccess(testResultStats);

        OperationResult testResultUnix = modelService.testResource(RESOURCE_DUMMY_UNIX_OID, task, task.getResult());
        TestUtil.assertSuccess(testResultUnix);

        OperationResult testResultAd = modelService.testResource(RESOURCE_OPENDJ_AD_SIMULATION_OID, task, task.getResult());
        TestUtil.assertSuccess(testResultAd);

        waitForTaskStart(TASK_TRIGGER_SCANNER_OID);
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID);
    }

    @Test
    public void test100JackAssignRoleStatistics() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_STATISTICS_OID, task, result);

        // WHEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
        AssertJUnit.assertNotNull("User jack not found", userAfter);

        display("User after", userAfter);

        assertUserJack(userAfter);
        assertLiveLinks(userAfter, 3);
        String accountStatsOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_STATS_OID);
        String accountUnixOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_UNIX_OID);
        String accountOpenDjOid = getLiveLinkRefOid(userAfter, RESOURCE_OPENDJ_AD_SIMULATION_OID);

        PrismObject<ShadowType> shadowStats = provisioningService.getObject(ShadowType.class, accountStatsOid, null, task, result);
        display("Stats account: ", shadowStats);
        PrismObject<ShadowType> shadowUnix = provisioningService.getObject(ShadowType.class, accountUnixOid, null, task, result);
        display("Unix account: ", shadowUnix);
        PrismObject<ShadowType> shadowOpenDj = provisioningService.getObject(ShadowType.class, accountOpenDjOid, null, task, result);
        display("AD account: ", shadowOpenDj);


        //internalId on unix dummy resource and title on openDJ simulation must be the same
        PrismProperty unixId = shadowUnix.findProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, UNIX_INTERNAL_ID));
        assertNotNull("No "+UNIX_INTERNAL_ID+" in "+shadowUnix, unixId);
        PrismProperty openDjSyncedId = shadowOpenDj.findProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, new QName(NS_RESOURCE_INSTANCE, "title")));
        assertNotNull("No 'title' in "+shadowOpenDj, openDjSyncedId);
        PrismAsserts.assertEquals("Unix id was not synced to the opendj properly.", String.valueOf(unixId.getAnyRealValue()), openDjSyncedId.getAnyRealValue());

        PrismProperty<Integer> generatedValue = userAfter.findExtensionItem(SCIENCE_EXTENSION_UID_QNAME);
         assertNotNull("Generated id value must not be null", generatedValue);
         assertFalse("Generated value must not be empty", generatedValue.isEmpty());
    }

    @Test
    public void test101UnassignRoleStats() throws Exception{
        Task task = getTestTask();

        OperationResult result = task.getResult();


        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        unassignRole(USER_JACK_OID, ROLE_STATISTICS_OID);


        PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
        AssertJUnit.assertNotNull("User jack not found", userJack);
        UserType jackType = userJack.asObjectable();

        display("User Jack", jackType);

        AssertJUnit.assertEquals("Wrong number of link refs", 3, jackType.getLinkRef().size());

        assertUserJack(userJack);
        assertLiveLinks(userJack, 3);
        String accountStatsOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_STATS_OID);
        String accountUnixOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_UNIX_OID);
        String accountOpenDjOid = getLiveLinkRefOid(userJack, RESOURCE_OPENDJ_AD_SIMULATION_OID);

        PrismObject<ShadowType> shadowStats = provisioningService.getObject(ShadowType.class, accountStatsOid, null, task, result);
        display("Stats account: ", shadowStats);
        PrismObject<ShadowType> shadowUnix = provisioningService.getObject(ShadowType.class, accountUnixOid, null, task, result);
        display("Unix account: ", shadowUnix);
        PrismObject<ShadowType> shadowOpenDj = provisioningService.getObject(ShadowType.class, accountOpenDjOid, null, task, result);
        display("AD account: ", shadowOpenDj);

        ObjectDelta<UserType> deleteStatsAccountDelta = prismContext.deltaFactory().object()
                .createModificationDeleteReference(UserType.class, USER_JACK_OID, UserType.F_LINK_REF,
                        accountStatsOid);
        executeChanges(deleteStatsAccountDelta, null, task, result);

        AssertJUnit.assertTrue("Expected empty assignment", jackType.getAssignment().isEmpty());

        PrismObject<UserType> userJackAfter = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
        AssertJUnit.assertNotNull("User jack not found", userJackAfter);
        UserType jackTypeAfter = userJackAfter.asObjectable();

        display("User Jack", jackTypeAfter);

        AssertJUnit.assertEquals("Wrong number of link refs", 2, jackTypeAfter.getLinkRef().size());

    }

    @Test
    public void test102AssignRoleStats() throws Exception{
        Task task = getTestTask();

        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        openDJController.stop();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_STATISTICS_OID, task, result);

        // THEN
        if (InternalsConfig.isShadowCachingFullByDefault()) {
            assertSuccess(result); // not reading from the resource, no error encountered
        } else {
            assertInProgress(result);
        }
        PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
        AssertJUnit.assertNotNull("User jack not found", userJack);
        UserType jackType = userJack.asObjectable();

        display("User Jack", jackType);

        AssertJUnit.assertEquals("Wrong number of link refs", 3, jackType.getLinkRef().size());

        assertUserJack(userJack);
        assertLiveLinks(userJack, 3);
        String accountStatsOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_STATS_OID);
        String accountUnixOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_UNIX_OID);
        String accountOpenDjOid = getLiveLinkRefOid(userJack, RESOURCE_OPENDJ_AD_SIMULATION_OID);

        PrismObject<ShadowType> shadowStats = provisioningService.getObject(ShadowType.class, accountStatsOid, null, task, result);
        display("Stats account: ", shadowStats);
        PrismObject<ShadowType> shadowUnix = provisioningService.getObject(ShadowType.class, accountUnixOid, null, task, result);
        display("Unix account: ", shadowUnix);
        PrismObject<ShadowType> shadowOpenDj = provisioningService.getObject(ShadowType.class, accountOpenDjOid, null, task, result);
        display("AD account: ", shadowOpenDj);

        openDJController.start();
    }

    @Test
    public void test200DeleteUserJack() throws Exception {
        Task task = getTestTask();

        OperationResult result = task.getResult();

        assertUserBefore(USER_JACK_OID);

        // WHEN
        deleteObject(UserType.class, USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();

        display("Result: ", result);
        if (!result.isSuccess() && !result.isHandledError()) {
            display(result);
            AssertJUnit.fail("Expected success or handled error, but got "+result.getStatus());
        }

        assertNoObject(UserType.class, USER_JACK_OID, task, result);
    }
}
