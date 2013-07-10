/*
 * Copyright (c) 2010-2013 Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.init.InfraInitialSetup;
import com.evolveum.midpoint.init.InitialDataImport;
import com.evolveum.midpoint.init.ModelInitialSetup;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.test.LogfileTestTailer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/ctx-webapp.xml",
        "file:src/main/webapp/WEB-INF/ctx-init.xml",
        "file:src/main/webapp/WEB-INF/ctx-security.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath*:ctx-repository.xml",
        "classpath:ctx-task.xml",
        "classpath:ctx-audit.xml",
        "classpath:ctx-configuration-test.xml",
        "classpath:ctx-common.xml",
        "classpath:ctx-provisioning.xml",
        "classpath:ctx-model.xml",
        "classpath*:ctx-workflow.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCleanStartup extends AbstractModelIntegrationTest {
	
	@Autowired(required=true)
	private InfraInitialSetup infraInitialSetup;
	
	@Autowired(required=true)
	private ModelInitialSetup modelInitialSetup;
	
	@Autowired(required=true)
	private InitialDataImport initialDataImport;
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		infraInitialSetup.init();
		modelInitialSetup.init();
		initialDataImport.init();
	}

	// work in progress
	@Test
	public void test001Logfiles() throws Exception {
		TestUtil.displayTestTile("test001Logfiles");
		
		// GIVEN - system startup and initialization that has already happened
		LogfileTestTailer tailer = new LogfileTestTailer(false);
				
		// THEN
		display("Tailing ...");
		tailer.tail();
		display("... done");
		
		display("Errors", tailer.getErrors());
		display("Warnings", tailer.getWarnings());
		
//		assertEquals("E", 0, tailer.getErrors().size());
//		assertEquals("W", 0, tailer.getWarnings().size());
		
		tailer.close();
	}

}
