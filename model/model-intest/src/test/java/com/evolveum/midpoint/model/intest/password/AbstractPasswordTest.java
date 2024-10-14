/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.password;

import static com.evolveum.midpoint.util.MiscUtil.assertNonNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.polystring.PolyString;

import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ItemComparisonResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractPasswordTest extends AbstractInitializedModelIntegrationTest {

    protected static final String USER_PASSWORD_1_CLEAR = "d3adM3nT3llN0Tal3s";
    protected static final String USER_PASSWORD_2_CLEAR = "bl4ckP3arl";
    protected static final String USER_PASSWORD_3_CLEAR = "wh3r3sTheRum?";
    private static final String USER_PASSWORD_3A_CLEAR = "wh3r3sTheRum!!";
    protected static final String USER_PASSWORD_4_CLEAR = "sh1v3rM3T1mb3rs";
    protected static final String USER_PASSWORD_5_CLEAR = "s3tSa1al";
    protected static final String USER_PASSWORD_AA_CLEAR = "AA"; // too short
    protected static final String USER_PASSWORD_A_CLEAR = "A"; // too short
    protected static final String USER_PASSWORD_JACK_CLEAR = "12jAcK34"; // contains username
    protected static final String USER_PASSWORD_SPARROW_CLEAR = "spaRRow123"; // contains familyName
    protected static final String USER_PASSWORD_VALID_1 = "abcd123";
    protected static final String USER_PASSWORD_VALID_2 = "abcd223";
    protected static final String USER_PASSWORD_VALID_3 = "abcd323";
    protected static final String USER_PASSWORD_VALID_4 = "abcd423";
    protected static final String USER_PASSWORD_VALID_5 = "abcd523";
    protected static final String USER_PASSWORD_VALID_6 = "abcd623";
    // Very long and very simple password. This is meant to violate the policies.
    protected static final String USER_PASSWORD_LLL_CLEAR = "lllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll";

    protected static final String PASSWORD_ALLIGATOR = "all1g4t0r";
    protected static final String PASSWORD_CROCODILE = "cr0c0d1l3";
    protected static final String PASSWORD_GIANT_LIZARD = "G14NTl1z4rd";

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "password");

    protected static final File RESOURCE_DUMMY_UGLY_FILE = new File(TEST_DIR, "resource-dummy-ugly.xml");
    protected static final String RESOURCE_DUMMY_UGLY_OID = "10000000-0000-0000-0000-000000344104";
    protected static final String RESOURCE_DUMMY_UGLY_NAME = "ugly";

    private static final DummyTestResource RESOURCE_DUMMY_PURPOSE = new DummyTestResource(
            TEST_DIR, "resource-dummy-purpose.xml", "519f131a-147b-11e7-a270-c38e2b225751", "purpose",
            c -> c.extendSchemaPirate());

    protected static final File RESOURCE_DUMMY_SOUVENIR_FILE = new File(TEST_DIR, "resource-dummy-souvenir.xml");
    protected static final String RESOURCE_DUMMY_SOUVENIR_OID = "f4fd7e90-ff6a-11e7-a504-4b84f92fec0e";
    protected static final String RESOURCE_DUMMY_SOUVENIR_NAME = "souvenir";

    protected static final File RESOURCE_DUMMY_MAVERICK_FILE = new File(TEST_DIR, "resource-dummy-maverick.xml");
    protected static final String RESOURCE_DUMMY_MAVERICK_OID = "72a928b6-ff7b-11e7-9643-7366d7749c31";
    protected static final String RESOURCE_DUMMY_MAVERICK_NAME = "maverick";

    protected static final File PASSWORD_POLICY_UGLY_FILE = new File(TEST_DIR, "password-policy-ugly.xml");
    protected static final String PASSWORD_POLICY_UGLY_OID = "cfb3fa9e-027a-11e7-8e2c-dbebaacaf4ee";

    protected static final File PASSWORD_POLICY_MAVERICK_FILE = new File(TEST_DIR, "password-policy-maverick.xml");
    protected static final String PASSWORD_POLICY_MAVERICK_OID = "b26d2bd4-ff83-11e7-94b3-8fa7a87aac6c";

    protected static final File SECURITY_POLICY_UGLY_FILE = new File(TEST_DIR, "security-policy-ugly.xml");
    protected static final String SECURITY_POLICY_UGLY_OID = "cfb3fa9e-eeee-eeee-eeee-dbebaacaf4ee";

    protected static final File SECURITY_POLICY_MAVERICK_FILE = new File(TEST_DIR, "security-policy-maverick.xml");
    protected static final String SECURITY_POLICY_MAVERICK_OID = "b26d2bd4-eeee-eeee-eeee-8fa7a87aac6c";

    protected static final File SECURITY_POLICY_DEFAULT_STORAGE_HASHING_FILE = new File(TEST_DIR, "security-policy-default-storage-hashing.xml");
    protected static final String SECURITY_POLICY_DEFAULT_STORAGE_HASHING_OID = "0ea3b93c-0425-11e7-bbc1-73566dc53d59";

    protected static final File SECURITY_POLICY_PASSWORD_STORAGE_NONE_FILE = new File(TEST_DIR, "security-policy-password-storage-none.xml");
    protected static final String SECURITY_POLICY_PASSWORD_STORAGE_NONE_OID = "2997a20a-0423-11e7-af65-a7ab7d19442c";

    protected static final File SECURITY_POLICY_GOVERNOR_FILE = new File(TEST_DIR, "security-policy-governor.xml");
    protected static final String SECURITY_POLICY_GOVERNOR_OID = "12344321-0000-0000-0055-000000000003";

    protected static final String USER_JACK_EMPLOYEE_NUMBER_NEW_BAD = "No1";
    protected static final String USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD = "pir321";
    protected static final String USER_RAPP_EMAIL = "rapp.scallion@evolveum.com";

    private static final TestObject<TaskType> TASK_CHANGE_JACK_ACCOUNT_PASSWORD = TestObject.file(TEST_DIR, "task-change-jack-account-password.xml", "442f8d91-4f1c-4651-b6c6-65b5aa3ab1d4");

    public static final String PASSWORD_HELLO_WORLD = "H3ll0w0rld";

    public static final int ORG_MINISTRY_OF_OFFENSE_PASSWORD_HISTORY_LENGTH = 3;
    public static final int GLOBAL_POLICY_NEW_PASSWORD_HISTORY_LENGTH = 5;

    protected String accountJackOid;
    protected String accountJackRedOid;
    protected String accountJackBlueOid;
    protected String accountJackUglyOid;
    protected String accountJackBlackOid;
    protected String accountJackYellowOid;
    protected String accountJackSouvenirOid;
    protected String accountJackMaverickOid;
    protected XMLGregorianCalendar lastPasswordChangeStart;
    protected XMLGregorianCalendar lastPasswordChangeEnd;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        InternalsConfig.setAvoidLoggingChange(isAvoidLoggingChange());
        super.initSystem(initTask, initResult);

        importObjectFromFile(PASSWORD_POLICY_UGLY_FILE);
        importObjectFromFile(SECURITY_POLICY_UGLY_FILE);
        importObjectFromFile(SECURITY_POLICY_DEFAULT_STORAGE_HASHING_FILE);
        importObjectFromFile(SECURITY_POLICY_PASSWORD_STORAGE_NONE_FILE);

        setGlobalSecurityPolicy(getSecurityPolicyOid(), initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_UGLY_NAME, RESOURCE_DUMMY_UGLY_FILE, RESOURCE_DUMMY_UGLY_OID, initTask, initResult);
        initDummyResourcePirate(RESOURCE_DUMMY_SOUVENIR_NAME, RESOURCE_DUMMY_SOUVENIR_FILE, RESOURCE_DUMMY_SOUVENIR_OID, initTask, initResult);

        RESOURCE_DUMMY_PURPOSE.initAndTest(this, initTask, initResult);

        repoAddObjectFromFile(USER_THREE_HEADED_MONKEY_FILE, UserType.class, true, initResult);

        repoAddObjectFromFile(ROLE_END_USER_FILE, initResult);

        importObjectFromFile(PASSWORD_POLICY_MAVERICK_FILE);
        importObjectFromFile(SECURITY_POLICY_MAVERICK_FILE);
        initDummyResourcePirate(RESOURCE_DUMMY_MAVERICK_NAME, RESOURCE_DUMMY_MAVERICK_FILE, RESOURCE_DUMMY_MAVERICK_OID, initTask, initResult);

        login(USER_ADMINISTRATOR_USERNAME);
    }

    protected abstract String getSecurityPolicyOid();

    @Test
    public void test000Sanity() throws Exception {
        AccountActivationNotifierType accountActivationNotifier = null;
        SystemConfigurationType systemConfig = getObject(SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value()).asObjectable();
        IntegrationTestTools.displayXml("system config", systemConfig.asPrismObject());
        for (EventHandlerType handler : systemConfig.getNotificationConfiguration().getHandler()) {
            displayValue("Handler: ", handler);
            List<AccountActivationNotifierType> accountActivationNotifiers = handler.getAccountActivationNotifier();
            if (!accountActivationNotifiers.isEmpty()) {
                accountActivationNotifier = accountActivationNotifiers.get(0);
            }
        }

        displayValue("Account activation notifier", accountActivationNotifier);
        assertNotNull("No accountActivationNotifier", accountActivationNotifier);
    }

    @Test
    public void test010AddPasswordPolicy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ObjectType> passwordPolicy = addObject(PASSWORD_POLICY_GLOBAL_FILE, task, result);

        // THEN
        assertSuccess(result);

        assertEquals("Wrong OID after add", PASSWORD_POLICY_GLOBAL_OID, passwordPolicy.getOid());

        // Check object
        PrismObject<ValuePolicyType> valuePolicy = repositoryService.getObject(ValuePolicyType.class, PASSWORD_POLICY_GLOBAL_OID, null, result);

        // TODO: more asserts
    }

    @Test
    public void test012AddSecurityPolicy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<SecurityPolicyType> securityPolicy = addObject(SECURITY_POLICY_GOVERNOR_FILE, task, result);

        // THEN
        assertSuccess(result);

        assertEquals("Wrong OID after add", SECURITY_POLICY_GOVERNOR_OID, securityPolicy.getOid());

        // Check object
        PrismObject<SecurityPolicyType> securityPolicyAfter = repositoryService.getObject(SecurityPolicyType.class, SECURITY_POLICY_GOVERNOR_OID, null, result);

        // TODO: more asserts
    }

    @Test
    public void test050CheckJackPassword() throws Exception {
        // GIVEN, WHEN
        // this happens during test initialization when user-jack.xml is added

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        // Password still encrypted. We haven't changed it yet.
        assertUserPassword(userJack, USER_JACK_PASSWORD, CredentialsStorageTypeType.ENCRYPTION);
    }

    @Test
    public void test051ModifyUserJackPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

