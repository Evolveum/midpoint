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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author Igor Farinic
 */
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class RepositorySearchTest extends AbstractTestNGSpringContextTests {

	@Autowired(required = true)
	private RepositoryService repositoryService;

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public RepositorySearchTest() {
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void searchUserByName() throws Exception {
		String userOid = "c0c010c0-d34d-b33f-f00d-111111111111";
		try {
			PrismObject<UserType> user = PrismTestUtil.parseObject(new File(
					"src/test/resources/user.xml"));
			repositoryService.addObject(user, new OperationResult("test"));

			QueryType query = PrismTestUtil.unmarshalObject(new File(
					"src/test/resources/query-user-by-name.xml"));
			List<PrismObject<UserType>> objectList = repositoryService.searchObjects(UserType.class, query, new PagingType(), new OperationResult("test"));
			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			PrismObject<UserType> foundUser = objectList.get(0);
			assertEquals("Cpt. Jack Sparrow", foundUser.asObjectable().getFullName());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(UserType.class, userOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

	@Test
	public void searchByNameAllObjectsTest() throws Exception {
		String userOid = "c0c010c0-d34d-b33f-f00d-111111111111";
		try {
			PrismObject<UserType> user = PrismTestUtil.parseObject(new File(
					"src/test/resources/user.xml"));
			repositoryService.addObject(user, new OperationResult("test"));

			QueryType query = PrismTestUtil.unmarshalObject(new File(
					"src/test/resources/query-all-by-name.xml"));
			List<PrismObject<ObjectType>> objectList = repositoryService.searchObjects(ObjectType.class, query, new PagingType(), new OperationResult("test"));
			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			PrismObject<UserType> foundUser = (PrismObject) objectList.get(0);
			assertEquals("Cpt. Jack Sparrow", foundUser.asObjectable().getFullName());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(UserType.class, userOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}
	
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void searchAccountByAttributes() throws Exception {
		String accountOid = "dbb0c37d-9ee6-44a4-8d39-016dbce18b4c";
		try {
			// add account
			PrismObject<AccountShadowType> accountShadow = PrismTestUtil.parseObject(new File(
					"src/test/resources/account.xml"));
			repositoryService.addObject(accountShadow, new OperationResult("test"));

			QueryType query = PrismTestUtil.unmarshalObject(new File(
					"src/test/resources/query-account-by-attributes.xml"));
			List<PrismObject<AccountShadowType>> objectList = repositoryService.searchObjects(AccountShadowType.class, query, new PagingType(), new OperationResult("test"));
			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			accountShadow = objectList.get(0);
			AccountShadowType accountShadowType = accountShadow.asObjectable();
			assertNotNull(accountShadowType.getAttributes().getAny());
			assertEquals("4d6cfc84-ef47-395d-906d-efd3c79e74b1", ((Element)accountShadowType.getAttributes().getAny()
					.get(0)).getTextContent());
			assertEquals("uid=jbond,ou=People,dc=example,dc=com",
					((Element)accountShadowType.getAttributes().getAny().get(1)).getTextContent());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(AccountShadowType.class, accountOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void searchAccountByAttributesAndResourceRef() throws Exception {
		String accountOid = "dbb0c37d-9ee6-44a4-8d39-016dbce18b4c";
		try {
			// add account
			PrismObject<AccountShadowType> accountShadow = PrismTestUtil.parseObject(new File(
					"src/test/resources/account.xml"));
			repositoryService.addObject(accountShadow, new OperationResult("test"));

			QueryType query = PrismTestUtil.unmarshalObject(new File(
					"src/test/resources/query-account-by-attributes-and-resource-ref.xml"));
			List<PrismObject<AccountShadowType>> objectList = repositoryService.searchObjects(AccountShadowType.class, query, new PagingType(), new OperationResult("test"));
			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			accountShadow = objectList.get(0);
			AccountShadowType accountShadowType = accountShadow.asObjectable();
			assertNotNull(accountShadowType.getAttributes().getAny());
			assertEquals("4d6cfc84-ef47-395d-906d-efd3c79e74b1", ((Element)accountShadowType.getAttributes().getAny()
					.get(0)).getTextContent());
			assertEquals("uid=jbond,ou=People,dc=example,dc=com",
					((Element)accountShadowType.getAttributes().getAny().get(1)).getTextContent());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(AccountShadowType.class, accountOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}


	@Test(expectedExceptions = IllegalArgumentException.class)
	public void searchAccountByNoAttributesUseQueryUtil() throws Exception {
		XPathSegment xpathSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
		Document doc = DOMUtil.getDocument();
		List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
		xpathSegments.add(xpathSegment);
		XPathHolder xpath = new XPathHolder(xpathSegments);

		List<Element> values = new ArrayList<Element>();

		Element filter = QueryUtil.createAndFilter(doc,
				QueryUtil.createEqualFilter(doc, xpath, values));

		QueryType query = new QueryType();
		query.setFilter(filter);

		repositoryService.searchObjects(AccountShadowType.class, query, new PagingType(), new OperationResult("test"));

	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void searchAccountByAttributesUseQueryUtil() throws Exception {
		String accountOid = "dbb0c37d-9ee6-44a4-8d39-016dbce18b4c";
		try {
			// add account
			PrismObject<AccountShadowType> accountShadow = PrismTestUtil.parseObject(new File(
					"src/test/resources/account.xml"));
			repositoryService.addObject(accountShadow, new OperationResult("test"));

			// prepare query's filter value
			XPathSegment xpathSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
			Document doc = DOMUtil.getDocument();
			List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
			xpathSegments.add(xpathSegment);
			XPathHolder xpath = new XPathHolder(xpathSegments);
			List<Object> values = new ArrayList<Object>();
			values.add((Element) DOMUtil
					.parseDocument(
							"<s:__NAME__ xmlns:s=\"http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd\">uid=jbond,ou=People,dc=example,dc=com</s:__NAME__>")
					.getFirstChild());

			// prepare query
			Element filter = QueryUtil.createAndFilter(doc,
					QueryUtil.createEqualFilter(doc, xpath, values));

			QueryType query = new QueryType();
			query.setFilter(filter);

			// search objects
			List<PrismObject<AccountShadowType>> objectList = repositoryService.searchObjects(AccountShadowType.class, query, new PagingType(), new OperationResult("test"));

			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			accountShadow = objectList.get(0);
			AccountShadowType accountShadowType = accountShadow.asObjectable();
			assertNotNull(accountShadowType.getAttributes().getAny());
			assertEquals("4d6cfc84-ef47-395d-906d-efd3c79e74b1", ((Element)accountShadowType.getAttributes().getAny()
					.get(0)).getTextContent());
			assertEquals("uid=jbond,ou=People,dc=example,dc=com",
					((Element)accountShadowType.getAttributes().getAny().get(1)).getTextContent());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(AccountShadowType.class, accountOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}

	}
	
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void searchConnectorByType() throws Exception {
		String userOid = "c0c010c0-d34d-b33f-f00d-111111111111";
		try {
			PrismObject<ConnectorType> connector = PrismTestUtil.parseObject(new File(
					"src/test/resources/connector.xml"));
			repositoryService.addObject(connector, new OperationResult("test"));

			QueryType query = PrismTestUtil.unmarshalObject(new File(
					"src/test/resources/query-connector-by-type.xml"));
			List<PrismObject<ConnectorType>> objectList = repositoryService.searchObjects(ConnectorType.class, query, new PagingType(), new OperationResult("test"));
			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			PrismObject<ConnectorType> foundConnector = objectList.get(0);
			assertEquals("ICF org.identityconnectors.ldap.LdapConnector", foundConnector.getName());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(UserType.class, userOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

}
