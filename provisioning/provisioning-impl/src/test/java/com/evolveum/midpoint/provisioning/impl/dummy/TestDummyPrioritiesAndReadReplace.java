/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 *
 * @author Radovan Semancik
 * @author Pavol Mederly
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyPrioritiesAndReadReplace extends AbstractDummyTest {

    protected String willIcfUid;

    public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-priorities-read-replace");
    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    protected MatchingRule<String> getUidMatchingRule() {
        return null;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        InternalMonitor.setTrace(InternalOperationClasses.CONNECTOR_OPERATIONS, true);
        // in order to have schema available here
        resourceType = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, taskManager.createTaskInstance(), initResult).asObjectable();
    }

    // copied from TestDummy
    @Test
    public void test100AddAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> account = prismContext.parseObject(getAccountWillFile());
        account.checkConsistence();

        display("Adding shadow", account);

        // WHEN
        String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

        // THEN
        result.computeStatus();
        display("add object result", result);
        TestUtil.assertSuccess("addObject has failed (result)", result);
        assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

        account.checkConsistence();

        PrismObject<ShadowType> accountRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
        willIcfUid = getIcfUid(accountRepo);

        ActivationType activationRepo = accountRepo.asObjectable().getActivation();
        if (supportsActivation()) {
            assertNotNull("No activation in "+accountRepo+" (repo)", activationRepo);
            assertEquals("Wrong activation enableTimestamp in "+accountRepo+" (repo)", ACCOUNT_WILL_ENABLE_TIMESTAMP, activationRepo.getEnableTimestamp());
        } else {
            assertNull("Activation sneaked in (repo)", activationRepo);
        }

        syncServiceMock.assertNotifySuccessOnly();

        PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);
        display("Account provisioning", accountProvisioning);
        ShadowType accountTypeProvisioning = accountProvisioning.asObjectable();
        display("account from provisioning", accountTypeProvisioning);
        PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_WILL_USERNAME, accountTypeProvisioning.getName());
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, accountTypeProvisioning.getKind());
        assertAttribute(accountProvisioning, SchemaConstants.ICFS_NAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(accountProvisioning, getUidMatchingRule(), SchemaConstants.ICFS_UID, willIcfUid);

        ActivationType activationProvisioning = accountTypeProvisioning.getActivation();
        if (supportsActivation()) {
            assertNotNull("No activation in "+accountProvisioning+" (provisioning)", activationProvisioning);
            assertEquals("Wrong activation administrativeStatus in "+accountProvisioning+" (provisioning)", ActivationStatusType.ENABLED, activationProvisioning.getAdministrativeStatus());
            TestUtil.assertEqualsTimestamp("Wrong activation enableTimestamp in "+accountProvisioning+" (provisioning)", ACCOUNT_WILL_ENABLE_TIMESTAMP, activationProvisioning.getEnableTimestamp());
        } else {
            assertNull("Activation sneaked in (provisioning)", activationProvisioning);
        }

        assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
                accountTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

        // Check if the account was created in the dummy resource

        DummyAccount dummyAccount = getDummyAccountAssert(ACCOUNT_WILL_USERNAME, willIcfUid);
        assertNotNull("No dummy account", dummyAccount);
        assertEquals("Username is wrong", ACCOUNT_WILL_USERNAME, dummyAccount.getName());
        assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
        assertTrue("The account is not enabled", dummyAccount.isEnabled());
        assertEquals("Wrong password", "3lizab3th", dummyAccount.getPassword());

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        PrismObject<ShadowType> shadowFromRepo = repositoryService.getObject(ShadowType.class,
                addedObjectOid, null, result);
        assertNotNull("Shadow was not created in the repository", shadowFromRepo);
        displayValue("Repository shadow", shadowFromRepo.debugDump());

        ProvisioningTestUtil.checkRepoAccountShadow(shadowFromRepo);

        checkUniqueness(accountProvisioning);
        //assertSteadyResource();
    }

    @Test
    public void test123ModifyObjectReplace() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        // todo add correct definition
        ObjectDelta<ShadowType> objectDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, dummyResourceCtl.getAttributeFullnamePath(), "Pirate Master Will Turner");
        PropertyDelta weaponDelta = objectDelta.createPropertyModification(dummyResourceCtl.getAttributeWeaponPath());
        weaponDelta.setDefinition(
                getAttributeDefinition(resourceType,
                        ShadowKindType.ACCOUNT, null,
                        DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));
        weaponDelta.setRealValuesToReplace("Gun");
        objectDelta.addModification(weaponDelta);
        PropertyDelta lootDelta = objectDelta.createPropertyModification(dummyResourceCtl.getAttributeLootPath());
        lootDelta.setDefinition(
                getAttributeDefinition(resourceType,
                        ShadowKindType.ACCOUNT, null,
                        DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME));
        lootDelta.setRealValuesToReplace(43);
        objectDelta.addModification(lootDelta);
        PropertyDelta titleDelta = objectDelta.createPropertyModification(dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME));
        titleDelta.setDefinition(
                getAttributeDefinition(resourceType,
                        ShadowKindType.ACCOUNT, null,
                        DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME));
        titleDelta.setRealValuesToReplace("Pirate Master");
        objectDelta.addModification(titleDelta);

        displayDumpable("ObjectDelta", objectDelta);
        objectDelta.checkConsistence();

        // WHEN
        provisioningService.modifyObject(ShadowType.class, objectDelta.getOid(), objectDelta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        objectDelta.checkConsistence();
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Pirate Master Will Turner");
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate Master");
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 43);
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Gun");

        assertTest123ModifyObjectReplaceResult(result);

        syncServiceMock.assertNotifySuccessOnly();

    }

    protected void assertTest123ModifyObjectReplaceResult(OperationResult result) {
        // BEWARE: very brittle!
        List<OperationResult> updatesExecuted = TestUtil.selectSubresults(result, ProvisioningTestUtil.CONNID_CONNECTOR_FACADE_CLASS_NAME + ".updateDelta");
        assertEquals("Wrong number of updates executed", 3, updatesExecuted.size());
        checkAttributesDelta(updatesExecuted.get(0), "updateDelta", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
        checkAttributesDelta(updatesExecuted.get(1), "updateDelta", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME);
        checkAttributesDelta(updatesExecuted.get(2), "updateDelta", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
    }

    protected void checkAttributesUpdated(OperationResult operationResult, String operation, String... attributeNames) {
        assertEquals("Wrong operation name", ProvisioningTestUtil.CONNID_CONNECTOR_FACADE_CLASS_NAME + "." + operation, operationResult.getOperation());
        Collection<String> updatedAttributes = parseUpdatedAttributes(operationResult.getParams().get("attributes").toString());
        assertEquals("Names of updated attributes do not match", new HashSet<>(Arrays.asList(attributeNames)), updatedAttributes);
    }

    protected void checkAttributesDelta(OperationResult operationResult, String operation, String... attributeNames) {
        assertEquals("Wrong operation name", ProvisioningTestUtil.CONNID_CONNECTOR_FACADE_CLASS_NAME + "." + operation, operationResult.getOperation());
        Collection<String> updatedAttributes = parseUpdatedAttributes(operationResult.getParams().get("attributesDelta").toString());
        assertEquals("Names of updated attributes do not match", new HashSet<>(Arrays.asList(attributeNames)), updatedAttributes);
    }

    // From something like this: [Attribute: {Name=fullname, Value=[Pirate Master Will Turner]},Attribute: {Name=title, Value=[Pirate Master]}]
    // we would like to get ["fullname", "title"]
    private Collection<String> parseUpdatedAttributes(String attributes) {
        Pattern pattern = Pattern.compile("Attribute: \\{Name=(\\w+),");
        Matcher matcher = pattern.matcher(attributes);
        Set<String> retval = new HashSet<>();
        while (matcher.find()) {
            retval.add(matcher.group(1));
        }
        return retval;
    }

    @Test
    public void test150ModifyObjectAddDelete() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        // NOT a read replace attribute
        // todo add correct definition
        ObjectDelta<ShadowType> objectDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, dummyResourceCtl.getAttributeFullnamePath(), "Pirate Great Master Will Turner");
        // read replace attribute, priority 0
        PropertyDelta weaponDelta = objectDelta.createPropertyModification(dummyResourceCtl.getAttributeWeaponPath());
        weaponDelta.setDefinition(
                getAttributeDefinition(resourceType,
                        ShadowKindType.ACCOUNT, null,
                        DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));
        weaponDelta.addRealValuesToAdd("Sword");
        weaponDelta.addRealValuesToDelete("GUN");            // case-insensitive treatment should work here
        objectDelta.addModification(weaponDelta);
        // read replace attribute, priority 1
        PropertyDelta lootDelta = objectDelta.createPropertyModification(dummyResourceCtl.getAttributeLootPath());
        lootDelta.setDefinition(
                getAttributeDefinition(resourceType,
                        ShadowKindType.ACCOUNT, null,
                        DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME));
        lootDelta.addRealValuesToAdd(44);
        lootDelta.addRealValuesToDelete(43);
        objectDelta.addModification(lootDelta);
        // NOT a read-replace attribute
        PropertyDelta titleDelta = objectDelta.createPropertyModification(dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME));
        titleDelta.setDefinition(
                getAttributeDefinition(resourceType,
                        ShadowKindType.ACCOUNT, null,
                        DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME));
        titleDelta.addRealValuesToAdd("Pirate Great Master");
        titleDelta.addRealValuesToDelete("Pirate Master");
        objectDelta.addModification(titleDelta);
        // read replace attribute
        PropertyDelta drinkDelta = objectDelta.createPropertyModification(dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME));
        drinkDelta.setDefinition(
                getAttributeDefinition(resourceType,
                        ShadowKindType.ACCOUNT, null,
                        DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME));
        drinkDelta.addRealValuesToAdd("orange juice");
        objectDelta.addModification(drinkDelta);

        displayDumpable("ObjectDelta", objectDelta);
        objectDelta.checkConsistence();

        // WHEN
        provisioningService.modifyObject(ShadowType.class, objectDelta.getOid(), objectDelta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        objectDelta.checkConsistence();
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Pirate Great Master Will Turner");
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate Great Master");
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 44);
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword");
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "orange juice");

        assertTest150ModifyObjectAddDeleteResult(result);

        syncServiceMock.assertNotifySuccessOnly();

        //assertSteadyResource();
    }

    protected void assertTest150ModifyObjectAddDeleteResult(OperationResult result) {
        // BEWARE: very brittle!
        List<OperationResult> updatesExecuted = TestUtil.selectSubresults(result,
                ProvisioningTestUtil.CONNID_CONNECTOR_FACADE_CLASS_NAME + ".updateDelta");
        assertEquals("Wrong number of updates executed", 3, updatesExecuted.size());
        checkAttributesDelta(updatesExecuted.get(0), "updateDelta", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);        // prio 0, read-replace
        checkAttributesDelta(updatesExecuted.get(1), "updateDelta", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME);            // prio 1, read-replace
        checkAttributesDelta(updatesExecuted.get(2), "updateDelta", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);    // prio none, read-replace + real replace
    }

}
