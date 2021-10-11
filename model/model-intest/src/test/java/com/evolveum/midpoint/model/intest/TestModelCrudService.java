/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.test.DummyResourceContoller;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * This is testing the DEPRECATED functions of model API. It should be removed once the functions are phased out.
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestModelCrudService extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/crud");
    public static final File TEST_CONTRACT_DIR = new File("src/test/resources/contract");

    public static final File RESOURCE_MAROON_FILE = new File(TEST_DIR, "resource-dummy-maroon.xml");
    public static final String RESOURCE_MAROON_OID = "10000000-0000-0000-0000-00000000e104";

    private static final File RESOURCE_MAROON_NO_DELETE_FILE = new File(TEST_DIR, "resource-dummy-maroon-no-delete.xml");
    private static final String RESOURCE_MAROON_NO_DELETE_OID = "10000000-0000-0000-0000-00000000e105";
    private static final String RESOURCE_MAROON_NO_DELETE_NAME = "maroonNoDelete";

    private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
    private static final String USER_BLACKBEARD_OID = "c0c010c0-d34d-b33f-f00d-161161116666";

    private static final File ACCOUNT_JACK_DUMMY_MAROON_NO_DELETE_FILE = new File(TEST_DIR, "account-jack-dummy-maroon-no-delete.xml");
    private static String accountOid;

    @Autowired
    protected ModelCrudService modelCrudService;

    private DummyResourceContoller maroonNoDeleteCtl;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        maroonNoDeleteCtl = initDummyResource(RESOURCE_MAROON_NO_DELETE_NAME, RESOURCE_MAROON_NO_DELETE_FILE, RESOURCE_MAROON_NO_DELETE_OID, initTask, initResult);
    }

    @Test
    public void test050AddResourceMaroon() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ResourceType resourceType = (ResourceType) PrismTestUtil.parseObject(RESOURCE_MAROON_FILE).asObjectable();

        // WHEN
        PrismObject<ResourceType> object = resourceType.asPrismObject();
        prismContext.adopt(resourceType);
        modelCrudService.addObject(object, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Make sure the resource has t:norm part of polystring name
        PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_MAROON_OID, null, task, result);
        assertEquals("Wrong orig in resource name", "Dummy Resource Maroon", resourceAfter.asObjectable().getName().getOrig());
        assertEquals("Wrong norm in resource name", "dummy resource maroon", resourceAfter.asObjectable().getName().getNorm());

    }

    @Test
    public void test100ModifyUserAddAccount() throws Exception {
        testModifyUserJackAddAccount(ACCOUNT_JACK_DUMMY_FILE, dummyResourceCtl);
    }

    @Test
    public void test119ModifyUserDeleteAccount() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test119ModifyUserDeleteAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(accountOid);

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference().createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
        modifications.add(accountDelta);

        // WHEN
        modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications, null, task, result);

        // THEN
        // Check accountRef
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());

        // Check is shadow is gone
        try {
            repositoryService.getObject(ShadowType.class, accountOid, null, result);
            AssertJUnit.fail("Shadow " + accountOid + " still exists");
        } catch (ObjectNotFoundException e) {
            // This is OK
        }

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
    }

    @Test
    public void test120AddAccount() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test120AddAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        // WHEN
        accountOid = modelCrudService.addObject(account, null, task, result);

        // THEN
        // Check accountRef (should be none)
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
    }

    @Test
    public void test121ModifyUserAddAccountRef() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test121ModifyUserAddAccountRef");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountOid);
        modifications.add(accountDelta);

        // WHEN
        modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications, null, task, result);

        // THEN
        // Check accountRef
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
        accountOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
    }

    @Test
    public void test128ModifyUserDeleteAccountRef() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test128ModifyUserDeleteAccountRef");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference().createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountOid);
        modifications.add(accountDelta);

        // WHEN
        modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check shadow (if it is unchanged)
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");

        // Check account (if it is unchanged)
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");

        // Check account in dummy resource (if it is unchanged)
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
    }

    @Test
    public void test129DeleteAccount() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test129DeleteAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // WHEN
        modelCrudService.deleteObject(ShadowType.class, accountOid, null, task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
    }

    @Test
    public void test150AddUserBlackbeardWithAccount() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test150AddUserBlackbeardWithAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_CONTRACT_DIR, "user-blackbeard-account-dummy.xml"));
        addAccountLinkRef(user, new File(TEST_CONTRACT_DIR, "account-blackbeard-dummy.xml"));

        // WHEN
        modelCrudService.addObject(user, null, task, result);

        // THEN
        // Check accountRef
        PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_BLACKBEARD_OID, null, task, result);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "blackbeard");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "blackbeard", "Edward Teach");

        // Check account in dummy resource
        assertDefaultDummyAccount("blackbeard", "Edward Teach", true);
    }

    @Test
    public void test210AddUserMorganWithAssignment() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test210AddUserMorganWithAssignment");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_CONTRACT_DIR, "user-morgan-assignment-dummy.xml"));

        // WHEN
        modelCrudService.addObject(user, null, task, result);

        // THEN
        // Check accountRef
        PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "morgan");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "morgan", "Sir Henry Morgan");

        // Check account in dummy resource
        assertDefaultDummyAccount("morgan", "Sir Henry Morgan", true);
    }

    @Test
    public void test220DeleteUserMorgan() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test220DeleteUserMorgan");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        assertDummyAccount(null, "morgan");

        // WHEN
        modelCrudService.deleteObject(FocusType.class, USER_MORGAN_OID, null, task, result);

        // THEN
        try {
            getUser(USER_MORGAN_OID);
            fail("User morgan exists even if he should not");
        } catch (ObjectNotFoundException e) {
            // ok
        }

        assertNoDummyAccount(null, "morgan");

        result.computeStatus();
        IntegrationTestTools.display(result);
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test301modifyJAckAddAccount() throws Exception {
        testModifyUserJackAddAccount(ACCOUNT_JACK_DUMMY_MAROON_NO_DELETE_FILE, maroonNoDeleteCtl);
    }

    // test302 has to run after test301, because test302 relies on accountOid set in test301
    @Test
    public void test302deleteUserJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        //WHEN
        try {
            deleteObject(UserType.class, USER_JACK_OID, task, result);
            fail("Unexpected success. Should fail because resource doesn't support delete and criticality is not set");
        } catch (UnsupportedOperationException e) {
            // this is expected
        }

        //THEN
        // Check shadow, must be still in repo, becuase resource doesn't support delete
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", maroonNoDeleteCtl.getResourceType());
        //check that situation was updated MID-4038
        assertSituation(accountShadow, null);

    }

    private void testModifyUserJackAddAccount(File accountFile, DummyResourceContoller dummyController) throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(accountFile);

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        modifications.add(accountDelta);

        // WHEN
        modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications, null, task, result);

        // THEN
        // Check accountRef
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", dummyController.getResourceType());
        assertSituation(accountShadow, SynchronizationSituationType.LINKED);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "jack", dummyController.getResourceType());

        // Check account in dummy resource
        assertDummyAccount(dummyController.getDummyResource().getInstanceName(), "jack", "Jack Sparrow", true);
    }
}
