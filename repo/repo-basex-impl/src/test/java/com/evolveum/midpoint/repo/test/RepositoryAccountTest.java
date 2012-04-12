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
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3._2001._04.xmlenc.EncryptedDataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.test.util.XmlAsserts;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
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
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

/**
 * 
 * @author Igor Farinic
 * 
 */
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-basex-test.xml" })
public class RepositoryAccountTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(RepositoryAccountTest.class);
	@Autowired(required = true)
	private RepositoryService repositoryService;
    @Autowired(required = true)
    private PrismContext prismContext;

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public RepositoryAccountTest() {
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

	@Test
	@SuppressWarnings("unchecked")
	public void testAccount() throws Exception {
		System.out.println("===[ testAccount ]===");
		final String accountOid = "dbb0c37d-9ee6-44a4-8d39-016dbce18b4c";
		final String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			// add resource
			PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"));
			PrismProperty<?> namespaceProp = resource.findProperty(ResourceType.F_NAMESPACE);
			System.out.println("Names   pace property:");
			System.out.println(namespaceProp.dump());
			repositoryService.addObject(resource, new OperationResult("test"));
			PrismObject<ResourceType> retrievedResource = repositoryService.getObject(ResourceType.class, resourceOid,
					new PropertyReferenceListType(), new OperationResult("test"));
			assertEquals(resource.getOid(), retrievedResource.getOid());

			// add account
			PrismObject<AccountShadowType> accountShadow = PrismTestUtil.parseObject(new File(
					"src/test/resources/account.xml"));
			accountShadow.setName(accountShadow.getDefinition().getName());
			
			repositoryService.addObject(accountShadow, new OperationResult("test"));

			// get account object
			PrismObject<AccountShadowType> retrievedAccount = repositoryService.getObject(AccountShadowType.class, accountOid,
					new PropertyReferenceListType(), new OperationResult("test"));
            LOGGER.debug("A\n{}", prismContext.silentMarshalObject(accountShadow.asObjectable()));
            LOGGER.debug("B\n{}", prismContext.silentMarshalObject(retrievedAccount.asObjectable()));
            EncryptedDataType data1 = accountShadow.asObjectable().getCredentials().getPassword().getProtectedString().getEncryptedData();
            EncryptedDataType data2 = retrievedAccount.asObjectable().getCredentials().getPassword().getProtectedString().getEncryptedData();
            data1.equals(data2);
			PrismAsserts.assertEquals(accountShadow, retrievedAccount);

			// list account objects with simple paging
			PagingType pagingType = PagingTypeFactory
					.createPaging(0, 5, OrderDirectionType.ASCENDING, "name");
			List<PrismObject<AccountShadowType>> objects = repositoryService.listObjects(AccountShadowType.class,
					pagingType, new OperationResult("test"));
			assertEquals(1, objects.size());
			PrismAsserts.assertEquivalent("Unexpected result from listObjects", accountShadow, objects.get(0));

			// delete object
			repositoryService.deleteObject(AccountShadowType.class, accountOid, new OperationResult("test"));
			try {
				repositoryService.getObject(AccountShadowType.class, accountOid,
						new PropertyReferenceListType(), new OperationResult("test"));
				Assert.fail("Object with oid " + accountOid + " was not deleted");
			} catch (ObjectNotFoundException ex) {
				// ignore
			}
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, accountOid, new OperationResult(
						"test"));
			} catch (Exception e) {
				// ignore errors during cleanup
			}
			try {
				repositoryService.deleteObject(ResourceType.class, resourceOid, new OperationResult("test"));
			} catch (Exception e) {
				// ignore errors during cleanup
			}
		}
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testAccountSearch() throws Exception {
		System.out.println("===[ testAccountSearch ]===");
		final String accountOid = "dbb0c37d-9ee6-44a4-8d39-016dbce18b4c";
		final String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			// add resource
			PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"));
			PrismProperty<?> namespaceProp = resource.findProperty(ResourceType.F_NAMESPACE);
			System.out.println("Names   pace property:");
			System.out.println(namespaceProp.dump());
			repositoryService.addObject(resource, new OperationResult("test"));
			PrismObject<ResourceType> retrievedResource = repositoryService.getObject(ResourceType.class, resourceOid,
					new PropertyReferenceListType(), new OperationResult("test"));
			assertEquals(resource.getOid(), retrievedResource.getOid());

			// add account
			PrismObject<AccountShadowType> accountShadow = PrismTestUtil.parseObject(new File(
					"src/test/resources/account.xml"));
			accountShadow.setName(accountShadow.getDefinition().getName());
			
			repositoryService.addObject(accountShadow, new OperationResult("test"));

			QueryType query = createShadowQuery(accountShadow.asObjectable());
			LOGGER.debug("Query:\n{}", DOMUtil.serializeDOMToString(query.getFilter()));
			
			List<PrismObject<AccountShadowType>> results = repositoryService.searchObjects(AccountShadowType.class, query, null, new OperationResult("test"));

			assertNotNull("Null results", results);
			assertFalse("Empty results", results.isEmpty());
			for (PrismObject<AccountShadowType> result: results) {
				LOGGER.debug("Search result:\n{}", result.dump());
			}
			
			// delete object
			repositoryService.deleteObject(AccountShadowType.class, accountOid, new OperationResult("test"));
			try {
				repositoryService.getObject(AccountShadowType.class, accountOid,
						new PropertyReferenceListType(), new OperationResult("test"));
				Assert.fail("Object with oid " + accountOid + " was not deleted");
			} catch (ObjectNotFoundException ex) {
				// ignore
			}
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, accountOid, new OperationResult(
						"test"));
			} catch (Exception e) {
				// ignore errors during cleanup
			}
			try {
				repositoryService.deleteObject(ResourceType.class, resourceOid, new OperationResult("test"));
			} catch (Exception e) {
				// ignore errors during cleanup
			}
		}
	}
	
	private static QueryType createShadowQuery(AccountShadowType resourceShadow) throws SchemaException {
		
		XPathHolder xpath = new XPathHolder(AccountShadowType.F_ATTRIBUTES);
		PrismContainer<?> attributesContainer = resourceShadow.asPrismObject().findContainer(AccountShadowType.F_ATTRIBUTES);
		PrismProperty<String> identifier = attributesContainer.findProperty(SchemaTestConstants.ICFS_UID);

		Document doc = DOMUtil.getDocument();
		Element filter;
		List<Element> identifierElements = PrismTestUtil.getPrismContext().getPrismDomProcessor().serializeItemToDom(identifier, doc);
		try {
			filter = QueryUtil.createAndFilter(doc, QueryUtil.createEqualRefFilter(doc, null,
					SchemaConstants.I_RESOURCE_REF, resourceShadow.getResourceRef().getOid()), QueryUtil
					.createEqualFilterFromElements(doc, xpath, identifierElements, resourceShadow
							.asPrismObject().getPrismContext()));
		} catch (SchemaException e) {
			throw new SchemaException("Schema error while creating search filter: " + e.getMessage(), e);
		}

		QueryType query = new QueryType();
		query.setFilter(filter);

		return query;
		
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testAccountShadowOwner() throws Exception {
		String userOid = "c0c010c0-d34d-b33f-f00d-111111111234";
		String accountRefOid = "8254880d-6584-425a-af2e-58f8ca394bbb";
		String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(
					"src/test/resources/resource-modified-removed-tags.xml"));
			repositoryService.addObject(resource, new OperationResult("test"));

			PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(
					"src/test/resources/account-delete-account-ref.xml"));
			repositoryService.addObject(account, new OperationResult("test"));

			PrismObject<UserType> user = PrismTestUtil.parseObject(new File(
					"src/test/resources/user-account-ref.xml"));
			assertEquals(1, user.asObjectable().getAccountRef().size());
			repositoryService.addObject(user, new OperationResult("test"));

			PrismObject<UserType> accountOwner = repositoryService.listAccountShadowOwner(accountRefOid,
					new OperationResult("test"));
			assertNotNull("No owner for account "+accountRefOid, accountOwner);
			assertEquals(userOid, accountOwner.getOid());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(ResourceType.class, resourceOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(AccountShadowType.class, accountRefOid, new OperationResult(
						"test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(UserType.class, userOid, new OperationResult("test"));
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
			PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(
					"src/test/resources/resource-modified-removed-tags.xml"));
			repositoryService.addObject(resource, new OperationResult("test"));

			PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(
					"src/test/resources/account-delete-account-ref.xml"));
			repositoryService.addObject(account, new OperationResult("test"));

			PrismObject<UserType> user = PrismTestUtil.parseObject(new File(
					"src/test/resources/user-account-ref.xml"));
			assertEquals(1, user.asObjectable().getAccountRef().size());
			repositoryService.addObject(user, new OperationResult("test"));

			List<PrismObject<T>> shadows = repositoryService.listResourceObjectShadows(resourceOid,
					(Class<T>) ObjectTypes.ACCOUNT.getClassDefinition(), new OperationResult("test"));
			assertNotNull(shadows);
			assertEquals(accountRefOid, shadows.get(0).getOid());

		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(ResourceType.class, resourceOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(AccountShadowType.class, accountRefOid, new OperationResult(
						"test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(UserType.class, userOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

}
