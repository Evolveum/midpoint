/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.csv;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityHostType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;

/**
 * The test of Provisioning service on the API level. The test is using CSV resource.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public abstract class AbstractCsvTest extends AbstractProvisioningIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/csv/");

    protected static final String RESOURCE_NS = MidPointConstants.NS_RI;

    protected static final String CSV_CONNECTOR_TYPE = "com.evolveum.polygon.connector.csv.CsvConnector";

    private static final File CSV_TARGET_FILE = new File("target/midpoint.csv");

    protected static final String ACCOUNT_JACK_FIRSTNAME = "Jack";
    protected static final String ACCOUNT_JACK_LASTNAME = "Sparrow";

    protected static final String ATTR_FIRSTNAME = "firstname";
    protected static final QName ATTR_FIRSTNAME_QNAME = new QName(RESOURCE_NS, ATTR_FIRSTNAME);

    protected static final String ATTR_LASTNAME = "lastname";
    protected static final QName ATTR_LASTNAME_QNAME = new QName(RESOURCE_NS, ATTR_LASTNAME);

    protected PrismObject<ResourceType> resource;
    protected ResourceType resourceType;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        resource = addResourceFromFile(getResourceFile(), CSV_CONNECTOR_TYPE, initResult);
        resourceType = resource.asObjectable();

        FileUtils.copyFile(getSourceCsvFile(), CSV_TARGET_FILE);
    }

    protected abstract File getResourceFile();

    protected abstract String getResourceOid();

    protected abstract File getSourceCsvFile();

    protected abstract File getAccountJackFile();

    protected abstract String getAccountJackOid();

    @Test
    public void test000Integrity() throws Exception {
        assertNotNull("Resource is null", resource);
        assertNotNull("ResourceType is null", resourceType);

        OperationResult result = createOperationResult();

        ResourceType resource = repositoryService.getObject(ResourceType.class, getResourceOid(),
                null, result).asObjectable();
        String connectorOid = resource.getConnectorRef().getOid();
        ConnectorType connector = repositoryService
                .getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
        assertNotNull(connector);
        display("CSVFile Connector", connector);

        // Check connector schema
        IntegrationTestTools.assertConnectorSchemaSanity(connector);
    }

    /**
     * This should be the very first test that works with the resource.
     * <p>
     * The original repository object does not have resource schema. The schema
     * should be generated from the resource on the first use. This is the test
     * that executes testResource and checks whether the schema was generated.
     */
    @Test
    public void test003Connection() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        // Check that there is no schema before test (pre-condition)
        ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, getResourceOid(),
                null, result).asObjectable();
        assertNotNull("No connector ref", resourceBefore.getConnectorRef());
        assertNotNull("No connector ref OID", resourceBefore.getConnectorRef().getOid());
        ConnectorType connector = repositoryService.getObject(ConnectorType.class, resourceBefore
                .getConnectorRef().getOid(), null, result).asObjectable();
        assertNotNull(connector);
        XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
        Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchemaElement(resourceBefore);
        AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

        // WHEN
        OperationResult testResult = provisioningService.testResource(getResourceOid(), task, result);

        // THEN
        display("Test result", testResult);
        TestUtil.assertSuccess("Test resource failed (result)", testResult);

        PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, getResourceOid(), null, result);
        ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
        display("Resource after test", resourceTypeRepoAfter);

        XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
        assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
        Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchemaElement(resourceTypeRepoAfter);
        assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);

        String resourceXml = prismContext.xmlSerializer().serialize(resourceRepoAfter);
        displayValue("Resource XML", resourceXml);

        CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
        assertNotNull("No caching metadata", cachingMetadata);
        assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
        assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

        Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
        ResourceSchema parsedSchema = ResourceSchemaFactory.parseNativeSchemaAsBare(xsdElement);
        assertNotNull("No schema after parsing", parsedSchema);

        // schema will be checked in next test
    }

    @Test
    public void test004Configuration() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        resource = provisioningService.getObject(ResourceType.class, getResourceOid(), null, task, result);
        resourceType = resource.asObjectable();

        PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertNotNull("No configuration container", configurationContainer);
        PrismContainerDefinition confContDef = configurationContainer.getDefinition();
        assertNotNull("No configuration container definition", confContDef);
        PrismContainer configurationPropertiesContainer =
                configurationContainer.findContainer(SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME);
        assertNotNull("No configuration properties container", configurationPropertiesContainer);
        PrismContainerDefinition confPropDef = configurationPropertiesContainer.getDefinition();
        assertNotNull("No configuration properties container definition", confPropDef);

    }

    @Test
    public void test005ParsedSchema() throws Exception {
        // THEN
        // The returned type should have the schema pre-parsed
        assertTrue(ResourceSchemaFactory.hasParsedSchema(resourceType));

        var completeSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resourceType);

        displayDumpable("Parsed resource schema", completeSchema);
        assertNotNull("No resource schema", completeSchema);

        ResourceObjectClassDefinition accountDef = completeSchema.findObjectClassDefinition(RI_ACCOUNT_OBJECT_CLASS);
        assertNotNull("Account definition is missing", accountDef);
        assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
        assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
        assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
        assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClassName()));

        assertAccountDefinition(accountDef);

        ShadowSimpleAttributeDefinition<?> icfsNameDef = accountDef.findSimpleAttributeDefinition(SchemaConstants.ICFS_NAME);
        assertNull("ICFS NAME definition sneaked in", icfsNameDef);

        ShadowSimpleAttributeDefinition<?> icfsUidDef = accountDef.findSimpleAttributeDefinition(SchemaConstants.ICFS_UID);
        assertNull("ICFS UID definition sneaked in", icfsUidDef);

        assertCompleteSchemaCached(completeSchema, ResourceSchemaFactory.getCompleteSchema(resourceType));
    }

    protected abstract void assertAccountDefinition(ResourceObjectClassDefinition accountDef);

    @Test
    public void test006Capabilities() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();

        // WHEN
        ResourceType resource = provisioningService.getObject(ResourceType.class, getResourceOid(), null, task, result)
                .asObjectable();

        // THEN
        display("Resource from provisioning", resource);
        displayValue("Resource from provisioning (XML)", PrismTestUtil.serializeToXml(resource));

        CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
        assertFalse("Empty capabilities returned", CapabilityUtil.isEmpty(nativeCapabilities));

        // Connector cannot do activation, this should be null
        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilities, ActivationCapabilityType.class);
        assertNull("Found activation capability while not expecting it", capAct);

        ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilities, ScriptCapabilityType.class);
        assertNotNull("No script capability", capScript);
        List<ScriptCapabilityHostType> scriptHosts = capScript.getHost();
        assertEquals("Wrong number of script hosts", 2, scriptHosts.size());
        assertScriptHost(capScript, ProvisioningScriptHostType.CONNECTOR);
        assertScriptHost(capScript, ProvisioningScriptHostType.RESOURCE);

        dumpResourceCapabilities(resource);
    }

    private void assertScriptHost(ScriptCapabilityType capScript, ProvisioningScriptHostType expectedHostType) {
        for (ScriptCapabilityHostType host : capScript.getHost()) {
            if (host.getType() == expectedHostType) {
                return;
            }
        }
        AssertJUnit.fail("No script capability with host type " + expectedHostType);
    }

    @Test
    public void test100AddAccountJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> shadowBefore = parseObject(getAccountJackFile());

        // WHEN
        when();
        provisioningService.addObject(shadowBefore, null, null, task, result);

        // THEN
        then();
        assertSuccess(result);
    }

    @Test
    public void test110GetAccountJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, getAccountJackOid(), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNotNull(shadow);

        display("Shadow after", shadow);

        ShadowType shadowType = shadow.asObjectable();
        PrismAsserts.assertEqualsPolyString("Wrong name", "jack", shadow.getName());
        assertNotNull(shadow.getOid());
        assertEquals(RI_ACCOUNT_OBJECT_CLASS, shadowType.getObjectClass());
        assertEquals(getResourceOid(), shadowType.getResourceRef().getOid());
        assertAccountJackAttributes(shadowType);
        assertNotNull("Missing activation", shadowType.getActivation());
        assertNotNull("Missing activation status", shadowType.getActivation().getAdministrativeStatus());
        assertEquals("Not enabled", ActivationStatusType.ENABLED, shadowType.getActivation().getAdministrativeStatus());

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadow.getOid(), null, result);
        ShadowType repoShadowType = repoShadow.asObjectable();
        assertEquals(RI_ACCOUNT_OBJECT_CLASS, repoShadowType.getObjectClass());
        assertEquals(getResourceOid(), repoShadowType.getResourceRef().getOid());
        assertAccountJackAttributesRepo(shadowType);

    }

    @Test
    public void test120ModifyShadowPrimaryIdentifier() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> shadowBefore = parseObject(getAccountJackFile());
        String newValueOfUIDAttr = "Jack2";
//        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
//                getAccountJackOid(), ItemPath.create(ShadowType.F_ATTRIBUTES, getQNameOfUID()), newValueOfUIDAttr);
        PropertyDelta<String> delta = prismContext.deltaFactory().property().createReplaceDelta(shadowBefore.getDefinition(),
                ShadowType.F_PRIMARY_IDENTIFIER_VALUE, newValueOfUIDAttr);
        displayDumpable("PropertyDelta", delta);

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, getAccountJackOid(), Collections.singletonList(delta),
                null, null, task, result);

        // THEN
        then();
        assertSuccess(result);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadowBefore.getOid(), null, result);
        ShadowType repoShadowType = repoShadow.asObjectable();
        assertEquals(newValueOfUIDAttr, repoShadowType.getPrimaryIdentifierValue());
    }

    protected abstract void assertAccountJackAttributes(ShadowType shadowType);

    protected abstract void assertAccountJackAttributesRepo(ShadowType shadowType);
}
