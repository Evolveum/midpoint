/*
 * Copyright (c) 2013-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.RunAsCapabilityType;

/**
 * Almost the same as TestDummy but this is using a UUID as ICF UID.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyUuidNonUniqueName extends TestDummyUuid {

    public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-uuid-nonunique-name");
    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

    public static final String ACCOUNT_FETTUCINI_NAME = "fettucini";
    public static final File ACCOUNT_FETTUCINI_ALFREDO_FILE = new File(TEST_DIR, "account-alfredo-fettucini.xml");
    public static final String ACCOUNT_FETTUCINI_ALFREDO_OID = "c0c010c0-d34d-b44f-f11d-444400009ffa";
    public static final String ACCOUNT_FETTUCINI_ALFREDO_FULLNAME = "Alfredo Fettucini";
    public static final File ACCOUNT_FETTUCINI_BILL_FILE = new File(TEST_DIR, "account-bill-fettucini.xml");
    public static final String ACCOUNT_FETTUCINI_BILL_OID = "c0c010c0-d34d-b44f-f11d-444400009ffb";
    public static final String ACCOUNT_FETTUCINI_BILL_FULLNAME = "Bill Fettucini";
    public static final String ACCOUNT_FETTUCINI_CARLO_FULLNAME = "Carlo Fettucini";

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected boolean isNameUnique() {
        return false;
    }

    @Override
    protected boolean supportsMemberOf() {
        return false;
    }

    // runAs is using name as an identifier. But name is not unique in this case. runAs won't work.
    @Override
    protected void assertRunAsCapability(RunAsCapabilityType capRunAs) {
        assertNull("Unexpected native runAs capability", capRunAs);
    }

    // runAs is using name as an identifier. But name is not unique in this case. runAs won't work.
    @Override
    protected String getLastModifierName(String expected) {
        return null;
    }

    @Test
    public void test770AddAccountFettuciniAlfredo() throws Exception {
        addFettucini(ACCOUNT_FETTUCINI_ALFREDO_FILE, ACCOUNT_FETTUCINI_ALFREDO_OID, ACCOUNT_FETTUCINI_ALFREDO_FULLNAME);
        searchFettucini(1);
    }

    @Test
    public void test772AddAccountFettuciniBill() throws Exception {
        addFettucini(ACCOUNT_FETTUCINI_BILL_FILE, ACCOUNT_FETTUCINI_BILL_OID, ACCOUNT_FETTUCINI_BILL_FULLNAME);
        searchFettucini(2);
    }

    /**
     * Add directly on resource. Therefore provisioning must create the shadow during search.
     */
    @Test
    public void test774AddAccountFettuciniCarlo() throws Exception {
        dummyResourceCtl.addAccount(ACCOUNT_FETTUCINI_NAME, ACCOUNT_FETTUCINI_CARLO_FULLNAME);
        searchFettucini(3);
    }

    @Override
    @Test
    public void test600AddAccountAlreadyExist() {
        // DO nothing. This test is meaningless in non-unique environment
    }

    private void addFettucini(File file, String oid, String expectedFullName) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> accountToAdd = prismContext.parseObject(file);
        accountToAdd.checkConsistence();

        display("Adding shadow", accountToAdd);

        when();
        String addedObjectOid = provisioningService.addObject(accountToAdd, null, null, task, result);

        then();
        assertSuccessVerbose(result);
        assertEquals(oid, addedObjectOid);

        accountToAdd.checkConsistence();

        String icfUid = assertRepoShadowNew(oid)
                .display()
                .assertName(ACCOUNT_FETTUCINI_NAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertCachedOrigValues(SchemaConstants.ICFS_NAME, ACCOUNT_FETTUCINI_NAME)
                .getIndexedPrimaryIdentifierValueRequired();

        syncServiceMock.assertSingleNotifySuccessOnly();

        var accountAfter = provisioningService.getShadow(oid, null, task, result);
        assertShadowNew(accountAfter)
                .display()
                .assertName(ACCOUNT_FETTUCINI_NAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertOrigValues(SchemaConstants.ICFS_NAME, ACCOUNT_FETTUCINI_NAME)
                .assertOrigValues(SchemaConstants.ICFS_UID, icfUid);

        // Check if the account was created in the dummy resource
        DummyAccount dummyAccount = getDummyAccountAssert(ACCOUNT_FETTUCINI_NAME, icfUid);
        assertNotNull("No dummy account", dummyAccount);
        assertEquals("Fullname is wrong", expectedFullName, dummyAccount.getAttributeValue("fullname"));

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        var repoShadow = getShadowRepo(addedObjectOid);
        assertNotNull("Shadow was not created in the repository", repoShadow);
        displayValue("Repository shadow", repoShadow.debugDump());

        ProvisioningTestUtil.checkRepoAccountShadow(repoShadow);

        // We do not check the uniqueness here, as the icfs:name is not unique here. (That's strange, as it SHOULD be unique.)
        assertSteadyResource();
    }

    private void searchFettucini(int expectedNumberOfFettucinis) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
                .and().item(ShadowType.F_OBJECT_CLASS).eq(RI_ACCOUNT_OBJECT_CLASS)
                .and().itemWithDef(getIcfNameDefinition(), ShadowType.F_ATTRIBUTES, getIcfNameDefinition().getItemName()).eq(ACCOUNT_FETTUCINI_NAME)
                .build();

        // WHEN
        List<PrismObject<ShadowType>> shadows = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
        assertEquals("Wrong number of Fettucinis found", expectedNumberOfFettucinis, shadows.size());
    }

    private PrismPropertyDefinition<String> getIcfNameDefinition() {
        return prismContext.definitionFactory().newPropertyDefinition(SchemaConstants.ICFS_NAME, DOMUtil.XSD_STRING);
    }
}
