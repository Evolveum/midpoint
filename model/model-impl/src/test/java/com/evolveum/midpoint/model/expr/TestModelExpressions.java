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

import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.common.expression.script.ScriptVariables;
import com.evolveum.midpoint.model.lens.TestProjector;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class TestModelExpressions extends AbstractTestNGSpringContextTests {

	private static final String TEST_DIR = "src/test/resources/expr";

	private static final QName PROPERTY_NAME = new QName(SchemaConstants.NS_C, "foo");
	
	private static final Trace LOGGER = TraceManager.getTrace(TestModelExpressions.class);

	
	@Autowired(required=true)
	private ScriptExpressionFactory scriptExpressionFactory;
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testHello() throws Exception {
		final String TEST_NAME = "testHello";
		displayTestTile(this, TEST_NAME);
		
		// GIVEN
        OperationResult result = new OperationResult(TestModelExpressions.class.getName() + "." + TEST_NAME);
		
		ScriptExpressionEvaluatorType scriptType = parseScriptType("expression-func.xml");
		ItemDefinition outputDefinition = new PrismPropertyDefinition(PROPERTY_NAME, PROPERTY_NAME, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());
		ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(scriptType, outputDefinition, TEST_NAME);
		
		ScriptVariables variables = null;
		
		// WHEN
		List<PrismPropertyValue<String>> scriptOutputs = scriptExpression.evaluate(variables, null, TEST_NAME, result);
		
		// THEN
		display("Script output", scriptOutputs);
		assertEquals("Unexpected numeber of script outputs", 1, scriptOutputs.size());
		PrismPropertyValue<String> scriptOutput = scriptOutputs.get(0);
		assertEquals("Unexpected script output", "Hello swashbuckler", scriptOutput.getValue());
		// TODO
		
	}
	
	private ScriptExpressionEvaluatorType parseScriptType(String fileName) throws SchemaException, FileNotFoundException, JAXBException {
		JAXBElement<ScriptExpressionEvaluatorType> expressionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, fileName), ScriptExpressionEvaluatorType.class);
		return expressionTypeElement.getValue();
	}

}
