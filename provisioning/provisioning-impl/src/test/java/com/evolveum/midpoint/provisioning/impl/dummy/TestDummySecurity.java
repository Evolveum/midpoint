/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.schema.processor.ObjectFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummySecurity extends AbstractDummyTest {

    private String willIcfUid;

    @Test
    public void test100AddAccountDrink() throws Exception {
        // GIVEN
        Task syncTask = getTestTask();
        OperationResult result = getTestOperationResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_WILL_FILE);
        account.checkConsistence();

        setAttribute(account, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water");

        display("Adding shadow", account);

        try {
            // WHEN
            provisioningService.addObject(account, null, null, syncTask, result);

            AssertJUnit.fail("Unexpected success");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }
    }

    private <T> void setAttribute(PrismObject<ShadowType> account, String attrName, T val) throws SchemaException {
        PrismContainer<Containerable> attrsCont = account.findContainer(ShadowType.F_ATTRIBUTES);
        ResourceAttribute<T> attr = ObjectFactory.createResourceAttribute(
            dummyResourceCtl.getAttributeQName(attrName), null, prismContext);
        attr.setRealValue(val);
        attrsCont.add(attr);
    }

    @Test
    public void test199AddAccount() throws Exception {
        // GIVEN
        Task syncTask = getTestTask();
        OperationResult result = getTestOperationResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_WILL_FILE);
        account.checkConsistence();

        setAttribute(account, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "At the moment?");
        setAttribute(account, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Eunuch");

        display("Adding shadow", account);

        // WHEN
        provisioningService.addObject(account, null, null, syncTask, result);

        // THEN
        PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, syncTask, result);
        display("Account provisioning", accountProvisioning);
        willIcfUid = getIcfUid(accountProvisioning);

    }

    @Test
    public void test200ModifyAccountDrink() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID,
                dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME),
                "RUM");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        delta.checkConsistence();
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "RUM");

        syncServiceMock.assertNotifySuccessOnly();
    }

    @Test
    public void test201ModifyAccountGossip() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID,
                dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME),
                "pirate");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        delta.checkConsistence();
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "pirate");

        syncServiceMock.assertNotifySuccessOnly();
    }

    @Test
    public void test210ModifyAccountQuote() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID,
                dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME),
                "eh?");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        try {
            // WHEN
            provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                    new OperationProvisioningScriptsType(), null, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }
    }

    @Test
    public void test300GetAccount() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        // WHEN
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, result);

        // THEN
        result.computeStatus();
        display("getObject result", result);
        TestUtil.assertSuccess(result);

        display("Retrieved account shadow", shadow);

        assertNotNull("No dummy account", shadow);

        checkAccountWill(shadow);

        checkUniqueness(shadow);
    }

    @Test
    public void test310SearchAllShadows() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();
        ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
                SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
        displayDumpable("All shadows query", query);

        // WHEN
        List<PrismObject<ShadowType>> allShadows = provisioningService.searchObjects(ShadowType.class,
                query, null, null, result);

        // THEN
        result.computeStatus();
        display("searchObjects result", result);
        TestUtil.assertSuccess(result);

        display("Found " + allShadows.size() + " shadows");

        assertFalse("No shadows found", allShadows.isEmpty());

        checkUniqueness(allShadows);

        for (PrismObject<ShadowType> shadow: allShadows) {
            assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);
            assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME);
        }

        assertEquals("Wrong number of results", 2, allShadows.size());
    }

    // TODO: search

    private void checkAccountWill(PrismObject<ShadowType> shadow) {
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "RUM");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "At the moment?");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Will Turner");
        assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);
        assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME);
        assertEquals("Unexpected number of attributes", 8, attributes.size());
    }
}
