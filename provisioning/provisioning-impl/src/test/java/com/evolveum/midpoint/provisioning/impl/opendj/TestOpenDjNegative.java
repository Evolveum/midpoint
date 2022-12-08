/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import static org.testng.AssertJUnit.*;

import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.impl.DummyTokenStorageImpl;

import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Test for provisioning service implementation. Using OpenDJ. But NOT STARTING IT.
 * Checking if appropriate errors are provided.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjNegative extends AbstractOpenDjTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddShadowFromFile(ACCOUNT_JBOND_REPO_FILE, initResult);
        repoAddShadowFromFile(ACCOUNT_SPARROW_REPO_FILE, initResult);
        repoAddShadowFromFile(ACCOUNT_JACK_REPO_FILE, initResult);
    }

    @BeforeClass
    public void stopLdap() {
        // Make sure that OpenDJ is stopped. We want to see the blood .. err ... errors
        try {
            openDJController.stop();
        } catch (Exception ex) {
            logger.trace("Exception during stopping already stopped LDAP (probably harmless)", ex);
        }
    }

    @Test
    public void test003Connection() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ResourceType resourceTypeBefore = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, result).asObjectable();
        display("Resource before testResource (repository)", resourceTypeBefore);
        assertNotNull("No connector ref", resourceTypeBefore.getConnectorRef());
        assertNotNull("No connector ref OID", resourceTypeBefore.getConnectorRef().getOid());
        connector = repositoryService.getObject(ConnectorType.class, resourceTypeBefore.getConnectorRef().getOid(), null, result);
        ConnectorType connectorType = connector.asObjectable();
        assertNotNull(connectorType);
        Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceTypeBefore);
        AssertJUnit.assertNull("Found schema element before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

        // WHEN
        OperationResult operationResult = provisioningService.testResource(RESOURCE_OPENDJ_OID, task, result);

        display("Test connection result (expected failure)", operationResult);
        TestUtil.assertFailure(operationResult);

        PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, result);
        display("Resource after testResource (repository)", resourceRepoAfter);
        ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
        displayValue("Resource after testResource (repository, XML)", PrismTestUtil.serializeToXml(resourceTypeRepoAfter));

        Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
        assertNull("Schema after test connection (and should not be)", resourceXsdSchemaElementAfter);
    }

    @Test
    public void test004ResourceAndConnectorCaching() throws Exception {
        OperationResult result = createOperationResult();
        Task task = createPlainTask();
        // WHEN
        // This should NOT throw an exception. It should just indicate the failure in results
        resource = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result);
        ResourceType resourceType = resource.asObjectable();

        // THEN
        result.computeStatus();
        display("getObject(resource) result", result);
        TestUtil.assertFailure(result);
        TestUtil.assertFailure(resource.asObjectable().getFetchResult());

        ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchema(resource);
        assertNull("Resource schema found", resourceSchema);

        // WHEN
        PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result);

        // THEN
        result.computeStatus();
        display("getObject(resourceAgain) result", result);
        TestUtil.assertFailure(result);
        TestUtil.assertFailure(resourceAgain.asObjectable().getFetchResult());

        ResourceType resourceTypeAgain = resourceAgain.asObjectable();
        assertNotNull("No connector ref", resourceTypeAgain.getConnectorRef());
        assertNotNull("No connector ref OID", resourceTypeAgain.getConnectorRef().getOid());

        PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        PrismContainer<Containerable> configurationContainerAgain = resourceAgain.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));
        assertTrue("Configurations not equals", configurationContainer.equals(configurationContainerAgain));

        ResourceSchema resourceSchemaAgain = ResourceSchemaFactory.getRawSchema(resourceAgain);
        assertNull("Resource schema (again)", resourceSchemaAgain);
    }

    /**
     * This goes to local repo, therefore the expected result is ObjectNotFound.
     * We know that the shadow does not exist.
     */
    @Test
    public void test110GetObjectNoShadow() throws Exception {
        OperationResult result = getTestOperationResult();

        try {
            provisioningService.getObject(ShadowType.class, NON_EXISTENT_OID,
                    null, taskManager.createTaskInstance(), result).asObjectable();

            AssertJUnit.fail("getObject succeeded unexpectedly");
        } catch (ObjectNotFoundException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    /**
     * This is using the shadow to go to the resource. But it cannot as OpenDJ is down.
     * It even cannot fetch schema. If there is no schema it does not even know how to process
     * identifiers in the shadow. Therefore the expected result is ConfigurationException (CommunicationException).
     * It must not be ObjectNotFound as we do NOT know that the shadow does not exist.
     */
    @Test
    public void test111GetObjectShadow() throws Exception {
        OperationResult result = getTestOperationResult();

        try {
            provisioningService.getObject(ShadowType.class, ACCOUNT_JBOND_OID,
                    null, taskManager.createTaskInstance(), result).asObjectable();

            AssertJUnit.fail("getObject succeeded unexpectedly");
        } catch (ConfigurationException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    @Test
    public void test121SearchAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        final String resourceNamespace = MidPointConstants.NS_RI;
        QName objectClass = new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME);

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), objectClass);

        try {

            // WHEN
            provisioningService.searchObjects(ShadowType.class, query, null, task, result);

            AssertJUnit.fail("searchObjectsIterative succeeded unexpectedly");
        } catch (ConfigurationException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        display(result);
        TestUtil.assertFailure(result);
    }

    @Test
    public void test122SearchAccountsIterative() throws SchemaException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        final String resourceNamespace = MidPointConstants.NS_RI;
        QName objectClass = new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME);

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), objectClass, prismContext);

        ResultHandler<ShadowType> handler = (shadow, lResult) -> {
            AssertJUnit.fail("handler called unexpectedly");
            return false;
        };

        try {

            // WHEN
            provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, task, result);

            AssertJUnit.fail("searchObjectsIterative succeeded unexpectedly");
        } catch (ConfigurationException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    @Test
    public void test130AddAccountWill() throws Exception {
        // GIVEN
        OperationResult result = getTestOperationResult();

        ShadowType object = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);

        display("Account to add", object);

        try {
            // WHEN
            provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);

            AssertJUnit.fail("addObject succeeded unexpectedly");
        } catch (ConfigurationException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    @Test
    public void test140AddDeleteAccountSparrow() throws Exception {
        // GIVEN
        OperationResult result = getTestOperationResult();

        try {

            provisioningService.deleteObject(ShadowType.class, ACCOUNT_SPARROW_OID, null, null, taskManager.createTaskInstance(), result);

            AssertJUnit.fail("addObject succeeded unexpectedly");
        } catch (ConfigurationException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);

    }

    @Test
    public void test150ModifyObject() throws Exception {
        // GIVEN
        OperationResult result = getTestOperationResult();

        ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(ACCOUNT_JACK_CHANGE_FILE, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, ShadowType.class, PrismTestUtil.getPrismContext());
        displayDumpable("Object change", delta);

        try {

            provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
                    delta.getModifications(), null, null, taskManager.createTaskInstance(), result);

            AssertJUnit.fail("addObject succeeded unexpectedly");
        } catch (ConfigurationException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    @Test
    public void test190Synchronize() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ResourceOperationCoordinates coords =
                ResourceOperationCoordinates.ofObjectClass(RESOURCE_OPENDJ_OID, OBJECT_CLASS_INETORGPERSON_QNAME);

        try {

            mockLiveSyncTaskHandler.synchronize(coords, new DummyTokenStorageImpl(), task, result);

            AssertJUnit.fail("synchronize succeeded unexpectedly");
        } catch (ConfigurationException e) {
            // When asking for specific object class, we expect configuration exception.
            // (Because there's no schema.)
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    @Test
    public void test195SynchronizeAllClasses() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ResourceOperationCoordinates coords = ResourceOperationCoordinates.ofResource(RESOURCE_OPENDJ_OID);

        try {

            mockLiveSyncTaskHandler.synchronize(coords, new DummyTokenStorageImpl(), task, result);

            AssertJUnit.fail("synchronize succeeded unexpectedly");
        } catch (CommunicationException e) {
            // Now the problem is right in the sync ConnId call.
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    // =========================================================================================================
    // Now lets replace the resource with one that has schema and capabilities. And re-run some of the tests.
    // OpenDJ is still down so the results should be the same. But the code may take a different path if
    // schema is present.
    // =========================================================================================================

    @Test
    public void test500ReplaceResource() throws Exception {
        OperationResult result = getTestOperationResult();

        // Delete should work fine even though OpenDJ is down
        provisioningService.deleteObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, null, taskManager.createTaskInstance(), result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        resource = addResourceFromFile(RESOURCE_OPENDJ_INITIALIZED_FILE, IntegrationTestTools.CONNECTOR_LDAP_TYPE, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

    }

    /**
     * This goes to local repo, therefore the expected result is ObjectNotFound.
     * We know that the shadow does not exist.
     */
    @Test
    public void test510GetObjectNoShadow() throws Exception {
        OperationResult result = getTestOperationResult();

        try {
            provisioningService.getObject(ShadowType.class, NON_EXISTENT_OID, null, taskManager.createTaskInstance(), result).asObjectable();

            AssertJUnit.fail("getObject succeeded unexpectedly");
        } catch (ObjectNotFoundException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    /**
     * This is using the shadow to go to the resource. But it cannot as OpenDJ is down.
     * Therefore the expected result is CommunicationException. It must not be ObjectNotFound as
     * we do NOT know that the shadow does not exist.
     * Provisioning should return a repo shadow and indicate the result both in operation result and
     * in fetchResult in the returned shadow.
     */
    @Test
    public void test511GetObjectShadow() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<ShadowType> acct =
                provisioningService.getObject(ShadowType.class, ACCOUNT_JBOND_OID, null, task, result);

        display("Account", acct);

        result.computeStatus();
        display("getObject result", result);
        assertEquals("Expected result partial error but was " + result.getStatus(),
                OperationResultStatus.PARTIAL_ERROR, result.getStatus());

        OperationResultType fetchResult = acct.asObjectable().getFetchResult();
        display("getObject fetchResult", fetchResult);
        assertEquals("Expected fetchResult partial error but was " + result.getStatus(),
                OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
    }

    @Test
    public void test521SearchAccounts() throws SchemaException, ObjectNotFoundException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        final String resourceNamespace = MidPointConstants.NS_RI;
        QName objectClass = new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME);

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), objectClass, prismContext);

        try {

            // WHEN
            provisioningService.searchObjects(ShadowType.class, query, null, task, result);

            AssertJUnit.fail("searchObjectsIterative succeeded unexpectedly");
        } catch (CommunicationException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    @Test
    public void test522SearchAccountsIterative() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        final String resourceNamespace = MidPointConstants.NS_RI;
        QName objectClass = new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME);

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), objectClass, prismContext);

        ResultHandler<ShadowType> handler = (shadow, lResult) -> {
            AssertJUnit.fail("handler called unexpectedly");
            return false;
        };

        try {

            // WHEN
            provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, task, result);

            AssertJUnit.fail("searchObjectsIterative succeeded unexpectedly");
        } catch (CommunicationException e) {
            displayExpectedException(e);
        }

        assertFailure(result);
    }

    @Test
    public void test530AddAccountWill() throws Exception {
        // GIVEN

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> object = parseObject(ACCOUNT_WILL_FILE);
        display("Account to add", object);

        // WHEN
        String addedObjectOid = provisioningService.addObject(object, null, null, task, result);

        // THEN
        result.computeStatus();
        display("addObject result", result);
        assertInProgress(result);

        assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
        ShadowAsserter.forShadow(repoShadow, "repo")
                .display()
                .assertName(ACCOUNT_WILL_DN)
                .pendingOperations()
                .singleOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .delta()
                .assertAdd();

        try {
            provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

            assertNotReached();
        } catch (GenericConnectorException e) {
            displayExpectedException(e);
        }

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
        PrismObject<ShadowType> provisioningAccountFuture = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, task, result);
        ShadowAsserter.forShadow(provisioningAccountFuture, "future")
                .display()
                .assertName(ACCOUNT_WILL_DN)
                .assertIsExists()
                .assertNotDead();
    }

    /**
     * Let us repeat the `ADD` operation.
     *
     * Currently, this fails - it creates a duplicate shadow. We perhaps should treat CommunicationException for ADD operation
     * in the same way as we do for MaintenanceException (i.e. trying to find an existing shadow). OR we should try a better
     * solution, common to all ADD operations.
     */
    @Test(enabled = false)
    public void test535AddAccountWillAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> object = parseObject(ACCOUNT_WILL_FILE);
        object.setOid(null); // To avoid failing on OID conflict
        display("Account to add", object);

        when("addObject operation is called");
        String addedObjectOid = provisioningService.addObject(object, null, null, task, result);

        then("status should be (still) IN_PROGRESS");
        result.computeStatus();
        display("addObject result", result);
        assertInProgress(result);

        and("OID should be the same as before");
        assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

        and("the shadow should have a single pending operation");
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
        ShadowAsserter.forShadow(repoShadow, "repo")
                .display()
                .assertName(ACCOUNT_WILL_DN)
                .pendingOperations()
                .singleOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .delta()
                .assertAdd();

        try {
            provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

            assertNotReached();
        } catch (GenericConnectorException e) {
            displayExpectedException(e);
        }

        Collection<SelectorOptions<GetOperationOptions>> options =
                GetOperationOptionsBuilder.create().futurePointInTime().build();
        PrismObject<ShadowType> provisioningAccountFuture =
                provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, task, result);
        ShadowAsserter.forShadow(provisioningAccountFuture, "future")
                .display()
                .assertName(ACCOUNT_WILL_DN)
                .assertIsExists()
                .assertNotDead();
    }

    @Test
    public void test540DeleteObject() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        provisioningService.deleteObject(ShadowType.class, ACCOUNT_SPARROW_OID, null, null, task, result);

        // THEN
        result.computeStatus();
        display("deleteObject result", result);
        assertInProgress(result);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, ACCOUNT_SPARROW_OID, null, result);
        ShadowAsserter.forShadow(repoShadow, "repo")
                .display()
                .pendingOperations()
                .singleOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .delta()
                .assertDelete();

        PrismObject<ShadowType> provisioningAccount = provisioningService.getObject(ShadowType.class, ACCOUNT_SPARROW_OID, null, task, result);
        ShadowAsserter.forShadow(provisioningAccount, "provisioning")
                .display()
                .pendingOperations()
                .singleOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .delta()
                .assertDelete();

    }

    @Test
    public void test550ModifyObject() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(ACCOUNT_JACK_CHANGE_FILE, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, ShadowType.class, PrismTestUtil.getPrismContext());
        displayDumpable("Object change", delta);

        // WHEN
        provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
                delta.getModifications(), null, null, task, result);

        // THEN
        result.computeStatus();
        display("deleteObject result", result);
        assertInProgress(result);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, ACCOUNT_JACK_OID, null, result);
        ShadowAsserter.forShadow(repoShadow, "repo")
                .display()
                .pendingOperations()
                .singleOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .delta()
                .assertModify();

        PrismObject<ShadowType> provisioningAccount = provisioningService.getObject(ShadowType.class, ACCOUNT_JACK_OID, null, task, result);
        ShadowAsserter.forShadow(provisioningAccount, "provisioning")
                .display()
                .pendingOperations()
                .singleOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .delta()
                .assertModify();
    }

    @Test
    public void test590Synchronize() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ResourceOperationCoordinates coords =
                ResourceOperationCoordinates.ofObjectClass(RESOURCE_OPENDJ_OID, OBJECT_CLASS_INETORGPERSON_QNAME);

        try {

            mockLiveSyncTaskHandler.synchronize(coords, new DummyTokenStorageImpl(), task, result);

            AssertJUnit.fail("addObject succeeded unexpectedly");
        } catch (CommunicationException e) {
            // The schema is there. So we expect the exception when contacting the resource.
            displayExpectedException(e);
        }

        assertFailure(result);
    }

}
