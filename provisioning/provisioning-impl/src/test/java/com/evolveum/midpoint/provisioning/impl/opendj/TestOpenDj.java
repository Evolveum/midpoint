/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_PASSWORD;

import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.serializeToXml;
import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.*;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.impl.polystring.DistinguishedNameNormalizer;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.opends.server.types.Entry;
import org.opends.server.util.LDIFException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.LiveSyncTokenStorage;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.DummyTokenStorageImpl;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.constants.TestResourceOpNames;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Test for provisioning service implementation.
 *
 * This test will initialize embedded OpenDJ as a target resource.
 *
 * @author Radovan Semancik
 * @author Katka Valalikova
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDj extends AbstractOpenDjTest {

    protected static final String USER_JACK_FULL_NAME = "Jack Sparrow";
    private static final File FILE_MODIFY_ASSOCIATION_REPLACE = new File(TEST_DIR, "account-modify-association.xml");

    private static final String[] JACK_FULL_NAME_LANG_EN_SK = {
            "en", "Jack Sparrow",
            "sk", "Džek Sperou"
    };

    private static final String[] JACK_FULL_NAME_LANG_EN_SK_RU_HR = {
            "en", "Jack Sparrow",
            "sk", "Džek Sperou",
            "ru", "Джек Воробей",
            "hr", "Ðek Sperou"
    };

    private static final String[] JACK_FULL_NAME_LANG_CZ_HR = {
            "cz", "Džek Sperou",
            "hr", "Ðek Sperou"
    };

    private static final File RESOURCE_OPENDJ_NO_READ_FILE = new File(TEST_DIR, "resource-opendj-no-read.xml");
    private static final File RESOURCE_OPENDJ_NO_CREATE_FILE = new File(TEST_DIR, "resource-opendj-no-create.xml");
    private static final File RESOURCE_OPENDJ_NO_DELETE_FILE = new File(TEST_DIR, "resource-opendj-no-delete.xml");
    private static final File RESOURCE_OPENDJ_NO_UPDATE_FILE = new File(TEST_DIR, "resource-opendj-no-update.xml");

    private static final String INTENT_LDAP_GROUP = "ldapGroup";
    private static final ResourceObjectTypeIdentification TYPE_LDAP_GROUP =
            ResourceObjectTypeIdentification.of(ShadowKindType.ENTITLEMENT, INTENT_LDAP_GROUP);

    private String groupSailorOid;

    protected int getNumberOfBaseContextShadows() {
        return 0;
    }

    @BeforeClass
    public void startLdap() throws Exception {
        doStartLdap();
        openDJController.addEntry("""
                dn: ou=specialgroups,dc=example,dc=com
                objectclass: organizationalUnit
                ou: specialgroups
                """);
    }

    @AfterClass
    public void stopLdap() {
        doStopLdap();
    }

    /**
     * This should be the very first test that works with the resource.
     *
     * The original repository object does not have resource schema. The schema should be generated from
     * the resource on the first use. This is the test that executes testResource and checks whether the
     * schema was generated.
     */
    @Test
    public void test003Connection() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ResourceType resourceTypeBefore =
                repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, result).asObjectable();
        assertNotNull("No connector ref", resourceTypeBefore.getConnectorRef());
        assertNotNull("No connector ref OID", resourceTypeBefore.getConnectorRef().getOid());
        connector = repositoryService.getObject(
                ConnectorType.class, resourceTypeBefore.getConnectorRef().getOid(), null, result);
        ConnectorType connectorType = connector.asObjectable();
        assertNotNull(connectorType);
        Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchemaElement(resourceTypeBefore);
        AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

        OperationResult operationResult = provisioningService.testResource(RESOURCE_OPENDJ_OID, task, result);

        display("Test connection result", operationResult);
        TestUtil.assertSuccess("Test connection failed", operationResult);

        PrismObject<ResourceType> resourceRepoAfter =
                repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, result);
        ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();

        display("Resource after testResource (repository)", resourceTypeRepoAfter);

        displayValue("Resource after testResource (repository, XML)", PrismTestUtil.serializeToXml(resourceTypeRepoAfter));

        XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
        assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
        Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchemaElement(resourceTypeRepoAfter);
        assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);

        CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
        assertNotNull("No caching metadata", cachingMetadata);
        assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
        assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

        Element xsdElement = Objects.requireNonNull(ResourceTypeUtil.getResourceXsdSchemaElement(resourceTypeRepoAfter));
        ResourceSchema parsedSchema = ResourceSchemaFactory.parseNativeSchemaAsBare(xsdElement);
        assertNotNull("No schema after parsing", parsedSchema);

        Collection<ResourceObjectClassDefinition> objectClasses = parsedSchema.getObjectClassDefinitions();
        List<QName> objectClassesToGenerate = ResourceTypeUtil.getSchemaGenerationConstraints(resourceTypeRepoAfter);
        if (!objectClassesToGenerate.isEmpty() && !objectClasses.isEmpty()) {
            assertEquals("Unexpected object classes in generate schema", objectClassesToGenerate.size(), objectClasses.size());
        }

        ResourceObjectClassDefinition inetOrgPersonDefinition =
                parsedSchema.findObjectClassDefinitionRequired(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        assertNull("The _PASSWORD_ attribute sneaked into schema",
                inetOrgPersonDefinition.findSimpleAttributeDefinition(ICFS_PASSWORD));
        assertNull("The userPassword attribute sneaked into schema",
                inetOrgPersonDefinition.findSimpleAttributeDefinition(new QName(NS_RI, "userPassword")));

        assertShadows(1);
    }

    @Test
    public void test004ResourceAndConnectorCaching() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        resource = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result);
        resourceBean = resource.asObjectable();
        ConnectorInstance configuredConnectorInstance =
                resourceManager.getConfiguredConnectorInstance(
                        resource.asObjectable(), ReadCapabilityType.class, false, result);
        assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
        var bareSchema = ResourceSchemaFactory.getBareSchema(resource);
        assertNotNull("No resource schema", bareSchema);

        when();
        PrismObject<ResourceType> resourceAgain =
                provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result);

        then();
        ResourceType resourceTypeAgain = resourceAgain.asObjectable();
        assertNotNull("No connector ref", resourceTypeAgain.getConnectorRef());
        assertNotNull("No connector ref OID", resourceTypeAgain.getConnectorRef().getOid());

        PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        PrismContainer<Containerable> configurationContainerAgain = resourceAgain.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));
        assertEquals("Configurations not equals", configurationContainerAgain, configurationContainer);

        var bareSchemaAgain = ResourceSchemaFactory.getBareSchema(resourceAgain);
        assertNotNull("No resource schema (again)", bareSchemaAgain);
        assertEquals("Schema serial number mismatch", resourceBean.getSchema().getCachingMetadata().getSerialNumber(),
                resourceTypeAgain.getSchema().getCachingMetadata().getSerialNumber());
        assertNativeSchemaCached(bareSchema, bareSchemaAgain);

        // Now we stick our nose deep inside the provisioning impl. But we need to make sure that the
        // configured connector is properly cached
        ConnectorInstance configuredConnectorInstanceAgain =
                resourceManager.getConfiguredConnectorInstance(
                        resourceAgain.asObjectable(), ReadCapabilityType.class, false, result);
        assertSame("Connector instance was not cached", configuredConnectorInstance, configuredConnectorInstanceAgain);

        assertShadows(1);
    }

    @Test
    public void test005Capabilities() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ResourceType resource = provisioningService
                .getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result)
                .asObjectable();

        then();
        display("Resource from provisioning", resource);
        displayValue("Resource from provisioning (XML)", serializeToXml(resource));

        CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
        assertFalse("Empty capabilities returned", CapabilityUtil.isEmpty(nativeCapabilities));
        CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilities, CredentialsCapabilityType.class);
        assertNotNull("credentials capability not found", capCred);
        PasswordCapabilityType capPassword = capCred.getPassword();
        assertNotNull("password capability not present", capPassword);
        assertPasswordCapability(capPassword);

        // Connector cannot do activation, this should be null
        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilities, ActivationCapabilityType.class);
        assertNull("Found activation capability while not expecting it", capAct);

        ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilities, ScriptCapabilityType.class);
        assertNotNull("No script capability", capScript);
        List<ScriptCapabilityHostType> scriptHosts = capScript.getHost();
        assertEquals("Wrong number of script hosts", 1, scriptHosts.size());
        ScriptCapabilityHostType scriptHost = scriptHosts.get(0);
        assertEquals("Wrong script host type", ProvisioningScriptHostType.CONNECTOR, scriptHost.getType());
//        assertEquals("Wrong script host language", ....., scriptHost.getLanguage());

        ReadCapabilityType capRead = CapabilityUtil.getCapability(nativeCapabilities, ReadCapabilityType.class);
        assertNotNull("No read capability", capRead);
        assertNull("Read capability is caching only", capRead.isCachingOnly());
        assertTrue("Read capability is not 'return default'", capRead.isReturnDefaultAttributesOption());

        CreateCapabilityType capCreate = CapabilityUtil.getCapability(nativeCapabilities, CreateCapabilityType.class);
        assertNotNull("No create capability", capCreate);

        UpdateCapabilityType capUpdate = CapabilityUtil.getCapability(nativeCapabilities, UpdateCapabilityType.class);
        assertNotNull("No update capability", capUpdate);

        DeleteCapabilityType capDelete = CapabilityUtil.getCapability(nativeCapabilities, DeleteCapabilityType.class);
        assertNotNull("No delete capability", capDelete);

        dumpResourceCapabilities(resource);

        capCred = ResourceTypeUtil.getEnabledCapability(resource, CredentialsCapabilityType.class);
        assertNotNull("credentials effective capability not found", capCred);
        assertNotNull("password effective capability not found", capCred.getPassword());
        // Although connector does not support activation, the resource specifies a way how to simulate it.
        // Therefore the following should succeed
        capAct = ResourceTypeUtil.getEnabledCapability(resource, ActivationCapabilityType.class);
        if (isActivationCapabilityClassSpecific()) {
            assertNull("activation capability should not be present at the resource level", capAct);
        } else {
            assertNotNull("activation capability not found", capAct);
        }

        PagedSearchCapabilityType capPage = ResourceTypeUtil.getEnabledCapability(resource, PagedSearchCapabilityType.class);
        assertNotNull("paged search capability not present", capPage);

        assertShadows(1);
    }

    protected void assertPasswordCapability(PasswordCapabilityType capPassword) {
        assertThat(capPassword.isReadable())
                .as("password capability readable flag")
                .isNotEqualTo(Boolean.TRUE);
    }

    @Test
    public void test006Schema() throws Exception {
        when();

        var bareSchema = ResourceSchemaFactory.getBareSchema(resourceBean);
        displayDumpable("Resource schema (bare)", bareSchema);

        var completeSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resourceBean);
        displayDumpable("Resource schema (complete)", completeSchema);

        ResourceObjectClassDefinition accountClassDefBare =
                bareSchema.findObjectClassDefinitionRequired(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        assertNotNull("Account definition is missing", accountClassDefBare);
        assertNotNull("Null identifiers in account", accountClassDefBare.getPrimaryIdentifiers());
        assertFalse("Empty identifiers in account", accountClassDefBare.getPrimaryIdentifiers().isEmpty());
        assertNotNull("Null secondary identifiers in account", accountClassDefBare.getSecondaryIdentifiers());
        assertFalse("Empty secondary identifiers in account", accountClassDefBare.getSecondaryIdentifiers().isEmpty());
        assertNotNull("No naming attribute in account", accountClassDefBare.getNamingAttribute());
        assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountClassDefBare.getNativeObjectClassName()));

        ShadowSimpleAttributeDefinition<?> idPrimaryDef =
                accountClassDefBare.findSimpleAttributeDefinitionRequired(getPrimaryIdentifierQName());
        assertEquals(1, idPrimaryDef.getMaxOccurs());
        assertEquals(0, idPrimaryDef.getMinOccurs());
        assertFalse("UID has create", idPrimaryDef.canAdd());
        assertFalse("UID has update", idPrimaryDef.canModify());
        assertTrue("No UID read", idPrimaryDef.canRead());
        assertTrue("UID definition not in identifiers", accountClassDefBare.getPrimaryIdentifiers().contains(idPrimaryDef));
        assertEquals("Wrong " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " frameworkAttributeName",
                ProvisioningTestUtil.CONNID_UID_NAME, idPrimaryDef.getFrameworkAttributeName());
        assertEquals("Wrong primary identifier matching rule", PrismConstants.UUID_MATCHING_RULE_NAME, idPrimaryDef.getMatchingRuleQName());

        ShadowSimpleAttributeDefinition<?> idSecondaryDef =
                accountClassDefBare.findSimpleAttributeDefinitionRequired(getSecondaryIdentifierQName());
        assertEquals(1, idSecondaryDef.getMaxOccurs());
        assertEquals(1, idSecondaryDef.getMinOccurs());
        assertTrue("No NAME create", idSecondaryDef.canAdd());
        assertTrue("No NAME update", idSecondaryDef.canModify());
        assertTrue("No NAME read", idSecondaryDef.canRead());
        assertTrue("NAME definition not in secondary identifiers", accountClassDefBare.getSecondaryIdentifiers().contains(idSecondaryDef));
        assertEquals("Wrong " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " frameworkAttributeName", ProvisioningTestUtil.CONNID_NAME_NAME, idSecondaryDef.getFrameworkAttributeName());
        assertEquals("Wrong secondary identifier matching rule", PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME, idSecondaryDef.getMatchingRuleQName());

        ShadowSimpleAttributeDefinition<?> cnDef = accountClassDefBare.findSimpleAttributeDefinition("cn");
        assertNotNull("No definition for cn", cnDef);
        assertEquals(-1, cnDef.getMaxOccurs());
        assertEquals(1, cnDef.getMinOccurs());
        assertTrue("No cn create", cnDef.canAdd());
        assertTrue("No cn update", cnDef.canModify());
        assertTrue("No cn read", cnDef.canRead());
        assertEquals("Wrong cn matching rule", PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME, cnDef.getMatchingRuleQName());

        ShadowSimpleAttributeDefinition<?> jpegPhoto = accountClassDefBare.findSimpleAttributeDefinition("jpegPhoto");
        assertNotNull("No definition for jpegPhoto", jpegPhoto);
        assertEquals(-1, jpegPhoto.getMaxOccurs());
        assertEquals(0, jpegPhoto.getMinOccurs());
        assertTrue("No jpegPhoto create", jpegPhoto.canAdd());
        assertTrue("No jpegPhoto update", jpegPhoto.canModify());
        assertTrue("No jpegPhoto read", jpegPhoto.canRead());
        assertNull("Wrong jpegPhoto matching rule", jpegPhoto.getMatchingRuleQName());

        ShadowSimpleAttributeDefinition<?> dsDef = accountClassDefBare.findSimpleAttributeDefinition("ds-pwp-account-disabled");
        assertNotNull("No definition for ds-pwp-account-disabled", dsDef);
        assertEquals(1, dsDef.getMaxOccurs());
        assertEquals(0, dsDef.getMinOccurs());
        assertTrue("No ds-pwp-account-disabled read", dsDef.canRead());
        assertTrue("No ds-pwp-account-disabled create", dsDef.canAdd());
        assertTrue("No ds-pwp-account-disabled update", dsDef.canModify());
        // TODO: MID-2358
