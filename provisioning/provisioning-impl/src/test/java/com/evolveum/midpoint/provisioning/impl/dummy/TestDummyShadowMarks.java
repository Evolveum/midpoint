/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil.getDefaultAccountObjectClass;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;


import static java.util.Objects.requireNonNull;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;


import com.evolveum.midpoint.provisioning.api.*;

import com.evolveum.midpoint.provisioning.impl.DummyTokenStorageImpl;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;

import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.*;
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
    private static final String BLACKBEARD_USERNAME = "BlackBeard";
    private static final String DRAKE_USERNAME = "Drake";
    // Make this ugly by design. it check for some caseExact/caseIgnore cases
    private static final String ACCOUNT_MURRAY_USERNAME = "muRRay";

    static final long VALID_FROM_MILLIS = 12322342345435L;
    static final long VALID_TO_MILLIS = 3454564324423L;

    private static final String GROUP_CORSAIRS_NAME = "corsairs";

    private static final TestObject<ArchetypeType> ARCHETYPE_SHADOW_MARK= TestObject.classPath(
            "initial-objects/archetype", "702-archetype-shadow-mark.xml",
            SystemObjectsType.ARCHETYPE_SHADOW_MARK.value());

    private static final TestObject<MarkType> TAG_PROTECTED_SHADOW = TestObject.classPath(
            "initial-objects/mark", "750-mark-protected-shadow.xml",
            SystemObjectsType.MARK_PROTECTED_SHADOW.value());


    String piratesIcfUid;

    protected String getMurrayRepoIcfName() {
        return ACCOUNT_MURRAY_USERNAME;
    }

    protected String getBlackbeardRepoIcfName() {
        return BLACKBEARD_USERNAME;
    }

    protected String getDrakeRepoIcfName() {
        return DRAKE_USERNAME;
    }

    protected ItemComparisonResult getExpectedPasswordComparisonResultMatch() {
        return ItemComparisonResult.NOT_APPLICABLE;
    }

    protected ItemComparisonResult getExpectedPasswordComparisonResultMismatch() {
        return ItemComparisonResult.NOT_APPLICABLE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        if (areMarksSupported()) {
            repoAdd(ARCHETYPE_SHADOW_MARK, initResult);
            repoAdd(TAG_PROTECTED_SHADOW, initResult);
        }
    }

    // test000-test100 in the superclasses



    // test102-test106 in the superclasses

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_SHADOW_MARKS_FILE;
    }


    protected Integer getTest18xApproxNumberOfSearchResults() {
        return 5;
    }

    protected String[] getSortedUsernames18x() {
        return new String[] { transformNameFromResource("Will"), "carla", "daemon", "meathook", transformNameFromResource("morgan") };
    }









    // test28x in TestDummyCaseIgnore



    // test4xx reserved for subclasses

    @Test
    public void test200MarkShadowProtected() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, task, result);

        assertNull("" + account + " is not protected", account.asObjectable().isProtectedObject());

        PolicyStatementType policyStat = new PolicyStatementType()
                .markRef(SystemObjectsType.MARK_PROTECTED_SHADOW.value(), MarkType.COMPLEX_TYPE)
                .type(PolicyStatementTypeType.APPLY);

        ObjectDelta<ShadowType> shadowDelta = prismContext.deltaFactory().object()
                .createModificationAddContainer(ShadowType.class, ACCOUNT_DAEMON_OID, ShadowType.F_POLICY_STATEMENT, policyStat);
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_DAEMON_OID, shadowDelta.getModifications(), null, null, task, result);

     // THEN
        assertSuccess(result);

        var accountAfter = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, task, result);

        assertEquals("" + accountAfter + " is not protected", Boolean.TRUE, accountAfter.asObjectable().isProtectedObject());

        assertSteadyResource();
    }


    @Test
    public void test201GetProtectedAccountShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, task, result);

        assertEquals("" + account + " is not protected", Boolean.TRUE, account.asObjectable().isProtectedObject());
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
        ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchemaRequired(resource.asObjectable());
        ResourceObjectClassDefinition defaultAccountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        //noinspection unchecked
        ResourceAttributeDefinition<String> fullnameAttrDef =
                (ResourceAttributeDefinition<String>) defaultAccountDefinition.findAttributeDefinition("fullname");
        ResourceAttribute<String> fullnameAttr = fullnameAttrDef.instantiate();
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

    private PrismObject<ShadowType> createAccountShadow(String username) throws SchemaException {
        ResourceSchema resourceSchema = requireNonNull(ResourceSchemaFactory.getRawSchema(resource));
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
                getDefaultAccountObjectClass(resourceBean));
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
                .markRef(SystemObjectsType.MARK_PROTECTED_SHADOW.value(), MarkType.COMPLEX_TYPE)
                .type(PolicyStatementTypeType.APPLY);
        ObjectDelta<ShadowType> shadowDelta = prismContext.deltaFactory().object()
                .createModificationDeleteContainer(ShadowType.class, ACCOUNT_DAEMON_OID, ShadowType.F_POLICY_STATEMENT, policyStat);

        // WHEN
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_DAEMON_OID, shadowDelta.getModifications(), null, null, task, result);

        // THEN
        assertSuccess(result);

        syncServiceMock.assertSingleNotifySuccessOnly();

        PrismObject<ShadowType> accountAfter = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, task, result);
        assertNull("" + accountAfter + " is not protected", accountAfter.asObjectable().isProtectedObject());

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


    // test999 shutdown in the superclass

    @SuppressWarnings("SameParameterValue")
    protected void checkCachedAccountShadow(
            PrismObject<ShadowType> shadowType,
            OperationResult parentResult,
            boolean fullShadow,
            XMLGregorianCalendar startTs,
            XMLGregorianCalendar endTs) throws SchemaException, ConfigurationException {
        checkAccountShadow(shadowType, parentResult, fullShadow);
    }

    private void checkEntitlementShadow(
            PrismObject<ShadowType> shadow, OperationResult parentResult, String objectClassLocalName, boolean fullShadow)
            throws SchemaException, ConfigurationException {
        ObjectChecker<ShadowType> checker = createShadowChecker(fullShadow);
        ShadowUtil.checkConsistence(shadow, parentResult.getOperation());
        IntegrationTestTools.checkEntitlementShadow(
                shadow.asObjectable(),
                resourceBean,
                repositoryService,
                checker,
                objectClassLocalName,
                getUidMatchingRule(),
                prismContext,
                parentResult);
    }

    @SuppressWarnings("ConstantConditions")
    private void checkAllShadows() throws SchemaException, ConfigurationException {
        ObjectChecker<ShadowType> checker = null;
        IntegrationTestTools.checkAllShadows(resourceBean, repositoryService, checker, prismContext);
    }

    protected void checkRepoEntitlementShadow(PrismObject<ShadowType> repoShadow) {
        ProvisioningTestUtil.checkRepoEntitlementShadow(repoShadow);
    }

    protected void assertSyncOldShadow(PrismObject<? extends ShadowType> oldShadow, String repoName) {
        assertSyncOldShadow(oldShadow, repoName, 2);
    }

    void assertSyncOldShadow(PrismObject<? extends ShadowType> oldShadow, String repoName, Integer expectedNumberOfAttributes) {
        assertNotNull("Old shadow missing", oldShadow);
        assertNotNull("Old shadow does not have an OID", oldShadow.getOid());
        PrismAsserts.assertClass("old shadow", ShadowType.class, oldShadow);
        ShadowType oldShadowType = oldShadow.asObjectable();
        ResourceAttributeContainer attributesContainer = ShadowUtil
                .getAttributesContainer(oldShadowType);
        assertNotNull("No attributes container in old shadow", attributesContainer);
        Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
        assertFalse("Attributes container is empty", attributes.isEmpty());
        if (expectedNumberOfAttributes != null) {
            assertEquals("Unexpected number of attributes", (int) expectedNumberOfAttributes, attributes.size());
        }
        ResourceAttribute<?> icfsNameAttribute = attributesContainer.findAttribute(SchemaConstants.ICFS_NAME);
        assertNotNull("No ICF name attribute in old  shadow", icfsNameAttribute);
        assertEquals("Wrong value of ICF name attribute in old  shadow", repoName,
                icfsNameAttribute.getRealValue());
    }
}
