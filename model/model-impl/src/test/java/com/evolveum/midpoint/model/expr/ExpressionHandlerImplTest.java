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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import javax.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml", 
		"classpath:application-context-configuration-test-no-repo.xml",
		"classpath:application-context-task.xml" })
public class ExpressionHandlerImplTest extends AbstractTestNGSpringContextTests {

	private static final Trace LOGGER = TraceManager.getTrace(ExpressionHandlerImplTest.class);
	private static final File TEST_FOLDER = new File("./src/test/resources");
	@Autowired
	private ModelController model;
	@Autowired
	private ExpressionHandler expressionHandler;

	@Test(expectedExceptions = ExpressionException.class)
	@SuppressWarnings("unchecked")
	public void testConfirmUserWithoutModel() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "./expr/account-xpath-evaluation-without-resource.xml"))).getValue();
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER, "./user-new.xml")))
				.getValue();

		ExpressionType expression = JAXBUtil
				.unmarshal(
						ExpressionType.class,
						"<object xsi:type=\"ExpressionType\" xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\" "
								+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
								+ "<code>declare namespace c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\";\n"
								+ "declare namespace dj=\"http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ\";\n"
								+ "$c:user/c:givenName = $c:account/c:attributes/dj:givenName</code></object>")
				.getValue();

		OperationResult result = new OperationResult("testConfirmUserWithoutModel");
		try {
			expressionHandler.evaluateConfirmationExpression(user, account, expression, result);
			Assert.fail();
		} finally {
			LOGGER.info(result.dump());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConfirmUser() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "./account-xpath-evaluation.xml"))).getValue();
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER, "./user-new.xml")))
				.getValue();

		ExpressionType expression = JAXBUtil
				.unmarshal(
						ExpressionType.class,
						"<object xsi:type=\"ExpressionType\" xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\" "
								+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
								+ "<code>declare namespace c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\";\n"
								+ "declare namespace dj=\"http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ\";\n"
								+ "$c:user/c:givenName = $c:account/c:attributes/dj:givenName</code></object>")
				.getValue();

		OperationResult result = new OperationResult("testConfirmUser");
		boolean confirmed = expressionHandler.evaluateConfirmationExpression(user, account, expression,
				result);
		LOGGER.info(result.dump());

		assertTrue(confirmed);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEvaluateExpression() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "./expr/account.xml"))).getValue();
		ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"./expr/resource.xml"))).getValue();
		account.setResource(resource);
		account.setResourceRef(null);

		Element valueExpressionElement = findChildElement(resource.getSynchronization().getCorrelation()
				.getFilter(), SchemaConstants.NS_C, "valueExpression");
		ExpressionType expression = XsdTypeConverter
				.toJavaValue(valueExpressionElement, ExpressionType.class);
		LOGGER.debug(DebugUtil.prettyPrint(expression));

		OperationResult result = new OperationResult("testCorrelationRule");
		expressionHandler.setModel(model);
		String name = expressionHandler.evaluateExpression(account, expression, result);
		LOGGER.info(result.dump());

		assertEquals("hbarbossa", name);
	}

	private Element findChildElement(Element element, String namespace, String name) {
		NodeList list = element.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && namespace.equals(node.getNamespaceURI())
					&& name.equals(node.getLocalName())) {
				return (Element) node;
			}
		}
		return null;
	}
}
