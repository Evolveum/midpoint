/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.model.intest.CommonArchetypes.ARCHETYPE_TASK_ITERATIVE_BULK_ACTION;
import static com.evolveum.midpoint.model.intest.CommonArchetypes.ARCHETYPE_TASK_SINGLE_BULK_ACTION;
import static com.evolveum.midpoint.model.intest.CommonTasks.TASK_TRIGGER_SCANNER_ON_DEMAND;

import java.io.File;
import java.io.IOException;

import com.evolveum.axiom.concepts.CheckedSupplier;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.ManualConnectorInstance;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Creates empty but functional environment for model integration tests - but without all the configured objects
 * in {@link AbstractConfiguredModelIntegrationTest}.
 *
 * Creates a repo with:
 *
 * - empty system configuration
 * - administrator user (from common dir)
 * - superuser role (from common dir)
 *
 * There are some standard archetypes defined, but not imported. Individual tests should import them if necessary.
 */
@Experimental
public abstract class AbstractEmptyModelIntegrationTest extends AbstractModelIntegrationTest {

    private static final File SYSTEM_CONFIGURATION_EMPTY_FILE = new File(COMMON_DIR, "system-configuration-empty.xml");

    static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
    protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

    protected static final TestObject<RoleType> ROLE_SUPERUSER = TestObject.file(
            COMMON_DIR, "role-superuser.xml", "00000000-0000-0000-0000-000000000004");

    protected PrismObject<UserType> userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        logger.trace("initSystem");

        // We want logging config from logback-test.xml and not from system config object (unless suppressed)
        InternalsConfig.setAvoidLoggingChange(isAvoidLoggingChange());
        super.initSystem(initTask, initResult);

        // System Configuration
        PrismObject<SystemConfigurationType> configuration;
        try {
            File systemConfigurationFile = getSystemConfigurationFile();
            if (systemConfigurationFile != null) {
                configuration = repoAddObjectFromFile(systemConfigurationFile, initResult);
            } else {
                configuration = addSystemConfigurationObject(initResult);
            }
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }
        if (configuration != null) {
            relationRegistry.applyRelationsConfiguration(configuration.asObjectable());
        }

        modelService.postInit(initResult);
        ManualConnectorInstance.setRandomDelayRange(0);

        // Users
        userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, UserType.class, initResult);
        repoAdd(ROLE_SUPERUSER, initResult);
        login(userAdministrator);
    }

    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_EMPTY_FILE;
    }

    // to be used in very specific cases only (it is invoked when getSystemConfigurationFile returns null).
    protected PrismObject<SystemConfigurationType> addSystemConfigurationObject(OperationResult initResult)
            throws IOException, CommonException, EncryptionException {
        return null;
    }

    // To be used when needed
    protected void importTaskArchetypes(OperationResult initResult) throws SchemaException,
            ObjectAlreadyExistsException, EncryptionException, IOException {
        repoAdd(ARCHETYPE_TASK_ITERATIVE_BULK_ACTION, initResult);
        repoAdd(ARCHETYPE_TASK_SINGLE_BULK_ACTION, initResult);
    }

    /**
     * Runs "trigger scanner on demand" task: starts it and waits for its completion.
     *
     * Note that the task must be imported before using this method. This method can be then run
     * repeatedly.
     */
    protected Task runTriggerScannerOnDemand(OperationResult result) throws CommonException {
        return rerunTask(TASK_TRIGGER_SCANNER_ON_DEMAND.oid, result);
    }

    protected Task runTriggerScannerOnDemandErrorsOk(OperationResult result) throws CommonException {
        return rerunTaskErrorsOk(TASK_TRIGGER_SCANNER_ON_DEMAND.oid, result);
    }

    protected void loginAdministrator() throws SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        login(userAdministrator);
    }

    protected void setRecordResourceStageEnabled(boolean enable, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {

        ItemPath path = ItemPath.create(
                SystemConfigurationType.F_AUDIT,
                SystemConfigurationAuditType.F_EVENT_RECORDING,
                SystemConfigurationAuditEventRecordingType.F_RECORD_RESOURCE_STAGE_CHANGES);

        modifyObjectReplaceProperty(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                path, task, result, enable);
    }

    @SuppressWarnings("SameParameterValue")
    protected <T> T waitForFinish(
            CheckedSupplier<StatusInfo<T>, CommonException> statusInformationSupplier,
            long timeout) throws CommonException {

        var checker = new Checker() {

            StatusInfo<T> lastStatusInformation;

            @Override
            public boolean check() throws CommonException {
                lastStatusInformation = statusInformationSupplier.get();
                return lastStatusInformation.status() != OperationResultStatus.IN_PROGRESS;
            }

            @Override
            public void timeout() {
                fail("Timeout while waiting for the operation to finish. Last status: " + lastStatusInformation);
            }
        };

        IntegrationTestTools.waitFor("Waiting for the operation to finish", checker, timeout, 500);
        if (checker.lastStatusInformation.status() != OperationResultStatus.SUCCESS) {
            fail("Operation did not finish successfully. Last status: " + checker.lastStatusInformation);
        }
        return checker.lastStatusInformation.result();
    }
}
