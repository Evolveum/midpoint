/**
 * Copyright (c) 2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 *
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
		displayTestTitle(TEST_NAME);
		
		if (!isOsUnix()) {
			displaySkip(TEST_NAME);
			return;
		}
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
		
        CommandLineScriptType scriptType = getScript(REPORT_PLAIN_ECHO_FILE);
        
		// WHEN
		displayWhen(TEST_NAME);
		commandLineScriptExecutor.executeScript(scriptType, null, "test", task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
	}
	
	@Test
	public void test110RedirExecuteEcho() throws Exception {
		final String TEST_NAME = "test110RedirExecuteEcho";
		displayTestTitle(TEST_NAME);
		
		if (!isOsUnix()) {
			displaySkip(TEST_NAME);
			return;
		}
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
		
        CommandLineScriptType scriptType = getScript(REPORT_REDIR_ECHO_FILE);
        
        ExpressionVariables variables = new ExpressionVariables();
        variables.addVariableDefinition(VAR_HELLOTEXT, "Hello World");
        
		// WHEN
		displayWhen(TEST_NAME);
		commandLineScriptExecutor.executeScript(scriptType, variables, "test", task, result);
		
		// THEN
		displayThen(TEST_NAME);
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
