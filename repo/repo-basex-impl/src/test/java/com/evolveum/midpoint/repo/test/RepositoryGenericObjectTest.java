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

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.XmlAsserts;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;

/**
 * 
 * @author Igor Farinic
 */

@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class RepositoryGenericObjectTest extends AbstractTestNGSpringContextTests {

	@Autowired(required = true)
	private RepositoryService repositoryService;

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public RepositoryGenericObjectTest() {
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

	private void compareObjects(GenericObjectType object, GenericObjectType retrievedObject)
			throws Exception {
		assertEquals(object.getOid(), retrievedObject.getOid());
		assertEquals(object.getName(), retrievedObject.getName());
		assertEquals(object.getObjectType(), retrievedObject.getObjectType());
		if (object.getExtension() != null && retrievedObject.getExtension() != null) {
			assertEquals(object.getExtension().getAny().size(), retrievedObject.getExtension()
					.getAny().size());
			List<Object> extensionElements = object.getExtension().getAny();
			int i = 0;
			for (Object element : extensionElements) {
				XmlAsserts.assertPatch(JAXBUtil.serializeElementToString(element),
						JAXBUtil.serializeElementToString(retrievedObject.getExtension().getAny().get(i)));
				i++;
			}
		} else if ((object.getExtension() != null && retrievedObject.getExtension() == null)
				|| (object.getExtension() == null && retrievedObject.getExtension() != null)) {
			Assert.fail("Extension section is null for one object but not null for other object");
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGenericObject() throws Exception {
		final String genericObjectOid = "c0c010c0-d34d-b33f-f00d-999111111111";
		try {

			// create object
			GenericObjectType genericObject = ((JAXBElement<GenericObjectType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/generic-object.xml"))).getValue();
			repositoryService.addObject(genericObject, new OperationResult("test"));

			// get object
			ObjectType retrievedObject = repositoryService.getObject(ObjectType.class, genericObjectOid,
					new PropertyReferenceListType(), new OperationResult("test"));
			compareObjects(genericObject, (GenericObjectType) retrievedObject);

			// list objects of type
			List<GenericObjectType> objects = repositoryService.listObjects(
					GenericObjectType.class, new PagingType(), new OperationResult("test"));
			assertNotNull(objects);
			assertEquals(1, objects.size());
			compareObjects(genericObject, (GenericObjectType) objects.get(0));

			// delete object
			repositoryService.deleteObject(GenericObjectType.class, genericObjectOid, new OperationResult("test"));
			try {
				repositoryService.getObject(ObjectType.class, genericObjectOid, new PropertyReferenceListType(), new OperationResult("test"));
				Assert.fail("Object with oid " + genericObjectOid + " was not deleted");
			} catch (ObjectNotFoundException ex) {
				//ignore
			}
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(GenericObjectType.class, genericObjectOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}
}
