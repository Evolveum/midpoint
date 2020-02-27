/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web;

import java.util.Collection;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-admin-gui-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCleanStartup extends AbstractModelIntegrationTest {

    public TestCleanStartup() {
        super();
        InternalsConfig.setAvoidLoggingChange(true);
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // The rest of the initialization happens as part of the spring context init
    }

    // work in progress
    @Test
    public void test001Logfiles() throws Exception {
        TestUtil.displayTestTitle("test001Logfiles");
        // GIVEN - system startup and initialization that has already happened
        LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME, false);

        // THEN
        display("Tailing ...");
        tailer.tail();
        display("... done");

        display("Errors", tailer.getErrors());
        display("Warnings", tailer.getWarnings());

        assertMessages("Error", tailer.getErrors(),
                "Unable to find file com/../../keystore.jceks",
                "Provided Icf connector path /C:/tmp is not a directory",
                "Provided Icf connector path C:\\tmp is not a directory",
                "Provided Icf connector path C:\\var\\tmp is not a directory",
                "Provided Icf connector path D:\\var\\tmp is not a directory");

        assertMessages("Warning", tailer.getWarnings());

        tailer.close();
    }

    private void assertMessages(String desc, Collection<String> actualMessages, String... expectedSubstrings) {
        for(String actualMessage: actualMessages) {
            boolean found = false;
            for (String expectedSubstring: expectedSubstrings) {
                if (actualMessage.contains(expectedSubstring)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                AssertJUnit.fail(desc+" \""+actualMessage+"\" was not expected ("+actualMessages.size()+" messages total)");
            }
        }
    }

}
