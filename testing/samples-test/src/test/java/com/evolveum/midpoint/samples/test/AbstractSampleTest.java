/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.samples.test;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Try to import selected samples to a real repository in an initialized system.
 *
 * We cannot import all the samples as some of them are mutually exclusive.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-samples-test-main.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public abstract class AbstractSampleTest extends AbstractModelIntegrationTest {

    protected static final File SAMPLES_DIRECTORY = new File("target/samples");
    protected static final File SCHEMA_DIRECTORY = new File ("src/test/resources/schema");
    protected static final File USER_ADMINISTRATOR_FILE = new File("src/test/resources/user-administrator.xml");

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSampleTest.class);

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // This should discover the connectors
        LOGGER.trace("initSystem: trying modelService.postInit()");
        modelService.postInit(initResult);
        LOGGER.trace("initSystem: modelService.postInit() done");

        PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        loginSuperUser(userAdministrator);
    }

}
