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

import static org.testng.AssertJUnit.*;
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

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.test.XmlAsserts;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author Igor Farinic
 */

@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
		"classpath:application-context-repository-test.xml" })
public class RepositoryAccountTest  extends AbstractTestNGSpringContextTests {

	@Autowired(required = true)
	private RepositoryService repositoryService;

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public RepositoryAccountTest() {
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

	private void compareObjects(AccountShadowType object, AccountShadowType retrievedObject) throws Exception {
		assertEquals(object.getOid(), retrievedObject.getOid());
		assertEquals(object.getName(), retrievedObject.getName());
		assertEquals(object.getCredentials().getPassword().getAny().toString(), object.getCredentials()
				.getPassword().getAny().toString());

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

	@Test
	@SuppressWarnings("unchecked")
	public void testAccount() throws Exception {
		final String accountOid = "dbb0c37d-9ee6-44a4-8d39-016dbce18b4c";
		final String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			// add resource
			ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"))).getValue();
			repositoryService.addObject(resource, new OperationResult("test"));
			ObjectType retrievedObject = repositoryService.getObject(resourceOid,
					new PropertyReferenceListType(), new OperationResult("test"));
			assertEquals(resource.getOid(), retrievedObject.getOid());

			// add account
			AccountShadowType accountShadow = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/account.xml"))).getValue();
			repositoryService.addObject(accountShadow, new OperationResult("test"));

			// get account object
			retrievedObject = repositoryService.getObject(accountOid, new PropertyReferenceListType(),
					new OperationResult("test"));
			compareObjects(accountShadow, (AccountShadowType) retrievedObject);

			// list account objects with simple paging
			PagingType pagingType = PagingTypeFactory
					.createPaging(0, 5, OrderDirectionType.ASCENDING, "name");
			ObjectListType objects = repositoryService.listObjects(ObjectTypes.ACCOUNT.getClassDefinition(),
					pagingType, new OperationResult("test"));
			assertEquals(1, objects.getObject().size());
			compareObjects(accountShadow, (AccountShadowType) objects.getObject().get(0));

			// delete object
			repositoryService.deleteObject(accountOid, new OperationResult("test"));
			try {
				repositoryService.getObject(accountOid, new PropertyReferenceListType(), new OperationResult(
						"test"));
				Assert.fail("Object with oid " + accountOid + " was not deleted");
			} catch (ObjectNotFoundException ex) {
				// ignore
			}
		} finally {
			try {
				repositoryService.deleteObject(accountOid, new OperationResult("test"));
			} catch (Exception e) {
				// ignore errors during cleanup
			}
			try {
				repositoryService.deleteObject(resourceOid, new OperationResult("test"));
			} catch (Exception e) {
				// ignore errors during cleanup
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testAccountShadowOwner() throws Exception {
		String userOid = "c0c010c0-d34d-b33f-f00d-111111111234";
		String accountRefOid = "8254880d-6584-425a-af2e-58f8ca394bbb";
		String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/resource-modified-removed-tags.xml"))).getValue();
			repositoryService.addObject(resource, new OperationResult("test"));

			AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/account-delete-account-ref.xml"))).getValue();
			repositoryService.addObject(account, new OperationResult("test"));

			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user-account-ref.xml"))).getValue();
			assertEquals(1, user.getAccountRef().size());
			repositoryService.addObject(user, new OperationResult("test"));

			UserType accountOwner = repositoryService.listAccountShadowOwner(accountRefOid,
					new OperationResult("test"));
			assertNotNull(accountOwner);
			assertEquals(userOid, accountOwner.getOid());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(resourceOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(accountRefOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(userOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public <T extends ResourceObjectShadowType> void testListResourceObjectShadows() throws Exception {
		String userOid = "c0c010c0-d34d-b33f-f00d-111111111234";
		String accountRefOid = "8254880d-6584-425a-af2e-58f8ca394bbb";
		String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/resource-modified-removed-tags.xml"))).getValue();
			repositoryService.addObject(resource, new OperationResult("test"));

			AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/account-delete-account-ref.xml"))).getValue();
			repositoryService.addObject(account, new OperationResult("test"));

			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user-account-ref.xml"))).getValue();
			assertEquals(1, user.getAccountRef().size());
			repositoryService.addObject(user, new OperationResult("test"));

			List<T> shadows = repositoryService.listResourceObjectShadows(resourceOid,
					(Class<T>) ObjectTypes.ACCOUNT.getClassDefinition(), new OperationResult("test"));
			assertNotNull(shadows);
			assertEquals(accountRefOid, shadows.get(0).getOid());

		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(resourceOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(accountRefOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(userOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

}
