/*
 * Copyright (c) 2016-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.orgstruct;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Caribbean orgstruct tests, assigning accounts to ordinary org members and managers.
 * This configuration is using direct configuration in root org.
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestOrgStructCaribbeanDirect extends AbstractOrgStructCaribbeanTest {

    protected static final File ORG_CARIBBEAN_FILE = new File(TEST_DIR, "org-caribbean-direct.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Override
    protected File getOrgCaribbeanFile() {
        return ORG_CARIBBEAN_FILE;
    }
}
