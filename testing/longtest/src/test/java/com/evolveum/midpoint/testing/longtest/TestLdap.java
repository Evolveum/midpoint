/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.longtest;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.test.CommonInitialObjects;

import org.apache.commons.lang3.mutable.MutableInt;
import org.opends.server.types.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.sync.tasks.recon.ReconciliationLauncher;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Mix of various tests for issues that are difficult to replicate using dummy resources.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-longtest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdap extends AbstractLongTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ldap");

    protected static final File ROLE_PIRATE_FILE = new File(TEST_DIR, "role-pirate.xml");
    protected static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556666";

    private static final String USER_LECHUCK_NAME = "lechuck";
    private static final String ACCOUNT_LECHUCK_NAME = "lechuck";
    private static final String ACCOUNT_CHARLES_NAME = "charles";

    protected static final File TASK_DELETE_OPENDJ_SHADOWS_FILE = new File(TEST_DIR, "task-delete-opendj-shadows.xml");
    protected static final String TASK_DELETE_OPENDJ_SHADOWS_OID = "412218e4-184b-11e5-9c9b-3c970e467874";

    protected static final File TASK_DELETE_OPENDJ_ACCOUNTS_FILE = new File(TEST_DIR, "task-delete-opendj-accounts.xml");
    protected static final String TASK_DELETE_OPENDJ_ACCOUNTS_OID = "b22c5d72-18d4-11e5-b266-001e8c717e5b";

    private static final int NUM_INITIAL_USERS = 3;
    // Make it at least 1501 so it will go over the 3000 entries size limit
    private static final int NUM_LDAP_ENTRIES = 1600;
