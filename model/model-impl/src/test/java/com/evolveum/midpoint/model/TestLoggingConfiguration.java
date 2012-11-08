/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.test.util.LogfileTestTailer;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.aspect.MidpointAspect;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuditingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SubSystemLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-configuration-test.xml",
        "classpath:application-context-provisioning.xml",
        "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml"})
public class TestLoggingConfiguration extends AbstractIntegrationTest {
	
	@Autowired(required = true)
	protected ModelService modelService;
	@Autowired(required = true)
	protected TaskManager taskManager;
	@Autowired(required = true)
	protected PrismContext prismContext;

	@Override
	public void initSystem(OperationResult initResult) throws Exception {
	}
	
	@Test
	public void test001CreateSystemConfiguration() throws Exception {
		displayTestTile("test001CreateSystemConfiguration");
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer();
		
		PrismObject<SystemConfigurationType> systemConfiguration = 
			PrismTestUtil.parseObject(new File(AbstractModelIntegrationTest.SYSTEM_CONFIGURATION_FILENAME));
		Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName()+".test001AddConfiguration");
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
		displayTestTile("test002InitialConfiguration");
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer();
		
		Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName()+".test002InitialConfiguration");
		OperationResult result = task.getResult();
		
		PrismObject<SystemConfigurationType> systemConfiguration = 
			PrismTestUtil.parseObject(new File(AbstractModelIntegrationTest.SYSTEM_CONFIGURATION_FILENAME));
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
		repositoryService.modifyObject(SystemConfigurationType.class, AbstractModelIntegrationTest.SYSTEM_CONFIGURATION_OID,
				modifications, result);
		
		// precondition
		tailer.logAndTail();		
		assertBasicLogging(tailer);
		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_TRACE, MidpointAspect.SUBSYSTEM_PROVISIONING);
		
		// WHEN
		modelService.postInit(result);
		
		// THEN
		tailer.logAndTail();
		
		assertBasicLogging(tailer);

		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_TRACE, MidpointAspect.SUBSYSTEM_PROVISIONING);
		
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

	@Test
	public void test003AddModelSubsystemLogger() throws Exception {
		displayTestTile("test003AddModelSubsystemLogger");
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer();
		
		Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName()+".test003AddModelSubsystemLogger");
		OperationResult result = task.getResult();
		
		// Precondition
		tailer.logAndTail();
		
		assertBasicLogging(tailer);

		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_DEBUG, MidpointAspect.SUBSYSTEM_MODEL);
		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_TRACE, MidpointAspect.SUBSYSTEM_MODEL);

		// Setup
		PrismObject<SystemConfigurationType> systemConfiguration = 
			PrismTestUtil.parseObject(new File(AbstractModelIntegrationTest.SYSTEM_CONFIGURATION_FILENAME));
		LoggingConfigurationType logging = systemConfiguration.asObjectable().getLogging();
		
		applyTestLoggingConfig(logging);
		
		SubSystemLoggerConfigurationType modelSubSystemLogger = new SubSystemLoggerConfigurationType();
		modelSubSystemLogger.setComponent(LoggingComponentType.MODEL);
		modelSubSystemLogger.setLevel(LoggingLevelType.DEBUG);
		logging.getSubSystemLogger().add(modelSubSystemLogger);
		
		ObjectDelta<SystemConfigurationType> systemConfigDelta = ObjectDelta.createModificationReplaceProperty(SystemConfigurationType.class, 
				AbstractModelIntegrationTest.SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_LOGGING, prismContext, 
				logging);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(systemConfigDelta);
		
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		tailer.logAndTail();
		
		assertBasicLogging(tailer);

		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_DEBUG, MidpointAspect.SUBSYSTEM_MODEL);
		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_TRACE, MidpointAspect.SUBSYSTEM_MODEL);
		
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
	public void test101EnableBasicAudit() throws Exception {
		displayTestTile("test101EnableBasicAudit");
		
		// GIVEN
		LogfileTestTailer tailer = new LogfileTestTailer();
		
		Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName()+".test101EnableBasicAudit");
		OperationResult result = task.getResult();
		
		// Precondition
		tailer.tail();
		tailer.assertNoAudit();

		// Setup
		PrismObject<SystemConfigurationType> systemConfiguration = 
			PrismTestUtil.parseObject(new File(AbstractModelIntegrationTest.SYSTEM_CONFIGURATION_FILENAME));
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
				AbstractModelIntegrationTest.SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_LOGGING, prismContext, 
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
		PrismObject<UserType> user = PrismTestUtil.parseObject(new File(AbstractModelIntegrationTest.USER_JACK_FILENAME));
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

	
	private void assertBasicLogging(LogfileTestTailer tailer) {
		tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_ERROR, MidpointAspect.SUBSYSTEM_MODEL);
		tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_TRACE, MidpointAspect.SUBSYSTEM_REPOSITORY);
	}

}
