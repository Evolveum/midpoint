/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.test;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.certification.impl.AccCertQueryHelper;
import com.evolveum.midpoint.certification.impl.AccCertUpdateHelper;
import com.evolveum.midpoint.certification.impl.CertificationManagerImpl;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class AbstractUninitializedCertificationTest extends AbstractModelIntegrationTest {

    public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");

    public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

    public static final File USER_ADMINISTRATOR_PLAIN_FILE = new File(COMMON_DIR, "user-administrator-plain.xml");
    protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

    @Autowired protected CertificationManagerImpl certificationManager;
    @Autowired protected AccessCertificationService certificationService;
    @Autowired protected AccCertUpdateHelper updateHelper;
    @Autowired protected AccCertQueryHelper queryHelper;

    protected RoleType roleSuperuser;
    protected UserType userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        logger.trace("initSystem");
        super.initSystem(initTask, initResult);

        provisioningService.postInit(initResult);
        modelService.postInit(initResult);

        // System Configuration
        try {
            repoAddObjectFromFile(getSystemConfigurationFile(), initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }

        // Administrator
        roleSuperuser = repoAddObjectFromFile(ROLE_SUPERUSER_FILE, RoleType.class, initResult).asObjectable();
        userAdministrator = repoAddObjectFromFile(getUserAdministratorFile(), UserType.class, initResult).asObjectable();
        login(userAdministrator.asPrismObject());
    }

    @NotNull
    protected File getUserAdministratorFile() {
        return USER_ADMINISTRATOR_PLAIN_FILE;
    }

    @NotNull
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

}