//        assertTrue("ds-pwp-account-disabled is NOT operational", dsDef.isOperational());

        assertMemberOfAttributeBare(accountClassDefBare);

        ShadowSimpleAttributeDefinition<?> labeledUriDef = accountClassDefBare.findSimpleAttributeDefinition("labeledURI");
        assertNotNull("No definition for labeledUri", labeledUriDef);
        assertEquals(-1, labeledUriDef.getMaxOccurs());
        assertEquals(0, labeledUriDef.getMinOccurs());
        assertTrue("No labeledUri create", labeledUriDef.canAdd());
        assertTrue("No labeledUri update", labeledUriDef.canModify());
        assertTrue("No labeledUri read", labeledUriDef.canRead());
        assertNull("Wrong labeledUri matching rule", labeledUriDef.getMatchingRuleQName());

        ShadowSimpleAttributeDefinition<?> secretaryDef = accountClassDefBare.findSimpleAttributeDefinition("secretary");
        assertNotNull("No definition for secretary", secretaryDef);
        assertEquals(-1, secretaryDef.getMaxOccurs());
        assertEquals(0, secretaryDef.getMinOccurs());
        assertTrue("No secretary create", secretaryDef.canAdd());
        assertTrue("No secretary update", secretaryDef.canModify());
        assertTrue("No secretary read", secretaryDef.canRead());
        assertEquals("Wrong secretary matching rule", PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME, secretaryDef.getMatchingRuleQName());

        ShadowSimpleAttributeDefinition<?> createTimestampDef = accountClassDefBare.findSimpleAttributeDefinition("createTimestamp");
        assertNotNull("No definition for createTimestamp", createTimestampDef);
        assertTimestampType("createTimestamp", createTimestampDef);
        assertEquals(1, createTimestampDef.getMaxOccurs());
        assertEquals(0, createTimestampDef.getMinOccurs());
        assertTrue("No createTimestamp read", createTimestampDef.canRead());
        assertFalse("Bad createTimestamp create", createTimestampDef.canAdd());
        assertFalse("Bad createTimestamp update", createTimestampDef.canModify());
        assertNull("Wrong createTimestamp matching rule", createTimestampDef.getMatchingRuleQName());

        // MID-5210
        ShadowSimpleAttributeDefinition<?> descriptionDef = accountClassDefBare.findSimpleAttributeDefinition(ATTRIBUTE_DESCRIPTION_NAME);
        assertNotNull("No definition for description", descriptionDef);
        assertPolyStringType("description", descriptionDef);
        assertEquals(-1, descriptionDef.getMaxOccurs());
        assertEquals(0, descriptionDef.getMinOccurs());
        assertTrue("No description read", descriptionDef.canRead());
        assertTrue("Bad description create", descriptionDef.canAdd());
        assertTrue("Bad description update", descriptionDef.canModify());
        assertNull("Wrong description matching rule", descriptionDef.getMatchingRuleQName());

        assertNull("The _PASSWORD_ attribute sneaked into schema",
                accountClassDefBare.findSimpleAttributeDefinition(ICFS_PASSWORD));

        assertNull("The userPassword attribute sneaked into schema",
                accountClassDefBare.findSimpleAttributeDefinition(new QName(NS_RI, "userPassword")));

        assertNull("The objectClass attribute sneaked into schema",
                accountClassDefBare.findSimpleAttributeDefinition(new QName(NS_RI, "objectClass")));

        assertNull("The objectclass attribute sneaked into schema",
                accountClassDefBare.findSimpleAttributeDefinition(new QName(NS_RI, "objectclass")));

        ResourceObjectClassDefinition posixAccountDefBare =
                bareSchema.findObjectClassDefinition(RESOURCE_OPENDJ_POSIX_ACCOUNT_OBJECTCLASS);
        assertNotNull("posixAccount definition is missing", posixAccountDefBare);
        assertNotNull("Null identifiers in posixAccount", posixAccountDefBare.getPrimaryIdentifiers());
        assertFalse("Empty identifiers in posixAccount", posixAccountDefBare.getPrimaryIdentifiers().isEmpty());
        assertNotNull("Null secondary identifiers in posixAccount", posixAccountDefBare.getSecondaryIdentifiers());
        assertFalse("Empty secondary identifiers in posixAccount", posixAccountDefBare.getSecondaryIdentifiers().isEmpty());
        assertNotNull("No naming attribute in posixAccount", posixAccountDefBare.getNamingAttribute());
        assertFalse("No nativeObjectClass in posixAccount", StringUtils.isEmpty(posixAccountDefBare.getNativeObjectClassName()));
        assertTrue("posixAccount is not auxiliary", posixAccountDefBare.isAuxiliary());

        ShadowSimpleAttributeDefinition<?> posixIdPrimaryDef =
                posixAccountDefBare.findSimpleAttributeDefinitionRequired(getPrimaryIdentifierQName());
        assertEquals(1, posixIdPrimaryDef.getMaxOccurs());
        assertEquals(0, posixIdPrimaryDef.getMinOccurs());
        assertFalse("UID has create", posixIdPrimaryDef.canAdd());
        assertFalse("UID has update", posixIdPrimaryDef.canModify());
        assertTrue("No UID read", posixIdPrimaryDef.canRead());
        assertTrue("UID definition not in identifiers", accountClassDefBare.getPrimaryIdentifiers().contains(posixIdPrimaryDef));
        assertEquals("Wrong " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " frameworkAttributeName",
                ProvisioningTestUtil.CONNID_UID_NAME, posixIdPrimaryDef.getFrameworkAttributeName());

        ShadowSimpleAttributeDefinition<?> posixIdSecondaryDef =
                posixAccountDefBare.findSimpleAttributeDefinitionRequired(getSecondaryIdentifierQName());
        assertEquals(1, posixIdSecondaryDef.getMaxOccurs());
        assertEquals(1, posixIdSecondaryDef.getMinOccurs());
        assertTrue("No NAME create", posixIdSecondaryDef.canAdd());
        assertTrue("No NAME update", posixIdSecondaryDef.canModify());
        assertTrue("No NAME read", posixIdSecondaryDef.canRead());
        assertTrue("NAME definition not in secondary identifiers",
                accountClassDefBare.getSecondaryIdentifiers().contains(posixIdSecondaryDef));
        assertEquals("Wrong " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME +
                " frameworkAttributeName", ProvisioningTestUtil.CONNID_NAME_NAME, posixIdSecondaryDef.getFrameworkAttributeName());

        ResourceObjectClassDefinition normalDef =
                bareSchema.findObjectClassDefinition(new QName(NS_RI, "normalTestingObjectClass"));
        displayDumpable("normalTestingObjectClass object class def", normalDef);
        assertNotNull("No definition for normalTestingObjectClass", normalDef);
        assertNotNull("The cn attribute missing in normalTestingObjectClass",
                normalDef.findSimpleAttributeDefinition(new QName(normalDef.getTypeName().getNamespaceURI(), "cn")));

        ResourceObjectClassDefinition hybridDef =
                bareSchema.findObjectClassDefinition(new QName(NS_RI, "hybridTestingObjectClass"));
        displayDumpable("Hybrid object class def", hybridDef);
        assertNotNull("No definition for hybridTestingObjectClass", hybridDef);
        assertNotNull("The cn attribute missing in hybridTestingObjectClass",
                hybridDef.findSimpleAttributeDefinition(new QName(hybridDef.getTypeName().getNamespaceURI(), "cn")));
        assertNotNull("The uuidIdentifiedAttribute attribute missing in hybridTestingObjectClass",
                hybridDef.findSimpleAttributeDefinition(new QName(hybridDef.getTypeName().getNamespaceURI(), "uuidIdentifiedAttribute")));

        ResourceObjectClassDefinition uuidDef =
                bareSchema.findObjectClassDefinition(new QName(NS_RI, "uuidIdentifiedObjectClass"));
        displayDumpable("uuidIdentifiedObjectClass object class def", uuidDef);
        assertNotNull("No definition for uuidIdentifiedObjectClass", uuidDef);
        assertNotNull("The uuidIdentifiedAttribute attribute missing in uuidIdentifiedObjectClass",
                uuidDef.findSimpleAttributeDefinition(new QName(uuidDef.getTypeName().getNamespaceURI(), "uuidIdentifiedAttribute")));

        ResourceObjectDefinition accountDef =
                completeSchema.findDefinitionForObjectClassRequired(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        var accountTypeDef = accountDef.getTypeDefinition();
        assertThat(accountTypeDef).as("account type definition").isNotNull();
        assertThat(accountTypeDef.getTypeDefinition().getKind()).as("kind").isEqualTo(ShadowKindType.ACCOUNT);
        assertThat(accountTypeDef.getTypeDefinition().getIntent()).as("intent").isEqualTo("default");

        var groupAssocDef = accountTypeDef.findAssociationDefinitionRequired(RI_GROUP);
        assertThat(groupAssocDef.canRead()).as("group association read").isTrue();
        assertThat(groupAssocDef.canAdd()).as("group association add").isTrue();
        assertThat(groupAssocDef.canModify()).as("group association modify").isTrue();

        assertShadows(1);
    }

    @SuppressWarnings("SameParameterValue")
    protected void assertTimestampType(String attrName, ShadowSimpleAttributeDefinition<?> def) {
        assertEquals("Wrong " + attrName + " type", DOMUtil.XSD_DATETIME, def.getTypeName());
    }

    @SuppressWarnings("SameParameterValue")
    private void assertPolyStringType(String attrName, ShadowSimpleAttributeDefinition<?> def) {
        assertEquals("Wrong " + attrName + " type", PolyStringType.COMPLEX_TYPE, def.getTypeName());
    }

    @Test
    public void test007RefinedSchema() throws Exception {
        when();
        var completeSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resourceBean);
        displayDumpable("Refined schema", completeSchema);

        assertCompleteSchemaCached(completeSchema, ResourceSchemaFactory.getCompleteSchema(resourceBean));

        ResourceObjectDefinition accountDef = completeSchema.findDefaultDefinitionForKindRequired(ShadowKindType.ACCOUNT);
        assertNotNull("Account definition is missing", accountDef);
        assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
        assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
        assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
        assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
        assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
        assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(
                accountDef.getObjectClassDefinition().getNativeObjectClassName()));

        ShadowSimpleAttributeDefinition<?> idPrimaryDef = accountDef.findSimpleAttributeDefinitionRequired(getPrimaryIdentifierQName());
        assertEquals(1, idPrimaryDef.getMaxOccurs());
        assertEquals(0, idPrimaryDef.getMinOccurs());
        assertFalse("UID has create", idPrimaryDef.canAdd());
        assertFalse("UID has update", idPrimaryDef.canModify());
        assertTrue("No UID read", idPrimaryDef.canRead());
        assertTrue("UID definition not in identifiers", accountDef.getPrimaryIdentifiers().contains(idPrimaryDef));
        assertEquals("Wrong " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " frameworkAttributeName", ProvisioningTestUtil.CONNID_UID_NAME, idPrimaryDef.getFrameworkAttributeName());

        ShadowSimpleAttributeDefinition<?> idSecondaryDef = accountDef.findSimpleAttributeDefinitionRequired(getSecondaryIdentifierQName());
        assertEquals(1, idSecondaryDef.getMaxOccurs());
        assertEquals(1, idSecondaryDef.getMinOccurs());
        assertTrue("No NAME create", idSecondaryDef.canAdd());
        assertTrue("No NAME update", idSecondaryDef.canModify());
        assertTrue("No NAME read", idSecondaryDef.canRead());
        assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(idSecondaryDef));
        assertEquals("Wrong NAME matching rule", PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME, idSecondaryDef.getMatchingRuleQName());
        assertEquals("Wrong " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " frameworkAttributeName", ProvisioningTestUtil.CONNID_NAME_NAME, idSecondaryDef.getFrameworkAttributeName());

        ShadowSimpleAttributeDefinition<?> cnDef = accountDef.findSimpleAttributeDefinition("cn");
        assertNotNull("No definition for cn", cnDef);
        assertEquals(-1, cnDef.getMaxOccurs());
        assertEquals(1, cnDef.getMinOccurs());
        assertTrue("No cn create", cnDef.canAdd());
        assertTrue("No cn update", cnDef.canModify());
        assertTrue("No cn read", cnDef.canRead());

        assertMemberOfAttributeRefined(accountDef);

        ShadowSimpleAttributeDefinition<?> secretaryDef = accountDef.findSimpleAttributeDefinition("secretary");
        assertNotNull("No definition for secretary", secretaryDef);
        assertEquals(-1, secretaryDef.getMaxOccurs());
        assertEquals(0, secretaryDef.getMinOccurs());
        assertTrue("No secretary create", secretaryDef.canAdd());
        assertTrue("No secretary update", secretaryDef.canModify());
        assertTrue("No secretary read", secretaryDef.canRead());
        assertEquals("Wrong secretary matching rule", PrismConstants.XML_MATCHING_RULE_NAME, secretaryDef.getMatchingRuleQName());

        ShadowSimpleAttributeDefinition<?> dsDef = accountDef.findSimpleAttributeDefinition("ds-pwp-account-disabled");
        assertNotNull("No definition for cn", dsDef);
        assertEquals(1, dsDef.getMaxOccurs());
        assertEquals(0, dsDef.getMinOccurs());
        assertTrue("No ds-pwp-account-disabled create", dsDef.canAdd());
        assertTrue("No ds-pwp-account-disabled update", dsDef.canModify());
        assertTrue("No ds-pwp-account-disabled read", dsDef.canRead());
        // TODO: MID-2358
//        assertTrue("ds-pwp-account-disabled is NOT operational", dsDef.isOperational());
        assertTrue("ds-pwp-account-disabled is NOT ignored", dsDef.isIgnored());

        assertNull("The _PASSWORD_ attribute sneaked into schema",
                accountDef.findSimpleAttributeDefinition(ICFS_PASSWORD));

        ResourceObjectClassDefinition posixAccountObjectClassDef =
                completeSchema.findObjectClassDefinition(RESOURCE_OPENDJ_POSIX_ACCOUNT_OBJECTCLASS);
        assertNotNull("posixAccount definition is missing", posixAccountObjectClassDef);
        assertNotNull("Null identifiers in posixAccount", posixAccountObjectClassDef.getPrimaryIdentifiers());
        assertFalse("Empty identifiers in posixAccount", posixAccountObjectClassDef.getPrimaryIdentifiers().isEmpty());
        assertNotNull("Null secondary identifiers in posixAccount", posixAccountObjectClassDef.getSecondaryIdentifiers());
        assertFalse("Empty secondary identifiers in posixAccount", posixAccountObjectClassDef.getSecondaryIdentifiers().isEmpty());
        assertNotNull("No naming attribute in posixAccount", posixAccountObjectClassDef.getNamingAttribute());
        assertFalse("No nativeObjectClass in posixAccount",
                StringUtils.isEmpty(posixAccountObjectClassDef.getObjectClassDefinition().getNativeObjectClassName()));
        assertTrue("posixAccount is not auxiliary", posixAccountObjectClassDef.getObjectClassDefinition().isAuxiliary());

        ShadowSimpleAttributeDefinition<?> posixIdPrimaryDef =
                posixAccountObjectClassDef.findSimpleAttributeDefinitionRequired(getPrimaryIdentifierQName());
        assertEquals(1, posixIdPrimaryDef.getMaxOccurs());
        assertEquals(0, posixIdPrimaryDef.getMinOccurs());
        assertFalse("UID has create", posixIdPrimaryDef.canAdd());
        assertFalse("UID has update", posixIdPrimaryDef.canModify());
        assertTrue("No UID read", posixIdPrimaryDef.canRead());
//        assertTrue("UID definition not in identifiers",
//                removeRefinedParts(accountDef.getPrimaryIdentifiers())
//                        .contains(posixIdPrimaryDef));
        assertEquals("Wrong " + OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME + " frameworkAttributeName",
                ProvisioningTestUtil.CONNID_UID_NAME, posixIdPrimaryDef.getFrameworkAttributeName());

        ShadowSimpleAttributeDefinition<?> posixIdSecondaryDef =
                posixAccountObjectClassDef.findSimpleAttributeDefinitionRequired(
                        getSecondaryIdentifierQName());
        assertEquals(1, posixIdSecondaryDef.getMaxOccurs());
        assertEquals(1, posixIdSecondaryDef.getMinOccurs());
        assertTrue("No NAME create", posixIdSecondaryDef.canAdd());
        assertTrue("No NAME update", posixIdSecondaryDef.canModify());
        assertTrue("No NAME read", posixIdSecondaryDef.canRead());
