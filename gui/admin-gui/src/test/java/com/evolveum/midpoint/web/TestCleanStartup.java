/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.init.InfraInitialSetup;
import com.evolveum.midpoint.init.InitialDataImport;
import com.evolveum.midpoint.init.ModelInitialSetup;
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

	@Autowired(required=true)
	private InfraInitialSetup infraInitialSetup;

	@Autowired(required=true)
	private ModelInitialSetup modelInitialSetup;

	@Autowired(required=true)
	private InitialDataImport initialDataImport;

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
