/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.intest.scripting;

import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.scripting.Data;
import com.evolveum.midpoint.model.scripting.ExecutionContext;
import com.evolveum.midpoint.model.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.model.test.LogfileTestTailer;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ExpressionPipelineType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ExpressionSequenceType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ExpressionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestScriptingBasic extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/scripting");
    private static final String DOT_CLASS = TestScriptingBasic.class.getName() + ".";
    private static final File LOG_FILE = new File(TEST_DIR, "log.xml");
    private static final File SEARCH_FOR_USERS_FILE = new File(TEST_DIR, "search-for-users.xml");
    private static final File SEARCH_FOR_SHADOWS_FILE = new File(TEST_DIR, "search-for-shadows.xml");
    private static final File SEARCH_FOR_SHADOWS_NOFETCH_FILE = new File(TEST_DIR, "search-for-shadows-nofetch.xml");
    private static final File SEARCH_FOR_RESOURCES_FILE = new File(TEST_DIR, "search-for-resources.xml");
    private static final File SEARCH_FOR_USERS_ACCOUNTS_FILE = new File(TEST_DIR, "search-for-users-accounts.xml");
    private static final File SEARCH_FOR_USERS_ACCOUNTS_NOFETCH_FILE = new File(TEST_DIR, "search-for-users-accounts-nofetch.xml");
    private static final File DISABLE_JACK_FILE = new File(TEST_DIR, "disable-jack.xml");
    private static final File ENABLE_JACK_FILE = new File(TEST_DIR, "enable-jack.xml");
    private static final File DELETE_AND_ADD_JACK_FILE = new File(TEST_DIR, "delete-and-add-jack.xml");
    private static final File MODIFY_JACK_FILE = new File(TEST_DIR, "modify-jack.xml");
    private static final File MODIFY_JACK_BACK_FILE = new File(TEST_DIR, "modify-jack-back.xml");

    @Autowired
    private ScriptingExpressionEvaluator scriptingExpressionEvaluator;

    @Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		InternalMonitor.reset();