//        assertTrue("NAME definition not in secondary identifiers",
//                removeRefinedParts(accountDef.getSecondaryIdentifiers())
//                        .contains(posixIdSecondaryDef));
        assertEquals("Wrong " + OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME + " frameworkAttributeName",
                ProvisioningTestUtil.CONNID_NAME_NAME, posixIdSecondaryDef.getFrameworkAttributeName());

        assertShadows(1);
    }

//    /**
//     * Removes the "refined" part (customization bean) from each resource attribute definition.
//     * Used to compare definitions from resource object type vs. resource object class.
//     */
//    private Collection<? extends ResourceAttributeDefinition<?>> removeRefinedParts(
//            Collection<? extends ResourceAttributeDefinition<?>> definitions) {
//        return definitions.stream()
//                .map(def ->
//                        ResourceAttributeDefinitionImpl.create(
//                                def.getNativeDefinition()))
//                .collect(Collectors.toList());
//    }

    @Test
    public void test110GetObject() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType objectToAdd = parseObjectType(ACCOUNT_JBOND_FILE, ShadowType.class);

        display(SchemaDebugUtil.prettyPrint(objectToAdd));
        display(objectToAdd.asPrismObject().debugDump());

        String addedObjectOid =
                provisioningService.addObject(objectToAdd.asPrismObject(), null, null, task, result);
        assertEquals(ACCOUNT_JBOND_OID, addedObjectOid);

        when();
        ShadowType provisioningShadow =
                provisioningService
                        .getObject(ShadowType.class, ACCOUNT_JBOND_OID, null, task, result)
                        .asObjectable();

        then();
        assertSuccess(result);

        and("provisioning-level shadow is OK");
        ShadowAsserter.forAbstractShadow(provisioningShadow)
                .display()
                .assertOrigValues(QNAME_DN, "uid=jbond,ou=People,dc=example,dc=com")
                .assertNormValues(QNAME_DN, "uid=jbond,ou=people,dc=example,dc=com")
                .assertOrigValues(QNAME_UID, "jbond")
                .assertOrigValues(QNAME_CN, "James Bond")
                .assertOrigValues(QNAME_SN, "Bond")
                .assertOid(ACCOUNT_JBOND_OID)
                .assertName("uid=jbond,ou=People,dc=example,dc=com")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertObjectClass(OBJECT_CLASS_INETORGPERSON_QNAME)
                .assertResource(RESOURCE_OPENDJ_OID)
                .assertHasPrimaryIdentifierAttribute()
                .assertHasIndexedPrimaryIdentifierValue();

        // TODO create assertions for these eventually
        assertNotNull("Missing activation", provisioningShadow.getActivation());
        assertNotNull("Missing activation status", provisioningShadow.getActivation().getAdministrativeStatus());
        assertEquals("Not enabled", ActivationStatusType.ENABLED, provisioningShadow.getActivation().getAdministrativeStatus());
        assertShadowPassword(provisioningShadow);
        Object createTimestamp = ShadowUtil.getAttributeValue(provisioningShadow, new QName(NS_RI, "createTimestamp"));
        assertTimestamp("createTimestamp", createTimestamp);

        and("repo shadow is OK");
        assertRepoShadowNew(provisioningShadow.getOid())
                .display()
                .assertCachedOrigValues(QNAME_DN, "uid=jbond,ou=People,dc=example,dc=com")
                .assertCachedNormValues(QNAME_DN, "uid=jbond,ou=people,dc=example,dc=com")
                .assertCachedOrigValues(QNAME_UID, "jbond")
                .assertCachedOrigValues(QNAME_CN, "James Bond")
                .assertCachedOrigValues(QNAME_SN, "Bond")
                .assertOid(ACCOUNT_JBOND_OID)
                .assertName("uid=jbond,ou=People,dc=example,dc=com")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertObjectClass(OBJECT_CLASS_INETORGPERSON_QNAME)
                .assertResource(RESOURCE_OPENDJ_OID)
                .assertHasIndexedPrimaryIdentifierValue();

        and("search by DN norm value works");

        var query = Resource.of(resourceBean)
                .queryFor(ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT)
                .and().item(
                        PATH_DN,
                        getAccountAttributeDefinitionRequired(QNAME_DN).toNormalizationAware())
                .eq(DistinguishedNameNormalizer.instance().poly("uid=jbond,ou=People,dc=example,dc=com"))
                .matchingNorm()
                .build();
        var objects = repositoryService.searchObjects(ShadowType.class, query, null, result);
        assertThat(objects)
                .as("objects found")
                .hasSize(1)
                .element(0)
                .extracting(PrismObject::getOid)
                .as("OID")
                .isEqualTo(ACCOUNT_JBOND_OID);

        and("Number of shadows is OK");
        assertShadows(2 + getNumberOfBaseContextShadows());
    }

    @SuppressWarnings("SameParameterValue")
    protected void assertTimestamp(String attrName, Object timestampValue) {
        if (!(timestampValue instanceof XMLGregorianCalendar)) {
            fail("Wrong type of " + attrName + ", expected XMLGregorianCalendar but was " + timestampValue.getClass());
        }
        assertBetween("Unreasonable date in " + attrName,
                XmlTypeConverter.createXMLGregorianCalendar(1900, 1, 1, 0, 0, 0),
                XmlTypeConverter.createXMLGregorianCalendar(2200, 1, 1, 0, 0, 0),
                (XMLGregorianCalendar) timestampValue);
    }

    protected void assertShadowPassword(ShadowType provisioningShadow) throws Exception {
        CredentialsType credentials = provisioningShadow.getCredentials();
        if (credentials == null) {
            return;
        }
        PasswordType passwordType = credentials.getPassword();
        if (passwordType == null) {
            return;
        }
        ProtectedStringType passwordValue = passwordType.getValue();
        assertNull("Unexpected password value in " + provisioningShadow + ": " + passwordValue, passwordValue);
    }

    /**
     * Let's try to fetch object that does not exist in the repository.
     */
    @Test
    public void test111GetObjectNotFoundRepo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            provisioningService.getObject(ObjectType.class, NON_EXISTENT_OID, null, task, result);
            Assert.fail("Expected exception, but haven't got one");
        } catch (ObjectNotFoundException e) {
            displayExpectedException(e);

            // Just to close the top-level result.
            result.recordFatalError("Error :-)");

            System.out.println("NOT FOUND REPO result:");
            System.out.println(result.debugDump());

            assertFalse(result.hasUnknownStatus());
            // TODO: check result
        } catch (CommunicationException | SchemaException e) {
            Assert.fail("Expected ObjectNotFoundException, but got" + e);
        }

        assertShadows(2 + getNumberOfBaseContextShadows());
    }

    /**
     * Let's try to fetch object that does exit in the repository but does not
     * exist in the resource.
     */
    @Test
    public void test112GetObjectNotFoundResource() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        var shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_BAD_OID, null, task, result);

        then();
        ShadowAsserter.forShadow(shadow, "provisioning")
                .display()
                .assertTombstone();
    }

    @Test
    public void test119Cleanup() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
        repositoryService.deleteObject(ShadowType.class, ACCOUNT_JBOND_OID, result);

        assertShadows(getNumberOfBaseContextShadows());
    }

    @Test
    public void test120AddAccountWill() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> accountBefore = parseObject(ACCOUNT_WILL_FILE);

        display("Account before", accountBefore);

        when();
        String addedObjectOid = provisioningService.addObject(accountBefore, null, null, task, result);

        then();
        assertSuccess(result);

        assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

        assertRepoShadowNew(ACCOUNT_WILL_OID)
                .display()
                .assertName("uid=will,ou=People,dc=example,dc=com")
                .assertCachedOrigValues(OpenDJController.RESOURCE_OPENDJ_DN, ACCOUNT_WILL_DN);
                //.assertCachedNormValues(OpenDJController.RESOURCE_OPENDJ_DN, StringUtils.lowerCase(ACCOUNT_WILL_DN)); //not yet

        ShadowType accountAfter = provisioningService
                .getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result)
                .asObjectable();
        ShadowAsserter.forAbstractShadow(accountAfter)
                .assertName("uid=will,ou=People,dc=example,dc=com");

        assertShadows(1 + getNumberOfBaseContextShadows());
    }

    @Test
    public void test121RenameAccountWillOnResource() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

