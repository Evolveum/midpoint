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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.test.XmlAsserts;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * 
 * @author Igor Farinic
 */
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class RepositoryResourceTest extends AbstractTestNGSpringContextTests {

	org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RepositoryResourceTest.class);

	@Autowired(required = true)
	private RepositoryService repositoryService;

	public RepositoryService getRepositoryService() {
		return repositoryService;
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

	private void compareObjects(ResourceType object, ResourceType retrievedObject) throws Exception {
		assertEquals(object.getOid(), retrievedObject.getOid());
		assertEquals(object.getName(), retrievedObject.getName());

		if (object.getExtension() != null && retrievedObject.getExtension() != null) {
			assertEquals(object.getExtension().getAny().size(), retrievedObject.getExtension().getAny()
					.size());
			List<Element> extensionElements = object.getExtension().getAny();
			int i = 0;
			for (Element element : extensionElements) {
				XmlAsserts.assertPatch(DOMUtil.serializeDOMToString(element),
						DOMUtil.serializeDOMToString(retrievedObject.getExtension().getAny().get(i)));
				i++;
			}
		} else if ((object.getExtension() != null && retrievedObject.getExtension() == null)
				|| (object.getExtension() == null && retrievedObject.getExtension() != null)) {
			Assert.fail("Extension section is null for one object but not null for other object");
		}
	}

	//FIXME: temporary solution till proper compare of JAXB objects without equals methods is implemented 
	private void compareNullObjects(ResourceType object, ResourceType retrievedObject) throws Exception {
		assertEquals(object.getSchemaHandling(), retrievedObject.getSchemaHandling());
		assertEquals(object.getConfiguration(), retrievedObject.getConfiguration());
		assertEquals(object.getSchema(), retrievedObject.getSchema());
		assertEquals(object.getScripts(), retrievedObject.getScripts());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResource() throws Exception {
		final String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			// add resource
			ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"))).getValue();
			repositoryService.addObject(resource, new OperationResult("test"));

			// get resource
			ObjectType retrievedObject = repositoryService.getObject(resourceOid,
					new PropertyReferenceListType(), new OperationResult("test"));
			compareObjects(resource, (ResourceType) retrievedObject);

			// list objects
			List<ResourceType> objects = repositoryService.listObjects(
					ResourceType.class, new PagingType(), new OperationResult("test"));
			assertNotNull(objects);
			assertEquals(1, objects.size());
			compareObjects(resource, (ResourceType) objects.get(0));

			// delete resource
			repositoryService.deleteObject(ResourceType.class, resourceOid, new OperationResult("test"));
			try {
				repositoryService.getObject(resourceOid, new PropertyReferenceListType(), new OperationResult("test"));
				Assert.fail("Object with oid " + resourceOid + " was not deleted");
			} catch (ObjectNotFoundException ex) {
				//ignore
			}
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(ResourceType.class, resourceOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResourceModification() throws Exception {
		final String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			// add object
			ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"))).getValue();
			repositoryService.addObject(resource, new OperationResult("test"));

			// get object
			ObjectType retrievedObject = repositoryService.getObject(resourceOid,
					new PropertyReferenceListType(), new OperationResult("test"));
			compareObjects(resource, (ResourceType) retrievedObject);

			// modify object
			ResourceType modifiedResource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/resource-modified-removed-tags.xml"))).getValue();
			ObjectModificationType objectModificationType = CalculateXmlDiff.calculateChanges(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"), new File(
					"src/test/resources/resource-modified-removed-tags.xml"));
			repositoryService.modifyObject(ResourceType.class, objectModificationType, new OperationResult("test"));
			retrievedObject = repositoryService.getObject(resourceOid,
					new PropertyReferenceListType(), new OperationResult("test"));
			compareObjects(modifiedResource, (ResourceType) retrievedObject);
			compareNullObjects(modifiedResource, (ResourceType) retrievedObject);

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