//    private static final int NUM_LDAP_ENTRIES = 100;

    /** Estimated time to import/recon one LDAP entry. 2000 is too low for Jenkins+Oracle; moreover, test910 used 3000 already. */
    private static final int TIME_PER_LDAP_ENTRY = 3000;

    private static final String LDAP_GROUP_PIRATES_DN = "cn=Pirates,ou=groups,dc=example,dc=com";

    protected ResourceType resourceOpenDjType;
    protected PrismObject<ResourceType> resourceOpenDj;

    @Autowired
    private ReconciliationLauncher reconciliationLauncher;

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() {
        //end profiling
        ProfilingDataManager.getInstance().printMapAfterTest();
        ProfilingDataManager.getInstance().stopProfilingAfterTest();

        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        CommonInitialObjects.SERVICE_ORIGIN_INTERNAL.init(this, initTask, initResult);

        // Users
        repoAddObjectFromFile(USER_BARBOSSA_FILE, initResult);
        repoAddObjectFromFile(USER_GUYBRUSH_FILE, initResult);

        // Roles
        repoAddObjectFromFile(ROLE_PIRATE_FILE, initResult);

        // Resources
        resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        resourceOpenDjType = resourceOpenDj.asObjectable();
        openDJController.setResource(resourceOpenDj);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        //initProfiling - start
        ProfilingDataManager profilingManager = ProfilingDataManager.getInstance();

        Map<ProfilingDataManager.Subsystem, Boolean> subsystems = new HashMap<>();
        subsystems.put(ProfilingDataManager.Subsystem.MODEL, true);
        subsystems.put(ProfilingDataManager.Subsystem.REPOSITORY, true);
        profilingManager.configureProfilingDataManagerForTest(subsystems, true);

        profilingManager.appendProfilingToTest();
        //initProfiling - end

        displayValue("initial LDAP content", openDJController.dumpEntries());
    }

    @Test
    public void test000Sanity() throws Exception {
        assertUsers(NUM_INITIAL_USERS);
    }

    /**
     * Barbossa is already member of LDAP group "pirates". The role adds this group as well.
     * This should go smoothly. No error expected.
     */
    @Test
    public void test200AssignRolePiratesToBarbossa() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_BARBOSSA_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        String accountDn = assertOpenDjAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true).getDN().toString();
        openDJController.assertUniqueMember(LDAP_GROUP_PIRATES_DN, accountDn);

        assertUsers(NUM_INITIAL_USERS);
    }

    /**
     * Just a first step for the following test.
     * Also, Guybrush has a photo. Check that binary property mapping works.
     */
    @Test
    public void test202AssignLdapAccountToGuybrush() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        byte[] photoIn = Files.readAllBytes(Paths.get(DOT_JPG_FILENAME));
        displayValue("Photo in", MiscUtil.bytesToHex(photoIn));
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_JPEG_PHOTO, task, result, photoIn);

        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .item(UserType.F_JPEG_PHOTO).retrieve()
                .build();
        PrismObject<UserType> userBefore = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, options, task, result);
        display("User before", userBefore);
        byte[] userJpegPhotoBefore = userBefore.asObjectable().getJpegPhoto();
        assertEquals("Photo byte length changed (user before)", photoIn.length, userJpegPhotoBefore.length);
        assertTrue("Photo bytes do not match (user before)", Arrays.equals(photoIn, userJpegPhotoBefore));

        // WHEN
        when();
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_OPENDJ_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertOpenDjAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        byte[] jpegPhotoLdap = OpenDJController.getAttributeValueBinary(entry, "jpegPhoto");
        assertNotNull("No jpegPhoto in LDAP entry", jpegPhotoLdap);
        assertEquals("Byte length changed (LDAP)", photoIn.length, jpegPhotoLdap.length);
        assertTrue("Bytes do not match (LDAP)", Arrays.equals(photoIn, jpegPhotoLdap));

        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, options, task, result);
        display("User after", userAfter);
        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadow = getShadowModel(accountOid);

        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        ItemName jpegPhotoQName = new ItemName(RESOURCE_OPENDJ_NAMESPACE, "jpegPhoto");
        PrismProperty<byte[]> jpegPhotoAttr = attributesContainer.findProperty(jpegPhotoQName);
        byte[] photoBytesOut = jpegPhotoAttr.getValues().get(0).getValue();

        displayValue("Photo bytes out", MiscUtil.bytesToHex(photoBytesOut));

        assertEquals("Photo byte length changed (shadow)", photoIn.length, photoBytesOut.length);
        assertTrue("Photo bytes do not match (shadow)", Arrays.equals(photoIn, photoBytesOut));

        assertUsers(NUM_INITIAL_USERS);
    }

    /**
     * Add guybrush to LDAP group before he gets the role. Make sure that the DN in the uniqueMember
     * attribute does not match (wrong case). This will check matching rule implementation in provisioning.
     */
    @Test
    public void test204AssignRolePiratesToGuybrush() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        openDJController.executeLdifChange(
                "dn: cn=Pirates,ou=groups,dc=example,dc=com\n" +
                "changetype: modify\n" +
                "add: uniqueMember\n" +
                "uniqueMember: uid=GuyBrush,ou=pEOPle,dc=EXAMPLE,dc=cOm");

        // WHEN
        when();
        assignRole(USER_GUYBRUSH_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        String accountDn = assertOpenDjAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true).getDN().toString();
        openDJController.assertUniqueMember(LDAP_GROUP_PIRATES_DN, accountDn);

        assertUsers(NUM_INITIAL_USERS);
    }

    @Test
    public void test400RenameLeChuckConflicting() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userLechuck = createUser(USER_LECHUCK_NAME, "LeChuck", true);
        userLechuck.asObjectable().getAssignment().add(createAccountAssignment(RESOURCE_OPENDJ_OID, null));
        userLechuck.asObjectable().setFamilyName(PrismTestUtil.createPolyStringType("LeChuck"));
        addObject(userLechuck);
        String userLechuckOid = userLechuck.getOid();

        PrismObject<ShadowType> accountCharles = createAccount(resourceOpenDj, toDn(ACCOUNT_CHARLES_NAME), true);
        addAttributeToShadow(accountCharles, "sn", "Charles");
        addAttributeToShadow(accountCharles, "cn", "Charles L. Charles");
        addObject(accountCharles);

        // preconditions
        assertOpenDjAccount(ACCOUNT_LECHUCK_NAME, "LeChuck", true);
        assertOpenDjAccount(ACCOUNT_CHARLES_NAME, "Charles L. Charles", true);

        // WHEN
        when();
        modifyUserReplace(userLechuckOid, UserType.F_NAME, task, result,
                PrismTestUtil.createPolyString(ACCOUNT_CHARLES_NAME));

        // THEN
        then();
        assertOpenDjAccount(ACCOUNT_CHARLES_NAME, "Charles L. Charles", true);
        assertOpenDjAccount(ACCOUNT_CHARLES_NAME + "1", "LeChuck", true);
        assertNoOpenDjAccount(ACCOUNT_LECHUCK_NAME);

        assertUsers(NUM_INITIAL_USERS + 1);
    }

    @Test
    public void test800BigLdapSearch() throws Exception {
        // GIVEN
        assertUsers(NUM_INITIAL_USERS + 1);
        loadLdapEntries("a", NUM_LDAP_ENTRIES);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(
                RESOURCE_OPENDJ_OID,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"));

        final MutableInt count = new MutableInt(0);
        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
            @Override
            public boolean handle(PrismObject<ShadowType> shadow, OperationResult parentResult) {
                count.increment();
                display("Found", shadow);
                return true;
            }
        };

        // WHEN
        when();
        modelService.searchObjectsIterative(ShadowType.class, query, handler, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // THEN
        then();

        assertEquals("Unexpected number of search results", NUM_LDAP_ENTRIES + 8, count.intValue());

        assertUsers(NUM_INITIAL_USERS + 1);
    }

    @Test
    public void test810BigImport() throws Exception {
        // GIVEN
        assertUsers(NUM_INITIAL_USERS + 1);

        loadLdapEntries("u", NUM_LDAP_ENTRIES);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        // WHEN
        when();
        //task.setExtensionPropertyValue(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS, 2);
        modelService.importFromResource(RESOURCE_OPENDJ_OID,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, 20000 + NUM_LDAP_ENTRIES * TIME_PER_LDAP_ENTRY);

        // THEN
        then();

        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        displayValue("Users", userCount);
        assertEquals("Unexpected number of users", 2 * NUM_LDAP_ENTRIES + 8, userCount);
    }

    @Test
    public void test820BigReconciliation() throws Exception {
        // GIVEN

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ResourceType resource = modelService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result).asObjectable();

        // WHEN
        when();
        reconciliationLauncher.launch(resource,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), task, result);

        // THEN
        then();
        waitForTaskFinish(task, 20000 + NUM_LDAP_ENTRIES * TIME_PER_LDAP_ENTRY);

        // THEN
        then();

        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        displayValue("Users", userCount);
        assertEquals("Unexpected number of users", 2 * NUM_LDAP_ENTRIES + 8, userCount);
    }

    @Test
    public void test900DeleteShadows() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        importObjectFromFile(TASK_DELETE_OPENDJ_SHADOWS_FILE);

        // THEN
        then();

        waitForTaskFinish(TASK_DELETE_OPENDJ_SHADOWS_OID, 20000 + NUM_LDAP_ENTRIES * TIME_PER_LDAP_ENTRY);

        // THEN
        then();

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        // @formatter:off
        assertTask(TASK_DELETE_OPENDJ_SHADOWS_OID, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(2 * NUM_LDAP_ENTRIES + 8, 0, 0);
        // @formatter:on

        assertOpenDjAccountShadows(0, true, task, result);
        assertUsers(2 * NUM_LDAP_ENTRIES + 8);

        // Check that the actual accounts were NOT deleted
        // (This also re-creates shadows)
        assertOpenDjAccountShadows(2 * NUM_LDAP_ENTRIES + 8, false, task, result);
    }

    @Test
    public void test910DeleteAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        importObjectFromFile(TASK_DELETE_OPENDJ_ACCOUNTS_FILE);

        // THEN
        then();

        waitForTaskFinish(TASK_DELETE_OPENDJ_ACCOUNTS_OID, 20000 + NUM_LDAP_ENTRIES * TIME_PER_LDAP_ENTRY);

        // THEN
        then();

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        // @formatter:off
        assertTask(TASK_DELETE_OPENDJ_SHADOWS_OID, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(2 * NUM_LDAP_ENTRIES + 8, 0, 0);
        // @formatter:on

        assertOpenDjAccountShadows(1, true, task, result);
        assertUsers(2 * NUM_LDAP_ENTRIES + 8);
        assertOpenDjAccountShadows(1, false, task, result);
    }

    private void assertOpenDjAccountShadows(int expected, boolean raw, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(
                RESOURCE_OPENDJ_OID,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"));

        final MutableInt count = new MutableInt(0);
        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
            @Override
            public boolean handle(PrismObject<ShadowType> shadow, OperationResult parentResult) {
                count.increment();
                display("Found", shadow);
                return true;
            }
        };
        Collection<SelectorOptions<GetOperationOptions>> options = null;
        if (raw) {
            options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
        }
        modelService.searchObjectsIterative(ShadowType.class, query, handler, options, task, result);
        assertEquals("Unexpected number of search results (raw=" + raw + ")", expected, count.intValue());
    }

    private String toDn(String username) {
        return "uid=" + username + "," + OPENDJ_PEOPLE_SUFFIX;
    }
}
