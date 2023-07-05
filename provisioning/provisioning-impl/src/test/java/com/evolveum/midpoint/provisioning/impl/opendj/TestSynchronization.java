/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.opendj;

import static org.testng.AssertJUnit.*;

import java.io.File;

import com.evolveum.midpoint.provisioning.impl.DummyTokenStorageImpl;
import com.evolveum.midpoint.provisioning.impl.MockLiveSyncTaskHandler;

import com.evolveum.midpoint.schema.ResourceOperationCoordinates;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.opends.server.core.AddOperation;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.types.ResultCode;
import org.opends.server.util.LDIFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.impl.mock.SynchronizationServiceMock;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.TestUtil;

@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestSynchronization extends AbstractIntegrationTest {

    protected static final TestObject<ArchetypeType> ARCHETYPE_OBJECT_MARK = TestObject.classPath(
            "initial-objects/archetype", "701-archetype-object-mark.xml", SystemObjectsType.ARCHETYPE_OBJECT_MARK.value());

    private static final TestObject<MarkType> MARK_PROTECTED_SHADOW = TestObject.classPath(
            "initial-objects/mark", "800-mark-protected.xml",
            SystemObjectsType.MARK_PROTECTED.value());

    private static final File TEST_DIR = new File("src/test/resources/synchronization/");

    private static final File RESOURCE_OPENDJ_FILE = AbstractOpenDjTest.RESOURCE_OPENDJ_FILE;

    private static final File LDIF_WILL_FILE = new File(TEST_DIR, "will.ldif");
    private static final File LDIF_CALYPSO_FILE = new File(TEST_DIR, "calypso.ldif");

    private static final String ACCOUNT_WILL_NAME = "uid=wturner,ou=People,dc=example,dc=com";

    private ResourceType resourceType;

    @Autowired private ProvisioningService provisioningService;
    @Autowired private ResourceObjectChangeListener syncServiceMock;
    @Autowired private MockLiveSyncTaskHandler mockLiveSyncTaskHandler;

    private final DummyTokenStorageImpl tokenStorage = new DummyTokenStorageImpl(0);

    @BeforeClass
    public static void startLdap() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopLdap() {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // We need to switch off the encryption checks. Some values cannot be encrypted as we do
        // not have a definition here
        InternalsConfig.encryptionChecks = false;
        // let provisioning discover the connectors
        provisioningService.postInit(initResult);

        resourceType = addResourceFromFile(RESOURCE_OPENDJ_FILE, IntegrationTestTools.CONNECTOR_LDAP_TYPE, initResult).asObjectable();

        //it is needed to declare the task owner, so we add the user admin to the reposiotry
        repoAddObjectFromFile(ProvisioningTestUtil.USER_ADMIN_FILE, initResult);
        if(areMarksSupported()) {
            repoAdd(ARCHETYPE_OBJECT_MARK, initResult);
            repoAdd(MARK_PROTECTED_SHADOW, initResult);
        }
    }

    @Test
    public void test010Sanity() throws Exception {
        final OperationResult result = createOperationResult();

        // WHEN
        PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, resourceType.getOid(), null, taskManager.createTaskInstance(), result);

        // THEN
        assertNotNull("Resource is null", resource);
        display("getObject(resource)", resource);

        result.computeStatus();
        display("getObject(resource) result", result);
        TestUtil.assertSuccess(result);

        // Make sure these were generated
        assertNotNull("No resource schema", resource.asObjectable().getSchema());
        assertNotNull("No native capabilities", resource.asObjectable().getCapabilities().getNative());

        tokenStorage.assertToken(0);
    }

    @Test
    public void test100SyncAddWill() throws Exception {
        final OperationResult result = createOperationResult();

        tokenStorage.assertToken(0);
        ((SynchronizationServiceMock) syncServiceMock).reset();

        // create add change in embedded LDAP
        LDIFImportConfig importConfig = new LDIFImportConfig(LDIF_WILL_FILE.getPath());
        LDIFReader ldifReader = new LDIFReader(importConfig);
        Entry entry = ldifReader.readEntry();
        display("Entry from LDIF", entry);

        AddOperation addOperation = openDJController.getInternalConnection().processAdd(entry);

        AssertJUnit.assertEquals("LDAP add operation failed", ResultCode.SUCCESS,
                addOperation.getResultCode());

        ResourceOperationCoordinates coords =
                ResourceOperationCoordinates.ofObjectClass(
                        resourceType.getOid(), AbstractOpenDjTest.RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);

        // WHEN

        mockLiveSyncTaskHandler.synchronize(coords, tokenStorage, getTestTask(), result);

        // THEN
        SynchronizationServiceMock mock = (SynchronizationServiceMock) syncServiceMock;

        assertEquals("Unexpected number of synchronization service calls", 1, mock.getCallCount());

        ResourceObjectShadowChangeDescription lastChange = mock.getLastChange();
//            ObjectDelta<? extends ShadowType> objectDelta = lastChange.getObjectDelta();
//            assertNotNull("Null object delta in change notification", objectDelta);
//            assertEquals("Wrong change type in delta in change notification", ChangeType.ADD, objectDelta.getChangeType());
        PrismObject<? extends ShadowType> currentShadow = lastChange.getShadowedResourceObject();
        assertNotNull("No current shadow in change notification", currentShadow);

        // TODO why is the value lowercased? Is it because it was taken from the change and not fetched from the resource?
        assertEquals("Wrong shadow name", ACCOUNT_WILL_NAME.toLowerCase(), currentShadow.asObjectable().getName().getOrig());

        ShadowType shadow = currentShadow.asObjectable();
        MetadataType metadata = shadow.getMetadata();
        assertTrue("Shadow doesn't have metadata", metadata != null && metadata.getCreateTimestamp() != null);

        tokenStorage.assertToken(1);
    }

    @Test
    public void test500SyncAddProtected() throws Exception {
        final OperationResult result = createOperationResult();

        tokenStorage.assertToken(1);
        ((SynchronizationServiceMock) syncServiceMock).reset();

        // create add change in embedded LDAP
        LDIFImportConfig importConfig = new LDIFImportConfig(LDIF_CALYPSO_FILE.getPath());
        LDIFReader ldifReader = new LDIFReader(importConfig);
        Entry entry = ldifReader.readEntry();
        ldifReader.close();
        display("Entry from LDIF", entry);
        AddOperation addOperation = openDJController.getInternalConnection().processAdd(entry);

        AssertJUnit.assertEquals("LDAP add operation failed", ResultCode.SUCCESS,
                addOperation.getResultCode());

        ResourceOperationCoordinates coords =
                ResourceOperationCoordinates.ofObjectClass(
                        resourceType.getOid(), AbstractOpenDjTest.RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);

        // WHEN
        mockLiveSyncTaskHandler.synchronize(coords, tokenStorage, getTestTask(), result);

        // THEN
        SynchronizationServiceMock mock = (SynchronizationServiceMock) syncServiceMock;

        assertEquals("Unexpected number of synchronization service calls", 0, mock.getCallCount());

//        ResourceObjectShadowChangeDescription lastChange = mock.getLastChange();
//        PrismObject<? extends ShadowType> currentShadow = lastChange.getCurrentShadow();
//        assertNotNull("No current shadow in change notification", currentShadow);
//        assertNotNull("No old shadow in change notification", lastChange.getOldShadow());
//
//        assertEquals("Wrong shadow name", PrismTestUtil.createPolyStringType(ACCOUNT_CALYPSO_NAME), currentShadow.asObjectable().getName());
//
//        assertNotNull("Calypso is not protected", currentShadow.asObjectable().isProtectedObject());
//        assertTrue("Calypso is not protected", currentShadow.asObjectable().isProtectedObject());

        tokenStorage.assertToken(2);
    }

}
