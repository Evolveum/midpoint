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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * 
 * @author Igor Farinic
 */
@ContextConfiguration(locations = { "../../../../../ctx-repository.xml",
		"classpath:ctx-repo-cache.xml",
		"classpath:ctx-configuration-basex-test.xml" })
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void searchUserByName() throws Exception {
		String userOid = "c0c010c0-d34d-b33f-f00d-111111111111";
		try {
			PrismObject<UserType> user = PrismTestUtil.parseObject(new File("src/test/resources/user.xml"));
			repositoryService.addObject(user, new OperationResult("test"));

			PrismObject<UserType> userType = repositoryService.getObject(UserType.class, userOid, new OperationResult("test get user"));
			System.out.println(userType.dump());
			
			
			QueryType query = PrismTestUtil.unmarshalObject(new File(
					"src/test/resources/query-user-by-name.xml"), QueryType.class);
			List<PrismObject<UserType>> objectList = repositoryService.searchObjects(UserType.class, query,
					new PagingType(), new OperationResult("test"));
			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			PrismObject<UserType> foundUser = objectList.get(0);
            PolyStringType poly = new PolyStringType();
            poly.setOrig("Cpt. Jack Sparrow");
            poly.setNorm("cpt jack sparrow");
            PrismAsserts.assertEqualsPolyString("Values not equal", poly, foundUser.asObjectable().getFullName());
//			assertEquals(poly.getNorm(), foundUser.asObjectable().getFullName().getNorm());
//			assertEquals(poly.getOrig(), foundUser.asObjectable().getFullName().getOrig());
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
			PrismObject<UserType> user = PrismTestUtil.parseObject(new File("src/test/resources/user.xml"));
			repositoryService.addObject(user, new OperationResult("test"));

			QueryType query = PrismTestUtil.unmarshalObject(new File(
					"src/test/resources/query-all-by-name.xml"), QueryType.class);
			List<PrismObject<UserType>> objectList = repositoryService.searchObjects(UserType.class,
					query, new PagingType(), new OperationResult("test"));
			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			PrismObject<UserType> foundUser = (PrismObject) objectList.get(0);
            PolyStringType poly = new PolyStringType();
            poly.setOrig("Cpt. Jack Sparrow");
            poly.setNorm("cpt jack sparrow");
            PrismAsserts.assertEqualsPolyString("Values not equal", poly, foundUser.asObjectable().getFullName());
//            assertEquals(poly.getNorm(), foundUser.asObjectable().getFullName().getNorm());
//			assertEquals(poly.getOrig(), foundUser.asObjectable().getFullName().getOrig());
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
					"src/test/resources/query-account-by-attributes.xml"), QueryType.class);
			List<PrismObject<AccountShadowType>> objectList = repositoryService.searchObjects(
					AccountShadowType.class, query, new PagingType(), new OperationResult("test"));
			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			accountShadow = objectList.get(0);
			AccountShadowType accountShadowType = accountShadow.asObjectable();
			assertNotNull(accountShadowType.getAttributes().getAny());
			PrismAsserts.assertPropertyValue(accountShadow.findContainer(AccountShadowType.F_ATTRIBUTES), SchemaTestConstants.ICFS_UID,
					"4d6cfc84-ef47-395d-906d-efd3c79e74b1");
			PrismAsserts.assertPropertyValue(accountShadow.findContainer(AccountShadowType.F_ATTRIBUTES), SchemaTestConstants.ICFS_NAME,
					"uid=jbond,ou=People,dc=example,dc=com");
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(AccountShadowType.class, accountOid, new OperationResult(
						"test"));
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
					"src/test/resources/query-account-by-attributes-and-resource-ref.xml"), QueryType.class);
			List<PrismObject<AccountShadowType>> objectList = repositoryService.searchObjects(
					AccountShadowType.class, query, new PagingType(), new OperationResult("test"));
			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			accountShadow = objectList.get(0);
			AccountShadowType accountShadowType = accountShadow.asObjectable();
			assertNotNull(accountShadowType.getAttributes().getAny());
			PrismAsserts.assertPropertyValue(accountShadow.findContainer(AccountShadowType.F_ATTRIBUTES), SchemaTestConstants.ICFS_UID,
					"4d6cfc84-ef47-395d-906d-efd3c79e74b1");
			PrismAsserts.assertPropertyValue(accountShadow.findContainer(AccountShadowType.F_ATTRIBUTES), SchemaTestConstants.ICFS_NAME,
				"uid=jbond,ou=People,dc=example,dc=com");
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(AccountShadowType.class, accountOid, new OperationResult(
						"test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void searchAccountByNoAttributesUseQueryUtil() throws Exception {
		System.out.println("asdfasdf");
		XPathSegment xpathSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
		Document doc = DOMUtil.getDocument();
		List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
		xpathSegments.add(xpathSegment);
		XPathHolder xpath = new XPathHolder(xpathSegments);

		List<Element> values = new ArrayList<Element>();

		Element equalFilter = null;
		for (Element e : values) {
			 equalFilter = QueryUtil.createEqualFilter(doc, xpath, values);
		}
		Element filter = QueryUtil.createAndFilter(doc, equalFilter);

		QueryType query = QueryUtil.createQuery(filter);
		System.out.println("Query: " + QueryUtil.dump(query));
		// query.setFilter(filter);

		repositoryService.searchObjects(AccountShadowType.class, query, new PagingType(),
				new OperationResult("test"));

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
			// XPathSegment xpathSegment = new
			// XPathSegment(SchemaConstants.I_ATTRIBUTES);
			// Document doc = DOMUtil.getDocument();
			// List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
			// xpathSegments.add(xpathSegment);
			// XPathHolder xpath = new XPathHolder(xpathSegments);
			// List<Object> values = new ArrayList<Object>();
			// values.add((Element) DOMUtil
			// .parseDocument(
			// "<s:__NAME__ xmlns:s=\"http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-2.xsd\">uid=jbond,ou=People,dc=example,dc=com</s:__NAME__>")
			// .getFirstChild());
			//
			// // prepare query
			// Element filter = QueryUtil.createAndFilter(doc,
			// QueryUtil.createEqualFilter(doc, xpath, values));
			QueryType query = PrismTestUtil.unmarshalObject(new File(
					"src/test/resources/query-account-by-attributes.xml"), QueryType.class);
			// QueryType query = new QueryType();
			// query.setFilter(filter);

			// search objects
			List<PrismObject<AccountShadowType>> objectList = repositoryService.searchObjects(
					AccountShadowType.class, query, new PagingType(), new OperationResult("test"));

			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			accountShadow = objectList.get(0);
			AccountShadowType accountShadowType = accountShadow.asObjectable();
			assertNotNull(accountShadowType.getAttributes().getAny());
			PrismAsserts.assertPropertyValue(accountShadow.findContainer(AccountShadowType.F_ATTRIBUTES), SchemaTestConstants.ICFS_UID,
				"4d6cfc84-ef47-395d-906d-efd3c79e74b1");
			PrismAsserts.assertPropertyValue(accountShadow.findContainer(AccountShadowType.F_ATTRIBUTES), SchemaTestConstants.ICFS_NAME,
				"uid=jbond,ou=People,dc=example,dc=com");
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(AccountShadowType.class, accountOid, new OperationResult(
						"test"));
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
					"src/test/resources/query-connector-by-type.xml"), QueryType.class);
			List<PrismObject<ConnectorType>> objectList = repositoryService.searchObjects(
					ConnectorType.class, query, new PagingType(), new OperationResult("test"));
			assertNotNull(objectList);
			assertEquals(1, objectList.size());

			PrismObject<ConnectorType> foundConnector = objectList.get(0);
			assertEquals("ICF org.identityconnectors.ldap.LdapConnector",
					foundConnector.findProperty(ObjectType.F_NAME).getRealValue());
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
