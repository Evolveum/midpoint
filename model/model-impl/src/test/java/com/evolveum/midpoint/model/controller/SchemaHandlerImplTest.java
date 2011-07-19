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
 */
package com.evolveum.midpoint.model.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.test.util.ModelTUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml" })
public class SchemaHandlerImplTest {

	private static final Trace LOGGER = TraceManager.getTrace(SchemaHandlerImplTest.class);
	@Autowired(required = true)
	private transient SchemaHandler handler;

	@Test(expected = IllegalArgumentException.class)
	public void processInboundHandlingNullUser() throws Exception {
		handler.processInboundHandling(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void processInboundHandlingNullResourceObjectShadow() throws Exception {
		handler.processInboundHandling(new UserType(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void processInboundHandlingNullResult() throws Exception {
		handler.processInboundHandling(new UserType(), new ResourceObjectShadowType(), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void processOutboundHandlingNullUser() throws Exception {
		handler.processOutboundHandling(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void processOutboundHandlingNullResourceObjectShadow() throws Exception {
		handler.processOutboundHandling(new UserType(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void processOutboundHandlingNullResult() throws Exception {
		handler.processOutboundHandling(new UserType(), new ResourceObjectShadowType(), null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testApplyOutboundSchemaHandlingOnAccount() throws Exception {

		AccountShadowType objectShadow = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/account-xpath-evaluation.xml"))).getValue();
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/user-new.xml"))).getValue();

		OperationResult result = new OperationResult("Process Outbound");
		ObjectModificationType changes = handler.processOutboundHandling(user, objectShadow, result);
		LOGGER.info(result.dump());
		// TODO: test changes object
		
		ResourceObjectShadowType appliedAccountShadow = ModelTUtil.patchXml(changes, objectShadow,
				AccountShadowType.class);

		assertEquals(8, appliedAccountShadow.getAttributes().getAny().size());
		final String NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2";
		final String NS_1 = "http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd";
		assertAttribute("cn", NS, "James Bond 007", appliedAccountShadow.getAttributes().getAny());
		assertAttribute("__NAME__", NS_1, "uid=janko nemenny,ou=people,dc=example,dc=com",
				appliedAccountShadow.getAttributes().getAny());
		assertAttribute("sn", NS, "", appliedAccountShadow.getAttributes().getAny());
		assertAttribute("__PASSWORD__", NS_1, "janco", appliedAccountShadow.getAttributes().getAny());
		assertAttribute("givenName", NS, "James Jr.", appliedAccountShadow.getAttributes().getAny());
		assertAttribute("givenName", "http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ",
				"James Jr.", appliedAccountShadow.getAttributes().getAny());
		assertAttribute("title", NS, "Mr.", appliedAccountShadow.getAttributes().getAny());
		assertAttribute("description", NS, "Created by IDM", appliedAccountShadow.getAttributes().getAny());
	}

	private void assertAttribute(String name, String namespace, String value, List<Element> attributes) {
		boolean found = false;
		for (Element element : attributes) {
			if (!element.getLocalName().equals(name) || !element.getNamespaceURI().equals(namespace)) {
				continue;
			}

			if (value.equals(element.getTextContent())) {
				found = true;
				break;
			}
		}

		assertTrue(found);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testApplyInboundSchemaHandlingOnUserReplace() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/account-xpath-evaluation.xml"))).getValue();
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/user-new.xml"))).getValue();

		OperationResult result = new OperationResult("testApplyInboundSchemaHandlingOnUserReplace");
		UserType appliedUser = handler.processInboundHandling(user, account, result);
		LOGGER.info(result.dump());

		assertEquals("jan prvy", appliedUser.getFullName());
		assertEquals("Mr.", appliedUser.getHonorificPrefix());
		// family name has to be null in source
		// family name will not be filled because it is referenced by not
		// defined attribute in resource schema
		assertNull(appliedUser.getFamilyName());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testApplyInboundSchemaHandlingOnUserAdd() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/account-xpath-evaluation.xml"))).getValue();

		OperationResult result = new OperationResult("testApplyInboundSchemaHandlingOnUserAdd");
		UserType appliedUser = handler.processInboundHandling(new UserType(), account, result);
		LOGGER.info(result.dump());

		assertEquals("jan prvy", appliedUser.getFullName());
		assertEquals("Mr.", appliedUser.getHonorificPrefix());
		assertNull(appliedUser.getHonorificSuffix());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testApplyInboundSchemaHandlingOnUserAddWithFilter() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/account-xpath-evaluation-filter.xml"))).getValue();
		List<Element> domAttrs = account.getAttributes().getAny();
		for (Element e : domAttrs) {
			if ("cn".equals(e.getLocalName())) {
				e.setTextContent("jan\u0007 prvy");
			}
		}

		OperationResult result = new OperationResult("testApplyInboundSchemaHandlingOnUserAddWithFilter");
		UserType appliedUser = handler.processInboundHandling(new UserType(), account, result);
		LOGGER.info(result.dump());

		assertEquals("jan prvy", appliedUser.getFullName());
		assertEquals("Mr.", appliedUser.getHonorificPrefix());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testApplyInboundSchemaHandlingOnEmptyUserExtension() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/account-xpath-evaluation-extension.xml"))).getValue();

		OperationResult result = new OperationResult("testApplyInboundSchemaHandlingOnEmptyUserExtension");
		UserType appliedUser = handler.processInboundHandling(new UserType(), account, result);
		LOGGER.info(result.dump());

		assertNotNull(appliedUser.getExtension());
		assertEquals("MikeFromExtension", appliedUser.getExtension().getAny().get(0).getTextContent());
		assertEquals("DudikoffFromExtension", appliedUser.getExtension().getAny().get(1).getTextContent());
	}
	
	//
	// @Test
	// @SuppressWarnings("unchecked")
	// public void testEvaluateCorrelationExpression() throws Exception {
	// JAXBElement<AccountShadowType> accountJaxb =
	// (JAXBElement<AccountShadowType>) JAXBUtil
	// .unmarshal(new File("src/test/resources/account-xpath-evaluation.xml"));
	// Document doc = DOMUtil
	// .parseDocument("<c:valueExpression ref='c:familyName' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd' xmlns:dj='http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ'>$c:account/c:attributes/dj:givenName</c:valueExpression>");
	// Element domElement = (Element) doc.getFirstChild();
	// ExpressionHolder expressionHolder = new ExpressionHolder(domElement);
	// String evaluatedExpression =
	// schemaHandling.evaluateCorrelationExpression(accountJaxb.getValue(),
	// expressionHolder);
	// assertEquals("James Jr.", evaluatedExpression);
	// }
	//
	// @Test
	// @SuppressWarnings("unchecked")
	// public void testApplyUserTemplate() throws Exception {
	// JAXBElement<AccountShadowType> accountJaxb =
	// (JAXBElement<AccountShadowType>) JAXBUtil
	// .unmarshal(new File("src/test/resources/account-user-template.xml"));
	// UserType appliedUser =
	// schemaHandling.applyInboundSchemaHandlingOnUser(new UserType(),
	// accountJaxb.getValue());
	//
	// JAXBElement<UserTemplateType> userTemplate =
	// (JAXBElement<UserTemplateType>) JAXBUtil
	// .unmarshal(new File("src/test/resources/user-template.xml"));
	// UserType finalAppliedUser = schemaHandling.applyUserTemplate(appliedUser,
	// userTemplate.getValue());
	// assertEquals("jan prvy", finalAppliedUser.getFullName());
	// }
}
