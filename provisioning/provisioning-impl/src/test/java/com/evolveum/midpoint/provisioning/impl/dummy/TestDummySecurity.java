/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_PATH;
import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_PATH;
import static com.evolveum.midpoint.test.IntegrationTestTools.createAllShadowsQuery;

import java.util.Collection;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ObjectFactory;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 *
 * It checks the access limitations.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummySecurity extends AbstractDummyTest {

    private String willIcfUid;

    /**
     * Drink is a non-creatable attribute.
     */
    @Test
    public void test100AddAccountDrink() throws Exception {
        // GIVEN
        Task syncTask = getTestTask();
        OperationResult result = getTestOperationResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> accountToAdd = prismContext.parseObject(ACCOUNT_WILL_FILE);
        accountToAdd.checkConsistence();

        setAttribute(accountToAdd, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water");

        display("Adding shadow", accountToAdd);

        try {
            // WHEN
            provisioningService.addObject(accountToAdd, null, null, syncTask, result);

            AssertJUnit.fail("Unexpected success");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }
    }

    private <T> void setAttribute(PrismObject<ShadowType> account, String attrName, T val) throws SchemaException {
        PrismContainer<Containerable> attrsCont = account.findContainer(ShadowType.F_ATTRIBUTES);
        ShadowSimpleAttribute<T> attr = ObjectFactory.createSimpleAttribute(dummyResourceCtl.getAttributeQName(attrName));
        attr.setRealValue(val);
        attrsCont.add(attr);
    }

    @Test
    public void test199AddAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> accountToAdd = prismContext.parseObject(ACCOUNT_WILL_FILE);
        accountToAdd.checkConsistence();

        setAttribute(accountToAdd, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "At the moment?");
        setAttribute(accountToAdd, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Eunuch");

        display("Adding shadow", accountToAdd);

        // WHEN
        provisioningService.addObject(accountToAdd, null, null, task, result);

        // THEN
        PrismObject<ShadowType> accountProvisioning =
                provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Account provisioning", accountProvisioning);
        willIcfUid = getIcfUid(accountProvisioning);
    }

    @Test
    public void test200ModifyAccountDrink() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(
                ShadowType.class, ACCOUNT_WILL_OID, DUMMY_ACCOUNT_ATTRIBUTE_DRINK_PATH, "RUM");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        then();
        assertSuccess(result);

        delta.checkConsistence();
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "RUM");

        syncServiceMock.assertSingleNotifySuccessOnly();
    }

    @Test
    public void test201ModifyAccountGossip() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(
                ShadowType.class, ACCOUNT_WILL_OID,
                DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_PATH,
                "pirate");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        then();
        assertSuccess(result);

        delta.checkConsistence();
        assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "pirate");

        syncServiceMock.assertSingleNotifySuccessOnly();
    }

    /**
     * Quote is a non-updatable attribute.
     */
    @Test
    public void test210ModifyAccountQuote() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(
                ShadowType.class, ACCOUNT_WILL_OID,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_PATH,
                "eh?");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        try {
            when();
            provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                    new OperationProvisioningScriptsType(), null, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }
    }

    @Test
    public void test300GetAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = createOperationResult();

        when();
        var shadow = provisioningService.getShadow(ACCOUNT_WILL_OID, null, task, result);

        then();
        assertSuccess(result);
        display("Retrieved account shadow", shadow);

        checkAccountWill(shadow);
        checkUniqueness(shadow);
    }

    @Test
    public void test310SearchAllShadows() throws Exception {
        Task task = getTestTask();
        OperationResult result = createOperationResult();
        ObjectQuery query = createAllShadowsQuery(resourceBean, RI_ACCOUNT_OBJECT_CLASS);
        displayDumpable("All shadows query", query);

        when();
        var allShadows = provisioningService.searchShadows(query, null, task, result);

        then();
        assertSuccess(result);

        display("Found " + allShadows.size() + " shadows");

        assertFalse("No shadows found", allShadows.isEmpty());

        checkUniqueness(allShadows);

        for (var shadow : allShadows) {
            assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);
            assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME);
        }

        // This is "will" only. The daemon (present before 4.9) is not there any more.
        assertEquals("Wrong number of results", 1, allShadows.size());
    }

    // TODO: search

    private void checkAccountWill(AbstractShadow shadow) {
        Collection<ShadowSimpleAttribute<?>> attributes = shadow.getSimpleAttributes();
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42L);
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "RUM");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "At the moment?");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Will Turner");
        assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);
        assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME);
        assertEquals("Unexpected number of attributes", 8, attributes.size());
    }
}
