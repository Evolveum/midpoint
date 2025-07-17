/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

import java.io.File;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Abstract superclass for Smart Integration tests.
 */
public abstract class AbstractSmartIntegrationTest extends AbstractModelIntegrationTest {

    public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");

    private static final TestObject<UserType> USER_ADMINISTRATOR = TestObject.file(
            COMMON_DIR, "user-administrator.xml", "00000000-0000-0000-0000-000000000002");
    private static final TestObject<RoleType> ROLE_SUPERUSER = TestObject.file(
            COMMON_DIR, "role-superuser.xml", "00000000-0000-0000-0000-000000000004");

    static final QName OC_ACCOUNT_QNAME = new QName(NS_RI, "account");

    /** Using the implementation in order to set mock service client for testing. */
    @Autowired SmartIntegrationServiceImpl smartIntegrationService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // We want logging config from logback-test.xml and not from system config object (unless suppressed)
        InternalsConfig.setAvoidLoggingChange(isAvoidLoggingChange());

        repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        repoAdd(USER_ADMINISTRATOR, initResult);
        repoAdd(ROLE_SUPERUSER, initResult);

        modelService.postInit(initResult);
        login(USER_ADMINISTRATOR.getNameOrig());
    }
}
