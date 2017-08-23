/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.getAttributeValue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.schema.processor.*;
import org.apache.commons.lang.StringUtils;
import org.opends.server.types.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.DistinguishedNameMatchingRule;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.match.UuidMatchingRule;
import com.evolveum.midpoint.prism.match.XmlMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.DeleteCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType.Host;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;

/**
 * Test for provisioning service implementation.
 * 
 * This test will initialize mock repository and fill-in some test data. The
 * "default" repository objects cannot be used, as the new provisioning service
 * implementation assumes a slightly different connector configuration that was
 * used in the OpenIDM.
 * 
 * This test will initialize embedded OpenDJ as a target resource.
 * 
 * The test calls the new Provisioning Service Interface (java). No WSDL mess.
 * 
 * @author Radovan Semancik
 * @author Katka Valalikova
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDj extends AbstractOpenDjTest {

	private static Trace LOGGER = TraceManager.getTrace(TestOpenDj.class);
	
	private String groupSailorOid;

	@Autowired
	TaskManager taskManager;
	
	protected int getNumberOfBaseContextShadows() {
		return 0;
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		openDJController.addEntry("dn: ou=specialgroups,dc=example,dc=com\n"+
		                          "objectclass: organizationalUnit\n"+
		                          "ou: specialgroups\n");
	}
	
	@BeforeClass
	public static void startLdap() throws Exception {
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("START:  ProvisioningServiceImplOpenDJTest");
		LOGGER.info("------------------------------------------------------------------------------");
		try {
		openDJController.startCleanServer();
		} catch (IOException ex) {
			LOGGER.error("Couldn't start LDAP.", ex);
			throw ex;
		}
	}

	@AfterClass
	public static void stopLdap() throws Exception {
		openDJController.stop();
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("STOP:  ProvisioningServiceImplOpenDJTest");
		LOGGER.info("------------------------------------------------------------------------------");
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
		final String TEST_NAME = "test003Connection";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		ResourceType resourceTypeBefore = repositoryService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, result).asObjectable();
		assertNotNull("No connector ref",resourceTypeBefore.getConnectorRef());
		assertNotNull("No connector ref OID",resourceTypeBefore.getConnectorRef().getOid());
		connector = repositoryService.getObject(ConnectorType.class, resourceTypeBefore.getConnectorRef().getOid(), null, result);
		ConnectorType connectorType = connector.asObjectable();
		assertNotNull(connectorType);
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceTypeBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);
		
		OperationResult	operationResult = provisioningService.testResource(RESOURCE_OPENDJ_OID, task);
		
		display("Test connection result",operationResult);
		TestUtil.assertSuccess("Test connection failed",operationResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		
		display("Resource after testResource (repository)",resourceTypeRepoAfter);
		
		display("Resource after testResource (repository, XML)", PrismTestUtil.serializeObjectToString(resourceTypeRepoAfter.asPrismObject(), PrismContext.LANG_XML));
		
		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNotNull("No schema after test connection",xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);
		
		CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata",cachingMetadata);
		assertNotNull("No retrievalTimestamp",cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber",cachingMetadata.getSerialNumber());
		
		Element xsdElement = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		ResourceSchema parsedSchema = ResourceSchemaImpl.parse(xsdElement, resourceTypeRepoAfter.toString(), prismContext);
		assertNotNull("No schema after parsing",parsedSchema);
		
		ObjectClassComplexTypeDefinition inetOrgPersonDefinition = parsedSchema.findObjectClassDefinition(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
		assertNull("The _PASSSWORD_ attribute sneaked into schema", inetOrgPersonDefinition.findAttributeDefinition(
				new QName(SchemaConstants.NS_ICF_SCHEMA,"password")));
		assertNull("The userPassword attribute sneaked into schema", inetOrgPersonDefinition.findAttributeDefinition(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeRepoAfter),"userPassword")));
		
		assertShadows(1);
	}
	
	@Test
	public void test004ResourceAndConnectorCaching() throws Exception {
		final String TEST_NAME = "test004ResourceAndConnectorCaching";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		resource = provisioningService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, null, result);
		resourceType = resource.asObjectable();
		ConnectorInstance configuredConnectorInstance = resourceManager.getConfiguredConnectorInstance(
				resource, ReadCapabilityType.class, false, result);
		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
		ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
		assertNotNull("No resource schema", resourceSchema);
		
		// WHEN
		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, task, result);
		
		// THEN
		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
		assertNotNull("No connector ref",resourceTypeAgain.getConnectorRef());
		assertNotNull("No connector ref OID",resourceTypeAgain.getConnectorRef().getOid());
		
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainer<Containerable> configurationContainerAgain = resourceAgain.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);		
		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));
		assertTrue("Configurations not equals", configurationContainer.equals(configurationContainerAgain));

		ResourceSchema resourceSchemaAgain = RefinedResourceSchemaImpl.getResourceSchema(resourceAgain, prismContext);
		assertNotNull("No resource schema (again)", resourceSchemaAgain);
		assertEquals("Schema serial number mismatch", resourceType.getSchema().getCachingMetadata().getSerialNumber(),
				resourceTypeAgain.getSchema().getCachingMetadata().getSerialNumber());
		assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);
		
		// Now we stick our nose deep inside the provisioning impl. But we need to make sure that the
		// configured connector is properly cached
		ConnectorInstance configuredConnectorInstanceAgain = resourceManager.getConfiguredConnectorInstance(
				resourceAgain, ReadCapabilityType.class, false, result);
		assertTrue("Connector instance was not cached", configuredConnectorInstance == configuredConnectorInstanceAgain);
		
		assertShadows(1);
	}
	
	@Test
	public void test005Capabilities() throws Exception {
		final String TEST_NAME = "test005Capabilities";
		TestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult(); 

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result).asObjectable();
		
		// THEN
		display("Resource from provisioninig", resource);
		display("Resource from provisioninig (XML)", PrismTestUtil.serializeObjectToString(resource.asPrismObject(), PrismContext.LANG_XML));
		
		CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
        assertFalse("Empty capabilities returned",nativeCapabilitiesList.isEmpty());
        CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilitiesList, CredentialsCapabilityType.class);
        assertNotNull("credentials capability not found",capCred);
        PasswordCapabilityType capPassword = capCred.getPassword();
        assertNotNull("password capability not present", capPassword);
        assertPasswordCapability(capPassword);
        
        // Connector cannot do activation, this should be null
        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList, ActivationCapabilityType.class);
        assertNull("Found activation capability while not expecting it" ,capAct);
        
        ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilitiesList, ScriptCapabilityType.class);
        assertNotNull("No script capability", capScript);
        List<Host> scriptHosts = capScript.getHost();
        assertEquals("Wrong number of script hosts", 1, scriptHosts.size());
        Host scriptHost = scriptHosts.get(0);
        assertEquals("Wrong script host type", ProvisioningScriptHostType.CONNECTOR, scriptHost.getType());
//        assertEquals("Wrong script host language", ....., scriptHost.getLanguage());
        
        CreateCapabilityType capCreate = CapabilityUtil.getCapability(nativeCapabilitiesList, CreateCapabilityType.class);
        assertNotNull("No create capability", capCreate);
        
        ReadCapabilityType capRead = CapabilityUtil.getCapability(nativeCapabilitiesList, ReadCapabilityType.class);
        assertNotNull("No read capability", capRead);
        
        UpdateCapabilityType capUpdate = CapabilityUtil.getCapability(nativeCapabilitiesList, UpdateCapabilityType.class);
        assertNotNull("No update capability", capUpdate);
        
        DeleteCapabilityType capDelete = CapabilityUtil.getCapability(nativeCapabilitiesList, DeleteCapabilityType.class);
        assertNotNull("No delete capability", capDelete);
        
        
        List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resource);
        for (Object capability : effectiveCapabilities) {
        	System.out.println("Capability: "+CapabilityUtil.getCapabilityDisplayName(capability)+" : "+capability);
        }
        
        capCred = ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
        assertNotNull("credentials effective capability not found",capCred);
        assertNotNull("password effective capability not found",capCred.getPassword());
        // Although connector does not support activation, the resource specifies a way how to simulate it.
        // Therefore the following should succeed
        capAct = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
        assertNotNull("activation capability not found",capAct);
        
        PagedSearchCapabilityType capPage =  ResourceTypeUtil.getEffectiveCapability(resource, PagedSearchCapabilityType.class);
		assertNotNull("paged search capability not present", capPage);
        
        assertShadows(1);
	}
	
	protected void assertPasswordCapability(PasswordCapabilityType capPassword) {
		assertTrue("Wrong password capability readable flag: "+capPassword.isReadable(), 
				capPassword.isReadable() != Boolean.TRUE);
	}

	@Test
	public void test006Schema() throws Exception {
		final String TEST_NAME = "test006RefinedSchema";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN

		// WHEN
		ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceType, prismContext);
		display("Resource schema", resourceSchema);

		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findObjectClassDefinition(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		ResourceAttributeDefinition<String> idPrimaryDef = accountDef.findAttributeDefinition(getPrimaryIdentifierQName());
		assertEquals(1, idPrimaryDef.getMaxOccurs());
		assertEquals(0, idPrimaryDef.getMinOccurs());
		assertFalse("UID has create", idPrimaryDef.canAdd());
		assertFalse("UID has update", idPrimaryDef.canModify());
		assertTrue("No UID read", idPrimaryDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getPrimaryIdentifiers().contains(idPrimaryDef));
		assertEquals("Wrong "+OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", ProvisioningTestUtil.CONNID_UID_NAME, idPrimaryDef.getFrameworkAttributeName());
		assertEquals("Wrong primary identifier matching rule", UuidMatchingRule.NAME, idPrimaryDef.getMatchingRuleQName());


		ResourceAttributeDefinition<String> idSecondaryDef = accountDef.findAttributeDefinition(getSecondaryIdentifierQName());
		assertEquals(1, idSecondaryDef.getMaxOccurs());
		assertEquals(1, idSecondaryDef.getMinOccurs());
		assertTrue("No NAME create", idSecondaryDef.canAdd());
		assertTrue("No NAME update", idSecondaryDef.canModify());
		assertTrue("No NAME read", idSecondaryDef.canRead());
		assertTrue("NAME definition not in secondary identifiers", accountDef.getSecondaryIdentifiers().contains(idSecondaryDef));
		assertEquals("Wrong "+OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", ProvisioningTestUtil.CONNID_NAME_NAME, idSecondaryDef.getFrameworkAttributeName());
		assertEquals("Wrong secondary identifier matching rule", DistinguishedNameMatchingRule.NAME, idSecondaryDef.getMatchingRuleQName());

		ResourceAttributeDefinition<String> cnDef = accountDef.findAttributeDefinition("cn");
		assertNotNull("No definition for cn", cnDef);
		assertEquals(-1, cnDef.getMaxOccurs());
		assertEquals(1, cnDef.getMinOccurs());
		assertTrue("No cn create", cnDef.canAdd());
		assertTrue("No cn update", cnDef.canModify());
		assertTrue("No cn read", cnDef.canRead());
		assertEquals("Wrong cn matching rule", StringIgnoreCaseMatchingRule.NAME, cnDef.getMatchingRuleQName());
		
		ResourceAttributeDefinition<byte[]> jpegPhoto = accountDef.findAttributeDefinition("jpegPhoto");
		assertNotNull("No definition for jpegPhoto", jpegPhoto);
		assertEquals(-1, jpegPhoto.getMaxOccurs());
		assertEquals(0, jpegPhoto.getMinOccurs());
		assertTrue("No jpegPhoto create", jpegPhoto.canAdd());
		assertTrue("No jpegPhoto update", jpegPhoto.canModify());
		assertTrue("No jpegPhoto read", jpegPhoto.canRead());
		assertEquals("Wrong jpegPhoto matching rule", null, jpegPhoto.getMatchingRuleQName());
		
		ResourceAttributeDefinition<String> dsDef = accountDef.findAttributeDefinition("ds-pwp-account-disabled");
		assertNotNull("No definition for ds-pwp-account-disabled", dsDef);
		assertEquals(1, dsDef.getMaxOccurs());
		assertEquals(0, dsDef.getMinOccurs());
		assertTrue("No ds-pwp-account-disabled read", dsDef.canRead());
		assertTrue("No ds-pwp-account-disabled create", dsDef.canAdd());
		assertTrue("No ds-pwp-account-disabled update", dsDef.canModify());
		// TODO: MID-2358
//		assertTrue("ds-pwp-account-disabled is NOT operational", dsDef.isOperational());
				
		ResourceAttributeDefinition<String> memberOfDef = accountDef.findAttributeDefinition("isMemberOf");
		assertNotNull("No definition for isMemberOf", memberOfDef);
		assertEquals(-1, memberOfDef.getMaxOccurs());
		assertEquals(0, memberOfDef.getMinOccurs());
		assertFalse("isMemberOf create", memberOfDef.canAdd());
		assertFalse("isMemberOf update", memberOfDef.canModify());
		assertTrue("No isMemberOf read", memberOfDef.canRead());
		assertEquals("Wrong isMemberOf matching rule", DistinguishedNameMatchingRule.NAME, memberOfDef.getMatchingRuleQName());

		ResourceAttributeDefinition<String> labeledUriDef = accountDef.findAttributeDefinition("labeledURI");
		assertNotNull("No definition for labeledUri", labeledUriDef);
		assertEquals(-1, labeledUriDef.getMaxOccurs());
		assertEquals(0, labeledUriDef.getMinOccurs());
		assertTrue("No labeledUri create", labeledUriDef.canAdd());
		assertTrue("No labeledUri update", labeledUriDef.canModify());
		assertTrue("No labeledUri read", labeledUriDef.canRead());
		assertEquals("Wrong labeledUri matching rule", null, labeledUriDef.getMatchingRuleQName());

		ResourceAttributeDefinition<String> secretaryDef = accountDef.findAttributeDefinition("secretary");
		assertNotNull("No definition for secretary", secretaryDef);
		assertEquals(-1, secretaryDef.getMaxOccurs());
		assertEquals(0, secretaryDef.getMinOccurs());
		assertTrue("No secretary create", secretaryDef.canAdd());
		assertTrue("No secretary update", secretaryDef.canModify());
		assertTrue("No secretary read", secretaryDef.canRead());
		assertEquals("Wrong secretary matching rule", DistinguishedNameMatchingRule.NAME, secretaryDef.getMatchingRuleQName());
		
		ResourceAttributeDefinition<String> createTimestampDef = accountDef.findAttributeDefinition("createTimestamp");
		assertNotNull("No definition for createTimestamp", createTimestampDef);
		assertEquals(1, createTimestampDef.getMaxOccurs());
		assertEquals(0, createTimestampDef.getMinOccurs());
		assertTrue("No createTimestamp read", createTimestampDef.canRead());
		assertFalse("Bad createTimestamp create", createTimestampDef.canAdd());
		assertFalse("Bad createTimestamp update", createTimestampDef.canModify());
		assertEquals("Wrong createTimestamp matching rule", null, createTimestampDef.getMatchingRuleQName());

		assertNull("The _PASSSWORD_ attribute sneaked into schema",
				accountDef.findAttributeDefinition(new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		assertNull("The userPassword attribute sneaked into schema",
				accountDef.findAttributeDefinition(new QName(accountDef.getTypeName().getNamespaceURI(), "userPassword")));
		
		assertNull("The objectClass attribute sneaked into schema",
				accountDef.findAttributeDefinition(new QName(accountDef.getTypeName().getNamespaceURI(), "objectClass")));

		assertNull("The objectclass attribute sneaked into schema",
				accountDef.findAttributeDefinition(new QName(accountDef.getTypeName().getNamespaceURI(), "objectclass")));

		ObjectClassComplexTypeDefinition posixAccountDef = resourceSchema.findObjectClassDefinition(RESOURCE_OPENDJ_POSIX_ACCOUNT_OBJECTCLASS);
		assertNotNull("posixAccount definition is missing", posixAccountDef);
		assertNotNull("Null identifiers in posixAccount", posixAccountDef.getPrimaryIdentifiers());
		assertFalse("Empty identifiers in posixAccount", posixAccountDef.getPrimaryIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in posixAccount", posixAccountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in posixAccount", posixAccountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in posixAccount", posixAccountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in posixAccount", StringUtils.isEmpty(posixAccountDef.getNativeObjectClass()));
		assertTrue("posixAccount is not auxiliary", posixAccountDef.isAuxiliary());

		ResourceAttributeDefinition<String> posixIdPrimaryDef = posixAccountDef.findAttributeDefinition(getPrimaryIdentifierQName());
		assertEquals(1, posixIdPrimaryDef.getMaxOccurs());
		assertEquals(0, posixIdPrimaryDef.getMinOccurs());
		assertFalse("UID has create", posixIdPrimaryDef.canAdd());
		assertFalse("UID has update", posixIdPrimaryDef.canModify());
		assertTrue("No UID read", posixIdPrimaryDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getPrimaryIdentifiers().contains(posixIdPrimaryDef));
		assertEquals("Wrong "+OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", ProvisioningTestUtil.CONNID_UID_NAME, posixIdPrimaryDef.getFrameworkAttributeName());

		ResourceAttributeDefinition<String> posixIdSecondaryDef = posixAccountDef.findAttributeDefinition(getSecondaryIdentifierQName());
		assertEquals(1, posixIdSecondaryDef.getMaxOccurs());
		assertEquals(1, posixIdSecondaryDef.getMinOccurs());
		assertTrue("No NAME create", posixIdSecondaryDef.canAdd());
		assertTrue("No NAME update", posixIdSecondaryDef.canModify());
		assertTrue("No NAME read", posixIdSecondaryDef.canRead());
		assertTrue("NAME definition not in secondary identifiers", accountDef.getSecondaryIdentifiers().contains(posixIdSecondaryDef));
		assertEquals("Wrong "+OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", ProvisioningTestUtil.CONNID_NAME_NAME, posixIdSecondaryDef.getFrameworkAttributeName());

		ObjectClassComplexTypeDefinition normalDef = resourceSchema.findObjectClassDefinition(new QName(RESOURCE_NS, "normalTestingObjectClass"));
		display("normalTestingObjectClass object class def", normalDef);
		assertNotNull("No definition for normalTestingObjectClass", normalDef);
		assertNotNull("The cn attribute missing in normalTestingObjectClass",
				normalDef.findAttributeDefinition(new QName(normalDef.getTypeName().getNamespaceURI(), "cn")));
		
		ObjectClassComplexTypeDefinition hybridDef = resourceSchema.findObjectClassDefinition(new QName(RESOURCE_NS, "hybridTestingObjectClass"));
		display("Hybrid object class def", hybridDef);
		assertNotNull("No definition for hybridTestingObjectClass", hybridDef);
		assertNotNull("The cn attribute missing in hybridTestingObjectClass",
				hybridDef.findAttributeDefinition(new QName(hybridDef.getTypeName().getNamespaceURI(), "cn")));
		assertNotNull("The uuidIdentifiedAttribute attribute missing in hybridTestingObjectClass",
				hybridDef.findAttributeDefinition(new QName(hybridDef.getTypeName().getNamespaceURI(), "uuidIdentifiedAttribute")));
		
		ObjectClassComplexTypeDefinition uuidDef = resourceSchema.findObjectClassDefinition(new QName(RESOURCE_NS, "uuidIdentifiedObjectClass"));
		display("uuidIdentifiedObjectClass object class def", uuidDef);
		assertNotNull("No definition for uuidIdentifiedObjectClass", uuidDef);
		assertNotNull("The uuidIdentifiedAttribute attribute missing in uuidIdentifiedObjectClass",
				uuidDef.findAttributeDefinition(new QName(uuidDef.getTypeName().getNamespaceURI(), "uuidIdentifiedAttribute")));
		
		assertShadows(1);
	}
	
	@Test
	public void test007RefinedSchema() throws Exception {
		final String TEST_NAME = "test007RefinedSchema";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN

		// WHEN
		RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType, prismContext);
		display("Refined schema", refinedSchema);

		// Check whether it is reusing the existing schema and not parsing it
		// all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching",
				refinedSchema == RefinedResourceSchemaImpl.getRefinedSchema(resourceType, prismContext));

		RefinedObjectClassDefinition accountDef = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		RefinedAttributeDefinition<String> idPrimaryDef = accountDef.findAttributeDefinition(getPrimaryIdentifierQName());
		assertEquals(1, idPrimaryDef.getMaxOccurs());
		assertEquals(0, idPrimaryDef.getMinOccurs());
		assertFalse("UID has create", idPrimaryDef.canAdd());
		assertFalse("UID has update", idPrimaryDef.canModify());
		assertTrue("No UID read", idPrimaryDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getPrimaryIdentifiers().contains(idPrimaryDef));
		assertEquals("Wrong "+OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", ProvisioningTestUtil.CONNID_UID_NAME, idPrimaryDef.getFrameworkAttributeName());

		RefinedAttributeDefinition<String> idSecondaryDef = accountDef.findAttributeDefinition(getSecondaryIdentifierQName());
		assertEquals(1, idSecondaryDef.getMaxOccurs());
		assertEquals(1, idSecondaryDef.getMinOccurs());
		assertTrue("No NAME create", idSecondaryDef.canAdd());
		assertTrue("No NAME update", idSecondaryDef.canModify());
		assertTrue("No NAME read", idSecondaryDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(idSecondaryDef));
		assertEquals("Wrong NAME matching rule", DistinguishedNameMatchingRule.NAME, idSecondaryDef.getMatchingRuleQName());
		assertEquals("Wrong "+OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", ProvisioningTestUtil.CONNID_NAME_NAME, idSecondaryDef.getFrameworkAttributeName());

		RefinedAttributeDefinition<String> cnDef = accountDef.findAttributeDefinition("cn");
		assertNotNull("No definition for cn", cnDef);
		assertEquals(-1, cnDef.getMaxOccurs());
		assertEquals(1, cnDef.getMinOccurs());
		assertTrue("No cn create", cnDef.canAdd());
		assertTrue("No cn update", cnDef.canModify());
		assertTrue("No cn read", cnDef.canRead());
		
		ResourceAttributeDefinition<String> memberOfDef = accountDef.findAttributeDefinition("isMemberOf");
		assertNotNull("No definition for isMemberOf", memberOfDef);
		assertEquals(-1, memberOfDef.getMaxOccurs());
		assertEquals(0, memberOfDef.getMinOccurs());
		assertFalse("isMemberOf create", memberOfDef.canAdd());
		assertFalse("isMemberOf update", memberOfDef.canModify());
		assertTrue("No isMemberOf read", memberOfDef.canRead());
		assertEquals("Wrong isMemberOf matching rule", DistinguishedNameMatchingRule.NAME, memberOfDef.getMatchingRuleQName());
		
		ResourceAttributeDefinition<String> secretaryDef = accountDef.findAttributeDefinition("secretary");
		assertNotNull("No definition for secretary", secretaryDef);
		assertEquals(-1, secretaryDef.getMaxOccurs());
		assertEquals(0, secretaryDef.getMinOccurs());
		assertTrue("No secretary create", secretaryDef.canAdd());
		assertTrue("No secretary update", secretaryDef.canModify());
		assertTrue("No secretary read", secretaryDef.canRead());
		assertEquals("Wrong secretary matching rule", XmlMatchingRule.NAME, secretaryDef.getMatchingRuleQName());
		
		RefinedAttributeDefinition<String> dsDef = accountDef.findAttributeDefinition("ds-pwp-account-disabled");
		assertNotNull("No definition for cn", dsDef);
		assertEquals(1, dsDef.getMaxOccurs());
		assertEquals(0, dsDef.getMinOccurs());
		assertTrue("No ds-pwp-account-disabled create", dsDef.canAdd());
		assertTrue("No ds-pwp-account-disabled update", dsDef.canModify());
		assertTrue("No ds-pwp-account-disabled read", dsDef.canRead());
		// TODO: MID-2358
//		assertTrue("ds-pwp-account-disabled is NOT operational", dsDef.isOperational());
		assertTrue("ds-pwp-account-disabled is NOT ignored", dsDef.isIgnored());

		assertNull("The _PASSSWORD_ attribute sneaked into schema",
				accountDef.findAttributeDefinition(new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));
		
		RefinedObjectClassDefinition posixAccountDef = refinedSchema.getRefinedDefinition(RESOURCE_OPENDJ_POSIX_ACCOUNT_OBJECTCLASS);
		assertNotNull("posixAccount definition is missing", posixAccountDef);
		assertNotNull("Null identifiers in posixAccount", posixAccountDef.getPrimaryIdentifiers());
		assertFalse("Empty identifiers in posixAccount", posixAccountDef.getPrimaryIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in posixAccount", posixAccountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in posixAccount", posixAccountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in posixAccount", posixAccountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in posixAccount", StringUtils.isEmpty(posixAccountDef.getNativeObjectClass()));
		assertTrue("posixAccount is not auxiliary", posixAccountDef.isAuxiliary());

		RefinedAttributeDefinition<String> posixIdPrimaryDef = posixAccountDef.findAttributeDefinition(getPrimaryIdentifierQName());
		assertEquals(1, posixIdPrimaryDef.getMaxOccurs());
		assertEquals(0, posixIdPrimaryDef.getMinOccurs());
		assertFalse("UID has create", posixIdPrimaryDef.canAdd());
		assertFalse("UID has update", posixIdPrimaryDef.canModify());
		assertTrue("No UID read", posixIdPrimaryDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getPrimaryIdentifiers().contains(posixIdPrimaryDef));
		assertEquals("Wrong "+OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", ProvisioningTestUtil.CONNID_UID_NAME, posixIdPrimaryDef.getFrameworkAttributeName());

		RefinedAttributeDefinition<String> posixIdSecondaryDef = posixAccountDef.findAttributeDefinition(getSecondaryIdentifierQName());
		assertEquals(1, posixIdSecondaryDef.getMaxOccurs());
		assertEquals(1, posixIdSecondaryDef.getMinOccurs());
		assertTrue("No NAME create", posixIdSecondaryDef.canAdd());
		assertTrue("No NAME update", posixIdSecondaryDef.canModify());
		assertTrue("No NAME read", posixIdSecondaryDef.canRead());
		assertTrue("NAME definition not in secondary identifiers", accountDef.getSecondaryIdentifiers().contains(posixIdSecondaryDef));
		assertEquals("Wrong "+OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", ProvisioningTestUtil.CONNID_NAME_NAME, posixIdSecondaryDef.getFrameworkAttributeName());

		assertShadows(1);
	}


	@Test
	public void test020ListResourceObjects() throws Exception {
		final String TEST_NAME = "test020ListResourceObjects";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		List<PrismObject<? extends ShadowType>> objectList = provisioningService.listResourceObjects(
				RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS, null, task, result);
		
		// THEN
		assertNotNull(objectList);
		assertFalse("Empty list returned",objectList.isEmpty());
		display("Resource object list "+RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS,objectList);
		
		assertShadows(1 + getNumberOfBaseContextShadows());
	}


	@Test
	public void test110GetObject() throws Exception {
		final String TEST_NAME = "test110GetObject";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType objectToAdd = parseObjectType(ACCOUNT_JBOND_FILE, ShadowType.class);

		display(SchemaDebugUtil.prettyPrint(objectToAdd));
		display(objectToAdd.asPrismObject().debugDump());
		
		String addedObjectOid = provisioningService.addObject(objectToAdd.asPrismObject(), null, null, task, result);
		assertEquals(ACCOUNT_JBOND_OID, addedObjectOid);
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		ShadowType provisioningShadow = provisioningService.getObject(ShadowType.class, ACCOUNT_JBOND_OID, null, task, result).asObjectable();

		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertNotNull(provisioningShadow);
		display("Account provisioning", provisioningShadow);
		
		PrismAsserts.assertEqualsPolyString("Name not equals.", "uid=jbond,ou=People,dc=example,dc=com", provisioningShadow.getName());
		
		final String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
		
        assertNotNull(provisioningShadow.getOid());
        assertNotNull(provisioningShadow.getName());
        assertEquals(new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME), provisioningShadow.getObjectClass());
        assertEquals(RESOURCE_OPENDJ_OID, provisioningShadow.getResourceRef().getOid());
        String idPrimaryVal = getAttributeValue(provisioningShadow, getPrimaryIdentifierQName());
        assertNotNull("No primary identifier ("+getPrimaryIdentifierQName().getLocalPart()+")", idPrimaryVal);
        String idSecondaryVal = getAttributeValue(provisioningShadow, getSecondaryIdentifierQName());
        assertNotNull("No secondary ("+getSecondaryIdentifierQName().getLocalPart()+")", idSecondaryVal);
        // Capitalization is the same as returned by OpenDJ
        assertEquals("Wrong secondary identifier", "uid=jbond,ou=People,dc=example,dc=com", idSecondaryVal);
        assertEquals("Wrong LDAP uid", "jbond", getAttributeValue(provisioningShadow, new QName(resourceNamespace, "uid")));
        assertEquals("Wrong LDAP cn", "James Bond", getAttributeValue(provisioningShadow, new QName(resourceNamespace, "cn")));
        assertEquals("Wrong LDAP sn", "Bond", getAttributeValue(provisioningShadow, new QName(resourceNamespace, "sn")));        
        assertNotNull("Missing activation", provisioningShadow.getActivation());
        assertNotNull("Missing activation status", provisioningShadow.getActivation().getAdministrativeStatus());
        assertEquals("Not enabled", ActivationStatusType.ENABLED, provisioningShadow.getActivation().getAdministrativeStatus());
        assertShadowPassword(provisioningShadow);
		
        ShadowType repoShadow = repositoryService.getObject(ShadowType.class, provisioningShadow.getOid(), null, result).asObjectable();
        display("Account repo", repoShadow);
        assertEquals(new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME), repoShadow.getObjectClass());
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());
        idPrimaryVal = getAttributeValue(repoShadow, getPrimaryIdentifierQName());
        assertNotNull("No primary identifier ("+getPrimaryIdentifierQName().getLocalPart()+") (repo)", idPrimaryVal);
        idSecondaryVal = getAttributeValue(repoShadow, getSecondaryIdentifierQName());
        assertNotNull("No secondary ("+getSecondaryIdentifierQName().getLocalPart()+") (repo)", idSecondaryVal);
        // must be all lowercase
        assertEquals("Wrong secondary identifier (repo)", "uid=jbond,ou=people,dc=example,dc=com", idSecondaryVal);

        assertShadows(2 + getNumberOfBaseContextShadows());        
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
		assertNull("Unexpected password value in "+provisioningShadow+": "+passwordValue, passwordValue);
	}

	/**
	 * Let's try to fetch object that does not exist in the repository.
	 */
	@Test
	public void test111GetObjectNotFoundRepo() throws Exception {
		final String TEST_NAME = "test111GetObjectNotFoundRepo";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		try {
			ObjectType object = provisioningService.getObject(ObjectType.class, NON_EXISTENT_OID, null, task, result).asObjectable();
			Assert.fail("Expected exception, but haven't got one");
		} catch (ObjectNotFoundException e) {
			// This is expected

			// Just to close the top-level result.
			result.recordFatalError("Error :-)");

			System.out.println("NOT FOUND REPO result:");
			System.out.println(result.debugDump());

			assertFalse(result.hasUnknownStatus());
			// TODO: check result
		} catch (CommunicationException e) {
			Assert.fail("Expected ObjectNotFoundException, but got" + e);
		} catch (SchemaException e) {
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
		final String TEST_NAME = "test112GetObjectNotFoundResource";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		try {
			ObjectType object = provisioningService.getObject(ObjectType.class, ACCOUNT_BAD_OID, null, task, result).asObjectable();
			Assert.fail("Expected exception, but haven't got one");
		} catch (ObjectNotFoundException e) {
			// This is expected

			// Just to close the top-level result.
			result.recordFatalError("Error :-)");

			System.out.println("NOT FOUND RESOURCE result:");
			System.out.println(result.debugDump());

			assertFalse(result.hasUnknownStatus());
			// TODO: check result
		} catch (CommunicationException e) {
			Assert.fail("Expected ObjectNotFoundException, but got" + e);
		} catch (SchemaException e) {
			Assert.fail("Expected ObjectNotFoundException, but got" + e);
		} finally {
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_JBOND_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
		}

		assertShadows(0 + getNumberOfBaseContextShadows());
	}

	@Test
	public void test120AddAccountWill() throws Exception {
		final String TEST_NAME = "test120AddAccountWill";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType object = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

		ShadowType repoShadowType =  repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal (repo)", "uid=will,ou=People,dc=example,dc=com", repoShadowType.getName());
		assertAttribute(repoShadowType, getSecondaryIdentifierQName(), StringUtils.lowerCase(ACCOUNT_WILL_DN));

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
				null, task, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", "uid=will,ou=People,dc=example,dc=com", provisioningAccountType.getName());
		
		assertShadows(1 + getNumberOfBaseContextShadows());
	}

	@Test
	public void test121RenameAccountWillOnResource() throws Exception{
		String TEST_NAME = "test121RenameAccountWillOnResource";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		openDJController.executeRenameChange(new File(TEST_DIR, "rename.ldif").getPath());
		
		Entry entry = openDJController.fetchEntry("uid=will123,ou=People,dc=example,dc=com");
		assertNotNull("Entry with dn uid=will123,ou=People,dc=example,dc=com does not exist", entry);
		
		ShadowType repoShadowType =  repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal (repo)", "uid=will,ou=People,dc=example,dc=com", repoShadowType.getName());
		assertAttribute(repoShadowType, getSecondaryIdentifierQName(), StringUtils.lowerCase(ACCOUNT_WILL_DN));

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
				null, task, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", "uid=will123,ou=People,dc=example,dc=com", provisioningAccountType.getName());
		assertAttribute(provisioningAccountType, getSecondaryIdentifierQName(), "uid=will123,ou=People,dc=example,dc=com");
		
		repoShadowType =  repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal (repo after provisioning)", "uid=will123,ou=People,dc=example,dc=com", repoShadowType.getName());
		assertAttribute(repoShadowType, getSecondaryIdentifierQName(), "uid=will123,ou=people,dc=example,dc=com");
		
		assertShadows(1 + getNumberOfBaseContextShadows());
	}
	
	@Test
	public void test125AddObjectNull() throws Exception {
		final String TEST_NAME = "test125AddObjectNull";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		String addedObjectOid = null;
		
		try {
		
			// WHEN
			addedObjectOid = provisioningService.addObject(null, null, null, task, result);
			
			Assert.fail("Expected IllegalArgumentException but haven't got one.");
		} catch(IllegalArgumentException ex){
			assertEquals("Object to add must not be null.", ex.getMessage());
		}
		
		assertShadows(1 + getNumberOfBaseContextShadows());
	}
	
	@Test
	public void test130AddDeleteAccountSparrow() throws Exception {
		final String TEST_NAME = "test130AddDeleteAccountSparrow";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ShadowType object = parseObjectType(ACCOUNT_SPARROW_FILE, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
		assertEquals(ACCOUNT_SPARROW_OID, addedObjectOid);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_SPARROW_OID, null, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		ShadowType objType = null;

		try {
			provisioningService.getObject(ShadowType.class, ACCOUNT_SPARROW_OID,
					null, task, result);
			Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
		} catch (ObjectNotFoundException ex) {
			display("Expected exception", ex);
		}

		try {
			objType = repositoryService.getObject(ShadowType.class, ACCOUNT_SPARROW_OID,
					null, result).asObjectable();
			// objType = container.getObject();
			Assert.fail("Expected exception, but haven't got one.");
		} catch (ObjectNotFoundException ex) {
			display("Expected exception", ex);
            assertTrue(ex.getMessage().contains(ACCOUNT_SPARROW_OID));
		}
		
		// Account shadow + shadow for base context
		assertShadows(2);
	}

	@Test
	public void test140AddAndModifyAccountJack() throws Exception {
		final String TEST_NAME = "test140AddAndModifyAccountJack";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType object = unmarshallValueFromFile(ACCOUNT_JACK_FILE, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
		assertEquals(ACCOUNT_JACK_OID, addedObjectOid);

		ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(ACCOUNT_JACK_CHANGE_FILE, ObjectModificationType.COMPLEX_TYPE);
		ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, object.asPrismObject().getDefinition());
		
		ItemPath icfNamePath = new ItemPath(
				ShadowType.F_ATTRIBUTES, getSecondaryIdentifierQName());
		PrismPropertyDefinition icfNameDef = object
				.asPrismObject().getDefinition().findPropertyDefinition(icfNamePath);
		ItemDelta renameDelta = PropertyDelta.createModificationReplaceProperty(icfNamePath, icfNameDef, "uid=rename,ou=People,dc=example,dc=com");
		((Collection)delta.getModifications()).add(renameDelta);
		
		display("Object change",delta);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
				delta.getModifications(), null, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		ShadowType accountType = provisioningService.getObject(ShadowType.class,
				ACCOUNT_JACK_OID, null, taskManager.createTaskInstance(), result).asObjectable();
		
		display("Object after change",accountType);
		
		String uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());
		List<Object> snValues = ShadowUtil.getAttributeValues(accountType, new QName(RESOURCE_NS, "sn"));
		assertNotNull("No 'sn' attribute", snValues);
		assertFalse("Surname attributes must not be empty", snValues.isEmpty());
		assertEquals(1, snValues.size());
		
		//check icf_name in the shadow object fetched only from the repository
		ShadowType repoShadow = repositoryService.getObject(ShadowType.class, objectChange.getOid(), null, result).asObjectable();
		String name = ShadowUtil.getSingleStringAttributeValue(repoShadow, getSecondaryIdentifierQName());
		assertEquals("After rename, dn is not equal.", "uid=rename,ou=people,dc=example,dc=com", name);
		assertEquals("shadow name not changed after rename", "uid=rename,ou=People,dc=example,dc=com", repoShadow.getName().getOrig());
		
		String changedSn = (String) snValues.get(0);
		
		assertNotNull(uid);
		
		// Check if object was modified in LDAP
		
		Entry response = openDJController.searchAndAssertByEntryUuid(uid);			
		display("LDAP account", response);
		
		OpenDJController.assertAttribute(response, "sn", "First");
		
		assertEquals("First", changedSn);
		
		assertShadows(3);
	}
	
	@Test
	public void test145ModifyAccountJackJpegPhoto() throws Exception {
		final String TEST_NAME = "test145ModifyAccountJackJpegPhoto";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		byte[] bytesIn = Files.readAllBytes(Paths.get(ProvisioningTestUtil.DOT_JPG_FILENAME));
		display("Bytes in", MiscUtil.binaryToHex(bytesIn));
		
		QName jpegPhotoQName = new QName(RESOURCE_OPENDJ_NS, "jpegPhoto");
		PropertyDelta<byte[]> jpegPhotoDelta = new PropertyDelta<>(new ItemPath(ShadowType.F_ATTRIBUTES, jpegPhotoQName), 
				null , prismContext);
		jpegPhotoDelta.setValueToReplace(new PrismPropertyValue<byte[]>(bytesIn));
		
		Collection<? extends ItemDelta> modifications = MiscSchemaUtil.createCollection(jpegPhotoDelta);
		
		display("Modifications",modifications);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, ACCOUNT_JACK_OID,
				modifications, null, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		Entry entry = openDJController.searchByUid("rename");
		display("LDAP Entry", entry);
		byte[] jpegPhotoLdap = OpenDJController.getAttributeValueBinary(entry, "jpegPhoto");
		assertNotNull("No jpegPhoto in LDAP entry", jpegPhotoLdap);
		assertEquals("Byte length changed (LDAP)", bytesIn.length, jpegPhotoLdap.length);
		assertTrue("Bytes do not match (LDAP)", Arrays.equals(bytesIn, jpegPhotoLdap));
		
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class,
				ACCOUNT_JACK_OID, null, taskManager.createTaskInstance(), result);
		
		display("Object after change",shadow);
		
		PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		PrismProperty<byte[]> jpegPhotoAttr = attributesContainer.findProperty(jpegPhotoQName);
		byte[] bytesOut = jpegPhotoAttr.getValues().get(0).getValue();
		
		display("Bytes out", MiscUtil.binaryToHex(bytesOut));
		
		assertEquals("Byte length changed (shadow)", bytesIn.length, bytesOut.length);
		assertTrue("Bytes do not match (shadow)", Arrays.equals(bytesIn, bytesOut));
		
		assertShadows(3);
	}
	
	/**
	 * Make a duplicate modification. Add a givenName value that is already there.
	 * Normal LDAP should fail. So check that connector and midPoitn handles that.
	 */
	@Test
	public void test147ModifyAccountJackGivenNameDuplicit() throws Exception {
		final String TEST_NAME = "test147ModifyAccountJackGivenNameDuplicit";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		PropertyDelta<String> givenNameDelta = new PropertyDelta<>(
				new ItemPath(ShadowType.F_ATTRIBUTES, new QName(RESOURCE_OPENDJ_NS, "givenName")), 
				null , prismContext);
		givenNameDelta.addValueToAdd(new PrismPropertyValue<String>("Jack"));
		
		// Also make an ordinary non-conflicting modification. We need to make sure that
		// the operation was not ignored as a whole
		PropertyDelta<String> titleDelta = new PropertyDelta<>(
				new ItemPath(ShadowType.F_ATTRIBUTES, new QName(RESOURCE_OPENDJ_NS, "title")), 
				null , prismContext);
		titleDelta.addValueToAdd(new PrismPropertyValue<String>("Great Captain"));
		
		Collection<? extends ItemDelta> modifications = MiscSchemaUtil.createCollection(givenNameDelta, titleDelta);
		
		display("Modifications",modifications);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, ACCOUNT_JACK_OID,
				modifications, null, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		Entry entry = openDJController.searchByUid("rename");
		display("LDAP Entry", entry);
		OpenDJController.assertAttribute(entry, "givenName", "Jack");
		OpenDJController.assertAttribute(entry, "title", "Great Captain");
		
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class,
				ACCOUNT_JACK_OID, null, taskManager.createTaskInstance(), result);
		
		display("Object after change",shadow);
		
		PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		PrismAsserts.assertPropertyValue(attributesContainer, new QName(RESOURCE_OPENDJ_NS, "givenName"), "Jack");
		PrismAsserts.assertPropertyValue(attributesContainer, new QName(RESOURCE_OPENDJ_NS, "title"), "Great Captain");
		
		assertShadows(3);
	}


	@Test
	public void test150ChangePassword() throws Exception {
		final String TEST_NAME = "test150ChangePassword";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType object = parseObjectType(ACCOUNT_MODIFY_PASSWORD_FILE, ShadowType.class);

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);

		assertEquals(ACCOUNT_MODIFY_PASSWORD_OID, addedObjectOid);
		
		ShadowType accountType = provisioningService.getObject(ShadowType.class,
				ACCOUNT_MODIFY_PASSWORD_OID, null, taskManager.createTaskInstance(), result).asObjectable();
		
		display("Object before password change",accountType);
		
		String uid = null;
		uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());
		assertNotNull(uid);
		
		Entry entryBefore = openDJController.searchAndAssertByEntryUuid(uid);			
		display("LDAP account before", entryBefore);

		String passwordBefore = OpenDJController.getAttributeValue(entryBefore, "userPassword");
		assertNull("Unexpected password before change",passwordBefore);
		
		ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "account-change-password.xml"), ObjectModificationType.COMPLEX_TYPE);
		ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, accountType.asPrismObject().getDefinition());
		display("Object change",delta);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(), null, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		
		// Check if object was modified in LDAP
		Entry entryAfter = openDJController.searchAndAssertByEntryUuid(uid);			
		display("LDAP account after", entryAfter);

		String passwordAfter = OpenDJController.getAttributeValue(entryAfter, "userPassword");
		assertNotNull("The password was not changed",passwordAfter);
		
		System.out.println("Changed password: "+passwordAfter);
		
		openDJController.assertPassword(entryAfter.getDN().toString(), "mehAbigH4X0R");

		assertShadows(4);
	}

	@Test
	public void test151AddObjectWithPassword() throws Exception {
		final String TEST_NAME = "test151AddObjectWithPassword";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType object = parseObjectType(ACCOUNT_NEW_WITH_PASSWORD_FILE, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		assertEquals(ACCOUNT_NEW_WITH_PASSWORD_OID, addedObjectOid);

		ShadowType accountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID,
				null, result).asObjectable();
//			assertEquals("lechuck", accountType.getName());
		PrismAsserts.assertEqualsPolyString("Name not equal.", "uid=lechuck,ou=People,dc=example,dc=com", accountType.getName());

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID,
				null,taskManager.createTaskInstance(), result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", "uid=lechuck,ou=People,dc=example,dc=com", provisioningAccountType.getName());
//			assertEquals("lechuck", provisioningAccountType.getName());
		
		String uid = null;
		for (Object e : accountType.getAttributes().getAny()) {
			if (getPrimaryIdentifierQName().equals(JAXBUtil.getElementQName(e))) {
				uid = ((Element)e).getTextContent();
			}
		}
		assertNotNull(uid);
		
		// Check if object was created in LDAP and that there is a password
		
		Entry entryAfter = openDJController.searchAndAssertByEntryUuid(uid);			
		display("LDAP account after", entryAfter);

		String passwordAfter = OpenDJController.getAttributeValue(entryAfter, "userPassword");
		assertNotNull("The password was not changed",passwordAfter);
		
		System.out.println("Account password: "+passwordAfter);
		
		openDJController.assertPassword(entryAfter.getDN().toString(), "t4k30v3rTh3W0rld");
			
		assertShadows(5);
	}
	
	@Test
    public void test160SearchAccountsIterative() throws Exception {
		final String TEST_NAME = "test160SearchAccountsIterative";
		TestUtil.displayTestTitle(TEST_NAME);
		
        // GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
        final String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
        QName objectClass = new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME);

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), objectClass, prismContext);
        
        final Collection<ObjectType> objects = new HashSet<ObjectType>();

        ResultHandler handler = new ResultHandler<ObjectType>() {

            @Override
            public boolean handle(PrismObject<ObjectType> prismObject, OperationResult parentResult) {
                ObjectType objectType = prismObject.asObjectable();
                objects.add(objectType);

                display("Found object", objectType);

                assertTrue(objectType instanceof ShadowType);
                ShadowType shadow = (ShadowType) objectType;
                assertNotNull(shadow.getOid());
                assertNotNull(shadow.getName());
                assertEquals(new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME), shadow.getObjectClass());
                assertEquals(RESOURCE_OPENDJ_OID, shadow.getResourceRef().getOid());
                String idPrimaryVal = getAttributeValue(shadow, getPrimaryIdentifierQName());
                assertNotNull("No primary identifier ("+getPrimaryIdentifierQName().getLocalPart()+")", idPrimaryVal);
                String idSecondaryVal = getAttributeValue(shadow, getSecondaryIdentifierQName());
                assertNotNull("No secondary ("+getSecondaryIdentifierQName().getLocalPart()+")", idSecondaryVal);
                assertEquals("Wrong shadow name", idSecondaryVal.toLowerCase(), shadow.getName().getOrig().toLowerCase());
                assertNotNull("Missing LDAP uid", getAttributeValue(shadow, new QName(resourceNamespace, "uid")));
                assertNotNull("Missing LDAP cn", getAttributeValue(shadow, new QName(resourceNamespace, "cn")));
                assertNotNull("Missing LDAP sn", getAttributeValue(shadow, new QName(resourceNamespace, "sn")));
                assertNotNull("Missing activation", shadow.getActivation());
                assertNotNull("Missing activation status", shadow.getActivation().getAdministrativeStatus());
                assertEquals("Not enabled", ActivationStatusType.ENABLED, shadow.getActivation().getAdministrativeStatus());
                return true;
            }
        };

        // WHEN
        provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, task, result);

        // THEN
        display("Count", objects.size());
        assertEquals("Unexpected number of shadows", 9, objects.size());

        // The extra shadow is a group shadow 
        assertShadows(11);
        
        // Bad things may happen, so let's check if the shadow is still there and that is has the same OID
        PrismObject<ShadowType> accountNew = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, taskManager.createTaskInstance(), result);
    }

	protected void assertShadows(int expectedCount) throws SchemaException {
		OperationResult result = new OperationResult(TestOpenDj.class.getName() + ".assertShadows");
		int actualCount = repositoryService.countObjects(ShadowType.class, null, result);
		if (actualCount != expectedCount) {
			ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
				@Override
				public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
					display("Repo shadow", object);
					return true;
				}
			};
			repositoryService.searchObjectsIterative(ShadowType.class, null, handler, null, false, result);
			assertEquals("Unexpected number of shadows in the repo", expectedCount, actualCount);
		}
	}

	@Test
	public void test170DisableAccount() throws Exception{
		final String TEST_NAME = "test170DisableAccount";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType object = parseObjectType(ACCOUNT_DISABLE_SIMULATED_FILE, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		assertEquals(ACCOUNT_DISABLE_SIMULATED_OID, addedObjectOid);

		ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(
                REQUEST_DISABLE_ACCOUNT_SIMULATED_FILE, ObjectModificationType.COMPLEX_TYPE);
		ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, object.asPrismObject().getDefinition());
		display("Object change",delta);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
				delta.getModifications(), null, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		ShadowType accountType = provisioningService.getObject(ShadowType.class,
				ACCOUNT_DISABLE_SIMULATED_OID, null, taskManager.createTaskInstance(), result).asObjectable();
		
		display("Object after change",accountType);
		
		assertEquals("The account was not disabled in the shadow", ActivationStatusType.DISABLED, accountType.getActivation().getAdministrativeStatus());
		
		String uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		
		// Check if object was modified in LDAP
		
		Entry response = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP account", response);
		
		String disabled = openDJController.getAttributeValue(response, "ds-pwp-account-disabled");
		assertNotNull("no ds-pwp-account-disabled attribute in account "+uid, disabled);

        display("ds-pwp-account-disabled after change: " + disabled);

        assertEquals("ds-pwp-account-disabled not set to \"TRUE\"", "TRUE", disabled);
        
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, ACCOUNT_DISABLE_SIMULATED_OID, null, result);
        ActivationType repoActivation = repoShadow.asObjectable().getActivation();
        assertNotNull("No activation in repo", repoActivation);
        XMLGregorianCalendar repoDisableTimestamp = repoActivation.getDisableTimestamp();
        assertNotNull("No activation disableTimestamp in repo", repoDisableTimestamp);
        assertEquals("Wrong activation disableTimestamp in repo", 
        		XmlTypeConverter.createXMLGregorianCalendar(2001, 2, 3, 4, 5, 6), repoDisableTimestamp);
	}

	@Test
	public void test175AddDisabledAccount() throws Exception {
		final String TEST_NAME = "test175AddDisabledAccount";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType object = parseObjectType(ACCOUNT_NEW_DISABLED_FILE, ShadowType.class);

		IntegrationTestTools.display("Adding object", object);

		// WHEN
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		
		// THEN
		assertEquals(ACCOUNT_NEW_DISABLED_OID, addedObjectOid);

		ShadowType accountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_DISABLED_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (repo)", "uid=rapp,ou=People,dc=example,dc=com", accountType.getName());

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_DISABLED_OID,
				null, taskManager.createTaskInstance(), result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (provisioning)", "uid=rapp,ou=People,dc=example,dc=com", provisioningAccountType.getName());
		
		String uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		
		// Check if object was modified in LDAP
		
		Entry response = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP account", response);
		
		String disabled = openDJController.getAttributeValue(response, "ds-pwp-account-disabled");
		assertNotNull("no ds-pwp-account-disabled attribute in account "+uid, disabled);

        System.out.println("ds-pwp-account-disabled after change: " + disabled);

        assertEquals("ds-pwp-account-disabled not set to \"TRUE\"", "TRUE", disabled);
        
        ActivationType repoActivation = accountType.getActivation();
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
		final String TEST_NAME = "test176AddEnabledAccount";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType object = parseObjectType(ACCOUNT_NEW_ENABLED_FILE, ShadowType.class);

		IntegrationTestTools.display("Adding object", object);

		// WHEN
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		
		// THEN
		assertEquals(ACCOUNT_NEW_ENABLED_OID, addedObjectOid);

		ShadowType accountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_ENABLED_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (repo)", "uid=cook,ou=People,dc=example,dc=com", accountType.getName());

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_ENABLED_OID,
				null, taskManager.createTaskInstance(), result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (provisioning)", "uid=cook,ou=People,dc=example,dc=com", provisioningAccountType.getName());
		
		String uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		
		// Check if object was modified in LDAP
		
		Entry response = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP account", response);
		
		String disabled = openDJController.getAttributeValue(response, "ds-pwp-account-disabled");
		assertEquals("ds-pwp-account-disabled not set to \"FALSE\"", "FALSE", disabled);
	}
	
	@Test
	public void test180GetUnlockedAccount() throws Exception {
		final String TEST_NAME = "test180GetUnlockedAccount";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
				null, task, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		
		PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, 
				LockoutStatusType.NORMAL);
	}
	
	@Test
	public void test182GetLockedAccount() throws Exception {
		final String TEST_NAME = "test182GetLockedAccount";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		openDJController.executeLdifChange(
				"dn: uid=will123,ou=People,dc=example,dc=com\n" +
				"changetype: modify\n" +
				"replace: pager\n" +
				"pager: 1"
				);

		// WHEN
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
				null, task, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		
		PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, 
				LockoutStatusType.LOCKED);
	}
	
	@Test
	public void test184UnlockAccount() throws Exception{
		final String TEST_NAME = "test184UnlockAccount";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, prismContext, LockoutStatusType.NORMAL);

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(),
				delta.getModifications(), null, null, task, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, taskManager.createTaskInstance(), result);
		
		display("Object after change",shadow);
		
		String uid = ShadowUtil.getSingleStringAttributeValue(shadow.asObjectable(), getPrimaryIdentifierQName());		
		assertNotNull(uid);
		
		// Check if object was modified in LDAP
		
		Entry response = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP account", response);
		
		String pager = openDJController.getAttributeValue(response, "pager");
		assertNull("Pager attribute found in account "+uid+": "+pager, pager);
		
		PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, 
				LockoutStatusType.NORMAL);
	}
	
	
	@Test
	public void test200SearchObjectsIterative() throws Exception {
		final String TEST_NAME = "test200SearchObjectsIterative";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ShadowType object = parseObjectType(ACCOUNT_SEARCH_ITERATIVE_FILE, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		assertEquals(ACCOUNT_SEARCH_ITERATIVE_OID, addedObjectOid);

		final List<ShadowType> objectTypeList = new ArrayList<ShadowType>();

		QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		provisioningService.searchObjectsIterative(ShadowType.class, query, null, new ResultHandler<ShadowType>() {

			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {

				return objectTypeList.add(object.asObjectable());
			}
		}, task, result);

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
		final String TEST_NAME = "test201SearchObjects";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType object = parseObjectType(ACCOUNT_SEARCH_FILE, ShadowType.class); 

		display("New object", object);
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		assertEquals(ACCOUNT_SEARCH_OID, addedObjectOid);

		QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

		// WHEN
		SearchResultList<PrismObject<ShadowType>> searchResults = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		display("Search resutls", searchResults);
		
		assertEquals("Unexpected number of search results", 14, searchResults.size());
		
		assertConnectorOperationIncrement(1, 29);
		assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
	}

	@Test
	public void test202SearchObjectsCompexFilter() throws Exception {
		final String TEST_NAME = "test202SearchObjectsCompexFilter";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_COMPLEX_FILTER_FILE,
                QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		provisioningService.applyDefinition(ShadowType.class, query, task, result);

		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		List<PrismObject<ShadowType>> objListType = 
			provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
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
		final String TEST_NAME = "test203SearchObjectsByDnExists";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ObjectQuery query = createAccountShadowQuerySecondaryIdentifier(ACCOUNT_BARBOSSA_DN, resource);

		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		List<PrismObject<ShadowType>> objListType = 
			provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
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
		final String TEST_NAME = "test205SearchObjectsByDnNotExists";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ObjectQuery query = createAccountShadowQuerySecondaryIdentifier(
				"uid=DoesNOTeXXXiSt,ou=People,dc=example,dc=com", resource);

		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		List<PrismObject<ShadowType>> objListType = 
			provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		for (PrismObject<ShadowType> objType : objListType) {
			assertNotNull("Null search result", objType);
			display("found object", objType);
		}
		
		assertEquals("Unexpected number of objects found", 0, objListType.size());
		
		assertConnectorOperationIncrement(1, 1);
		assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
	}

	@Test
	public void test230SearchObjectsPagedNoOffset() throws Exception {
		final String TEST_NAME = "test230SearchObjectsPagedNoOffset";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		ObjectPaging paging = ObjectPaging.createPaging(null, 3);
		query.setPaging(paging);
		
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		List<PrismObject<ShadowType>> searchResults = 
			provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		assertSuccess(result);
		display("Search resutls", searchResults);
		
		assertSearchResults(searchResults, "cook", "drake", "hbarbossa" );
		
		assertConnectorOperationIncrement(1, 7);
		assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
	}
	
	@Test
	public void test231SearchObjectsPagedOffsetZero() throws Exception {
		final String TEST_NAME = "test231SearchObjectsPagedOffsetZero";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		ObjectPaging paging = ObjectPaging.createPaging(0, 4);
		query.setPaging(paging);
		
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		List<PrismObject<ShadowType>> searchResults = 
			provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		assertSuccess(result);
		display("Search resutls", searchResults);
		
		assertSearchResults(searchResults, "cook", "drake", "hbarbossa", "idm");
		
		assertConnectorOperationIncrement(1, 9);
		assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
	}
	
	@Test
	public void test232SearchObjectsPagedOffset() throws Exception {
		final String TEST_NAME = "test232SearchObjectsPagedOffset";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		ObjectPaging paging = ObjectPaging.createPaging(2, 5);
		query.setPaging(paging);
		
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> searchResults = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		assertSuccess(result);
		display("Search resutls", searchResults);
		
		// The results should be this:
		assertSearchResults(searchResults, "hbarbossa", "idm", "jbeckett", "jbond", "jgibbs" );
		
		assertConnectorOperationIncrement(1, 11);
		assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
	}
	
	@Test
	public void test233SearchObjectsPagedNoOffsetSortSn() throws Exception {
		final String TEST_NAME = "test233SearchObjectsPagedNoOffsetSortSn";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		ObjectPaging paging = ObjectPaging.createPaging(null, 4);
		paging.setOrdering(ObjectOrdering.createOrdering(
				new ItemPath(ShadowType.F_ATTRIBUTES, new QName(RESOURCE_NS, "sn")), OrderDirection.ASCENDING));
		query.setPaging(paging);
		
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		List<PrismObject<ShadowType>> searchResults = 
			provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		assertSuccess(result);
		display("Search resutls", searchResults);
		
		assertSearchResults(searchResults, "monk", "hbarbossa", "jbeckett", "jbond" );
		
		assertConnectorOperationIncrement(1, 9);
		assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
	}
	
	@Test
	public void test234SearchObjectsPagedOffsetSortSn() throws Exception {
		final String TEST_NAME = "test234SearchObjectsPagedOffsetSortSn";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		ObjectPaging paging = ObjectPaging.createPaging(2, 4);
		paging.setOrdering(ObjectOrdering.createOrdering(
				new ItemPath(ShadowType.F_ATTRIBUTES, new QName(RESOURCE_NS, "sn")), OrderDirection.ASCENDING));
		query.setPaging(paging);
		
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		List<PrismObject<ShadowType>> searchResults = 
			provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		assertSuccess(result);
		display("Search resutls", searchResults);
		
		assertSearchResults(searchResults, "jbeckett", "jbond", "cook", "drake" );
		
		assertConnectorOperationIncrement(1, 9);
		assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
	}
		
	private void assertSearchResults(List<PrismObject<ShadowType>> searchResults, String... expectedUids) {
		assertEquals("Unexpected number of search results", expectedUids.length, searchResults.size());
		int i = 0;
		for (PrismObject<ShadowType> searchResult: searchResults) {
			assertShadow(searchResult);
			ResourceAttribute<String> uidAttr = ShadowUtil.getAttribute(searchResult, new QName(RESOURCE_NS, "uid"));
			String uid = uidAttr.getRealValues().iterator().next();
			display("found uid", uid);
			assertEquals("Wrong uid (index "+i+")", expectedUids[i], uid);
			i++;
		}
	}
	
	@Test
	public void test250CountObjects() throws Exception {
		final String TEST_NAME = "test250CountObjects";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_ALL_ACCOUNTS_FILE, QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);

		// WHEN
		Integer count = provisioningService.countObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		
		assertEquals("Unexpected number of search results", (Integer)14, count);
	}
	
	/**
	 * The exception comes from the resource. There is no shadow for this object.
	 */
	@Test
	public void test300AddObjectObjectAlreadyExistResource() throws Exception {
		final String TEST_NAME = "test300AddObjectObjectAlreadyExistResource";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_SEARCH_FILE);
		display("Account to add", account);
		
		try {
			// WHEN
			provisioningService.addObject(account, null, null, task, result);
			
			AssertJUnit.fail("Expected addObject operation to fail but it was successful");
			
		} catch (ObjectAlreadyExistsException e) {
			// This is expected
			display("Expected exception", e);
			
			// The exception should originate from the LDAP layers
			IntegrationTestTools.assertInMessageRecursive(e, "LDAP");
		}
		
		// TODO: search to check that the shadow with the same NAME exists (search for OID will not do)

	}
	
	@Test
	public void test310AddObjectNoSn() throws Exception {
		final String TEST_NAME = "test310AddObjectNoSn";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_NO_SN_FILE);
		display("Account to add", account);
		
		try {
			// WHEN
			provisioningService.addObject(account, null, null, task, result);
			
			AssertJUnit.fail("Expected addObject operation to fail but it was successful");
			
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
			
			// This error should be detectable before it reaches a resource. Therefore we check that the
			// cause was not a LDAP exception
			
			// MID-1007
//			IntegrationTestTools.assertNotInMessageRecursive(e, "LDAP");
		}
		
		// TODO: search to check that the shadow with the same NAME exists (search for OID will not do)

	}
	
	@Test
	public void test320AddAccountPosix() throws Exception {
		final String TEST_NAME = "test320AddAccountPosix";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType object = parseObjectType(ACCOUNT_POSIX_MCMUTTON_FILE, ShadowType.class);
		display("Adding account", object);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertEquals(ACCOUNT_POSIX_MCMUTTON_OID, addedObjectOid);

		ShadowType repoShadowType =  repositoryService.getObject(ShadowType.class, ACCOUNT_POSIX_MCMUTTON_OID,
				null, result).asObjectable();
		display("Repo shadow", repoShadowType);
		PrismAsserts.assertEqualsPolyString("Name not equal (repo)", ACCOUNT_POSIX_MCMUTTON_DN, repoShadowType.getName());
		assertAttribute(repoShadowType, getSecondaryIdentifierQName(), StringUtils.lowerCase(ACCOUNT_POSIX_MCMUTTON_DN));
		MidPointAsserts.assertObjectClass(repoShadowType, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS, RESOURCE_OPENDJ_POSIX_ACCOUNT_OBJECTCLASS);

		ShadowType provisioningShadowType = provisioningService.getObject(ShadowType.class, ACCOUNT_POSIX_MCMUTTON_OID,
				null, task, result).asObjectable();
		display("Provisioning shadow", provisioningShadowType);
		PrismAsserts.assertEqualsPolyString("Name not equal.", ACCOUNT_POSIX_MCMUTTON_DN, provisioningShadowType.getName());
		MidPointAsserts.assertObjectClass(provisioningShadowType, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS, RESOURCE_OPENDJ_POSIX_ACCOUNT_OBJECTCLASS);
		assertAttribute(provisioningShadowType, "cn", "Haggis McMutton");
		assertAttribute(provisioningShadowType, "sn", "McMutton");
		assertAttribute(provisioningShadowType, "homeDirectory", "/home/scotland");
		assertAttribute(provisioningShadowType, "uidNumber", 1001);
		
		String uid = ShadowUtil.getSingleStringAttributeValue(repoShadowType, getPrimaryIdentifierQName());
		assertNotNull(uid);
		
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
		final String TEST_NAME = "test322ModifyAccountPosix";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(ACCOUNT_POSIX_MCMUTTON_CHANGE_FILE, ObjectModificationType.COMPLEX_TYPE);
		ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, getShadowDefinition());
		
		display("Object change",delta);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
				delta.getModifications(), null, null, taskManager.createTaskInstance(), result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		ShadowType accountType = provisioningService.getObject(ShadowType.class,
				ACCOUNT_POSIX_MCMUTTON_OID, null, task, result).asObjectable();
		display("Object after change",accountType);

		String uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());
		assertNotNull(uid);
		assertAttribute(accountType, "cn", "Haggis McMutton");
		assertAttribute(accountType, "homeDirectory", "/home/caribbean");
		assertAttribute(accountType, "roomNumber", "Barber Shop");
		assertAttribute(accountType, "uidNumber", 1001);
		
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
		final String TEST_NAME = "test329DeleteAccountPosix";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_POSIX_MCMUTTON_OID, null, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		try {
			provisioningService.getObject(ShadowType.class, ACCOUNT_POSIX_MCMUTTON_OID, null, task, result);
			Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
		} catch (ObjectNotFoundException ex) {
			display("Expected exception", ex);
		}

		try {
			repositoryService.getObject(ShadowType.class, ACCOUNT_POSIX_MCMUTTON_OID, null, result);
			// objType = container.getObject();
			Assert.fail("Expected exception, but haven't got one.");
		} catch (ObjectNotFoundException ex) {
			display("Expected exception", ex);
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
		final String TEST_NAME = "test330SearchForPosixAccount";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_VANHELGEN_FILE,
                QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		provisioningService.applyDefinition(ShadowType.class, query, task, result);
		
		Entry entry = openDJController.addEntryFromLdifFile(ACCOUNT_POSIX_VANHELGEN_LDIF_FILE);

		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
		
		// WHEN
		List<PrismObject<ShadowType>> objListType = 
			provisioningService.searchObjects(ShadowType.class, query, null, null, result);
		
		// THEN
		for (PrismObject<ShadowType> objType : objListType) {
			assertNotNull("Null search result", objType);
			display("found object", objType);
		}
		
		assertEquals("Unexpected number of objects found", 1, objListType.size());
		
		PrismObject<ShadowType> provisioningShadow = objListType.get(0);
		assertAttribute(provisioningShadow, "cn", "Edward Van Helgen");
		assertAttribute(provisioningShadow, "homeDirectory", "/home/vanhelgen");
		assertAttribute(provisioningShadow, "uidNumber", 1002);
		
		assertConnectorOperationIncrement(1, 3);
		assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
		
		assertShadows(17);
	}
	
	// TODO: synchronization of auxiliary object classes
	
	
	
	@Test
	public void test400AddGroupSwashbucklers() throws Exception {
		final String TEST_NAME = "test400AddGroupSwashbucklers";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ShadowType object = parseObjectType(GROUP_SWASHBUCKLERS_FILE, ShadowType.class);
		IntegrationTestTools.display("Adding object", object);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertEquals(GROUP_SWASHBUCKLERS_OID, addedObjectOid);

		ShadowType shadowType =  repositoryService.getObject(ShadowType.class, GROUP_SWASHBUCKLERS_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (repo)", GROUP_SWASHBUCKLERS_DN, shadowType.getName());

		PrismObject<ShadowType> provisioningShadow = provisioningService.getObject(ShadowType.class, GROUP_SWASHBUCKLERS_OID,
				null, taskManager.createTaskInstance(), result);
		ShadowType provisioningShadowType = provisioningShadow.asObjectable();
		assertEquals("Wrong ICF name (provisioning)", dnMatchingRule.normalize(GROUP_SWASHBUCKLERS_DN), 
				dnMatchingRule.normalize(provisioningShadowType.getName().getOrig()));
		
		String uid = ShadowUtil.getSingleStringAttributeValue(shadowType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		ResourceAttribute<Object> memberAttr = ShadowUtil.getAttribute(provisioningShadow, new QName(RESOURCE_OPENDJ_NS, GROUP_MEMBER_ATTR_NAME));
		assertNull("Member attribute sneaked in", memberAttr);
		
		Entry ldapEntry = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP group", ldapEntry);
		assertNotNull("No LDAP group entry");
		String groupDn = ldapEntry.getDN().toString();
		assertEquals("Wrong group DN", dnMatchingRule.normalize(GROUP_SWASHBUCKLERS_DN), dnMatchingRule.normalize(groupDn));
		
		assertShadows(18);
	}
	
	@Test
	public void test402AddAccountMorganWithAssociation() throws Exception {
		final String TEST_NAME = "test402AddAccountMorganWithAssociation";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType object = parseObjectType(ACCOUNT_MORGAN_FILE, ShadowType.class);
		IntegrationTestTools.display("Adding object", object);

		// WHEN
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
		
		// THEN
		assertEquals(ACCOUNT_MORGAN_OID, addedObjectOid);

		ShadowType shadowType =  repositoryService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (repo)", ACCOUNT_MORGAN_DN, shadowType.getName());

		ShadowType provisioningShadowType = provisioningService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID,
				null, taskManager.createTaskInstance(), result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (provisioning)",
				ACCOUNT_MORGAN_DN, provisioningShadowType.getName());
		
		String uid = ShadowUtil.getSingleStringAttributeValue(shadowType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		
		List<ShadowAssociationType> associations = provisioningShadowType.getAssociation();
		assertEquals("Unexpected number of associations", 1, associations.size());
		ShadowAssociationType association = associations.get(0);
		assertEquals("Wrong group OID in association", GROUP_SWASHBUCKLERS_OID, association.getShadowRef().getOid());
		
		Entry accountEntry = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP account", accountEntry);
		assertNotNull("No LDAP account entry");
		String accountDn = accountEntry.getDN().toString();
		assertEquals("Wrong account DN", ACCOUNT_MORGAN_DN, accountDn);
		
		Entry groupEntry = openDJController.fetchEntry(GROUP_SWASHBUCKLERS_DN);
		display("LDAP group", groupEntry);
		assertNotNull("No LDAP group entry");
		openDJController.assertUniqueMember(groupEntry, accountDn);
		
		assertShadows(19);
	}
	
	@Test
	public void test405GetGroupSwashbucklers() throws Exception {
		final String TEST_NAME = "test405GetGroupSwashbucklers";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<ShadowType> provisioningShadow = provisioningService.getObject(ShadowType.class, GROUP_SWASHBUCKLERS_OID,
				null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		ShadowType provisioningShadowType = provisioningShadow.asObjectable();
		assertEquals("Wrong ICF name (provisioning)",  dnMatchingRule.normalize(GROUP_SWASHBUCKLERS_DN),
				dnMatchingRule.normalize(provisioningShadowType.getName().getOrig()));
		
		String uid = ShadowUtil.getSingleStringAttributeValue(provisioningShadowType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		ResourceAttribute<Object> memberAttr = ShadowUtil.getAttribute(provisioningShadow, new QName(RESOURCE_OPENDJ_NS, GROUP_MEMBER_ATTR_NAME));
		assertNull("Member attribute sneaked in", memberAttr);
		
		Entry ldapEntry = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP group", ldapEntry);
		assertNotNull("No LDAP group entry");
		String groupDn = ldapEntry.getDN().toString();
		assertEquals("Wrong group DN", dnMatchingRule.normalize(GROUP_SWASHBUCKLERS_DN), dnMatchingRule.normalize(groupDn));
		
		assertShadows(19);
	}

	@Test
	public void test410CreateLdapGroupAndSearchGroups() throws Exception {
		final String TEST_NAME = "test410CreateLdapGroupAndSearchGroups";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		openDJController.addEntry("dn: cn=seadogs,ou=groups,dc=EXAMPLE,dc=com\n" +
								"objectClass: groupOfUniqueNames\n" +
								"objectClass: top\n" +
								"cn: seadogs");
		
		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_OPENDJ_OID, 
				RESOURCE_OPENDJ_GROUP_OBJECTCLASS, prismContext);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> resultList = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
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
		final String TEST_NAME = "test412CreateLdapGroupWithMemberAndGet";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		openDJController.addEntry("dn: cn=sailor,ou=Groups,dc=example,dc=com\n" +
								"objectClass: groupOfUniqueNames\n" +
								"objectClass: top\n" +
								"cn: sailor\n" +
								"uniqueMember: uid=MOrgan,ou=PEOPLE,dc=example,dc=com");
		
		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_OPENDJ_OID, 
				RESOURCE_OPENDJ_GROUP_OBJECTCLASS, prismContext);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		assertSuccess(result);
		display("Account shadow after", shadow);
		
		assertShadows(21);
		
		PrismObject<ShadowType> groupSailorShadow = findShadowByName(RESOURCE_OPENDJ_GROUP_OBJECTCLASS, "cn=sailor,ou=groups,dc=example,dc=com", resource, result);
		display("Group shadow", groupSailorShadow);
		groupSailorOid = groupSailorShadow.getOid();
		
		assertEntitlementGroup(shadow, groupSailorOid);
		
		assertShadows(21);
	}
	

	@Test
	public void test414AddGroupCorsairsAssociateUser() throws Exception {
		final String TEST_NAME = "test414AddGroupCorsairsAssociateUser";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ShadowType object = parseObjectType(GROUP_CORSAIRS_FILE, ShadowType.class);
		IntegrationTestTools.display("Adding object", object);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertEquals(GROUP_CORSAIRS_OID, addedObjectOid);

		ShadowType shadowType =  repositoryService.getObject(ShadowType.class, GROUP_CORSAIRS_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (repo)", GROUP_CORSAIRS_DN, shadowType.getName());

		// Do NOT read provisioning shadow here. We want everything to be "fresh"
		
		assertShadows(22);
	}

	@Test
	public void test416AssociateUserToCorsairs() throws Exception {
		final String TEST_NAME = "test416AssociateUserToCorsairs";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_MORGAN_OID, 
				ASSOCIATION_GROUP_NAME, GROUP_CORSAIRS_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		
		Entry groupEntry = openDJController.fetchEntry(GROUP_CORSAIRS_DN);
		display("LDAP group", groupEntry);
		assertNotNull("No LDAP group entry");
		openDJController.assertUniqueMember(groupEntry, ACCOUNT_MORGAN_DN);
		
		assertShadows(22);
	}
	
	@Test
	public void test418GetMorgan() throws Exception {
		final String TEST_NAME = "test418GetMorgan";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		assertSuccess(result);
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
		final String TEST_NAME = "test429DeleteAccountMorgan";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
	
		// WHEN
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_MORGAN_OID, null, null, task, result);

		ShadowType objType = null;

		try {
			objType = provisioningService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID,
					null, task, result).asObjectable();
			Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
		} catch (ObjectNotFoundException ex) {
			System.out.println("Catched ObjectNotFoundException.");
			assertNull(objType);
		}

		try {
			objType = repositoryService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID,
					null, result).asObjectable();
			// objType = container.getObject();
			Assert.fail("Expected exception, but haven't got one.");
		} catch (Exception ex) {
			assertNull(objType);
            assertEquals(ex.getClass(), ObjectNotFoundException.class);
            assertTrue(ex.getMessage().contains(ACCOUNT_MORGAN_OID));
		}
		
		Entry groupEntry = openDJController.fetchEntry(GROUP_SWASHBUCKLERS_DN);
		display("LDAP group", groupEntry);
		assertNotNull("No LDAP group entry");
		openDJController.assertNoUniqueMember(groupEntry, ACCOUNT_MORGAN_DN);
		
		assertShadows(21);
	}
	
	@Test
	public void test450ListGroupsObjectclass() throws Exception {
		final String TEST_NAME = "test450ListGroupsObjectclass";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_OPENDJ_OID, 
        		RESOURCE_OPENDJ_GROUP_OBJECTCLASS, prismContext);
        display("query", query);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> objects = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		display("found objects", objects);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertEquals("Wrong number of objects found", 5, objects.size());
		
		assertShadows(21);
	}
	
	@Test
	public void test452ListLdapGroupsKindIntent() throws Exception {
		final String TEST_NAME = "test452ListLdapGroupsKindIntent";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID,
        		ShadowKindType.ENTITLEMENT, "ldapGroup", prismContext);
        display("query", query);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> objects = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		display("found objects", objects);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertEquals("Wrong number of objects found", 5, objects.size());
		
		assertShadows(21);
	}

	@Test
	public void test454ListSpecialGroupsKindIntent() throws Exception {
		final String TEST_NAME = "test454ListSpecialGroupsKindIntent";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID,
        		ShadowKindType.ENTITLEMENT, "specialGroup", prismContext);
        display("query", query);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> objects = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
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
		final String TEST_NAME = "test456AddGroupSpecialists";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ShadowType object = parseObjectType(GROUP_SPECIALISTS_FILE, ShadowType.class);
		IntegrationTestTools.display("Adding object", object);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertEquals(GROUP_SPECIALISTS_OID, addedObjectOid);

		ShadowType shadowType =  repositoryService.getObject(ShadowType.class, GROUP_SPECIALISTS_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (repo)", GROUP_SPECIALISTS_DN, shadowType.getName());

		PrismObject<ShadowType> provisioningShadow = provisioningService.getObject(ShadowType.class, GROUP_SPECIALISTS_OID,
				null, taskManager.createTaskInstance(), result);
		ShadowType provisioningShadowType = provisioningShadow.asObjectable();
		assertEquals("Wrong ICF name (provisioning)", dnMatchingRule.normalize(GROUP_SPECIALISTS_DN), 
				dnMatchingRule.normalize(provisioningShadowType.getName().getOrig()));
		
		String uid = ShadowUtil.getSingleStringAttributeValue(shadowType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		ResourceAttribute<Object> memberAttr = ShadowUtil.getAttribute(provisioningShadow, new QName(RESOURCE_OPENDJ_NS, GROUP_MEMBER_ATTR_NAME));
		assertNull("Member attribute sneaked in", memberAttr);
		
		Entry ldapEntry = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP group", ldapEntry);
		assertNotNull("No LDAP group entry");
		String groupDn = ldapEntry.getDN().toString();
		assertEquals("Wrong group DN", dnMatchingRule.normalize(GROUP_SPECIALISTS_DN), dnMatchingRule.normalize(groupDn));
		
		assertShadows(23);
	}
	
	@Test
	public void test457ListLdapGroupsKindIntent() throws Exception {
		final String TEST_NAME = "test457ListLdapGroupsKindIntent";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID,
        		ShadowKindType.ENTITLEMENT, "ldapGroup", prismContext);
        display("query", query);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> objects = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		display("found objects", objects);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertEquals("Wrong number of objects found", 5, objects.size());
		
		assertShadows(23);
	}

	@Test
	public void test458ListSpecialGroupsKindIntent() throws Exception {
		final String TEST_NAME = "test458ListSpecialGroupsKindIntent";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID,
        		ShadowKindType.ENTITLEMENT, "specialGroup", prismContext);
        display("query", query);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> objects = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		display("found objects", objects);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		// Check that none of the normal LDAP groups appear here ... even if they have the same objectclass
		assertEquals("Wrong number of objects found", 1, objects.size());
		
		// Discovered base context for specialgroups
		assertShadows(23);
	}
	
	/**
	 * List organizationUnits with intent ou-people. There are no sub-ous in People.
	 * But the definition has objectclass organizationalUnit and it has baseContext that
	 * is also organizationalUnit. This test therefore makes sure this will not end up
	 * in endless loop (stack overflow).
	 */
	@Test
	public void test460ListOrganizationalUnitPeopleKindIntent() throws Exception {
		final String TEST_NAME = "test460ListOrganizationalUnitPeopleKindIntent";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID,
        		ShadowKindType.GENERIC, "ou-people", prismContext);
        display("query", query);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> objects = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		display("found objects", objects);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		// Just the ou=People itself
		assertEquals("Wrong number of objects found", 1, objects.size());
		
		assertShadows(24);
	}
	
	@Test
	public void test701ConfiguredCapabilityNoRead() throws Exception{
		final String TEST_NAME = "test701ConfiguredCapabilityNoRead";
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		addResourceFromFile(new File(TEST_DIR, "resource-opendj-no-read.xml"), IntegrationTestTools.CONNECTOR_LDAP_TYPE, true, result);
		
		try {
			provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
					null, task, result);
			AssertJUnit
					.fail("Expected unsupported operation exception, but haven't got one.");
		} catch (SystemException ex) {
				// this is expected..				
		}
	}

	@Test
	public void test702ConfiguredCapabilityNoCreate() throws Exception{
		final String TEST_NAME = "test702ConfiguredCapabilityNoCreate";
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		addResourceFromFile(new File(TEST_DIR, "/resource-opendj-no-create.xml"), IntegrationTestTools.CONNECTOR_LDAP_TYPE, true, result);
		
		try {
			PrismObject<ShadowType> shadow = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class).asPrismObject();
			provisioningService.addObject(shadow, null, null, task, result);
			AssertJUnit
					.fail("Expected unsupported operation exception, but haven't got one.");
		} catch (SystemException ex) {
			LOGGER.info("exception: {}", ex.getMessage(), ex);
				// this is expected..				
		}
	}
	
	@Test
	public void test703ConfiguredCapabilityNoDelete() throws Exception{
		final String TEST_NAME = "test703ConfiguredCapabilityNoDelete";
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		addResourceFromFile(new File(TEST_DIR, "/resource-opendj-no-delete.xml"), IntegrationTestTools.CONNECTOR_LDAP_TYPE, true, result);
		
		try {
			provisioningService.deleteObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, task, result);
			AssertJUnit
					.fail("Expected unsupported operation exception, but haven't got one.");
		} catch (SystemException ex) {
				// this is expected..				
		}
	}
	
	@Test
	public void test704ConfiguredCapabilityNoUpdate() throws Exception{
		final String TEST_NAME = "test704ConfiguredCapabilityNoUpdate";
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		addResourceFromFile(new File(TEST_DIR, "/resource-opendj-no-update.xml"), IntegrationTestTools.CONNECTOR_LDAP_TYPE, true, result);		
		
		try {
			PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(new ItemPath(ShadowType.F_ATTRIBUTES, new QName(resourceType.getNamespace(), "sn")), prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class), "doesnotmatter");
			Collection modifications = MiscUtil.createCollection(delta);
			provisioningService.modifyObject(ShadowType.class, ACCOUNT_WILL_OID, modifications, null, null, task, result);
			AssertJUnit
					.fail("Expected unsupported operation exception, but haven't got one.");
		} catch (SystemException ex) {
				// this is expected..				
		}
	}
	
	@Test
	public void test710AddResourceOpenDjBadCredentials() throws Exception {
		final String TEST_NAME = "test710AddResourceOpenDjBadCredentials";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_OPENDJ_BAD_CREDENTIALS_FILE);
		fillInConnectorRef(resource, IntegrationTestTools.CONNECTOR_LDAP_TYPE, result);

		// WHEN
		String addedObjectOid = provisioningService.addObject(resource, null, null, task, result);
		
		// THEN
		result.computeStatus();
		display(result);
		assertSuccess(result);
		
		assertEquals("Wrong oid", RESOURCE_OPENDJ_BAD_CREDENTIALS_OID, addedObjectOid);
	}
	
	@Test
	public void test713ConnectionBadCredentials() throws Exception {
		final String TEST_NAME = "test713ConnectionBadCredentials";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		OperationResult	testResult = provisioningService.testResource(RESOURCE_OPENDJ_BAD_CREDENTIALS_OID, task);
		
		display("Test connection result (expected failure)",testResult);
		TestUtil.assertFailure(testResult);
		
		OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
		OperationResult connectResult = connectorResult.findSubresult(ConnectorTestOperation.CONNECTOR_CONFIGURATION.getOperation());
		assertNotNull("No connector connect result", connectResult);
		// MID-4103
//		assertTrue("Unexpected connector initialization message: "+connectResult.getMessage(), connectResult.getMessage().contains("invalidCredentials"));
//		assertTrue("Unexpected connector initialization message: "+connectResult.getMessage(), connectResult.getMessage().contains("49"));
	}
	
	@Test
	public void test720AddResourceOpenDjBadBindDn() throws Exception {
		final String TEST_NAME = "test720AddResourceOpenDjBadBindDn";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_OPENDJ_BAD_BIND_DN_FILE);
		fillInConnectorRef(resource, IntegrationTestTools.CONNECTOR_LDAP_TYPE, result);

		// WHEN
		String addedObjectOid = provisioningService.addObject(resource, null, null, task, result);
		
		// THEN
		result.computeStatus();
		display(result);
		assertSuccess(result);
		
		assertEquals("Wrong oid", RESOURCE_OPENDJ_BAD_BIND_DN_OID, addedObjectOid);
	}
	
	@Test
	public void test723ConnectionBadBindDn() throws Exception {
		final String TEST_NAME = "test723ConnectionBadBindDn";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		OperationResult	testResult = provisioningService.testResource(RESOURCE_OPENDJ_BAD_BIND_DN_OID, task);
		
		display("Test connection result (expected failure)",testResult);
		TestUtil.assertFailure(testResult);
		
		OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
		OperationResult initResult = connectorResult.findSubresult(ConnectorTestOperation.CONNECTOR_CONFIGURATION.getOperation());
		// MID-4103
//		assertTrue("Unexpected connector initialization message: "+initResult.getMessage(), initResult.getMessage().contains("invalidCredentials"));
//		assertTrue("Unexpected connector initialization message: "+initResult.getMessage(), initResult.getMessage().contains("49"));
	}
	
	protected void assertEntitlementGroup(PrismObject<ShadowType> account, String entitlementOid) {
		ShadowAssociationType associationType = IntegrationTestTools.assertAssociation(account, ASSOCIATION_GROUP_NAME, entitlementOid);
		PrismContainerValue identifiersCVal = associationType.getIdentifiers().asPrismContainerValue();
		PrismProperty<String> dnProp = identifiersCVal.findProperty(getSecondaryIdentifierQName());
		assertNotNull("No DN identifier in group association in "+account+", got "+identifiersCVal, dnProp);
		
	}
	
	protected void assertConnectorOperationIncrement(int expectedIncrementSmart, int expectedIncrementDumb) {
		assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, expectedIncrementSmart);
	}
}
