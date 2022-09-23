/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.createRaw;
import static com.evolveum.midpoint.schema.GetOperationOptions.createTolerateRawData;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings({ "SimplifiedTestNGAssertion", "SameParameterValue" })
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestStrangeCases extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/strange");

    private static final File USER_DEGHOULASH_FILE = new File(TEST_DIR, "user-deghoulash.xml");
    private static final String USER_DEGHOULASH_OID = "c0c010c0-d34d-b33f-f00d-1d11dd11dd11";
    private static final String USER_DEGHOULASH_NAME = "deghoulash";

    private static final File USER_TEMPLATE_STRANGE_FILE = new File(TEST_DIR, "user-template-strange.xml");
    private static final String USER_TEMPLATE_STRANGE_OID = "830060c0-87f4-11e7-9a48-57789b5d92c7";

    private static final String RESOURCE_DUMMY_CIRCUS_NAME = "circus";
    private static final File RESOURCE_DUMMY_CIRCUS_FILE = new File(TEST_DIR, "resource-dummy-circus.xml");
    private static final String RESOURCE_DUMMY_CIRCUS_OID = "65d73d14-bafb-11e6-9de3-ff46daf6e769";

    private static final File ROLE_IDIOT_FILE = new File(TEST_DIR, "role-idiot.xml");
    private static final String ROLE_IDIOT_OID = "12345678-d34d-b33f-f00d-555555550001";

    private static final File ROLE_IDIOT_ASSIGNMENT_FILE = new File(TEST_DIR, "role-idiot-assignment.xml");
    @SuppressWarnings("unused")
    private static final String ROLE_IDIOT_ASSIGNMENT_OID = "12345678-d34d-b33f-f00d-5a5555550001";

    private static final File ROLE_STUPID_FILE = new File(TEST_DIR, "role-stupid.xml");
    @SuppressWarnings("unused")
    private static final String ROLE_STUPID_OID = "12345678-d34d-b33f-f00d-555555550002";

    private static final File ROLE_STUPID_ASSIGNMENT_FILE = new File(TEST_DIR, "role-stupid-assignment.xml");
    private static final String ROLE_STUPID_ASSIGNMENT_OID = "12345678-d34d-b33f-f00d-5a5555550002";

    private static final File ROLE_BAD_CONSTRUCTION_RESOURCE_REF_FILE = new File(TEST_DIR, "role-bad-construction-resource-ref.xml");
    private static final String ROLE_BAD_CONSTRUCTION_RESOURCE_REF_OID = "54084f2c-eba0-11e5-8278-03ea5d7058d9";

    private static final TestResource<?> ROLE_BAD_CONSTRUCTION_RESOURCE_REF_LAX =
            new TestResource<>(TEST_DIR, "role-bad-construction-resource-ref-lax.xml", "ba3ff0cb-1543-4295-9280-ac5a9efbe8a6");

    private static final File ROLE_META_BAD_CONSTRUCTION_RESOURCE_REF_FILE = new File(TEST_DIR, "role-meta-bad-construction-resource-ref.xml");
    private static final String ROLE_META_BAD_CONSTRUCTION_RESOURCE_REF_OID = "90b931ae-eba8-11e5-977a-b73ba58cf18b";

    private static final File ROLE_TARGET_BAD_CONSTRUCTION_RESOURCE_REF_FILE = new File(TEST_DIR, "role-target-bad-construction-resource-ref.xml");
    @SuppressWarnings("unused")
    private static final String ROLE_TARGET_BAD_CONSTRUCTION_RESOURCE_REF_OID = "e69b791a-eba8-11e5-80f5-33732b18f10a";

    private static final File ROLE_RECURSION_FILE = new File(TEST_DIR, "role-recursion.xml");
    private static final String ROLE_RECURSION_OID = "12345678-d34d-b33f-f00d-555555550003";

    private static final File ROLE_RECURSION_ASSIGNMENT_FILE = new File(TEST_DIR, "role-recursion-assignment.xml");
    private static final String ROLE_RECURSION_ASSIGNMENT_OID = "12345678-d34d-b33f-f00d-5a5555550003";

    private static final String NON_EXISTENT_ACCOUNT_OID = "f000f000-f000-f000-f000-f000f000f000";

    private static final String RESOURCE_NONEXISTENT_OID = "00000000-f000-f000-f000-f000f000f000";

    private static final File CASE_APPROVAL_FILE = new File(TEST_DIR, "case-approval.xml");
    private static final String CASE_APPROVAL_OID = "402d844c-66c5-4070-8608-f0c4010284b9";

    private static final File SYSTEM_CONFIGURATION_STRANGE_FILE = new File(TEST_DIR, "system-configuration-strange.xml");

    private static final XMLGregorianCalendar USER_DEGHOULASH_FUNERAL_TIMESTAMP =
            XmlTypeConverter.createXMLGregorianCalendar(1771, 1, 2, 11, 22, 33);

    private static final File TREASURE_ISLAND_FILE = new File(TEST_DIR, "treasure-island.txt");

    // MID-6544
    private static final DummyTestResource RESOURCE_NO_CREATE = new DummyTestResource(TEST_DIR, "resource-dummy-no-create.xml",
            "3be6feda-1099-4e3c-9499-27d6520c5987", "no-create");
    private static final TestResource<RoleType> ROLE_NO_CREATE = new TestResource<>(TEST_DIR, "role-no-create.xml",
            "d339b2d7-da23-489c-9352-2d3442b88fdf");
    private static final TestResource<ArchetypeType> SEQUENCE_EXTERNAL_USER = new TestResource<>(TEST_DIR,
            "sequence-external-user.xml", "4ebf1b7b-95ea-4277-8eac-97f05c0b0300");
    private static final TestResource<ArchetypeType> ARCHETYPE_EXTERNAL_USER = new TestResource<>(TEST_DIR,
            "archetype-external-user.xml", "472d5fcb-1632-46c1-8c81-eea90dcd6b28");

    private static String treasureIsland;

    private String accountGuybrushOid;

    public TestStrangeCases() {
        super();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_CIRCUS_NAME, RESOURCE_DUMMY_CIRCUS_FILE, RESOURCE_DUMMY_CIRCUS_OID, initTask, initResult);

        getDummyResourceController(RESOURCE_DUMMY_RED_NAME).addAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Monkey Island");

        repoAddObjectFromFile(ACCOUNT_GUYBRUSH_DUMMY_RED_FILE, initResult);

        treasureIsland = IOUtils.toString(new FileInputStream(TREASURE_ISLAND_FILE), Charset.defaultCharset())
                .replace("\r\n", "\n");     // for Windows compatibility

        addObject(ROLE_IDIOT_FILE, initTask, initResult);
        addObject(ROLE_STUPID_FILE, initTask, initResult);
        addObject(ROLE_IDIOT_ASSIGNMENT_FILE, initTask, initResult);
        addObject(ROLE_RECURSION_FILE, initTask, initResult);
        addObject(ROLE_BAD_CONSTRUCTION_RESOURCE_REF_FILE, initTask, initResult);
        addObject(ROLE_META_BAD_CONSTRUCTION_RESOURCE_REF_FILE, initTask, initResult);
        addObject(ROLE_BAD_CONSTRUCTION_RESOURCE_REF_LAX, initTask, initResult);

        repoAddObjectFromFile(USER_TEMPLATE_STRANGE_FILE, initResult);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_STRANGE_OID, initResult);

        initDummyResource(RESOURCE_NO_CREATE, initTask, initResult);
        addObject(ROLE_NO_CREATE, initTask, initResult);
        addObject(SEQUENCE_EXTERNAL_USER, initTask, initResult);
        addObject(ARCHETYPE_EXTERNAL_USER, initTask, initResult);

