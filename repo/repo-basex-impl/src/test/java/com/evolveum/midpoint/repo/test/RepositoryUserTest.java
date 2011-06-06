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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.math.BigInteger;

import javax.xml.bind.JAXBElement;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.DOMUtil;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author Igor Farinic
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
		"../../../../../application-context-repository-test.xml" })
public class RepositoryUserTest {

	@Autowired(required = true)
	private RepositoryPortType repositoryService;

	public RepositoryPortType getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryPortType repositoryService) {
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

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUser() throws Exception {
		String oid = "c0c010c0-d34d-b33f-f00d-111111111111";
		try {
			//get actual number of user objects in the repo
			PagingType pagingType = new PagingType();
			pagingType.setMaxSize(BigInteger.valueOf(5));
			pagingType.setOffset(BigInteger.valueOf(0));
			pagingType.setOrderBy(Utils.fillPropertyReference("name"));
			pagingType.setOrderDirection(OrderDirectionType.ASCENDING);
			ObjectListType objects = repositoryService.listObjects(
					QNameUtil.qNameToUri(SchemaConstants.I_USER_TYPE), pagingType);
			int actualSize = objects.getObject().size();

			//add new user object
			ObjectContainerType objectContainer = new ObjectContainerType();
			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user.xml"))).getValue();
			objectContainer.setObject(user);
			repositoryService.addObject(objectContainer);

			//get the object
			ObjectContainerType retrievedObjectContainer = repositoryService.getObject(oid,
					new PropertyReferenceListType());
			assertEquals(user.getOid(), ((UserType) (retrievedObjectContainer.getObject())).getOid());
			assertEquals(1, ((UserType) (retrievedObjectContainer.getObject())).getAdditionalNames().size());
			assertEquals(user.getAdditionalNames(),
					((UserType) (retrievedObjectContainer.getObject())).getAdditionalNames());
			assertEquals(user.getEMailAddress(),
					((UserType) (retrievedObjectContainer.getObject())).getEMailAddress());
			assertEquals(1, ((UserType) (retrievedObjectContainer.getObject())).getEMailAddress().size());
			assertEquals(user.getEmployeeNumber(),
					((UserType) (retrievedObjectContainer.getObject())).getEmployeeNumber());
			assertEquals(user.getEmployeeType(),
					((UserType) (retrievedObjectContainer.getObject())).getEmployeeType());
			assertEquals(1, ((UserType) (retrievedObjectContainer.getObject())).getEmployeeType().size());
			assertEquals(user.getFamilyName(),
					((UserType) (retrievedObjectContainer.getObject())).getFamilyName());
			assertEquals(user.getFullName(),
					((UserType) (retrievedObjectContainer.getObject())).getFullName());
			assertEquals(user.getGivenName(),
					((UserType) (retrievedObjectContainer.getObject())).getGivenName());
			assertEquals(user.getHonorificPrefix(),
					((UserType) (retrievedObjectContainer.getObject())).getHonorificPrefix());
			assertEquals(user.getHonorificSuffix(),
					((UserType) (retrievedObjectContainer.getObject())).getHonorificSuffix());

			//list the objects
			objects = repositoryService.listObjects(QNameUtil.qNameToUri(SchemaConstants.I_USER_TYPE),
					pagingType);
			boolean oidTest = false;

			//check if user under test is retrieved from the repo
			for (ObjectType o : objects.getObject()) {
				if (oid.equals(o.getOid())) {
					oidTest = true;
				}
			}
			assertTrue(oidTest);
			assertEquals(actualSize + 1, objects.getObject().size());

			// delete object
			repositoryService.deleteObject(oid);
			try {
				repositoryService.getObject(oid, new PropertyReferenceListType());
				fail("Object with oid " + oid + " was not deleted");
			} catch (FaultMessage ex) {
				if (!(ex.getFaultInfo() instanceof ObjectNotFoundFaultType)) {
					throw ex;
				}
			}		
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(oid);
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
			ObjectContainerType objectContainer = new ObjectContainerType();
			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user-without-extension.xml"))).getValue();
			objectContainer.setObject(user);
			repositoryService.addObject(objectContainer);
			ObjectContainerType retrievedObjectContainer = repositoryService.getObject(oid,
					new PropertyReferenceListType());
			assertEquals(user.getOid(), ((UserType) (retrievedObjectContainer.getObject())).getOid());

			//modify user add extension
			ObjectModificationType objectModificationType = CalculateXmlDiff.calculateChanges(new File(
					"src/test/resources/user-without-extension.xml"), new File(
					"src/test/resources/user-added-extension.xml"));
			repositoryService.modifyObject(objectModificationType);

			//check the extension in the object
			retrievedObjectContainer = repositoryService.getObject(oid, new PropertyReferenceListType());
			user = (UserType) retrievedObjectContainer.getObject();
			assertEquals(user.getOid(), ((UserType) (retrievedObjectContainer.getObject())).getOid());
			assertNotNull(user.getExtension().getAny());
			assertEquals("ship", user.getExtension().getAny().get(0).getLocalName());
			assertEquals("Black Pearl", user.getExtension().getAny().get(0).getTextContent());

		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(oid);
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
			ObjectContainerType objectContainer = new ObjectContainerType();
			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user-without-oid.xml"))).getValue();
			objectContainer.setObject(user);
			oid = repositoryService.addObject(objectContainer);
			ObjectContainerType retrievedObjectContainer = repositoryService.getObject(oid,
					new PropertyReferenceListType());
			//check if oid was generated for the object
			final UserType retrievedUser = (UserType) (retrievedObjectContainer.getObject());
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
					repositoryService.deleteObject(oid);
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
			ObjectContainerType objectContainer = new ObjectContainerType();
			ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/resource-modified-removed-tags.xml"))).getValue();
			objectContainer.setObject(resource);
			repositoryService.addObject(objectContainer);

			objectContainer = new ObjectContainerType();
			AccountShadowType accountToDelete = ((JAXBElement<AccountShadowType>) JAXBUtil
					.unmarshal(new File("src/test/resources/account-delete-account-ref.xml"))).getValue();
			objectContainer.setObject(accountToDelete);
			repositoryService.addObject(objectContainer);

			objectContainer = new ObjectContainerType();
			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user-account-ref.xml"))).getValue();
			assertEquals(1, user.getAccountRef().size());
			objectContainer.setObject(user);
			repositoryService.addObject(objectContainer);

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
			repositoryService.modifyObject(modifications);

			//check if account ref was removed from the object
			ObjectContainerType retrievedObjectContainer = repositoryService.getObject(oid,
					new PropertyReferenceListType());
			UserType retrievedUser = (UserType) retrievedObjectContainer.getObject();
			assertEquals(oid, retrievedUser.getOid());
			assertEquals(0, retrievedUser.getAccountRef().size());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(resourceOid);
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(accountRefToDeleteOid);
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(oid);
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

}
