/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.csv;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.test.asserter.predicates.StringAssertionPredicates.startsWith;
import static com.evolveum.midpoint.test.asserter.predicates.TimeAssertionPredicates.timeBetween;

import java.io.File;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Testing broken CSV e.g. with respect to availability status setting (MID-6019).
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestCsvBroken extends AbstractProvisioningIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/csv/");

    private static final TestObject<ResourceType> RESOURCE_CSV_BROKEN = TestObject.file(
            TEST_DIR, "resource-csv-broken.xml", "3e02a0b0-8c9a-4d45-b1b4-cf9b9b6e7918");

    private static final String CSV_CONNECTOR_TYPE = "com.evolveum.polygon.connector.csv.CsvConnector";

    private static final File CSV_SOURCE_FILE = new File(TEST_DIR, "midpoint-guid.csv");

    private static final File CSV_FILE_OK = new File("target/midpoint.csv");
    private static final File CSV_FILE_NON_EXISTING = new File("target/non-existing-file.csv");

    private OperationalStateType oldestStateRecord;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addResource(RESOURCE_CSV_BROKEN, CSV_CONNECTOR_TYPE, initResult);

        FileUtils.copyFile(CSV_SOURCE_FILE, CSV_FILE_OK);
    }

    /**
     * Testing the connection on broken resource. Availability status should be set to BROKEN.
     */
    @Test
    public void test100Connection() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        long before = System.currentTimeMillis();
        OperationResult testResult = provisioningService.testResource(RESOURCE_CSV_BROKEN.oid, task, result);
        long after = System.currentTimeMillis();

        // THEN
        display("Test result", testResult);
        TestUtil.assertFailure("Test resource succeeded unexpectedly", testResult);

        PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_CSV_BROKEN.oid,
                null, result);

        String localNodeId = taskManager.getNodeId();
        assertResource(resourceRepoAfter, "Resource after test")
                .display()
                .operationalState()
                    .assertAny()
                    .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.BROKEN)
                    .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                    .assertItemValueSatisfies(OperationalStateType.F_TIMESTAMP, timeBetween(before, after))
                    .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status set to BROKEN because test resource"))
                .end()
                .operationalStateHistory()
                    .assertSize(1)
                    .value(0)
                        .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.BROKEN)
                        .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                        .assertItemValueSatisfies(OperationalStateType.F_TIMESTAMP, timeBetween(before, after))
                        .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status set to BROKEN because test resource"));

        oldestStateRecord = resourceRepoAfter.asObjectable().getOperationalState();
        assertContainsOldestStateRecord(resourceRepoAfter);
    }

    /**
     * Fixing the resource and testing again. Availability status should be set to UP.
     */
    @Test
    public void test110FixResourceAndTestConnection() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setCsvFile(RESOURCE_CSV_BROKEN.oid, CSV_FILE_OK.getPath(), result);

        // WHEN
        long before = System.currentTimeMillis();
        OperationResult testResult = provisioningService.testResource(RESOURCE_CSV_BROKEN.oid, task, result);
        long after = System.currentTimeMillis();

        // THEN
        display("Test result", testResult);
        TestUtil.assertSuccess("Test resource failed", testResult);

        PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_CSV_BROKEN.oid,
                null, result);

        String localNodeId = taskManager.getNodeId();
        assertResource(resourceRepoAfter, "Resource after test")
                .display()
                .operationalState()
                .assertAny()
                    .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.UP)
                    .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                    .assertItemValueSatisfies(OperationalStateType.F_TIMESTAMP, timeBetween(before, after))
                    .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status changed from BROKEN to UP because resource test"))
                .end()
                .operationalStateHistory()
                .assertSize(2)
                    // TODO ordering of values is fragile
                    .value(0)
                        .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.BROKEN)
                        .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                        .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status set to BROKEN because test resource"))
                        .end()
                    .value(1)
                        .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.UP)
                        .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                        .assertItemValueSatisfies(OperationalStateType.F_TIMESTAMP, timeBetween(before, after))
                        .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status changed from BROKEN to UP because resource test"))
                        .end();

        assertContainsOldestStateRecord(resourceRepoAfter);
    }

    /**
     * Breaking the resource and testing again. Availability status should be set to DOWN (why not BROKEN?)
     */
    @Test
    public void test120BreakResourceAndTestConnection() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setCsvFile(RESOURCE_CSV_BROKEN.oid, CSV_FILE_NON_EXISTING.getPath(), result);

        // WHEN
        long before = System.currentTimeMillis();
        OperationResult testResult = provisioningService.testResource(RESOURCE_CSV_BROKEN.oid, task, result);
        long after = System.currentTimeMillis();

        // THEN
        display("Test result", testResult);
        TestUtil.assertFailure("Test resource succeeded unexpectedly", testResult);

        PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_CSV_BROKEN.oid,
                null, result);

        String localNodeId = taskManager.getNodeId();
        assertResource(resourceRepoAfter, "Resource after test")
                .display()
                .operationalState()
                .assertAny()
                    .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.DOWN)     // todo why DOWN and not BROKEN as before?
                    .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                    .assertItemValueSatisfies(OperationalStateType.F_TIMESTAMP, timeBetween(before, after))
                    .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status changed from UP to DOWN because testing connection using"))
                .end()
                .operationalStateHistory()
                .assertSize(3);

        assertContainsOldestStateRecord(resourceRepoAfter);
    }

    /**
     * Fixing the resource and executing searchObjects + getObject. Availability status should be set to UP.
     */
    @Test
    public void test130FixResourceAndSearchObjects() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setCsvFile(RESOURCE_CSV_BROKEN.oid, CSV_FILE_OK.getPath(), result);

        /*
         * We need to remove cached configured connector instances. Otherwise the changed configuration will not get
         * propagated to the cached connector instances.
         *
         * TODO Is this behavior OK? Shouldn't we fix it?
         */
        connectorManager.invalidate(null, null, null);

        // WHEN
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_CSV_BROKEN.oid)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .build();

        long before = System.currentTimeMillis();
        List<PrismObject<ShadowType>> resourceObjects = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
        display("Resource objects found", resourceObjects);
        assertEquals("Wrong # of CSV resource objects", 2, resourceObjects.size());

        long count = resourceObjects.stream()
                .map(o -> o.asObjectable())
                .filter(s -> s.getMetadata() == null || s.getMetadata().getCreateTimestamp() == null)
                .count();
        assertEquals("There are shadows without metadata", 0, count);

        /*
         * It looks like successful searchObjects operation does not trigger setting resource availability to UP.
         * But getObject does, so let's use it here.
         *
         * TODO Is this behavior OK? Shouldn't we fix this?
         */
        provisioningService.getObject(ShadowType.class, resourceObjects.get(0).getOid(), null, task, result);

        long after = System.currentTimeMillis();

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("searchObjects/getObject failed", result);

        PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_CSV_BROKEN.oid,
                null, result);

        String localNodeId = taskManager.getNodeId();
        assertResource(resourceRepoAfter, "Resource after test")
                .display()
                .operationalState()
                    .assertAny()
                    .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.UP)
                    .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                    .assertItemValueSatisfies(OperationalStateType.F_TIMESTAMP, timeBetween(before, after))
                    .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status changed from DOWN to UP"))
                    .end()
                .operationalStateHistory()
                    .assertSize(4);

        assertContainsOldestStateRecord(resourceRepoAfter);
    }

    /**
     * Introducing two more status changes and checking that history entry cleanup works well.
     */
    @Test
    public void test140TwoMoreStatusChanges() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        long before = System.currentTimeMillis();
        setCsvFile(RESOURCE_CSV_BROKEN.oid, CSV_FILE_NON_EXISTING.getPath(), result);
        provisioningService.testResource(RESOURCE_CSV_BROKEN.oid, task, result);
        setCsvFile(RESOURCE_CSV_BROKEN.oid, CSV_FILE_OK.getPath(), result);
        provisioningService.testResource(RESOURCE_CSV_BROKEN.oid, task, result);
        long after = System.currentTimeMillis();

        // THEN
        PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_CSV_BROKEN.oid,
                null, result);

        String localNodeId = taskManager.getNodeId();
        assertResource(resourceRepoAfter, "Resource after test")
                .display()
                .operationalState()
                    .assertAny()
                    .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.UP)
                    .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                    .assertItemValueSatisfies(OperationalStateType.F_TIMESTAMP, timeBetween(before, after))
                    .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status changed from DOWN to UP because resource test"))
                .end()
                .operationalStateHistory()
                    .assertSize(5);     // not 6, because 5 is the limit

        assertDoesNotContainOldestStateRecord(resourceRepoAfter);
    }

    private void setCsvFile(String resourceOid, String file, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        QName propertyName = new QName("filePath");
        ItemPath propertyPath = ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION, SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME, propertyName);
        PrismPropertyDefinition<String> propertyDefinition = prismContext.definitionFactory().createPropertyDefinition(propertyName, DOMUtil.XSD_STRING);
        List<ItemDelta<?, ?>> deltas = deltaFor(ResourceType.class)
                .item(propertyPath, propertyDefinition)
                .replace(file)
                .asItemDeltas();
        repositoryService.modifyObject(ResourceType.class, resourceOid, deltas, result);
    }

    private void assertContainsOldestStateRecord(PrismObject<ResourceType> resource) {
        assertContainsOldestStateRecord(resource, true);
    }

    private void assertDoesNotContainOldestStateRecord(PrismObject<ResourceType> resource) {
        assertContainsOldestStateRecord(resource, false);
    }

    private void assertContainsOldestStateRecord(PrismObject<ResourceType> resource, boolean expected) {
        boolean actual = resource.asObjectable().getOperationalStateHistory().contains(oldestStateRecord);
        assertEquals("Mismatch in oldest state record existence", expected, actual);
    }
}
