/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import org.testng.collections.Sets;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final File NOTIFICATION_ABOUT_JACK_FILE = new File(TEST_DIR, "notification-about-jack.xml");
    private static final File NOTIFICATION_ABOUT_JACK_TYPE2_FILE = new File(TEST_DIR, "notification-about-jack-type2.xml");
	private static final File SCRIPTING_USERS_FILE = new File(TEST_DIR, "scripting-users.xml");

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
    	final String TEST_NAME = "test100EmptySequence";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
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
    	final String TEST_NAME = "test110EmptyPipeline";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
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
    	final String TEST_NAME = "test120Log";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> logAction = parseAnyData(LOG_FILE);

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

    private PrismProperty parseAnyData(File file) throws IOException, SchemaException {
        return (PrismProperty) prismContext.parserFor(file).parseItem();
    }

    @Test
    public void test200SearchUser() throws Exception {
    	final String TEST_NAME = "test200SearchUser";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<SearchExpressionType> expression = parseAnyData(SEARCH_FOR_USERS_FILE);

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
    	final String TEST_NAME = "test205SearchForResources";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<SearchExpressionType> expression = parseAnyData(SEARCH_FOR_RESOURCES_FILE);

        // WHEN
        Data output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result).getFinalOutput();

        // THEN
        IntegrationTestTools.display("output", output.getData());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(13, output.getData().size());
    }

    @Test
    public void test206SearchForRoles() throws Exception {
    	final String TEST_NAME = "test206SearchForRoles";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<SearchExpressionType> expression = parseAnyData(SEARCH_FOR_ROLES_FILE);

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
    	final String TEST_NAME = "test210SearchForShadows";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<SearchExpressionType> expression = parseAnyData(SEARCH_FOR_SHADOWS_FILE);

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
    	final String TEST_NAME = "test215SearchForShadowsNoFetch";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<SearchExpressionType> expression = parseAnyData(SEARCH_FOR_SHADOWS_NOFETCH_FILE);

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
    	final String TEST_NAME = "test220SearchForUsersAccounts";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<SearchExpressionType> expression = parseAnyData(SEARCH_FOR_USERS_ACCOUNTS_FILE);

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
    	final String TEST_NAME = "test225SearchForUsersAccountsNoFetch";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<SearchExpressionType> expression = parseAnyData(SEARCH_FOR_USERS_ACCOUNTS_NOFETCH_FILE);

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
    	final String TEST_NAME = "test300DisableJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(DISABLE_JACK_FILE);

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
    	final String TEST_NAME = "test310EnableJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(ENABLE_JACK_FILE);

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
    	final String TEST_NAME = "test320DeleteAndAddJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(DELETE_AND_ADD_JACK_FILE);

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
    	final String TEST_NAME = "test330ModifyJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(MODIFY_JACK_FILE);

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
    	final String TEST_NAME = "test340ModifyJackBack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(MODIFY_JACK_BACK_FILE);

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
    	final String TEST_NAME = "test350RecomputeJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(RECOMPUTE_JACK_FILE);

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
    	final String TEST_NAME = "test360AssignToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(ASSIGN_TO_JACK_FILE);

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
    	final String TEST_NAME = "test370AssignToJackInBackground";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(ASSIGN_TO_JACK_2_FILE);

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

    @Deprecated
    @Test
    public void test380DisableJackInBackgroundSimple() throws Exception {
    	final String TEST_NAME = "test380DisableJackInBackgroundSimple";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);

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

    @Test
    public void test400PurgeSchema() throws Exception {
    	final String TEST_NAME = "test400PurgeSchema";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        Task task = taskManager.createTaskInstance();
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(PURGE_DUMMY_BLACK_SCHEMA_FILE);

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
    	final String TEST_NAME = "test410TestResource";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(TEST_DUMMY_RESOURCE_FILE);

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

    @Test
    public void test420NotificationAboutJack() throws Exception {
        final String TEST_NAME = "test420NotificationAboutJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(NOTIFICATION_ABOUT_JACK_FILE);
        prepareNotifications();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

        // THEN
        IntegrationTestTools.display("output", output.getFinalOutput());
        IntegrationTestTools.display("stdout", output.getConsoleOutput());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(0, output.getFinalOutput().getData().size());
        assertEquals("Produced 1 event(s)\n", output.getConsoleOutput());

        IntegrationTestTools.display("Dummy transport", dummyTransport);
        checkDummyTransportMessages("Custom", 1);
        Message m = dummyTransport.getMessages("dummy:Custom").get(0);
        assertEquals("Wrong message body", "jack/" + USER_JACK_OID, m.getBody());
        assertEquals("Wrong message subject", "Ad hoc notification", m.getSubject());
    }

    @Test
    public void test430NotificationAboutJackType2() throws Exception {
        final String TEST_NAME = "test430NotificationAboutJackType2";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
        PrismProperty<ScriptingExpressionType> expression = parseAnyData(NOTIFICATION_ABOUT_JACK_TYPE2_FILE);
        prepareNotifications();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

        // THEN
        IntegrationTestTools.display("output", output.getFinalOutput());
        IntegrationTestTools.display("stdout", output.getConsoleOutput());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(0, output.getFinalOutput().getData().size());
        assertEquals("Produced 1 event(s)\n", output.getConsoleOutput());

        IntegrationTestTools.display("Dummy transport", dummyTransport);
        checkDummyTransportMessages("Custom", 1);
        Message m = dummyTransport.getMessages("dummy:Custom").get(0);
        assertEquals("Wrong message body", "1", m.getBody());
        assertEquals("Wrong message subject", "Ad hoc notification 2", m.getSubject());

        checkDummyTransportMessages("CustomType2", 1);
        m = dummyTransport.getMessages("dummy:CustomType2").get(0);
        assertEquals("Wrong message body", "[POV:user:c0c010c0-d34d-b33f-f00d-111111111111(jack)]", m.getBody());
        assertEquals("Wrong message subject", "Failure notification of type 2", m.getSubject());
    }

    @Test
	public void test500ScriptingUsers() throws Exception {
		final String TEST_NAME = "test500ScriptingUsers";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(DOT_CLASS + TEST_NAME);
		PrismProperty<ScriptingExpressionType> expression = parseAnyData(SCRIPTING_USERS_FILE);

		// WHEN
		ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression.getAnyValue().getValue(), result);

		// THEN
		TestUtil.assertSuccess(result);
		Data data = output.getFinalOutput();
		assertEquals("Unexpected # of items in output", 5, data.getData().size());
		Set<String> realOids = new HashSet<>();
		for (PrismValue value : data.getData()) {
			PrismObject<UserType> user = ((PrismObjectValue<UserType>) value).asPrismObject();
			assertEquals("Description not set", "Test", user.asObjectable().getDescription());
			realOids.add(user.getOid());
		}
		assertEquals("Unexpected OIDs in output",
				Sets.newHashSet(Arrays.asList(USER_ADMINISTRATOR_OID, USER_JACK_OID, USER_BARBOSSA_OID, USER_GUYBRUSH_OID, USER_ELAINE_OID)),
				realOids);
		IntegrationTestTools.display("stdout", output.getConsoleOutput());
		IntegrationTestTools.display(result);
		result.computeStatus();
	}


	private void assertNoOutputData(ExecutionContext output) {
        assertTrue("Script returned unexpected data", output.getFinalOutput() == null || output.getFinalOutput().getData().isEmpty());
    }

    // the following tests are a bit crude but for now it should be OK

    private void assertAttributesNotFetched(List<PrismValue> data) {
        for (PrismValue value : data) {
            if (((PrismObjectValue<ShadowType>) value).asObjectable().getAttributes().getAny().size() > 2) {
                throw new AssertionError("There are some unexpected attributes present in " + value.debugDump());
            }
        }
    }

    private void assertAttributesFetched(List<PrismValue> data) {
        for (PrismValue value : data) {
            if (((PrismObjectValue<ShadowType>) value).asObjectable().getAttributes().getAny().size() <= 2) {
                throw new AssertionError("There are no attributes present in " + value.debugDump());
            }
        }
    }

}