//        // Set password create channel to legacy value (MID-6547).
//        repositoryService.modifyObject(
//                UserType.class, USER_JACK_OID,
//                deltaFor(UserType.class)
//                        .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
//                            .replace(Channel.USER.getLegacyUri())
//                        .asItemDeltas(),
//                result);

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_1_CLEAR, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
        assertPasswordMetadata(userJack, false, startCal, endCal);
        // Password policy is not active yet. No history should be kept.
        assertPasswordHistoryEntries(userJack);

        // Check channel migration (MID-6547).
        //assertThat(userJack.asObjectable().getCredentials().getPassword().getMetadata().getCreateChannel()).isEqualTo(Channel.USER.getUri());

        assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
    }

    @Test
    public void test060CheckJackPasswordModelInteraction() throws Exception {
        if (getPasswordStorageType() == CredentialsStorageTypeType.NONE) {
            // Nothing to check in this case
            return;
        }

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN, THEN
        ProtectedStringType userPasswordPsGood = new ProtectedStringType();
        userPasswordPsGood.setClearValue(USER_PASSWORD_1_CLEAR);
        assertTrue("Good password check failed",
                modelInteractionService.checkPassword(USER_JACK_OID, userPasswordPsGood, task, result));

        ProtectedStringType userPasswordPsBad = new ProtectedStringType();
        userPasswordPsBad.setClearValue("this is not a password");
        assertFalse("Bad password check failed",
                modelInteractionService.checkPassword(USER_JACK_OID, userPasswordPsBad, task, result));

        ProtectedStringType userPasswordPsEmpty = new ProtectedStringType();
        assertFalse("Empty password check failed",
                modelInteractionService.checkPassword(USER_JACK_OID, userPasswordPsEmpty, task, result));

        assertFalse("Null password check failed",
                modelInteractionService.checkPassword(USER_JACK_OID, null, task, result));

    }

    @Test
    public void test070AddUserHerman() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        addObject(USER_HERMAN_FILE, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_HERMAN_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_HERMAN_OID, USER_HERMAN_USERNAME,
                USER_HERMAN_FULL_NAME, USER_HERMAN_GIVEN_NAME, USER_HERMAN_FAMILY_NAME);

        assertUserPassword(userAfter, USER_HERMAN_PASSWORD);
        assertPasswordMetadata(userAfter, true, startCal, endCal);
        // Password policy is not active yet. No history should be kept.
        assertPasswordHistoryEntries(userAfter);

        assertSingleUserPasswordNotification(USER_HERMAN_USERNAME, USER_HERMAN_PASSWORD);
    }

    @Test
    public void test100JackAssignAccountDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadowRepo = getShadowRepo(accountJackOid);
        display("Repo shadow", accountShadowRepo);
        assertDummyAccountShadowRepo(accountShadowRepo, accountJackOid, USER_JACK_USERNAME);
        // MID-3860
        assertShadowPasswordMetadata(accountShadowRepo.getPrismObject(), startCal, endCal, false, true);
        assertShadowPurpose(accountShadowRepo.getPrismObject(), false);

        // Check account
        PrismObject<ShadowType> accountShadowModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        display("Model shadow", accountShadowModel);
        assertDummyAccountShadowModel(accountShadowModel, accountJackOid, USER_JACK_USERNAME, USER_JACK_FULL_NAME);
        assertShadowPasswordMetadata(accountShadowModel, startCal, endCal, false, true);
        assertShadowPurpose(accountShadowModel, false);

        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        assertDummyPasswordConditional(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        assertSingleAccountPasswordNotificationConditional(null, USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
        assertNoUserPasswordNotifications();
    }

    /**
     * This time user has assigned account. Account password should be changed as well.
     */
    @Test
    public void test110ModifyUserJackPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

//        // Set account password create channel to legacy value (MID-6547).
//        repositoryService.modifyObject(
//                ShadowType.class, accountJackOid,
//                deltaFor(ShadowType.class)
//                        .item(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
//                            .replace(Channel.USER.getLegacyUri())
//                        .asItemDeltas(),
//                result);

        // WHEN
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_2_CLEAR, task, result);

        // THEN
        assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertUserPassword(userJack, USER_PASSWORD_2_CLEAR);
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_2_CLEAR);
        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        // Check shadow
        var accountShadowRepo = getShadowRepo(accountJackOid);
        displayDumpable("Repo shadow", accountShadowRepo);
        assertDummyAccountShadowRepo(accountShadowRepo, accountJackOid, "jack");
        // MID-3860
        assertShadowPasswordMetadata(
                accountShadowRepo.getPrismObject(), lastPasswordChangeStart, lastPasswordChangeEnd, true, false);
        assertShadowPurpose(accountShadowRepo.getPrismObject(), false);

