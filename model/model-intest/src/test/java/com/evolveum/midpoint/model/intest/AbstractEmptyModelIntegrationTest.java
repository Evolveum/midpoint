/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.ManualConnectorInstance;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.File;
import java.io.IOException;

/**
 * Creates empty but functional environment for model integration tests - but without all the configured objects
 * in {@link AbstractConfiguredModelIntegrationTest}.
 *
 * Creates a repo with:
 *  - empty system configuration
 *  - administrator user (from common dir)
 *  - superuser role (from common dir)
 *
 * There are some standard archetypes defined, but not imported. Individual tests should import them if necessary.
 */
@Experimental
public abstract class AbstractEmptyModelIntegrationTest extends AbstractModelIntegrationTest {

    private static final File SYSTEM_CONFIGURATION_EMPTY_FILE = new File(COMMON_DIR, "system-configuration-empty.xml");

    static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
    protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

    static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

    private static final TestResource<ArchetypeType> ARCHETYPE_TASK_ITERATIVE_BULK_ACTION = new TestResource<>(COMMON_DIR, "archetype-task-iterative-bulk-action.xml", SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value());
    private static final TestResource<ArchetypeType> ARCHETYPE_TASK_SINGLE_BULK_ACTION = new TestResource<>(COMMON_DIR, "archetype-task-single-bulk-action.xml", SystemObjectsType.ARCHETYPE_SINGLE_BULK_ACTION_TASK.value());

    protected PrismObject<UserType> userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        logger.trace("initSystem");

        // We want logging config from logback-test.xml and not from system config object (unless suppressed)
        InternalsConfig.setAvoidLoggingChange(isAvoidLoggingChange());
        super.initSystem(initTask, initResult);

        modelService.postInit(initResult);
        ManualConnectorInstance.setRandomDelayRange(0);

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

        // Users
        userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, UserType.class, initResult);
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
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
    @SuppressWarnings("WeakerAccess")
    protected void importTaskArchetypes(OperationResult initResult) throws SchemaException,
            ObjectAlreadyExistsException, EncryptionException, IOException {
        repoAdd(ARCHETYPE_TASK_ITERATIVE_BULK_ACTION, initResult);
        repoAdd(ARCHETYPE_TASK_SINGLE_BULK_ACTION, initResult);
    }
}
