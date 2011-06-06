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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;

/**
 * 
 * @author Igor Farinic
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
		"../../../../../application-context-repository-test.xml" })
public class RepositorySearchTest {

	@Autowired(required = true)
	private RepositoryPortType repositoryService;

	public RepositoryPortType getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryPortType repositoryService) {
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

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void searchUser() throws Exception {
		String userOid = "c0c010c0-d34d-b33f-f00d-111111111111";
		try {
			ObjectContainerType objectContainer = new ObjectContainerType();
			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user.xml"))).getValue();
			objectContainer.setObject(user);
			repositoryService.addObject(objectContainer);

			QueryType query = (QueryType) ((JAXBElement) JAXBUtil.unmarshal(new File(
					"src/test/resources/query-user-by-name.xml"))).getValue();
			ObjectListType objectList = repositoryService.searchObjects(query, new PagingType());
			assertNotNull(objectList);
			assertNotNull(objectList.getObject());
			assertEquals(1, objectList.getObject().size());

			UserType foundUser = (UserType) objectList.getObject().get(0);
			assertEquals("Cpt. Jack Sparrow", foundUser.getFullName());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(userOid);
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
			ObjectContainerType objectContainer = new ObjectContainerType();
			AccountShadowType accountShadow = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/account.xml"))).getValue();
			objectContainer.setObject(accountShadow);
			repositoryService.addObject(objectContainer);

			QueryType query = (QueryType) ((JAXBElement) JAXBUtil.unmarshal(new File(
					"src/test/resources/query-account-by-attributes.xml"))).getValue();
			ObjectListType objectList = repositoryService.searchObjects(query, new PagingType());
			assertNotNull(objectList);
			assertNotNull(objectList.getObject());
			assertEquals(1, objectList.getObject().size());

			accountShadow = (AccountShadowType) objectList.getObject().get(0);
			assertNotNull(accountShadow.getAttributes().getAny());
			assertEquals("4d6cfc84-ef47-395d-906d-efd3c79e74b1", accountShadow.getAttributes().getAny()
					.get(0).getTextContent());
			assertEquals("uid=jbond,ou=People,dc=example,dc=com",
					accountShadow.getAttributes().getAny().get(1).getTextContent());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(accountOid);
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
			ObjectContainerType objectContainer = new ObjectContainerType();
			AccountShadowType accountShadow = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/account.xml"))).getValue();
			objectContainer.setObject(accountShadow);
			repositoryService.addObject(objectContainer);

			QueryType query = (QueryType) ((JAXBElement) JAXBUtil.unmarshal(new File(
					"src/test/resources/query-account-by-attributes-and-resource-ref.xml"))).getValue();
			ObjectListType objectList = repositoryService.searchObjects(query, new PagingType());
			assertNotNull(objectList);
			assertNotNull(objectList.getObject());
			assertEquals(1, objectList.getObject().size());

			accountShadow = (AccountShadowType) objectList.getObject().get(0);
			assertNotNull(accountShadow.getAttributes().getAny());
			assertEquals("4d6cfc84-ef47-395d-906d-efd3c79e74b1", accountShadow.getAttributes().getAny()
					.get(0).getTextContent());
			assertEquals("uid=jbond,ou=People,dc=example,dc=com",
					accountShadow.getAttributes().getAny().get(1).getTextContent());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(accountOid);
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

	@Test
	@SuppressWarnings({ "rawtypes" })
	public void searchResourceStateByResourceRef() throws Exception {
		String resourceOid = "d0db5be9-cb93-401f-b6c1-86ffffe4cd5e";
		String resourceStateOid = "d0db5be9-cb93-401f-b6c1-111111111111";
		try {
			// insert new resource state
			ResourceStateType newResourceState = new ResourceStateType();
			newResourceState.setOid(resourceStateOid);
			newResourceState.setName("ResourceStateForSearch");
			ObjectReferenceType resourceRef = new ObjectReferenceType();
			resourceRef.setOid(resourceOid);
			newResourceState.setResourceRef(resourceRef);
			ResourceStateType.SynchronizationState state = new ResourceStateType.SynchronizationState();
			Document doc = DOMUtil.getDocument();
			Element element = doc.createElement("fakeNode");
			element.setTextContent("fakeValue");
			doc.appendChild(element);
			state.getAny().add((Element) doc.getFirstChild());
			newResourceState.setSynchronizationState(state);
			ObjectContainerType container = new ObjectContainerType();
			container.setObject(newResourceState);
			repositoryService.addObject(container);

			// search for object
			QueryType query = (QueryType) ((JAXBElement) JAXBUtil.unmarshal(new File(
					"src/test/resources/query-resource-state-by-resource-ref.xml"))).getValue();
			ObjectListType objectList = repositoryService.searchObjects(query, new PagingType());
			assertNotNull(objectList);
			assertNotNull(objectList.getObject());
			assertEquals(1, objectList.getObject().size());

			ResourceStateType resourceState = (ResourceStateType) objectList.getObject().get(0);
			assertNotNull(resourceState);
			assertNotNull(resourceOid, resourceState.getResourceRef().getOid());
			assertNotNull(resourceState.getSynchronizationState().getAny());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(resourceStateOid);
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void searchAccountByNoAttributesUseQueryUtil() throws Exception {
		XPathSegment xpathSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
		Document doc = DOMUtil.getDocument();
		List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
		xpathSegments.add(xpathSegment);
		XPathType xpath = new XPathType(xpathSegments);

		List<Element> values = new ArrayList<Element>();

		Element filter = QueryUtil.createAndFilter(doc,
				QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_ACCOUNT_TYPE)),
				QueryUtil.createEqualFilter(doc, xpath, values));

		QueryType query = new QueryType();
		query.setFilter(filter);

		repositoryService.searchObjects(query, new PagingType());

	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void searchAccountByAttributesUseQueryUtil() throws Exception {
		String accountOid = "dbb0c37d-9ee6-44a4-8d39-016dbce18b4c";
		try {
			// add account
			ObjectContainerType objectContainer = new ObjectContainerType();
			AccountShadowType accountShadow = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/account.xml"))).getValue();
			objectContainer.setObject(accountShadow);
			repositoryService.addObject(objectContainer);

			// prepare query's filter value
			XPathSegment xpathSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
			Document doc = DOMUtil.getDocument();
			List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
			xpathSegments.add(xpathSegment);
			XPathType xpath = new XPathType(xpathSegments);
			List<Element> values = new ArrayList<Element>();
			values.add((Element) DOMUtil
					.parseDocument(
							"<s:__NAME__ xmlns:s=\"http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd\">uid=jbond,ou=People,dc=example,dc=com</s:__NAME__>")
					.getFirstChild());

			// prepare query
			Element filter = QueryUtil.createAndFilter(doc,
					QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_ACCOUNT_SHADOW_TYPE)),
					QueryUtil.createEqualFilter(doc, xpath, values));

			QueryType query = new QueryType();
			query.setFilter(filter);

			// search objects
			ObjectListType objectList = repositoryService.searchObjects(query, new PagingType());

			assertNotNull(objectList);
			assertNotNull(objectList.getObject());
			assertEquals(1, objectList.getObject().size());

			accountShadow = (AccountShadowType) objectList.getObject().get(0);
			assertNotNull(accountShadow.getAttributes().getAny());
			assertEquals("4d6cfc84-ef47-395d-906d-efd3c79e74b1", accountShadow.getAttributes().getAny()
					.get(0).getTextContent());
			assertEquals("uid=jbond,ou=People,dc=example,dc=com",
					accountShadow.getAttributes().getAny().get(1).getTextContent());
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(accountOid);
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}

	}
}
