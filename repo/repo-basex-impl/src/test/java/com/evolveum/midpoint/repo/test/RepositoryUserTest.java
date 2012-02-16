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
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author Igor Farinic
 */
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
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
		String oid = "c0c010c0-d34d-b33f-f00d-111111111111";
		try {
			//get actual number of user objects in the repo
			
			PagingType pagingType = PagingTypeFactory.createPaging(0, 5, OrderDirectionType.ASCENDING, "name");
			List<UserType> objects = repositoryService.listObjects(UserType.class,
					pagingType, new OperationResult("test"));
			int actualSize = objects.size();

			//add new user object
			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user.xml"))).getValue();
			repositoryService.addObject(user, new OperationResult("test"));

			//get the object
			ObjectType retrievedObject = repositoryService.getObject(ObjectType.class, oid,
					new PropertyReferenceListType(), new OperationResult("test"));
			assertEquals(user.getOid(), retrievedObject.getOid());
			assertEquals(1, ((UserType) retrievedObject).getAdditionalNames().size());
			assertEquals(user.getAdditionalNames(),
					((UserType) retrievedObject).getAdditionalNames());
			assertEquals(user.getEMailAddress(),
					((UserType) retrievedObject).getEMailAddress());
			assertEquals(1, ((UserType) retrievedObject).getEMailAddress().size());
			assertEquals(user.getEmployeeNumber(),
					((UserType) retrievedObject).getEmployeeNumber());
			assertEquals(user.getEmployeeType(),
					((UserType) retrievedObject).getEmployeeType());
			assertEquals(1, ((UserType) retrievedObject).getEmployeeType().size());
			assertEquals(user.getFamilyName(),
					((UserType) retrievedObject).getFamilyName());
			assertEquals(user.getFullName(),
					((UserType) retrievedObject).getFullName());
			assertEquals(user.getGivenName(),
					((UserType) retrievedObject).getGivenName());
			assertEquals(user.getHonorificPrefix(),
					((UserType) retrievedObject).getHonorificPrefix());
			assertEquals(user.getHonorificSuffix(),
					((UserType) retrievedObject).getHonorificSuffix());
			
			// Test retrieval of extension
			System.out.println("Extension");
			System.out.println(ObjectTypeUtil.dump(user.getExtension()));
			assertEquals(2,user.getExtension().getAny().size());
			Element ext1 = (Element)user.getExtension().getAny().get(0);
			assertTrue(QNameUtil.compareQName(PIRACY_SHIP, ext1));
			assertEquals("Black Pearl",ext1.getTextContent());
			Element ext2 = (Element)user.getExtension().getAny().get(1);
			assertTrue(QNameUtil.compareQName(PIRACY_LOOT, ext2));
			// assert correct xsi:type attribute
			QName xsiType = DOMUtil.resolveXsiType(ext2,"default");
			assertEquals(DOMUtil.XSD_INTEGER,xsiType);
			Object lootObject = XmlTypeConverter.toJavaValue(ext2);
			assertTrue(lootObject instanceof Integer);
			assertEquals(123123,lootObject);

			//list the objects
			objects = repositoryService.listObjects(UserType.class,
					pagingType, new OperationResult("test"));
			boolean oidTest = false;

			//check if user under test is retrieved from the repo
			for (UserType o : objects) {
				if (oid.equals(o.getOid())) {
					oidTest = true;
				}
			}
			assertTrue(oidTest);
			assertEquals(actualSize + 1, objects.size());

			// delete object
			repositoryService.deleteObject(UserType.class, oid, new OperationResult("test"));
			try {
				repositoryService.getObject(ObjectType.class, oid, new PropertyReferenceListType(), new OperationResult("test"));
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
		String oid = "c0c010c0-d34d-b33f-f00d-222222222222";
		try {
			//store user without extension
			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user-without-extension.xml"))).getValue();
			repositoryService.addObject(user, new OperationResult("test"));
			ObjectType retrievedObject = repositoryService.getObject(ObjectType.class, oid,
					new PropertyReferenceListType(), new OperationResult("test"));
			assertEquals(user.getOid(), ((UserType) (retrievedObject)).getOid());

			
			ObjectModificationType objectModificationType = ((JAXBElement<ObjectModificationType>) JAXBUtil.unmarshal(new File(
			"src/test/resources/request/user-modify-add-extension.xml"))).getValue();
			
//			//modify user add extension
//			ObjectModificationType objectModificationType = CalculateXmlDiff.calculateChanges(new File(
//					"src/test/resources/user-without-extension.xml"), new File(
//					"src/test/resources/user-added-extension.xml"));
			
			repositoryService.modifyObject(UserType.class, objectModificationType, new OperationResult("test"));

			//check the extension in the object
			retrievedObject = repositoryService.getObject(ObjectType.class, oid, new PropertyReferenceListType(), new OperationResult("test"));
			assertEquals(user.getOid(), retrievedObject.getOid());
			assertNotNull(((UserType)retrievedObject).getExtension().getAny());
			assertEquals("ship", ((Element)((UserType)retrievedObject).getExtension().getAny().get(0)).getLocalName());
			assertEquals("Black Pearl", ((Element)((UserType)retrievedObject).getExtension().getAny().get(0)).getTextContent());

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
		String oid = null;
		try {
			//store new user object without oid			
			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user-without-oid.xml"))).getValue();	
			oid = repositoryService.addObject(user, new OperationResult("test"));
			ObjectType retrievedObject = repositoryService.getObject(ObjectType.class, oid,
					new PropertyReferenceListType(), new OperationResult("test"));
			//check if oid was generated for the object
			final UserType retrievedUser = (UserType) retrievedObject;
			assertEquals(oid, retrievedUser.getOid());
			assertEquals(user.getAdditionalNames(), retrievedUser.getAdditionalNames());
			assertEquals(user.getEMailAddress(), retrievedUser.getEMailAddress());
			assertEquals(user.getEmployeeNumber(), retrievedUser.getEmployeeNumber());
			assertEquals(user.getEmployeeType(), retrievedUser.getEmployeeType());
			assertEquals(user.getFamilyName(), retrievedUser.getFamilyName());
			assertEquals(user.getFullName(), retrievedUser.getFullName());
			assertEquals(user.getGivenName(), retrievedUser.getGivenName());
			assertEquals(user.getHonorificPrefix(), retrievedUser.getHonorificPrefix());
			assertEquals(user.getHonorificSuffix(), retrievedUser.getHonorificSuffix());
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
		String oid = "c0c010c0-d34d-b33f-f00d-111111111234";
		String accountRefToDeleteOid = "8254880d-6584-425a-af2e-58f8ca394bbb";
		String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/resource-modified-removed-tags.xml"))).getValue();
			repositoryService.addObject(resource, new OperationResult("test"));

			AccountShadowType accountToDelete = ((JAXBElement<AccountShadowType>) JAXBUtil
					.unmarshal(new File("src/test/resources/account-delete-account-ref.xml"))).getValue();
			repositoryService.addObject(accountToDelete, new OperationResult("test"));

			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user-account-ref.xml"))).getValue();
			assertEquals(1, user.getAccountRef().size());
			repositoryService.addObject(user, new OperationResult("test"));

			// modify user - delete it's accountRef
			ObjectModificationType modifications = new ObjectModificationType();
			modifications.setOid(oid);
			PropertyModificationType modification = new PropertyModificationType();
			modification.setModificationType(PropertyModificationTypeType.delete);
			modification.setPath(null);
			PropertyModificationType.Value value = new PropertyModificationType.Value();
			value.getAny()
					.add((Element) DOMUtil
							.parseDocument(
									"<i:accountRef xmlns:i='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd' type=\"account\" oid=\"8254880d-6584-425a-af2e-58f8ca394bbb\"/>")
							.getFirstChild());
			modification.setValue(value);
			modifications.getPropertyModification().add(modification);
			repositoryService.modifyObject(UserType.class, modifications, new OperationResult("test"));

			//check if account ref was removed from the object
			ObjectType retrievedObject = repositoryService.getObject(ObjectType.class, oid,
					new PropertyReferenceListType(), new OperationResult("test"));
			UserType retrievedUser = (UserType) retrievedObject;
			assertEquals(oid, retrievedUser.getOid());
			assertEquals(0, retrievedUser.getAccountRef().size());
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
