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

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExpressionPipelineType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExpressionSequenceType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.SearchExpressionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;

import java.io.File;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author mederly
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
    private static final File SEARCH_FOR_ROLES_FILE = new File(TEST_DIR, "search-for-roles.xml");
    private static final File SEARCH_FOR_USERS_ACCOUNTS_FILE = new File(TEST_DIR, "search-for-users-accounts.xml");
    private static final File SEARCH_FOR_USERS_ACCOUNTS_NOFETCH_FILE = new File(TEST_DIR, "search-for-users-accounts-nofetch.xml");
    private static final File DISABLE_JACK_FILE = new File(TEST_DIR, "disable-jack.xml");
    private static final File ENABLE_JACK_FILE = new File(TEST_DIR, "enable-jack.xml");
    private static final File DELETE_AND_ADD_JACK_FILE = new File(TEST_DIR, "delete-and-add-jack.xml");
    private static final File MODIFY_JACK_FILE = new File(TEST_DIR, "modify-jack.xml");
    private static final File MODIFY_JACK_BACK_FILE = new File(TEST_DIR, "modify-jack-back.xml");
    private static final File RECOMPUTE_JACK_FILE = new File(TEST_DIR, "recompute-jack.xml");;
    private static final File ASSIGN_TO_JACK_FILE = new File(TEST_DIR, "assign-to-jack.xml");
    private static final File ASSIGN_TO_JACK_2_FILE = new File(TEST_DIR, "assign-to-jack-2.xml");
    private static final File PURGE_DUMMY_BLACK_SCHEMA_FILE = new File(TEST_DIR, "purge-dummy-black-schema.xml");
    private static final File TEST_DUMMY_RESOURCE_FILE = new File(TEST_DIR, "test-dummy-resource.xml");

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
        ObjectFactory of = new ObjectFactory();

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
        ObjectFactory of = new ObjectFactory();

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
        PrismProperty<ScriptingExpressionType> logAction = (PrismProperty) prismContext.parseAnyData(LOG_FILE);

        LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
        tailer.tail();
        tailer.setExpecteMessage("Custom message:");

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(logAction.getAnyValue().getValue(), result);

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
        PrismProperty<SearchExpressionType> expression = (PrismProperty) prismContext.parseAnyData(SEARCH_FOR_USERS_FILE);

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result).getFinalOutput();

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
        PrismProperty<SearchExpressionType> expression = (PrismProperty) prismContext.parseAnyData(SEARCH_FOR_RESOURCES_FILE);

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result).getFinalOutput();

        // THEN
        IntegrationTestTools.display("output", output.getData());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(10, output.getData().size());
    }

    @Test
    public void test206SearchForRoles() throws Exception {
        TestUtil.displayTestTile(this, "test206SearchForRoles");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test206SearchForRoles");
        PrismProperty<SearchExpressionType> expression = (PrismProperty) prismContext.parseAnyData(SEARCH_FOR_ROLES_FILE);

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result).getFinalOutput();

        // THEN
        IntegrationTestTools.display("output", output.getData());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        //assertEquals(9, output.getData().size());
    }

    @Test
    public void test210SearchForShadows() throws Exception {
        TestUtil.displayTestTile(this, "test210SearchForShadows");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test210SearchForShadows");
        PrismProperty<SearchExpressionType> expression = (PrismProperty) prismContext.parseAnyData(SEARCH_FOR_SHADOWS_FILE);

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result).getFinalOutput();

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
        PrismProperty<SearchExpressionType> expression = (PrismProperty) prismContext.parseAnyData(SEARCH_FOR_SHADOWS_NOFETCH_FILE);

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result).getFinalOutput();

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
        PrismProperty<SearchExpressionType> expression = (PrismProperty) prismContext.parseAnyData(SEARCH_FOR_USERS_ACCOUNTS_FILE);

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result).getFinalOutput();

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
        PrismProperty<SearchExpressionType> expression = (PrismProperty) prismContext.parseAnyData(SEARCH_FOR_USERS_ACCOUNTS_NOFETCH_FILE);

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result).getFinalOutput();

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
        PrismProperty<ScriptingExpressionType> expression = (PrismProperty) prismContext.parseAnyData(DISABLE_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getConsoleOutput());
        assertEquals("Disabled user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertAdministrativeStatusDisabled(searchObjectByName(UserType.class, "jack"));
    }

    @Test
    public void test310EnableJack() throws Exception {
        TestUtil.displayTestTile(this, "test310EnableJack");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test310EnableJack");
        PrismProperty<ScriptingExpressionType> expression = (PrismProperty) prismContext.parseAnyData(ENABLE_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getConsoleOutput());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Enabled user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        assertAdministrativeStatusEnabled(searchObjectByName(UserType.class, "jack"));
    }

    @Test
    public void test320DeleteAndAddJack() throws Exception {
        TestUtil.displayTestTile(this, "test320DeleteAndAddJack");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test320DeleteAndAddJack");
        PrismProperty<ScriptingExpressionType> expression = (PrismProperty) prismContext.parseAnyData(DELETE_AND_ADD_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getConsoleOutput());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Deleted user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\nAdded user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        assertAdministrativeStatusEnabled(searchObjectByName(UserType.class, "jack"));
    }

    @Test
    public void test330ModifyJack() throws Exception {
        TestUtil.displayTestTile(this, "test330ModifyJack");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test330ModifyJack");
        PrismProperty<ScriptingExpressionType> expression = (PrismProperty) prismContext.parseAnyData(MODIFY_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getConsoleOutput());
        IntegrationTestTools.display(result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Modified user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        assertEquals("Nowhere", searchObjectByName(UserType.class, "jack").asObjectable().getLocality().getOrig());
    }

    @Test
    public void test340ModifyJackBack() throws Exception {
        TestUtil.displayTestTile(this, "test340ModifyJackBack");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test340ModifyJackBack");
        PrismProperty<ScriptingExpressionType> expression = (PrismProperty) prismContext.parseAnyData(MODIFY_JACK_BACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getConsoleOutput());
        IntegrationTestTools.display(result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Modified user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        assertEquals("Caribbean", searchObjectByName(UserType.class, "jack").asObjectable().getLocality().getOrig());
    }

    @Test
    public void test350RecomputeJack() throws Exception {
        TestUtil.displayTestTile(this, "test350RecomputeJack");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test350RecomputeJack");
        PrismProperty<ScriptingExpressionType> expression = (PrismProperty) prismContext.parseAnyData(RECOMPUTE_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getConsoleOutput());
        IntegrationTestTools.display(result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Recomputed user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
    }

    @Test
    public void test360AssignToJack() throws Exception {
        TestUtil.displayTestTile(this, "test360AssignToJack");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test360AssignToJack");
        PrismProperty<ScriptingExpressionType> expression = (PrismProperty) prismContext.parseAnyData(ASSIGN_TO_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

        // THEN
        assertNoOutputData(output);
        IntegrationTestTools.display("stdout", output.getConsoleOutput());
        IntegrationTestTools.display(result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        //assertEquals("Recomputed user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        IntegrationTestTools.display("jack after assignments creation", jack);
        assertAssignedAccount(jack, "10000000-0000-0000-0000-000000000104");
        assertAssignedRole(jack, "12345678-d34d-b33f-f00d-55555555cccc");
    }

    @Test
    public void test370AssignToJackInBackground() throws Exception {
        TestUtil.displayTestTile(this, "test370AssignToJackInBackground");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test370AssignToJackInBackground");
        PrismProperty<ScriptingExpressionType> expression = (PrismProperty) prismContext.parseAnyData(ASSIGN_TO_JACK_2_FILE);

        // WHEN
        Task task = taskManager.createTaskInstance();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        scriptingExpressionEvaluator.evaluateExpressionInBackground(expression.getAnyValue().getValue(), task, result);
        waitForTaskFinish(task.getOid(), false);
        task.refresh(result);

        // THEN
        IntegrationTestTools.display(task.getResult());
        TestUtil.assertSuccess(task.getResult());
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        IntegrationTestTools.display("jack after assignment creation", jack);
        assertAssignedRole(jack, "12345678-d34d-b33f-f00d-555555556677");
    }

    @Test
    public void test380DisableJackInBackgroundSimple() throws Exception {
        TestUtil.displayTestTile(this, "test380DisableJackInBackgroundSimple");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test380DisableJackInBackgroundSimple");

        // WHEN
        Task task = taskManager.createTaskInstance();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        scriptingExpressionEvaluator.evaluateExpressionInBackground(UserType.COMPLEX_TYPE,
                ObjectQueryUtil.createOrigNameQuery("jack", prismContext).getFilter(),
                "disable", task, result);

        waitForTaskFinish(task.getOid(), false);
        task.refresh(result);

        // THEN
        IntegrationTestTools.display(task.getResult());
        TestUtil.assertSuccess(task.getResult());
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        IntegrationTestTools.display("jack after disable script", jack);
        assertAdministrativeStatusDisabled(jack);
    }

    @Test(enabled = true)
    public void test400PurgeSchema() throws Exception {
        TestUtil.displayTestTile(this, "test400PurgeSchema");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test400PurgeSchema");
        Task task = taskManager.createTaskInstance();
        PrismProperty<ScriptingExpressionType> expression = (PrismProperty) prismContext.parseAnyData(PURGE_DUMMY_BLACK_SCHEMA_FILE);

//        ResourceType dummy = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_BLACK_OID, null, task, result).asObjectable();
//        IntegrationTestTools.display("dummy resource before purge schema", dummy.asPrismObject());
//        IntegrationTestTools.display("elements: " + dummy.getSchema().getDefinition().getAny().get(0).getElementsByTagName("*").getLength());
//        IntegrationTestTools.display("schema as XML: " + DOMUtil.printDom(dummy.getSchema().getDefinition().getAny().get(0)));

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

        // THEN
        IntegrationTestTools.display("output", output.getFinalOutput());
        IntegrationTestTools.display("stdout", output.getConsoleOutput());
        IntegrationTestTools.display(result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(1, output.getFinalOutput().getData().size());

//        dummy = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_BLACK_OID, null, result).asObjectable();
//        IntegrationTestTools.display("dummy resource from repo", dummy.asPrismObject());
//        IntegrationTestTools.display("elements: " + dummy.getSchema().getDefinition().getAny().get(0).getElementsByTagName("*").getLength());
//        IntegrationTestTools.display("schema as XML: " + DOMUtil.printDom(dummy.getSchema().getDefinition().getAny().get(0)));

        //AssertJUnit.assertNull("Schema is still present", dummy.getSchema());
        // actually, schema gets downloaded just after purging it
        assertEquals("Purged schema information from resource:10000000-0000-0000-0000-000000000305(Dummy Resource Black)\n", output.getConsoleOutput());
    }


    @Test
    public void test410TestResource() throws Exception {
        TestUtil.displayTestTile(this, "test410TestResource");

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + "test410TestResource");
        PrismProperty<ScriptingExpressionType> expression = (PrismProperty) prismContext.parseAnyData(TEST_DUMMY_RESOURCE_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

        // THEN
        IntegrationTestTools.display("output", output.getFinalOutput());
        IntegrationTestTools.display("stdout", output.getConsoleOutput());
        ResourceType dummy = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, taskManager.createTaskInstance(), result).asObjectable();
        IntegrationTestTools.display("dummy resource after test connection", dummy.asPrismObject());
        IntegrationTestTools.display(result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(1, output.getFinalOutput().getData().size());
        assertEquals("Tested resource:10000000-0000-0000-0000-000000000004(Dummy Resource): SUCCESS\n", output.getConsoleOutput());
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
