/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.LegacyValidator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.sql.testing.CarefulAnt;
import com.evolveum.midpoint.repo.sql.testing.ResourceCarefulAntUtil;
import com.evolveum.midpoint.repo.sql.testing.SqlRepoTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.FailableFunction;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Various tests with resource definitions. Getting resources, modifications, etc.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestResources extends AbstractConfiguredModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/contract");

    private static final int MAX_RANDOM_SEQUENCE_ITERATIONS = 15;

    private static final List<CarefulAnt<ResourceType>> ants = new ArrayList<>();
    private static CarefulAnt<ResourceType> descriptionAnt;
    private static String lastVersion;
    private static final Random rnd = new Random();

    protected DummyResource dummyResource;
    protected DummyResourceContoller dummyResourceCtl;
    protected PrismObject<ResourceType> resourceDummy;

    protected DummyResource dummyResourceRed;
    protected DummyResourceContoller dummyResourceCtlRed;
    protected PrismObject<ResourceType> resourceDummyRed;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // This is necessary to make this test pass. The reason is that we store the dummy resource
        // (containing protected string in cleartext) directly into the repository.
        InternalsConfig.readEncryptionChecks = false;

        dummyResourceCtl = DummyResourceContoller.create(null);
        dummyResourceCtl.extendSchemaPirate();
        dummyResource = dummyResourceCtl.getDummyResource();
        dummyResourceCtl.addAttrDef(dummyResource.getAccountObjectClass(),
                DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, String.class, false, false);

        // Add resource directly to repo to avoid any initialization
        resourceDummy = PrismTestUtil.parseObject(RESOURCE_DUMMY_FILE);
        PrismObject<ConnectorType> connectorDummy = findConnectorByTypeAndVersion(CONNECTOR_DUMMY_TYPE, CONNECTOR_DUMMY_VERSION, initResult);
        resourceDummy.asObjectable().getConnectorRef().setOid(connectorDummy.getOid());
        repositoryService.addObject(resourceDummy, null, initResult);

        dummyResourceCtl.setResource(resourceDummy);

        dummyResourceCtlRed = DummyResourceContoller.create(RESOURCE_DUMMY_RED_NAME, resourceDummyRed);
        dummyResourceCtlRed.extendSchemaPirate();
        dummyResourceRed = dummyResourceCtlRed.getDummyResource();

        // Add resource directly to repo to avoid any initialization
        resourceDummyRed = PrismTestUtil.parseObject(RESOURCE_DUMMY_RED_FILE);
        resourceDummyRed.asObjectable().getConnectorRef().setOid(connectorDummy.getOid());
        repositoryService.addObject(resourceDummyRed, null, initResult);

        dummyResourceCtlRed.setResource(resourceDummyRed);

        ResourceCarefulAntUtil.initAnts(ants, RESOURCE_DUMMY_FILE, prismContext);
        descriptionAnt = ants.get(0);
        InternalMonitor.reset();
        InternalMonitor.setTrace(InternalOperationClasses.SHADOW_FETCH_OPERATIONS, true);
        InternalMonitor.setTrace(InternalOperationClasses.RESOURCE_SCHEMA_OPERATIONS, true);
        InternalMonitor.setTrace(InternalOperationClasses.CONNECTOR_OPERATIONS, true);
        InternalsConfig.encryptionChecks = false;

        InternalMonitor.setTrace(InternalCounters.PRISM_OBJECT_CLONE_COUNT, true);
    }

    /**
     * MID-3424
     */
    @Test
    public void test050GetResourceRaw() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());

        when();
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options, task, result);

        then();
        assertSuccess(result);

        display("Resource", resource);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 0);

        assertResourceDummy(resource, false, false);

        assertNull("Schema sneaked in", ResourceTypeUtil.getResourceXsdSchemaElement(resource));

        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
    }

    /**
     * MID-3424
     */
    @Test
    public void test052GetResourceNoFetch() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
                GetOperationOptions.createNoFetch());

        when();
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options,
                task, result);

        then();
        assertSuccess(result);

        display("Resource", resource);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 3);

        assertResourceDummy(resource, false);

        assertNull("Schema sneaked in", ResourceTypeUtil.getResourceXsdSchemaElement(resource));

        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1); // First "real" read
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 1);
    }

    /**
     * Make sure that resource caching works well even if noFetch is used.
     */
    @Test
    public void test053GetResourceNoFetchAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
                GetOperationOptions.createNoFetch());

        when();
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options,
                task, result);

        then();
        assertSuccess(result);

        display("Resource", resource);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 1);

        assertResourceDummy(resource, false);

        assertNull("Schema sneaked in", ResourceTypeUtil.getResourceXsdSchemaElement(resource));

        // Previous noFetch read did NOT place resource in the cache. Because the resource
        // may not be complete.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
    }

    /**
     * MID-3424
     */
    @Test
    public void test055GetResourceNoFetchReadOnly() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        GetOperationOptions option = GetOperationOptions.createNoFetch();
        option.setReadOnly(true);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(option);

        when();
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options,
                task, result);

        then();
        assertSuccess(result);

        display("Resource", resource);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 1);

        assertResourceDummy(resource, false);

        assertNull("Schema sneaked in", ResourceTypeUtil.getResourceXsdSchemaElement(resource));

        // Previous noFetch read did NOT place resource in the cache. Because the resource
        // may not be complete.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
    }

    /**
     * MID-3424
     */
    @Test
    public void test100SearchResourcesNoFetch() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());

        when();
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, options, task, result);

        then();
        assertNotNull("null search return", resources);
        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 2);

        for (PrismObject<ResourceType> resource : resources) {
            assertResource(resource, false);
        }

        // No explicit get. Search is doing all the work.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        assertSteadyResources();
    }

    /**
     * MID-3424
     */
    @Test
    public void test102SearchResourcesNoFetchReadOnly() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        GetOperationOptions option = GetOperationOptions.createNoFetch();
        option.setReadOnly(true);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(option);

        when();
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, options, task, result);

        then();
        assertNotNull("null search return", resources);
        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 2);

        for (PrismObject<ResourceType> resource : resources) {
            assertResource(resource, false);
        }

        // No explicit get. Search is doing all the work.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        assertSteadyResources();
    }

    /**
     * MID-3424
     */
    @Test
    public void test105SearchResourcesIterativeNoFetch() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        final List<PrismObject<ResourceType>> resources = new ArrayList<>();

        ResultHandler<ResourceType> handler = (resource, parentResult) -> {
            assertResource(resource, false);
            resources.add(resource);
            return true;
        };

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());

        when();
        modelService.searchObjectsIterative(ResourceType.class, null, handler, options, task, result);

        then();
        assertSuccess(result);

        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 2);          // temporary (MID-5465)

        // No explicit get. Search is doing all the work.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        assertSteadyResources();
    }

    /**
     * MID-3424
     */
    @Test
    public void test107SearchResourcesIterativeNoFetchReadOnly() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        final List<PrismObject<ResourceType>> resources = new ArrayList<>();

        ResultHandler<ResourceType> handler = (resource, parentResult) -> {
            assertResource(resource, false);
            resources.add(resource);
            return true;
        };

        GetOperationOptions option = GetOperationOptions.createNoFetch();
        option.setReadOnly(true);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(option);

        when();
        modelService.searchObjectsIterative(ResourceType.class, null, handler, options, task, result);

        then();
        assertSuccess(result);

        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 2);          // temporary (MID-5465)

        // No explicit get. Search is doing all the work.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        assertSteadyResources();
    }

    @Test
    public void test110GetResourceDummy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        when();
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);

        then();
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 4);

        assertResourceDummy(resource, true);

        // TODO not sure why are there 2 read counts. Should be 1. But this is not that important right now.
        // Some overhead on initial resource read is OK. What is important is that it does not increase during
        // normal account operations.
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 2);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT, 1); // cachingMetadata
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);

        assertEquals("Wrong dummy useless string", RESOURCE_DUMMY_USELESS_STRING, dummyResource.getUselessString());

        assertSteadyResources();
    }

    @Test
    public void test112GetResourceDummyReadOnly() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        when();
        PrismObject<ResourceType> resource = modelService.getObject(
                ResourceType.class, RESOURCE_DUMMY_OID, readOnly(), task, result);

        then();
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 0);

        assertResourceDummy(resource, true);

        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);

        assertSteadyResources();
    }

    @Test
    public void test120SearchResources() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();

        when();
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, null, task, result);

        then();
        assertNotNull("null search return", resources);
        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertSuccess(result);

        for (PrismObject<ResourceType> resource : resources) {
            assertResource(resource, true);
        }

        // Obviously, there is some uninitialized resource in the system
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT, 1); // cachingMetadata
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        assertSteadyResources();
    }

    @Test
    public void test125SearchResourcesIterative() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertSteadyResources();

        final List<PrismObject<ResourceType>> resources = new ArrayList<>();

        ResultHandler<ResourceType> handler = (resource, parentResult) -> {
            assertResource(resource, true);
            resources.add(resource);
            return true;
        };

        when();
        modelService.searchObjectsIterative(ResourceType.class, null, handler, null, task, result);

        then();
        assertSuccess(result);

        assertFalse("Empty search return", resources.isEmpty());
        assertEquals("Unexpected number of resources found", 2, resources.size());

        assertSteadyResources();
    }

    private void assertResourceDummy(PrismObject<ResourceType> resource, boolean expectSchema) {
        assertResourceDummy(resource, expectSchema, true);
    }

    private void assertResourceDummy(PrismObject<ResourceType> resource, boolean expectSchema, boolean prohibitRaw) {
        assertResource(resource, expectSchema, prohibitRaw);

        if (!prohibitRaw) {
            return;
        }

        PrismContainer<ConnectorConfigurationType> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinition = configurationContainer.getDefinition();
        assertDummyConfigurationContainerDefinition(configurationContainerDefinition, "from container");

        PrismContainer<Containerable> configurationPropertiesContainer = configurationContainer.findContainer(SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME);
        assertNotNull("No container " + SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME, configurationPropertiesContainer);

        assertConfigurationPropertyDefinition(configurationPropertiesContainer,
                "uselessString", DOMUtil.XSD_STRING, 0, 1, "UI_INSTANCE_USELESS_STRING", "UI_INSTANCE_USELESS_STRING_HELP");

        PrismContainerDefinition<Containerable> configurationPropertiesContainerDefinition = configurationPropertiesContainer.getDefinition();
        assertThat(configurationPropertiesContainerDefinition)
                .withFailMessage("No container definition in " + configurationPropertiesContainer)
                .isNotNull();

        assertConfigurationPropertyDefinition(configurationPropertiesContainerDefinition,
                "uselessString", DOMUtil.XSD_STRING, 0, 1, "UI_INSTANCE_USELESS_STRING", "UI_INSTANCE_USELESS_STRING_HELP");

        PrismObjectDefinition<ResourceType> objectDefinition = resource.getDefinition();
        assertNotNull("No object definition in resource", objectDefinition);
        PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinitionFromObjectDefinition = objectDefinition.findContainerDefinition(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertDummyConfigurationContainerDefinition(configurationContainerDefinitionFromObjectDefinition, "from object definition");
    }

    private void assertDummyConfigurationContainerDefinition(
            PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinition,
            String desc) {
        displayDumpable("Dummy configuration container definition " + desc, configurationContainerDefinition);
        PrismContainerDefinition<Containerable> configurationPropertiesContainerDefinition = configurationContainerDefinition.findContainerDefinition(SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME);
        assertNotNull("No container definition for " + SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME + " " + desc, configurationPropertiesContainerDefinition);

        assertConfigurationPropertyDefinition(configurationPropertiesContainerDefinition,
                "uselessString", DOMUtil.XSD_STRING, 0, 1, "UI_INSTANCE_USELESS_STRING", "UI_INSTANCE_USELESS_STRING_HELP");

    }

    private void assertConfigurationPropertyDefinition(PrismContainerDefinition<Containerable> containerDefinition,
            String propertyLocalName, QName expectedType, int expectedMinOccurs, int expectedMaxOccurs, String expectedDisplayName, String expectedHelp) {
        ItemName propName = new ItemName(containerDefinition.getTypeName().getNamespaceURI(), propertyLocalName);
        PrismPropertyDefinition<?> propDef = containerDefinition.findPropertyDefinition(propName);
        assertConfigurationPropertyDefinition(propDef, expectedType, expectedMinOccurs, expectedMaxOccurs, expectedDisplayName, expectedHelp);
    }

    private void assertConfigurationPropertyDefinition(PrismContainer<?> container,
            String propertyLocalName, QName expectedType, int expectedMinOccurs, int expectedMaxOccurs, String expectedDisplayName, String expectedHelp) {
        QName propName = new QName(container.getDefinition().getTypeName().getNamespaceURI(), propertyLocalName);
        PrismProperty<?> prop = container.findProperty(ItemName.fromQName(propName));
        assertNotNull("No property " + propName, prop);
        PrismPropertyDefinition<?> propDef = prop.getDefinition();
        assertNotNull("No definition for property " + prop, propDef);
        assertConfigurationPropertyDefinition(propDef, expectedType, expectedMinOccurs, expectedMaxOccurs, expectedDisplayName, expectedHelp);
    }

    private void assertConfigurationPropertyDefinition(PrismPropertyDefinition<?> propDef, QName expectedType,
            int expectedMinOccurs, int expectedMaxOccurs, String expectedDisplayName, String expectedHelp) {
        PrismAsserts.assertDefinition(propDef, propDef.getItemName(), expectedType, expectedMinOccurs, expectedMaxOccurs);
        assertEquals("Wrong displayName in " + propDef.getItemName() + " definition", expectedDisplayName, propDef.getDisplayName());
        assertEquals("Wrong help in " + propDef.getItemName() + " definition", expectedHelp, propDef.getHelp());
    }

    private void assertResource(PrismObject<ResourceType> resource, boolean expectSchema) {
        assertResource(resource, expectSchema, true);
    }

    private void assertResource(PrismObject<ResourceType> resource, boolean expectSchema, boolean prohibitRaw) {
        display("Resource", resource);
        displayDumpable("Resource def", resource.getDefinition());
        if (prohibitRaw) {
            PrismContainer<ConnectorConfigurationType> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
            assertNotNull("No Resource connector configuration def", configurationContainer);
            PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinition = configurationContainer.getDefinition();
            displayDumpable("Resource connector configuration def", configurationContainerDefinition);
            displayDumpable("Resource connector configuration def complex type def", configurationContainerDefinition.getComplexTypeDefinition());
            assertEquals("Wrong compile-time class in Resource connector configuration in " + resource, ConnectorConfigurationType.class,
                    configurationContainer.getCompileTimeClass());
            assertEquals("configurationContainer maxOccurs", 1, configurationContainerDefinition.getMaxOccurs());
        }

        resource.checkConsistence(true, prohibitRaw);

        Element schema = ResourceTypeUtil.getResourceXsdSchemaElement(resource);
        if (expectSchema) {
            assertNotNull("no schema in " + resource, schema);
        } else {
            assertNull("Unexpected schema in " + resource + ": " + schema, schema);
        }
    }

    @Test
    public void test200GetResourceRawAfterSchema() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        IntegrationTestTools.assertNoRepoThreadLocalCache();

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());

        when();
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options, task, result);

        then();
        IntegrationTestTools.assertNoRepoThreadLocalCache();
        SqlRepoTestUtil.assertVersionProgress(null, resource.getVersion());
        lastVersion = resource.getVersion();
        displayValue("Initial version", lastVersion);

        assertSuccess(result);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
    }

    /**
     * Red resource has an expression for uselessString configuration property. Check that.
     */
    @Test
    public void test210GetResourceDummyRed() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        when();
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_RED_OID, null, task, result);

        then();
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 1);

        assertResourceDummy(resource, true);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);

        assertEquals("Wrong RED useless string", RESOURCE_DUMMY_RED_USELESS_STRING, dummyResourceRed.getUselessString());
    }

    @Test
    public void test750GetResourceRaw() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // precondition
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());

        when();
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, options, task, result);

        then();
        assertSuccess(result);

        display("Resource", resource);
        IntegrationTestTools.displayXml("Initialized dummy resource", resource);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 0);

        assertResourceDummy(resource, true, false);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
    }

    @Test
    public void test752GetResourceDummy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        when();
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);

        then();
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 1);

        assertResourceDummy(resource, true);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
    }

    @Test
    public void test760ModifyConfigurationString() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ResourceType> resourceDelta = createConfigurationPropertyDelta(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME,
                "whatever wherever");

        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(resourceDelta), null, task, result);

        then();
        assertSuccess(result);

        PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        PrismAsserts.assertPropertyValue(resourceAfter,
                getConfigurationPropertyPath(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME),
                "whatever wherever");

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 2);
    }

    @Test
    public void test761ModifyConfigurationStringRaw() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ResourceType> resourceDelta = createConfigurationPropertyDelta(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME,
                "whatever raw wherever");

        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(resourceDelta), executeOptions().raw(),
                task, result);

        then();
        assertSuccess(result);

        PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        PrismAsserts.assertPropertyValue(resourceAfter,
                getConfigurationPropertyPath(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME),
                "whatever raw wherever");

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
    }

    @Test
    public void test765ModifyConfigurationDiffExpressionRawPrismContextParse() throws Exception {
        modifyConfigurationDiffExpressionRaw(xml -> prismContext.parseObject(xml));
    }

    /**
     * This is what GUI "Repository objects" page really does with XML.
     */
    @Test
    public void test767ModifyConfigurationDiffExpressionRawValidatorParse() throws Exception {
        modifyConfigurationDiffExpressionRaw(xml -> {
            final Holder<PrismObject<ResourceType>> objectHolder = new Holder<>();
            EventHandler<ResourceType> handler = new EventHandler<>() {

                @Override
                public EventResult preMarshall(Element objectElement, Node postValidationTree,
                        OperationResult objectResult) {
                    return EventResult.cont();
                }

                @Override
                public EventResult postMarshall(
                        ResourceType object, Element objectElement, OperationResult objectResult) {
                    objectHolder.setValue(object.asPrismObject());
                    return EventResult.cont();
                }

                @Override
                public void handleGlobalError(OperationResult currentResult) {
                }
            };
            LegacyValidator<ResourceType> validator = new LegacyValidator<>(prismContext, handler);
            validator.setVerbose(true);
            validator.setValidateSchema(false);
            OperationResult result = createOperationResult("validator");
            validator.validateObject(xml, result);
            return objectHolder.getValue();
        });
    }

    private void modifyConfigurationDiffExpressionRaw(
            FailableFunction<String, PrismObject<ResourceType>> parser) throws Exception {

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resourceBefore = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        // just to improve readability
        resourceBefore.removeProperty(ObjectType.F_FETCH_RESULT);
        String serializedResource = prismContext.xmlSerializer().serialize(resourceBefore);
        String modifiedResourceXml = serializedResource.replace("whatever raw wherever",
                "<expression><const xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"c:ConstExpressionEvaluatorType\">useless</const></expression>");
        displayValue("New resource XML", modifiedResourceXml);

        PrismObject<ResourceType> modifiedResource = parser.apply(modifiedResourceXml);
        display("New resource", modifiedResource);

        // just for fun
        String serializedModifiedResource = prismContext.xmlSerializer().serialize(modifiedResource);
        assertNotNull(serializedModifiedResource);

        ObjectDelta<ResourceType> diffDelta = resourceBefore.diff(modifiedResource, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
        displayDumpable("Diff delta", diffDelta);

        when();
        executeChanges(diffDelta, executeOptions().raw(), task, result);

        then();
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);

        // Evaluate expression, re-apply configuration
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_OID, task, result);
        TestUtil.assertSuccess("Dummy resource test", testResult);

        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 2);

        PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        display("Resource after", resourceAfter);

        assertEquals("Wrong default useless string", IntegrationTestTools.CONST_USELESS, dummyResource.getUselessString());

        // The resource is already cached (along with the parsed schema) as a result of "modify availability state" action
        // in testConnection operation.
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
    }

    private ObjectDelta<ResourceType> createConfigurationPropertyDelta(QName elementQName, String newValue) {
        ItemPath propPath = getConfigurationPropertyPath(elementQName);
        PrismPropertyDefinition<String> propDef = prismContext.definitionFactory().newPropertyDefinition(IntegrationTestTools.RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME,
                DOMUtil.XSD_STRING);
        PropertyDelta<String> propDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(propPath, propDef, newValue);
        ObjectDelta<ResourceType> resourceDelta = prismContext.deltaFactory().object()
                .createModifyDelta(RESOURCE_DUMMY_OID, propDelta, ResourceType.class);
        displayDumpable("Resource delta", resourceDelta);
        return resourceDelta;
    }

    private ItemPath getConfigurationPropertyPath(QName elementQName) {
        return ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION, SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME,
                elementQName);
    }

    @Test
    public void test800GetResourceDummy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        when();
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);

        then();
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 1);

        assertResourceDummy(resource, true);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);

        IntegrationTestTools.displayXml("Initialized dummy resource", resource);
    }

    @Test
    public void test820SingleDescriptionModify() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        singleModify(descriptionAnt, -1, task, result);
    }

    @Test
    public void test840RandomModifySequence() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        for (int i = 0; i <= MAX_RANDOM_SEQUENCE_ITERATIONS; i++) {
            singleRandomModify(i, task, result);
        }
    }

    private void singleRandomModify(int iteration, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        int i = rnd.nextInt(ants.size());
        CarefulAnt<ResourceType> ant = ants.get(i);
        singleModify(ant, iteration, task, result);
    }

    private void singleModify(CarefulAnt<ResourceType> ant, int iteration, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {

        given();
        ItemDelta<?, ?> itemDelta = ant.createDelta(iteration);
        ObjectDelta<ResourceType> objectDelta = prismContext.deltaFactory().object()
                .createModifyDelta(RESOURCE_DUMMY_OID, itemDelta, ResourceType.class);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);

        IntegrationTestTools.assertNoRepoThreadLocalCache();

        ModelExecuteOptions options = executeOptions().raw();

        when();
        modelService.executeChanges(deltas, options, task, result);

        then();
        IntegrationTestTools.assertNoRepoThreadLocalCache();
        Collection<SelectorOptions<GetOperationOptions>> getOptions = SelectorOptions.createCollection(GetOperationOptions.createRaw());
        PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, getOptions, task, result);
        SqlRepoTestUtil.assertVersionProgressOptional(lastVersion, resourceAfter.getVersion());
        lastVersion = resourceAfter.getVersion();
        displayValue("Version", lastVersion);

        Element xsdSchema = ResourceTypeUtil.getResourceXsdSchemaElement(resourceAfter);
        if (xsdSchema != null) {
            String targetNamespace = xsdSchema.getAttribute("targetNamespace");
            assertNotNull("No targetNamespace in schema after application of " + objectDelta, targetNamespace);
        }

        IntegrationTestTools.assertNoRepoThreadLocalCache();

        ant.assertModification(resourceAfter, iteration);
    }

    private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
    }
}
