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
import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;

/**
 * 
 * @author Igor Farinic
 */
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-basex-test.xml" })
public class RepositoryUserTest extends AbstractTestNGSpringContextTests {

	private static final String PIRACY_NS = "http://midpoint.evolveum.com/xml/ns/samples/piracy";
	private static final QName PIRACY_SHIP = new QName(PIRACY_NS,"ship");
	private static final QName PIRACY_LOOT = new QName(PIRACY_NS,"loot");
	
	@Autowired(required = true)
	private RepositoryService repositoryService;

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public RepositoryUserTest() {
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
	@Test
	public void testUser() throws Exception {
		displayTestTile("testUser");
		String oid = "c0c010c0-d34d-b33f-f00d-111111111111";
		try {
			//get actual number of user objects in the repo
			
			PagingType pagingType = PagingTypeFactory.createPaging(0, 5, OrderDirectionType.ASCENDING, "name");
			List<PrismObject<UserType>> objects = repositoryService.listObjects(UserType.class,
					pagingType, new OperationResult("test"));
			int actualSize = objects.size();

			//add new user object
			PrismObject<UserType> user = PrismTestUtil.parseObject(new File(
					"src/test/resources/user.xml"));
			repositoryService.addObject(user, new OperationResult("test"));

			//get the object
			PrismObject<UserType> retrievedObject = repositoryService.getObject(UserType.class, oid, new OperationResult("test"));
			
			PrismAsserts.assertEquals(user, retrievedObject);

			//list the objects
			objects = repositoryService.listObjects(UserType.class,
					pagingType, new OperationResult("test"));
			boolean oidTest = false;

			//check if user under test is retrieved from the repo
			for (PrismObject<UserType> o : objects) {
				if (oid.equals(o.getOid())) {
					oidTest = true;
				}
			}
			assertTrue(oidTest);
			assertEquals(actualSize + 1, objects.size());

			// delete object
			repositoryService.deleteObject(UserType.class, oid, new OperationResult("test"));
			try {
				repositoryService.getObject(ObjectType.class, oid, new OperationResult("test"));
				Assert.fail("Object with oid " + oid + " was not deleted");
			} catch (ObjectNotFoundException ex) {
				//ignore
			}		
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
	@Test
	public void testUserAddExtension() throws Exception {
		displayTestTile("testUserAddExtension");
		String oid = "c0c010c0-d34d-b33f-f00d-222222222222";
		try {
			//store user without extension
			PrismObject<UserType> user = PrismTestUtil.parseObject(new File(
					"src/test/resources/user-without-extension.xml"));
			repositoryService.addObject(user, new OperationResult("test"));
			PrismObject<UserType> retrievedObject = repositoryService.getObject(UserType.class, oid, new OperationResult("test"));
			PrismAsserts.assertEquals(user, retrievedObject);
			
			PrismObject<UserType> userAddedExtension = PrismTestUtil.parseObject(new File(
			"src/test/resources/user-added-extension.xml"));
			
			ObjectDelta<UserType> delta = user.diff(userAddedExtension);
			
//			ObjectModificationType objectModificationType = PrismTestUtil.unmarshalObject(new File(
//			"src/test/resources/request/user-modify-add-extension.xml"), ObjectModificationType.class);
//			ObjectDelta<UserType> delta = DeltaConvertor.createObjectDelta(objectModificationType, UserType.class, PrismTestUtil.getPrismContext());
//			//modify user add extension
//			ObjectModificationType objectModificationType = CalculateXmlDiff.calculateChanges(new File(
//					"src/test/resources/user-without-extension.xml"), new File(
//					"src/test/resources/user-added-extension.xml"));
			
			repositoryService.modifyObject(UserType.class, delta.getOid(), delta.getModifications(), new OperationResult("test"));

			//check the extension in the object
			retrievedObject = repositoryService.getObject(UserType.class, oid, new OperationResult("test"));
			PrismAsserts.assertEquals(userAddedExtension, retrievedObject);

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
	@Test
	public void testUserAddWithoutOid() throws Exception {
		displayTestTile("testUserAddWithoutOid");
		String oid = null;
		try {
			//store new user object without oid			
			PrismObject<UserType> user = PrismTestUtil.parseObject(new File(
					"src/test/resources/user-without-oid.xml"));	
			oid = repositoryService.addObject(user, new OperationResult("test"));
			PrismObject<UserType> retrievedObject = repositoryService.getObject(UserType.class, oid, new OperationResult("test"));
			//check if oid was generated for the object
			assertEquals(oid, retrievedObject.getOid());
			user.setOid(oid);
			PrismAsserts.assertEquals(user, retrievedObject);
			
		} finally {
			if (oid != null) {
				// to be sure try to delete the object as part of cleanup
				try {
					repositoryService.deleteObject(UserType.class, oid, new OperationResult("test"));
				} catch (Exception ex) {
					// ignore exceptions during cleanup
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUserDeleteAccountRef() throws Exception {
		displayTestTile("testUserDeleteAccountRef");
		String oid = "c0c010c0-d34d-b33f-f00d-111111111234";
		String accountRefToDeleteOid = "8254880d-6584-425a-af2e-58f8ca394bbb";
		String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(
					"src/test/resources/resource-modified-removed-tags.xml"));
			repositoryService.addObject(resource, new OperationResult("test"));

			PrismObject<AccountShadowType> accountToDelete = PrismTestUtil.parseObject(new File("src/test/resources/account-delete-account-ref.xml"));
			repositoryService.addObject(accountToDelete, new OperationResult("test"));

			PrismObject<UserType> user = PrismTestUtil.parseObject(new File(
					"src/test/resources/user-account-ref.xml"));
			assertEquals(1, user.asObjectable().getAccountRef().size());
			repositoryService.addObject(user, new OperationResult("test"));

			PrismObject<UserType> oldUser = user.clone();
			
			user.remove(user.findItem(UserType.F_ACCOUNT_REF));
			// modify user - delete it's accountRef
//			ObjectModificationType modifications = new ObjectModificationType();
//			modifications.setOid(oid);
//			PropertyModificationType modification = new PropertyModificationType();
//			modification.setModificationType(PropertyModificationTypeType.delete);
//			modification.setPath(null);
//			PropertyModificationType.Value value = new PropertyModificationType.Value();
//			value.getAny()
//					.add((Element) DOMUtil
//							.parseDocument(
//									"<i:accountRef xmlns:i='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd' type=\"i:AccountShadowType\" oid=\"8254880d-6584-425a-af2e-58f8ca394bbb\"/>")
//							.getFirstChild());
//			modification.setValue(value);
//			modifications.getPropertyModification().add(modification);
//			ObjectDelta<UserType> delta = DeltaConvertor.createObjectDelta(modifications, UserType.class, PrismTestUtil.getPrismContext());
			ObjectDelta<UserType> delta = oldUser.diff(user);
			repositoryService.modifyObject(UserType.class, delta.getOid(), delta.getModifications(), new OperationResult("test"));

			//check if account ref was removed from the object
			PrismObject<UserType> retrievedObject = repositoryService.getObject(UserType.class, oid, new OperationResult("test"));
			assertEquals(oid, retrievedObject.getOid());
			assertEquals(0, retrievedObject.asObjectable().getAccountRef().size());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(ResourceType.class, resourceOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(AccountShadowType.class, accountRefToDeleteOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(UserType.class, oid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

}
