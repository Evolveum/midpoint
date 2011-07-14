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
package com.evolveum.midpoint.model.expr;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import javax.xml.bind.JAXBElement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.ExpressionHolder;

/**
 * 
 * @author lazyman
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml" })
public class ExpressionHandlerImplTest {

	private static final Trace LOGGER = TraceManager.getTrace(ExpressionHandlerImplTest.class);
	private static final File TEST_FOLDER = new File("./src/test/resources");
	@Autowired
	private ExpressionHandler expressionHandler;

	@Test(expected = ExpressionException.class)
	@SuppressWarnings("unchecked")
	public void testConfirmUserWithoutModel() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "./expr/account-xpath-evaluation-without-resource.xml"))).getValue();
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER, "./user-new.xml")))
				.getValue();

		Document doc = DOMUtil.parseDocument("<confirmation "
				+ "xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd' "
				+ "xmlns:dj='http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ'>"
				+ "$c:user/c:givenName = $c:account/c:attributes/dj:givenName</confirmation>");
		Element element = (Element) doc.getFirstChild();
		ExpressionHolder expression = new ExpressionHolder(element);

		OperationResult result = new OperationResult("testConfirmUserWithoutModel");
		try {
			expressionHandler.evaluateConfirmationExpression(user, account, expression, result);
			fail();
		} finally {
			LOGGER.info(result.debugDump());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConfirmUser() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "./account-xpath-evaluation.xml"))).getValue();
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER, "./user-new.xml")))
				.getValue();

		Document doc = DOMUtil.parseDocument("<confirmation "
				+ "xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd' "
				+ "xmlns:dj='http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ'>"
				+ "$c:user/c:givenName = $c:account/c:attributes/dj:givenName</confirmation>");
		Element element = (Element) doc.getFirstChild();
		ExpressionHolder expression = new ExpressionHolder(element);

		OperationResult result = new OperationResult("testConfirmUser");
		boolean confirmed = expressionHandler.evaluateConfirmationExpression(user, account, expression,
				result);
		LOGGER.info(result.debugDump());

		assertTrue(confirmed);
	}
}
