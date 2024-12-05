/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertNotEmpty;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
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

    private ResourceType badResourceBean;
    private ConnectorType ldapConnectorBean;
    private ConnectorFactory factory;
    private ConnectorSchema connectorSchema;

    @Autowired ConnectorFactory connectorFactoryIcfImpl;
    @Autowired Protector protector;
    @Autowired PrismContext prismContext;

    protected static OpenDJController openDJController = new OpenDJController();

    @Override
    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        SchemaDebugUtil.initializePrettyPrinter();
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
        resourceBean = resource.asObjectable();

        // Resource: Second copy for negative test cases
        PrismObject<ResourceType> badResource = PrismTestUtil.parseObject(RESOURCE_OPENDJ_BAD_FILE);
        badResourceBean = badResource.asObjectable();

        // Connector
        PrismObject<ConnectorType> connector = PrismTestUtil.parseObject(CONNECTOR_LDAP_FILE);
        ldapConnectorBean = connector.asObjectable();

        factory = connectorFactoryIcfImpl;

        connectorSchema = factory.generateConnectorConfigurationSchema(ldapConnectorBean);
        AssertJUnit.assertNotNull("Cannot generate connector schema", connectorSchema);
        displayDumpable("Connector schema", connectorSchema);

        cc = factory.createConnectorInstance(ldapConnectorBean,
                "OpenDJ resource",
                "description of OpenDJ connector instance");

        OperationResult result = new OperationResult("initUcf");
        configure(
                resourceBean.getConnectorConfiguration(),
                ResourceTypeUtil.getSchemaGenerationConstraints(resourceBean),
                result);

        cc.initialize(null, null, result);
        // TODO: assert something

        var nativeResourceSchema = cc.fetchResourceSchema(result);
        resourceSchema = ResourceSchemaFactory.nativeToBare(nativeResourceSchema);
        displayDumpable("Resource schema", resourceSchema);
        AssertJUnit.assertNotNull(resourceSchema);

        completeResourceSchema = ResourceSchemaFactory.parseCompleteSchema(resourceBean, nativeResourceSchema);
    }

    @AfterMethod
    public void shutdownUcf() {
    }

    @Test
    public void test010ConnectorSchemaSanity() throws SchemaException {
        IntegrationTestTools.assertConnectorSchemaSanity(connectorSchema, "LDAP connector", true);

        PrismContainerDefinition<?> configurationDefinition = connectorSchema.getConnectorConfigurationContainerDefinition();
        PrismContainerDefinition<?> configurationPropertiesDefinition =
                configurationDefinition.findContainerDefinition(SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME);

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
        assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.getSimpleAttributeDefinitions().isEmpty());

        Collection<? extends ShadowSimpleAttributeDefinition<?>> identifiers = accountDefinition.getPrimaryIdentifiers();
        assertNotNull("Null identifiers for " + objectClassQname, identifiers);
        assertFalse("Empty identifiers for " + objectClassQname, identifiers.isEmpty());

        ShadowSimpleAttributeDefinition<?> idPrimaryDef = accountDefinition.findSimpleAttributeDefinition(
                new QName(MidPointConstants.NS_RI, OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME));
        assertNotNull("No definition for attribute " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME, idPrimaryDef);
        assertTrue("Attribute " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " in not an identifier",
                accountDefinition.isPrimaryIdentifier(
                        idPrimaryDef.getItemName()));
        assertTrue("Attribute " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " in not in identifiers list", identifiers.contains(idPrimaryDef));
        assertEquals("Attribute " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " has wrong native name", OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME, idPrimaryDef.getNativeAttributeName());
        assertEquals("Attribute " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " has wrong framework name", Uid.NAME, idPrimaryDef.getFrameworkAttributeName());
        assertEquals("Attribute " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " has wrong type", DOMUtil.XSD_STRING, idPrimaryDef.getTypeName());

        ShadowSimpleAttributeDefinition<?> idSecondaryDef =
                accountDefinition.findSimpleAttributeDefinition(OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
        assertNotNull("No definition for attribute " + SchemaConstants.ICFS_NAME, idSecondaryDef);
        assertTrue("Attribute " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " in not secondary identifier",
                accountDefinition.isSecondaryIdentifier(
                        idSecondaryDef.getItemName()));
        assertFalse("Attribute " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " in in identifiers list and it should NOT be", identifiers.contains(idSecondaryDef));
        assertTrue("Attribute " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " in not in secondary identifiers list", accountDefinition.getSecondaryIdentifiers().contains(idSecondaryDef));
        assertEquals("Attribute " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " has wrong native name", OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME, idSecondaryDef.getNativeAttributeName());
        assertEquals("Attribute " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " has wrong framework name", Name.NAME, idSecondaryDef.getFrameworkAttributeName());
        assertEquals("Attribute " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " has wrong type", DOMUtil.XSD_STRING, idSecondaryDef.getTypeName());

        assertEquals("Unexpected identifiers: " + identifiers, 1, identifiers.size());
        assertEquals("Unexpected secondary identifiers: " + accountDefinition.getSecondaryIdentifiers(), 1, accountDefinition.getSecondaryIdentifiers().size());
    }

    private Collection<ShadowAttribute<?, ?, ?, ?>> addSampleResourceObject(String name, String givenName, String familyName)
            throws Exception {
        OperationResult result = new OperationResult(this.getClass().getName() + ".testAdd");

        QName objectClassQname = OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME;
        ResourceObjectClassDefinition accountDefinition = resourceSchema.findObjectClassDefinition(objectClassQname);
        assertNotNull("No object class definition " + objectClassQname, accountDefinition);
        ShadowAttributesContainer resourceObject = accountDefinition.toShadowAttributesContainerDefinition().instantiate();

        resourceObject.addAttribute(
                accountDefinition
                        .findSimpleAttributeDefinitionRequired(OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER)
                        .instantiateFromRealValue("uid=" + name + ",ou=people,dc=example,dc=com"));

        resourceObject.addAttribute(
                accountDefinition
                        .findSimpleAttributeDefinitionRequired(QNAME_SN)
                        .instantiateFromRealValue(familyName));

        resourceObject.addAttribute(
                accountDefinition
                        .findSimpleAttributeDefinitionRequired(QNAME_CN)
                        .instantiateFromRealValue(givenName + " " + familyName));

        resourceObject.addAttribute(
                accountDefinition
                        .findSimpleAttributeDefinitionRequired(QNAME_GIVEN_NAME)
                        .instantiateFromRealValue(givenName));

        PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);

        var ctx = createExecutionContext();
        var ret = cc.addObject(shadow, ctx, result);
        return ret.getReturnValue();
    }

    @Test
    public void test100AddDeleteObject() throws Exception {
        OperationResult result = createOperationResult();

        var attributes = addSampleResourceObject("john", "John", "Smith");

        var ctx = createExecutionContext();

        String uid;
        for (var attribute : attributes) {
            if (SchemaConstants.ICFS_UID.equals(attribute.getElementName())) {
                uid = ((ShadowSimpleAttribute<?>) attribute).getValue(String.class).getValue();
                System.out.println("uuid:" + uid);
                assertNotNull(uid);
            }
        }

        ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);
        var identification =
                ResourceObjectIdentification.fromAttributes(accountDefinition, attributes)
                        .ensurePrimary();

        cc.deleteObject(identification, null, ctx, result);

        UcfResourceObject resObj = null;
        try {
            resObj = cc.fetchObject(identification, null, ctx, result);
            Assert.fail();
        } catch (ObjectNotFoundException ex) {
            AssertJUnit.assertNull(resObj);
        }

    }

    @Test
    public void test110ChangeModifyObject() throws Exception {
        OperationResult result = createOperationResult();

        var attributes = addSampleResourceObject("john", "John", "Smith");

        Set<Operation> changes = new HashSet<>();

        changes.add(createAddAttributeChange("employeeNumber", "123123123"));
        changes.add(createReplaceAttributeChange("sn", "Smith007"));
        changes.add(createAddAttributeChange("street", "Wall Street"));
        changes.add(createDeleteAttributeChange("givenName", "John"));

        ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);
        var identification = ResourceObjectIdentification
                .fromAttributes(accountDefinition, attributes)
                .ensurePrimary();

        var ctx = createExecutionContext();

        cc.modifyObject(identification, null, changes, null, ctx, result);

        var resourceObject = cc.fetchObject(identification, null, ctx, result);
        ShadowAttributesContainer resObj = ShadowUtil.getAttributesContainer(resourceObject.getBean());

        AssertJUnit.assertNull(resObj.findSimpleAttribute(QNAME_GIVEN_NAME));

        String addedEmployeeNumber = resObj
                .findSimpleAttribute(QNAME_EMPLOYEE_NUMBER).getValue(String.class)
                .getValue();
        String changedSn = resObj.findSimpleAttribute(QNAME_SN)
                .getValues(String.class).iterator().next().getValue();
        String addedStreet = resObj.findSimpleAttribute(new QName(MidPointConstants.NS_RI, "street"))
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
        var ctx = createExecutionContext();
        UcfSyncToken lastToken = cc.fetchCurrentToken(accountDefinition, ctx, result);

        System.out.println("Property:");
        System.out.println(SchemaDebugUtil.prettyPrint(lastToken));
        System.out.println("token " + lastToken.toString());

        assertNotNull("No last token", lastToken);

        CollectingChangeListener handler = new CollectingChangeListener();
        cc.fetchChanges(accountDefinition, lastToken, null, null, ctx, handler, result);

        List<UcfLiveSyncChange> changes = handler.getChanges();
        displayValue("Changes", changes);

        // No changes (token-only changes are gone in 4.0.1)
        AssertJUnit.assertEquals(0, changes.size());
    }

    private PrismProperty<String> createProperty(String propertyName, String propertyValue) throws SchemaException {
        return resourceSchema.findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME)
                .<String>findSimpleAttributeDefinitionRequired(new QName(MidPointConstants.NS_RI, propertyName))
                .instantiateFromRealValue(propertyValue);
    }

    private PropertyModificationOperation createReplaceAttributeChange(String propertyName, String propertyValue)
            throws SchemaException {
        PrismProperty<?> property = createProperty(propertyName, propertyValue);
        ItemPath propertyPath = ItemPath.create(ShadowType.F_ATTRIBUTES,
                new QName(MidPointConstants.NS_RI, propertyName));
        PropertyDelta delta = prismContext.deltaFactory().property().create(propertyPath, property.getDefinition());
        delta.setRealValuesToReplace(propertyValue);
        return new PropertyModificationOperation(delta);
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
        return new PropertyModificationOperation(delta);
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

        ConnectorInstance badConnector = factory.createConnectorInstance(
                ldapConnectorBean, "bad resource", "bad resource description");
        badConnector.configure(
                new ConnectorConfiguration(
                        badResourceBean.getConnectorConfiguration().asPrismContainerValue(),
                        List.of()),
                new ConnectorConfigurationOptions(),
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

        Document xsdSchema = resourceSchema.serializeNativeToXsd();

        System.out
                .println("-------------------------------------------------------------------------------------");
        System.out.println(DOMUtil.printDom(xsdSchema));
        System.out
                .println("-------------------------------------------------------------------------------------");

        ResourceObjectClassDefinition accountDefinition = resourceSchema
                .findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);

        AssertJUnit.assertFalse("No identifiers for account object class ", accountDefinition
                .getPrimaryIdentifiers().isEmpty());

        PrismPropertyDefinition<?> uidDefinition = accountDefinition.findSimpleAttributeDefinition(
                new QName(MidPointConstants.NS_RI,
                        OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME));
        AssertJUnit.assertNotNull(uidDefinition);

        for (Definition def : resourceSchema.getDefinitions()) {
            if (def instanceof ShadowAttributesContainerDefinition rdef) {
                assertNotEmpty("No type name in object class", rdef.getTypeName());

                // This is maybe not that important, but just for a sake of completeness
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
        ShadowAttributesContainer resourceObject = createResourceObject(
                "uid=Teell,ou=People,dc=example,dc=com", "Teell William", "Teell");

        OperationResult addResult = createOperationResult("addObject");

        var ctx = createExecutionContext();

        PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);
        // Add a testing object
        var addObjectResult = cc.addObject(shadow, ctx, addResult);

        var accountDefinition = resourceObject.getDefinition().getResourceObjectDefinition();
        var identification = getPrimaryIdentification(accountDefinition, addObjectResult);

        // Determine object class from the schema
        OperationResult result = createOperationResult("fetchObject");

        // WHEN
        var ro = cc.fetchObject(identification, null, ctx, result);

        // THEN

        AssertJUnit.assertNotNull(ro);
        System.out.println("Fetched object:\n" + ro.debugDump());
        System.out.println("Result:");
        System.out.println(result.debugDump());

        assertEquals("Wrong LDAP uid", "Teell",
                IntegrationTestTools.getAttributeValue(ro.getBean(), new QName(NS_RI, "uid")));

    }

    private static ResourceObjectIdentification.WithPrimary getPrimaryIdentification(
            ResourceObjectDefinition accountDefinition, UcfAddReturnValue addObjectResult) {
        return ResourceObjectIdentification.fromAttributes(
                        accountDefinition,
                        Objects.requireNonNull(addObjectResult.getKnownCreatedObjectAttributes()))
                .ensurePrimary();
    }

    @Test
    public void test510Search() throws Exception {
        // GIVEN

        var ctx = createExecutionContext();

        ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);
        // Determine object class from the schema

        UcfObjectHandler handler = (ucfObject, result) -> {
            displayDumpable("Search: found", ucfObject);
            return true;
        };

        OperationResult result = createOperationResult();

        // WHEN
        cc.search(
                accountDefinition, null, handler, null, null,
                null, null, ctx, result);

        // THEN

    }

    @Test
    public void test600CreateAccountWithPassword() throws Exception {
        // GIVEN
        ShadowAttributesContainer resourceObject = createResourceObject(
                "uid=lechuck,ou=people,dc=example,dc=com", "Ghost Pirate LeChuck", "LeChuck");

        ProtectedStringType ps = protector.encryptString("t4k30v3rTh3W0rld");

        OperationResult addResult = createOperationResult();

        PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);
        CredentialsType credentials = new CredentialsType();
        PasswordType pass = new PasswordType();
        pass.setValue(ps);
        credentials.setPassword(pass);
        shadow.asObjectable().setCredentials(credentials);

        var ctx = createExecutionContext();

        // WHEN
        var addObjectResult = cc.addObject(shadow, ctx, addResult);

        var accountDefinition = resourceObject.getDefinition().getResourceObjectDefinition();
        var identification = getPrimaryIdentification(accountDefinition, addObjectResult);

        // THEN

        var entryUuid = (String) identification.getPrimaryIdentifier().getValue().getRealValue();
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
        ShadowAttributesContainer resourceObject = createResourceObject(
                "uid=drake,ou=People,dc=example,dc=com", "Sir Francis Drake", "Drake");
        PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);

        OperationResult addResult = createOperationResult();

        var ctx = createExecutionContext();

        // Add a testing object
        var addObjectResult = cc.addObject(shadow, ctx, addResult);

        var accountDefinition = resourceObject.getDefinition().getResourceObjectDefinition();
        var identification = getPrimaryIdentification(accountDefinition, addObjectResult);
        var entryUuid = (String) identification.getPrimaryIdentifier().getValue().getRealValue();

        Entry entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
        displayValue("Entry before change", entry);
        String passwordBefore = OpenDJController.getAttributeValue(entry, "userPassword");
        // We have set no password during create, therefore the password should
        // be empty
        assertNull(passwordBefore);

        var identifiers = identification.getPrimaryIdentifiers();
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
        RawType value = new RawType(passPsXnode.frozen());
        propMod.getValue().add(value);

        //set the modification type
        propMod.setModificationType(ModificationTypeType.REPLACE);

        //noinspection unchecked,rawtypes
        PropertyDelta<ProtectedStringType> passDelta = (PropertyDelta) DeltaConvertor.createItemDelta(propMod, shadow.getDefinition());
        //noinspection rawtypes,unchecked
        PropertyModificationOperation<ProtectedStringType> passwordModification = new PropertyModificationOperation(passDelta);
        changes.add(passwordModification);

        cc.modifyObject(identification, null, changes, null, ctx, result);

        // THEN

        entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
        displayValue("Entry after change", entry);

        String passwordAfter = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull(passwordAfter);

        System.out.println("Account password: " + passwordAfter);
    }

    private ShadowAttributesContainer createResourceObject(String dn, String sn, String cn) throws SchemaException {
        // Account type is hardcoded now
        var accountOcDef = resourceSchema.findObjectClassDefinitionRequired(OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME);
        // Determine identifier from the schema
        var attributeContainer = accountOcDef.toShadowAttributesContainerDefinition().instantiate();

        ShadowSimpleAttributeDefinition<String> road = accountOcDef.findSimpleAttributeDefinitionRequired(QNAME_SN);
        ShadowSimpleAttribute<String> roa = road.instantiate();
        roa.setRealValue(sn);
        attributeContainer.addAttribute(roa);

        road = accountOcDef.findSimpleAttributeDefinitionRequired(QNAME_CN);
        roa = road.instantiate();
        roa.setRealValue(cn);
        attributeContainer.addAttribute(roa);

        road = accountOcDef.findSimpleAttributeDefinitionRequired(OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
        roa = road.instantiate();
        roa.setRealValue(dn);
        attributeContainer.addAttribute(roa);

        return attributeContainer;
    }

    private <T extends ShadowType> PrismObject<T> wrapInShadow(
            Class<T> type, ShadowAttributesContainer resourceObject) throws SchemaException {
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
