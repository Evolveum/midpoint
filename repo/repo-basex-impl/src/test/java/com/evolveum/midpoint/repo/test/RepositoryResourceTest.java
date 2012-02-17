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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.diff.CalculateXmlDiff;
import com.evolveum.midpoint.test.util.PrismAsserts;
import com.evolveum.midpoint.test.util.PrismTestUtil;
import com.evolveum.midpoint.test.util.XmlAsserts;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
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

	org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RepositoryResourceTest.class);

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

	@Test
	@SuppressWarnings("unchecked")
	public void testResource() throws Exception {
		final String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			// add resource
			PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"));
			repositoryService.addObject(resource, new OperationResult("test"));

			// get resource
			PrismObject<ResourceType> retrievedObject = repositoryService.getObject(ResourceType.class, resourceOid,
					new PropertyReferenceListType(), new OperationResult("test"));
			PrismAsserts.assertEquals(resource, retrievedObject);

			// list objects
			List<PrismObject<ResourceType>> objects = repositoryService.listObjects(
					ResourceType.class, new PagingType(), new OperationResult("test"));
			assertNotNull(objects);
			assertEquals(1, objects.size());
			PrismAsserts.assertEquals(resource, objects.get(0));

			// delete resource
			repositoryService.deleteObject(ResourceType.class, resourceOid, new OperationResult("test"));
			try {
				repositoryService.getObject(ObjectType.class, resourceOid, new PropertyReferenceListType(), new OperationResult("test"));
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
			PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"));
			repositoryService.addObject(resource, new OperationResult("test"));

			// get object
			PrismObject<ResourceType> retrievedObject = repositoryService.getObject(ResourceType.class, resourceOid,
					new PropertyReferenceListType(), new OperationResult("test"));
			PrismAsserts.assertEquals(resource, retrievedObject);

			// modify object
			PrismObject<ResourceType> modifiedResource = PrismTestUtil.parseObject(new File(
					"src/test/resources/resource-modified-removed-tags.xml"));
			ObjectDelta<ResourceType> objectModificationType = DiffUtil.diff(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"), new File(
					"src/test/resources/resource-modified-removed-tags.xml"), 
					ResourceType.class, PrismTestUtil.getPrismContext());
			
			repositoryService.modifyObject(ResourceType.class, objectModificationType, new OperationResult("test"));
			retrievedObject = repositoryService.getObject(ResourceType.class, resourceOid,
					new PropertyReferenceListType(), new OperationResult("test"));
			
			PrismAsserts.assertEquals(modifiedResource, retrievedObject);

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
