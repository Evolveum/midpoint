/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertNotEmpty;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.opends.server.types.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.*;

/**
 * Test UCF implementation with OpenDJ and ICF LDAP connector.
 *
 * This test is using embedded OpenDJ as a resource and ICF LDAP connector. The
 * test is executed by direct calls to the UCF interface.
 *
 * @author Radovan Semancik
 * @author Katka Valalikova
 *
 * This is an UCF test. It should not need repository or other things
 * from the midPoint spring context except from the provisioning beans.
 * But due to a general issue with spring context initialization this is
 * a lesser evil for now (MID-392)
 */
@ContextConfiguration(locations = { "classpath:ctx-ucf-connid-test.xml" })
public class TestUcfOpenDj extends AbstractUcfDummyTest {

    private static final File RESOURCE_OPENDJ_FILE = new File(UcfTestUtil.TEST_DIR, "resource-opendj.xml");
    private static final File RESOURCE_OPENDJ_BAD_FILE = new File(UcfTestUtil.TEST_DIR, "resource-opendj-bad.xml");
    private static final File CONNECTOR_LDAP_FILE = new File(UcfTestUtil.TEST_DIR, "connector-ldap.xml");

    private ResourceType resourceType;
    private ResourceType badResourceType;
    private ConnectorType connectorType;
    private ConnectorFactory factory;
    private ConnectorInstance cc;
    private PrismSchema connectorSchema;
    private ResourceSchema resourceSchema;

    @Autowired ConnectorFactory connectorFactoryIcfImpl;
    @Autowired Protector protector;
    @Autowired PrismContext prismContext;

    protected static OpenDJController openDJController = new OpenDJController();

    @Override
    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @BeforeClass
    public void startLdap() throws Exception {
        logger.info("------------------------------------------------------------------------------");
        logger.info("START:  OpenDjUcfTest");
        logger.info("------------------------------------------------------------------------------");
        openDJController.startCleanServer();
    }

    @AfterClass
    public void stopLdap() {
        openDJController.stop();
        logger.info("------------------------------------------------------------------------------");
        logger.info("STOP:  OpenDjUcfTest");
        logger.info("------------------------------------------------------------------------------");
    }

    @BeforeMethod
    public void initUcf() throws Exception {
        // Resource
        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_OPENDJ_FILE);
        resourceType = resource.asObjectable();

        // Resource: Second copy for negative test cases
        PrismObject<ResourceType> badResource = PrismTestUtil.parseObject(RESOURCE_OPENDJ_BAD_FILE);
        badResourceType = badResource.asObjectable();

        // Connector
        PrismObject<ConnectorType> connector = PrismTestUtil.parseObject(CONNECTOR_LDAP_FILE);
        connectorType = connector.asObjectable();

        factory = connectorFactoryIcfImpl;

        connectorSchema = factory.generateConnectorConfigurationSchema(connectorType);
        AssertJUnit.assertNotNull("Cannot generate connector schema", connectorSchema);
        displayDumpable("Connector schema", connectorSchema);

        cc = factory.createConnectorInstance(connectorType,
                "OpenDJ resource",
                "description of OpenDJ connector instance");

        OperationResult result = new OperationResult("initUcf");
        cc.configure(
                resourceType.getConnectorConfiguration().asPrismContainerValue(),
                ResourceTypeUtil.getSchemaGenerationConstraints(resourceType),
                result);
        cc.initialize(null, null, false, result);
        // TODO: assert something

        resourceSchema = cc.fetchResourceSchema(result);
        displayDumpable("Resource schema", resourceSchema);