//        result.tracingProfile(
//                tracer.compileProfile(
//                        addRepositoryAndSqlLogging(
//                                createModelAndProvisioningLoggingTracingProfile()),
//                        result));

        given("account is renamed on the resource");
        openDJController.executeRenameChange(new File(TEST_DIR, "rename.ldif").getPath());

        openDJController.fetchEntryRequired("uid=will123,ou=People,dc=example,dc=com");

        then("old name is still present in the repo");
        assertRepoShadowNew(ACCOUNT_WILL_OID)
                .assertName(ACCOUNT_WILL_DN)
                .assertCachedOrigValues(QNAME_DN, ACCOUNT_WILL_DN);

        when("account is fetched via provisioning");
        ShadowType provisioningAccount = provisioningService
                .getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result)
                .asObjectable();

        then("new name is returned");
        assertShadowNew(provisioningAccount)
                .assertName("uid=will123,ou=People,dc=example,dc=com")
                .assertOrigValues(QNAME_DN, "uid=will123,ou=People,dc=example,dc=com");

        and("the name is updated in the repo");
        assertRepoShadowNew(ACCOUNT_WILL_OID)
                .assertName("uid=will123,ou=People,dc=example,dc=com")
                .assertCachedOrigValues(QNAME_DN, "uid=will123,ou=People,dc=example,dc=com");

        assertShadows(1 + getNumberOfBaseContextShadows());
    }

    @Test
    public void test125AddObjectNull() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            //noinspection DataFlowIssue
            provisioningService.addObject(null, null, null, task, result);
        } catch (NullPointerException e) {
            displayExpectedException(e);
            assertThat(e.getMessage()).as("exception message").isEqualTo("Object to add must not be null.");
        } catch (IllegalArgumentException e) {
            // When running with @NotNull parameters checked
            displayExpectedException(e);
            assertThat(e.getMessage()).as("exception message").contains("Argument for @NotNull parameter 'object'");
        }
    }

    @Test
    public void test130AddDeleteAccountSparrow() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("account is added");
        ShadowType object = parseObjectType(ACCOUNT_SPARROW_FILE, ShadowType.class);

        System.out.println(SchemaDebugUtil.prettyPrint(object));
        System.out.println(object.asPrismObject().debugDump());

        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
        assertEquals(ACCOUNT_SPARROW_OID, addedObjectOid);

        and("it is deleted");
        provisioningService.deleteObject(ShadowType.class, ACCOUNT_SPARROW_OID, null, null, task, result);

        then("the 'provisioning get' should fail with an exception");
        try {
            provisioningService.getObject(ShadowType.class, ACCOUNT_SPARROW_OID, null, task, result);
            Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
        } catch (ObjectNotFoundException ex) {
            displayExpectedException(ex);
        }

        and("the 'repo get' should fail with an exception");
        try {
            getShadowRepoLegacy(ACCOUNT_SPARROW_OID);
            Assert.fail("Expected exception, but haven't got one.");
        } catch (ObjectNotFoundException ex) {
            displayExpectedException(ex);
            assertTrue(ex.getMessage().contains(ACCOUNT_SPARROW_OID));
        }

        // Account shadow + shadow for base context for groups (but only for simulated references)
        assertShadows(hasNativeReferences() ? 1 : 2);
    }

    @Test
    public void test140AddAndModifyAccountJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = unmarshalValueFromFile(ACCOUNT_JACK_FILE);

        System.out.println(SchemaDebugUtil.prettyPrint(object));
        System.out.println(object.asPrismObject().debugDump());

        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
        assertEquals(ACCOUNT_JACK_OID, addedObjectOid);

        ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(ACCOUNT_JACK_CHANGE_FILE, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, object.asPrismObject().getDefinition());

        ItemPath icfNamePath = ItemPath.create(ShadowType.F_ATTRIBUTES, getSecondaryIdentifierQName());
        PrismPropertyDefinition<?> icfNameDef = object.asPrismObject().getDefinition().findPropertyDefinition(icfNamePath);
        ItemDelta<?, ?> renameDelta = prismContext.deltaFactory().property()
                .createModificationReplaceProperty(icfNamePath, icfNameDef, "uid=rename,ou=People,dc=example,dc=com");
        //noinspection unchecked,rawtypes
        ((Collection) delta.getModifications()).add(renameDelta);

        displayDumpable("Object change", delta);

        when();
        provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
                delta.getModifications(), null, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        ShadowType provisioningAccount = provisioningService
                .getObject(ShadowType.class, ACCOUNT_JACK_OID, null, task, result)
                .asObjectable();

        display("Object after change", provisioningAccount);

        String uid = assertShadowNew(provisioningAccount)
                .assertOrigValues(QNAME_DN, "uid=rename,ou=People,dc=example,dc=com")
                .assertOrigValues(QNAME_SN, "First")
                .getOrigValue(QNAME_ENTRY_UUID);

        assertRepoShadowNew(objectChange.getOid())
                .assertCachedOrigValues(QNAME_DN, "uid=rename,ou=People,dc=example,dc=com")
                .assertCachedNormValues(QNAME_DN, "uid=rename,ou=people,dc=example,dc=com")
                .assertName("uid=rename,ou=People,dc=example,dc=com");

        assertNotNull(uid);

        // Check if object was modified in LDAP

        Entry response = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account", response);

        OpenDJController.assertAttribute(response, "sn", "First");

        assertShadows(hasNativeReferences() ? 2 : 3);
    }

    @Test
    public void test145ModifyAccountJackJpegPhoto() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        byte[] bytesIn = Files.readAllBytes(Paths.get(ProvisioningTestUtil.DOT_JPG_FILENAME));
        displayValue("Bytes in", MiscUtil.bytesToHex(bytesIn));

        ItemName jpegPhotoQName = new ItemName(NS_RI, "jpegPhoto");
        PropertyDelta<byte[]> jpegPhotoDelta =
                prismContext.deltaFactory().property()
                        .create(ItemPath.create(ShadowType.F_ATTRIBUTES, jpegPhotoQName), null);
        jpegPhotoDelta.setRealValuesToReplace(bytesIn);

        Collection<? extends ItemDelta<?, ?>> modifications = MiscSchemaUtil.createCollection(jpegPhotoDelta);

        display("Modifications", modifications);

        when();
        provisioningService.modifyObject(
                ShadowType.class, ACCOUNT_JACK_OID, modifications, null, null, task, result);

        then();
        assertSuccess(result);

        Entry entry = openDJController.searchByUid("rename");
        display("LDAP Entry", entry);
        byte[] jpegPhotoLdap = OpenDJController.getAttributeValueBinary(entry, "jpegPhoto");
        assertNotNull("No jpegPhoto in LDAP entry", jpegPhotoLdap);
        assertEquals("Byte length changed (LDAP)", bytesIn.length, jpegPhotoLdap.length);
        assertArrayEquals("Bytes do not match (LDAP)", bytesIn, jpegPhotoLdap);

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_JACK_OID, null, task, result);

        display("Object after change", shadow);

        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<byte[]> jpegPhotoAttr = attributesContainer.findProperty(jpegPhotoQName);
        byte[] bytesOut = jpegPhotoAttr.getValues().get(0).getValue();

        displayValue("Bytes out", MiscUtil.bytesToHex(bytesOut));

        assertEquals("Byte length changed (shadow)", bytesIn.length, bytesOut.length);
        assertArrayEquals("Bytes do not match (shadow)", bytesIn, bytesOut);

        assertShadows(hasNativeReferences() ? 2 : 3);
    }

    /**
     * Make a duplicate modification. Add a givenName value that is already there.
     * Normal LDAP should fail. So check that connector and midPoint handles that.
     */
    @Test
    public void test147ModifyAccountJackGivenNameDuplicate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PropertyDelta<String> givenNameDelta = prismContext.deltaFactory().property().create(
                ItemPath.create(ShadowType.F_ATTRIBUTES, new QName(NS_RI, "givenName")),
                null);
        givenNameDelta.addRealValuesToAdd("Jack");

        // Also make an ordinary non-conflicting modification. We need to make sure that
        // the operation was not ignored as a whole
        PropertyDelta<String> titleDelta = prismContext.deltaFactory().property().create(
                ItemPath.create(ShadowType.F_ATTRIBUTES, new QName(NS_RI, "title")),
                null);
        titleDelta.addRealValuesToAdd("Great Captain");

        Collection<? extends ItemDelta<?, ?>> modifications = MiscSchemaUtil.createCollection(givenNameDelta, titleDelta);

        display("Modifications", modifications);

        when();
        provisioningService.modifyObject(
                ShadowType.class, ACCOUNT_JACK_OID, modifications, null, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = openDJController.searchByUid("rename");
        display("LDAP Entry", entry);
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "title", "Great Captain");

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_JACK_OID, null, task, result);

        display("Object after change", shadow);

        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        PrismAsserts.assertPropertyValue(attributesContainer, new ItemName(NS_RI, "givenName"), "Jack");
        PrismAsserts.assertPropertyValue(attributesContainer, new ItemName(NS_RI, "title"), "Great Captain");

        assertShadows(hasNativeReferences() ? 2 : 3);
    }

    @Test
    public void test150ChangePassword() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(ACCOUNT_MODIFY_PASSWORD_FILE, ShadowType.class);

        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);

        assertEquals(ACCOUNT_MODIFY_PASSWORD_OID, addedObjectOid);

        ShadowType accountType = provisioningService
                .getObject(ShadowType.class, ACCOUNT_MODIFY_PASSWORD_OID, null, task, result)
                .asObjectable();

        display("Object before password change", accountType);

        String uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());
        assertNotNull(uid);

        Entry entryBefore = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account before", entryBefore);

        String passwordBefore = OpenDJController.getAttributeValue(entryBefore, "userPassword");
        assertNull("Unexpected password before change", passwordBefore);

        ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "account-change-password.xml"), ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, accountType.asPrismObject().getDefinition());
        displayDumpable("Object change", delta);

        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(), null, null, task, result);

        then();

        // Check if object was modified in LDAP
        Entry entryAfter = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account after", entryAfter);

        String passwordAfter = OpenDJController.getAttributeValue(entryAfter, "userPassword");
        assertNotNull("The password was not changed", passwordAfter);

        System.out.println("Changed password: " + passwordAfter);

        openDJController.assertPassword(entryAfter.getDN().toString(), "mehAbigH4X0R");

        assertShadows(hasNativeReferences() ? 3 : 4);
    }

    @Test
    public void test151AddObjectWithPassword() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(ACCOUNT_NEW_WITH_PASSWORD_FILE, ShadowType.class);

        System.out.println(SchemaDebugUtil.prettyPrint(object));
        System.out.println(object.asPrismObject().debugDump());

        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
        assertEquals(ACCOUNT_NEW_WITH_PASSWORD_OID, addedObjectOid);

        assertRepoShadowNew(ACCOUNT_NEW_WITH_PASSWORD_OID)
                .assertName("uid=lechuck,ou=People,dc=example,dc=com");

        String uid = assertProvisioningShadowNew(ACCOUNT_NEW_WITH_PASSWORD_OID)
                .assertName("uid=lechuck,ou=People,dc=example,dc=com")
                .getOrigValue(QNAME_ENTRY_UUID);

        assertNotNull(uid);

        // Check if object was created in LDAP and that there is a password

        Entry entryAfter = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account after", entryAfter);

        String passwordAfter = OpenDJController.getAttributeValue(entryAfter, "userPassword");
        assertNotNull("The password was not changed", passwordAfter);

        System.out.println("Account password: " + passwordAfter);

        openDJController.assertPassword(entryAfter.getDN().toString(), "t4k30v3rTh3W0rld");

        assertShadows(hasNativeReferences() ? 4 : 5);
    }

    @Test
    public void test160SearchAccountsIterative() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), OBJECT_CLASS_INETORGPERSON_QNAME);

        final Collection<ObjectType> objects = new HashSet<>();

        ResultHandler<ShadowType> handler = (prismObject, lResult) -> {
            ShadowType shadow = prismObject.asObjectable();
            objects.add(shadow);

            display("Found object", shadow);

            var aShadow = AbstractShadow.of(shadow);

            assertNotNull(shadow.getOid());
            assertNotNull(shadow.getName());
            assertEquals(OBJECT_CLASS_INETORGPERSON_QNAME, shadow.getObjectClass());
            assertEquals(RESOURCE_OPENDJ_OID, shadow.getResourceRef().getOid());
            String idPrimaryVal = getAttributeValue(shadow, getPrimaryIdentifierQName());
            assertNotNull("No primary identifier (" + getPrimaryIdentifierQName().getLocalPart() + ")", idPrimaryVal);
            String idSecondaryVal = getAttributeValue(shadow, getSecondaryIdentifierQName());
            assertNotNull("No secondary (" + getSecondaryIdentifierQName().getLocalPart() + ")", idSecondaryVal);
            assertEquals("Wrong shadow name", idSecondaryVal.toLowerCase(), shadow.getName().getOrig().toLowerCase());
            var uid = aShadow.getAttributeRealValue(QNAME_UID);
            assertNotNull("Missing LDAP uid", uid);
            assertNotNull("Missing LDAP cn", aShadow.getAttributeRealValue(QNAME_CN));
            assertNotNull("Missing LDAP sn", aShadow.getAttributeRealValue(QNAME_SN));
            assertNotNull("Missing activation", shadow.getActivation());
            assertNotNull("Missing activation status", shadow.getActivation().getAdministrativeStatus());
            assertEquals("Not enabled", ActivationStatusType.ENABLED, shadow.getActivation().getAdministrativeStatus());

            if ("jgibbs".equals(uid)) {
                assertShadow(shadow, "jgibbs")
                        .associations()
                        .assertValuesCount(1);
            }
            return true;
        };

        when();
        SearchResultMetadata searchMetadata =
                provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, task, result);

        then();
        displayValue("Count", objects.size());
        assertEquals("Unexpected number of shadows", 9, objects.size());

        // The extra shadow is a group shadow
        assertShadows(hasNativeReferences() ? 10 : 11);

        // Bad things may happen, so let's check if the shadow is still there and that is has the same OID
        provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

        // No paging. Which means SPR search. We do not have result number estimate.
        assertApproxNumberOfAllResults(searchMetadata, null);
    }

    @Test
    public void test161SearchAccountsIterativeOffset2Page3() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QName objectClass = new QName(NS_RI, OBJECT_CLASS_INETORGPERSON_NAME);

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), objectClass);
        ObjectPaging paging = prismContext.queryFactory().createPaging(2, 3);
        query.setPaging(paging);

        final Collection<ObjectType> objects = new HashSet<>();

        ResultHandler<ShadowType> handler = (prismObject, lResult) -> {
            ShadowType shadow = prismObject.asObjectable();
            objects.add(shadow);
            display("Found object", shadow);
            return true;
        };

        when();
        SearchResultMetadata searchMetadata =
                provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, task, result);

        then();
        displayValue("Count", objects.size());
        assertEquals("Unexpected number of shadows", 3, objects.size());

        // The extra shadow is a group shadow
        assertShadows(hasNativeReferences() ? 10 : 11);

        // Bad things may happen, so let's check if the shadow is still there and that is has the same OID
        provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

        // VLV search if forced by using offset. So we have result number estimate.
        assertApproxNumberOfAllResults(searchMetadata, 9);
    }

    @Test
    public void test162SearchAccountsIterativeOffsetNullPage5() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QName objectClass = new QName(NS_RI, OBJECT_CLASS_INETORGPERSON_NAME);

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), objectClass);
        ObjectPaging paging = prismContext.queryFactory().createPaging(null, 3);
        query.setPaging(paging);

        final Collection<ObjectType> objects = new HashSet<>();

        ResultHandler<ShadowType> handler = (prismObject, lResult) -> {
            ShadowType shadow = prismObject.asObjectable();
            objects.add(shadow);
            display("Found object", shadow);
            return true;
        };

        when();
        SearchResultMetadata searchMetadata =
                provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, task, result);

        then();
        displayValue("Count", objects.size());
        assertEquals("Unexpected number of shadows", 3, objects.size());

        // The extra shadow is a group shadow
        assertShadows(hasNativeReferences() ? 10 : 11);

        // Bad things may happen, so let's check if the shadow is still there and that is has the same OID
        provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

        // No offset. Which means SPR search. We do not have result number estimate.
        assertApproxNumberOfAllResults(searchMetadata, null);
    }

    @Override
    protected void assertShadows(int expectedCount) throws SchemaException {
        OperationResult result = new OperationResult(TestOpenDj.class.getName() + ".assertShadows");
        int actualCount = repositoryService.countObjects(ShadowType.class, null, null, result);
        if (actualCount != expectedCount) {
            ResultHandler<ShadowType> handler = (object, parentResult) -> {
                display("Repo shadow", object);
                return true;
            };
            repositoryService.searchObjectsIterative(
                    ShadowType.class, null, handler, null, true, result);
            assertEquals("Unexpected number of shadows in the repo", expectedCount, actualCount);
        }
    }

    @Test
    public void test170DisableAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(ACCOUNT_DISABLE_SIMULATED_FILE, ShadowType.class);

        System.out.println(SchemaDebugUtil.prettyPrint(object));
        System.out.println(object.asPrismObject().debugDump());

        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
        assertEquals(ACCOUNT_DISABLE_SIMULATED_OID, addedObjectOid);

        ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(
                REQUEST_DISABLE_ACCOUNT_SIMULATED_FILE, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, object.asPrismObject().getDefinition());
        displayDumpable("Object change", delta);

        when();
        provisioningService.modifyObject(
                ShadowType.class,
                objectChange.getOid(),
                delta.getModifications(),
                null,
                null,
                task,
                result);

        then();
        ShadowType accountAfter =
                provisioningService
                        .getObject(ShadowType.class, ACCOUNT_DISABLE_SIMULATED_OID, null, task, result)
                        .asObjectable();

        display("Object after change", accountAfter);

        assertEquals("The account was not disabled in the shadow",
                ActivationStatusType.DISABLED, accountAfter.getActivation().getAdministrativeStatus());

        String uid = ShadowUtil.getSingleStringAttributeValue(accountAfter, getPrimaryIdentifierQName());
        assertNotNull(uid);

        // Check if object was modified in LDAP

        Entry response = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account", response);

        String disabled = OpenDJController.getAttributeValue(response, "ds-pwp-account-disabled");
        assertNotNull("no ds-pwp-account-disabled attribute in account " + uid, disabled);

        display("ds-pwp-account-disabled after change: " + disabled);

        assertEquals("ds-pwp-account-disabled not set to \"TRUE\"", "TRUE", disabled);

        PrismObject<ShadowType> repoShadow = getShadowRepoLegacy(ACCOUNT_DISABLE_SIMULATED_OID);
        ActivationType repoActivation = repoShadow.asObjectable().getActivation();
        assertNotNull("No activation in repo", repoActivation);
        XMLGregorianCalendar repoDisableTimestamp = repoActivation.getDisableTimestamp();
        assertNotNull("No activation disableTimestamp in repo", repoDisableTimestamp);
        assertEquals("Wrong activation disableTimestamp in repo",
                XmlTypeConverter.createXMLGregorianCalendar(2001, 2, 3, 4, 5, 6),
                repoDisableTimestamp);
    }

    @Test
    public void test175AddDisabledAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(ACCOUNT_NEW_DISABLED_FILE, ShadowType.class);

        IntegrationTestTools.display("Adding object", object);

        when();
        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);

        then();
        assertEquals(ACCOUNT_NEW_DISABLED_OID, addedObjectOid);

        var repoShadow = assertRepoShadowNew(ACCOUNT_NEW_DISABLED_OID)
                .assertName("uid=rapp,ou=People,dc=example,dc=com")
                .assertHasIndexedPrimaryIdentifierValue()
                .getObjectable();
        String uid = repoShadow.getPrimaryIdentifierValue();

        assertRepoShadowNew(ACCOUNT_NEW_DISABLED_OID)
                .assertName("uid=rapp,ou=People,dc=example,dc=com");

        // Check if object was modified in LDAP

        Entry response = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account", response);

        String disabled = OpenDJController.getAttributeValue(response, "ds-pwp-account-disabled");
        assertNotNull("no ds-pwp-account-disabled attribute in account " + uid, disabled);

        System.out.println("ds-pwp-account-disabled after change: " + disabled);

        assertEquals("ds-pwp-account-disabled not set to \"TRUE\"", "TRUE", disabled);

        ActivationType repoActivation = repoShadow.getActivation();
        assertNotNull("No activation in repo", repoActivation);
        XMLGregorianCalendar repoDisableTimestamp = repoActivation.getDisableTimestamp();
        assertNotNull("No activation disableTimestamp in repo", repoDisableTimestamp);
        assertEquals("Wrong activation disableTimestamp in repo",
                XmlTypeConverter.createXMLGregorianCalendar(1999, 8, 7, 6, 5, 4), repoDisableTimestamp);
    }

    /**
     * Adding account with EXPLICIT enable. This triggers simulated activation in a different way.
     */
    @Test
    public void test176AddEnabledAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(ACCOUNT_NEW_ENABLED_FILE, ShadowType.class);

        IntegrationTestTools.display("Adding object", object);

        when();
        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);

        then();
        assertEquals(ACCOUNT_NEW_ENABLED_OID, addedObjectOid);

        var repoShadow = assertRepoShadowNew(ACCOUNT_NEW_ENABLED_OID)
                .assertName("uid=cook,ou=People,dc=example,dc=com")
                .assertHasIndexedPrimaryIdentifierValue()
                .getObjectable();
        String uid = repoShadow.getPrimaryIdentifierValue();

        assertRepoShadowNew(ACCOUNT_NEW_ENABLED_OID)
                .assertName("uid=cook,ou=People,dc=example,dc=com");

        // Check if object was modified in LDAP

        Entry response = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account", response);

        String disabled = OpenDJController.getAttributeValue(response, "ds-pwp-account-disabled");
        assertEquals("ds-pwp-account-disabled not set to \"FALSE\"", "FALSE", disabled);
    }

    @Test
    public void test180GetUnlockedAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
                null, task, result);

        then();
        result.computeStatus();
        assertSuccess(result);

        PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS,
                LockoutStatusType.NORMAL);
    }

    @Test
    public void test182GetLockedAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.executeLdifChange(
                """
                        dn: uid=will123,ou=People,dc=example,dc=com
                        changetype: modify
                        replace: pager
                        pager: 1"""
        );

        when();
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
                null, task, result);

        then();
        result.computeStatus();
        assertSuccess(result);

        PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS,
                LockoutStatusType.LOCKED);
    }

    @Test
    public void test184UnlockAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, LockoutStatusType.NORMAL);

        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(),
                delta.getModifications(), null, null, task, result);

        then();
        result.computeStatus();
        assertSuccess(result);

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

        display("Object after change", shadow);

        String uid = ShadowUtil.getSingleStringAttributeValue(shadow.asObjectable(), getPrimaryIdentifierQName());
        assertNotNull(uid);

        // Check if object was modified in LDAP

        Entry response = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account", response);

        String pager = OpenDJController.getAttributeValue(response, "pager");
        assertNull("Pager attribute found in account " + uid + ": " + pager, pager);

        PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS,
                LockoutStatusType.NORMAL);
    }

    @Test
    public void test200SearchObjectsIterative() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(ACCOUNT_SEARCH_ITERATIVE_FILE, ShadowType.class);

        System.out.println(SchemaDebugUtil.prettyPrint(object));
        System.out.println(object.asPrismObject().debugDump());

        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
        assertEquals(ACCOUNT_SEARCH_ITERATIVE_OID, addedObjectOid);

        final List<ShadowType> objectTypeList = new ArrayList<>();

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);

        provisioningService.searchObjectsIterative(
                ShadowType.class,
                query,
                null,
                (object1, parentResult) -> objectTypeList.add(object1.asObjectable()),
                task,
                result);

        // TODO: check result
        System.out.println("ObjectType list size: " + objectTypeList.size());

        for (ObjectType objType : objectTypeList) {
            if (objType == null) {
                System.out.println("Object not found in repo");
            } else {
                //System.out.println("obj name: " + objType.getName());
                System.out.println(object.asPrismObject().debugDump());
            }
        }
    }

    @Test
    public void test201SearchObjects() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(ACCOUNT_SEARCH_FILE, ShadowType.class);

        display("New object", object);
        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
        assertEquals(ACCOUNT_SEARCH_OID, addedObjectOid);

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        when();
        SearchResultList<PrismObject<ShadowType>> searchResults =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        result.computeStatus();
        assertSuccess(result);
        display("Search results", searchResults);

        assertEquals("Unexpected number of search results", 14, searchResults.size());

        assertConnectorOperationIncrement(1, 29);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        // SPR search. No estimate.
        assertApproxNumberOfAllResults(searchResults.getMetadata(), null);
    }

    @Test
    public void test202SearchObjectsComplexFilter() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_COMPLEX_FILTER_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);
        provisioningService.applyDefinition(ShadowType.class, query, task, result);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        when();
        List<PrismObject<ShadowType>> objListType =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        for (PrismObject<ShadowType> objType : objListType) {
            assertNotNull("Null search result", objType);
            display("found object", objType);
        }

        assertEquals("Unexpected number of objects found", 1, objListType.size());

        assertConnectorOperationIncrement(1, 3);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
    }

    @Test
    public void test203SearchObjectsByDnExists() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query =
                createAccountShadowQuerySecondaryIdentifier(ACCOUNT_BARBOSSA_DN, resource, false, false);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        when();
        List<PrismObject<ShadowType>> objListType =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        assertSuccess(result);

        for (PrismObject<ShadowType> objType : objListType) {
            assertNotNull("Null search result", objType);
            display("found object", objType);
        }

        assertEquals("Unexpected number of objects found", 1, objListType.size());

        PrismObject<ShadowType> shadow = objListType.get(0);
        assertAttribute(shadow, "dn", ACCOUNT_BARBOSSA_DN);

        assertConnectorOperationIncrement(1, 3);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
    }

    /**
     * Search for non-existent DN should return no results. It should NOT
     * throw an error.
     * MID-3730
     */
    @Test
    public void test205SearchObjectsByDnNotExists() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = createAccountShadowQuerySecondaryIdentifier(
                "uid=DoesNOTeXXXiSt,ou=People,dc=example,dc=com", resource, false, false);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        when();
        List<PrismObject<ShadowType>> objListType =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        assertSuccess(result);

        for (PrismObject<ShadowType> objType : objListType) {
            assertNotNull("Null search result", objType);
            display("found object", objType);
        }

        assertEquals("Unexpected number of objects found", 0, objListType.size());

        assertConnectorOperationIncrement(1, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
    }

    /**
     * MID-5383
     */
    @Test
    public void test206SearchObjectsComplexFilterStartsWith() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_COMPLEX_FILTER_STARTS_WITH_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);
        provisioningService.applyDefinition(ShadowType.class, query, task, result);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        when();
        List<PrismObject<ShadowType>> objListType =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        assertSuccess(result);

        for (PrismObject<ShadowType> objType : objListType) {
            assertNotNull("Null search result", objType);
            display("found object", objType);
        }

        assertEquals("Unexpected number of objects found", 1, objListType.size());

        assertConnectorOperationIncrement(1, 3);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
    }

    /**
     * Searches for given CN with kind=entitlement, intent=unlimitedGroup (leading to OC of groupOfUniqueNames).
     * The problem is that this CN exists for a user. The connector should sort this out.
     *
     * MID-6898
     */
    @Test
    public void test210SearchObjectsFromAnotherObjectClass() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var query = Resource.of(resourceBean)
                .queryFor(ShadowKindType.ENTITLEMENT, "unlimitedGroup")
                .and().item(MidPointTestConstants.PATH_CN).eq("Will Turner")
                .build();

        when();
        List<PrismObject<ShadowType>> shadows =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        assertSuccess(result);

        assertEquals("Unexpected number of objects found", 0, shadows.size());
    }

    @Test
    public void test230SearchObjectsPagedNoOffset() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);

        ObjectPaging paging = prismContext.queryFactory().createPaging(null, 3);
        query.setPaging(paging);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        when();
        SearchResultList<PrismObject<ShadowType>> searchResults =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        assertSuccess(result);
        display("Search results", searchResults);

        assertSearchResults(searchResults, "cook", "drake", "hbarbossa");

        assertConnectorOperationIncrement(1, 7);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        // null offset, SPR, no estimate.
        assertApproxNumberOfAllResults(searchResults.getMetadata(), null);
    }

    @Test
    public void test231SearchObjectsPagedOffsetZero() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);

        ObjectPaging paging = prismContext.queryFactory().createPaging(0, 4);
        query.setPaging(paging);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        when();
        SearchResultList<PrismObject<ShadowType>> searchResults =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        assertSuccess(result);
        display("Search results", searchResults);

        assertSearchResults(searchResults, "cook", "drake", "hbarbossa", "idm");

        assertConnectorOperationIncrement(1, 9);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        // VLV search if forced by using offset. So we have result number estimate.
        assertApproxNumberOfAllResults(searchResults.getMetadata(), 14);
    }

    @Test
    public void test232SearchObjectsPagedOffset() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);

        ObjectPaging paging = prismContext.queryFactory().createPaging(2, 5);
        query.setPaging(paging);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        when();
        SearchResultList<PrismObject<ShadowType>> searchResults =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        result.computeStatus();
        assertSuccess(result);
        display("Search results", searchResults);

        // The results should be this:
        assertSearchResults(searchResults, "hbarbossa", "idm", "jbeckett", "jbond", "jgibbs");

        assertConnectorOperationIncrement(1, 11);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        // VLV search if forced by using offset. So we have result number estimate.
        assertApproxNumberOfAllResults(searchResults.getMetadata(), 14);
    }

    @Test
    public void test233SearchObjectsPagedNoOffsetSortSn() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);

        ObjectPaging paging = prismContext.queryFactory().createPaging(null, 4);
        paging.setOrdering(prismContext.queryFactory().createOrdering(MidPointTestConstants.PATH_SN, OrderDirection.ASCENDING));
        query.setPaging(paging);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        when();
        SearchResultList<PrismObject<ShadowType>> searchResults =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        result.computeStatus();
        assertSuccess(result);
        display("Search results", searchResults);

        assertSearchResults(searchResults, "monk", "hbarbossa", "jbeckett", "jbond");

        assertConnectorOperationIncrement(1, 9);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        // null offset means SPR search means no estimate
        assertApproxNumberOfAllResults(searchResults.getMetadata(), null);
    }

    @Test
    public void test234SearchObjectsPagedOffsetSortSn() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);

        ObjectPaging paging = prismContext.queryFactory().createPaging(2, 4);
        paging.setOrdering(prismContext.queryFactory().createOrdering(MidPointTestConstants.PATH_SN, OrderDirection.ASCENDING));
        query.setPaging(paging);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        when();
        List<PrismObject<ShadowType>> searchResults =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        result.computeStatus();
        assertSuccess(result);
        display("Search results", searchResults);

        assertSearchResults(searchResults, "jbeckett", "jbond", "cook", "drake");

        assertConnectorOperationIncrement(1, 9);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
    }

    private void assertSearchResults(List<PrismObject<ShadowType>> searchResults, String... expectedUids) {
        assertEquals("Unexpected number of search results", expectedUids.length, searchResults.size());
        int i = 0;
        for (PrismObject<ShadowType> searchResult : searchResults) {
            new PrismObjectAsserter<>((PrismObject<? extends ObjectType>) searchResult)
                    .assertSanity();
            ShadowSimpleAttribute<String> uidAttr = ShadowUtil.getSimpleAttribute(searchResult, new QName(NS_RI, "uid"));
            String uid = uidAttr.getRealValues().iterator().next();
            displayValue("found uid", uid);
            assertEquals("Wrong uid (index " + i + ")", expectedUids[i], uid);
            i++;
        }
    }

    /**
     * Account counting is simulated.
     * For "dumber" resource it is defined in schemaHandling as a object-type-specific capability.
     */
    @Test
    public void test250CountAccounts() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);

        when();
        Integer count = provisioningService.countObjects(ShadowType.class, query, null, task, result);

        then();
        assertSuccess(result);
        displayValue("All accounts count", count);

        assertEquals("Unexpected number of search results", (Integer) 14, count);
    }

    /**
     * Account counting is simulated.
     * But "dumber" resource do not have any simulation for this.
     */
    @Test
    public void test252CountLdapGroups() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_LDAP_GROUPS_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);

        when();
        Integer count = provisioningService.countObjects(ShadowType.class, query, null, task, result);

        then();
        assertSuccess(result);
        displayValue("All LDAP groups count", count);

        assertEquals("Unexpected number of search results", getExpectedLdapGroupCountTest25x(), count);
    }

    protected Integer getExpectedLdapGroupCountTest25x() {
        return 1;
    }

    /**
     * The exception comes from the resource. There is no shadow for this object.
     */
    @Test
    public void test300AddObjectObjectAlreadyExistResource() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_SEARCH_FILE);
        display("Account to add", account);

        try {
            when();
            provisioningService.addObject(account, null, null, task, result);

            AssertJUnit.fail("Expected addObject operation to fail but it was successful");

        } catch (ObjectAlreadyExistsException e) {
            displayExpectedException(e);

            // The exception should originate from the LDAP layers
            IntegrationTestTools.assertInMessageRecursive(e, "LDAP");
        }

        // TODO: search to check that the shadow with the same NAME exists (search for OID will not do)

    }

    @Test
    public void test310AddObjectNoSn() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_NO_SN_FILE);
        display("Account to add", account);

        try {
            when();
            provisioningService.addObject(account, null, null, task, result);

            AssertJUnit.fail("Expected addObject operation to fail but it was successful");

        } catch (SchemaException e) {
            displayExpectedException(e);

            // This error should be detectable before it reaches a resource. Therefore we check that the
            // cause was not a LDAP exception

            // MID-1007
//            IntegrationTestTools.assertNotInMessageRecursive(e, "LDAP");
        }

        // TODO: search to check that the shadow with the same NAME exists (search for OID will not do)

    }

    @Test
    public void test320AddAccountPosix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(ACCOUNT_POSIX_MCMUTTON_FILE, ShadowType.class);
        display("Adding account", object);

        when();
        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);

        then();
        assertEquals(ACCOUNT_POSIX_MCMUTTON_OID, addedObjectOid);

        String uid = assertRepoShadowNew(ACCOUNT_POSIX_MCMUTTON_OID)
                .display()
                .assertName(ACCOUNT_POSIX_MCMUTTON_DN)
                .assertCachedOrigValues(QNAME_DN, ACCOUNT_POSIX_MCMUTTON_DN)
                .assertCachedNormValues(QNAME_DN, ACCOUNT_POSIX_MCMUTTON_DN.toLowerCase())
                .assertObjectClass(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS)
                .assertAuxiliaryObjectClasses(RESOURCE_OPENDJ_POSIX_ACCOUNT_OBJECTCLASS)
                .getIndexedPrimaryIdentifierValueRequired();

        assertProvisioningShadowNew(ACCOUNT_POSIX_MCMUTTON_OID)
                .display()
                .assertName(ACCOUNT_POSIX_MCMUTTON_DN)
                .assertObjectClass(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS)
                .assertAuxiliaryObjectClasses(RESOURCE_OPENDJ_POSIX_ACCOUNT_OBJECTCLASS)
                .assertOrigValues(QNAME_CN, "Haggis McMutton")
                .assertOrigValues(QNAME_SN, "McMutton")
                .assertOrigValues("homeDirectory", "/home/scotland")
                .assertOrigValues("uidNumber", BigInteger.valueOf(1001));

        // Check if object was modified in LDAP

        Entry entry = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account", entry);
        OpenDJController.assertAttribute(entry, "cn", "Haggis McMutton");
        OpenDJController.assertAttribute(entry, "sn", "McMutton");
        OpenDJController.assertAttribute(entry, "uidNumber", "1001");
        OpenDJController.assertAttribute(entry, "loginShell", "/bin/whisky");
        OpenDJController.assertAttribute(entry, "homeDirectory", "/home/scotland");

        assertShadows(17);
    }

    @Test
    public void test322ModifyAccountPosix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectModificationType objectChange =
                PrismTestUtil.parseAtomicValue(ACCOUNT_POSIX_MCMUTTON_CHANGE_FILE, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, getShadowDefinition());

        displayDumpable("Object change", delta);

        when();
        provisioningService.modifyObject(
                ShadowType.class, objectChange.getOid(), delta.getModifications(), null, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        ShadowType accountType = provisioningService.getObject(ShadowType.class,
                ACCOUNT_POSIX_MCMUTTON_OID, null, task, result).asObjectable();
        display("Object after change", accountType);

        String uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());
        assertNotNull(uid);
        assertAttribute(accountType, "cn", "Haggis McMutton");
        assertAttribute(accountType, "homeDirectory", "/home/caribbean");
        assertAttribute(accountType, "roomNumber", "Barber Shop");
        assertAttribute(accountType, "uidNumber", BigInteger.valueOf(1001));

        // Check if object was modified in LDAP

        Entry entry = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account", entry);
        OpenDJController.assertAttribute(entry, "cn", "Haggis McMutton");
        OpenDJController.assertAttribute(entry, "homeDirectory", "/home/caribbean");
        OpenDJController.assertAttribute(entry, "roomNumber", "Barber Shop");
        OpenDJController.assertAttribute(entry, "uidNumber", "1001");

        assertShadows(17);
    }

    @Test
    public void test329DeleteAccountPosix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        provisioningService.deleteObject(ShadowType.class, ACCOUNT_POSIX_MCMUTTON_OID, null, null, task, result);

        then();

        try {
            provisioningService.getObject(ShadowType.class, ACCOUNT_POSIX_MCMUTTON_OID, null, task, result);
            Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
        } catch (ObjectNotFoundException ex) {
            displayExpectedException(ex);
        }

        try {
            repositoryService.getObject(ShadowType.class, ACCOUNT_POSIX_MCMUTTON_OID, GetOperationOptions.createRawCollection(), result);
            // objType = container.getObject();
            Assert.fail("Expected exception, but haven't got one.");
        } catch (ObjectNotFoundException ex) {
            displayExpectedException(ex);
            assertTrue(ex.getMessage().contains(ACCOUNT_POSIX_MCMUTTON_OID));
        }

        assertShadows(16);
    }

    /**
     * Search for account created directly on resource (no shadow in repo). The account has
     * posixAccount auxiliary object class. Provisioning should figure that out.
     */
    @Test
    public void test330SearchForPosixAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_VANHELGEN_FILE, QueryType.COMPLEX_TYPE);
        ObjectQuery query = getQueryConverter().createObjectQuery(ShadowType.class, queryType);
        provisioningService.applyDefinition(ShadowType.class, query, task, result);

        Entry entry = openDJController.addEntryFromLdifFile(ACCOUNT_POSIX_VANHELGEN_LDIF_FILE);
        display("Added entry", entry);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        when();
        List<PrismObject<ShadowType>> objListType =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        for (PrismObject<ShadowType> objType : objListType) {
            assertNotNull("Null search result", objType);
            display("found object", objType);
        }

        assertEquals("Unexpected number of objects found", 1, objListType.size());

        PrismObject<ShadowType> provisioningShadow = objListType.get(0);
        assertAttribute(provisioningShadow, "cn", "Edward Van Helgen");
        assertAttribute(provisioningShadow, "homeDirectory", "/home/vanhelgen");
        assertAttribute(provisioningShadow, "uidNumber", BigInteger.valueOf(1002));

        assertConnectorOperationIncrement(1, 3);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        assertShadows(17);
    }

    // TODO: synchronization of auxiliary object classes

    @Test
    public void test400AddGroupSwashbucklers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(GROUP_SWASHBUCKLERS_FILE, ShadowType.class);
        display("Adding object", object);

        when();
        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);

        then();
        assertEquals(GROUP_SWASHBUCKLERS_OID, addedObjectOid);

        String uid = assertRepoShadowNew(GROUP_SWASHBUCKLERS_OID)
                .assertName(GROUP_SWASHBUCKLERS_DN_ORIG)
                .getIndexedPrimaryIdentifierValueRequired();

        assertProvisioningShadowNew(GROUP_SWASHBUCKLERS_OID)
                .assertName(GROUP_SWASHBUCKLERS_DN_ORIG)
                .assertOrigValues(GROUP_UNIQUE_MEMBER_ATTR_QNAME); // should be no values for members attribute

        Entry ldapEntry = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP group", ldapEntry);
        assertNotNull("No LDAP group entry", ldapEntry);
        String groupDn = ldapEntry.getDN().toString();
        assertEquals("Wrong group DN", GROUP_SWASHBUCKLERS_DN_ORIG, groupDn);

        assertShadows(18);
    }

    @Test
    public void test402AddAccountMorganWithAssociation() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(ACCOUNT_MORGAN_FILE, ShadowType.class);
        IntegrationTestTools.display("Adding object", object);

        when();
        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);

        then();
        assertEquals(ACCOUNT_MORGAN_OID, addedObjectOid);

        assertRepoShadowNew(ACCOUNT_MORGAN_OID)
                .assertName(ACCOUNT_MORGAN_DN);

        String swashbucklersUid = assertRepoShadowNew(GROUP_SWASHBUCKLERS_OID)
                .getIndexedPrimaryIdentifierValueRequired();

        // @formatter:off
        ShadowAsserter<Void> provisioningShadowAsserter = assertShadowProvisioning(ACCOUNT_MORGAN_OID)
                .assertName(ACCOUNT_MORGAN_DN)
                .associations()
                    .assertSize(1)
                    .association(ASSOCIATION_GROUP_NAME)
                        .assertSize(1)
                        .forShadowOid(GROUP_SWASHBUCKLERS_OID)
                            .assertIdentifierValueMatching(QNAME_DN, GROUP_SWASHBUCKLERS_DN_ORIG)
                            .assertIdentifierValueMatching(QNAME_ENTRY_UUID, swashbucklersUid)
                        .end()
                    .end()
                .end();
        // @formatter:on

        String uid = provisioningShadowAsserter
                .attributes()
                .getSimpleAttributeValue(getPrimaryIdentifierQName());
        assertNotNull(uid);

        Entry accountEntry = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account", accountEntry);
        assertNotNull("No LDAP account entry", accountEntry);
        String accountDn = accountEntry.getDN().toString();
        assertEquals("Wrong account DN", ACCOUNT_MORGAN_DN, accountDn);

        Entry groupEntry = openDJController.fetchEntryRequired(GROUP_SWASHBUCKLERS_DN_ORIG);
        display("LDAP group", groupEntry);
        openDJController.assertUniqueMember(groupEntry, accountDn);

        assertShadows(19);
    }

    @Test
    public void test403ModifyMorganReplaceAssociation() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ObjectModificationType modification =
                prismContext.parserFor(FILE_MODIFY_ASSOCIATION_REPLACE).parseRealValue(ObjectModificationType.class);
        ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(modification, ShadowType.class);
        try {
            provisioningService.modifyObject(
                    ShadowType.class, ACCOUNT_MORGAN_OID, delta.getModifications(), null, null, task, result);
            assertNotReached();
        } catch (SchemaException e) {
            displayExpectedException("replace delta is not supported for association", e);
        }

        then();
        assertRepoShadowNew(ACCOUNT_MORGAN_OID)
                .assertName(ACCOUNT_MORGAN_DN);

        ShadowAsserter<Void> provisioningShadowAsserter = assertShadowProvisioning(ACCOUNT_MORGAN_OID)
                .assertName(ACCOUNT_MORGAN_DN)
                .associations()
                .assertSize(1)
                .association(ASSOCIATION_GROUP_NAME)
                .assertShadowOids(GROUP_SWASHBUCKLERS_OID)
                .end()
                .end();

        String uid = provisioningShadowAsserter
                .attributes()
                .getSimpleAttributeValue(getPrimaryIdentifierQName());
        assertNotNull(uid);

        Entry accountEntry = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP account", accountEntry);
        assertNotNull("No LDAP account entry", accountEntry);
        String accountDn = accountEntry.getDN().toString();
        assertEquals("Wrong account DN", ACCOUNT_MORGAN_DN, accountDn);

        Entry groupEntry = openDJController.fetchEntryRequired(GROUP_SWASHBUCKLERS_DN_ORIG);
        display("LDAP group", groupEntry);
        openDJController.assertUniqueMember(groupEntry, accountDn);

        assertShadows(19);
    }

    @Test
    public void test405GetGroupSwashbucklers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        PrismObject<ShadowType> provisioningShadow =
                provisioningService.getObject(ShadowType.class, GROUP_SWASHBUCKLERS_OID, null, task, result);

        then();
        String uid = assertShadowNew(provisioningShadow.asObjectable())
                .assertName(GROUP_SWASHBUCKLERS_DN_ORIG)
                .attributes()
                .assertNoSimpleAttribute(GROUP_UNIQUE_MEMBER_ATTR_QNAME)
                .getSimpleAttributeValue(getPrimaryIdentifierQName());

        assertThat(uid).as("uid").isNotNull();

        Entry ldapEntry = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP group", ldapEntry);
        assertNotNull("No LDAP group entry", ldapEntry);
        String groupDn = ldapEntry.getDN().toString();
        assertEquals("Wrong group DN", GROUP_SWASHBUCKLERS_DN_ORIG, groupDn);

        assertShadows(19);
    }

    @Test
    public void test410CreateLdapGroupAndSearchGroups() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.addEntry("""
                dn: cn=seadogs,ou=groups,dc=EXAMPLE,dc=com
                objectClass: groupOfUniqueNames
                objectClass: top
                cn: seadogs""");

        ObjectQuery query =
                ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_GROUP_OBJECTCLASS);

        when();
        SearchResultList<PrismObject<ShadowType>> resultList =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        result.computeStatus();
        assertSuccess(result);
        display("Search result", resultList);

        IntegrationTestTools.assertSearchResultNames(resultList, dnMatchingRule,
                "cn=Pirates,ou=groups,dc=example,dc=com",
                "cn=swashbucklers,ou=Groups,dc=example,dc=com",
                "cn=seadogs,ou=Groups,dc=example,dc=com");

        assertShadows(20);
    }

    @Test
    public void test412CreateLdapGroupWithMemberAndGet() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.addEntry("""
                dn: cn=sailor,ou=Groups,dc=example,dc=com
                objectClass: groupOfUniqueNames
                objectClass: top
                cn: sailor
                uniqueMember: uid=MOrgan,ou=PEOPLE,dc=example,dc=com""");

        when();
        var shadow = provisioningService.getShadow(ACCOUNT_MORGAN_OID, null, task, result);

        then();
        assertSuccess(result);
        display("Account shadow after", shadow);

        assertShadows(21);

        var sailorRepoShadow = findShadowByName(
                RESOURCE_OPENDJ_GROUP_OBJECTCLASS, "cn=sailor,ou=groups,dc=example,dc=com", resource, result);
        display("Group shadow", sailorRepoShadow);
        groupSailorOid = sailorRepoShadow.getOid();

        assertEntitlementGroup(shadow.getPrismObject(), groupSailorOid);

        assertShadows(21);
    }

    @Test
    public void test414AddGroupCorsairsAssociateUser() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(GROUP_CORSAIRS_FILE, ShadowType.class);
        IntegrationTestTools.display("Adding object", object);

        when();
        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);

        then();
        assertEquals(GROUP_CORSAIRS_OID, addedObjectOid);

        assertRepoShadowNew(GROUP_CORSAIRS_OID)
                .assertName(GROUP_CORSAIRS_DN);

        // Do NOT read provisioning shadow here. We want everything to be "fresh"

        assertShadows(22);
    }

    @Test
    public void test416AssociateUserToCorsairs() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = createEntitleDelta(ACCOUNT_MORGAN_OID, ASSOCIATION_GROUP_NAME, GROUP_CORSAIRS_OID);
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        when();
        provisioningService.modifyObject(
                ShadowType.class,
                delta.getOid(),
                delta.getModifications(),
                new OperationProvisioningScriptsType(),
                null,
                task,
                result);

        then();
        Entry groupEntry = openDJController.fetchEntryRequired(GROUP_CORSAIRS_DN);
        display("LDAP group", groupEntry);
        openDJController.assertUniqueMember(groupEntry, ACCOUNT_MORGAN_DN);

        assertShadows(22);
    }

    @Test
    public void test418GetMorgan() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID, null, task, result);

        then();
        result.computeStatus();
        assertSuccess(result);
        display("Shadow", shadow);

        assertEntitlementGroup(shadow, GROUP_SWASHBUCKLERS_OID);
        assertEntitlementGroup(shadow, groupSailorOid);
        assertEntitlementGroup(shadow, GROUP_CORSAIRS_OID);

        assertShadows(22);
    }

    /**
     * MID-6770
     */
    @Test
    public void test419PhantomRenameMorgan() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var delta = Resource.of(resourceBean)
                .deltaFor(OBJECT_CLASS_INETORGPERSON_QNAME)
                .item(PATH_DN).replace("uid=morgan,ou=People,dc=example,dc=com")
                .asObjectDelta(ACCOUNT_MORGAN_OID);

        when();
        provisioningService.modifyObject(
                ShadowType.class, ACCOUNT_MORGAN_OID, delta.getModifications(), null, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID, null, task, result);
        display("Shadow", shadow);

        assertEntitlementGroup(shadow, GROUP_SWASHBUCKLERS_OID);
        assertEntitlementGroup(shadow, groupSailorOid);
        assertEntitlementGroup(shadow, GROUP_CORSAIRS_OID);

        assertShadows(22);
    }

    /**
     * MID-6770
     *
     * The same as before, but now the DN attribute in shadow is out of date.
     * We intentionally change it via direct repo call before the provisioning action.
     */
    @Test
    public void test420PhantomRenameMorganRotten() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var normAwareDef = getAccountAttributeDefinitionRequired(QNAME_DN).toNormalizationAware();
        var normAwareAttr = normAwareDef.adoptRealValuesAndInstantiate(
                List.of("uid=morgan-rotten,ou=People,dc=example,dc=com"));
        ObjectDelta<ShadowType> rotDelta = deltaFor(ShadowType.class)
                .item(PATH_DN, normAwareDef)
                .replaceRealValues(normAwareAttr.getRealValues())
                .asObjectDelta(ACCOUNT_MORGAN_OID);
        repositoryService.modifyObject(ShadowType.class, ACCOUNT_MORGAN_OID, rotDelta.getModifications(), result);

        // This is no-op on resource. (The DN on resource has not changed.)
        ObjectDelta<ShadowType> delta = Resource.of(resourceBean)
                .deltaFor(OBJECT_CLASS_INETORGPERSON_QNAME)
                .item(PATH_DN).replace("uid=morgan,ou=People,dc=example,dc=com")
                .asObjectDelta(ACCOUNT_MORGAN_OID);

        when();
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_MORGAN_OID, delta.getModifications(),
                null, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID, null, task, result);
        display("Shadow", shadow);

        assertEntitlementGroup(shadow, GROUP_SWASHBUCKLERS_OID);
        assertEntitlementGroup(shadow, groupSailorOid);
        assertEntitlementGroup(shadow, GROUP_CORSAIRS_OID);

        assertShadows(22);
    }

    /**
     * Morgan has a group associations. If the account is gone the group memberships should also be gone.
     */
    @Test
    public void test429DeleteAccountMorgan() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        provisioningService.deleteObject(ShadowType.class, ACCOUNT_MORGAN_OID, null, null, task, result);

        try {
            provisioningService
                    .getObject(ShadowType.class, ACCOUNT_MORGAN_OID, null, task, result)
                    .asObjectable();
            Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
        } catch (ObjectNotFoundException ex) {
            displayExpectedException(ex);
        }

        try {
            getShadowRepoLegacy(ACCOUNT_MORGAN_OID);
            Assert.fail("Expected exception, but haven't got one.");
        } catch (Exception ex) {
            assertEquals(ex.getClass(), ObjectNotFoundException.class);
            assertTrue(ex.getMessage().contains(ACCOUNT_MORGAN_OID));
        }

        Entry groupEntry = openDJController.fetchEntryRequired(GROUP_SWASHBUCKLERS_DN_NORM);
        display("LDAP group", groupEntry);
        if (!hasNativeReferences()) {
            openDJController.assertNoUniqueMember(groupEntry, ACCOUNT_MORGAN_DN);
        } else {
            // currently, there's no referential integrity set up on this OpenDJ, so the membership is still there
        }

        assertShadows(21);
    }

    @Test
    public void test450ListGroupsObjectclass() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query =
                ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_GROUP_OBJECTCLASS);
        displayDumpable("query", query);

        when();
        SearchResultList<PrismObject<ShadowType>> objects =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        display("found objects", objects);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong number of objects found", 5, objects.size());

        assertShadows(21);
    }

    @Test
    public void test452ListLdapGroupsKindIntent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(
                RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, INTENT_LDAP_GROUP);
        displayDumpable("query", query);

        when();
        SearchResultList<PrismObject<ShadowType>> objects =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        display("found objects", objects);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong number of objects found", 5, objects.size());

        assertShadows(21);
    }

    @Test
    public void test454ListSpecialGroupsKindIntent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(
                RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, "specialGroup");
        displayDumpable("query", query);

        when();
        SearchResultList<PrismObject<ShadowType>> objects =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        display("found objects", objects);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Check that none of the normal LDAP groups appear here ... even if they have the same objectclass
        assertEquals("Wrong number of objects found", 0, objects.size());

        // Discovered base context for specialgroups
        assertShadows(22);
    }

    @Test
    public void test456AddGroupSpecialists() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(GROUP_SPECIALISTS_FILE, ShadowType.class);
        display("Adding object", object);

        when();
        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);

        then();
        assertEquals(GROUP_SPECIALISTS_OID, addedObjectOid);

        var uid = assertRepoShadowNew(GROUP_SPECIALISTS_OID)
                .assertName(GROUP_SPECIALISTS_DN_ORIG)
                .getIndexedPrimaryIdentifierValueRequired();

        assertProvisioningShadowNew(GROUP_SPECIALISTS_OID)
                .assertName(GROUP_SPECIALISTS_DN_ORIG)
                .attributes()
                .assertNoSimpleAttribute(GROUP_UNIQUE_MEMBER_ATTR_QNAME);

        Entry ldapEntry = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP group", ldapEntry);
        assertNotNull("No LDAP group entry", ldapEntry);
        String groupDn = ldapEntry.getDN().toString();
        assertEquals("Wrong group DN", GROUP_SPECIALISTS_DN_NORM, dnMatchingRule.normalize(groupDn));

        assertShadows(23);
    }

    @Test
    public void test457ListLdapGroupsKindIntent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(
                RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, INTENT_LDAP_GROUP);
        displayDumpable("query", query);

        when();
        SearchResultList<PrismObject<ShadowType>> objects = provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        display("found objects", objects);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong number of objects found", 5, objects.size());

        assertShadows(23);
    }

    @Test
    public void test458ListSpecialGroupsKindIntent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(
                RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, "specialGroup");
        displayDumpable("query", query);

        when();
        SearchResultList<PrismObject<ShadowType>> objects =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        display("found objects", objects);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // The "specialists" group is now here
        assertEquals("Wrong number of objects found", 1, objects.size());

        assertShadows(23);
    }

    /**
     * List objects (organizational units) with kind `generic` and intent `ou-people`. (There are no sub-ous in People,
     * so the test should return only the People OU.)
     *
     * The basic problem is that the definition of `generic` : `ou-people` has an object class
     * of `organizationalUnit` and - at the same time - it has a `baseContext` pointing
     * to the same object class of `organizationalUnit`.
     *
     * This test therefore makes sure this will not end up in endless loop (stack overflow).
     */
    @Test
    public void test460ListOrganizationalUnitPeopleKindIntent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query =
                ObjectQueryUtil.createResourceAndKindIntent(
                        RESOURCE_OPENDJ_OID, ShadowKindType.GENERIC, "ou-people");
        displayDumpable("query", query);

        when();
        SearchResultList<PrismObject<ShadowType>> objects =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        then();
        display("found objects", objects);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Just the ou=People itself
        assertEquals("Wrong number of objects found", 1, objects.size());

        assertShadows(24);
    }

    /**
     * Description is a "language tag" attribute (PolyString).
     * MID-5210
     */
    @Test
    public void test470AddAccountPolyDescription() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> accountBefore = parseObject(ACCOUNT_POLY_FILE);

        display("Account before", accountBefore);

        when();
        provisioningService.addObject(accountBefore, null, null, task, result);

        then();
        assertSuccess(result);

        Entry entry = openDJController.fetchEntry(ACCOUNT_POLY_DN);
        display("LDAP Entry", entry);
        String descriptionStringAfter = OpenDJController.getAttributeValue(entry, ATTRIBUTE_DESCRIPTION_NAME);
        assertNotNull("No description in LDAP entry", descriptionStringAfter);
        assertEquals("Unexpected description in LDAP entry", ACCOUNT_POLY_DESCRIPTION_ORIG, descriptionStringAfter);

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_POLY_OID, null, task, result);

        display("Object after change", shadow);

        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<PolyString> descAttr = attributesContainer.findProperty(ATTRIBUTE_DESCRIPTION_QNAME);
        PolyString descriptionPolyStringAfter = descAttr.getValues().get(0).getValue();
        displayDumpable("description after (shadow)", descriptionPolyStringAfter);

        assertEquals("Wrong orig in description polystring (shadow)", ACCOUNT_POLY_DESCRIPTION_ORIG, descriptionPolyStringAfter.getOrig());

        assertShadows(25);
    }

    /**
     * Description is a "language tag" attribute (PolyString).
     * Simple modification with just "orig". No languages yet.
     * MID-5210
     */
    @Test
    public void test472ModifyAccountJackDescriptionOrig() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PolyString descriptionBefore = new PolyString("Bar");

        PropertyDelta<PolyString> descriptionDelta = prismContext.deltaFactory().property().create(
                ItemPath.create(ShadowType.F_ATTRIBUTES, ATTRIBUTE_DESCRIPTION_QNAME),
                null);
        descriptionDelta.setRealValuesToReplace(descriptionBefore);

        Collection<? extends ItemDelta<?, ?>> modifications = MiscSchemaUtil.createCollection(descriptionDelta);

        display("Modifications", modifications);

        when();
        provisioningService.modifyObject(
                ShadowType.class, ACCOUNT_JACK_OID, modifications, null, null, task, result);

        then();
        assertSuccess(result);

        Entry entry = openDJController.searchByUid("rename");
        display("LDAP Entry", entry);
        assertDescription(entry, descriptionBefore.getOrig() /* no langs */);

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_JACK_OID, null, task, result);

        display("Object after change", shadow);

        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<PolyString> descAttr = attributesContainer.findProperty(ATTRIBUTE_DESCRIPTION_QNAME);
        assertPolyString(descAttr.getValues().get(0).getValue(), "description after (shadow from provisioning)")
                .assertOrig(descriptionBefore.getOrig())
                .assertNoLangs();

        assertShadows(25);
    }

    /**
     * Description is a "language tag" attribute (PolyString).
     * Modification with languages.
     * MID-5210
     */
    @Test
    public void test474ModifyAccountJackDescriptionLangEnSk() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PolyString descriptionBefore = new PolyString(USER_JACK_FULL_NAME);
        descriptionBefore.setLang(MiscUtil.paramsToMap(JACK_FULL_NAME_LANG_EN_SK));

        PropertyDelta<PolyString> descriptionDelta = prismContext.deltaFactory().property().create(
                ItemPath.create(ShadowType.F_ATTRIBUTES, ATTRIBUTE_DESCRIPTION_QNAME),
                null);
        descriptionDelta.setRealValuesToReplace(descriptionBefore);

        Collection<? extends ItemDelta<?, ?>> modifications = MiscSchemaUtil.createCollection(descriptionDelta);

        display("Modifications", modifications);

        when();
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_JACK_OID,
                modifications, null, null, task, result);

        then();
        assertSuccess(result);

        Entry entry = openDJController.searchByUid("rename");
        display("LDAP Entry", entry);
        assertDescription(entry, USER_JACK_FULL_NAME, JACK_FULL_NAME_LANG_EN_SK);

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_JACK_OID, null, task, result);

        display("Object after change", shadow);

        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<PolyString> descAttr = attributesContainer.findProperty(ATTRIBUTE_DESCRIPTION_QNAME);
        assertPolyString(descAttr.getValues().get(0).getValue(), "description after (shadow from provisioning)")
                .assertOrig(descriptionBefore.getOrig())
                .assertLangs(JACK_FULL_NAME_LANG_EN_SK);

        assertShadows(25);
    }

    /**
     * Description is a "language tag" attribute (PolyString).
     * Modification with more languages.
     * MID-5210
     */
    @Test
    public void test476ModifyAccountJackDescriptionLangEnSkRuHr() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PolyString descriptionBefore = new PolyString(USER_JACK_FULL_NAME);
        descriptionBefore.setLang(MiscUtil.paramsToMap(JACK_FULL_NAME_LANG_EN_SK_RU_HR));

        PropertyDelta<PolyString> descriptionDelta = prismContext.deltaFactory().property().create(
                ItemPath.create(ShadowType.F_ATTRIBUTES, ATTRIBUTE_DESCRIPTION_QNAME),
                null);
        descriptionDelta.setRealValuesToReplace(descriptionBefore);

        Collection<? extends ItemDelta<?, ?>> modifications = MiscSchemaUtil.createCollection(descriptionDelta);

        display("Modifications", modifications);

        when();
        provisioningService.modifyObject(
                ShadowType.class, ACCOUNT_JACK_OID, modifications, null, null, task, result);

        then();
        assertSuccess(result);

        Entry entry = openDJController.searchByUid("rename");
        display("LDAP Entry", entry);
        assertDescription(entry, USER_JACK_FULL_NAME, JACK_FULL_NAME_LANG_EN_SK_RU_HR);

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_JACK_OID, null, task, result);

        display("Object after change", shadow);

        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<PolyString> descAttr = attributesContainer.findProperty(ATTRIBUTE_DESCRIPTION_QNAME);
        assertPolyString(descAttr.getValues().get(0).getValue(), "description after (shadow from provisioning)")
                .assertOrig(descriptionBefore.getOrig())
                .assertLangs(JACK_FULL_NAME_LANG_EN_SK_RU_HR);

        assertShadows(25);
    }

    /**
     * Description is a "language tag" attribute (PolyString).
     * Modification with languages, some are new, some are deleted.
     * MID-5210
     */
    @Test
    public void test478ModifyAccountJackDescriptionLangEnSkRuHr() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PolyString descriptionBefore = new PolyString(USER_JACK_FULL_NAME);
        descriptionBefore.setLang(MiscUtil.paramsToMap(JACK_FULL_NAME_LANG_CZ_HR));

        PropertyDelta<PolyString> descriptionDelta = prismContext.deltaFactory().property().create(
                ItemPath.create(ShadowType.F_ATTRIBUTES, ATTRIBUTE_DESCRIPTION_QNAME),
                null);
        descriptionDelta.setRealValuesToReplace(descriptionBefore);

        Collection<? extends ItemDelta<?, ?>> modifications = MiscSchemaUtil.createCollection(descriptionDelta);

        display("Modifications", modifications);

        when();
        provisioningService.modifyObject(
                ShadowType.class, ACCOUNT_JACK_OID, modifications, null, null, task, result);

        then();
        assertSuccess(result);

        Entry entry = openDJController.searchByUid("rename");
        display("LDAP Entry", entry);
        assertDescription(entry, USER_JACK_FULL_NAME, JACK_FULL_NAME_LANG_CZ_HR);

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_JACK_OID, null, task, result);

        display("Object after change", shadow);

        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<PolyString> descAttr = attributesContainer.findProperty(ATTRIBUTE_DESCRIPTION_QNAME);
        assertPolyString(descAttr.getValues().get(0).getValue(), "description after (shadow from provisioning)")
                .assertOrig(descriptionBefore.getOrig())
                .assertLangs(JACK_FULL_NAME_LANG_CZ_HR);

        assertShadows(25);
    }

    /**
     * Deletion of a PolyString value.
     * MID-5970
     */
    @Test
    public void test478bModifyAccountJackDeleteDescription() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PolyString descriptionBefore = new PolyString(USER_JACK_FULL_NAME);
        descriptionBefore.setLang(MiscUtil.paramsToMap(JACK_FULL_NAME_LANG_CZ_HR));

        PropertyDelta<PolyString> descriptionDelta = prismContext.deltaFactory().property().create(
                ItemPath.create(ShadowType.F_ATTRIBUTES, ATTRIBUTE_DESCRIPTION_QNAME),
                null);
        descriptionDelta.addRealValuesToDelete(descriptionBefore);

        Collection<? extends ItemDelta<?, ?>> modifications = MiscSchemaUtil.createCollection(descriptionDelta);

        display("Modifications", modifications);

        when();
        provisioningService.modifyObject(
                ShadowType.class, ACCOUNT_JACK_OID, modifications, null, null, task, result);

        then();
        assertSuccess(result);

        Entry entry = openDJController.searchByUid("rename");
        display("LDAP Entry", entry);
        assertDescription(entry, null);

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_JACK_OID, null, task, result);

        display("Object after change", shadow);

        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<PolyString> descAttr = attributesContainer.findProperty(ATTRIBUTE_DESCRIPTION_QNAME);
        if (descAttr != null) {
            assertEquals("Wrong # of values in description: " + descAttr, 0, descAttr.getValues().size());
        } else {
            // ok, attribute is not there
        }

        assertShadows(25);
    }

    /**
     * Description is a "language tag" attribute (PolyString).
     * Modification without any values. Clean slate again.
     * MID-5210
     */
    @Test
    public void test479ModifyAccountJackDescriptionJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PolyString descriptionBefore = new PolyString(USER_JACK_FULL_NAME);

        PropertyDelta<PolyString> descriptionDelta = prismContext.deltaFactory().property().create(
                ItemPath.create(ShadowType.F_ATTRIBUTES, ATTRIBUTE_DESCRIPTION_QNAME),
                null);
        descriptionDelta.setRealValuesToReplace(descriptionBefore);

        Collection<? extends ItemDelta<?, ?>> modifications =
                MiscSchemaUtil.createCollection(descriptionDelta);

        display("Modifications", modifications);

        when();
        provisioningService.modifyObject(ShadowType.class, ACCOUNT_JACK_OID,
                modifications, null, null, task, result);

        then();
        assertSuccess(result);

        Entry entry = openDJController.searchByUid("rename");
        display("LDAP Entry", entry);
        assertDescription(entry, USER_JACK_FULL_NAME /* no langs */);

        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_JACK_OID, null, task, result);

        display("Object after change", shadow);

        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<PolyString> descAttr = attributesContainer.findProperty(ATTRIBUTE_DESCRIPTION_QNAME);
        assertPolyString(descAttr.getValues().get(0).getValue(), "description after (shadow from provisioning)")
                .assertOrig(descriptionBefore.getOrig())
                .assertNoLangs();

        assertShadows(25);
    }

    @Test
    public void test480AddOuSuper() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType object = parseObjectType(OU_SUPER_FILE, ShadowType.class);
        display("Adding object", object);

        when();
        String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);

        then();
        assertEquals(OU_SUPER_OID, addedObjectOid);

        var uid = assertRepoShadowNew(OU_SUPER_OID)
                .assertName(OU_SUPER_DN_ORIG)
                .getIndexedPrimaryIdentifierValueRequired();

        assertProvisioningShadowNew(OU_SUPER_OID)
                .assertName(OU_SUPER_DN_ORIG);

        Entry ldapEntry = openDJController.searchAndAssertByEntryUuid(uid);
        display("LDAP ou", ldapEntry);
        assertNotNull("No LDAP ou entry", ldapEntry);
        String groupDn = ldapEntry.getDN().toString();
        assertEquals("Wrong ou DN", OU_SUPER_DN_ORIG, groupDn);

        assertShadows(26);
    }

    /**
     * Try to delete ou=Super,dc=example,dc=com. But before doing that create a subobject:
     * ou=sub,ou=Super,dc=example,dc=com. LDAP server should normally refuse to delete the Super OU
     * because it is not empty. But we have configured use of "tree delete" control here.
     * Therefore the delete should work.
     *
     * MID-5935
     */
    @Test
    public void test489DeleteOuSuperWithSub() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        createSubOrg();

        when();
        provisioningService.deleteObject(ShadowType.class, OU_SUPER_OID, null, null, task, result);

        then();
        assertSuccess(result);

        assertNoRepoShadow(OU_SUPER_OID);
        openDJController.assertNoEntry(OU_SUPER_DN_ORIG);

        assertShadows(25);
    }

    void createSubOrg() throws IOException, LDIFException {
        openDJController.addEntry("""
                dn: ou=sub,ou=Super,dc=example,dc=com
                objectClass: organizationalUnit
                ou: sub""");
    }

    /** Checks adding a membership for a group that no longer exists. Correct exception should be returned. MID-10015. */
    @Test(enabled=false) // MID-10015
    public void test500AddingNonExistingGroupMembership() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var accountName = "a-" + getTestNameShort();
        var accountDn = "uid=" + accountName + ",ou=People,dc=example,dc=com";

        var groupName = "g-" + getTestNameShort();
        var groupDn = "cn=" + groupName + ",ou=Groups,dc=example,dc=com";

        given("group exists on the resource");
        var groupShadow = Resource.of(resource)
                .shadow(TYPE_LDAP_GROUP)
                .withSimpleAttribute(QNAME_DN, groupDn)
                .asPrismObject();
        var groupOid = provisioningService.addObject(groupShadow, null, null, task, result);

        and("account exists on the resource (not in the group)");
        var accountShadow = Resource.of(resource)
                .shadow(ACCOUNT_DEFAULT)
                .withSimpleAttribute(QNAME_DN, accountDn)
                .withSimpleAttribute(QNAME_CN, accountName)
                .withSimpleAttribute(QNAME_SN, accountName)
                .asPrismObject();
        var accountOid = provisioningService.addObject(accountShadow, null, null, task, result);

        and("group is deleted (on the resource)");
        openDJController.delete(groupDn);

        when("account is entitled with group membership");
        try {
            provisioningService.modifyObject(
                    ShadowType.class,
                    accountOid,
                    createEntitleDelta(accountOid, RI_GROUP, groupOid)
                            .getModifications(),
                    null, null, task, result);
        } catch (Exception e) {
            assertExpectedException(e);
            if (e instanceof ObjectNotFoundException objectNotFoundException) {
                fail("We need to distinguish between group and account not found. But we got: " + objectNotFoundException);
            }
        }
    }

    /** Tests renaming a group (especially using native references). MID-9938. */
    @Test(enabled = false) // MID-9938
    public void test510RenamingGroup() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        var accountName = "a-" + getTestNameShort();
        var accountDn = "uid=" + accountName + ",ou=People,dc=example,dc=com";

        var groupName = "g-" + getTestNameShort();
        var groupDn = "cn=" + groupName + ",ou=Groups,dc=example,dc=com";
        var groupDnNew = "cn=" + groupName + "-new,ou=Groups,dc=example,dc=com";

        given("account + group exist on the resource");
        var accountShadow = Resource.of(resource)
                .shadow(ACCOUNT_DEFAULT)
                .withSimpleAttribute(QNAME_DN, accountDn)
                .withSimpleAttribute(QNAME_CN, accountName)
                .withSimpleAttribute(QNAME_SN, accountName)
                .asAbstractShadow();
        var accountOid = provisioningService.addObject(accountShadow.getPrismObject(), null, null, task, result);

        var groupShadow = Resource.of(resource)
                .shadow(TYPE_LDAP_GROUP)
                .withSimpleAttribute(QNAME_DN, groupDn)
                .asAbstractShadow();
        var groupOid = provisioningService.addObject(groupShadow.getPrismObject(), null, null, task, result);

        provisioningService.modifyObject(
                ShadowType.class,
                accountOid,
                createEntitleDelta(accountOid, RI_GROUP, groupOid)
                        .getModifications(),
                null, null, task, result);

        when("group is renamed");
        provisioningService.modifyObject(
                ShadowType.class,
                groupOid,
                Resource.of(resource)
                        .deltaFor(groupShadow.getObjectClass())
                        .item(PATH_DN)
                        .replace(groupDnNew)
                        .asItemDeltas(),
                null, null, task, result);

        then("all is OK");
        assertSuccess(result);
        var groupAfter = provisioningService.getShadow(groupOid, null, task, result);
        assertShadowAfter(groupAfter.getPrismObject())
                .attributes()
                .assertValue(QNAME_DN, groupDnNew);
    }

    @Test
    public void test701ConfiguredCapabilityNoRead() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addResourceFromFile(
                RESOURCE_OPENDJ_NO_READ_FILE, IntegrationTestTools.CONNECTOR_LDAP_TYPE, true, result);

        // The cached shadow contains some extra attributes. Invalidation makes that less serious.
        // (We do the invalidation not only if FULL shadow caching is enforced, but if ANY caching is enforced.)
        if (InternalsConfig.isShadowCachingOnByDefault()) {
            invalidateShadowCache(RESOURCE_OPENDJ_OID);
        }

        try {
            provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
            AssertJUnit.fail("Expected unsupported operation exception, but haven't got one.");
        } catch (UnsupportedOperationException ex) {
            displayExpectedException(ex);
        }
    }

    @Test
    public void test702ConfiguredCapabilityNoCreate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addResourceFromFile(RESOURCE_OPENDJ_NO_CREATE_FILE, IntegrationTestTools.CONNECTOR_LDAP_TYPE, true, result);

        try {
            PrismObject<ShadowType> shadow = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class).asPrismObject();
            provisioningService.addObject(shadow, null, null, task, result);
            AssertJUnit.fail("Expected unsupported operation exception, but haven't got one.");
        } catch (UnsupportedOperationException ex) {
            displayExpectedException(ex);
        }
    }

    @Test
    public void test703ConfiguredCapabilityNoDelete() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addResourceFromFile(RESOURCE_OPENDJ_NO_DELETE_FILE, IntegrationTestTools.CONNECTOR_LDAP_TYPE, true, result);

        // The cached shadow contains some extra attributes. Invalidation makes that less serious.
        // (We do the invalidation not only if FULL shadow caching is enforced, but if ANY caching is enforced.)
        if (InternalsConfig.isShadowCachingOnByDefault()) {
            invalidateShadowCache(RESOURCE_OPENDJ_OID);
        }

        try {
            provisioningService.deleteObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, task, result);
            AssertJUnit.fail("Expected unsupported operation exception, but haven't got one.");
        } catch (UnsupportedOperationException ex) {
            displayExpectedException(ex);
        }
    }

    @Test
    public void test704ConfiguredCapabilityNoUpdate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addResourceFromFile(RESOURCE_OPENDJ_NO_UPDATE_FILE, IntegrationTestTools.CONNECTOR_LDAP_TYPE, true, result);

        // The cached shadow contains some extra attributes. Invalidation makes that less serious.
        // (We do the invalidation not only if FULL shadow caching is enforced, but if ANY caching is enforced.)
        if (InternalsConfig.isShadowCachingOnByDefault()) {
            invalidateShadowCache(RESOURCE_OPENDJ_OID);
        }

        try {
            PropertyDelta<String> delta =
                    prismContext.deltaFactory().property().createModificationReplaceProperty(
                            MidPointTestConstants.PATH_SN,
                            prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class),
                            "doesnotmatter");
            provisioningService.modifyObject(
                    ShadowType.class, ACCOUNT_WILL_OID, List.of(delta), null, null, task, result);
            AssertJUnit.fail("Expected unsupported operation exception, but haven't got one.");
        } catch (UnsupportedOperationException ex) {
            displayExpectedException(ex);
        }
    }

    @Test
    public void test710AddResourceOpenDjBadCredentials() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_OPENDJ_BAD_CREDENTIALS_FILE);
        fillInConnectorRef(resource, IntegrationTestTools.CONNECTOR_LDAP_TYPE, result);

        when();
        String addedObjectOid = provisioningService.addObject(resource, null, null, task, result);

        then();
        result.computeStatus();
        display(result);
        assertSuccess(result);

        assertEquals("Wrong oid", RESOURCE_OPENDJ_BAD_CREDENTIALS_OID, addedObjectOid);
    }

    @Test
    public void test713ConnectionBadCredentials() throws Exception {
        Task task = getTestTask();

        when();
        OperationResult testResult = provisioningService.testResource(RESOURCE_OPENDJ_BAD_CREDENTIALS_OID, task, task.getResult());

        display("Test connection result (expected failure)", testResult);
        TestUtil.assertFailure(testResult);

        OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
        OperationResult connectResult = connectorResult.findSubresult(TestResourceOpNames.CONNECTOR_INITIALIZATION.getOperation());
        assertNotNull("No connector connect result", connectResult);
        // MID-4103
        assertTrue("Unexpected connector initialization message: "+connectResult.getMessage(), connectResult.getMessage().contains("invalidCredentials"));
        assertTrue("Unexpected connector initialization message: "+connectResult.getMessage(), connectResult.getMessage().contains("49"));
    }

    @Test
    public void test720AddResourceOpenDjBadBindDn() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_OPENDJ_BAD_BIND_DN_FILE);
        fillInConnectorRef(resource, IntegrationTestTools.CONNECTOR_LDAP_TYPE, result);

        when();
        String addedObjectOid = provisioningService.addObject(resource, null, null, task, result);

        then();
        result.computeStatus();
        display(result);
        assertSuccess(result);

        assertEquals("Wrong oid", RESOURCE_OPENDJ_BAD_BIND_DN_OID, addedObjectOid);
    }

    @Test
    public void test723ConnectionBadBindDn() throws Exception {
        Task task = getTestTask();

        when();
        OperationResult testResult = provisioningService.testResource(RESOURCE_OPENDJ_BAD_BIND_DN_OID, task, task.getResult());

        display("Test connection result (expected failure)", testResult);
        TestUtil.assertFailure(testResult);

        OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
        OperationResult initResult = connectorResult.findSubresult(TestResourceOpNames.CONNECTOR_INITIALIZATION.getOperation());
        // MID-4103
        assertTrue("Unexpected connector initialization message: "+initResult.getMessage(), initResult.getMessage().contains("invalidCredentials"));
        assertTrue("Unexpected connector initialization message: "+initResult.getMessage(), initResult.getMessage().contains("49"));
    }

    /** Creates two entries with `uidNumber` bigger than {@link Integer#MAX_VALUE}. MID-4424. */
    @Test
    public void test730IntegerOver32Bits() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long withinLong = 1_000_000_000_000_000L; // 10^15 = 3_8D7E_A4C6_8000 hex (51 bits)
        BigInteger overLong = new BigInteger("10").pow(30); // 10^30 ~ 102 bits

        given("account with uidNumber over Integer but within Long");
        openDJController.addEntry("dn: uid=within-long,ou=People,dc=example,dc=com\n"
                + "uid: within-long\n"
                + "cn: Within Long\n"
                + "sn: Long\n"
                + "givenName: Within\n"
                + "objectclass: top\n"
                + "objectclass: person\n"
                + "objectclass: organizationalPerson\n"
                + "objectclass: inetOrgPerson\n"
                + "objectclass: posixAccount\n"
                + "uidNumber: " + withinLong + "\n"
                + "gidNumber: 1000\n"
                + "homeDirectory: /dev/null\n"
                + "\n");

        when("it is retrieved");
        List<PrismObject<ShadowType>> objectsWithinLong =
                provisioningService.searchObjects(
                        ShadowType.class,
                        Resource.of(resource) // requires test004 to run before this test
                                .queryFor(OBJECT_CLASS_INETORGPERSON_QNAME)
                                .and().item(PATH_CN).eq("Within Long")
                                .build(),
                        null, task, result);

        then("uidNumber is OK");
        assertThat(objectsWithinLong).as("retrieved accounts").hasSize(1);
        assertShadowAfter(objectsWithinLong.get(0))
                .attributes()
                .assertValue(QNAME_UID_NUMBER, BigInteger.valueOf(withinLong));

        given("account with uidNumber over Long");
        openDJController.addEntry("dn: uid=over-long,ou=People,dc=example,dc=com\n"
                + "uid: over-long\n"
                + "cn: Over Long\n"
                + "sn: Long\n"
                + "givenName: Over\n"
                + "objectclass: top\n"
                + "objectclass: person\n"
                + "objectclass: organizationalPerson\n"
                + "objectclass: inetOrgPerson\n"
                + "objectclass: posixAccount\n"
                + "uidNumber: " + overLong + "\n"
                + "gidNumber: 1000\n"
                + "homeDirectory: /dev/null\n"
                + "\n");

        when("it is retrieved");
        List<PrismObject<ShadowType>> objectsOverLong =
                provisioningService.searchObjects(
                        ShadowType.class,
                        Resource.of(resource)
                                .queryFor(OBJECT_CLASS_INETORGPERSON_QNAME)
                                .and().item(PATH_CN).eq("Over Long")
                                .build(),
                        null, task, result);

        then("uidNumber is OK");
        assertThat(objectsOverLong).as("retrieved accounts").hasSize(1);
        assertShadowAfter(objectsOverLong.get(0))
                .attributes()
                .assertValue(QNAME_UID_NUMBER, overLong);
    }

    /** Tests the wildcard LS with DELETE event. It has no OC information, requiring very careful treatment. */
    @Test
    public void test740WildcardLiveSyncWithDeleteEvent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LiveSyncTokenStorage tokenStorage = new DummyTokenStorageImpl();
        ResourceOperationCoordinates coords = ResourceOperationCoordinates.ofResource(resource.getOid());

        var accountDef = Resource.of(resource)
                .getCompleteSchemaRequired()
                .findObjectClassDefinitionRequired(OBJECT_CLASS_INETORGPERSON_QNAME);

        given("LDAP account");
        var accountToAdd = new ShadowType()
                .resourceRef(resource.getOid(), ResourceType.COMPLEX_TYPE)
                .objectClass(OBJECT_CLASS_INETORGPERSON_QNAME);
        ShadowUtil.getOrCreateAttributesContainer(accountToAdd, accountDef)
                .addSimpleAttribute(QNAME_DN, "uid=test740,ou=People,dc=example,dc=com")
                .addSimpleAttribute(QNAME_UID, "test740")
                .addSimpleAttribute(QNAME_CN, "Test740")
                .addSimpleAttribute(QNAME_SN, "Test");
        provisioningService.addObject(accountToAdd.asPrismObject(), null, null, task, result);

        and("livesync token is fetched");
        mockLiveSyncTaskHandler.synchronize(coords, tokenStorage, task, result);
        syncServiceMock.reset();

        when("account is deleted on LDAP");
        openDJController.delete("uid=test740,ou=People,dc=example,dc=com");

        and("livesync is executed");
        mockLiveSyncTaskHandler.synchronize(coords, tokenStorage, task, result);

        then("operation is successful and there was a delete event");
        assertSuccess(result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        and("there was a model notification");
        syncServiceMock.assertNotifyChange();
    }

    /**
     * Look inside OpenDJ logs to check for clues of undesirable behavior.
     * MID-7091
     */
    @Test
    public void test900OpenDjLogSanity() throws Exception {
        MutableInt abandons = new MutableInt(0);
        openDJController.scanAccessLog(line -> {
            if (line.contains("ABANDON")) {
                abandons.increment();
            }
        });
        if (abandons.intValue() > 0) {
            fail("Too many ABANDONs in OpenDJ access log (" + abandons.intValue() + ")");
        }
    }

    // Account must be non-raw (deeply), i.e. it has to have RAC/SAssocC instead of PrismContainer for attributes/associations
    private void assertEntitlementGroup(PrismObject<ShadowType> account, String entitlementOid) {
        var associationValue = IntegrationTestTools.assertAssociationObjectRef(account, ASSOCIATION_GROUP_NAME, entitlementOid);
        var dnProp = associationValue
                .getSingleObjectShadowRequired()
                .getAttributesContainer()
                .findSimpleAttribute(getSecondaryIdentifierQName());
        assertNotNull("No DN identifier in group association in " + account, dnProp);
    }

    @SuppressWarnings("SameParameterValue")
    protected void assertConnectorOperationIncrement(int expectedIncrementSmart, int expectedIncrementDumb) {
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, expectedIncrementSmart);
    }

    private void assertDescription(Entry entry, String expectedOrigValue, String... params) {
        OpenDJController.assertAttributeLang(entry, ATTRIBUTE_DESCRIPTION_NAME, expectedOrigValue, params);
    }

    @SuppressWarnings("SameParameterValue")
    private <T> ShadowSimpleAttributeDefinition<T> getAccountAttributeDefinitionRequired(QName attrName) throws SchemaException {
        return ResourceSchemaFactory.getBareSchema(resourceBean)
                .findObjectClassDefinitionRequired(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS)
                .findSimpleAttributeDefinitionRequired(attrName);
    }

    @SuppressWarnings("SameParameterValue")
    private ObjectDelta<ShadowType> createEntitleDelta(String subjectOid, QName assocName, String objectOid)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        var object = AbstractShadow.of(
                provisioningService.getObject(
                        ShadowType.class, objectOid, createNoFetchCollection(), getTestTask(), getTestOperationResult()));
        var accountDef = Resource.of(resource)
                .getCompleteSchemaRequired()
                .getObjectTypeDefinitionRequired(ACCOUNT_DEFAULT);
        var assocDef = accountDef.findAssociationDefinitionRequired(assocName);
        return Resource.of(resource).deltaFor(accountDef.getObjectClassName())
                .item(ShadowType.F_ASSOCIATIONS, assocName)
                .add(assocDef.createValueFromFullDefaultObject(object))
                .asObjectDelta(subjectOid);
    }

    protected void assertMemberOfAttributeBare(ResourceObjectClassDefinition accountClassDefBare) {
        ShadowSimpleAttributeDefinition<?> memberOfDef = accountClassDefBare.findSimpleAttributeDefinition("isMemberOf");
        assertNotNull("No definition for isMemberOf", memberOfDef);
        assertEquals(-1, memberOfDef.getMaxOccurs());
        assertEquals(0, memberOfDef.getMinOccurs());
        assertFalse("isMemberOf create", memberOfDef.canAdd());
        assertFalse("isMemberOf update", memberOfDef.canModify());
        assertTrue("No isMemberOf read", memberOfDef.canRead());
        assertEquals("Wrong isMemberOf matching rule", PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME, memberOfDef.getMatchingRuleQName());
    }

    protected void assertMemberOfAttributeRefined(ResourceObjectDefinition accountDef) {
        ShadowSimpleAttributeDefinition<?> memberOfDef = accountDef.findSimpleAttributeDefinition("isMemberOf");
        assertNotNull("No definition for isMemberOf", memberOfDef);
        assertEquals(-1, memberOfDef.getMaxOccurs());
        assertEquals(0, memberOfDef.getMinOccurs());
        assertFalse("isMemberOf create", memberOfDef.canAdd());
        assertFalse("isMemberOf update", memberOfDef.canModify());
        assertTrue("No isMemberOf read", memberOfDef.canRead());
        assertEquals("Wrong isMemberOf matching rule", PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME, memberOfDef.getMatchingRuleQName());
    }

    protected boolean isActivationCapabilityClassSpecific() {
        return true;
    }

    protected boolean hasNativeReferences() {
        return false;
    }
}
