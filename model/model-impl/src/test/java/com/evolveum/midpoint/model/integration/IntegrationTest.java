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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.commons.codec.binary.Base64;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.objects.ResourceAttribute;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.service.ResourceAccessInterface;
import com.evolveum.midpoint.provisioning.service.ResourceConnector;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author Vilo Repan
 */
@Ignore //FIXME: fix test
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml", "classpath:application-context-provisioning.xml" })
public class IntegrationTest {

	private static final Trace trace = TraceManager.getTrace(IntegrationTest.class);
	@Autowired(required = true)
	ModelPortType modelService;
	@Autowired(required = true)
	RepositoryService repositoryService;
	@SuppressWarnings("rawtypes")
	@Autowired(required = true)
	private ResourceAccessInterface rai;

	@SuppressWarnings("unchecked")
	@Test
	public void createDefaultUserAccounts() throws Exception {
		String resourceOid = null;
		String userOid = null;
		String accountOid = null;
		try {
			ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/resource-simple.xml"))).getValue();
			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user-default-accounts.xml"))).getValue();

			resourceOid = resource.getOid();
			// test objects
			skipTestsIfExists(resource.getOid());
			skipTestsIfExists(user.getOid());

			// mocking repository
			repositoryService.addObject(resource, new OperationResult("Add Object"));
			// mock provisioning
			ResourceConnector c = new ResourceConnector(resource) {

				@Override
				public Object getConfiguration() {
					throw new UnsupportedOperationException(
							"Get configuration method not implemented - mock.");
				}
			};
			when(rai.getConnector()).thenReturn(c);

			Answer<Object> answer = new Answer<Object>() {

				ResourceObject object;

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					if ("get".equals(invocation.getMethod().getName())) {
						return object;
					}
					object = (ResourceObject) invocation.getArguments()[1];

					ResourceObjectShadowType shadow = (ResourceObjectShadowType) invocation.getArguments()[2];
					List<Element> elements = shadow.getAttributes().getAny();
					for (Element element : elements) {
						ResourceAttribute attribute = object.getValue(new QName(element.getNamespaceURI(),
								element.getLocalName()));
						if (attribute != null) {
							attribute.addJavaValue(element.getTextContent());
						}
					}
					return object;
				}
			};
			when(
					rai.add(any(OperationalResultType.class), any(ResourceObject.class),
							any(ResourceObjectShadowType.class))).thenAnswer(answer);
			when(rai.get(any(OperationalResultType.class), any(ResourceObject.class))).thenAnswer(answer);

			// test begins
			userOid = modelService
					.addObject(user, new Holder<OperationResultType>(new OperationResultType()));

			user = (UserType) repositoryService.getObject(userOid, new PropertyReferenceListType(),
					new OperationResult("Get Object"));

			// test user
			assertNotNull(user);
			assertEquals("chivas", user.getName());
			assertEquals("Chivas Regal", user.getFullName());
			assertEquals("Chivas", user.getGivenName());
			assertEquals("Regal", user.getFamilyName());
			assertEquals(1, user.getEMailAddress().size());
			assertEquals("chivas@regal.com", user.getEMailAddress().get(0));
			// test user account
			assertEquals(1, user.getAccountRef().size());
			ObjectReferenceType accountRef = user.getAccountRef().get(0);
			accountOid = accountRef.getOid();

			AccountShadowType account = (AccountShadowType) modelService.getObject(accountOid,
					new PropertyReferenceListType(), new Holder<OperationResultType>(
							new OperationResultType()));
			// test account credentials
			assertEquals(resource.getOid(), account.getResourceRef().getOid());
			assertNotNull(account.getCredentials());
			assertNotNull(account.getCredentials().getPassword());
			assertNotNull(account.getCredentials().getPassword().getAny());

			Element element = (Element) account.getCredentials().getPassword().getAny();
			assertNotNull(element.getTextContent());
			assertEquals(4, new String(Base64.decodeBase64(element.getTextContent())).length());
			// test account attributes
			assertEquals(
					"uid=chivas,ou=people,dc=example,dc=com",
					getAttributeValue("http://midpoint.evolveum."
							+ "com/xml/ns/public/resource/idconnector/resource-schema-1.xsd", "__NAME__",
							account));
			assertEquals("Chivas Regal", getAttributeValue("cn", account));
			assertEquals("Chivas", getAttributeValue("givenName", account));
			assertEquals("Regal", getAttributeValue("sn", account));
			assertEquals("Created by IDM", getAttributeValue("description", account));
		} finally {
			deleteObject(accountOid);
			deleteObject(resourceOid);
			deleteObject(userOid);
		}
	}

	private String getAttributeValue(String namespace, String name, AccountShadowType account) {
		ResourceObjectShadowType.Attributes attributes = account.getAttributes();
		List<Element> elements = attributes.getAny();
		for (Element element : elements) {
			if (namespace.equals(element.getNamespaceURI()) && element.getLocalName().equals(name)) {
				return element.getTextContent();
			}
		}

		return null;
	}

	private String getAttributeValue(String name, AccountShadowType account) {
		return getAttributeValue("http://midpoint.evolveum.com/xml/ns/public/resource/instances/"
				+ "a1a1a1a1-76e0-48e2-86d6-3d4f02d3e1a2", name, account);
	}

	private void deleteObject(String oid) {
		if (oid == null) {
			return;
		}
		trace.info("Test cleanup: Removing object '{}'", oid);
		try {
			repositoryService.deleteObject(oid, new OperationResult("Delete Object"));
		} catch (Exception ex) {
			trace.error("Couldn't delete '{}', reason: {}", new Object[] { oid, ex.getMessage() });
		}
	}

	private void skipTestsIfExists(String oid) {
		try {
			repositoryService.getObject(oid, new PropertyReferenceListType(), new OperationResult(
					"Get Object"));

			// delete
			repositoryService.deleteObject(oid, new OperationResult("Delete Object"));
			// skip
			// fail("Object with oid '" + oid + "'");
		} catch (Exception ex) {
		}
	}
}
