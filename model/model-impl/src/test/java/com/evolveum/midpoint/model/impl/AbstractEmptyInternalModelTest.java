/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import java.io.File;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


/**
 * Creates empty but functional environment for internal model tests - but without all the configured objects
 * in {@link AbstractInternalModelIntegrationTest}.
 *
 * Creates a repo with:
 *
 * - empty system configuration
 * - administrator user (from common dir)
 * - superuser role (from common dir)
 *
 * See also `AbstractEmptyModelIntegrationTest` in `model-intest` module.
 */
@Experimental
public abstract class AbstractEmptyInternalModelTest extends AbstractModelIntegrationTest {

    private static final File SYSTEM_CONFIGURATION_EMPTY_FILE = new File(COMMON_DIR, "system-configuration-empty.xml");

    private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
    protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

    private static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

    protected PrismObject<UserType> userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        logger.trace("initSystem");

        // We want logging config from logback-test.xml and not from system config object (unless suppressed)
        InternalsConfig.setAvoidLoggingChange(isAvoidLoggingChange());
        super.initSystem(initTask, initResult);

        PrismObject<SystemConfigurationType> configuration = repoAddObjectFromFile(getSystemConfigurationFile(), initResult);
        relationRegistry.applyRelationsConfiguration(configuration.asObjectable());

        modelService.postInit(initResult);

        userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, UserType.class, initResult);
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        login(userAdministrator);
    }

    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_EMPTY_FILE;
    }
}