//        // Check channel migration (MID-6547).
//        assertThat(accountShadowRepo.getBean().getCredentials().getPassword().getMetadata().getCreateChannel()).isEqualTo(Channel.USER.getUri());

        // Check account
        PrismObject<ShadowType> accountShadowModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        display("Model shadow", accountShadowModel);
        assertDummyAccountShadowModel(accountShadowModel, accountJackOid, "jack", "Jack Sparrow");
        assertShadowPasswordMetadata(accountShadowModel, lastPasswordChangeStart, lastPasswordChangeEnd, true, false);
        assertShadowPurpose(accountShadowModel, false);

        assertSingleAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_2_CLEAR);
        assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_2_CLEAR);
    }

    /**
     * Modify account password. User's password should be unchanged
     */
    @Test
    public void test111ModifyAccountJackPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        modifyAccountChangePassword(accountJackOid, USER_PASSWORD_3_CLEAR, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        // User should still have old password
        assertUserPassword(userJack, USER_PASSWORD_2_CLEAR);
        // Account has new password
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_3_CLEAR);

        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        assertSingleAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_3_CLEAR);
        assertNoUserPasswordNotifications();
    }

    /**
     * Changing shadow password from the task. Checking taskRef being correctly set.
     *
     * MID-7179
     */
    @Test
    public void test112ModifyAccountJackPasswordInBackground() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        when();
        addObject(TASK_CHANGE_JACK_ACCOUNT_PASSWORD, task, result);
        waitForTaskCloseOrSuspend(TASK_CHANGE_JACK_ACCOUNT_PASSWORD.oid);

        then();
        assertTask(TASK_CHANGE_JACK_ACCOUNT_PASSWORD.oid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertSuccessCount(0, 1);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        // User should still have old password
        assertUserPassword(userJack, USER_PASSWORD_2_CLEAR);
        // Account has new password
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_3A_CLEAR);

        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        // @formatter:off
        assertUser(userJack.getOid(), "after")
                .links()
                    .singleLive()
                        .resolveTarget()
                            .display("shadow after")
                            .passwordMetadata()
                                .assertModifyTaskOid(TASK_CHANGE_JACK_ACCOUNT_PASSWORD.oid);
        // @formatter:on

        assertSingleAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_3A_CLEAR);
        assertNoUserPasswordNotifications();
    }

    /**
     * Modify both user and account password. As password outbound mapping is weak the user should have its own password
     * and account should have its own password.
     */
    @Test
    public void test115ModifyJackPasswordUserAndAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        ProtectedStringType userPasswordPs4 = new ProtectedStringType();
        userPasswordPs4.setClearValue(USER_PASSWORD_4_CLEAR);
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, PASSWORD_VALUE_PATH, userPasswordPs4);

        ProtectedStringType userPasswordPs5 = new ProtectedStringType();
        userPasswordPs5.setClearValue(USER_PASSWORD_5_CLEAR);
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountJackOid, getDummyResourceObject(),
                PASSWORD_VALUE_PATH, userPasswordPs5);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta, userDelta);

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        // User should still have old password
        assertUserPassword(userJack, USER_PASSWORD_4_CLEAR);
        // Account has new password
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_5_CLEAR);

        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        assertSingleAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_5_CLEAR);
        assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_4_CLEAR);
    }

    /**
     * Add red and ugly dummy resource to the mix. This would be fun.
     */
    @Test
    public void test120JackAssignAccountDummyRedAndUgly() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, task, result);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_UGLY_OID, null, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertLiveLinks(userJack, 3);
        accountJackRedOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        accountJackUglyOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_UGLY_OID);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_4_CLEAR);

        assertDummyAccount(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER);

        // User and default dummy account should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_4_CLEAR);
        assertDummyPassword(USER_JACK_USERNAME, USER_PASSWORD_5_CLEAR);

        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        if (isPasswordEncryption()) {
            assertAccountPasswordNotifications(2);
            assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_4_CLEAR);
            assertHasAccountPasswordNotification(RESOURCE_DUMMY_UGLY_NAME, USER_JACK_USERNAME, USER_JACK_EMPLOYEE_NUMBER);
        } else {
            assertSingleAccountPasswordNotification(RESOURCE_DUMMY_UGLY_NAME, USER_JACK_USERNAME, USER_JACK_EMPLOYEE_NUMBER);
        }
        assertNoUserPasswordNotifications();
    }

    /**
     * Modify both user and account password. Red dummy has a strong password mapping. User change should override account
     * change.
     */
    @Test
    public void test121ModifyJackPasswordUserAndAccountRed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        ProtectedStringType userPasswordPs1 = new ProtectedStringType();
        userPasswordPs1.setClearValue(USER_PASSWORD_1_CLEAR);
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, PASSWORD_VALUE_PATH, userPasswordPs1);

        ProtectedStringType userPasswordPs2 = new ProtectedStringType();
        userPasswordPs2.setClearValue(USER_PASSWORD_2_CLEAR);
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountJackRedOid, getDummyResourceObject(),
                PASSWORD_VALUE_PATH, userPasswordPs2);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, USER_JACK_FULL_NAME);

        // User should still have old password
        assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
        // Red account has the same account as user
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        // ... and default account has also the same password as user now. There was no other change on default dummy instance
        // so even the weak mapping took place.
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        // this one is not changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER);

        assertLiveLinks(userJack, 3);

        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        displayAccountPasswordNotifications();
        assertAccountPasswordNotifications(2);
        assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
        assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
        assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
    }

    /**
     * MID-3682
     */
    @Test
    public void test122ModifyAccountUglyJackPasswordBad() throws Exception {
        prepareTest();

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyAccountChangePassword(accountJackUglyOid, "#badPassword!", task, result);

        // THEN
        then();
        assertPartialError(result);

        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    /**
     * Jack employee number is mapped to ugly resource password.
     * Change employee number to something that does NOT comply with ugly resource password policy.
     * MID-3769
     */
    @Test
    public void test125ModifyJackEmployeeNumberBad() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result,
                USER_JACK_EMPLOYEE_NUMBER_NEW_BAD);

        // THEN
        assertPartialError(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);

        assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        // ugly password should be changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER);

        assertLiveLinks(userJack, 3);

        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    /**
     * Jack employee number is mapped to ugly resource password.
     * Change employee number to something that does comply with ugly resource password policy.
     * MID-3769
     */
    @Test
    public void test128ModifyJackEmployeeNumberGood() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result,
                USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);

        assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        // ugly password should be changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        assertLiveLinks(userJack, 3);

        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        assertSingleAccountPasswordNotification(RESOURCE_DUMMY_UGLY_NAME, USER_JACK_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);
        assertNoUserPasswordNotifications();
    }

    /**
     * Black resource has minimum password length constraint (enforced by midPoint).
     */
    @Test
    public void test130JackAssignAccountDummyBlack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_BLACK_OID, null, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertLiveLinks(userJack, 4);
        accountJackBlackOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_BLACK_OID);

        // Check account in dummy resource (black)
        assertDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        // Other resources should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        assertPasswordHistoryEntries(userJack);

        assertSingleAccountPasswordNotificationConditional(RESOURCE_DUMMY_BLACK_NAME, USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
        assertNoUserPasswordNotifications();
    }

    /**
     * MID-3682
     */
    @Test
    public void test132ModifyAccountBlackJackPasswordBad() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyAccountChangePassword(accountJackBlackOid, USER_PASSWORD_A_CLEAR, task, result);

        // THEN
        then();
        assertPartialError(result);

        assertDummyPasswordConditional(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    @Test
    public void test139JackUnassignAccountDummyBlack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_BLACK_OID, null, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertLiveLinks(userJack, 3);
        assertNotLinked(userJack, accountJackBlackOid);

        // Check account in dummy resource (black)
        assertNoDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        // Other resources should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        assertPasswordHistoryEntries(userJack);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    /**
     * Yellow resource has minimum password length constraint (enforced by resource).
     * But this time the password is OK.
     */
    @Test
    public void test140JackAssignAccountDummyYellow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertLiveLinks(userJack, 4);
        accountJackYellowOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_YELLOW_OID);

        // Check account in dummy resource (yellow)
        DummyAccount dummyAccountYellow = assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        displayDumpable("Yellow dummy account", dummyAccountYellow);
        assertDummyPasswordConditionalGenerated(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        // Other resources should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        assertPasswordHistoryEntries(userJack);

        displayAccountPasswordNotifications();
        assertSingleAccountPasswordNotificationConditionalGenerated(RESOURCE_DUMMY_YELLOW_NAME, USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
        assertNoUserPasswordNotifications();
    }

    /**
     * Yellow resource has minimum password length constraint. Change password to something shorter.
     * MID-3033, MID-2134
     */
    @Test
    public void test142ModifyUserJackPasswordAA() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_AA_CLEAR, task, result);

        // THEN
        then();
        assertPartialError(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertLiveLinks(userJack, 4);

        // Check account in dummy resource (yellow): password is too short for this, original password should remain there
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        // Check account in dummy resource (red)
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_AA_CLEAR);

        // User and default dummy account should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_AA_CLEAR);
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_AA_CLEAR);

        // this one is not changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        assertPasswordHistoryEntries(userJack);

        displayAccountPasswordNotifications();
        displayUserPasswordNotifications();
        assertAccountPasswordNotifications(2);
        assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_AA_CLEAR);
        assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_AA_CLEAR);
        assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_AA_CLEAR);
    }

    /**
     * Three headed monkey has no credentials. No password, nothing.
     * Just three heads.
     * MID-4631
     */
    @Test
    public void test150AssignMonkeyDummyAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        assignAccountToUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        getSingleLinkOid(userAfter);

        // Check account in dummy resource
        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME, USER_THREE_HEADED_MONKEY_FULL_NAME, true);
    }

    /**
     * Three headed monkey has no credentials. No password, nothing.
     * Just three heads.
     * MID-4631
     */
    @Test
    public void test152ModifyUserMonkeyPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_1_CLEAR, task, result);

        // THEN
        then();
        assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_1_CLEAR);
    }

    /**
     * Set up a password that will be illegal after password policy change.
     * MID-4791
     */
    @Test
    public void test154ModifyUserMonkeyPasswordA() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_A_CLEAR, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);
    }

    @Test
    public void test200ApplyPasswordPolicyHistoryLength() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        applyPasswordPolicy(PASSWORD_POLICY_GLOBAL_OID, getSecurityPolicyOid(), task, result);
        modifyObjectReplaceProperty(SecurityPolicyType.class, getSecurityPolicyOid(),
                ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_HISTORY_LENGTH),
                task, result, 3);

        // THEN
        then();
        assertSuccess(result);
    }

    // test202 is in subclasses. Different behavior for encryption and hashing

    /**
     * Unassign red account. Red resource has a strong password mapping. That would cause a lot
     * of trouble. We have tested that enough.
     */
    @Test
    public void test204UnassignAccountRed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertLiveLinks(userBefore, 4);

        // Red resource has disable-instead-of-delete. So we need to be brutal to get rid of the red account
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        ObjectDelta<ShadowType> shadowDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountJackRedOid);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta, shadowDelta), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 3);
        assertNotLinked(userAfter, accountJackRedOid);

        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account in dummy resource (yellow): password is too short for this, original password should remain there
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        // User and default dummy account should have unchanged passwords
        assertUserPassword(userAfter, USER_PASSWORD_AA_CLEAR);
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_AA_CLEAR);

        // this one is not changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        assertPasswordHistoryEntries(userAfter);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    /**
     * Reconcile user after password policy change. Nothing should be changed in the user.
     * Password history should still be empty. We haven't changed the password yet.
     * MID-3567
     */
    @Test
    public void test206ReconcileUserJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertLiveLinks(userBefore, 3);

        // WHEN
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 3);

        // Check account in dummy resource (yellow): password is too short for this, original password should remain there
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        // User and default dummy account should have unchanged passwords
        assertUserPassword(userAfter, USER_PASSWORD_AA_CLEAR);
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_AA_CLEAR);

        // this one is not changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        assertPasswordHistoryEntries(userAfter);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    /**
     * Change to password that complies with password policy.
     */
    @Test
    public void test210ModifyUserJackPasswordGood() throws Exception {
        doTestModifyUserJackPasswordSuccessWithHistory(
                USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
    }

    /**
     * Reconcile user. Nothing should be changed.
     * MID-3567
     */
    @Test
    public void test212ReconcileUserJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertJackPasswordsWithHistory(USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
    }

    /**
     * Recompute user. Nothing should be changed.
     * MID-3567
     */
    @Test
    public void test214RecomputeUserJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertJackPasswordsWithHistory(USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
    }

    /**
     * Change to password that violates the password policy (but is still OK for yellow resource).
     */
    @Test
    public void test220ModifyUserJackPasswordBadA() throws Exception {
        doTestModifyUserJackPasswordFailureWithHistory(
                USER_PASSWORD_1_CLEAR, USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
    }

    /**
     * Change to password that violates the password policy (but is still OK for yellow resource).
     * Use a different delta (container delta instead of property delta).
     * MID-2857
     */
    @Test
    public void test222ModifyUserJackPasswordBadContainer() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(USER_PASSWORD_1_CLEAR);
        PasswordType passwordType = new PasswordType();
        passwordType.setValue(userPasswordPs);

        ObjectDelta<UserType> objectDelta = prismContext.deltaFactory().object()
                .createModificationReplaceContainer(UserType.class, USER_JACK_OID,
                        ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD),
                        passwordType);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);

        try {
            // WHEN
            modelService.executeChanges(deltas, null, task, result);

            AssertJUnit.fail("Unexpected success");

        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        // THEN
        assertFailure(result);
        assertUserFriendlyMessage(result, "PolicyViolationException.message.credentials.password");

        assertJackPasswordsWithHistory(USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
        assertNoUserPasswordNotifications();
    }

    /**
     * Change to password that violates the password policy (contains username)
     * MID-1657
     */
    @Test
    public void test224ModifyUserJackPasswordBadJack() throws Exception {
        doTestModifyUserJackPasswordFailureWithHistory(
                USER_PASSWORD_JACK_CLEAR, USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
    }

    /**
     * Change to password that violates the password policy (contains family name)
     * MID-1657
     */
    @Test
    public void test226ModifyUserJackPasswordBadSparrow() throws Exception {
        doTestModifyUserJackPasswordFailureWithHistory(
                USER_PASSWORD_SPARROW_CLEAR, USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
    }

    /**
     * Change to password that complies with password policy. Again. See that
     * the change is applied correctly and that it is included in the history.
     */
    @Test
    public void test230ModifyUserJackPasswordGoodAgain() throws Exception {
        doTestModifyUserJackPasswordSuccessWithHistory(
                USER_PASSWORD_VALID_2, USER_PASSWORD_AA_CLEAR, USER_PASSWORD_VALID_1);
    }

    /**
     * Change to password that is good but it is the same as current password.
     */
    @Test
    public void test235ModifyUserJackPasswordGoodSameAsCurrent() throws Exception {
        doTestModifyUserJackPasswordFailureWithHistory(
                USER_PASSWORD_VALID_2, USER_PASSWORD_VALID_2, USER_PASSWORD_AA_CLEAR, USER_PASSWORD_VALID_1);
    }

    /**
     * Change to password that is good but it is already in the history.
     */
    @Test
    public void test236ModifyUserJackPasswordGoodInHistory() throws Exception {
        doTestModifyUserJackPasswordFailureWithHistory(
                USER_PASSWORD_VALID_1, USER_PASSWORD_VALID_2, USER_PASSWORD_AA_CLEAR, USER_PASSWORD_VALID_1);
    }

    /**
     * Change to password that is bad and it is already in the history.
     */
    @Test
    public void test237ModifyUserJackPasswordBadInHistory() throws Exception {
        doTestModifyUserJackPasswordFailureWithHistory(
                USER_PASSWORD_AA_CLEAR, USER_PASSWORD_VALID_2, USER_PASSWORD_AA_CLEAR, USER_PASSWORD_VALID_1);
    }

    /**
     * Change to password that complies with password policy. Again.
     * This time there are enough passwords in the history. So the history should
     * be truncated.
     */
    @Test
    public void test240ModifyUserJackPasswordGoodAgainOverHistory() throws Exception {
        doTestModifyUserJackPasswordSuccessWithHistory(
                USER_PASSWORD_VALID_3, USER_PASSWORD_VALID_1, USER_PASSWORD_VALID_2);
    }

    /**
     * Change to password that complies with password policy. Again.
     * This time there are enough passwords in the history. So the history should
     * be truncated.
     */
    @Test
    public void test241ModifyUserJackPasswordGoodAgainOverHistoryAgain() throws Exception {
        doTestModifyUserJackPasswordSuccessWithHistory(
                USER_PASSWORD_VALID_4, USER_PASSWORD_VALID_2, USER_PASSWORD_VALID_3);
    }

    /**
     * Reuse old password. Now the password should be out of the history, so
     * the system should allow its reuse.
     */
    @Test
    public void test248ModifyUserJackPasswordGoodReuse() throws Exception {
        doTestModifyUserJackPasswordSuccessWithHistory(
                USER_PASSWORD_VALID_1, USER_PASSWORD_VALID_3, USER_PASSWORD_VALID_4);
    }

    /**
     * When historyAllowExistingPasswordReuse is true and password history is ON,
     * existing password value (as found in UserType) can be set again as the new password.
     * Only until maxAge.
     */
    @Test
    public void test250ExistingPasswordReuseFail() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyObjectReplaceProperty(SecurityPolicyType.class, getSecurityPolicyOid(),
                ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_HISTORY_ALLOW_EXISTING_PASSWORD_REUSE),
                task, result, Boolean.FALSE);

        // WHEN
        when();

        try {
            modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_1, task, result); // modify with same pwd
            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        // THEN
        then();
        assertFailure(result);
    }

    @Test
    public void test251ExistingPasswordReuseFailExpired() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyObjectReplaceProperty(SecurityPolicyType.class, getSecurityPolicyOid(),
                ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_HISTORY_ALLOW_EXISTING_PASSWORD_REUSE),
                task, result, Boolean.TRUE);
        modifyObjectReplaceProperty(SecurityPolicyType.class, getSecurityPolicyOid(),
                ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_MAX_AGE),
                task, result, XmlTypeConverter.createDuration("P5D"));


        clock.overrideDuration("P6D"); //move system clock so password is expired

        // WHEN
        when();

        try {
            modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_1, task, result); // modify with same pwd
            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        // THEN
        then();
        assertFailure(result);
    }

    @Test
    public void test252ExistingPasswordReuseSucceed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        clock.resetOverride(); // reset system clock, so passoword is not expired
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_1, task, result); // modify with same pwd

        // THEN
        then();
        assertSuccess(result);
    }

    @Test
    public void test253ExistingPasswordReuseCheckMetadata() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        var modifyTimestampBefore = getPasswordModifyTimestampRequired(getUser(USER_JACK_OID));

        when();
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_1, task, result); // modify with same pwd

        then();
        var modifyTimestampAfter = getPasswordModifyTimestampRequired(getUser(USER_JACK_OID));

        assertEquals("modifyTimestamp changed when same pwd value was set", modifyTimestampBefore, modifyTimestampAfter);
    }

    private static XMLGregorianCalendar getPasswordModifyTimestampRequired(PrismObject<UserType> user) {
        var metadata = assertNonNull(FocusTypeUtil.getPasswordMetadata(user.asObjectable()), "no password metadata");
        var storageMetadata = assertNonNull(metadata.getStorage(), "no storage metadata");
        return assertNonNull(storageMetadata.getModifyTimestamp(), "no modifyTimestamp in storage metadata");
    }

    private void doTestModifyUserJackPasswordSuccessWithHistory(
            String newPassword, String... expectedPasswordHistory)
            throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserChangePassword(USER_JACK_OID, newPassword, task, result);

        // THEN
        assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        assertJackPasswordsWithHistory(newPassword, expectedPasswordHistory);

        displayAccountPasswordNotifications();
        assertAccountPasswordNotifications(2);
        assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, newPassword);
        assertHasAccountPasswordNotification(RESOURCE_DUMMY_YELLOW_NAME, USER_JACK_USERNAME, newPassword);
        assertSingleUserPasswordNotification(USER_JACK_USERNAME, newPassword);
    }

    private void doTestModifyUserJackPasswordFailureWithHistory(
            String newPassword, String oldPassword, String... expectedPasswordHistory)
            throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        try {
            // WHEN
            modifyUserChangePassword(USER_JACK_OID, newPassword, task, result);

            AssertJUnit.fail("Unexpected success");

        } catch (PolicyViolationException e) {
            // This is expected
            displayException("Exected exception", e);
        }

        // THEN
        assertFailure(result);

        assertJackPasswordsWithHistory(oldPassword, expectedPasswordHistory);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    private void assertJackPasswordsWithHistory(String expectedCurrentPassword, String... expectedPasswordHistory) throws Exception {
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertLiveLinks(userJack, 3);
        accountJackYellowOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_YELLOW_OID);

        assertUserPassword(userJack, expectedCurrentPassword);
        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, expectedCurrentPassword);

        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, expectedCurrentPassword);

        // this one is not changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        assertPasswordHistoryEntries(userJack, expectedPasswordHistory);
    }

    // TODO: add user with password that violates the policy

    /**
     * Create an org, and create two parentOrgRefs for jack (MID-3099).
     * Change to password that violates the password policy.
     */
    @Test
    public void test300TwoParentOrgRefs() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        assignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, null);
        assignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);

        UserType jack = getUser(USER_JACK_OID).asObjectable();
        display("jack", jack);
        assertEquals("Wrong # of parentOrgRefs", 2, jack.getParentOrgRef().size());

        ObjectDelta<OrgType> orgDelta = prismContext.deltaFor(OrgType.class)
                .item(OrgType.F_SECURITY_POLICY_REF).replace(itemFactory().createReferenceValue(SECURITY_POLICY_GOVERNOR_OID))
                .asObjectDelta(ORG_GOVERNOR_OFFICE_OID);
        executeChanges(orgDelta, null, task, result);

        OrgType govOffice = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID).asObjectable();
        display("governor's office", govOffice);
        assertEquals("Wrong OID of security policy ref", SECURITY_POLICY_GOVERNOR_OID, govOffice.getSecurityPolicyRef().getOid());

        try {
            // WHEN
            modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_1_CLEAR, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        // THEN
        assertFailure(result);
        assertUserFriendlyMessage(result, "PolicyViolationException.message.credentials.password");

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertLiveLinks(userJack, 3);

        // Make sure that the password is unchanged

        assertUserPassword(userJack, USER_PASSWORD_VALID_1);
        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);

        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);

        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    /**
     * Prepare system for password strength tests that follow.
     * Also unassign yellow resource (requires non-empty password), all orgs, and remove default password policy.
     */
    @Test
    public void test310PreparePasswordStrengthTests() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);
        unassignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, null, task, result);
        unassignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER, task, result);

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, task, result);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertLiveLinks(userAfter, 4);

        // password mapping is normal
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);

        // password mapping is strong
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);

        // password mapping is weak
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assert31xBluePasswordAfterAssignment(userAfter);

        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        // this one is not changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        if (isPasswordEncryption()) {
            assertAccountPasswordNotifications(2);
            assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_1);
            assertHasAccountPasswordNotification(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_1);

        } else {
            assertNoAccountPasswordNotifications();
        }
        assertNoUserPasswordNotifications();
    }

    @Test
    public void test312ChangeUserPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_2, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_2);
        assertLiveLinks(userAfter, 4);

        // password mapping is normal
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_2);

        // password mapping is strong
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_2);

        // password mapping is weak
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assert31xBluePasswordAfterPasswordChange(userAfter);

        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        // this one is not changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        displayAccountPasswordNotifications();
        if (isPasswordEncryption()) {
            assertAccountPasswordNotifications(2);
            assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
            assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
            // not BLUE, it already has a password
        } else {
            assertAccountPasswordNotifications(3);
            assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
            assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
            assertHasAccountPasswordNotification(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
        }
        assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
    }

    protected abstract void assert31xBluePasswordAfterAssignment(PrismObject<UserType> userAfter) throws Exception;

    protected abstract void assert31xBluePasswordAfterPasswordChange(PrismObject<UserType> userAfter) throws Exception;

    /*
     *  Policy prevents the password from being removed.
     */
    @Test
    public void test314RemovePasswordFail() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        setPasswordMinOccurs(1, task, result);

        try {
            // WHEN+THEN
            when();
            try {
                modifyUserReplace(USER_JACK_OID, PASSWORD_VALUE_PATH, task, result /*, no value */);
                fail("unexpected success");
            } catch (PolicyViolationException e) {
                assertMessage(e, "Provided password does not satisfy the policies: The value must be present.");
            }
        } finally {
            setPasswordMinOccurs(null, task, result);
        }
        assertNoUserPasswordNotifications();
    }

    private void setPasswordMinOccurs(Integer value, Task task, OperationResult result) throws CommonException {
        ObjectDelta<SecurityPolicyType> delta = prismContext.deltaFor(SecurityPolicyType.class)
                .item(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_MIN_OCCURS)
                .replace(value != null ? value.toString() : null)
                .asObjectDelta(getSecurityPolicyOid());
        executeChanges(delta, null, task, result);
    }

    /*
     *  Remove password. It should be removed from red resource as well. (MID-3111)
     */
    @Test
    public void test315RemovePassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, PASSWORD_VALUE_PATH, task, result /*, no value */);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        assertNull("User password is not null", userAfter.asObjectable().getCredentials().getPassword().getValue());
        assertLiveLinks(userAfter, 4);

        // password mapping is normal
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, null);

        // password mapping is strong
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null);

        // password mapping is weak here - so no change is expected
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assert31xBluePasswordAfterPasswordChange(userAfter);

        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        // this one is not changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        displayAllNotifications();
        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    /*
     *  User has no password. There is an inbound/generate mapping from default dummy
     *  resource. This should kick in now and set a random password.
     */
    @Test
    public void test316UserRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();

        // We need to make inbound mappings from dummy resource to be evaluated.
        //  - For the non-cached scenario, this is done automatically (in click #4) because we have full shadow from previous
        //    computations, so it is used.
        //  - For the cached scenario, we have the data, but (formally) we don't have the full (freshly loaded) shadow.
        //    We are careful (coward?) enough to skip inbounds even if cached data are there. So in order to make inbounds
        //    executed, let us invalidate the shadow cache, so that the full shadow is loaded.
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_OID);
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        assertNotNull("User password is still null", userAfter.asObjectable().getCredentials().getPassword().getValue());
        assertLiveLinks(userAfter, 4);

        // TODO: why are the resource passwords null ???

        // password mapping is normal
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, null);

        // password mapping is strong
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null);

        // password mapping is weak here - so no change is expected
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assert31xBluePasswordAfterPasswordChange(userAfter);

        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        // this one is not changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        displayAccountPasswordNotifications();
        assertNoAccountPasswordNotifications();
        assertUserPasswordNotifications(1);
    }

    /**
     * Change password to set predictable password instead of randomly-generated one.
     * That will be nicer for future tests.
     */
    @Test
    public void test318ChangeUserPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_3, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = assertUserAfter(USER_JACK_OID)
                .assertPassword(USER_PASSWORD_VALID_3, getPasswordStorageType())
                .assertLiveLinks(4)
                .getObject();

        // default password mapping is normal
        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertPassword(USER_PASSWORD_VALID_3)
                // Admin password reset, no runAs
                .assertLastModifier(null);

        // RED password mapping is strong
        assertDummyAccountByUsername(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME)
                .assertEnabled()
                .assertPassword(USER_PASSWORD_VALID_3)
                // and RED resource has no runAs capability
                .assertLastModifier(null);

        // password mapping is weak
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assert31xBluePasswordAfterPasswordChange(userAfter);

        // this one is not changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        displayAccountPasswordNotifications();
        assertAccountPasswordNotifications(2);
        assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_VALID_3);
        assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_3);
        // not BLUE, it already has a password
        assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_VALID_3);
    }

    @Test
    public void test320ChangeEmployeeNumber() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result, "emp0000");

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        //assertUserJack(userJack, "Jack Sparrow");            // we changed employeeNumber, so this would fail
        assertUserPassword(userAfter, USER_PASSWORD_VALID_3);

        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);
        assert31xBluePasswordAfterPasswordChange(userAfter);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);

        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "emp0000");

        assertSingleAccountPasswordNotification(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "emp0000");
        assertNoUserPasswordNotifications();
    }

    @Test
    public void test330RemoveEmployeeNumber() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        //assertUserJack(userJack, "Jack Sparrow");                    // we changed employeeNumber, so this would fail
        assertUserPassword(userAfter, USER_PASSWORD_VALID_3);

        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);
        assert31xBluePasswordAfterPasswordChange(userAfter);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);

        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's try to change it to another illegal password. Just for the sake of completeness.
     * MID-4791
     */
    @Test
    public void test340ModifyUserMonkeyPasswordAA() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        try {
            // WHEN
            when();

            modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_AA_CLEAR, task, result);

            assertNotReached();

        } catch (PolicyViolationException e) {
            // expected
        }

        // THEN
        then();
        assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Recompute. The password has not changed, there should be no error.
     * MID-4791
     */
    @Test
    public void test341RecomputeMonkey() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        recomputeUser(USER_THREE_HEADED_MONKEY_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Reconcile. The password has not changed, there should be no error.
     * MID-4791
     */
    @Test
    public void test342ReconcileMonkey() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        reconcileUser(USER_THREE_HEADED_MONKEY_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's make an innocent user-only change (not mapped to any resource).
     * Current password should be unaffected.
     * MID-4791
     */
    @Test
    public void test343ModifyUserMonkeyDescription() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);

        // WHEN
        when();
        modifyUserReplace(USER_THREE_HEADED_MONKEY_OID, UserType.F_DESCRIPTION, task, result, "Look behind you! A three-headed MONKEY!");

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's make a user modification that is mapped to a resource.
     * Current password should be unaffected.
     * MID-4791
     */
    @Test
    public void test344ModifyUserMonkeyLocality() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserReplace(USER_THREE_HEADED_MONKEY_OID, UserType.F_LOCALITY, task, result,
                PolyString.fromOrig("Monkey Island"));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertDummyAccountAttribute(null, USER_THREE_HEADED_MONKEY_NAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Monkey Island");
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Attempt to create account on blue resource should fail. Global password
     * policy is applied to resources as well.
     * MID-4791
     */
    @Test
    public void test345AssignMonkeyAccountBlue() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();

        assignAccountToUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);

        // THEN
        then();
        assertPartialError(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_THREE_HEADED_MONKEY_NAME);
    }

    /**
     * Get rid of that bad assignment.
     */
    @Test
    public void test346UnassignMonkeyAccountBlue() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();

        unassignAccountFromUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_THREE_HEADED_MONKEY_NAME);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's assign yellow account. Yellow resource has a minimum password length
     * (resource-enforced). Therefore assignment should fail.
     * MID-4791
     */
    @Test
    public void test347AssignMonkeyAccountYellow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();

        assignAccountToUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

        // THEN
        then();
        assertPartialError(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_THREE_HEADED_MONKEY_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_THREE_HEADED_MONKEY_NAME);
    }

    @Test
    public void test348UnassignMonkeyAccountYellow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();

        unassignAccountFromUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_THREE_HEADED_MONKEY_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_THREE_HEADED_MONKEY_NAME);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's assign pirate role to monkey. The role is modifying existing dummy
     * account. But it is not changing password, therefore it should all go well.
     * MID-4791
     */
    @Test
    public void test349AssignMonkeyPirate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        assignRole(USER_THREE_HEADED_MONKEY_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME, USER_THREE_HEADED_MONKEY_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_THREE_HEADED_MONKEY_NAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Monkey Island");
        assertDummyAccountAttribute(null, USER_THREE_HEADED_MONKEY_NAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's dissable the user. Account should be disabled as well.
     * But it is not changing password, therefore it should all go well.
     * MID-4791
     */
    @Test
    public void test350DisableMonkey() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserReplace(USER_THREE_HEADED_MONKEY_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME, USER_THREE_HEADED_MONKEY_FULL_NAME, false);
        assertDummyAccountAttribute(null, USER_THREE_HEADED_MONKEY_NAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Monkey Island");
        assertDummyAccountAttribute(null, USER_THREE_HEADED_MONKEY_NAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's re-enable the user. Account should be disabled as well.
     * But it is not changing password, therefore it should all go well.
     * MID-4791
     */
    @Test
    public void test351EnableMonkey() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserReplace(USER_THREE_HEADED_MONKEY_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME, USER_THREE_HEADED_MONKEY_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_THREE_HEADED_MONKEY_NAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Monkey Island");
        assertDummyAccountAttribute(null, USER_THREE_HEADED_MONKEY_NAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's unassign pirate role to monkey. The role is modifying existing dummy
     * account. But it is not changing password, therefore it should all go well.
     * MID-4791
     */
    @Test
    public void test352UnassignMonkeyPirate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        unassignRole(USER_THREE_HEADED_MONKEY_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME, USER_THREE_HEADED_MONKEY_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_THREE_HEADED_MONKEY_NAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Monkey Island");
        assertNoDummyAccountAttribute(null, USER_THREE_HEADED_MONKEY_NAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's try to change it to a valid password.
     * MID-4791
     */
    @Test
    public void test354ModifyUserMonkeyPasswordValid1() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_VALID_1, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
    }

    /**
     * Monkey has valid password now.
     * Let's make an innocent change again. Just for the sake of completeness.
     * MID-4791
     */
    @Test
    public void test355ModifyUserMonkeyDescriptionAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserReplace(USER_THREE_HEADED_MONKEY_OID, UserType.F_DESCRIPTION, task, result, "Look behind you! A three-headed MONKEY!");

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
    }

    /**
     * Add user with password and an assignment. check that the account is provisioned and has password.
     * Tests proper initial cleartext password handling in all cases.
     */
    @Test
    public void test400AddUserRappWithAssignment() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = PrismTestUtil.parseObject(USER_RAPP_FILE);
        AssignmentType assignmentType = createConstructionAssignment(RESOURCE_DUMMY_OID, null, null);
        UserType userBeforeType = userBefore.asObjectable();
        userBeforeType
                .fullName(createPolyStringType(USER_RAPP_FULLNAME))
                .emailAddress(USER_RAPP_EMAIL); // Make sure Rapp has e-mail address otherwise the notifications will not be sent to transport
        userBeforeType.getAssignment().add(assignmentType);
        setPassword(userBefore, USER_PASSWORD_VALID_1);
        display("User before", userBefore);

        // WHEN
        when();
        addObject(userBefore, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("User after", userAfter);
        String accountOid = getSingleLinkOid(userAfter);

        var accountShadow = getShadowRepo(accountOid);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_RAPP_USERNAME);
        assertShadowPurpose(accountShadow.getPrismObject(), true);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowPurpose(accountModel, true);

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertSingleAccountPasswordNotification(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertSingleUserPasswordNotification(USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
    }

    /**
     * Make sure recompute does not destroy the situation.
     */
    @Test
    public void test401UserRappRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        recomputeUser(USER_RAPP_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("User after", userAfter);
        String accountOid = getSingleLinkOid(userAfter);

        var accountShadow = getShadowRepo(accountOid);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_RAPP_USERNAME);
        assertShadowPurpose(accountShadow.getPrismObject(), true);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowPurpose(accountModel, true);

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    /**
     * add new assignment to the user, check that account is provisioned and has correct purpose
     */
    @Test
    public void test402AssignRappDummyRed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
        display("User before", userBefore);

        // WHEN
        when();
        assignAccountToUser(USER_RAPP_OID, RESOURCE_DUMMY_RED_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 2);

        String accountDefaultOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
        String accountRedOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
        var accountShadowRed = getShadowRepo(accountRedOid);
        display("Repo shadow RED", accountShadowRed);
        assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountShadowRed.getPrismObject(), false);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountModelRed, false);

        // DEFAULT shadows
        var accountShadow = getShadowRepo(accountDefaultOid);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowPurpose(accountShadow.getPrismObject(), null);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowPurpose(accountModel, null);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        displayAllNotifications();
        assertSingleAccountPasswordNotificationConditional(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertAccountActivationNotification(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME);
        assertNoUserPasswordNotifications();
    }

    /**
     * Make sure recompute does not destroy the situation.
     */
    @Test
    public void test403UserRappRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
        display("User before", userBefore);

        // WHEN
        when();
        recomputeUser(USER_RAPP_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 2);

        String accountDefaultOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
        String accountRedOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
        var accountShadowRed = getShadowRepo(accountRedOid);
        display("Repo shadow RED", accountShadowRed);
        assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountShadowRed.getPrismObject(), false);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountModelRed, false);

        // DEFAULT shadows
        var accountShadow = getShadowRepo(accountDefaultOid);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowPurpose(accountShadow.getPrismObject(), null);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowPurpose(accountModel, null);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    /**
     * initialize the account (password and purpose delta), check account password and purpose
     */
    @Test
    public void test404InitializeRappDummyRed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
        display("User before", userBefore);
        String accountRedOid = getLiveLinkRefOid(userBefore, RESOURCE_DUMMY_RED_OID);

        ObjectDelta<ShadowType> shadowDelta = createAccountInitializationDelta(accountRedOid, USER_PASSWORD_VALID_1);

        // WHEN
        when();
        executeChanges(shadowDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 2);

        String accountDefaultOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
        getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
        var accountShadowRed = getShadowRepo(accountRedOid);
        displayDumpable("Repo shadow RED", accountShadowRed);
        assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountShadowRed.getPrismObject(), null);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        displayDumpable("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountModelRed, null);

        // DEFAULT shadows
        var accountShadow = getShadowRepo(accountDefaultOid);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowPurpose(accountShadow.getPrismObject(), null);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowPurpose(accountModel, null);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertSingleAccountInitializationPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertNoUserPasswordNotifications();
    }

    /**
     * Make sure recompute does not destroy the situation.
     */
    @Test
    public void test405UserRappRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
        display("User before", userBefore);

        // WHEN
        when();
        recomputeUser(USER_RAPP_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 2);

        String accountDefaultOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
        String accountRedOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
        var accountShadowRed = getShadowRepo(accountRedOid);
        display("Repo shadow RED", accountShadowRed);
        assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountShadowRed, null);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountModelRed, null);

        // DEFAULT shadows
        var accountShadow = getShadowRepo(accountDefaultOid);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowPurpose(accountShadow, null);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowPurpose(accountModel, null);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    /**
     * add new assignment to the user. This resource has explicit purpose mapping.
     */
    @Test
    public void test410AssignRappDummyPurpose() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
        display("User before", userBefore);

        // WHEN
        when();
        assignAccountToUser(USER_RAPP_OID, RESOURCE_DUMMY_PURPOSE.oid, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 3);

        String accountOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_PURPOSE.oid);

        assertDummyAccount(RESOURCE_DUMMY_PURPOSE.name, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_PURPOSE.name, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        var shadow = getShadowRepo(accountOid);
        display("Repo shadow PURPOSE", shadow);
        assertAccountShadowRepo(shadow, accountOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_PURPOSE.name));
        assertShadowPurpose(shadow, false);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertSingleAccountPasswordNotificationConditional(RESOURCE_DUMMY_PURPOSE.name, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertNoUserPasswordNotifications();
    }

    @Test
    public void test412InitializeRappDummyPurpose() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
        display("User before", userBefore);
        String accountOid = getLiveLinkRefOid(userBefore, RESOURCE_DUMMY_PURPOSE.oid);

        ObjectDelta<ShadowType> shadowDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountOid);
        ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(USER_PASSWORD_VALID_1);
        shadowDelta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, passwordPs);
        shadowDelta.addModificationReplaceProperty(ShadowType.F_PURPOSE, ShadowPurposeType.REGULAR);

        // WHEN
        when();
        executeChanges(shadowDelta, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 3);

        accountOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_PURPOSE.oid);

        assertDummyAccount(RESOURCE_DUMMY_PURPOSE.name, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_PURPOSE.name, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        var accountShadowPurpose = getShadowRepo(accountOid);
        display("Repo shadow PURPOSE", accountShadowPurpose);
        assertAccountShadowRepo(accountShadowPurpose, accountOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_PURPOSE.name));
        assertShadowPurpose(accountShadowPurpose, null);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
        String accountRedOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        var accountShadowRed = getShadowRepo(accountRedOid);
        display("Repo shadow RED", accountShadowRed);
        assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountShadowRed, null);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountModelRed, null);

        // DEFAULT shadows
        String accountDefaultOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_OID);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowPurpose(accountModel, null);

        var accountShadow = getShadowRepo(accountDefaultOid);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowPurpose(accountShadow, null);

        assertSingleAccountInitializationPasswordNotification(RESOURCE_DUMMY_PURPOSE.name, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertNoUserPasswordNotifications();
    }

    @Test
    public void test414UserRappRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
        display("User before", userBefore);

        // WHEN
        when();
        recomputeUser(USER_RAPP_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 3);

        assertDummyAccount(RESOURCE_DUMMY_PURPOSE.name, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_PURPOSE.name, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // PURPOSE shadows

        String accountPurposeOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_PURPOSE.oid);

        var accountShadowPurpose = getShadowRepo(accountPurposeOid);
        display("Repo shadow PURPOSE", accountShadowPurpose);
        assertAccountShadowRepo(accountShadowPurpose, accountPurposeOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_PURPOSE.name));
        assertShadowPurpose(accountShadowPurpose, null);

        // RED shadows
        String accountRedOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        var accountShadowRed = getShadowRepo(accountRedOid);
        display("Repo shadow RED", accountShadowRed);
        assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountShadowRed, null);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountModelRed, null);

        // DEFAULT shadows
        String accountDefaultOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_OID);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowPurpose(accountModel, null);

        var accountShadow = getShadowRepo(accountDefaultOid);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowPurpose(accountShadow, null);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    @Test
    public void test416UserRappSubtypeWreck() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
        display("User before", userBefore);

        // WHEN
        when();
        modifyUserReplace(USER_RAPP_OID, UserType.F_SUBTYPE, task, result, "WRECK");

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 3);

        assertDummyAccount(RESOURCE_DUMMY_PURPOSE.name, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_PURPOSE.name, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // PURPOSE shadows

        String accountPurposeOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_PURPOSE.oid);

        var accountShadowPurpose = getShadowRepo(accountPurposeOid);
        display("Repo shadow PURPOSE", accountShadowPurpose);
        assertAccountShadowRepo(accountShadowPurpose, accountPurposeOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_PURPOSE.name));

        // RED shadows
        String accountRedOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        var accountShadowRed = getShadowRepo(accountRedOid);
        display("Repo shadow RED", accountShadowRed);
        assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountShadowRed, null);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowPurpose(accountModelRed, null);

        // DEFAULT shadows
        String accountDefaultOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_OID);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowPurpose(accountModel, null);

        var accountShadow = getShadowRepo( accountDefaultOid);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowPurpose(accountShadow, null);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();

    }

    /**
     * Add user without a password, but with an assignment. Check that the account is provisioned.
     * The account will always be in a proposed state, even if password encryption is used.
     * The default purpose algorithm does not consider generated password to be good enough for the account to be active.
     * MID-5629
     */
    @Test
    public void test420AddUserDrakeWithAssignment() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = PrismTestUtil.parseObject(USER_DRAKE_FILE);
        UserType userBeforeType = userBefore.asObjectable();
        userBeforeType.getAssignment().add(createConstructionAssignment(RESOURCE_DUMMY_ORANGE_OID, null, null));
        assertNull("Unexpected credentials", userBeforeType.getCredentials());
        display("User before", userBefore);

        // WHEN
        when();
        addObject(userBefore, task, result);

        // THEN
        then();
        assertSuccess(result);

        String accountOid = assertUserAfter(USER_DRAKE_OID)
                .singleLink()
                .getOid();

        assertRepoShadow(accountOid)
                // Purpose is always 'incomplete', even for encrypted passwords.
                // The default purpose algorithm does not consider generated password to be good enough for the account to be active.
                .assertPurpose(ShadowPurposeType.INCOMPLETE);

        assertModelShadow(accountOid)
                .assertPurpose(ShadowPurposeType.INCOMPLETE);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_DRAKE_USERNAME, USER_DRAKE_FULLNAME, true);
    }

    /**
     * MID-4397
     */
    @Test
    public void test500JackAssignResourceSouvenir() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // preconditions
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignedAccount(userBefore, RESOURCE_DUMMY_OID);
        assertAssignedAccount(userBefore, RESOURCE_DUMMY_RED_OID);
        assertAssignedAccount(userBefore, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedAccount(userBefore, RESOURCE_DUMMY_UGLY_OID);
        assertAssignments(userBefore, 4);
        assertAccount(userBefore, RESOURCE_DUMMY_OID);
        assertAccount(userBefore, RESOURCE_DUMMY_RED_OID);
        accountJackBlueOid = assertAccount(userBefore, RESOURCE_DUMMY_BLUE_OID);
        assertAccount(userBefore, RESOURCE_DUMMY_UGLY_OID);
        assertLiveLinks(userBefore, 4);
        assertUserPassword(userBefore, USER_PASSWORD_VALID_3);

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_SOUVENIR_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 5);
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_OID);
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_RED_OID);
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_UGLY_OID);
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_SOUVENIR_OID);
        assertLiveLinks(userAfter, 5);
        assertAccount(userAfter, RESOURCE_DUMMY_OID);
        assertAccount(userAfter, RESOURCE_DUMMY_RED_OID);
        assertAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAccount(userAfter, RESOURCE_DUMMY_UGLY_OID);
        accountJackSouvenirOid = assertAccount(userAfter, RESOURCE_DUMMY_SOUVENIR_OID);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_3);

        PrismObject<ShadowType> shadowPasswordCachingModel = getShadowModel(accountJackSouvenirOid);
        assertShadowPurpose(shadowPasswordCachingModel, false);

        assertDummyPasswordConditional(RESOURCE_DUMMY_SOUVENIR_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);

        displayAllNotifications();
        assertSingleAccountPasswordNotificationConditional(RESOURCE_DUMMY_SOUVENIR_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_3);
        assertAccountActivationNotification(RESOURCE_DUMMY_SOUVENIR_NAME, USER_JACK_USERNAME);
        assertNoUserPasswordNotifications();
    }

    /**
     * MID-4397
     */
    @Test
    public void test502JackInitializeAccountSouvenir() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        ObjectDelta<ShadowType> shadowDelta = createAccountInitializationDelta(accountJackSouvenirOid, PASSWORD_ALLIGATOR);

        // WHEN
        when();
        executeChanges(shadowDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 5);
        assertLiveLinks(userAfter, 5);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_3);

        var shadowPasswordCachingRepo = getShadowRepo(accountJackSouvenirOid);
        displayDumpable("Shadow repo", shadowPasswordCachingRepo);
        assertShadowPurpose(shadowPasswordCachingRepo.getPrismObject(), null);
        assertCachedResourcePassword(shadowPasswordCachingRepo, PASSWORD_ALLIGATOR);

        assertDummyPassword(RESOURCE_DUMMY_SOUVENIR_NAME, ACCOUNT_JACK_DUMMY_USERNAME, PASSWORD_ALLIGATOR);

        ItemComparisonResult comparisonResult = provisioningService.compare(ShadowType.class, accountJackSouvenirOid, SchemaConstants.PATH_PASSWORD_VALUE,
                PASSWORD_ALLIGATOR, task, result);
        assertEquals("Wrong comparison result", ItemComparisonResult.MATCH, comparisonResult);

        // TODO
