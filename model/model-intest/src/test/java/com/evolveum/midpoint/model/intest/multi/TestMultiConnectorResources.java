/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.multi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorInstanceSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;

/**
 * Test resources that have several connectors.
 *
 * MID-5921
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMultiConnectorResources extends AbstractConfiguredModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/multi-connector");

    // Black dummy resource for testing tolerant attributes
    private static final File RESOURCE_DUMMY_OPALINE_FILE = new File(TEST_DIR, "resource-dummy-opaline.xml");
    private static final String RESOURCE_DUMMY_OPALINE_OID = "d4a2a030-0a1c-11ea-b61c-67d35cfea30f";
    private static final String RESOURCE_DUMMY_OPALINE_NAME = "opaline";
    private static final String RESOURCE_DUMMY_OPALINE_SCRIPT_NAME = "opaline-script";
    private static final String CONF_USELESS_OPALINE = "USEless-opaline";
    private static final String CONF_USELESS_SCRIPT = "USEless-script";

    private static final String SCRIPT_RUNNER_NS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnectorScriptRunner";
    private static final ItemName SCRIPT_RUNNER_INSTANCE_ID = new ItemName(SCRIPT_RUNNER_NS, "instanceId");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_OPALINE_NAME, RESOURCE_DUMMY_OPALINE_FILE, RESOURCE_DUMMY_OPALINE_OID, initTask, initResult);
        DummyResourceContoller opalineScriptController = DummyResourceContoller.create(RESOURCE_DUMMY_OPALINE_SCRIPT_NAME, getDummyResourceObject(RESOURCE_DUMMY_OPALINE_NAME));
        dummyResourceCollection.initDummyResource(RESOURCE_DUMMY_OPALINE_SCRIPT_NAME, opalineScriptController);

        repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);
        repoAddObjectFromFile(SECURITY_POLICY_BENEVOLENT_FILE, initResult);
        repoAddObjectFromFile(PASSWORD_POLICY_BENEVOLENT_FILE, initResult);

        repoAddObjectFromFile(USER_JACK_FILE, true, initResult);
        repoAddObjectFromFile(USER_GUYBRUSH_FILE, true, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        // GIVEN
        Task task = getTestTask();

        // WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_OPALINE_OID, task);

        // THEN
        display("Test result", testResult);
        TestUtil.assertSuccess("Opaline dummy test result", testResult);

        // Makes sure that both resources are configured
        assertEquals("Wrong OPALINE useless string", CONF_USELESS_OPALINE, getDummyResource(RESOURCE_DUMMY_OPALINE_NAME).getUselessString());
        assertEquals("Wrong OPALINE-SCRIPT useless string", CONF_USELESS_SCRIPT, getDummyResource(RESOURCE_DUMMY_OPALINE_SCRIPT_NAME).getUselessString());
    }

    @Test
    public void test100JackAssignDummyOpaline() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OPALINE_OID, null, task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .singleLink()
                .target()
                .assertResource(RESOURCE_DUMMY_OPALINE_OID)
                .assertName(ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccountByUsername(RESOURCE_DUMMY_OPALINE_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);
    }

    /**
     * Tests `applyDefinition` method w.r.t. additional connectors.
     *
     * MID-7918
     */
    @Test
    public void test200CheckConfigurationPropertyDeltaDefinition() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a delta for configuration property of additional connector");
        PropertyDelta<Object> propertyDelta = createConfigurationPropertyDelta();
        ObjectDelta<ResourceType> objectDelta = createObjectDelta(propertyDelta);

        when("applying definition to the delta");
        provisioningService.applyDefinition(objectDelta, task, result);

        then("the definition is there");
        ItemDelta<?, ?> propertyDeltaAfter = objectDelta.getModifications().iterator().next();
        assertThat(propertyDeltaAfter.getDefinition()).as("definition").isNotNull();
    }

    private PropertyDelta<Object> createConfigurationPropertyDelta() {
        PrismContainer<Containerable> additionalConnectorsContainer =
                getDummyResourceObject(RESOURCE_DUMMY_OPALINE_NAME).findContainer(ResourceType.F_ADDITIONAL_CONNECTOR);
        // Using legacy API because we intentionally provide no definition here
        PropertyDelta<Object> propertyDelta = prismContext.deltaFactory().property().create(
                ItemPath.create(
                        ResourceType.F_ADDITIONAL_CONNECTOR,
                        additionalConnectorsContainer.getValue().getId(),
                        ConnectorInstanceSpecificationType.F_CONNECTOR_CONFIGURATION,
                        SchemaConstants.ICF_CONFIGURATION_PROPERTIES,
                        SCRIPT_RUNNER_INSTANCE_ID),
                null);
        propertyDelta.setValueToReplace(
                prismContext.itemFactory().createPropertyValue("opaline-script-2"));
        return propertyDelta;
    }

    private ObjectDelta<ResourceType> createObjectDelta(ItemDelta<?, ?> itemDelta) {
        return prismContext.deltaFactory().object()
                .createModifyDelta(RESOURCE_DUMMY_OPALINE_OID, itemDelta, ResourceType.class);
    }

    /**
     * The same as previous test, but this time really executing the delta.
     *
     * MID-7918
     */
    @Test
    public void test210ModifyConfigurationProperty() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a delta for configuration property of additional connector");
        PropertyDelta<Object> propertyDelta = createConfigurationPropertyDelta();
        ObjectDelta<ResourceType> objectDelta = createObjectDelta(propertyDelta);

        when("executing the delta");
        modelService.executeChanges(
                List.of(objectDelta), ModelExecuteOptions.create().raw(), task, result);

        then("the property is updated");
        assertPropertyAfter("opaline-script-2", task, result);
    }

    /**
     * This time we replace the whole `configurationProperties` container.
     *
     * MID-7918
     */
    @Test
    public void test220ReplaceConfigurationPropertiesContainer() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a delta for configuration properties container of additional connector");
        PrismContainer<Containerable> additionalConnectorsContainer =
                getDummyResourceObject(RESOURCE_DUMMY_OPALINE_NAME).findContainer(ResourceType.F_ADDITIONAL_CONNECTOR);
        // Using legacy API because we intentionally provide no definition here
        ContainerDelta<Containerable> containerDelta = prismContext.deltaFactory().container().create(
                ItemPath.create(
                        ResourceType.F_ADDITIONAL_CONNECTOR,
                        additionalConnectorsContainer.getValue().getId(),
                        ConnectorInstanceSpecificationType.F_CONNECTOR_CONFIGURATION,
                        SchemaConstants.ICF_CONFIGURATION_PROPERTIES),
                null);
        PrismContainerValue<Containerable> containerValue = prismContext.itemFactory().createContainerValue();
        PrismProperty<String> property = prismContext.itemFactory().createProperty(SCRIPT_RUNNER_INSTANCE_ID);
        property.setRealValue("opaline-script-220");
        containerValue.add(property);
        containerDelta.setValueToReplace(containerValue);

        ObjectDelta<ResourceType> objectDelta = createObjectDelta(containerDelta);

        when("applying definition to the delta");
        provisioningService.applyDefinition(objectDelta, task, result);

        then("the definition is there");
        ItemDelta<?, ?> containerDeltaAfter = objectDelta.getModifications().iterator().next();

        PrismContainerValue<?> configurationPropertiesPcv =
                (PrismContainerValue<?>) containerDeltaAfter.getValuesToReplace().iterator().next();
        PrismProperty<?> propertyAfter = configurationPropertiesPcv.findProperty(SCRIPT_RUNNER_INSTANCE_ID);
        assertThat(propertyAfter.getDefinition()).as("definition").isNotNull();

        when("executing the delta");
        modelService.executeChanges(
                List.of(objectDelta), ModelExecuteOptions.create().raw(), task, result);

        then("the property is updated");
        assertPropertyAfter("opaline-script-220", task, result);
    }

    /**
     * As the previous test, but this time we replace the whole `connectorConfiguration` container.
     *
     * MID-7918
     */
    @Test
    public void test230ReplaceConnectorConfigurationContainer() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a delta for connector configuration container of additional connector");
        PrismContainer<Containerable> additionalConnectorsContainer =
                getDummyResourceObject(RESOURCE_DUMMY_OPALINE_NAME).findContainer(ResourceType.F_ADDITIONAL_CONNECTOR);
        // Using legacy API because we intentionally provide no definition here
        ContainerDelta<Containerable> containerDelta = prismContext.deltaFactory().container().create(
                ItemPath.create(
                        ResourceType.F_ADDITIONAL_CONNECTOR,
                        additionalConnectorsContainer.getValue().getId(),
                        ConnectorInstanceSpecificationType.F_CONNECTOR_CONFIGURATION),
                null);

        PrismContainerValue<Containerable> configurationPropertiesContainerValue =
                prismContext.itemFactory().createContainerValue();
        PrismProperty<String> property = prismContext.itemFactory().createProperty(SCRIPT_RUNNER_INSTANCE_ID);
        property.setRealValue("opaline-script-230");
        configurationPropertiesContainerValue.add(property);

        //noinspection unchecked
        PrismContainer<Containerable> configurationPropertiesContainer =
                prismContext.itemFactory().createContainer(SchemaConstants.ICF_CONFIGURATION_PROPERTIES);
        configurationPropertiesContainer.add(configurationPropertiesContainerValue);

        PrismContainerValue<Containerable> connectorConfigurationContainerValue =
                prismContext.itemFactory().createContainerValue();
        connectorConfigurationContainerValue.add(configurationPropertiesContainer);
        containerDelta.setValueToReplace(connectorConfigurationContainerValue);

        ObjectDelta<ResourceType> objectDelta = createObjectDelta(containerDelta);

        when("applying definition to the delta");
        provisioningService.applyDefinition(objectDelta, task, result);

        then("the definition is there");
        ItemDelta<?, ?> containerDeltaAfter = objectDelta.getModifications().iterator().next();

        PrismContainerValue<?> connectorConfigurationPcv =
                (PrismContainerValue<?>) containerDeltaAfter.getValuesToReplace().iterator().next();
        PrismProperty<?> propertyAfter = connectorConfigurationPcv.findProperty(
                ItemPath.create(
                        SchemaConstants.ICF_CONFIGURATION_PROPERTIES,
                        SCRIPT_RUNNER_INSTANCE_ID));
        assertThat(propertyAfter.getDefinition()).as("definition").isNotNull();

        when("executing the delta");
        modelService.executeChanges(
                List.of(objectDelta), ModelExecuteOptions.create().raw(), task, result);

        then("the property is updated");
        assertPropertyAfter("opaline-script-230", task, result);
    }

    /**
     * Finally, here we create a delta to replace the whole `additionalConnector` container.
     *
     * MID-7918
     */
    @Test
    public void test240ReplaceAdditionalConnectorContainer() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a delta for additional connector (replace)");
        ObjectReferenceType connectorRef =
                getDummyResourceObject(RESOURCE_DUMMY_OPALINE_NAME).asObjectable()
                        .getAdditionalConnector().get(0).getConnectorRef();
        ConnectorInstanceSpecificationType additionalConnector = new ConnectorInstanceSpecificationType();
        additionalConnector.connectorRef(connectorRef.clone());
        additionalConnector.setName("scripting");
        ConnectorConfigurationType connectorConfiguration = new ConnectorConfigurationType();
        additionalConnector.setConnectorConfiguration(connectorConfiguration);

        PrismContainerValue<Containerable> configurationPropertiesContainerValue =
                prismContext.itemFactory().createContainerValue();
        PrismProperty<String> property = prismContext.itemFactory().createProperty(SCRIPT_RUNNER_INSTANCE_ID);
        property.setRealValue("opaline-script-240");
        configurationPropertiesContainerValue.add(property);

        //noinspection unchecked
        PrismContainer<Containerable> configurationPropertiesContainer =
                prismContext.itemFactory().createContainer(SchemaConstants.ICF_CONFIGURATION_PROPERTIES);
        configurationPropertiesContainer.add(configurationPropertiesContainerValue);

        //noinspection unchecked
        connectorConfiguration.asPrismContainerValue().add(configurationPropertiesContainer);

        // This time we provide (generic) definition for the delta. The "applyDefinition" code is not expected
        // to deal with this situation.
        ContainerDelta<ConnectorInstanceSpecificationType> containerDelta =
                prismContext.deltaFactory().container().create(
                        ResourceType.F_ADDITIONAL_CONNECTOR,
                        prismContext.getSchemaRegistry()
                                .findContainerDefinitionByCompileTimeClass(ConnectorInstanceSpecificationType.class));
        //noinspection unchecked
        containerDelta.setValueToReplace(additionalConnector.asPrismContainerValue());

        ObjectDelta<ResourceType> objectDelta = createObjectDelta(containerDelta);

        when("applying definition to the delta");
        provisioningService.applyDefinition(objectDelta, task, result);

        then("the definition is there");
        ItemDelta<?, ?> containerDeltaAfter = objectDelta.getModifications().iterator().next();

        PrismContainerValue<?> additionalContainerPcv =
                (PrismContainerValue<?>) containerDeltaAfter.getValuesToReplace().iterator().next();
        PrismProperty<?> propertyAfter = additionalContainerPcv.findProperty(
                ItemPath.create(
                        ConnectorInstanceSpecificationType.F_CONNECTOR_CONFIGURATION,
                        SchemaConstants.ICF_CONFIGURATION_PROPERTIES,
                        SCRIPT_RUNNER_INSTANCE_ID));

        assertThat(propertyAfter.getDefinition()).as("definition").isNotNull();

        when("executing the delta");
        modelService.executeChanges(
                List.of(objectDelta), ModelExecuteOptions.create().raw(), task, result);

        then("the property is updated");
        assertPropertyAfter("opaline-script-240", task, result);
    }

    private void assertPropertyAfter(String expected, Task task, OperationResult result) throws CommonException {
        PrismObject<ResourceType> resourceAfter =
                repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OPALINE_OID, null, result);
        provisioningService.applyDefinition(resourceAfter, task, result);
        Object valueAfter = resourceAfter.asObjectable().getAdditionalConnector().get(0)
                .getConnectorConfiguration().asPrismContainerValue()
                .findContainer(SchemaConstants.ICF_CONFIGURATION_PROPERTIES).getValue()
                .findProperty(SCRIPT_RUNNER_INSTANCE_ID).getRealValue();
        assertThat(valueAfter).as("property value after").isEqualTo(expected);
    }
}