//		InternalMonitor.setTraceShadowFetchOperation(true);
//		InternalMonitor.setTraceResourceSchemaOperations(true);
	}

    @Test
    public void test100EmptySequence() throws Exception {
        TestUtil.displayTestTile(this, "test100EmptySequence");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test100EmptySequence");
        ExpressionSequenceType sequence = new ExpressionSequenceType();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(sequence, result);

        // THEN
        assertNoOutputData(output);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test110EmptyPipeline() throws Exception {
        TestUtil.displayTestTile(this, "test110EmptyPipeline");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test110EmptyPipeline");
        ExpressionPipelineType pipeline = new ExpressionPipelineType();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(pipeline, result);

        // THEN
        assertNoOutputData(output);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test120Log() throws Exception {
        TestUtil.displayTestTile(this, "test120Log");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test120Log");
        ExpressionType action = prismContext.getPrismJaxbProcessor().unmarshalElement(LOG_FILE, ExpressionType.class).getValue();

        LogfileTestTailer tailer = new LogfileTestTailer();
        tailer.tail();
        tailer.setExpecteMessage("Custom message:");

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(action, result);

        // THEN
        assertNoOutputData(output);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        tailer.tail();
        tailer.assertExpectedMessage();
    }

    @Test
    public void test200SearchUser() throws Exception {
        TestUtil.displayTestTile(this, "test200SearchUser");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test200SearchUser");
        ExpressionType expression = prismContext.getPrismJaxbProcessor().unmarshalElement(SEARCH_FOR_USERS_FILE, ExpressionType.class).getValue();

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression, result).getFinalOutput();

        // THEN
        IntegrationTestTools.display("output", output.getData());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(2, output.getData().size());
        //assertEquals("administrator", ((PrismObject<UserType>) output.getData().get(0)).asObjectable().getName().getOrig());
    }

    @Test
    public void test205SearchForResources() throws Exception {
        TestUtil.displayTestTile(this, "test205SearchForResources");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test205SearchForResources");
        ExpressionType expression = prismContext.getPrismJaxbProcessor().unmarshalElement(SEARCH_FOR_RESOURCES_FILE, ExpressionType.class).getValue();

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression, result).getFinalOutput();

        // THEN
        IntegrationTestTools.display("output", output.getData());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(9, output.getData().size());
    }

    @Test
    public void test210SearchForShadows() throws Exception {
        TestUtil.displayTestTile(this, "test210SearchForShadows");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test210SearchForShadows");
        ExpressionType expression = prismContext.getPrismJaxbProcessor().unmarshalElement(SEARCH_FOR_SHADOWS_FILE, ExpressionType.class).getValue();

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression, result).getFinalOutput();

        // THEN
        IntegrationTestTools.display("output", output.getData());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(5, output.getData().size());
        assertAttributesFetched(output.getData());
    }

    @Test
    public void test215SearchForShadowsNoFetch() throws Exception {
        TestUtil.displayTestTile(this, "test215SearchForShadowsNoFetch");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test215SearchForShadowsNoFetch");
        ExpressionType expression = prismContext.getPrismJaxbProcessor().unmarshalElement(SEARCH_FOR_SHADOWS_NOFETCH_FILE, ExpressionType.class).getValue();

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression, result).getFinalOutput();

        // THEN
        IntegrationTestTools.display("output", output.getData());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(5, output.getData().size());
        assertAttributesNotFetched(output.getData());
    }

    @Test
    public void test220SearchForUsersAccounts() throws Exception {
        TestUtil.displayTestTile(this, "test220SearchForUsersAccounts");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test220SearchForUsersAccounts");
        ExpressionType expression = prismContext.getPrismJaxbProcessor().unmarshalElement(SEARCH_FOR_USERS_ACCOUNTS_FILE, ExpressionType.class).getValue();

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression, result).getFinalOutput();

        // THEN
        IntegrationTestTools.display("output", output.getData());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(4, output.getData().size());
        assertAttributesFetched(output.getData());
    }

    @Test
    public void test225SearchForUsersAccountsNoFetch() throws Exception {
        TestUtil.displayTestTile(this, "test225SearchForUsersAccountsNoFetch");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test225SearchForUsersAccountsNoFetch");
        ExpressionType expression = prismContext.getPrismJaxbProcessor().unmarshalElement(SEARCH_FOR_USERS_ACCOUNTS_NOFETCH_FILE, ExpressionType.class).getValue();

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression, result).getFinalOutput();

        // THEN
        IntegrationTestTools.display("output", output.getData());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(4, output.getData().size());
        assertAttributesNotFetched(output.getData());
    }

    @Test
    public void test300DisableJack() throws Exception {
        TestUtil.displayTestTile(this, "test300DisableJack");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test300DisableJack");
        ExpressionType expression = prismContext.getPrismJaxbProcessor().unmarshalElement(DISABLE_JACK_FILE, ExpressionType.class).getValue();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getStdOut());
        assertEquals("Disabled user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getStdOut());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertAdministrativeStatusDisabled(searchObjectByName(UserType.class, "jack"));
    }

    @Test
    public void test310EnableJack() throws Exception {
        TestUtil.displayTestTile(this, "test310EnableJack");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test310EnableJack");
        ExpressionType expression = prismContext.getPrismJaxbProcessor().unmarshalElement(ENABLE_JACK_FILE, ExpressionType.class).getValue();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getStdOut());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Enabled user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getStdOut());
        assertAdministrativeStatusEnabled(searchObjectByName(UserType.class, "jack"));
    }

    @Test
    public void test320DeleteAndAddJack() throws Exception {
        TestUtil.displayTestTile(this, "test320DeleteAndAddJack");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test320DeleteAndAddJack");
        ExpressionType expression = prismContext.getPrismJaxbProcessor().unmarshalElement(DELETE_AND_ADD_JACK_FILE, ExpressionType.class).getValue();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getStdOut());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Deleted user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\nAdded user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getStdOut());
        assertAdministrativeStatusEnabled(searchObjectByName(UserType.class, "jack"));
    }

    @Test
    public void test330ModifyJack() throws Exception {
        TestUtil.displayTestTile(this, "test330ModifyJack");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test330ModifyJack");
        ExpressionType expression = prismContext.getPrismJaxbProcessor().unmarshalElement(MODIFY_JACK_FILE, ExpressionType.class).getValue();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getStdOut());
        IntegrationTestTools.display(result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Modified user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getStdOut());
        assertEquals("Nowhere", searchObjectByName(UserType.class, "jack").asObjectable().getLocality().getOrig());
    }

    @Test
    public void test340ModifyJackBack() throws Exception {
        TestUtil.displayTestTile(this, "test340ModifyJackBack");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test340ModifyJackBack");
        ExpressionType expression = prismContext.getPrismJaxbProcessor().unmarshalElement(MODIFY_JACK_BACK_FILE, ExpressionType.class).getValue();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getStdOut());
        IntegrationTestTools.display(result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Modified user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getStdOut());
        assertEquals("Caribbean", searchObjectByName(UserType.class, "jack").asObjectable().getLocality().getOrig());
    }

    private void assertNoOutputData(ExecutionContext output) {
        assertTrue("Script returned unexpected data", output.getFinalOutput() == null || output.getFinalOutput().getData().isEmpty());
    }

    // the following tests are a bit crude but for now it should be OK

    private void assertAttributesNotFetched(List<Item> data) {
        for (Item item : data) {
            if (((PrismObject<ShadowType>) item).asObjectable().getAttributes().getAny().size() > 2) {
                throw new AssertionError("There are some unexpected attributes present in " + item.debugDump());
            }
        }
    }

    private void assertAttributesFetched(List<Item> data) {
        for (Item item : data) {
            if (((PrismObject<ShadowType>) item).asObjectable().getAttributes().getAny().size() <= 2) {
                throw new AssertionError("There are no attributes present in " + item.debugDump());
            }
        }
    }



}
