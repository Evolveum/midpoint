/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.commandline;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CommandLineScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

/**
 * @author semancik
 */
@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestCommandLine extends AbstractIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/commandline");

    protected static final File REPORT_PLAIN_ECHO_FILE = new File(TEST_DIR, "report-plain-echo.xml");
    protected static final File REPORT_REDIR_ECHO_FILE = new File(TEST_DIR, "report-redir-echo.xml");

    private static final QName VAR_HELLOTEXT = new QName(SchemaConstants.NS_C, "hellotext");

    @Autowired private CommandLineScriptExecutor commandLineScriptExecutor;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
    }

    @Test
    public void test100PlainExecuteEcho() throws Exception {
        final String TEST_NAME = "test100PlainExecuteEcho";

        if (!isOsUnix()) {
            displaySkip(TEST_NAME);
            return;
        }

        Task task = getTestTask();
        OperationResult result = task.getResult();

        CommandLineScriptType scriptType = getScript(REPORT_PLAIN_ECHO_FILE);

        // WHEN
        when(TEST_NAME);
        commandLineScriptExecutor.executeScript(scriptType, null, "test", task, result);

        // THEN
        then(TEST_NAME);
        assertSuccess(result);

    }

    @Test
    public void test110RedirExecuteEcho() throws Exception {
        final String TEST_NAME = "test110RedirExecuteEcho";

        if (!isOsUnix()) {
            displaySkip(TEST_NAME);
            return;
        }

        Task task = getTestTask();
        OperationResult result = task.getResult();

        CommandLineScriptType scriptType = getScript(REPORT_REDIR_ECHO_FILE);

        ExpressionVariables variables = ExpressionVariables.create(prismContext,
            VAR_HELLOTEXT, "Hello World", PrimitiveType.STRING);

        // WHEN
        when(TEST_NAME);
        commandLineScriptExecutor.executeScript(scriptType, variables, "test", task, result);

        // THEN
        then(TEST_NAME);
        assertSuccess(result);

        File targetFile = new File(MidPointTestConstants.TARGET_DIR_PATH, "echo-out");
        assertTrue("Target file is not there", targetFile.exists());
        String targetFileContent = FileUtils.readFileToString(targetFile);
        assertEquals("Wrong target file content", "Hello World", targetFileContent);
    }

    private CommandLineScriptType getScript(File file) throws SchemaException, IOException {
        PrismObject<ReportType> report = parseObject(file);
        return report.asObjectable().getPostReportScript();
    }

}
