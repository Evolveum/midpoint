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

package com.evolveum.midpoint.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.ExpressionHolder;

/**
 * 
 * @author sleepwalker
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml", "classpath:application-context-repository-test.xml",
		"classpath:application-context-provisioning.xml", "classpath:application-context-model-test.xml" })
public class SchemaHandlingTest {

	@Autowired
	SchemaHandling schemaHandling;

	public SchemaHandlingTest() {
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
	@SuppressWarnings("unchecked")
	public void testApplyOutboundSchemaHandlingOnAccount() throws Exception {
		JAXBElement<AccountShadowType> accountJaxb = (JAXBElement<AccountShadowType>) JAXBUtil
				.unmarshal(new File("src/test/resources/account-xpath-evaluation.xml"));
		JAXBElement<UserType> userJaxb = (JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/user-new.xml"));
		ResourceObjectShadowType appliedAccountShadow = schemaHandling.applyOutboundSchemaHandlingOnAccount(
				userJaxb.getValue(), accountJaxb.getValue(), accountJaxb.getValue().getResource());
		// we will check number of attributes, because attributes not defined in
		// resource schema mustn't be applied
		assertEquals(8, appliedAccountShadow.getAttributes().getAny().size());
		assertEquals("cn", appliedAccountShadow.getAttributes().getAny().get(0).getLocalName());
		assertEquals("James Bond 007", appliedAccountShadow.getAttributes().getAny().get(0).getTextContent());
		assertEquals("__NAME__", appliedAccountShadow.getAttributes().getAny().get(1).getLocalName());
		assertEquals("uid=janko nemenny,ou=people,dc=example,dc=com", appliedAccountShadow.getAttributes()
				.getAny().get(1).getTextContent());
		assertEquals("sn", appliedAccountShadow.getAttributes().getAny().get(2).getLocalName());
		assertEquals("", appliedAccountShadow.getAttributes().getAny().get(2).getTextContent());
		assertEquals("givenName", appliedAccountShadow.getAttributes().getAny().get(4).getLocalName());
		assertEquals("James Jr.", appliedAccountShadow.getAttributes().getAny().get(4).getTextContent());
		assertEquals("description", appliedAccountShadow.getAttributes().getAny().get(7).getLocalName());
		assertEquals("Created by IDM", appliedAccountShadow.getAttributes().getAny().get(7).getTextContent());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testApplyInboundSchemaHandlingOnUserReplace() throws Exception {
		JAXBElement<AccountShadowType> accountJaxb = (JAXBElement<AccountShadowType>) JAXBUtil
				.unmarshal(new File("src/test/resources/account-xpath-evaluation.xml"));
		JAXBElement<UserType> userJaxb = (JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/user-new.xml"));
		UserType appliedUser = schemaHandling.applyInboundSchemaHandlingOnUser(userJaxb.getValue(),
				accountJaxb.getValue());
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
		JAXBElement<AccountShadowType> accountJaxb = (JAXBElement<AccountShadowType>) JAXBUtil
				.unmarshal(new File("src/test/resources/account-xpath-evaluation.xml"));
		UserType appliedUser = schemaHandling.applyInboundSchemaHandlingOnUser(new UserType(),
				accountJaxb.getValue());
		assertEquals("jan prvy", appliedUser.getFullName());
		assertEquals("Mr.", appliedUser.getHonorificPrefix());
		assertNull(appliedUser.getHonorificSuffix());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testApplyInboundSchemaHandlingOnUserAddWithFilter() throws Exception {
		JAXBElement<AccountShadowType> accountJaxb = (JAXBElement<AccountShadowType>) JAXBUtil
				.unmarshal(new File("src/test/resources/account-xpath-evaluation-filter.xml"));
		List<Element> domAttrs = accountJaxb.getValue().getAttributes().getAny();
		for (Element e : domAttrs) {
			if ("cn".equals(e.getLocalName())) {
				e.setTextContent("jan\u0007 prvy");
			}
		}
		UserType appliedUser = schemaHandling.applyInboundSchemaHandlingOnUser(new UserType(),
				accountJaxb.getValue());
		assertEquals("jan prvy", appliedUser.getFullName());
		assertEquals("Mr.", appliedUser.getHonorificPrefix());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testApplyInboundSchemaHandlingOnEmptyUserExtension() throws Exception {
		JAXBElement<AccountShadowType> accountJaxb = (JAXBElement<AccountShadowType>) JAXBUtil
				.unmarshal(new File("src/test/resources/account-xpath-evaluation-extension.xml"));
		UserType appliedUser = schemaHandling.applyInboundSchemaHandlingOnUser(new UserType(),
				accountJaxb.getValue());
		assertNotNull(appliedUser.getExtension());
		assertEquals("MikeFromExtension", appliedUser.getExtension().getAny().get(0).getTextContent());
		assertEquals("DudikoffFromExtension", appliedUser.getExtension().getAny().get(1).getTextContent());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConfirmUser() throws Exception {
		JAXBElement<AccountShadowType> accountJaxb = (JAXBElement<AccountShadowType>) JAXBUtil
				.unmarshal(new File("src/test/resources/account-xpath-evaluation.xml"));
		JAXBElement<UserType> userJaxb = (JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
				"src/test/resources/user-new.xml"));
		Document doc = DOMUtil
				.parseDocument("<confirmation xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd' xmlns:dj='http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ'>$c:user/c:givenName = $c:account/c:attributes/dj:givenName</confirmation>");
		Element domElement = (Element) doc.getFirstChild();
		ExpressionHolder expressionHolder = new ExpressionHolder(domElement);
		boolean confirmed = schemaHandling.confirmUser(userJaxb.getValue(), accountJaxb.getValue(),
				expressionHolder);
		assertTrue(confirmed);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEvaluateCorrelationExpression() throws Exception {
		JAXBElement<AccountShadowType> accountJaxb = (JAXBElement<AccountShadowType>) JAXBUtil
				.unmarshal(new File("src/test/resources/account-xpath-evaluation.xml"));
		Document doc = DOMUtil
				.parseDocument("<c:valueExpression ref='c:familyName' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd' xmlns:dj='http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ'>$c:account/c:attributes/dj:givenName</c:valueExpression>");
		Element domElement = (Element) doc.getFirstChild();
		ExpressionHolder expressionHolder = new ExpressionHolder(domElement);
		String evaluatedExpression = schemaHandling.evaluateCorrelationExpression(accountJaxb.getValue(),
				expressionHolder);
		assertEquals("James Jr.", evaluatedExpression);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testApplyUserTemplate() throws Exception {
		JAXBElement<AccountShadowType> accountJaxb = (JAXBElement<AccountShadowType>) JAXBUtil
				.unmarshal(new File("src/test/resources/account-user-template.xml"));
		UserType appliedUser = schemaHandling.applyInboundSchemaHandlingOnUser(new UserType(),
				accountJaxb.getValue());

		JAXBElement<UserTemplateType> userTemplate = (JAXBElement<UserTemplateType>) JAXBUtil
				.unmarshal(new File("src/test/resources/user-template.xml"));
		UserType finalAppliedUser = schemaHandling.applyUserTemplate(appliedUser, userTemplate.getValue());
		assertEquals("jan prvy", finalAppliedUser.getFullName());
	}
}
