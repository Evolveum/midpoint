/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 * @author mederly
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestModelExpressions extends AbstractInternalModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/expr");

    private static final QName PROPERTY_NAME = new QName(SchemaConstants.NS_C, "foo");

    private static final String CHEF_OID = "00000003-0000-0000-0000-000000000000";
    private static final String CHEESE_OID = "00000002-0000-0000-0000-000000000000";
    private static final String CHEESE_JR_OID = "00000002-0000-0000-0000-000000000001";
    private static final String ELAINE_OID = "00000001-0000-0000-0000-000000000000";
    private static final String LECHUCK_OID = "00000007-0000-0000-0000-000000000000";
    private static final String F0006_OID = "00000000-8888-6666-0000-100000000006";

    @Autowired
    private ScriptExpressionFactory scriptExpressionFactory;
    @Autowired private ExpressionFactory expressionFactory;

    @Autowired
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
        assertExecuteScriptExpressionString(null, "Hello swashbuckler");
    }

    private ScriptExpressionEvaluatorType parseScriptType(String fileName)
            throws SchemaException, IOException {
        ScriptExpressionEvaluatorType expressionType = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, fileName), ScriptExpressionEvaluatorType.COMPLEX_TYPE);
        return expressionType;
    }

    @Test
    public void testGetUserByOid() throws Exception {
        // GIVEN
        PrismObject<UserType> chef = repositoryService.getObject(
                UserType.class, CHEF_OID, null, getTestOperationResult());

        ExpressionVariables variables = createVariables(ExpressionConstants.VAR_USER, chef, chef.getDefinition());

        // WHEN, THEN
        assertExecuteScriptExpressionString(variables, chef.asObjectable().getName().getOrig());
    }

    @Test
    public void testGetManagersOids() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();
        String shortTestName = getTestNameShort();

        PrismObject<UserType> chef = repositoryService.getObject(UserType.class, CHEF_OID, null, result);

        ScriptExpressionEvaluatorType scriptType = parseScriptType("expression-" + shortTestName + ".xml");
        PrismPropertyDefinition<String> outputDefinition =
                getPrismContext().definitionFactory().createPropertyDefinition(
                        PROPERTY_NAME, DOMUtil.XSD_STRING);
        ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(
                scriptType, outputDefinition, MiscSchemaUtil.getExpressionProfile(),
                expressionFactory, shortTestName, task, result);
        ExpressionVariables variables =
                createVariables(ExpressionConstants.VAR_USER, chef, chef.getDefinition());

        // WHEN
        List<PrismPropertyValue<String>> scriptOutputs =
                evaluate(scriptExpression, variables, false, shortTestName, null, result);

        // THEN
        display("Script output", scriptOutputs);
        assertEquals("Unexpected number of script outputs", 3, scriptOutputs.size());
        Set<String> oids = new HashSet<>();
        oids.add(scriptOutputs.get(0).getValue());
        oids.add(scriptOutputs.get(1).getValue());
        oids.add(scriptOutputs.get(2).getValue());
        Set<String> expectedOids = new HashSet<>(
                Arrays.asList(CHEESE_OID, CHEESE_JR_OID, LECHUCK_OID));
        assertEquals("Unexpected script output", expectedOids, oids);
    }

    /**
     * MID-2887
     */
    @Test
    public void testIsUniquePropertyValue() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String testName = getTestNameShort();

        PrismObject<UserType> chef = repositoryService.getObject(UserType.class, CHEF_OID, null, result);

        ScriptExpressionEvaluatorType scriptType = parseScriptType("expression-" + testName + ".xml");
        PrismPropertyDefinition<Boolean> outputDefinition = getPrismContext().definitionFactory().createPropertyDefinition(PROPERTY_NAME, DOMUtil.XSD_BOOLEAN);
        ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(scriptType, outputDefinition,
                MiscSchemaUtil.getExpressionProfile(), expressionFactory, testName, task, result);

        ExpressionVariables variables = createVariables(
                ExpressionConstants.VAR_USER, chef, chef.getDefinition(),
                ExpressionConstants.VAR_VALUE, "Scumm Bar Chef", String.class);

        // WHEN
        List<PrismPropertyValue<Boolean>> scriptOutputs = evaluate(scriptExpression, variables, false, testName, null, result);

        // THEN
        display("Script output", scriptOutputs);
        assertEquals("Unexpected number of script outputs", 1, scriptOutputs.size());
        Boolean scriptOutput = scriptOutputs.get(0).getValue();
        assertEquals("Unexpected script output", Boolean.TRUE, scriptOutput);
    }

    @Test
    public void testGetOrgByName() throws Exception {
        assertExecuteScriptExpressionString(null, F0006_OID);
    }

    @Test
    public void testGetLinkedShadowName() throws Exception {
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        ExpressionVariables variables = createVariables(
                ExpressionConstants.VAR_USER, user, user.getDefinition());

        assertExecuteScriptExpressionString(variables, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
    }

    @Test
    public void testGetLinkedShadowKindIntentUsername() throws Exception {
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        ExpressionVariables variables = createVariables(
                ExpressionConstants.VAR_USER, user, user.getDefinition());

        assertExecuteScriptExpressionString(variables, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
    }

    @Test
    public void testGetLinkedShadowKindIntentFullname() throws Exception {
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        ExpressionVariables variables = createVariables(
                ExpressionConstants.VAR_USER, user, user.getDefinition());

        assertExecuteScriptExpressionString(variables, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
    }

    @Test
    public void testGetLinkedShadowNameRepo() throws Exception {
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        ExpressionVariables variables = createVariables(
                ExpressionConstants.VAR_USER, user, user.getDefinition());

        assertExecuteScriptExpressionString(variables, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
    }

    @Test
    public void testGetLinkedShadowKindIntentUsernameRepo() throws Exception {
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        ExpressionVariables variables = createVariables(
                ExpressionConstants.VAR_USER, user, user.getDefinition());

        assertExecuteScriptExpressionString(variables, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
    }

    @Test
    public void testGetLinkedShadowKindIntentFullnameRepo() throws Exception {
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        ExpressionVariables variables = createVariables(
                ExpressionConstants.VAR_USER, user, user.getDefinition());

        assertExecuteScriptExpressionString(variables, null);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
    }

    private void assertExecuteScriptExpressionString(
            ExpressionVariables variables, String expectedOutput)
            throws ConfigurationException, ExpressionEvaluationException, ObjectNotFoundException,
            IOException, CommunicationException, SchemaException, SecurityViolationException {
        String output = executeScriptExpressionString(variables);
        assertEquals("Unexpected script output", expectedOutput, output);
    }

    private String executeScriptExpressionString(ExpressionVariables variables)
            throws SecurityViolationException, ExpressionEvaluationException, SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, IOException {
        // GIVEN
        Task task = createPlainTask("executeScriptExpressionString");
        OperationResult result = createOperationResult();
        String shortTestName = getTestNameShort();

        ScriptExpressionEvaluatorType scriptType = parseScriptType("expression-" + shortTestName + ".xml");
        ItemDefinition outputDefinition =
                getPrismContext().definitionFactory().createPropertyDefinition(
                        PROPERTY_NAME, DOMUtil.XSD_STRING);
        ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(
                scriptType, outputDefinition, MiscSchemaUtil.getExpressionProfile(),
                expressionFactory, shortTestName, task, result);
        if (variables == null) {
            variables = new ExpressionVariables();
        }

        // WHEN
        when();
        List<PrismPropertyValue<String>> scriptOutputs = evaluate(scriptExpression, variables, false, shortTestName, null, result);

        // THEN
        then();
        display("Script output", scriptOutputs);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        if (scriptOutputs.size() == 0) {
            return null;
        }

        assertEquals("Unexpected number of script outputs", 1, scriptOutputs.size());
        PrismPropertyValue<String> scriptOutput = scriptOutputs.get(0);
        if (scriptOutput == null) {
            return null;
        }
        return scriptOutput.getValue();

    }

    private <T> List<PrismPropertyValue<T>> evaluate(ScriptExpression scriptExpression, ExpressionVariables variables, boolean useNew,
            String contextDescription, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
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
