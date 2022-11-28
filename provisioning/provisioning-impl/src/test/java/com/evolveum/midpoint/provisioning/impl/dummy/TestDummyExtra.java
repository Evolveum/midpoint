/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.test.IntegrationTestTools.createDetitleDelta;
import static com.evolveum.midpoint.test.IntegrationTestTools.createEntitleDelta;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;

/**
 * Almost the same as TestDummy but with some extra things, such as:
 *
 * 1. readable password,
 * 2. account-account associations,
 * 3. configured `cap:activation` section to reproduce MID-6656.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext
public class TestDummyExtra extends TestDummy {

    public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-extra");
    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

    private static final String DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME = "mate";

    protected static final QName ASSOCIATION_CREW_NAME = new QName(RESOURCE_DUMMY_NS, "crew");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected void extraDummyResourceInit() throws Exception {
        DummyObjectClass accountObjectClass = dummyResource.getAccountObjectClass();
        dummyResourceCtl.addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME, String.class, false, true);
    }

    @Override
    protected int getExpectedRefinedSchemaDefinitions() {
        return super.getExpectedRefinedSchemaDefinitions() + 1;
    }

    @Override
    protected void assertSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType) throws Exception {
        // schema is extended, displayOrders are changed
        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(resourceSchema, resourceType, false, 20);

        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        ResourceObjectDefinition accountRDef = refinedSchema.findDefaultDefinitionForKindRequired(ShadowKindType.ACCOUNT);

        Collection<ResourceAssociationDefinition> associationDefinitions = accountRDef.getAssociationDefinitions();
        assertEquals("Wrong number of association defs", 3, associationDefinitions.size());
        ResourceAssociationDefinition crewAssociationDef = accountRDef.findAssociationDefinition(ASSOCIATION_CREW_NAME);
        assertNotNull("No definition for crew association", crewAssociationDef);
    }

    @Override
    protected void assertNativeCredentialsCapability(CredentialsCapabilityType capCred) {
        PasswordCapabilityType passwordCapabilityType = capCred.getPassword();
        assertNotNull("password native capability not present", passwordCapabilityType);
        Boolean readable = passwordCapabilityType.isReadable();
        assertNotNull("No 'readable' inducation in password capability", readable);
        assertTrue("Password not 'readable' in password capability", readable);
    }

    @Test
    public void test400AddAccountElizabeth() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_ELIZABETH_FILE);
        account.checkConsistence();

        display("Adding shadow", account);

        // WHEN
        when();
        String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertEquals(ACCOUNT_ELIZABETH_OID, addedObjectOid);

        account.checkConsistence();

        syncServiceMock.assertSingleNotifySuccessOnly();

        PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_ELIZABETH_OID, null, task, result);

        display("Account will from provisioning", accountProvisioning);

        DummyAccount dummyAccount = getDummyAccountAssert(ACCOUNT_ELIZABETH_USERNAME, ACCOUNT_ELIZABETH_USERNAME);
        assertNotNull("No dummy account", dummyAccount);
        assertTrue("The account is not enabled", dummyAccount.isEnabled());

        checkUniqueness(accountProvisioning);
        assertSteadyResource();
    }

    /**
     * MID-2668
     */
    @Test
    public void test410AssociateCrewWillElizabeth() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = createEntitleDelta(ACCOUNT_WILL_OID, ASSOCIATION_CREW_NAME, ACCOUNT_ELIZABETH_OID);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        syncServiceMock.assertSingleNotifySuccessOnly();
        delta.checkConsistence();

        DummyAccount dummyAccountWill = getDummyAccountAssert(ACCOUNT_WILL_USERNAME, ACCOUNT_WILL_USERNAME);
        displayDumpable("Dummy account will", dummyAccountWill);
        assertNotNull("No dummy account will", dummyAccountWill);
        assertTrue("The account will is not enabled", dummyAccountWill.isEnabled());
        assertDummyAttributeValues(dummyAccountWill, DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME, ACCOUNT_ELIZABETH_USERNAME);

        PrismObject<ShadowType> accountWillProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);
        display("Account will from provisioning", accountWillProvisioning);
        assertAssociation(accountWillProvisioning, ASSOCIATION_CREW_NAME, ACCOUNT_ELIZABETH_OID);

        assertSteadyResource();
    }

    /**
     * MID-2668
     */
    @Test
    public void test419DisassociateCrewWillElizabeth() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = createDetitleDelta(ACCOUNT_WILL_OID, ASSOCIATION_CREW_NAME, ACCOUNT_ELIZABETH_OID);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        result.computeStatus();
        display("modifyObject result", result);
        TestUtil.assertSuccess(result);

        syncServiceMock.assertSingleNotifySuccessOnly();
        delta.checkConsistence();

        DummyAccount dummyAccountWill = getDummyAccountAssert(ACCOUNT_WILL_USERNAME, ACCOUNT_WILL_USERNAME);
        displayDumpable("Dummy account will", dummyAccountWill);
        assertNotNull("No dummy account will", dummyAccountWill);
        assertTrue("The account will is not enabled", dummyAccountWill.isEnabled());
        assertNoDummyAttribute(dummyAccountWill, DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME);

        PrismObject<ShadowType> accountWillProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);
        display("Account will from provisioning", accountWillProvisioning);
        assertNoAssociation(accountWillProvisioning, ASSOCIATION_CREW_NAME, ACCOUNT_ELIZABETH_OID);

        assertSteadyResource();
    }

    // TODO: disassociate

    @Test
    public void test499DeleteAccountElizabeth() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        // WHEN
        when();
        provisioningService.deleteObject(ShadowType.class, ACCOUNT_ELIZABETH_OID, null, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        syncServiceMock.assertSingleNotifySuccessOnly();
        assertNoRepoObject(ShadowType.class, ACCOUNT_ELIZABETH_OID);

        assertNoDummyAccount(ACCOUNT_ELIZABETH_USERNAME, ACCOUNT_ELIZABETH_USERNAME);

        assertSteadyResource();
    }

    @Override
    protected void checkAccountWill(
            PrismObject<ShadowType> shadow, OperationResult result, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs)
            throws SchemaException, EncryptionException, ConfigurationException {
        super.checkAccountWill(shadow, result, startTs, endTs);
        assertPassword(shadow.asObjectable(), accountWillCurrentPassword);
    }
}
