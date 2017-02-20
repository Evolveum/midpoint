/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.expr;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.controller.ModelController;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * 
 * @author lazyman
 * @author mederly
 * @author semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestModelExpressions extends AbstractInternalModelIntegrationTest {

	private static final File TEST_DIR = new File("src/test/resources/expr");

	private static final QName PROPERTY_NAME = new QName(SchemaConstants.NS_C, "foo");
	
	private static final Trace LOGGER = TraceManager.getTrace(TestModelExpressions.class);

    private static final String CHEF_OID = "00000003-0000-0000-0000-000000000000";
    private static final String CHEESE_OID = "00000002-0000-0000-0000-000000000000";
    private static final String CHEESE_JR_OID = "00000002-0000-0000-0000-000000000001";
    private static final String ELAINE_OID = "00000001-0000-0000-0000-000000000000";
    private static final String LECHUCK_OID = "00000007-0000-0000-0000-000000000000";
    private static final String F0006_OID = "00000000-8888-6666-0000-100000000006";

    @Autowired(required=true)
	private ScriptExpressionFactory scriptExpressionFactory;

    @Autowired(required = true)
    private TaskManager taskManager;

    private static final File TEST_EXPRESSIONS_OBJECTS_FILE = new File(TEST_DIR, "orgstruct.xml");

    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		importObjectFromFile(TEST_EXPRESSIONS_OBJECTS_FILE);
	}

	@Test
	public void testHello() throws Exception {
		final String TEST_NAME = "testHello";
		TestUtil.displayTestTile(this, TEST_NAME);
		
        assertExecuteScriptExpressionString(TEST_NAME, null, "Hello swashbuckler");
	}

    private ScriptExpressionEvaluatorType parseScriptType(String fileName) throws SchemaException, IOException, JAXBException {
		ScriptExpressionEvaluatorType expressionType = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, fileName), ScriptExpressionEvaluatorType.COMPLEX_TYPE);
		return expressionType;
	}

    @Test
    public void testGetUserByOid() throws Exception {
        final String TEST_NAME = "testGetUserByOid";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(TestModelExpressions.class.getName() + "." + TEST_NAME);
        PrismObject<UserType> chef = repositoryService.getObject(UserType.class, CHEF_OID, null, result);
        
        ExpressionVariables variables = ExpressionVariables.create(ExpressionConstants.VAR_USER, chef);

        // WHEN, THEN
        assertExecuteScriptExpressionString(TEST_NAME, variables, chef.asObjectable().getName().getOrig());
    }

    @Test
    public void testGetManagersOids() throws Exception {
        final String TEST_NAME = "testGetManagersOids";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(TestModelExpressions.class.getName() + "." + TEST_NAME);

        PrismObject<UserType> chef = repositoryService.getObject(UserType.class, CHEF_OID, null, result);

        ScriptExpressionEvaluatorType scriptType = parseScriptType("expression-" + TEST_NAME + ".xml");
        PrismPropertyDefinition<String> outputDefinition = new PrismPropertyDefinition<>(PROPERTY_NAME, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());
        ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(scriptType, outputDefinition, TEST_NAME);
        ExpressionVariables variables = new ExpressionVariables();
        variables.addVariableDefinition(new QName(SchemaConstants.NS_C, "user"), chef);

        // WHEN
        List<PrismPropertyValue<String>> scriptOutputs = evaluate(scriptExpression, variables, false, TEST_NAME, null, result);

        // THEN
        display("Script output", scriptOutputs);
        assertEquals("Unexpected number of script outputs", 3, scriptOutputs.size());
        Set<String> oids = new HashSet<String>();
        oids.add(scriptOutputs.get(0).getValue());
        oids.add(scriptOutputs.get(1).getValue());
        oids.add(scriptOutputs.get(2).getValue());
        Set<String> expectedOids = new HashSet<String>(Arrays.asList(new String[] { CHEESE_OID, CHEESE_JR_OID, LECHUCK_OID }));
        assertEquals("Unexpected script output", expectedOids, oids);
    }
    
    /**
     * MID-2887
     */
    @Test
    public void testIsUniquePropertyValue() throws Exception {
        final String TEST_NAME = "testIsUniquePropertyValue";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(TestModelExpressions.class.getName() + "." + TEST_NAME);

        PrismObject<UserType> chef = repositoryService.getObject(UserType.class, CHEF_OID, null, result);

        ScriptExpressionEvaluatorType scriptType = parseScriptType("expression-" + TEST_NAME + ".xml");
        PrismPropertyDefinition<Boolean> outputDefinition = new PrismPropertyDefinition<>(PROPERTY_NAME, DOMUtil.XSD_BOOLEAN, PrismTestUtil.getPrismContext());
        ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(scriptType, outputDefinition, TEST_NAME);
        ExpressionVariables variables = new ExpressionVariables();
        variables.addVariableDefinition(new QName(SchemaConstants.NS_C, "user"), chef);
        variables.addVariableDefinition(new QName(SchemaConstants.NS_C, "value"), "Scumm Bar Chef");

        // WHEN
        List<PrismPropertyValue<Boolean>> scriptOutputs = evaluate(scriptExpression, variables, false, TEST_NAME, null, result);

        // THEN
        display("Script output", scriptOutputs);
        assertEquals("Unexpected number of script outputs", 1, scriptOutputs.size());
        Boolean scriptOutput = scriptOutputs.get(0).getValue();
        assertEquals("Unexpected script output", Boolean.TRUE, scriptOutput);
    }

    @Test
    public void testGetOrgByName() throws Exception {
        final String TEST_NAME = "testGetOrgByName";
        TestUtil.displayTestTile(this, TEST_NAME);
        assertExecuteScriptExpressionString(TEST_NAME, null, F0006_OID);
    }
    
    @Test
    public void testGetLinkedShadowName() throws Exception {
        final String TEST_NAME = "testGetLinkedShadowName";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        rememberShadowFetchOperationCount();
        
        ExpressionVariables variables = ExpressionVariables.create(ExpressionConstants.VAR_USER, getUser(USER_GUYBRUSH_OID));
        
        assertExecuteScriptExpressionString(TEST_NAME, variables, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        
        assertShadowFetchOperationCountIncrement(1);
    }

    @Test
    public void testGetLinkedShadowKindIntentUsername() throws Exception {
        final String TEST_NAME = "testGetLinkedShadowKindIntentUsername";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        rememberShadowFetchOperationCount();
        
        ExpressionVariables variables = ExpressionVariables.create(ExpressionConstants.VAR_USER, getUser(USER_GUYBRUSH_OID));
        
        assertExecuteScriptExpressionString(TEST_NAME, variables, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        
        assertShadowFetchOperationCountIncrement(1);
    }
    
    @Test
    public void testGetLinkedShadowKindIntentFullname() throws Exception {
        final String TEST_NAME = "testGetLinkedShadowKindIntentFullname";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        rememberShadowFetchOperationCount();
        
        ExpressionVariables variables = ExpressionVariables.create(ExpressionConstants.VAR_USER, getUser(USER_GUYBRUSH_OID));
        
        assertExecuteScriptExpressionString(TEST_NAME, variables, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME);
        
        assertShadowFetchOperationCountIncrement(1);
    }
    
    @Test
    public void testGetLinkedShadowNameRepo() throws Exception {
        final String TEST_NAME = "testGetLinkedShadowNameRepo";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        rememberShadowFetchOperationCount();
        
        ExpressionVariables variables = ExpressionVariables.create(ExpressionConstants.VAR_USER, getUser(USER_GUYBRUSH_OID));
        
        assertExecuteScriptExpressionString(TEST_NAME, variables, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        
        assertShadowFetchOperationCountIncrement(0);
    }

    @Test
    public void testGetLinkedShadowKindIntentUsernameRepo() throws Exception {
        final String TEST_NAME = "testGetLinkedShadowKindIntentUsernameRepo";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        rememberShadowFetchOperationCount();
        
        ExpressionVariables variables = ExpressionVariables.create(ExpressionConstants.VAR_USER, getUser(USER_GUYBRUSH_OID));
        
        assertExecuteScriptExpressionString(TEST_NAME, variables, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        
        assertShadowFetchOperationCountIncrement(0);
    }
    
    @Test
    public void testGetLinkedShadowKindIntentFullnameRepo() throws Exception {
        final String TEST_NAME = "testGetLinkedShadowKindIntentFullnameRepo";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        rememberShadowFetchOperationCount();
        
        ExpressionVariables variables = ExpressionVariables.create(ExpressionConstants.VAR_USER, getUser(USER_GUYBRUSH_OID));
        
        assertExecuteScriptExpressionString(TEST_NAME, variables, null);
        
        assertShadowFetchOperationCountIncrement(0);
    }

    
    private void assertExecuteScriptExpressionString(final String TEST_NAME, ExpressionVariables variables, String expectedOutput) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, IOException, JAXBException {
    	String output = executeScriptExpressionString(TEST_NAME, variables);
    	assertEquals("Unexpected script output", expectedOutput, output);
    }
    
    private String executeScriptExpressionString(final String TEST_NAME, ExpressionVariables variables) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
    	// GIVEN
    	OperationResult result = new OperationResult(TestModelExpressions.class.getName() + "." + TEST_NAME);
    	
    	ScriptExpressionEvaluatorType scriptType = parseScriptType("expression-" + TEST_NAME + ".xml");
    	ItemDefinition outputDefinition = new PrismPropertyDefinition(PROPERTY_NAME, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());
        ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(scriptType, outputDefinition, TEST_NAME);
        if (variables == null) {
        	variables = new ExpressionVariables();
        }

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<PrismPropertyValue<String>> scriptOutputs = evaluate(scriptExpression, variables, false, TEST_NAME, null, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        display("Script output", scriptOutputs);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        if (scriptOutputs.size() == 0) {
        	return null;
        }
        
        assertEquals("Unexpected number of script outputs", 1, scriptOutputs.size());
        return scriptOutputs.get(0).getValue();
    	
    }

    private <T> List<PrismPropertyValue<T>> evaluate(ScriptExpression scriptExpression, ExpressionVariables variables, boolean useNew,
                                                      String contextDescription, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        if (task == null) {
            task = taskManager.createTaskInstance();
        }
        try {
            ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));

            return scriptExpression.evaluate(variables, null, useNew, contextDescription, task, result);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

}
