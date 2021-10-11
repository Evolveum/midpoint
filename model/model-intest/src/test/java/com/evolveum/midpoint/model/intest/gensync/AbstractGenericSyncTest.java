/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.gensync;

import java.io.File;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Generic synchronization test. We create role and assign a resource to it.
 * Entitlement (group) should be created.
 *
 * @author Radovan Semancik
 *
 */
@SuppressWarnings("unused")
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AbstractGenericSyncTest extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/gensync");

    static final File USER_OTIS_FILE = new File(TEST_DIR, "user-otis.xml");
    static final String USER_OTIS_OID = "fd5039c8-ddc8-11e4-8ec7-001e8c717e5b";
    static final String USER_OTIS_USERNAME = "otis";

    static final File ROLE_SWASHBUCKLER_FILE = new File(TEST_DIR, "role-swashbuckler.xml");
    static final String ROLE_SWASHBUCKLER_OID = "12345678-d34d-b33f-f00d-5b5b5b5b5b5b";
    static final String ROLE_SWASHBUCKLER_NAME = "Swashbuckler";
    static final String ROLE_SWASHBUCKLER_DESCRIPTION = "Requestable role Swashbuckler";

    static final File ROLE_PRISONER_FILE = new File(TEST_DIR, "role-prisoner.xml");
    static final String ROLE_PRISONER_OID = "90c332ec-ddc8-11e4-bb3b-001e8c717e5b";

    static final String GROUP_SWASHBUCKLER_DUMMY_NAME = "swashbuckler";

    private static final File ROLE_META_DUMMYGROUP_FILE = new File(TEST_DIR, "role-meta-dummygroup.xml");
    static final String ROLE_META_DUMMYGROUP_OID = "12348888-d34d-8888-8888-555555556666";

    private static final File SYSTEM_CONFIGURATION_GENSYNC_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final File OBJECT_TEMPLATE_ROLE_FILE = new File(TEST_DIR, "object-template-role.xml");

    private static final File LOOKUP_ROLE_TYPE_FILE = new File(TEST_DIR, "lookup-role-type.xml");
    static final String LOOKUP_ROLE_TYPE_OID = "70000000-0000-0000-1111-000000000021";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(OBJECT_TEMPLATE_ROLE_FILE, initResult);
        repoAddObjectFromFile(ROLE_META_DUMMYGROUP_FILE, initResult);
        repoAddObjectFromFile(LOOKUP_ROLE_TYPE_FILE, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_GENSYNC_FILE;
    }

}