//        assertSingleAccountInitializationPasswordNotification(RESOURCE_DUMMY_PASSWORD_CACHING_NAME, USER_JACK_USERNAME, PASSWORD_Alligator);

        assertNoUserPasswordNotifications();
    }

    /**
     * MID-4397
     */
    @Test
    public void test510JackAssignResourceMaverick() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // preconditions
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 5);
        assertLiveLinks(userBefore, 5);
        assertUserPassword(userBefore, USER_PASSWORD_VALID_3);

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_MAVERICK_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 6);
        assertLiveLinks(userAfter, 6);
        accountJackMaverickOid = assertAccount(userAfter, RESOURCE_DUMMY_MAVERICK_OID);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_3);

        assertDummyPasswordConditional(RESOURCE_DUMMY_MAVERICK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);

        displayAllNotifications();
        assertSingleAccountPasswordNotificationConditional(RESOURCE_DUMMY_MAVERICK_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_3);
        assertAccountActivationNotification(RESOURCE_DUMMY_MAVERICK_NAME, USER_JACK_USERNAME);
        assertNoUserPasswordNotifications();
    }

    /**
     * Attempt to initialize account with password which is the same as on the souvenir
     * resource should fail. Maverick resource password policy states that the password
     * must be different that the password on souvenir resource.
     * MID-4397
     */
    @Test
    public void test512JackInitializeAccountMaverickAlligator() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        ObjectDelta<ShadowType> shadowDelta = createAccountInitializationDelta(accountJackMaverickOid, PASSWORD_ALLIGATOR);

        // WHEN
        when();
        executeChanges(shadowDelta, null, task, result);

        // THEN
        then();
        assertPartialError(result);
        assertUserFriendlyMessage(result, "PolicyViolationException.message.projectionPassword");

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 6);
        assertLiveLinks(userAfter, 6);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_3);

        assertDummyPasswordConditional(RESOURCE_DUMMY_MAVERICK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);
        assertNoUserPasswordNotifications();

        // TODO
