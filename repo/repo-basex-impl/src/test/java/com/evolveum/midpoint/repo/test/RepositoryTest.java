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
 * 
 */

package com.evolveum.midpoint.repo.test;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * 
 * @author Igor Farinic
 */
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-basex-test.xml" })
public class RepositoryTest extends AbstractTestNGSpringContextTests {

	@Autowired(required = true)
	private RepositoryService repositoryService;

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
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

	@SuppressWarnings("unchecked")
	@Test(expectedExceptions = ObjectAlreadyExistsException.class)
	public void addObjectThatAlreadyExists() throws Exception {
		String oid = "c0c010c0-d34d-b33f-f00d-111111111111";
		try {
			// store user
			PrismObject<UserType> user = PrismTestUtil.parseObject(new File(
					"src/test/resources/user.xml"));
			repositoryService.addObject(user, new OperationResult("test"));
			
			//try to store object with the same oid again, but different name
			PrismObject<UserType> user2 = PrismTestUtil.parseObject(new File(
			"src/test/resources/user.xml"));
			repositoryService.addObject(user2, new OperationResult("test"));
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(UserType.class, oid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}

	}

	@SuppressWarnings("unchecked")
	@Test(expectedExceptions = ObjectAlreadyExistsException.class)
	public void addObjectWithTheSameName() throws Exception {
		String oid = null;
		try {
			// store user
			PrismObject<UserType> user = PrismTestUtil.parseObject(new File(
					"src/test/resources/user-without-oid.xml"));
			repositoryService.addObject(user, new OperationResult("test"));
			oid = user.getOid();
			//try to store the same object with no oid again, exception is expected
			user.setOid(null);
			repositoryService.addObject(user, new OperationResult("test"));
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(UserType.class, oid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}

	}

	
	@Test(expectedExceptions = ObjectNotFoundException.class)
	public void getNotExistingObject() throws Exception {
		String oid = "c0c010c0-d34d-b33f-f00d-111111111234";
		//try to get not existing object, exception is expected
		repositoryService.getObject(ObjectType.class, oid, new OperationResult("test"));
	}
	
	@Test
	public void listObjectsNoObjectsOfThatTypeReturnsEmptyList() throws Exception {
		List<PrismObject<ResourceType>> retrievedList = repositoryService.listObjects(ResourceType.class, null, new OperationResult("test"));
		assertNotNull(retrievedList);
		assertEquals(0, retrievedList.size());
	}	

	@Test(expectedExceptions = ObjectNotFoundException.class)
	public void modifyNotExistingObject() throws Exception {
		String oid = "c0c010c0-d34d-b33f-f00d-111111111234";
		Collection<? extends ItemDelta> mods = PropertyDelta.createModificationReplacePropertyCollection(UserType.F_FULL_NAME,
			PrismTestUtil.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class), "Foo Bar");
//		ObjectModificationType objModifications = new ObjectModificationType();
//		objModifications.setOid(oid);
//		PropertyModificationType modification = new PropertyModificationType();
//		Value value = new Value();
//		Element element = DOMUtil.getFirstChildElement(DOMUtil.parseDocument("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
//				"<fullName xmlns='http://midpoint.evolveum.com/xml/ns/public/common/common-2a'>Foo Bar</fullName>"));
//		value.getAny().add(element);
//		modification.setValue(value);
//		objModifications.getPropertyModification().add(modification);
		//try to modify not existing object, exception is expected
		repositoryService.modifyObject(UserType.class, oid, mods, new OperationResult("test"));
	}
	
	@Test(expectedExceptions = ObjectNotFoundException.class)
	public void deleteNotExistingObject() throws Exception {
		String oid = "c0c010c0-d34d-b33f-f00d-111111111234";
		//try to delete not existing object, exception is expected
		repositoryService.deleteObject(UserType.class, oid, new OperationResult("test"));	
	}

	@Test
	public void nameWithApostrophe() throws Exception {
		PrismObject<TaskType> task = PrismTestUtil.parseObject(new File(
				"src/test/resources/task-with-apostrophe.xml"));		// task name contains an apostrophe
		String oid = repositoryService.addObject(task, new OperationResult("test"));
		
		// cleanup
		repositoryService.deleteObject(TaskType.class, oid, new OperationResult("test"));
	}
	
	@Test
	public void testDynamicExtension() throws Exception {
		PrismObject<TaskType> task = PrismTestUtil.parseObject(new File(
				"src/test/resources/task-with-apostrophe.xml"));		// task name contains an apostrophe
		PrismContainer<?> extensionContainer = task.getExtension();
		
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(SchemaConstants.SYNC_TOKEN,
				SchemaConstants.SYNC_TOKEN, DOMUtil.XSD_INT, PrismTestUtil.getPrismContext());
		propDef.setDynamic(true);
		PrismProperty<Integer> tokenProperty = propDef.instantiate();
		tokenProperty.addValue(new PrismPropertyValue<Integer>(42));
		extensionContainer.add(tokenProperty);
		
		String oid = repositoryService.addObject(task, new OperationResult("test"));
		
		// WHEN
		PrismObject<TaskType> repoTask = repositoryService.getObject(TaskType.class, oid, new OperationResult("test"));
		PrismContainer<?> repoExtensionContainer = repoTask.getExtension();
		PrismProperty<Integer> repoTokenProperty = repoExtensionContainer.findProperty(SchemaConstants.SYNC_TOKEN);
		assertNotNull("No token after reading from repo", repoTokenProperty);
		PrismPropertyDefinition repoTokenDef = repoTokenProperty.getDefinition();
		assertNotNull("No token definition after reading from repo", repoTokenDef);
		assertEquals("Wrong type in token definition after reading from repo", DOMUtil.XSD_INT, repoTokenDef.getTypeName());
		assertTrue("Token definition is not dynamic after reading from repo", repoTokenDef.isDynamic());
		assertEquals("Wrong token value after reading from repo", (Integer)42, repoTokenProperty.getValues().iterator().next().getValue());
		
		// cleanup
		repositoryService.deleteObject(TaskType.class, oid, new OperationResult("test"));
	}

}