//        DebugUtil.setDetailedDebugDump(true);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_STRANGE_FILE;
    }

    /**
     * Make sure we can log in as administrator. There are some problems in the
     * system configuration (e.g. unknown collection). But this should not profibit
     * login.
     * <p>
     * Note: this will probably die even before it gets to this test. Test class
     * initialization won't work if administrator cannot log in. But initialization
     * code may change in the future. Therefore it is better to have also an explicit
     * test for this.
     * <p>
     * MID-5328
     */
    @Test
    public void test010SanityAdministrator() throws Exception {
        given();

        when();
        loginAdministrator();

        then();
        assertLoggedInUsername(USER_ADMINISTRATOR_USERNAME);
    }

    /**
     * Recursion role points to itself. The assignment should fail.
     * MID-3652
     */
    @Test
    public void test050AddRoleRecursionAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        try {
            when();
            addObject(ROLE_RECURSION_ASSIGNMENT_FILE, task, result);

            assertNotReached();
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        then();
        assertFailure(result);

        assertNoObject(RoleType.class, ROLE_RECURSION_ASSIGNMENT_OID);
    }

    /**
     * Stupid: see Idiot
     * Idiot: see Stupid
     * <p>
     * In this case the assignment loop is broken after few attempts.
     * <p>
     * MID-3652
     */
    @Test
    public void test060AddRoleStupidAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        addObject(ROLE_STUPID_ASSIGNMENT_FILE, task, result);

        then();
        assertSuccess(result);

        PrismObject<RoleType> role = getObject(RoleType.class, ROLE_STUPID_ASSIGNMENT_OID);
        new PrismObjectAsserter<>(role).assertSanity();
    }

    /**
     * Attempt to add account (red) that already exists, but it is not linked.
     * Account is added using linkRef with account object.
     */
    @Test
    public void test100ModifyUserGuybrushAddAccountDummyRedNoAttributesConflict() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_GUYBRUSH_DUMMY_RED_FILE);
        // Remove the attributes. This will allow outbound mapping to take place instead.
        account.removeContainer(ShadowType.F_ATTRIBUTES);
        display("New account", account);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_GUYBRUSH_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(userDelta);

        dummyAuditService.clear();

        try {
            when();
            modelService.executeChanges(deltas, null, task, getCheckingProgressListenerCollection(), result);

            AssertJUnit.fail("Unexpected executeChanges success");
        } catch (ObjectAlreadyExistsException e) {
            displayExpectedException(e);
        }

        //TODO: this is not yet expected.. there is a checking code in the ProjectionValueProcessor..
        // the situation is that the account which should be created has the same ICFS_NAME as the on already existing in resource
        // as the resource-dummy-red doesn't contain synchronization configuration part, we don't know the rule according to
        // which to try to match newly created account and the old one, therefore it will now ended in this ProjectionValuesProcessor with the error
        // this is not a trivial fix, so it has to be designed first.. (the same problem in TestAssignmentError.test222UserAssignAccountDeletedShadowRecomputeNoSync()
