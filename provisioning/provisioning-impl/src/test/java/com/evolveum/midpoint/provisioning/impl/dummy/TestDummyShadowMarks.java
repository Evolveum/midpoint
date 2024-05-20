/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.LiveSyncTokenStorage;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.DummyTokenStorageImpl;
import com.evolveum.midpoint.schema.ResourceOperationCoordinates;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyShadowMarks extends AbstractBasicDummyTest {

    private static final File RESOURCE_DUMMY_SHADOW_MARKS_FILE = new File(TEST_DIR,"resource-dummy-shadow-marks.xml");

    private static final String MARK_PROTECTED_OID = SystemObjectsType.MARK_PROTECTED.value();

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_SHADOW_MARKS_FILE;
    }

    @Test
    public void test200MarkShadowProtected() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, task, result);

        assertNull(account + " is not protected", account.asObjectable().isProtectedObject());

        PolicyStatementType policyStat = new PolicyStatementType()
                .markRef(SystemObjectsType.MARK_PROTECTED.value(), MarkType.COMPLEX_TYPE)
                .type(PolicyStatementTypeType.APPLY);

        ObjectDelta<ShadowType> shadowDelta = prismContext.deltaFactory().object()
                .createModificationAddContainer(ShadowType.class, ACCOUNT_DAEMON_OID, ShadowType.F_POLICY_STATEMENT, policyStat);
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_DAEMON_OID, shadowDelta.getModifications(), null, null, task, result);

     // THEN
        assertSuccess(result);

        var accountAfter = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, task, result);

        assertEquals("Provisioning: Effective mark references Protected", MARK_PROTECTED_OID, accountAfter.asObjectable().getEffectiveMarkRef().get(0).getOid());
        assertEquals(accountAfter + " is not protected", Boolean.TRUE, accountAfter.asObjectable().isProtectedObject());

        var accountRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, result);
        assertEquals("Repository: Effective mark references Protected", MARK_PROTECTED_OID, accountRepo.asObjectable().getEffectiveMarkRef().get(0).getOid());
        assertSteadyResource();
    }


    @Test
    public void test201GetProtectedAccountShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        var account = provisioningService.getShadow(ACCOUNT_DAEMON_OID, null, task, result);

        assertTrue(account + " is not protected", account.isProtectedObject());
        checkUniqueness(account);

        assertSuccess(result);
        assertSteadyResource();
    }

    /**
     * Attribute modification should fail.
     */
    @Test
    public void test202ModifyProtectedAccountShadowAttributes() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        Collection<PropertyDelta<String>> modifications = new ArrayList<>(1);
        ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);
        ResourceObjectClassDefinition defaultAccountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        ShadowSimpleAttributeDefinition<String> fullnameAttrDef = defaultAccountDefinition.findSimpleAttributeDefinition("fullname");
        ShadowSimpleAttribute<String> fullnameAttr = fullnameAttrDef.instantiate();
        PropertyDelta<String> fullnameDelta = fullnameAttr.createDelta(ItemPath.create(ShadowType.F_ATTRIBUTES,
                fullnameAttrDef.getItemName()));
        fullnameDelta.setRealValuesToReplace("Good Daemon");
        modifications.add(fullnameDelta);

        // WHEN
        try {
            provisioningService.modifyObject(ShadowType.class, ACCOUNT_DAEMON_OID, modifications, null, null, task, result);
            AssertJUnit.fail("Expected security exception while modifying 'daemon' account");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }

        assertFailure(result);
        syncServiceMock.assertSingleNotifyFailureOnly();

//        checkConsistency();

        assertSteadyResource();
    }

    /**
     * Modification of non-attribute property should go OK.
     */
    @Test
    public void test203ModifyProtectedAccountShadowProperty() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> shadowDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(ShadowType.class, ACCOUNT_DAEMON_OID,
                        ShadowType.F_SYNCHRONIZATION_SITUATION, SynchronizationSituationType.DISPUTED);

        // WHEN
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_DAEMON_OID, shadowDelta.getModifications(), null, null, task, result);

        // THEN
        assertSuccess(result);

        syncServiceMock.assertSingleNotifySuccessOnly();

        PrismObject<ShadowType> shadowAfter = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, task, result);
        assertEquals("Wrong situation", SynchronizationSituationType.DISPUTED, shadowAfter.asObjectable().getSynchronizationSituation());

        var accountRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, result);
        assertEquals("Repository: Effective mark references Protected", MARK_PROTECTED_OID, accountRepo.asObjectable().getEffectiveMarkRef().get(0).getOid());

        assertSteadyResource();
    }

    @Test
    public void test209DeleteProtectedAccountShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        try {
            when();

            provisioningService.deleteObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, null, task, result);

            AssertJUnit.fail("Expected security exception while deleting 'daemon' account");
        } catch (SecurityViolationException e) {
            then();
            displayExpectedException(e);
        }

        assertFailure(result);

        syncServiceMock.assertSingleNotifyFailureOnly();

