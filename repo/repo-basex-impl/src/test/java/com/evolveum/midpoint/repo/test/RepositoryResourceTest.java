/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 Igor Farinic
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.repo.test;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceAccountTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType;

/**
 * 
 * @author Igor Farinic
 */
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-basex-test.xml" })
public class RepositoryResourceTest extends AbstractTestNGSpringContextTests {
	
	private static final File RESOURCE_FILE = new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml");

	private static final int NUM_TESTS = 10;

	private final String RESOURCE_OID = "aae7be60-df56-11df-8608-0002a5d5c51b";

	org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RepositoryResourceTest.class);

	@Autowired(required = true)
	private RepositoryService repositoryService;

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public RepositoryResourceTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@BeforeMethod
	public void setUp() {

	}

	@AfterMethod
	public void tearDown() {
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResource() throws Exception {
		try {
			
			PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_FILE);
			
			checkResource(resource, "before add");
			
			// ADD resource
			repositoryService.addObject(resource, new OperationResult("test"));

			checkResource(resource, "after add");
			
			// GET resource
			PrismObject<ResourceType> retrievedObject = repositoryService.getObject(ResourceType.class, RESOURCE_OID,
					new OperationResult("test"));
			
			checkResource(retrievedObject, "after get");
			
			PrismAsserts.assertEquivalent("add/get cycle: not equivalent", resource, retrievedObject);

			// LIST objects
			List<PrismObject<ResourceType>> objects = repositoryService.listObjects(
					ResourceType.class, new PagingType(), new OperationResult("test"));
			assertNotNull(objects);
			assertEquals(1, objects.size());
			checkResource(objects.get(0), "after list");
			PrismAsserts.assertEquals(resource, objects.get(0));

			// delete resource
			repositoryService.deleteObject(ResourceType.class, RESOURCE_OID, new OperationResult("test"));
			try {
				repositoryService.getObject(ObjectType.class, RESOURCE_OID, new OperationResult("test"));
				Assert.fail("Object with oid " + RESOURCE_OID + " was not deleted");
			} catch (ObjectNotFoundException ex) {
				//ignore
			}
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(ResourceType.class, RESOURCE_OID, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

// These two tests take too much time. Use only if really necessary
//	@Test
//	public void testResourceLoop() throws Exception {
//		for(int i=0; i<NUM_TESTS; i++) {
//			testResource();
//		}
//	}
//
//	@Test
//	public void testReadLoop() throws Exception {
//		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_FILE);
//		checkResource(resource, "before add");
//		// ADD resource
//		repositoryService.addObject(resource, new OperationResult("test"));
//		checkResource(resource, "after add");
//
//		for(int i=0; i<NUM_TESTS*2; i++) {			
//			// GET resource
//			PrismObject<ResourceType> retrievedObject = repositoryService.getObject(ResourceType.class, RESOURCE_OID,
//					new PropertyReferenceListType(), new OperationResult("test"));
//			checkResource(retrievedObject, "after get");
//		}
//		
//		repositoryService.deleteObject(ResourceType.class, RESOURCE_OID, new OperationResult("test"));
//	}

	
	private void checkResource(PrismObject<ResourceType> resource, String desc) {
		assertEquals("Wrong OID (prism) "+desc, RESOURCE_OID, resource.getOid());
		
		ResourceType resourceType = resource.asObjectable();
		assertEquals("Wrong OID (jaxb) "+desc, RESOURCE_OID, resourceType.getOid());
		
		SchemaHandlingType schemaHandling = resourceType.getSchemaHandling();
		assertNotNull("No schema handling (JAXB) "+desc, schemaHandling);
		assertFalse("No account types "+desc, schemaHandling.getAccountType().isEmpty());
		for(ResourceAccountTypeDefinitionType accountType: schemaHandling.getAccountType()) {
			String name = accountType.getName();
			assertNotNull("Account type without a name "+desc, name);
			assertNotNull("Account type "+name+" does not have an objectClass "+desc, accountType.getObjectClass());
		}
		
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONFIGURATION);
		assertNotNull("No configuration container", configurationContainer);
		PrismContainer<Containerable> configPropsContainer = configurationContainer.findContainer(SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No configuration properties container", configPropsContainer);
		List<Item<?>> configProps = configPropsContainer.getValue().getItems();
		assertEquals("Wrong number of config properties", 6, configProps.size());
		PrismProperty<Object> credentialsProp = configPropsContainer.findProperty(new QName(SchemaTestConstants.NS_ICFC_LDAP,"credentials"));
		assertNotNull("No credentials property", credentialsProp);
		assertEquals("Wrong number of credentials property value", 1, credentialsProp.getValues().size());
		PrismPropertyValue<Object> credentialsPropertyValue = credentialsProp.getValues().iterator().next();
		assertNotNull("No credentials property value", credentialsPropertyValue);
		Object rawElement = credentialsPropertyValue.getRawElement();
		assertTrue("Wrong element class "+rawElement.getClass(), rawElement instanceof Element);
		Element rawDomElement = (Element)rawElement;
		assertEquals("Wrong credentials element namespace", SchemaTestConstants.NS_ICFC_LDAP, rawDomElement.getNamespaceURI());
		assertEquals("Wrong credentials element local name", "credentials", rawDomElement.getLocalName());
		Element clearValueElement = DOMUtil.getChildElement(rawDomElement, ProtectedStringType.F_CLEAR_VALUE);
		assertNotNull("No clearValue element", clearValueElement);
		assertEquals("Wrong clearValue element namespace", SchemaConstants.NS_C, clearValueElement.getNamespaceURI());
		assertEquals("Wrong clearValue element local name", "clearValue", clearValueElement.getLocalName());
		assertEquals("Wrong clearValue element context", "secret", clearValueElement.getTextContent());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResourceModification() throws Exception {
		final String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			// add object
			PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_FILE);
			repositoryService.addObject(resource, new OperationResult("test"));

			// get object
			PrismObject<ResourceType> retrievedObject = repositoryService.getObject(ResourceType.class, resourceOid,
					new OperationResult("test"));
			PrismAsserts.assertEquals(resource, retrievedObject);

			// modify object
			PrismObject<ResourceType> modifiedResource = PrismTestUtil.parseObject(new File(
					"src/test/resources/resource-modified-removed-tags.xml"));
			modifiedResource.setName(modifiedResource.getDefinition().getName());
			
			ObjectDelta<ResourceType> objectModificationType = DiffUtil.diff(RESOURCE_FILE, new File(
					"src/test/resources/resource-modified-removed-tags.xml"), 
					ResourceType.class, PrismTestUtil.getPrismContext());
			
			
			repositoryService.modifyObject(ResourceType.class, objectModificationType.getOid(), 
					objectModificationType.getModifications(), new OperationResult("test"));
			retrievedObject = repositoryService.getObject(ResourceType.class, resourceOid,
					new OperationResult("test"));
			
			//we need to remove the configuration part of the resource which is parsed
			//after the real definition is applied..(the resource in the repo contain this definition,
			//but before we save the object to the repo it doesn't contain it).
//			retrievedObject.remove(retrievedObject.findItem(ResourceType.F_CONFIGURATION));
			System.out.println("retrieved: "+PrismTestUtil.getPrismContext().getPrismDomProcessor().serializeObjectToString(retrievedObject));
			System.out.println("modifiedResource: "+PrismTestUtil.getPrismContext().getPrismDomProcessor().serializeObjectToString(modifiedResource));
			PrismAsserts.assertEquivalent("Modification result does not match",modifiedResource, retrievedObject);

		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(ResourceType.class, resourceOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}
}
