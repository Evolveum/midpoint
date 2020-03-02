/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.multi;

import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.*;

/**
 * Test resources that have several connectors.
 *
 * MID-5921
 *
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMultiConnectorResources extends AbstractConfiguredModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/multi-connector");

    private static final Trace LOGGER = TraceManager.getTrace(TestMultiConnectorResources.class);

    // Black dummy resource for testing tolerant attributes
    protected static final File RESOURCE_DUMMY_OPALINE_FILE = new File(TEST_DIR, "resource-dummy-opaline.xml");
    protected static final String RESOURCE_DUMMY_OPALINE_OID = "d4a2a030-0a1c-11ea-b61c-67d35cfea30f";
    protected static final String RESOURCE_DUMMY_OPALINE_NAME = "opaline";
    protected static final String RESOURCE_DUMMY_OPALINE_SCRIPT_NAME = "opaline-script";
    protected static final String RESOURCE_DUMMY_OPALINE_NAMESPACE = MidPointConstants.NS_RI;
    private static final String CONF_USELESS_OPALINE = "USEless-opaline";
    private static final String CONF_USELESS_SCRIPT = "USEless-script";


    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_OPALINE_NAME, RESOURCE_DUMMY_OPALINE_FILE, RESOURCE_DUMMY_OPALINE_OID, initTask, initResult);
        DummyResourceContoller opalineScriptController = DummyResourceContoller.create(RESOURCE_DUMMY_OPALINE_SCRIPT_NAME, getDummyResourceObject(RESOURCE_DUMMY_OPALINE_NAME));
        dummyResourceCollection.initDummyResource(RESOURCE_DUMMY_OPALINE_SCRIPT_NAME, opalineScriptController);

        repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);
        repoAddObjectFromFile(PASSWORD_POLICY_BENEVOLENT_FILE, initResult);

        repoAddObjectFromFile(USER_JACK_FILE, true, initResult);
        repoAddObjectFromFile(USER_GUYBRUSH_FILE, true, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        final String TEST_NAME = "test000Sanity";

        // GIVEN
        Task task = getTestTask();

        // WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_OPALINE_OID, task);

        // THEN
        display("Test result", testResult);
        TestUtil.assertSuccess("Opaline dummy test result", testResult);

        // Makes sure that both resources are configured
        assertEquals("Wrong OPALINE useless string", CONF_USELESS_OPALINE, getDummyResource(RESOURCE_DUMMY_OPALINE_NAME).getUselessString());
        assertEquals("Wrong OPALINE-SCRIPT useless string", CONF_USELESS_SCRIPT, getDummyResource(RESOURCE_DUMMY_OPALINE_SCRIPT_NAME).getUselessString());
    }

    @Test
    public void test100JackAssignDummyOpaline() throws Exception {
        final String TEST_NAME = "test100JackAssignDummyOpaline";

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OPALINE_OID, null, task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .singleLink()
                    .target()
                        .assertResource(RESOURCE_DUMMY_OPALINE_OID)
                        .assertName(ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccountByUsername(RESOURCE_DUMMY_OPALINE_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);
    }

}