//        assertSingleAccountInitializationPasswordNotification(RESOURCE_DUMMY_PASSWORD_CACHING_NAME, USER_JACK_USERNAME, PASSWORD_Alligator);
    }

    /**
     * Attempt to initialize account with a different password should pass.
     * MID-4397
     */
    @Test
    public void test514JackInitializeAccountMaverickCrododile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        ObjectDelta<ShadowType> shadowDelta = createAccountInitializationDelta(accountJackMaverickOid, PASSWORD_CROCODILE);

        // WHEN
        when();
        executeChanges(shadowDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 6);
        assertLiveLinks(userAfter, 6);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_3);

        var shadowMaverickRepo = getShadowRepo(accountJackMaverickOid);
        displayDumpable("Shadow repo", shadowMaverickRepo);
        assertShadowPurpose(shadowMaverickRepo.getPrismObject(), null);

        assertDummyPassword(RESOURCE_DUMMY_MAVERICK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, PASSWORD_CROCODILE);

        // TODO
//        assertSingleAccountInitializationPasswordNotification(RESOURCE_DUMMY_PASSWORD_CACHING_NAME, USER_JACK_USERNAME, PASSWORD_Alligator);
        assertNoUserPasswordNotifications();
    }

    /**
     * Attempt to change account password to a conflicting one. It should fail.
     * MID-4397
     */
    @Test
    public void test516JackChangePasswordResourceMaverickAlligator() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyAccountChangePassword(accountJackMaverickOid, PASSWORD_ALLIGATOR, task, result);

        // THEN
        then();
        assertPartialError(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 6);
        assertLiveLinks(userAfter, 6);

        assertDummyPassword(RESOURCE_DUMMY_MAVERICK_NAME, USER_JACK_USERNAME, PASSWORD_CROCODILE);
        assertNoUserPasswordNotifications();
    }

    /**
     * Attempt to change account password to non conflicting one. It should pass.
     * MID-4397
     */
    @Test
    public void test518JackChangePasswordResourceMaverickGiantLizard() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyAccountChangePassword(accountJackMaverickOid, PASSWORD_GIANT_LIZARD, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 6);
        assertLiveLinks(userAfter, 6);

        assertDummyPassword(RESOURCE_DUMMY_MAVERICK_NAME, USER_JACK_USERNAME, PASSWORD_GIANT_LIZARD);
        assertNoUserPasswordNotifications();
    }

    /**
     * MID-4397
     */
    @Test
    public void test530JackUnassignResourceSouvenir() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // preconditions
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 6);
        assertLiveLinks(userBefore, 6);

        // WHEN
        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_SOUVENIR_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 5);
        assertAssignedNoAccount(userAfter, RESOURCE_DUMMY_SOUVENIR_OID);
        assertLiveLinks(userAfter, 5);
        assertNoUserPasswordNotifications();
    }

    /**
     * Make it harder for the next test (test535ModifyUserJackPasswordAlligator).
     * We set conflicting password on blue resource. But the password policy
     * should NOT check blue resource. So the check should pass anyway.
     * MID-4397
     */
    @Test
    public void test532JackChangePasswordResourceBlueAlligator() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 5);
        assertLiveLinks(userBefore, 5);
        accountJackBlueOid = assertAccount(userBefore, RESOURCE_DUMMY_BLUE_OID);

        // WHEN
        when();
        modifyAccountChangePassword(accountJackBlueOid, PASSWORD_ALLIGATOR, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 5);
        assertAssignedNoAccount(userAfter, RESOURCE_DUMMY_SOUVENIR_OID);
        assertLiveLinks(userAfter, 5);

        assertDummyPassword(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, PASSWORD_ALLIGATOR);
        assertNoUserPasswordNotifications();
    }

    /**
     * Additional check: check that the Alligator password would be otherwise legal for user and
     * maverick resource. This is additional check that it was really the presence of souvenir
     * resource that caused the policy violation.
     * MID-4397
     */
    @Test
    public void test535ModifyUserJackPasswordAlligator() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserChangePassword(USER_JACK_OID, PASSWORD_ALLIGATOR, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 5);
        assertLiveLinks(userAfter, 5);
        assertUserPassword(userAfter, PASSWORD_ALLIGATOR);

        assertDummyPassword(RESOURCE_DUMMY_MAVERICK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, PASSWORD_ALLIGATOR);
        assertSingleUserPasswordNotification(USER_JACK_USERNAME, PASSWORD_ALLIGATOR);
    }

    /**
     * MID-4397
     */
    @Test
    public void test539JackUnassignResourceMaverick() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // preconditions
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 5);
        assertLiveLinks(userBefore, 5);

        // WHEN
        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_MAVERICK_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 4);
        assertAssignedNoAccount(userAfter, RESOURCE_DUMMY_MAVERICK_OID);
        assertAssignedNoAccount(userAfter, RESOURCE_DUMMY_SOUVENIR_OID);
        assertLiveLinks(userAfter, 4);
        assertNoUserPasswordNotifications();
    }

    /**
     * MID-4507
     */
    @Test
    public void test550JackManyPasswordChangesClear() throws Exception {
        testJackManyPasswordChanges("TesT550x", null);
    }

    /**
     * MID-4507
     */
    @Test
    public void test552JackManyPasswordChangesEncrypted() throws Exception {
        testJackManyPasswordChanges("TesT552x", CredentialsStorageTypeType.ENCRYPTION);
    }

    /**
     * MID-4507
     */
    public void testJackManyPasswordChanges(String passwordPrefix, CredentialsStorageTypeType storageType) throws Exception {

        // GIVEN
        prepareTest();

        // preconditions
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 4);
        assertLiveLinks(userBefore, 4);

        for (int i = 1; i < 10; i++) {
            testJackManyPasswordChangesAttempt(passwordPrefix, storageType, i);
        }
    }

    private void testJackManyPasswordChangesAttempt(
            String passwordPrefix, CredentialsStorageTypeType storageType, int i)
            throws Exception {
        Task task = createTask("testJackManyPasswordChangesAttempt-" + i);
        OperationResult result = task.getResult();

        String newPassword = passwordPrefix + i;
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(newPassword);
        if (storageType == CredentialsStorageTypeType.ENCRYPTION) {
            protector.encrypt(userPasswordPs);
        }

        // WHEN
        when("iteration-" + i);
        modifyUserReplace(USER_JACK_OID, PASSWORD_VALUE_PATH, task, result, userPasswordPs);

        // THEN
        then("iteration-" + i);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserPassword(userAfter, newPassword);
        assertAssignments(userAfter, 4);
        assertLiveLinks(userAfter, 4);
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, newPassword);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, newPassword);
    }

    /**
     * Jack changing his own password. He does it as an admin
     * and there is no old password specified. RunAs should NOT be used.
     * <p>
     * This also sets predictable password for next test.
     * <p>
     * MID-4661
     */
    @Test
    public void test560ChangeJackPasswordSuperuser() throws Exception {
        // GIVEN
        prepareTest();

        assignRole(USER_JACK_OID, ROLE_SUPERUSER.oid);

        // preconditions
        assertUserBefore(USER_JACK_OID)
                .displayWithProjections()
                .assertLiveLinks(4);

        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertLastModifier(null);

        login(USER_JACK_USERNAME);

        Task task = createTask(getSecurityContextPrincipal());
        task.setChannel(SchemaConstants.CHANNEL_USER_URI);
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_4, task, result);

        // THEN
        then();
        login(USER_ADMINISTRATOR_USERNAME);
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .assertPassword(USER_PASSWORD_VALID_4, getPasswordStorageType())
                .assertLiveLinks(4);

        // default password mapping is normal
        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertPassword(USER_PASSWORD_VALID_4)
                .assertLastModifier(null);

        // RED password mapping is strong
        assertDummyAccountByUsername(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME)
                .assertEnabled()
                .assertPassword(USER_PASSWORD_VALID_4)
                // and RED resource has no runAs capability
                .assertLastModifier(null);

        // BLUE password mapping is weak, we do not really care about password change here
        // we do not really care about ugly resource either

        displayAccountPasswordNotifications();
        assertAccountPasswordNotifications(2);
        assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_VALID_4);
        assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_4);
        // not BLUE, it already has a password
        assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_VALID_4);
    }

    /**
     * Self-service password change. User's own identity should be used
     * to change password on resource that have runAs capability.
     * <p>
     * MID-4661
     */
    @Test
    public void test562ChangeJackPasswordSelfService() throws Exception {
        // GIVEN
        prepareTest();

        // preconditions
        assertUserBefore(USER_JACK_OID)
                .displayWithProjections()
                .assertLiveLinks(4);

        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertPassword(USER_PASSWORD_VALID_4)
                .assertLastModifier(null);

        login(USER_JACK_USERNAME);

        Task task = createTask(getSecurityContextPrincipal());
        task.setChannel(SchemaConstants.CHANNEL_SELF_SERVICE_URI);
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createOldNewPasswordDelta(USER_JACK_OID,
                USER_PASSWORD_VALID_4, USER_PASSWORD_VALID_5);

        // WHEN
        when();
        executeChanges(objectDelta, null, task, result);

        // THEN
        then();
        login(USER_ADMINISTRATOR_USERNAME);
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .assertPassword(USER_PASSWORD_VALID_5, getPasswordStorageType())
                .assertLiveLinks(4);

        // default password mapping is normal
        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertPassword(USER_PASSWORD_VALID_5)
                .assertLastModifier(ACCOUNT_JACK_DUMMY_USERNAME);

        // RED password mapping is strong
        assertDummyAccountByUsername(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME)
                .assertEnabled()
                .assertPassword(USER_PASSWORD_VALID_5)
                // and RED resource has no runAs capability
                .assertLastModifier(null);

        // BLUE password mapping is weak, we do not really care about password change here
        // we do not really care about ugly resource either

        displayAccountPasswordNotifications();
        assertAccountPasswordNotifications(2);
        assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_VALID_5);
        assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_5);
        // not BLUE, it already has a password
        assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_VALID_5);
    }

    /**
     * Admin is changing Jack's password.
     * Old password is (strangely) specified.
     * But as this is not user changing its own password then RunAs
     * should NOT be used.
     * <p>
     * MID-4661
     */
    @Test
    public void test564ChangeJackPasswordAdmin() throws Exception {
        // GIVEN
        prepareTest();

        // preconditions
        assertUserBefore(USER_JACK_OID)
                .displayWithProjections()
                .assertLiveLinks(4);

        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertPassword(USER_PASSWORD_VALID_5)
                .assertLastModifier(ACCOUNT_JACK_DUMMY_USERNAME);

        login(USER_ADMINISTRATOR_USERNAME);

        Task task = createTask(getSecurityContextPrincipal());
        task.setChannel(SchemaConstants.CHANNEL_USER_URI);
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createOldNewPasswordDelta(USER_JACK_OID,
                USER_PASSWORD_VALID_5, USER_PASSWORD_VALID_6);

        // WHEN
        when();
        executeChanges(objectDelta, null, task, result);

        // THEN
        then();
        login(USER_ADMINISTRATOR_USERNAME);
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .assertPassword(USER_PASSWORD_VALID_6, getPasswordStorageType())
                .assertLiveLinks(4);

        // default password mapping is normal
        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertPassword(USER_PASSWORD_VALID_6)
                .assertLastModifier(null);

        // RED password mapping is strong
        assertDummyAccountByUsername(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME)
                .assertEnabled()
                .assertPassword(USER_PASSWORD_VALID_6)
                // and RED resource has no runAs capability
                .assertLastModifier(null);

        // BLUE password mapping is weak, we do not really care about password change here
        // we do not really care about ugly resource either

        displayAccountPasswordNotifications();
        assertAccountPasswordNotifications(2);
        assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_VALID_6);
        assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_6);
        // not BLUE, it already has a password
        assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_VALID_6);
    }

    private ObjectDelta<ShadowType> createAccountInitializationDelta(String accountOid, String newAccountPassword) {
        ObjectDelta<ShadowType> shadowDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountOid);
        ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(newAccountPassword);
        shadowDelta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, passwordPs);
        shadowDelta.addModificationReplaceProperty(ShadowType.F_PURPOSE, ShadowPurposeType.REGULAR);
        return shadowDelta;
    }

    protected void assertDummyPassword(String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException, InterruptedException {
        assertDummyPassword(null, userId, expectedClearPassword);
    }

    protected void assertDummyPasswordConditional(String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException, InterruptedException {
        if (isPasswordEncryption()) {
            assertDummyPassword(null, userId, expectedClearPassword);
        }
    }

    protected void assertDummyPasswordConditional(String instance, String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException, InterruptedException {
        if (isPasswordEncryption()) {
            super.assertDummyPassword(instance, userId, expectedClearPassword);
        }
    }

    protected void assertDummyPasswordConditionalGenerated(String instance, String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException, InterruptedException {
        if (isPasswordEncryption()) {
            super.assertDummyPassword(instance, userId, expectedClearPassword);
        } else {
            assertDummyPasswordNotEmpty(instance, userId);
        }
    }

    protected void assertSingleAccountPasswordNotificationConditional(String dummyResourceName, String username, String password) {
        if (isPasswordEncryption()) {
            assertSingleAccountPasswordNotification(dummyResourceName, username, password);
        }
    }

    protected void assertSingleAccountPasswordNotificationConditionalGenerated(String dummyResourceName, String username, String password) {
        if (isPasswordEncryption()) {
            assertSingleAccountPasswordNotification(dummyResourceName, username, password);
        } else {
            assertSingleAccountPasswordNotificationGenerated(dummyResourceName, username);
        }
    }

    private void assertSingleAccountInitializationPasswordNotification(String dummyResourceName, String username, String password) {
        assertSingleAccountPasswordNotification(dummyResourceName, username, password);
    }

    protected abstract void assertAccountActivationNotification(String dummyResourceName, String username);

    private void assertShadowPurpose(RawRepoShadow shadow, boolean focusCreated) {
        assertShadowPurpose(shadow.getPrismObject(), focusCreated);
    }

    protected abstract void assertShadowPurpose(PrismObject<ShadowType> shadow, boolean focusCreated);

    private void assertShadowPurpose(RawRepoShadow shadow, ShadowPurposeType expected) {
        assertShadowPurpose(shadow.getPrismObject(), expected);
    }

    void assertShadowPurpose(PrismObject<ShadowType> shadow, ShadowPurposeType expected) {
        if (expected == null) {
            ShadowPurposeType actual = shadow.asObjectable().getPurpose();
            if (actual != null && actual != ShadowPurposeType.REGULAR) {
                fail("Expected default purpose for " + shadow + ", but was " + actual);
            }
        } else {
            PrismAsserts.assertPropertyValue(shadow, ShadowType.F_PURPOSE, expected);
        }
    }

    private void assertShadowPasswordMetadata(PrismObject<ShadowType> shadow,
            XMLGregorianCalendar startCal, XMLGregorianCalendar endCal, boolean clearPasswordAvailable, boolean passwordCreated) {
        if (!clearPasswordAvailable && getPasswordStorageType() == CredentialsStorageTypeType.HASHING) {
            return;
        }
        assertShadowPasswordMetadata(shadow, passwordCreated, startCal, endCal, USER_ADMINISTRATOR_OID, SchemaConstants.CHANNEL_USER_URI);
    }

    // 9XX tests: Password minimal age, no password, etc.

    /**
     * Let's have a baseline for other 90x tests.
     */
    @Test
    public void test900ModifyUserElainePassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_ELAINE_OID);
        display("User before", userBefore);

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserChangePassword(USER_ELAINE_OID, USER_PASSWORD_VALID_1, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_ELAINE_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);

        displayAccountPasswordNotifications();
        assertAccountPasswordNotifications(3);
        assertHasAccountPasswordNotification(null, USER_ELAINE_USERNAME, USER_PASSWORD_VALID_1);
        assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_ELAINE_USERNAME, USER_PASSWORD_VALID_1);
        assertHasAccountPasswordNotification(RESOURCE_DUMMY_BLUE_NAME, USER_ELAINE_USERNAME, USER_PASSWORD_VALID_1);
    }

    @Test
    public void test902SetPasswordMinAge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        modifyObjectReplaceProperty(SecurityPolicyType.class, getSecurityPolicyOid(),
                ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_MIN_AGE),
                task, result, XmlTypeConverter.createDuration("PT10M"));

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<SecurityPolicyType> securityPolicy = getObject(SecurityPolicyType.class, getSecurityPolicyOid());
        display("Security policy after", securityPolicy);
        PrismAsserts.assertPropertyValue(securityPolicy,
                ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_MIN_AGE),
                XmlTypeConverter.createDuration("PT10M"));

        assertNoAccountPasswordNotifications();
    }

    /**
     * Password modification is obviously before the password minAge has passed.
     * Therefore this should fail.
     */
    @Test
    public void test904ModifyUserElainePasswordAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        try {

            // WHEN
            modifyUserChangePassword(USER_ELAINE_OID, USER_PASSWORD_VALID_2, task, result);

            assertNotReached();

        } catch (PolicyViolationException e) {

        }

        // THEN
        result.computeStatus();
        TestUtil.assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_ELAINE_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
    }

    @Test
    public void test906ModifyUserElainePasswordLater() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        clock.overrideDuration("PT15M");

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserChangePassword(USER_ELAINE_OID, USER_PASSWORD_VALID_3, task, result);

        // THEN
        assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_ELAINE_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_3);

        displayAccountPasswordNotifications();
        assertAccountPasswordNotifications(2);
        assertHasAccountPasswordNotification(null, USER_ELAINE_USERNAME, USER_PASSWORD_VALID_3);
        assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_ELAINE_USERNAME, USER_PASSWORD_VALID_3);
        // BLUE resource already has a password
        assertSingleUserPasswordNotification(USER_ELAINE_USERNAME, USER_PASSWORD_VALID_3);

    }

    /*
     *  Policy prevents creating user with no password.
     */
    @Test
    public void test910AddUserWithNoPasswordFail() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        setPasswordMinOccurs(1, task, result);

        try {
            // WHEN+THEN
            when();
            try {
                UserType user = new UserType(prismContext).name("passwordless");
                addObject(user, task, result);
                fail("unexpected success");
            } catch (PolicyViolationException e) {
                assertMessage(e, "Provided password does not satisfy the policies: The value must be present.");
            }
        } finally {
            setPasswordMinOccurs(null, task, result);
        }
        assertNoUserPasswordNotifications();
    }

    /**
     * MID-4593
     */
    @Test
    public void test920AddCredentials() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        setPasswordMinOccurs(0, task, result);

        UserType alice = new UserType(prismContext).name("alice");
        addObject(alice, task, result);

        PrismObject<UserType> aliceReloaded = getUser(alice.getOid());
        assertNull("alice has credentials", aliceReloaded.asObjectable().getCredentials());

        // WHEN
        ProtectedStringType value = new ProtectedStringType();
        value.setClearValue(PASSWORD_HELLO_WORLD);
        CredentialsType credentials = new CredentialsType(prismContext)
                .beginPassword()
                .value(value)
                .end();
        ObjectDelta<UserType> objectDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS).add(credentials)
                .asObjectDelta(alice.getOid());

        executeChanges(objectDelta, null, task, result);

        // THEN
        PrismObject<UserType> aliceAfter = getUser(alice.getOid());
        display("alice after credentials add", aliceAfter);
        assertUserPassword(aliceAfter, PASSWORD_HELLO_WORLD);
        assertPasswordCreateMetadata(aliceAfter);
    }

    /**
     * MID-4593
     */
    @Test
    public void test922ReplaceCredentials() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        setPasswordMinOccurs(0, task, result);

        UserType user = new UserType(prismContext).name("bob");
        addObject(user, task, result);

        PrismObject<UserType> userReloaded = getUser(user.getOid());
        assertNull("user has credentials", userReloaded.asObjectable().getCredentials());

        // WHEN
        ProtectedStringType value = new ProtectedStringType();
        value.setClearValue(PASSWORD_HELLO_WORLD);
        CredentialsType credentials = new CredentialsType(prismContext)
                .beginPassword()
                .value(value)
                .end();
        ObjectDelta<UserType> objectDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS).replace(credentials)
                .asObjectDelta(user.getOid());

        executeChanges(objectDelta, null, task, result);

        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        display("user after operation", userAfter);
        assertUserPassword(userAfter, PASSWORD_HELLO_WORLD);
        assertPasswordCreateMetadata(userAfter);
    }

    /**
     * MID-4593
     */
    @Test
    public void test924AddPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        setPasswordMinOccurs(0, task, result);

        UserType user = new UserType(prismContext).name("charlie");
        addObject(user, task, result);

        PrismObject<UserType> userReloaded = getUser(user.getOid());
        assertNull("user has credentials", userReloaded.asObjectable().getCredentials());

        // WHEN
        ProtectedStringType value = new ProtectedStringType();
        value.setClearValue(PASSWORD_HELLO_WORLD);
        PasswordType password = new PasswordType(prismContext).value(value);
        ObjectDelta<UserType> objectDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD).add(password)
                .asObjectDelta(user.getOid());

        executeChanges(objectDelta, null, task, result);

        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        display("user after operation", userAfter);
        assertUserPassword(userAfter, PASSWORD_HELLO_WORLD);
        assertPasswordCreateMetadata(userAfter);
    }

    /**
     * MID-4593
     */
    @Test
    public void test926ReplacePassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        setPasswordMinOccurs(0, task, result);

        UserType user = new UserType(prismContext).name("david");
        addObject(user, task, result);

        PrismObject<UserType> userReloaded = getUser(user.getOid());
        assertNull("user has credentials", userReloaded.asObjectable().getCredentials());

        // WHEN
        ProtectedStringType value = new ProtectedStringType();
        value.setClearValue(PASSWORD_HELLO_WORLD);
        PasswordType password = new PasswordType(prismContext).value(value);
        ObjectDelta<UserType> objectDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD).replace(password)
                .asObjectDelta(user.getOid());

        executeChanges(objectDelta, null, task, result);

        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        display("user after operation", userAfter);
        assertUserPassword(userAfter, PASSWORD_HELLO_WORLD);
        assertPasswordCreateMetadata(userAfter);
    }

    /**
     * MID-4593
     */
    @Test
    public void test928AddPasswordValue() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        setPasswordMinOccurs(0, task, result);

        UserType user = new UserType(prismContext).name("eve");
        addObject(user, task, result);

        PrismObject<UserType> userReloaded = getUser(user.getOid());
        assertNull("user has credentials", userReloaded.asObjectable().getCredentials());

        // WHEN
        ProtectedStringType value = new ProtectedStringType();
        value.setClearValue(PASSWORD_HELLO_WORLD);
        ObjectDelta<UserType> objectDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE).add(value)
                .asObjectDelta(user.getOid());

        executeChanges(objectDelta, null, task, result);

        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        display("user after operation", userAfter);
        assertUserPassword(userAfter, PASSWORD_HELLO_WORLD);
        assertPasswordModifyMetadata(userAfter);
    }

    /**
     * MID-4593
     */
    @Test
    public void test929ReplacePasswordValue() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        setPasswordMinOccurs(0, task, result);

        UserType user = new UserType(prismContext).name("frank");
        addObject(user, task, result);

        PrismObject<UserType> userReloaded = getUser(user.getOid());
        assertNull("user has credentials", userReloaded.asObjectable().getCredentials());

        // WHEN
        ProtectedStringType value = new ProtectedStringType();
        value.setClearValue(PASSWORD_HELLO_WORLD);
        ObjectDelta<UserType> objectDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE).replace(value)
                .asObjectDelta(user.getOid());

        executeChanges(objectDelta, null, task, result);

        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        display("user after operation", userAfter);
        assertUserPassword(userAfter, PASSWORD_HELLO_WORLD);
        assertPasswordModifyMetadata(userAfter);
    }

    /**
     * Remove global password policy, set organizational policy, check that such password policy is NOT applied to resources.
     * We want to keep global security policy here. We just want to remove global password policy.
     * Global security policy sets the storage mechanism (encryption, hashing). We want to keep that.
     * MID-4793, MID-4791
     */
    @Test
    public void test950RemoveGlobalPasswordPolicy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        applyPasswordPolicy(null, getSecurityPolicyOid(), task, result);
        modifyObjectReplaceProperty(SecurityPolicyType.class, getSecurityPolicyOid(),
                ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_HISTORY_LENGTH),
                task, result /* no value */);
        modifyObjectReplaceProperty(SecurityPolicyType.class, getSecurityPolicyOid(),
                ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_MIN_AGE),
                task, result /* no value */);
        modifyObjectReplaceProperty(SecurityPolicyType.class, getSecurityPolicyOid(),
                ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_MAX_AGE),
                task, result /* no value */);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<SecurityPolicyType> securityPolicyAfter = getObject(SecurityPolicyType.class, getSecurityPolicyOid());
        display("Security policy", securityPolicyAfter);

        // Make sure no password policy is applied by setting some insane passwords
        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_LLL_CLEAR, task, result);
        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_A_CLEAR, task, result);
    }

    /**
     * Set up monkey in an organization and set up organizational security policy.
     * MID-4793
     */
    @Test
    public void test952SetupOrgPolicy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<SecurityPolicyType> securityPolicy = prismContext.createObject(SecurityPolicyType.class);
        securityPolicy.asObjectable()
                .name("Ministry security policy")
                .beginCredentials()
                .beginPassword()
                .valuePolicyRef(PASSWORD_POLICY_GLOBAL_OID, ValuePolicyType.COMPLEX_TYPE)
                .historyLength(ORG_MINISTRY_OF_OFFENSE_PASSWORD_HISTORY_LENGTH);
        String ministrySecurityPolicyOid = addObject(securityPolicy, task, result);

        PrismReferenceValue securityPolicyRef = itemFactory().createReferenceValue();
        securityPolicyRef.setOid(ministrySecurityPolicyOid);
        modifyObjectReplaceReference(OrgType.class, ORG_MINISTRY_OF_OFFENSE_OID,
                OrgType.F_SECURITY_POLICY_REF, task, result, securityPolicyRef);

        // WHEN
        when();
        assignOrg(USER_THREE_HEADED_MONKEY_OID, ORG_MINISTRY_OF_OFFENSE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<OrgType> ministryPolicyAfter = getObject(OrgType.class, ORG_MINISTRY_OF_OFFENSE_OID);
        display("Ministry after", ministryPolicyAfter);

        PrismObject<SecurityPolicyType> securityPolicyAfter = getObject(SecurityPolicyType.class, ministrySecurityPolicyOid);
        display("Ministry security policy", securityPolicyAfter);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertAssignedOrg(userAfter, ORG_MINISTRY_OF_OFFENSE_OID);
    }

    /**
     * Monkey has password that does not comply with current ministry password policy.
     * Let's try to change it to another illegal password to check that the ministry policy applies.
     * MID-4793
     */
    @Test
    public void test954ModifyUserMonkeyPasswordAA() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        try {
            // WHEN
            when();

            modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_AA_CLEAR, task, result);

            assertNotReached();

        } catch (PolicyViolationException e) {
            // expected
        }

        // THEN
        then();
        assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);
    }

    /**
     * Jack is not part of ministry. Password policy should not apply to jack.
     * MID-4793
     */
    @Test
    public void test956ModifyUserJackPasswordAA() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();

        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_AA_CLEAR, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_AA_CLEAR);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Recompute. The password has not changed, there should be no error.
     * MID-4791, MID-4793
     */
    @Test
    public void test961RecomputeMonkey() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        recomputeUser(USER_THREE_HEADED_MONKEY_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);

        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Reconcile. The password has not changed, there should be no error.
     * MID-4791, MID-4793
     */
    @Test
    public void test962ReconcileMonkey() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        reconcileUser(USER_THREE_HEADED_MONKEY_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's make a user modification that is mapped to a resource.
     * Current password should be unaffected.
     * MID-4791, MID-4793
     */
    @Test
    public void test964ModifyUserMonkeyLocality() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserReplace(USER_THREE_HEADED_MONKEY_OID, UserType.F_LOCALITY, task, result,
                PolyString.fromOrig("Scabb Island"));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertDummyAccountAttribute(null, USER_THREE_HEADED_MONKEY_NAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Scabb Island");
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * But this is organizational password policy and it should apply only
     * to the user. Not the resources. Create of blue account should go well.
     * MID-4793
     */
    @Test
    public void test965AssignMonkeyAccountBlue() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();

        assignAccountToUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 3);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_THREE_HEADED_MONKEY_NAME);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's assign yellow account. Yellow resource has a minimum password length
     * (resource-enforced). Even though midPoint password policy is not applied,
     * resource won't accept the account. Therefore assignment should fail.
     * MID-4793
     */
    @Test
    public void test966AssignMonkeyAccountYellow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();

        assignAccountToUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

        // THEN
        then();
        assertPartialError(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 4);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_THREE_HEADED_MONKEY_NAME);
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_THREE_HEADED_MONKEY_NAME);
    }

    /**
     * Apply global password policy again. Now we have both organizational and global policies.
     * And they have different password history lengths.
     * MID-4082
     */
    @Test
    public void test970ReapplyGlobalPasswordPolicy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // preconditions
        assertUserBefore(USER_JACK_OID)
                .displayWithProjections()
                .assertLiveLinks(4);

        // Make sure that no global password policy is applied by setting some insane passwords
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_LLL_CLEAR);
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_A_CLEAR);

        // WHEN
        when();
        applyPasswordPolicy(PASSWORD_POLICY_GLOBAL_OID, getSecurityPolicyOid(), task, result);
        modifyObjectReplaceProperty(SecurityPolicyType.class, getSecurityPolicyOid(),
                ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_HISTORY_LENGTH),
                task, result, GLOBAL_POLICY_NEW_PASSWORD_HISTORY_LENGTH);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<SecurityPolicyType> securityPolicyAfter = getObject(SecurityPolicyType.class, getSecurityPolicyOid());
        display("Security policy", securityPolicyAfter);

        // Make sure that global password policy is applied by setting some insane passwords - and fail
        modifyUserChangePasswordPolicyViolation(USER_JACK_OID, USER_PASSWORD_LLL_CLEAR);
        modifyUserChangePasswordPolicyViolation(USER_JACK_OID, USER_PASSWORD_A_CLEAR);
    }

    /**
     * Now we have both organizational and global policies. And they have different password history lengths.
     * Jack does not belong to ministry of offense. Therefore global password policy is applied.
     * Change Jack's password, try to fill out all password history places.
     * MID-4082
     */
    @Test
    public void test972ChangePasswordJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_1);
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_2);

        // global password history applied, this should NOT work.
        // but it won't work in both settings. So no difference yet.
        modifyUserChangePasswordPolicyViolation(USER_JACK_OID, USER_PASSWORD_VALID_1);

        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_3);
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_4);

        // global password history applied, this should NOT work (but would work in organizational history)
        modifyUserChangePasswordPolicyViolation(USER_JACK_OID, USER_PASSWORD_VALID_1);

        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_5);
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_6);

        // Over password history length, this should work now
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_1);

        // THEN
        then();
        assertSuccess(result);

    }

    /**
     * Now we have both organizational and global policies. And they have different password history lengths.
     * Monkey does belong to ministry of offense. Therefore organizational password policy is applied.
     * Change Monkey's password, try to fill out all password history places.
     * MID-4082
     */
    @Test
    public void test974ChangePasswordJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();
        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_VALID_1);
        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_VALID_2);

        // organizational password history applied, this should NOT work.
        // but it won't work in both settings. So no difference yet.
        modifyUserChangePasswordPolicyViolation(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_VALID_1);

        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_VALID_3);
        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_VALID_4);

        // organizational password history applied, this should work as there is a shorter password history
        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_VALID_1);

        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_VALID_5);
        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_VALID_6);

        // Over password history length, this should work now
        modifyUserChangePassword(USER_THREE_HEADED_MONKEY_OID, USER_PASSWORD_VALID_2);

        // THEN
        then();
        assertSuccess(result);
    }

    private void modifyUserChangePasswordPolicyViolation(String userOid, String newPassword) throws CommonException {
        Task task = createTask("modifyUserChangePasswordPolicyViolation");
        OperationResult result = task.getResult();
        try {
            modifyUserChangePassword(userOid, newPassword, task, result);
            fail("Change of password '" + newPassword + "' succeeded for user " + userOid + ", expected failure");
        } catch (PolicyViolationException e) {
            assertFailure(result);
        }
    }

    protected void prepareTest() throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        prepareNotifications();
    }

    protected PrismObject<ShadowType> getBlueShadow(PrismObject<UserType> userAfter) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String accountBlueOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_BLUE_OID);
        Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + ".getBlueShadow");
        OperationResult result = task.getResult();
        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .item(SchemaConstants.PATH_PASSWORD_VALUE).retrieve()
                .build();
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountBlueOid, options, task, result);
        result.computeStatus();
        TestUtil.assertSuccess("getObject(Account) result not success", result);
        display("Blue shadow", shadow);
        return shadow;
    }

    protected boolean isPasswordEncryption() {
        return getPasswordStorageType() == CredentialsStorageTypeType.ENCRYPTION;
    }

    protected void assertCachedResourcePassword(RawRepoShadow shadow, String expectedPassword) throws Exception {
        CredentialsType credentials = shadow.getBean().getCredentials();
        if (expectedPassword == null && credentials == null) {
            return;
        }
        assertNotNull("Missing credentials in repo shadow " + shadow, credentials);
        PasswordType passwordType = credentials.getPassword();
        if (expectedPassword == null && passwordType == null) {
            return;
        }
        assertNotNull("Missing password credential in repo shadow " + shadow, passwordType);
        ProtectedStringType protectedStringType = passwordType.getValue();
        assertNotNull("No password value in repo shadow " + shadow, protectedStringType);
        assertProtectedString("Wrong password value in repo shadow " + shadow, expectedPassword, protectedStringType, CredentialsStorageTypeType.HASHING);
    }

    private void assertPasswordCreateMetadata(PrismObject<UserType> user) {
        CredentialsType credentials = user.asObjectable().getCredentials();
        assertNotNull("No credentials", credentials);
        PasswordType password = credentials.getPassword();
        assertNotNull("No credentials/password", password);
        var metadata = ValueMetadataTypeUtil.getMetadata(password);
        assertNotNull("No credentials/password/metadata", metadata);
        var storage = metadata.getStorage();
        assertNotNull("No credentials/password/metadata/storage", storage);
        assertNotNull("No credentials/password/metadata/storage/createTimestamp", storage.getCreateTimestamp());
        assertNotNull("No credentials/password/metadata/storage/creatorRef", storage.getCreatorRef());
        assertEquals("Wrong createChannel", SchemaConstants.CHANNEL_USER_URI, storage.getCreateChannel());
    }

    private void assertPasswordModifyMetadata(PrismObject<UserType> user) {
        CredentialsType credentials = user.asObjectable().getCredentials();
        assertNotNull("No credentials", credentials);
        PasswordType password = credentials.getPassword();
        assertNotNull("No credentials/password", password);
        var metadata = ValueMetadataTypeUtil.getMetadata(password);
        assertNotNull("No credentials/password/metadata", metadata);
        var storage = metadata.getStorage();
        assertNotNull("No credentials/password/metadata/storage", storage);
        assertNotNull("No credentials/password/metadata/storage/modifyTimestamp", storage.getModifyTimestamp());
        assertNotNull("No credentials/password/metadata/storage/modifierRef", storage.getModifierRef());
        assertEquals("Wrong modifyChannel", SchemaConstants.CHANNEL_USER_URI, storage.getModifyChannel());
    }

    protected void assertUserFriendlyMessage(OperationResult result, String expectedKey) {
        assertThat(result.getUserFriendlyMessage()).as("user friendly message").isNotNull();
        assertThat(((SingleLocalizableMessage) result.getUserFriendlyMessage()).getKey())
                .as("user friendly message key")
                .isEqualTo(expectedKey);
    }
}