        AssertJUnit.assertNotNull(resourceSchema);

    }

    @AfterMethod
    public void shutdownUcf() {
    }

    @Test
    public void test010ConnectorSchemaSanity() {
        IntegrationTestTools.assertConnectorSchemaSanity(connectorSchema, "LDAP connector", true);

        PrismContainerDefinition configurationDefinition =
                connectorSchema.findItemDefinitionByElementName(new QName(ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart()),
                        PrismContainerDefinition.class);
        PrismContainerDefinition configurationPropertiesDefinition =
                configurationDefinition.findContainerDefinition(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);

        PrismPropertyDefinition<String> propHost = configurationPropertiesDefinition.findPropertyDefinition(new ItemName(UcfTestUtil.CONNECTOR_LDAP_NS, "host"));
        assertNotNull("No definition for configuration property 'host' in connector schema", propHost);
        PrismAsserts.assertDefinition(propHost, new QName(UcfTestUtil.CONNECTOR_LDAP_NS, "host"), DOMUtil.XSD_STRING, 1, 1);
        assertEquals("Wrong property 'host' display name", "Host", propHost.getDisplayName());
        assertEquals("Wrong property 'host' help", "The name or IP address of the LDAP server host.", propHost.getHelp());
        assertEquals("Wrong property 'host' display order", (Integer) 1, propHost.getDisplayOrder()); // MID-2642

        PrismPropertyDefinition<String> propPort = configurationPropertiesDefinition.findPropertyDefinition(new ItemName(UcfTestUtil.CONNECTOR_LDAP_NS, "port"));
        assertNotNull("No definition for configuration property 'port' in connector schema", propPort);
        PrismAsserts.assertDefinition(propPort, new QName(UcfTestUtil.CONNECTOR_LDAP_NS, "port"), DOMUtil.XSD_INT, 0, 1);
        assertEquals("Wrong property 'port' display name", "Port number", propPort.getDisplayName());
        assertEquals("Wrong property 'port' help", "LDAP server port number.", propPort.getHelp());
        assertEquals("Wrong property 'port' display order", (Integer) 2, propPort.getDisplayOrder()); // MID-2642
    }

    @Test
    public void test020ResourceSchemaSanity() {
        QName objectClassQname = OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME;
        ResourceObjectClassDefinition accountDefinition = resourceSchema.findObjectClassDefinition(objectClassQname);
        assertNotNull("No object class definition " + objectClassQname, accountDefinition);
        assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isEmpty());
        assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isIgnored());

        Collection<? extends ResourceAttributeDefinition<?>> identifiers = accountDefinition.getPrimaryIdentifiers();
        assertNotNull("Null identifiers for " + objectClassQname, identifiers);
        assertFalse("Empty identifiers for " + objectClassQname, identifiers.isEmpty());

        ResourceAttributeDefinition<?> idPrimaryDef = accountDefinition.findAttributeDefinition(
                new QName(MidPointConstants.NS_RI, OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME));
        assertNotNull("No definition for attribute " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME, idPrimaryDef);
        assertTrue("Attribute " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " in not an identifier",
                accountDefinition.isPrimaryIdentifier(
                        idPrimaryDef.getItemName()));
        assertTrue("Attribute " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " in not in identifiers list", identifiers.contains(idPrimaryDef));
        assertEquals("Attribute " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " has wrong native name", OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME, idPrimaryDef.getNativeAttributeName());
        assertEquals("Attribute " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " has wrong framework name", Uid.NAME, idPrimaryDef.getFrameworkAttributeName());

        ResourceAttributeDefinition<?> idSecondaryDef =
                accountDefinition.findAttributeDefinition(OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
        assertNotNull("No definition for attribute " + SchemaConstants.ICFS_NAME, idSecondaryDef);
        assertTrue("Attribute " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " in not secondary identifier",
                accountDefinition.isSecondaryIdentifier(
                        idSecondaryDef.getItemName()));
        assertFalse("Attribute " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " in in identifiers list and it should NOT be", identifiers.contains(idSecondaryDef));
        assertTrue("Attribute " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " in not in secomdary identifiers list", accountDefinition.getSecondaryIdentifiers().contains(idSecondaryDef));
        assertEquals("Attribute " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " has wrong native name", OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME, idSecondaryDef.getNativeAttributeName());
        assertEquals("Attribute " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " has wrong framework name", Name.NAME, idSecondaryDef.getFrameworkAttributeName());

        assertEquals("Unexpected identifiers: " + identifiers, 1, identifiers.size());
        assertEquals("Unexpected secondary identifiers: " + accountDefinition.getSecondaryIdentifiers(), 1, accountDefinition.getSecondaryIdentifiers().size());
    }

    private Collection<ResourceAttribute<?>> addSampleResourceObject(String name, String givenName, String familyName)
            throws Exception {
        OperationResult result = new OperationResult(this.getClass().getName() + ".testAdd");

        QName objectClassQname = OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME;
        ResourceObjectClassDefinition accountDefinition = resourceSchema.findObjectClassDefinition(objectClassQname);
        assertNotNull("No object class definition " + objectClassQname, accountDefinition);
        ResourceAttributeContainer resourceObject = accountDefinition.instantiate(ShadowType.F_ATTRIBUTES);

        //noinspection unchecked
        ResourceAttributeDefinition<String> attributeDefinition =
                (ResourceAttributeDefinition<String>)
                        accountDefinition.findAttributeDefinitionRequired(OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
        ResourceAttribute<String> attribute = attributeDefinition.instantiate();
        attribute.setRealValue("uid=" + name + ",ou=people,dc=example,dc=com");
        resourceObject.add(attribute);

        //noinspection unchecked
        attributeDefinition = (ResourceAttributeDefinition<String>) accountDefinition.findAttributeDefinitionRequired(QNAME_SN);
        attribute = attributeDefinition.instantiate();
        attribute.setRealValue(familyName);
        resourceObject.add(attribute);

        //noinspection unchecked
        attributeDefinition = (ResourceAttributeDefinition<String>) accountDefinition.findAttributeDefinitionRequired(QNAME_CN);
        attribute = attributeDefinition.instantiate();
        attribute.setRealValue(givenName + " " + familyName);
        resourceObject.add(attribute);

        //noinspection unchecked
        attributeDefinition = (ResourceAttributeDefinition<String>)
                accountDefinition.findAttributeDefinitionRequired(QNAME_GIVEN_NAME);
        attribute = attributeDefinition.instantiate();
        attribute.setRealValue(givenName);
        resourceObject.add(attribute);

        PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);

        AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> ret = cc.addObject(shadow, null, result);
        return ret.getReturnValue();
    }

    @Test
    public void test100AddDeleteObject() throws Exception {
        OperationResult result = createOperationResult();

        Collection<ResourceAttribute<?>> identifiers = addSampleResourceObject("john", "John", "Smith");

        String uid;
        for (ResourceAttribute<?> resourceAttribute : identifiers) {
            if (SchemaConstants.ICFS_UID.equals(resourceAttribute.getElementName())) {
                uid = resourceAttribute.getValue(String.class).getValue();
                System.out.println("uuid:" + uid);
                assertNotNull(uid);
            }
        }

        ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);

        cc.deleteObject(accountDefinition, null, identifiers, null, result);

        ResourceObjectIdentification identification = ResourceObjectIdentification.createFromAttributes(
                accountDefinition, identifiers);
        PrismObject<ShadowType> resObj = null;
        try {
            resObj = cc.fetchObject(identification, null, null, result);
            Assert.fail();
        } catch (ObjectNotFoundException ex) {
            AssertJUnit.assertNull(resObj);
        }

    }

    @Test
    public void test110ChangeModifyObject() throws Exception {
        OperationResult result = createOperationResult();

        Collection<ResourceAttribute<?>> identifiers = addSampleResourceObject("john", "John", "Smith");

        Set<Operation> changes = new HashSet<>();

        changes.add(createAddAttributeChange("employeeNumber", "123123123"));
        changes.add(createReplaceAttributeChange("sn", "Smith007"));
        changes.add(createAddAttributeChange("street", "Wall Street"));
        changes.add(createDeleteAttributeChange("givenName", "John"));

        ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);
        ResourceObjectIdentification identification = ResourceObjectIdentification.createFromAttributes(
                accountDefinition, identifiers);

        cc.modifyObject(identification, null, changes, null, null, result);

        PrismObject<ShadowType> shadow = cc.fetchObject(identification, null, null, result);
        ResourceAttributeContainer resObj = ShadowUtil.getAttributesContainer(shadow);

        AssertJUnit.assertNull(resObj.findAttribute(QNAME_GIVEN_NAME));

        String addedEmployeeNumber = resObj
                .findAttribute(QNAME_EMPLOYEE_NUMBER).getValue(String.class)
                .getValue();
        String changedSn = resObj.findAttribute(QNAME_SN)
                .getValues(String.class).iterator().next().getValue();
        String addedStreet = resObj.findAttribute(new QName(MidPointConstants.NS_RI, "street"))
                .getValues(String.class).iterator().next().getValue();

        System.out.println("changed employee number: " + addedEmployeeNumber);
        System.out.println("changed sn: " + changedSn);
        System.out.println("added street: " + addedStreet);

        AssertJUnit.assertEquals("123123123", addedEmployeeNumber);
        AssertJUnit.assertEquals("Smith007", changedSn);
        AssertJUnit.assertEquals("Wall Street", addedStreet);

    }

    @Test
    public void test200FetchChanges() throws Exception {
        OperationResult result = createOperationResult();
        ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);
        UcfSyncToken lastToken = cc.fetchCurrentToken(accountDefinition, null, result);

        System.out.println("Property:");
        System.out.println(SchemaDebugUtil.prettyPrint(lastToken));
        System.out.println("token " + lastToken.toString());

        assertNotNull("No last token", lastToken);

        CollectingChangeListener handler = new CollectingChangeListener();
        cc.fetchChanges(accountDefinition, lastToken, null, null, null, handler, result);

        List<UcfLiveSyncChange> changes = handler.getChanges();
        displayValue("Changes", changes);

        // No changes (token-only changes are gone in 4.0.1)
        AssertJUnit.assertEquals(0, changes.size());
    }

    private PrismProperty createProperty(String propertyName, String propertyValue) throws SchemaException {
        ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);
        ResourceAttributeDefinition propertyDef = accountDefinition.findAttributeDefinition(new QName(
                MidPointConstants.NS_RI, propertyName));
        ResourceAttribute property = propertyDef.instantiate();
        property.setRealValue(propertyValue);
        return property;
    }

    private PropertyModificationOperation createReplaceAttributeChange(String propertyName, String propertyValue)
            throws SchemaException {
        PrismProperty<?> property = createProperty(propertyName, propertyValue);
        ItemPath propertyPath = ItemPath.create(ShadowType.F_ATTRIBUTES,
                new QName(MidPointConstants.NS_RI, propertyName));
        PropertyDelta delta = prismContext.deltaFactory().property().create(propertyPath, property.getDefinition());
        delta.setRealValuesToReplace(propertyValue);
        PropertyModificationOperation attributeModification = new PropertyModificationOperation(delta);
        return attributeModification;
    }

    private PropertyModificationOperation createAddAttributeChange(String propertyName, String propertyValue)
            throws SchemaException {
        PrismProperty<?> property = createProperty(propertyName, propertyValue);
        ItemPath propertyPath = ItemPath.create(ShadowType.F_ATTRIBUTES,
                new QName(MidPointConstants.NS_RI, propertyName));
        PropertyDelta delta = prismContext.deltaFactory().property().create(propertyPath, property.getDefinition());
        delta.addRealValuesToAdd(propertyValue);
        PropertyModificationOperation attributeModification = new PropertyModificationOperation(delta);
        return attributeModification;
    }

    private PropertyModificationOperation createDeleteAttributeChange(String propertyName, String propertyValue)
            throws SchemaException {
        PrismProperty<?> property = createProperty(propertyName, propertyValue);
        ItemPath propertyPath = ItemPath.create(ShadowType.F_ATTRIBUTES,
                new QName(MidPointConstants.NS_RI, propertyName));
        PropertyDelta delta = prismContext.deltaFactory().property().create(propertyPath, property.getDefinition());
        delta.addRealValuesToDelete(propertyValue);
        PropertyModificationOperation attributeModification = new PropertyModificationOperation(delta);
        return attributeModification;
    }

    /**
     * Simple call to connector test() method.
     */
    @Test
    public void test300TestConnection() {
        // GIVEN
        OperationResult result = new OperationResult(contextName());

        // WHEN

        cc.test(result);

        // THEN
        result.computeStatus("test failed");
        AssertJUnit.assertNotNull(result);
        OperationResult connectorConnectionResult = result.getSubresults().get(0);
        AssertJUnit.assertNotNull(connectorConnectionResult);
        System.out.println("Test \"connector connection\" result: " + connectorConnectionResult);
        AssertJUnit.assertTrue(connectorConnectionResult.isSuccess());
        AssertJUnit.assertTrue(result.isSuccess());
    }

    /**
     * Simple call to connector test() method.
     */
    @Test
    public void test310TestConnectionNegative() throws Exception {
        // GIVEN
        OperationResult result = new OperationResult(contextName());

        ConnectorInstance badConnector = factory.createConnectorInstance(connectorType,
                "bad resource", "bad resource description");
        badConnector.configure(
                badResourceType.getConnectorConfiguration().asPrismContainerValue(),
                ConnectorConfigurationOptions.DEFAULT,
                result);

        // WHEN

        badConnector.test(result);

        // THEN
        result.computeStatus("test failed");
        displayDumpable("Test result (FAILURE EXPECTED)", result);
        AssertJUnit.assertNotNull(result);
        OperationResult connectorConnectionResult = result.getSubresults().get(1);
        AssertJUnit.assertNotNull(connectorConnectionResult);
        System.out.println("Test \"connector connection\" result: " + connectorConnectionResult
                + " (FAILURE EXPECTED)");
        assertFalse("Unexpected success of bad connector test", connectorConnectionResult.isSuccess());
        assertFalse(result.isSuccess());
    }

    /**
     * Test fetching and translating resource schema.
     */
    @Test
    public void test400FetchResourceSchema() throws Exception {
        // GIVEN

        // WHEN
        // The schema was fetched during test init. Now just check if it was OK.

        // THEN

        AssertJUnit.assertNotNull(resourceSchema);

        System.out.println(resourceSchema.debugDump());

        Document xsdSchema = resourceSchema.serializeToXsd();

        System.out
                .println("-------------------------------------------------------------------------------------");
        System.out.println(DOMUtil.printDom(xsdSchema));
        System.out
                .println("-------------------------------------------------------------------------------------");

        ResourceObjectClassDefinition accountDefinition = resourceSchema
                .findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);

        AssertJUnit.assertFalse("No identifiers for account object class ", accountDefinition
                .getPrimaryIdentifiers().isEmpty());

        PrismPropertyDefinition<?> uidDefinition = accountDefinition.findAttributeDefinition(
                new QName(MidPointConstants.NS_RI,
                        OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME));
        AssertJUnit.assertNotNull(uidDefinition);

        for (Definition def : resourceSchema.getDefinitions()) {
            if (def instanceof ResourceAttributeContainerDefinition) {
                ResourceAttributeContainerDefinition rdef = (ResourceAttributeContainerDefinition) def;
                assertNotEmpty("No type name in object class", rdef.getTypeName());
                assertNotEmpty("No native object class for " + rdef.getTypeName(),
                        rdef.getNativeObjectClass());

                // This is maybe not that important, but just for a sake of
                // completeness
                assertNotEmpty("No name for " + rdef.getTypeName(), rdef.getItemName());
            }
        }

    }

    @Test
    public void test410Capabilities() throws Exception {
        // GIVEN
        OperationResult result = new OperationResult(contextName());

        // WHEN
        CapabilityCollectionType capabilities = cc.fetchCapabilities(result);

        // THEN
        result.computeStatus("getCapabilities failed");
        TestUtil.assertSuccess("getCapabilities failed (result)", result);
        assertFalse("Empty capabilities returned", CapabilityUtil.isEmpty(capabilities));
        CredentialsCapabilityType capCred = CapabilityUtil.getCapability(capabilities, CredentialsCapabilityType.class);
        assertThat(capCred).isNotNull();
        assertNotNull("password capability not present", capCred.getPassword());

        PagedSearchCapabilityType capPage = CapabilityUtil.getCapability(capabilities, PagedSearchCapabilityType.class);
        assertNotNull("paged search capability not present", capPage);
    }

    @Test
    public void test500FetchObject() throws Exception {
        // GIVEN
        ResourceAttributeContainer resourceObject = createResourceObject(
                "uid=Teell,ou=People,dc=example,dc=com", "Teell William", "Teell");

        OperationResult addResult = createOperationResult("addObject");

        PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);
        // Add a testing object
        cc.addObject(shadow, null, addResult);

        ResourceObjectDefinition accountDefinition = resourceObject.getDefinition().getComplexTypeDefinition();

        Collection<ResourceAttribute<?>> identifiers = resourceObject.getPrimaryIdentifiers();
        // Determine object class from the schema

        ResourceObjectIdentification identification = new ResourceObjectIdentification(accountDefinition, identifiers, null);
        OperationResult result = createOperationResult("fetchObject");

        // WHEN
        PrismObject<ShadowType> ro = cc.fetchObject(identification, null, null, result);

        // THEN

        AssertJUnit.assertNotNull(ro);
        System.out.println("Fetched object:\n" + ro.debugDump());
        System.out.println("Result:");
        System.out.println(result.debugDump());

        assertEquals("Wrong LDAP uid", "Teell", IntegrationTestTools.getAttributeValue(ro.asObjectable(), new QName(NS_RI, "uid")));

    }

    @Test
    public void test510Search() throws Exception {
        // GIVEN

        UcfExecutionContext ctx = createExecutionContext(resourceType);

        ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);
        // Determine object class from the schema

        UcfObjectHandler handler = (ucfObject, result) -> {
            displayDumpable("Search: found", ucfObject);
            return true;
        };

        OperationResult result = createOperationResult();

        // WHEN
        cc.search(accountDefinition, null, handler, null, null, null, null, ctx, result);

        // THEN

    }

    @Test
    public void test600CreateAccountWithPassword() throws Exception {
        // GIVEN
        ResourceAttributeContainer resourceObject = createResourceObject(
                "uid=lechuck,ou=people,dc=example,dc=com", "Ghost Pirate LeChuck", "LeChuck");

        ProtectedStringType ps = protector.encryptString("t4k30v3rTh3W0rld");

        OperationResult addResult = createOperationResult();

        PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);
        CredentialsType credentials = new CredentialsType();
        PasswordType pass = new PasswordType();
        pass.setValue(ps);
        credentials.setPassword(pass);
        shadow.asObjectable().setCredentials(credentials);

        // WHEN
        cc.addObject(shadow, null, addResult);

        // THEN

        String entryUuid = (String) resourceObject.getPrimaryIdentifier().getValue().getValue();
        Entry entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
        displayValue("Entry before change", entry);
        String passwordAfter = OpenDJController.getAttributeValue(entry, "userPassword");

        assertNotNull(passwordAfter);

        System.out.println("Changed password: " + passwordAfter);

        // TODO
    }

    @Test
    public void test610ChangePassword() throws Exception {
        // GIVEN
        ResourceAttributeContainer resourceObject = createResourceObject(
                "uid=drake,ou=People,dc=example,dc=com", "Sir Francis Drake", "Drake");
        PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);

        OperationResult addResult = createOperationResult();

        // Add a testing object
        cc.addObject(shadow, null, addResult);

        String entryUuid = (String) resourceObject.getPrimaryIdentifier().getValue().getValue();
        Entry entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
        displayValue("Entry before change", entry);
        String passwordBefore = OpenDJController.getAttributeValue(entry, "userPassword");
        // We have set no password during create, therefore the password should
        // be empty
        assertNull(passwordBefore);

        ResourceObjectDefinition accountDefinition = resourceObject.getDefinition().getComplexTypeDefinition();

        Collection<ResourceAttribute<?>> identifiers = resourceObject.getPrimaryIdentifiers();
        // Determine object class from the schema

        OperationResult result = new OperationResult(this.getClass().getName() + ".testFetchObject");

        // WHEN

        Set<Operation> changes = new HashSet<>();
        ProtectedStringType passPs = protector.encryptString("salalala");

        ItemDeltaType propMod = new ItemDeltaType();
        //create modification path
        ItemPathType path = prismContext.itemPathParser().asItemPathType("credentials/password/value");
        propMod.setPath(path);

        //set the replace value
        XNode passPsXnode = prismContext.xnodeSerializer().root(new QName("dummy")).serializeRealValue(passPs).getSubnode();
        RawType value = new RawType(passPsXnode.frozen(), prismContext);
        propMod.getValue().add(value);

        //set the modification type
        propMod.setModificationType(ModificationTypeType.REPLACE);

        PropertyDelta<ProtectedStringType> passDelta = (PropertyDelta) DeltaConvertor.createItemDelta(propMod, shadow.getDefinition());
        PropertyModificationOperation<ProtectedStringType> passwordModification = new PropertyModificationOperation(passDelta);
        changes.add(passwordModification);

        ResourceObjectIdentification identification = ResourceObjectIdentification.createFromAttributes(
                accountDefinition, identifiers);

        cc.modifyObject(identification, null, changes, null, null, result);

        // THEN

        entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
        displayValue("Entry after change", entry);

        String passwordAfter = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull(passwordAfter);

        System.out.println("Account password: " + passwordAfter);
    }

    private ResourceAttributeContainer createResourceObject(String dn, String sn, String cn) throws SchemaException {
        // Account type is hardcoded now
        ResourceObjectClassDefinition accountDefinition = resourceSchema
                .findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);
        // Determine identifier from the schema
        ResourceAttributeContainer resourceObject = accountDefinition.instantiate(ShadowType.F_ATTRIBUTES);

        //noinspection unchecked
        ResourceAttributeDefinition<String> road =
                (ResourceAttributeDefinition<String>)
                        accountDefinition.findAttributeDefinitionRequired(QNAME_SN);
        ResourceAttribute<String> roa = road.instantiate();
        roa.setRealValue(sn);
        resourceObject.add(roa);

        //noinspection unchecked
        road = (ResourceAttributeDefinition<String>)
                accountDefinition.findAttributeDefinitionRequired(QNAME_CN);
        roa = road.instantiate();
        roa.setRealValue(cn);
        resourceObject.add(roa);

        //noinspection unchecked
        road = (ResourceAttributeDefinition<String>)
                accountDefinition.findAttributeDefinitionRequired(OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
        roa = road.instantiate();
        roa.setRealValue(dn);
        resourceObject.add(roa);

        return resourceObject;
    }

    private <T extends ShadowType> PrismObject<T> wrapInShadow(Class<T> type, ResourceAttributeContainer resourceObject) throws SchemaException {
        PrismObjectDefinition<T> shadowDefinition = getShadowDefinition(type);
        PrismObject<T> shadow = shadowDefinition.instantiate();
        resourceObject.setElementName(ShadowType.F_ATTRIBUTES);
        shadow.getValue().add(resourceObject);
        return shadow;
    }

    private <T extends ShadowType> PrismObjectDefinition<T> getShadowDefinition(Class<T> type) {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    }
}
