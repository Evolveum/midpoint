/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.model.api.util.ResourceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.NativeObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.NativeResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Tests that the "refresh schema" operation on a resource (deleteSchema + testResource, the same two steps
 * the GUI "Refresh schema" button executes, see {@code ProvisioningObjectsUtil.refreshResourceSchema})
 * actually stores the fresh connector schema on the FIRST call.
 *
 * The scenario: the resource is tested (schema stored, connector instance cached), then the connector's
 * ("remote") schema changes (a new auxiliary object class appears in the dummy connector instance),
 * and the schema is refreshed. Before the fix for the stale-cached-connector-schema issue, the first
 * refresh call re-stored the stale in-memory schema of the cached connector instance and the fresh
 * schema only appeared after the second call.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestResourceSchemaRefresh extends AbstractConfiguredModelIntegrationTest {

    protected static final String RESOURCE_OID = "a5c3b7d9-1f2e-4a3b-8c4d-9e0f1a2b3c4d";

    /** Dedicated dummy connector instance, so the test does not interfere with other tests. */
    protected static final String RESOURCE_INSTANCE_ID = "schemaRefreshTest";

    private static final String NEW_OBJECT_CLASS_NAME = "ExtraObjectClass";
    private static final String ACCOUNT_OBJECT_CLASS_NAME = "AccountObjectClass";

    private DummyResourceContoller dummyResourceCtl;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummyResourceCtl = DummyResourceContoller.create(RESOURCE_INSTANCE_ID);
        dummyResourceCtl.extendSchemaPirate();

        // Add resource directly to repo to avoid any initialization
        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_DUMMY_FILE);
        resource.asObjectable().setOid(RESOURCE_OID);
        resource.asObjectable().getConnectorRef().setOid(
                findConnectorByTypeAndVersion(CONNECTOR_DUMMY_TYPE, CONNECTOR_DUMMY_VERSION, initResult).getOid());
        PrismProperty<Object> instanceId = resource.findProperty(
                ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION,
                        SchemaConstants.ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME, "instanceId"));
        instanceId.setRealValue(RESOURCE_INSTANCE_ID);
        repositoryService.addObject(resource, null, initResult);
        display("Resource", resource);
    }

    @Test
    public void test100RefreshSchemaReflectsConnectorSchemaChange() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN: the resource has been tested; its schema is stored and the connector instance is cached
        TestUtil.assertSuccess("initial test resource", modelService.testResource(RESOURCE_OID, task, result));
        NativeResourceSchema baselineSchema = getStoredNativeSchema(task, result);
        display("Stored schema object classes (baseline)", objectClassNames(baselineSchema));
        assertThat(baselineSchema)
                .as("schema stored after the initial test")
                .isNotNull();
        assertThat(hasObjectClass(baselineSchema, ACCOUNT_OBJECT_CLASS_NAME))
                .as("baseline schema contains the account object class")
                .isTrue();
        assertThat(hasObjectClass(baselineSchema, NEW_OBJECT_CLASS_NAME))
                .as("baseline schema does not contain the new object class")
                .isFalse();

        // WHEN: the connector (remote) schema changes - a new auxiliary object class appears
        DummyObjectClass extraClass = new DummyObjectClass();
        extraClass.addAttributeDefinition("name", String.class, false, false);
        dummyResourceCtl.getDummyResource().addAuxiliaryObjectClass(NEW_OBJECT_CLASS_NAME, extraClass);

        // THEN: the first "refresh schema" stores the fresh schema
        refreshSchema(task, result);
        NativeResourceSchema afterFirstRefresh = getStoredNativeSchema(task, result);
        display("Stored schema object classes (after 1st refresh)", objectClassNames(afterFirstRefresh));
        boolean firstRefreshFresh = hasObjectClass(afterFirstRefresh, NEW_OBJECT_CLASS_NAME);

        // For comparison: the second "refresh schema" (refreshing twice was the only way to get the
        // fresh schema before the fix)
        refreshSchema(task, result);
        NativeResourceSchema afterSecondRefresh = getStoredNativeSchema(task, result);
        display("Stored schema object classes (after 2nd refresh)", objectClassNames(afterSecondRefresh));
        boolean secondRefreshFresh = hasObjectClass(afterSecondRefresh, NEW_OBJECT_CLASS_NAME);

        assertThat(firstRefreshFresh)
                .as("connector schema was not refreshed on the FIRST call: the stale schema of the cached "
                        + "connector instance was re-stored instead of fetching the fresh one from the connector. "
                        + "Fresh after 1st refresh: %s, fresh after 2nd refresh: %s", firstRefreshFresh, secondRefreshFresh)
                .isTrue();
        assertThat(secondRefreshFresh)
                .as("connector schema was not refreshed even on the second call")
                .isTrue();
    }

    /** The same two steps the GUI "Refresh schema" button executes. */
    private void refreshSchema(Task task, OperationResult result) throws Exception {
        ResourceUtils.deleteSchema(RESOURCE_OID, modelService, task, result);
        TestUtil.assertSuccess("test resource (schema refresh)", modelService.testResource(RESOURCE_OID, task, result));
    }

    private NativeResourceSchema getStoredNativeSchema(Task task, OperationResult result) throws Exception {
        PrismObject<ResourceType> storedResource =
                plainRepositoryService.getObject(ResourceType.class, RESOURCE_OID, null, result);
        return ResourceSchemaFactory.getNativeSchema(storedResource.asObjectable());
    }

    private static boolean hasObjectClass(NativeResourceSchema schema, String objectClassName) {
        if (schema == null) {
            return false;
        }
        return schema.getObjectClassDefinitions().stream().anyMatch(def ->
                objectClassName.equals(def.getNativeObjectClassName())
                        || objectClassName.equals(def.getName()));
    }

    private static List<String> objectClassNames(NativeResourceSchema schema) {
        if (schema == null) {
            return List.of();
        }
        return schema.getObjectClassDefinitions().stream()
                .map(NativeObjectClassDefinition::getName)
                .sorted()
                .collect(Collectors.toList());
    }
}