//        //WHEN
//        displayWhen();
//        modelService.executeChanges(MiscUtil.createCollection(userDelta), null, task, getCheckingProgressListenerCollection(), result);
//
//        // THEN
//        displayThen();
//        assertPartialError(result);
        // end of TODO:

        // Check accountRef
        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);
        UserType userGuybrushType = userAfter.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userGuybrushType.getLinkRef().size());
        ObjectReferenceType accountRefType = userGuybrushType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood");

        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);

        result.computeStatus();
        display("executeChanges result", result);
        TestUtil.assertFailure("executeChanges result", result);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();

        Collection<ObjectDeltaOperation<? extends ObjectType>> auditExecution0Deltas = dummyAuditService.getExecutionDeltas(0);
        assertEquals("Wrong number of execution deltas", 0, auditExecution0Deltas.size());
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);

        //TODO: enable after fixing the above mentioned problem in ProjectionValueProcessor
//        // Strictly speaking, there should be just 2 records, not 3.
//        // But this is a strange case, not a common one. Midpoint does two attempts.
//        // We do not really mind about extra provisioning attempts.
//        assertEquals("Wrong number of execution deltas", 3, auditExecution0Deltas.size());
//        // SUCCESS because there is a handled error. Real error is in next audit record.
//        dummyAuditService.assertExecutionOutcome(0, OperationResultStatus.SUCCESS);
//
//        auditExecution0Deltas = dummyAuditService.getExecutionDeltas(1);
//        assertEquals("Wrong number of execution deltas", 2, auditExecution0Deltas.size());
//        dummyAuditService.assertExecutionOutcome(1, OperationResultStatus.PARTIAL_ERROR);
    }

    @Test
    public void test180DeleteHalfAssignmentFromUser() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        PrismObject<UserType> userOtis = createUser("otis", "Otis");
        fillinUserAssignmentAccountConstruction(userOtis, RESOURCE_DUMMY_OID);

        display("Half-assigned user", userOtis);

        // Remember the assignment so we know what to remove
        AssignmentType assignmentType = userOtis.asObjectable().getAssignment().iterator().next();

        // Add to repo to avoid processing of the assignment
        String userOtisOid = repositoryService.addObject(userOtis, null, result);

        //noinspection unchecked
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createModificationDeleteContainer(UserType.class,
                userOtisOid, UserType.F_ASSIGNMENT, assignmentType.asPrismContainerValue().clone());
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        when();
        modelService.executeChanges(deltas, null, task, getCheckingProgressListenerCollection(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userOtisAfter = getUser(userOtisOid);
        assertNotNull("Otis is gone!", userOtisAfter);
        // Check accountRef
        assertUserNoAccountRefs(userOtisAfter);

        // Check if dummy resource account is gone
        assertNoDummyAccount("otis");

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test190DeleteHalfAssignedUser() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        PrismObject<UserType> userNavigator = createUser("navigator", "Head of the Navigator");
        fillinUserAssignmentAccountConstruction(userNavigator, RESOURCE_DUMMY_OID);

        display("Half-assigned user", userNavigator);

        // Add to repo to avoid processing of the assignment
        String userNavigatorOid = repositoryService.addObject(userNavigator, null, result);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createDeleteDelta(UserType.class, userNavigatorOid
        );
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        when();
        modelService.executeChanges(deltas, null, task, getCheckingProgressListenerCollection(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        try {
            getUser(userNavigatorOid);
            AssertJUnit.fail("navigator is still alive!");
        } catch (ObjectNotFoundException ex) {
            // This is OK
        }

        // Check if dummy resource account is gone
        assertNoDummyAccount("navigator");

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, UserType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * User with linkRef that points nowhere.
     * MID-2134
     */
    @Test
    public void test200ModifyUserJackBrokenAccountRefAndPolyString() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        addBrokenAccountRef(USER_JACK_OID);
        // Make sure that the polystring does not have correct norm value
        PolyString fullNamePolyString = new PolyString("Magnificent Captain Jack Sparrow", null);

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
                fullNamePolyString);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result, 2);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Magnificent Captain Jack Sparrow");
        assertAccounts(USER_JACK_OID, 0);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.HANDLED_ERROR);
    }

    /**
     * Not much to see here. Just making sure that add account goes smoothly
     * after previous test. Also preparing setup for following tests.
     * The account is simply added, not assigned. This makes it quite fragile.
     * This is the way how we like it (in the tests).
     */
    @Test
    public void test210ModifyUserAddAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_FILE, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Check accountRef
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        getSingleLinkOid(userJack);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
    }

    /**
     * Not much to see here. Just preparing setup for following tests.
     * The account is simply added, not assigned. This makes it quite fragile.
     * This is the way how we like it (in the tests).
     */
    @Test
    public void test212ModifyUserAddAccountRed() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when();
        modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Check accountRef
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertLiveLinks(userJack, 2);
        String accountJackRedOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        assertNotNull(accountJackRedOid);

        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Magnificent Captain Jack Sparrow", true);
    }

    /**
     * Cause schema violation on the account during a provisioning operation. This should fail
     * the operation, but other operations should proceed and the account should definitely NOT
     * be unlinked.
     * MID-2134
     */
    @Test
    public void test212ModifyUserJackBrokenSchemaViolationPolyString() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();

        getDummyResource().setModifyBreakMode(BreakMode.SCHEMA);

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
                new PolyString("Cpt. Jack Sparrow", null));

        then();
        result.computeStatus();
        display("Result", result);
        TestUtil.assertPartialError(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Cpt. Jack Sparrow");

        assertLiveLinks(userJack, 2);
        String accountJackRedOidAfter = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        assertNotNull(accountJackRedOidAfter);

        // The change was not propagated here because of schema violation error
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        // The change should be propagated here normally
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Cpt. Jack Sparrow", true);
    }

    /**
     * Cause schema violation on the account during a provisioning operation. This should fail
     * the operation, but other operations should proceed and the account should definitely NOT
     * be unlinked.
     * MID-2134
     */
    @Test
    public void test214ModifyUserJackBrokenPassword() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();

        getDummyResource().setModifyBreakMode(BreakMode.SCHEMA);

        when();
        modifyUserChangePassword(USER_JACK_OID, "whereStheRUM", task, result);

        then();
        result.computeStatus();
        display("Result", result);
        TestUtil.assertPartialError(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Cpt. Jack Sparrow");
        assertEncryptedUserPassword(userJack, "whereStheRUM");

        assertLiveLinks(userJack, 2);
        String accountJackRedOidAfter = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        assertNotNull(accountJackRedOidAfter);

        // The change was not propagated here because of schema violation error
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        // The change should be propagated here normally
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Cpt. Jack Sparrow", true);
    }

    /**
     * Cause modification that will be mapped to the account and that will cause
     * conflict (AlreadyExistsException). Make sure that midPoint does not end up
     * in endless loop.
     * MID-3451
     */
    @Test
    public void test220ModifyUserJackBrokenConflict() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();

        getDummyResource().setModifyBreakMode(BreakMode.CONFLICT);

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, createPolyString("High seas"));

        then();
        result.computeStatus();
        display("Result", result);
        TestUtil.assertPartialError(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_LOCALITY, createPolyString("High seas"));

        assertLiveLinks(userJack, 2);
        String accountJackRedOidAfter = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        assertNotNull(accountJackRedOidAfter);

        // The change was not propagated here because of schema violation error
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Caribbean");

        // The change should be propagated here normally
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Cpt. Jack Sparrow", true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "High seas");

        getDummyResource().setModifyBreakMode(BreakMode.NONE);
    }

    /**
     * User modification triggers PolicyViolationException being thrown from the
     * user template mapping. Make sure that there is a user friendly message.
     * MID-2650
     */
    @Test
    public void test230ModifyUserJackUserTemplatePolicyViolation() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource().setModifyBreakMode(BreakMode.NONE);
        dummyAuditService.clear();

        try {
            when();
            modifyUserReplace(USER_JACK_OID, UserType.F_COST_CENTER, task, result, "broke");

            assertNotReached();

        } catch (ExpressionEvaluationException e) {
            then();

            displayException("Exception (expected)", e);
            assertExceptionUserFriendly(e, "We do not serve your kind here");

            display("Result", result);
            assertFailure(result);
        }

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);

    }

    // Lets test various extension magic and border cases now. This is maybe quite high in the architecture for
    // this test, but we want to make sure that none of the underlying components will screw the things up.

    @Test
    public void test300ExtensionSanity() {
        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismContainerDefinition<Containerable> extensionContainerDef = userDef.findContainerDefinition(UserType.F_EXTENSION);
        PrismAsserts.assertPropertyDefinition(extensionContainerDef, PIRACY_SHIP, DOMUtil.XSD_STRING, 1, 1);
        PrismAsserts.assertIndexed(extensionContainerDef, PIRACY_SHIP, true);
        PrismAsserts.assertPropertyDefinition(extensionContainerDef, PIRACY_TALES, DOMUtil.XSD_STRING, 0, 1);
        PrismAsserts.assertIndexed(extensionContainerDef, PIRACY_TALES, false);
        PrismAsserts.assertPropertyDefinition(extensionContainerDef, PIRACY_WEAPON, DOMUtil.XSD_STRING, 0, -1);
        PrismAsserts.assertIndexed(extensionContainerDef, PIRACY_WEAPON, true);
        PrismAsserts.assertPropertyDefinition(extensionContainerDef, PIRACY_LOOT, DOMUtil.XSD_INT, 0, 1);
        PrismAsserts.assertIndexed(extensionContainerDef, PIRACY_LOOT, true);
        PrismAsserts.assertPropertyDefinition(extensionContainerDef, PIRACY_BAD_LUCK, DOMUtil.XSD_LONG, 0, -1);
        PrismAsserts.assertIndexed(extensionContainerDef, PIRACY_BAD_LUCK, null);
        PrismAsserts.assertPropertyDefinition(extensionContainerDef, PIRACY_FUNERAL_TIMESTAMP, DOMUtil.XSD_DATETIME, 0, 1);
        PrismAsserts.assertIndexed(extensionContainerDef, PIRACY_FUNERAL_TIMESTAMP, true);
    }

    @Test
    public void test301AddUserDeGhoulash() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_DEGHOULASH_FILE);
        ObjectDelta<UserType> userAddDelta = DeltaFactory.Object.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userAddDelta);

        when();
        modelService.executeChanges(deltas, null, task, getCheckingProgressListenerCollection(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
        display("User after change execution", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAccounts(USER_DEGHOULASH_OID, 0);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
        dummyAuditService.assertExecutionSuccess();

        assertBasicDeGhoulashExtension(userDeGhoulash);
    }

    @Test
    public void test310SearchDeGhoulashByShip() throws Exception {
        searchDeGhoulash(PIRACY_SHIP, "The Undead Pot");
    }

    // There is no test311SearchDeGhoulashByTales
    // We cannot search by "tales". This is non-indexed string.

    @Test
    public void test312SearchDeGhoulashByWeaponSpoon() throws Exception {
        searchDeGhoulash(PIRACY_WEAPON, "spoon");
    }

    @Test
    public void test313SearchDeGhoulashByWeaponFork() throws Exception {
        searchDeGhoulash(PIRACY_WEAPON, "fork");
    }

    @Test
    public void test314SearchDeGhoulashByLoot() throws Exception {
        searchDeGhoulash(PIRACY_LOOT, 424242);
    }

    @Test
    public void test315SearchDeGhoulashByBadLuck13() throws Exception {
        searchDeGhoulash(PIRACY_BAD_LUCK, 13L);
    }

    // The "badLuck" property is non-indexed. But it is long, therefore it is still searchable
    @Test
    public void test316SearchDeGhoulashByBadLuck28561() throws Exception {
        searchDeGhoulash(PIRACY_BAD_LUCK, 28561L);
    }

    @Test
    public void test317SearchDeGhoulashByFuneralTimestamp() throws Exception {
        searchDeGhoulash(PIRACY_FUNERAL_TIMESTAMP, USER_DEGHOULASH_FUNERAL_TIMESTAMP);
    }

    private <T> void searchDeGhoulash(QName propName, T propValue) throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // Simple query
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_EXTENSION, propName).eq(propValue)
                .build();
        expect();
        searchDeGhoulash(query, task, result);

        // Complex query, combine with a name. This results in join down in the database
        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_NAME).eq(USER_DEGHOULASH_NAME)
                .and().item(UserType.F_EXTENSION, propName).eq(propValue)
                .build();

        searchDeGhoulash(query, task, result);
    }

    private void searchDeGhoulash(ObjectQuery query, Task task, OperationResult result) throws Exception {
        when();
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, query, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertFalse("No user found", users.isEmpty());
        assertEquals("Wrong number of users found", 1, users.size());
        PrismObject<UserType> userDeGhoulash = users.iterator().next();
        display("Found user", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");

        assertBasicDeGhoulashExtension(userDeGhoulash);
    }

    private void assertBasicDeGhoulashExtension(PrismObject<UserType> userDeGhoulash) {
        assertExtension(userDeGhoulash, PIRACY_SHIP, "The Undead Pot");
        assertExtension(userDeGhoulash, PIRACY_TALES, treasureIsland);
        assertExtension(userDeGhoulash, PIRACY_WEAPON, "fork", "spoon");
        assertExtension(userDeGhoulash, PIRACY_LOOT, 424242);
        assertExtension(userDeGhoulash, PIRACY_BAD_LUCK, 13L, 169L, 2197L, 28561L, 371293L, 131313131313131313L);
        assertExtension(userDeGhoulash, PIRACY_FUNERAL_TIMESTAMP, USER_DEGHOULASH_FUNERAL_TIMESTAMP);
    }

    /**
     * Idiot and Stupid are cyclic roles. However, the assignment should proceed because now that's allowed.
     */
    @Test
    public void test330AssignDeGhoulashIdiot() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        assignRole(USER_DEGHOULASH_OID, ROLE_IDIOT_OID, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
        display("User after change execution", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAssignedRoles(userDeGhoulash, ROLE_IDIOT_OID);
    }

    /**
     * Recursion role points to itself. The assignment should fail.
     * MID-3652
     */
    @Test
    public void test332AssignDeGhoulashRecursion() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        try {
            when();
            assignRole(USER_DEGHOULASH_OID, ROLE_RECURSION_OID, task, result);

            assertNotReached();
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        then();
        assertFailure(result);

        PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
        display("User after change execution", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAssignedRoles(userDeGhoulash, ROLE_IDIOT_OID);
    }

    /**
     * Stupid: see Idiot
     * Idiot: see Stupid
     * <p>
     * In this case the assignment loop is broken after few attempts.
     * <p>
     * MID-3652
     */
    @Test
    public void test334AssignDeGhoulashStupidAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        assignRole(USER_DEGHOULASH_OID, ROLE_STUPID_ASSIGNMENT_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
        display("User after change execution", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAssignedRoles(userDeGhoulash, ROLE_IDIOT_OID, ROLE_STUPID_ASSIGNMENT_OID);
    }

    /**
     * Stupid: see Idiot
     * Idiot: see Stupid
     * <p>
     * In this case the assignment loop is broken after few attempts.
     * <p>
     * MID-3652
     */
    @Test
    public void test336UnassignDeGhoulashStupidAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        unassignRole(USER_DEGHOULASH_OID, ROLE_STUPID_ASSIGNMENT_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
        display("User after change execution", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAssignedRoles(userDeGhoulash, ROLE_IDIOT_OID);
    }

    @Test
    public void test340AssignDeGhoulashConstructionNonExistentResource() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        // We have loose referential consistency. Even if the target resource is not present
        // the assignment should be added. The error is indicated in the result.
        assignAccountToUser(USER_DEGHOULASH_OID, RESOURCE_NONEXISTENT_OID, null, task, result);

        then();
        result.computeStatus();
        display("Result", result);
        TestUtil.assertFailure(result);

        PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
        display("User after change execution", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAssignments(userDeGhoulash, 2);
    }

    @Test
    public void test349UnAssignDeGhoulashConstructionNonExistentResource() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        unassignAccountFromUser(USER_DEGHOULASH_OID, RESOURCE_NONEXISTENT_OID, null, task, result);

        then();
        result.computeStatus();
        display("Result", result);
        TestUtil.assertFailure(result);

        PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
        display("User after change execution", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAssignments(userDeGhoulash, 1);
        assertAssignedRoles(userDeGhoulash, ROLE_IDIOT_OID);
    }

    @Test
    public void test350AssignDeGhoulashRoleBadConstructionResourceRef() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        assignRole(USER_DEGHOULASH_OID, ROLE_BAD_CONSTRUCTION_RESOURCE_REF_OID, task, result);

        then();
        result.computeStatus();
        display("result", result);
        TestUtil.assertPartialError(result);
        String message = result.getMessage();
        TestUtil.assertMessageContains(message, "role:" + ROLE_BAD_CONSTRUCTION_RESOURCE_REF_OID);
        TestUtil.assertMessageContains(message, "Bad resourceRef in construction");
        TestUtil.assertMessageContains(message, TestUtil.NON_EXISTENT_OID);

        PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
        display("User after change execution", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAssignedRoles(userDeGhoulash, ROLE_BAD_CONSTRUCTION_RESOURCE_REF_OID, ROLE_IDIOT_OID);
    }

    @Test
    public void test351UnAssignDeGhoulashRoleBadConstructionResourceRef() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        unassignRole(USER_DEGHOULASH_OID, ROLE_BAD_CONSTRUCTION_RESOURCE_REF_OID, task, result);

        then();
        result.computeStatus();
        display("result", result);
        TestUtil.assertPartialError(result);
        String message = result.getMessage();
        TestUtil.assertMessageContains(message, "role:" + ROLE_BAD_CONSTRUCTION_RESOURCE_REF_OID);
        TestUtil.assertMessageContains(message, "Bad resourceRef in construction");
        TestUtil.assertMessageContains(message, TestUtil.NON_EXISTENT_OID);

        PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
        display("User after change execution", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAssignedRoles(userDeGhoulash, ROLE_IDIOT_OID);
    }

    @Test
    public void test355AssignDeGhoulashRoleBadConstructionResourceRefLax() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        assignRole(USER_DEGHOULASH_OID, ROLE_BAD_CONSTRUCTION_RESOURCE_REF_LAX.oid, task, result);

        then();
        result.computeStatus();
        display("result", result);
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
        display("User after change execution", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAssignedRoles(userDeGhoulash, ROLE_BAD_CONSTRUCTION_RESOURCE_REF_LAX.oid, ROLE_IDIOT_OID);
    }

    @Test
    public void test356UnAssignDeGhoulashRoleBadConstructionResourceRefLax() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        unassignRole(USER_DEGHOULASH_OID, ROLE_BAD_CONSTRUCTION_RESOURCE_REF_LAX.oid, task, result);

        then();
        result.computeStatus();
        display("result", result);
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userDeGhoulash = getUser(USER_DEGHOULASH_OID);
        display("User after change execution", userDeGhoulash);
        assertUser(userDeGhoulash, USER_DEGHOULASH_OID, "deghoulash", "Charles DeGhoulash", "Charles", "DeGhoulash");
        assertAssignedRoles(userDeGhoulash, ROLE_IDIOT_OID);
    }

    @Test
    public void test360AddRoleTargetBadConstructionResourceRef() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        addObject(ROLE_TARGET_BAD_CONSTRUCTION_RESOURCE_REF_FILE, task, result);

        then();
        result.computeStatus();
        display("result", result);
        TestUtil.assertPartialError(result);
        String message = result.getMessage();
        TestUtil.assertMessageContains(message, "role:" + ROLE_META_BAD_CONSTRUCTION_RESOURCE_REF_OID);
        TestUtil.assertMessageContains(message, "Bad resourceRef in construction metarole");
        TestUtil.assertMessageContains(message, TestUtil.NON_EXISTENT_OID);
    }

    @Test
    public void test400ImportJackMockTask() throws Exception {
        given();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        importObjectFromFile(TASK_MOCK_JACK_FILE);

        then();
        result.computeStatus();
        assertSuccess(result);

        waitForTaskFinish(TASK_MOCK_JACK_OID, false);
    }

    @Test
    public void test401ListTasks() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        List<PrismObject<TaskType>> objects = modelService.searchObjects(TaskType.class, null, null, task, result);

        then();
        assertSuccess(result);

        display("Tasks", objects);
        assertEquals("Unexpected number of tasks", 1, objects.size());
        boolean found = false;
        for (PrismObject<TaskType> object : objects) {
            if (object.getOid().equals(TASK_MOCK_JACK_OID)) {
                found = true;
            }
        }
        assertTrue("Mock task not found (model)", found);

//        ClusterStatusInformation clusterStatusInformation = taskManager.getRunningTasksClusterwide(result);
//        display("Cluster status", clusterStatusInformation);
//        TaskInfo jackTaskInfo = null;
//        Set<TaskInfo> taskInfos = clusterStatusInformation.getTasks();
//        for (TaskInfo taskInfo: taskInfos) {
//            if (taskInfo.getOid().equals(TASK_MOCK_JACK_OID)) {
//                jackTaskInfo = taskInfo;
//            }
//        }
//        assertNotNull("Mock task not found (taskManager)", jackTaskInfo);

        // Make sure that the tasks still runs
        waitForTaskFinish(TASK_MOCK_JACK_OID, false);

    }

    /**
     * Delete user jack. See that Jack's tasks are still there (although they may be broken)
     */
    @Test
    public void test410DeleteJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        deleteObject(UserType.class, USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        // Make sure the user is gone
        assertNoObject(UserType.class, USER_JACK_OID, task, result);

        List<PrismObject<TaskType>> objects = modelService.searchObjects(TaskType.class, null, null, task, result);
        display("Tasks", objects);
        assertEquals("Unexpected number of tastsk", 1, objects.size());
        PrismObject<TaskType> jackTask = null;
        for (PrismObject<TaskType> object : objects) {
            if (object.getOid().equals(TASK_MOCK_JACK_OID)) {
                jackTask = object;
            }
        }
        assertNotNull("Mock task not found (model)", jackTask);
        display("Jack's task (model)", jackTask);

//        ClusterStatusInformation clusterStatusInformation = taskManager.getRunningTasksClusterwide(result);
//        display("Cluster status", clusterStatusInformation);
//        TaskInfo jackTaskInfo = null;
//        Set<TaskInfo> taskInfos = clusterStatusInformation.getTasks();
//        for (TaskInfo taskInfo: taskInfos) {
//            if (taskInfo.getOid().equals(TASK_MOCK_JACK_OID)) {
//                jackTaskInfo = taskInfo;
//            }
//        }
//        assertNotNull("Mock task not found (taskManager)", jackTaskInfo);
//        display("Jack's task (taskManager)", jackTaskInfo);

        // TODO: check task status

    }

    /**
     * Adding approval case with a complex polystring name (no orig or norm).
     * Orig and norm should be automatically computed when the object is added.
     * MID-5577
     */
    @Test
    public void test450AddApprovalCase() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();

        when();
        addObject(CASE_APPROVAL_FILE, task, result);

        then();
        assertSuccess(result);

        assertCaseAfter(CASE_APPROVAL_OID)
                .name()
                .assertOrig("Assigning role \"Manager\" to user \"vera\"");

    }

    @Test
    public void test500EnumerationExtension() {
        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismPropertyDefinition<String> markDef = userDef.findPropertyDefinition(ItemPath.create(UserType.F_EXTENSION, PIRACY_MARK));

        when();
        TestUtil.assertSetEquals("Wrong allowedValues in mark", MiscUtil.getValuesFromDisplayableValues(markDef.getAllowedValues()),
                "pegLeg", "noEye", "hook", "tatoo", "scar", "bravery");

        for (DisplayableValue<String> disp : markDef.getAllowedValues()) {
            if (disp.getValue().equals("pegLeg")) {
                assertEquals("Wrong pegLeg label", "Peg Leg", disp.getLabel());
            }
        }
    }

    @Test
    public void test502EnumerationStoreGood() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        userDef.findPropertyDefinition(ItemPath.create(UserType.F_EXTENSION, PIRACY_MARK));

        dummyAuditService.clear();

        when();
        modifyObjectReplaceProperty(UserType.class, USER_GUYBRUSH_OID, ItemPath.create(UserType.F_EXTENSION, PIRACY_MARK),
                task, result, "bravery");

        then();
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        PrismProperty<String> markProp = user.findProperty(ItemPath.create(UserType.F_EXTENSION, PIRACY_MARK));
        assertEquals("Bad mark", "bravery", markProp.getRealValue());
    }

    /**
     * Guybrush has stored mark "bravery". Change schema so this value becomes illegal.
     * They try to read it.
     */
    @Test // MID-2260
    public void test510EnumerationGetBad() throws Exception {
        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismPropertyDefinition<String> markDef = userDef.findPropertyDefinition(ItemPath.create(UserType.F_EXTENSION, PIRACY_MARK));
        Iterator<? extends DisplayableValue<String>> iterator = markDef.getAllowedValues().iterator();
        DisplayableValue<String> braveryValue = null;
        while (iterator.hasNext()) {
            DisplayableValue<String> disp = iterator.next();
            if (disp.getValue().equals("bravery")) {
                braveryValue = disp;
                iterator.remove();
            }
        }

        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();

        when();
        PrismObject<UserType> user = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismProperty<String> markProp = user.findProperty(ItemPath.create(UserType.F_EXTENSION, PIRACY_MARK));
        assertEquals("Bad mark", null, markProp.getRealValue());

        //noinspection unchecked
        ((Collection<DisplayableValue<String>>) markDef.getAllowedValues()).add(braveryValue);        // because of the following test
    }

    /**
     * Store value in extension/ship. Then remove extension/ship definition from the schema.
     * The next read should NOT fail, because of the 'tolerate raw data' mode.
     */
    @Test
    public void test520ShipReadBadTolerateRawData() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();

        modifyObjectReplaceProperty(UserType.class, USER_GUYBRUSH_OID, ItemPath.create(UserType.F_EXTENSION, PIRACY_SHIP),
                task, result, "The Pink Lady");
        assertSuccess(result);

        changeDefinition(PIRACY_SHIP, PIRACY_SHIP_BROKEN);

        when();
        modelService.getObject(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(createTolerateRawData()), task, result);

        then();
        assertSuccess(result);
    }

    private void changeDefinition(QName piracyShip, ItemName piracyShipBroken) {
        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismContainerDefinition<?> extensionDefinition = userDef.getExtensionDefinition();
        List<? extends ItemDefinition> extensionDefs = extensionDefinition.getComplexTypeDefinition().getDefinitions();
        for (ItemDefinition<?> itemDefinition : extensionDefs) {
            if (itemDefinition.getItemName().equals(piracyShip)) {
                ((ItemDefinitionTestAccess) itemDefinition).replaceName(piracyShipBroken);
            }
        }
    }

    /**
     * Read guybrush again.
     * The operation should fail, because the raw mode itself does not allow raw data (except for some objects).
     * <p>
     * TODO reconsider this eventually, and change the test
     */
    @Test
    public void test522ShipReadBadRaw() {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();

        expect();
        try {
            modelService.getObject(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(createRaw()), task, result);
            fail("unexpected success");
        } catch (Throwable t) {
            System.out.println("Got expected exception: " + t);
        }
    }

    /**
     * Read guybrush again.
     * The operation should fail, because of the plain mode.
     * <p>
     * TODO reconsider this eventually, and change the test
     */
    @Test
    public void test524ShipReadBadPlain() {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();

        expect();
        try {
            modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
            fail("unexpected success");
        } catch (Throwable t) {
            System.out.println("Got expected exception: " + t);
        }
    }

    @Test
    public void test529FixSchema() {
        changeDefinition(PIRACY_SHIP_BROKEN, PIRACY_SHIP);
    }

    /**
     * Circus resource has a circular dependency. It should fail, but it should
     * fail with a proper error.
     * MID-3522
     */
    @Test
    public void test550AssignCircus() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        try {
            when();
            assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_CIRCUS_OID, null, task, result);

            assertNotReached();
        } catch (PolicyViolationException e) {
            then();
            result.computeStatus();
            TestUtil.assertFailure(result);
        }

    }

    @Test
    public void test600AddUserGuybrushAssignAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        when();
        modelService.executeChanges(deltas, null, task, getCheckingProgressListenerCollection(), result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after change execution", userAfter);
        accountGuybrushOid = getSingleLinkOid(userAfter);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountGuybrushOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountGuybrushOid, USER_GUYBRUSH_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountGuybrushOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountGuybrushOid, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
    }

    /**
     * Set attribute that is not in the schema directly into dummy resource.
     * Get that account. Make sure that the operations does not die.
     */
    @Test(enabled = false) // MID-2880
    public void test610GetAccountGuybrushRogueAttribute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource().setEnforceSchema(false);
        DummyAccount dummyAccount = getDummyAccount(null, USER_GUYBRUSH_USERNAME);
        dummyAccount.addAttributeValues("rogue", "habakuk");
        getDummyResource().setEnforceSchema(true);

        when();
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountGuybrushOid, null, task, result);

        then();
        assertSuccess(result);

        display("Shadow after", shadow);
        assertDummyAccountShadowModel(shadow, accountGuybrushOid, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);

        assertDummyAccountAttribute(null, USER_GUYBRUSH_USERNAME, "rogue", "habakuk");
    }

    /**
     * Adds existing assignment (phantom change) and looks how audit and notifications look like.
     * MID-6370
     */
    @Test
    public void test700AddExistingAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserType user = new UserType(prismContext)
                .name("user700")
                .beginAssignment()
                .targetRef(ROLE_SUPERUSER_OID, RoleType.COMPLEX_TYPE)
                .end();

        addObject(user, task, result);

        dummyTransport.clearMessages();
        notificationManager.setDisabled(false);
        dummyAuditService.clear();

        when();
        assignRole(user.getOid(), ROLE_SUPERUSER_OID, task, result);

        then();
        displayDumpable("dummy transport", dummyTransport);
        displayDumpable("dummy audit", dummyAuditService);

        // TODO some asserts here - currently there is an ADD audit record and ADD notification
        //  In the future we can think of indicating that these adds are - in fact - phantom ones.
    }

    /**
     * Adds external user. The operation will fail because of non-creatable account.
     * The sequence should be in consistent state afterwards: MID-6455.
     */
    @Test
    public void test710AddUserWithNonCreatableAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserType user = new UserType(prismContext)
                .beginAssignment()
                .targetRef(ARCHETYPE_EXTERNAL_USER.oid, ArchetypeType.COMPLEX_TYPE)
                .<UserType>end()
                .beginAssignment()
                .targetRef(ROLE_NO_CREATE.oid, RoleType.COMPLEX_TYPE)
                .end();

        when();
        try {
            addObject(user, task, result);
        } catch (Exception e) {
            displayExpectedException(e);
        }

        then();
        assertUserAfterByUsername("ext_0")
                .assignments()
                .assertArchetype(ARCHETYPE_EXTERNAL_USER.oid)
                .assertRole(ROLE_NO_CREATE.oid)
                .end()
                .assertLiveLinks(0);

        PrismObject<SequenceType> sequenceAfter = getObjectViaRepo(SequenceType.class, SEQUENCE_EXTERNAL_USER.oid);
        displayDumpable("sequence", sequenceAfter);

        long next = repositoryService.advanceSequence(SEQUENCE_EXTERNAL_USER.oid, result);
        assertThat(next).as("next sequence number").isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    private <O extends ObjectType, T> void assertExtension(
            PrismObject<O> object, QName propName, T... expectedValues) {
        PrismContainer<Containerable> extensionContainer = object.findContainer(ObjectType.F_EXTENSION);
        assertNotNull("No extension container in " + object, extensionContainer);
        PrismProperty<T> extensionProperty = extensionContainer.findProperty(ItemName.fromQName(propName));
        assertNotNull("No extension property " + propName + " in " + object, extensionProperty);
        PrismAsserts.assertPropertyValues("Values of extension property " + propName, extensionProperty.getValues(), expectedValues);
    }

    /**
     * Break the user in the repo by inserting accountRef that points nowhere.
     */
    private void addBrokenAccountRef(String userOid) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult("addBrokenAccountRef");

        Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFactory().reference().createModificationAddCollection(UserType.class,
                UserType.F_LINK_REF, NON_EXISTENT_ACCOUNT_OID);
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);

        assertSuccess(result);
    }
}
