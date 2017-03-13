/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.util.Collection;

import com.evolveum.midpoint.util.aspect.ProfilingDataManager;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.connector.DummyConnector;
import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubSystemLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLoggingConfiguration extends AbstractConfiguredModelIntegrationTest {
	
	final String JUL_LOGGER_NAME = "com.exmple.jul.logger"; 
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		InternalsConfig.avoidLoggingChange = false;
		// DO NOT call super.initSystem() as this will install system config. We do not want that here.
		userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
		repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
		login(userAdministrator);
	}
	
	@Test
	public void test001CreateSystemConfiguration() throws Exception {
		final String TEST_NAME = "test001CreateSystemConfiguration";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
		
		PrismObject<SystemConfigurationType> systemConfiguration = PrismTestUtil.parseObject(SYSTEM_CONFIGURATION_FILE);
		Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName()+"."+TEST_NAME);
		OperationResult result = task.getResult();
		ObjectDelta<SystemConfigurationType> systemConfigurationAddDelta = ObjectDelta.createAddDelta(systemConfiguration);		
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(systemConfigurationAddDelta);
		
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		tailer.logAndTail();
		
		assertBasicLogging(tailer);
		// TODO: more asserts
		
		tailer.close();
		
	}

	
	@Test
	public void test002InitialConfiguration() throws Exception {
		final String TEST_NAME = "test002InitialConfiguration";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
		
		Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName()+"."+TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<SystemConfigurationType> systemConfiguration = PrismTestUtil.parseObject(SYSTEM_CONFIGURATION_FILE);
		LoggingConfigurationType logging = systemConfiguration.asObjectable().getLogging();
		
		applyTestLoggingConfig(logging);
		
		SubSystemLoggerConfigurationType modelSubSystemLogger = new SubSystemLoggerConfigurationType();
		modelSubSystemLogger.setComponent(LoggingComponentType.PROVISIONING);
		modelSubSystemLogger.setLevel(LoggingLevelType.TRACE);
		logging.getSubSystemLogger().add(modelSubSystemLogger);
		
		PrismObjectDefinition<SystemConfigurationType> systemConfigurationTypeDefinition =
			prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(SystemConfigurationType.class);
		Collection<? extends ItemDelta> modifications = 
			PropertyDelta.createModificationReplacePropertyCollection(SystemConfigurationType.F_LOGGING, 
					systemConfigurationTypeDefinition, logging);
		
		// Modify directly in repository, so the logging code in model will not notice the change
		repositoryService.modifyObject(SystemConfigurationType.class, AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_OID,
				modifications, result);
		
		// precondition
		tailer.logAndTail();		
		assertBasicLogging(tailer);
		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_TRACE, ProfilingDataManager.Subsystem.PROVISIONING.name());
		
		// WHEN
		repositoryService.postInit(result);
		modelService.postInit(result);

		// THEN
		tailer.logAndTail();
		
		assertBasicLogging(tailer);

		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_TRACE, ProfilingDataManager.Subsystem.PROVISIONING.name());
		
		tailer.close();
		
	}
	
	/**
	 * Overwrite initial system configuration by itself. Check that everything
	 * still works.
	 */
	@Test
	public void test004OverwriteInitialConfiguration() throws Exception {
		final String TEST_NAME = "test004OverwriteInitialConfiguration";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
		
		Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName()+"."+TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<SystemConfigurationType> systemConfiguration = getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value());
		String previousVersion = systemConfiguration.getVersion();
		systemConfiguration.setVersion(null);
				
		// precondition
		tailer.logAndTail();		
		assertBasicLogging(tailer);
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_TRACE, ProfilingDataManager.Subsystem.PROVISIONING.name());
		
		ObjectDelta<SystemConfigurationType> delta = ObjectDelta.createAddDelta(systemConfiguration);
		ModelExecuteOptions options = ModelExecuteOptions.createOverwrite();
		
		// WHEN
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), options, task, result);
		
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		tailer.logAndTail();
		
		assertBasicLogging(tailer);

		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_TRACE, ProfilingDataManager.Subsystem.PROVISIONING.name());
		
		tailer.close();
		
		PrismObject<SystemConfigurationType> systemConfigurationNew = getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value());
		String newVersion = systemConfigurationNew.getVersion();
		assertTrue("Versions do not follow: "+previousVersion+" -> "+newVersion,
				Integer.parseInt(previousVersion) < Integer.parseInt(newVersion));
		
	}
		
	@Test
	public void test010AddModelSubsystemLogger() throws Exception {
		final String TEST_NAME = "test010AddModelSubsystemLogger";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
		
		Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName()+"."+TEST_NAME);
		OperationResult result = task.getResult();
		
		// Precondition
		tailer.logAndTail();
		
		assertBasicLogging(tailer);

		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_DEBUG, ProfilingDataManager.Subsystem.MODEL.name());
		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_TRACE, ProfilingDataManager.Subsystem.MODEL.name());

		// Setup
		PrismObject<SystemConfigurationType> systemConfiguration = 
			PrismTestUtil.parseObject(AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_FILE);
		LoggingConfigurationType logging = systemConfiguration.asObjectable().getLogging();
		
		applyTestLoggingConfig(logging);
		
		SubSystemLoggerConfigurationType modelSubSystemLogger = new SubSystemLoggerConfigurationType();
		modelSubSystemLogger.setComponent(LoggingComponentType.MODEL);
		modelSubSystemLogger.setLevel(LoggingLevelType.DEBUG);
		logging.getSubSystemLogger().add(modelSubSystemLogger);
		
		ObjectDelta<SystemConfigurationType> systemConfigDelta = ObjectDelta.createModificationReplaceProperty(SystemConfigurationType.class, 
				AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_LOGGING, prismContext, 
				logging);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(systemConfigDelta);
		
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		tailer.logAndTail();
		
		assertBasicLogging(tailer);

		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_DEBUG, ProfilingDataManager.Subsystem.MODEL.name());
		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_TRACE, ProfilingDataManager.Subsystem.MODEL.name());
		
		// Test that the class logger for this test messages works
		// GIVEN
		tailer.setExpecteMessage("This is THE MESSage");
		
		// WHEN
		display("This is THE MESSage");
		
		// THEN
		tailer.tail();
		tailer.assertExpectedMessage();
		
		tailer.close();
		
	}


	@Test
	public void test020JulLoggingDisabled() throws Exception {
		final String TEST_NAME = "test020JulLoggingDisabled";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
		
		java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(JUL_LOGGER_NAME);
				
		// WHEN
		julLogger.severe(LogfileTestTailer.MARKER + " JULsevere");
		julLogger.warning(LogfileTestTailer.MARKER + " JULwarning");
		julLogger.info(LogfileTestTailer.MARKER + " JULinfo");
		julLogger.fine(LogfileTestTailer.MARKER + " JULfine");
		julLogger.finer(LogfileTestTailer.MARKER + " JULfiner");
		julLogger.finest(LogfileTestTailer.MARKER + " JULfinest");
		
		tailer.tail();
		
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_ERROR, "JULsevere");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_WARN, "JULwarning");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_INFO, "JULinfo");
		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_DEBUG, "JULfine");
		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_DEBUG, "JULfiner");
		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_TRACE, "JULfinest");
		
		tailer.close();
		
	}

	@Test
	public void test021JulLoggingEnabled() throws Exception {
		final String TEST_NAME = "test021JulLoggingEnabled";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
		
		java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(JUL_LOGGER_NAME);
		
		Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName()+"."+TEST_NAME);
		OperationResult result = task.getResult();
		
		// Setup
		PrismObject<SystemConfigurationType> systemConfiguration = 
			PrismTestUtil.parseObject(AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_FILE);
		LoggingConfigurationType logging = systemConfiguration.asObjectable().getLogging();
		
		applyTestLoggingConfig(logging);
		
		ClassLoggerConfigurationType classLogerCongif = new ClassLoggerConfigurationType();
		classLogerCongif.setPackage(JUL_LOGGER_NAME);
		classLogerCongif.setLevel(LoggingLevelType.ALL);
		logging.getClassLogger().add(classLogerCongif );
				
		ObjectDelta<SystemConfigurationType> systemConfigDelta = ObjectDelta.createModificationReplaceProperty(SystemConfigurationType.class, 
				AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_LOGGING, prismContext, 
				logging);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(systemConfigDelta);
		
		modelService.executeChanges(deltas, null, task, result);
		
		// WHEN
		julLogger.severe(LogfileTestTailer.MARKER + " JULsevere");
		julLogger.warning(LogfileTestTailer.MARKER + " JULwarning");
		julLogger.info(LogfileTestTailer.MARKER + " JULinfo");
		julLogger.fine(LogfileTestTailer.MARKER + " JULfine");
		julLogger.finer(LogfileTestTailer.MARKER + " JULfiner");
		julLogger.finest(LogfileTestTailer.MARKER + " JULfinest");
		
		tailer.tail();
		
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_ERROR, "JULsevere");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_WARN, "JULwarning");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_INFO, "JULinfo");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_DEBUG, "JULfine");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_DEBUG, "JULfiner");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_TRACE, "JULfinest");
		
		tailer.close();
		
	}
	
	/**
	 * Test if connectors log properly. The dummy connector logs on all levels when the
	 * "test" operation is invoked. So let's try it.
	 */
	@Test
	public void test030ConnectorLogging() throws Exception {
		final String TEST_NAME = "test030ConnectorLogging";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
		// ICF logging is prefixing the messages;
		tailer.setAllowPrefix(true);
		
		Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName()+"."+TEST_NAME);
		OperationResult result = task.getResult();
		
		importObjectFromFile(RESOURCE_DUMMY_FILE, result);
		
		// Setup
		PrismObject<SystemConfigurationType> systemConfiguration = 
			PrismTestUtil.parseObject(AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_FILE);
		LoggingConfigurationType logging = systemConfiguration.asObjectable().getLogging();
		
		applyTestLoggingConfig(logging);
		
		ClassLoggerConfigurationType classLogerCongif = new ClassLoggerConfigurationType();
		classLogerCongif.setPackage(DummyConnector.class.getPackage().getName());
		classLogerCongif.setLevel(LoggingLevelType.ALL);
		logging.getClassLogger().add(classLogerCongif );
				
		ObjectDelta<SystemConfigurationType> systemConfigDelta = ObjectDelta.createModificationReplaceProperty(SystemConfigurationType.class, 
				AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_LOGGING, prismContext, 
				logging);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(systemConfigDelta);
		
		modelService.executeChanges(deltas, null, task, result);
				
		// INFO part
		
		java.util.logging.Logger dummyConnctorJulLogger = java.util.logging.Logger.getLogger(DummyConnector.class.getName());
		LOGGER.info("Dummy connector JUL logger as seen by the test: {}; classloader {}", 
				dummyConnctorJulLogger, dummyConnctorJulLogger.getClass().getClassLoader());
		
		// WHEN
		modelService.testResource(RESOURCE_DUMMY_OID, task);
		
		// THEN
		tailer.tail();
		
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_ERROR, "DummyConnectorIcfError");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_WARN, "DummyConnectorIcfWarn");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_DEBUG, "DummyConnectorIcfInfo");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_TRACE, "DummyConnectorIcfOk");
		
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_ERROR, "DummyConnectorJULsevere");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_WARN, "DummyConnectorJULwarning");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_INFO, "DummyConnectorJULinfo");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_DEBUG, "DummyConnectorJULfine");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_DEBUG, "DummyConnectorJULfiner");
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_TRACE, "DummyConnectorJULfinest");
		
		tailer.close();
		
	}

	
	@Test
	public void test101EnableBasicAudit() throws Exception {
		TestUtil.displayTestTile("test101EnableBasicAudit");
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
		
		Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName()+".test101EnableBasicAudit");
		OperationResult result = task.getResult();
		
		// Precondition
		tailer.tail();
		tailer.assertNoAudit();

		// Setup
		PrismObject<SystemConfigurationType> systemConfiguration = 
			PrismTestUtil.parseObject(AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_FILE);
		LoggingConfigurationType logging = systemConfiguration.asObjectable().getLogging();
		
		applyTestLoggingConfig(logging);
		
		AuditingConfigurationType auditingConfigurationType = logging.getAuditing();
		if (auditingConfigurationType == null) {
			auditingConfigurationType = new AuditingConfigurationType();
			logging.setAuditing(auditingConfigurationType);
		}
		auditingConfigurationType.setEnabled(true);
		auditingConfigurationType.setDetails(false);
		
		ObjectDelta<SystemConfigurationType> systemConfigDelta = ObjectDelta.createModificationReplaceProperty(SystemConfigurationType.class, 
				AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_LOGGING, prismContext, 
				logging);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(systemConfigDelta);
		
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// Make sure that the (optional) audit message from the above change will not get into the way
		tailer.tail();
		tailer.reset();
		
		// This message will appear in the log and will help diagnose problems
		display("TEST: Applied audit config, going to execute test change");
		
		// try do execute some change (add user object), it should be audited
		PrismObject<UserType> user = PrismTestUtil.parseObject(AbstractInitializedModelIntegrationTest.USER_JACK_FILE);
		deltas = MiscSchemaUtil.createCollection(ObjectDelta.createAddDelta(user));
		
		modelService.executeChanges(deltas, null, task, result);

		// This message will appear in the log and will help diagnose problems
		display("TEST: Executed test change");
		
		// THEN
		
		tailer.tail();
		tailer.assertAudit(2);
		tailer.assertAuditRequest();
		tailer.assertAuditExecution();
		
		tailer.close();
		
	}

	private void applyTestLoggingConfig(LoggingConfigurationType logging) {
		// Make sure that this class has a special entry in the config so we will see the messages from this test code
		ClassLoggerConfigurationType testClassLogger = new ClassLoggerConfigurationType();
		testClassLogger.setPackage(TestLoggingConfiguration.class.getName());
		testClassLogger.setLevel(LoggingLevelType.TRACE);
		logging.getClassLogger().add(testClassLogger);
		
		ClassLoggerConfigurationType integrationTestToolsLogger = new ClassLoggerConfigurationType();
		integrationTestToolsLogger.setPackage(IntegrationTestTools.class.getName());
		integrationTestToolsLogger.setLevel(LoggingLevelType.TRACE);
		logging.getClassLogger().add(integrationTestToolsLogger);
	}
	
	private void assertBasicLogging(LogfileTestTailer tailer) {
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_ERROR, ProfilingDataManager.Subsystem.MODEL.name());
		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_TRACE, ProfilingDataManager.Subsystem.REPOSITORY.name());
	}

}
