/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * More extensive and specialized caching tests.
 *
 * The basic tests are in {@link TestDummyCaching} and its subclasses, as a part of standard {@link TestDummy} procedures.
 * This class provides more specialized tests.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyCachingExtra extends AbstractDummyTest {

    public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-caching-extra");
    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

    @Override
    public void initSystem(Task task, OperationResult result) throws Exception {
        super.initSystem(task, result);

        testResourceAssertSuccess(RESOURCE_DUMMY_OID, task, result);

        resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        resourceBean = resource.asObjectable();
        rememberSteadyResources();

        addAccountDaemon(result);
    }

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Test
    public static void a() {
        //TODO
    }
}
