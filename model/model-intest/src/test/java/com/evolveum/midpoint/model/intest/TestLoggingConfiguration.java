/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.cast;
import static com.evolveum.midpoint.test.IntegrationTestTools.LOGGER;

import java.util.Collection;
import java.util.Iterator;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.connector.DummyConnector;
import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLoggingConfiguration extends AbstractConfiguredModelIntegrationTest {

    private static final String JUL_LOGGER_NAME = "com.example.jul.logger";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        InternalsConfig.setAvoidLoggingChange(false);
        // DO NOT call super.initSystem() as this will install system config. We do not want that here.
        userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        login(userAdministrator);
    }

    @Test
    public void test001CreateSystemConfiguration() throws Exception {
        // GIVEN
        LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);

        PrismObject<SystemConfigurationType> systemConfiguration = PrismTestUtil.parseObject(SYSTEM_CONFIGURATION_FILE);
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        ObjectDelta<SystemConfigurationType> systemConfigurationAddDelta = DeltaFactory.Object.createAddDelta(systemConfiguration);
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
        // GIVEN
        LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);

        Task task = createPlainTask();
        OperationResult result = task.getResult();

        PrismObject<SystemConfigurationType> systemConfiguration = PrismTestUtil.parseObject(SYSTEM_CONFIGURATION_FILE);
        LoggingConfigurationType logging = systemConfiguration.asObjectable().getLogging();

        applyTestLoggingConfig(logging);

        SubSystemLoggerConfigurationType modelSubSystemLogger = new SubSystemLoggerConfigurationType();
        modelSubSystemLogger.setComponent(LoggingComponentType.PROVISIONING);
        modelSubSystemLogger.setLevel(LoggingLevelType.TRACE);
        logging.getSubSystemLogger().add(modelSubSystemLogger);

        Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_LOGGING)
                .replace(logging.asPrismContainerValue().clone())
                .asItemDeltas();

        // Modify directly in repository, so the logging code in model will not notice the change
        plainRepositoryService.modifyObject(SystemConfigurationType.class, AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_OID,
                modifications, result);

        // precondition
        tailer.logAndTail();
        assertBasicLogging(tailer);
        tailer.assertMarkerNotLogged(LogfileTestTailer.LEVEL_TRACE, ProfilingDataManager.Subsystem.PROVISIONING.name());

        // WHEN
        repositoryService.postInit(result);
        provisioningService.postInit(result);
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
        // GIVEN
        LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);

        Task task = createPlainTask();
        OperationResult result = task.getResult();

        PrismObject<SystemConfigurationType> systemConfiguration = getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value());
        String previousVersion = systemConfiguration.getVersion();
        systemConfiguration.setVersion(null);

        // precondition
        tailer.logAndTail();
        assertBasicLogging(tailer);
        tailer.assertMarkerLogged(LogfileTestTailer.LEVEL_TRACE, ProfilingDataManager.Subsystem.PROVISIONING.name());

        ObjectDelta<SystemConfigurationType> delta = DeltaFactory.Object.createAddDelta(systemConfiguration);
        ModelExecuteOptions options = executeOptions().overwrite();

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
        assertTrue("Versions do not follow: " + previousVersion + " -> " + newVersion,
                Integer.parseInt(previousVersion) < Integer.parseInt(newVersion));

    }

    @Test
    public void test010AddModelSubsystemLogger() throws Exception {
        // GIVEN
        LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);

        Task task = createPlainTask();
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

        ObjectDelta<SystemConfigurationType> systemConfigDelta = prismContext.deltaFactory().object().createModificationReplaceContainer(SystemConfigurationType.class,
                AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_LOGGING,
                logging.clone());
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
        // GIVEN
        LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);

        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(JUL_LOGGER_NAME);

        Task task = createPlainTask();
        OperationResult result = task.getResult();

        // Setup
        PrismObject<SystemConfigurationType> systemConfiguration =
                PrismTestUtil.parseObject(AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_FILE);
        LoggingConfigurationType logging = systemConfiguration.asObjectable().getLogging();

        applyTestLoggingConfig(logging);

        ClassLoggerConfigurationType classLoggerConfig = new ClassLoggerConfigurationType();
        classLoggerConfig.setPackage(JUL_LOGGER_NAME);
        classLoggerConfig.setLevel(LoggingLevelType.ALL);
        logging.getClassLogger().add(classLoggerConfig);

        ObjectDelta<SystemConfigurationType> systemConfigDelta = prismContext.deltaFactory().object().createModificationReplaceContainer(SystemConfigurationType.class,
                AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_LOGGING,
                logging.clone());
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
        // GIVEN
        LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
        // ICF logging is prefixing the messages;
        tailer.setAllowPrefix(true);

        Task task = createPlainTask();
        OperationResult result = task.getResult();

        importObjectFromFile(RESOURCE_DUMMY_FILE, result);

        // Setup
        PrismObject<SystemConfigurationType> systemConfiguration =
                PrismTestUtil.parseObject(AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_FILE);
        LoggingConfigurationType logging = systemConfiguration.asObjectable().getLogging();

        applyTestLoggingConfig(logging);

        ClassLoggerConfigurationType classLoggerConfig = new ClassLoggerConfigurationType();
        classLoggerConfig.setPackage(DummyConnector.class.getPackage().getName());
        classLoggerConfig.setLevel(LoggingLevelType.ALL);
        logging.getClassLogger().add(classLoggerConfig);

        ObjectDelta<SystemConfigurationType> systemConfigDelta = prismContext.deltaFactory().object().createModificationReplaceContainer(SystemConfigurationType.class,
                AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_LOGGING,
                logging.clone());
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(systemConfigDelta);

        modelService.executeChanges(deltas, null, task, result);

        // INFO part

        java.util.logging.Logger dummyConnectorJulLogger =
                java.util.logging.Logger.getLogger(DummyConnector.class.getName());
        LOGGER.info("Dummy connector JUL logger as seen by the test: {}; classloader {}",
                dummyConnectorJulLogger, dummyConnectorJulLogger.getClass().getClassLoader());

        // WHEN
        modelService.testResource(RESOURCE_DUMMY_OID, task, result);

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
        // GIVEN
        LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);

        Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName() + ".test101EnableBasicAudit");
        OperationResult result = task.getResult();

        // Precondition
        tailer.tail();
        tailer.assertNoAudit();

        // Setup
        PrismObject<SystemConfigurationType> systemConfiguration =
                PrismTestUtil.parseObject(AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_FILE);
        LoggingConfigurationType logging = systemConfiguration.asObjectable().getLogging();

        applyTestLoggingConfig(logging);

        LoggingAuditingConfigurationType auditingConfigurationType = logging.getAuditing();
        if (auditingConfigurationType == null) {
            auditingConfigurationType = new LoggingAuditingConfigurationType();
            logging.setAuditing(auditingConfigurationType);
        }
        auditingConfigurationType.setEnabled(true);
        auditingConfigurationType.setDetails(false);

        ObjectDelta<SystemConfigurationType> systemConfigDelta = prismContext.deltaFactory().object().createModificationReplaceContainer(SystemConfigurationType.class,
                AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_LOGGING,
                logging.clone());
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
        deltas = MiscSchemaUtil.createCollection(DeltaFactory.Object.createAddDelta(user));

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

    // MID-5674
    @Test
    public void test110SetMaxHistory() throws Exception {
        // GIVEN
        Task task = taskManager.createTaskInstance(TestLoggingConfiguration.class.getName() + ".test101EnableBasicAudit");
        OperationResult result = task.getResult();

        // Setup
        PrismObject<SystemConfigurationType> systemConfiguration =
                PrismTestUtil.parseObject(AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_FILE);
        LoggingConfigurationType logging = systemConfiguration.asObjectable().getLogging();

        applyTestLoggingConfig(logging);

        ((FileAppenderConfigurationType) logging.getAppender().get(0)).setMaxHistory(100);

        Collection<ObjectDelta<? extends ObjectType>> deltas =
                cast(prismContext.deltaFor(SystemConfigurationType.class)
                        .item(SystemConfigurationType.F_LOGGING)
                        .replace(logging.asPrismContainerValue().clone())
                        .asObjectDeltas(AbstractInitializedModelIntegrationTest.SYSTEM_CONFIGURATION_OID));

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        assertNotNull("No logger", logger);
        Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
        RollingFileAppender<ILoggingEvent> fileAppender = null;
        while (appenderIterator.hasNext()) {
            Appender<ILoggingEvent> appender = appenderIterator.next();
            System.out.println("Appender: " + appender);
            if (appender instanceof RollingFileAppender) {
                fileAppender = (RollingFileAppender<ILoggingEvent>) appender;
                break;
            }
        }
        assertNotNull("No file appender", fileAppender);
        RollingPolicy rollingPolicy = fileAppender.getRollingPolicy();
        System.out.println("Rolling policy = " + rollingPolicy);
        assertTrue("Wrong type of rolling policy", rollingPolicy instanceof TimeBasedRollingPolicy);
        TimeBasedRollingPolicy<?> timeBasedRollingPolicy = (TimeBasedRollingPolicy<?>) rollingPolicy;
        assertEquals("Wrong maxHistory", 100, timeBasedRollingPolicy.getMaxHistory());
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