//        checkConsistency();

        assertSteadyResource();
    }

    private PrismObject<ShadowType> createAccountShadow(String username) throws SchemaException, ConfigurationException {
        ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);
        ResourceObjectClassDefinition defaultAccountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        ShadowType shadowType = new ShadowType();
        shadowType.setName(PrismTestUtil.createPolyStringType(username));
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resource.getOid());
        shadowType.setResourceRef(resourceRef);
        shadowType.setObjectClass(defaultAccountDefinition.getTypeName());
        PrismObject<ShadowType> shadow = shadowType.asPrismObject();
        PrismContainer<Containerable> attrsCont = shadow.findOrCreateContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<String> icfsNameProp = attrsCont.findOrCreateProperty(SchemaConstants.ICFS_NAME);
        icfsNameProp.setRealValue(username);
        return shadow;
    }

    @Test
    protected void testAddProtectedAccount(String username) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> shadow = createAccountShadow(username);

        // WHEN
        try {
            provisioningService.addObject(shadow, null, null, task, result);
            AssertJUnit.fail("Expected security exception while adding '" + username + "' account");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }

        assertFailure(result);

        syncServiceMock.assertSingleNotifyFailureOnly();

        assertSteadyResource();
    }

    private final LiveSyncTokenStorage tokenStorage = new DummyTokenStorageImpl();

    @Test
    public void test300LiveSyncInit() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyResource.setSyncStyle(DummySyncStyle.DUMB);
        dummyResource.clearDeltas();
        syncServiceMock.reset();

        // Dry run to remember the current sync token in the task instance.
        // Otherwise a last sync token would be used and
        // no change would be detected
        ResourceOperationCoordinates coords = getDefaultAccountObjectClassCoordinates();

        when();
        mockLiveSyncTaskHandler.synchronize(coords, tokenStorage, task, result);

        then();
        assertSuccess(result);

        // No change, no fun
        syncServiceMock.assertNoNotifyChange();

        checkAllShadows();

        assertSteadyResource();
    }

    @Test
    public void test301LiveSyncModifyProtectedAccount() throws Exception {
        // GIVEN
        Task syncTask = getTestTask();
        OperationResult result = syncTask.getResult();

        syncServiceMock.reset();

        DummyAccount dummyAccount = getDummyAccountAssert(ACCOUNT_DAEMON_USERNAME, daemonIcfUid);
        dummyAccount.replaceAttributeValue("fullname", "Maxwell deamon");

        ResourceOperationCoordinates coords = getDefaultAccountObjectClassCoordinates();

        when();
        mockLiveSyncTaskHandler.synchronize(coords, tokenStorage, syncTask, result);

        then();
        assertSuccess(result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        syncServiceMock.assertNoNotifyChange();

        checkAllShadows();

        assertSteadyResource();
    }



    private @NotNull ResourceOperationCoordinates getDefaultAccountObjectClassCoordinates() {
        return ResourceOperationCoordinates.ofObjectClass(
                RESOURCE_DUMMY_OID,
                RI_ACCOUNT_OBJECT_CLASS);
    }

    /**
     * Modification of non-attribute property should go OK.
     */
    @Test
    public void test400RemoveProtectedMark() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();


        PolicyStatementType policyStat = new PolicyStatementType()
                .markRef(SystemObjectsType.MARK_PROTECTED.value(), MarkType.COMPLEX_TYPE)
                .type(PolicyStatementTypeType.APPLY);
        ObjectDelta<ShadowType> shadowDelta = prismContext.deltaFactory().object()
                .createModificationDeleteContainer(ShadowType.class, ACCOUNT_DAEMON_OID, ShadowType.F_POLICY_STATEMENT, policyStat);

        // WHEN
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_DAEMON_OID, shadowDelta.getModifications(), null, null, task, result);

        // THEN
        assertSuccess(result);

        syncServiceMock.assertSingleNotifySuccessOnly();

        PrismObject<ShadowType> accountAfter = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, task, result);
        assertNull(accountAfter + " is not protected", accountAfter.asObjectable().isProtectedObject());

        var accountRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, result);
        assertTrue("Repository: Effective marks should be empty", accountRepo.asObjectable().getEffectiveMarkRef().isEmpty());
        assertSteadyResource();
    }

    @Test
    public void test401DeleteUnprotectedAccountShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();


        when();

        provisioningService.deleteObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, null, task, result);
        assertSuccess(result);

        syncServiceMock.assertSingleNotifySuccessOnly();
        assertSteadyResource();
    }

    @SuppressWarnings("ConstantConditions")
    private void checkAllShadows() throws SchemaException, ConfigurationException {
        ObjectChecker<ShadowType> checker = null;
        IntegrationTestTools.checkAllShadows(resourceBean, repositoryService, checker);
    }
}
