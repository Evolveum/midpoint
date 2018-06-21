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
package com.evolveum.midpoint.audit.impl;

import static org.testng.AssertJUnit.assertNotNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-audit.xml",
		"classpath:ctx-task.xml",
        "classpath:ctx-audit-test.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath:ctx-repository-test.xml",
        "classpath:ctx-configuration-test.xml"})
public class TestAuditServiceImpl extends AbstractTestNGSpringContextTests {

	private static final String LOG_FILENAME = "target/test.log";

	@Autowired
	AuditService auditService;

	@Autowired
	TaskManager taskManager;

	@Test
	public void testAuditSimple() throws FileNotFoundException, InterruptedException {
		// GIVEN
		AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.ADD_OBJECT);
		Task task = taskManager.createTaskInstance();

		// WHEN
		auditService.audit(auditRecord, task);

		// THEN

		//Thread.sleep(2000);
		System.err.println("FOOOOOOOOOOOOO");
		String auditLine = parseAuditLineFromLogFile(LOG_FILENAME);
		assertNotNull(auditLine);
		System.out.println("Audit line:");
		System.out.println(auditLine);
	}

	private String parseAuditLineFromLogFile(String filename) throws FileNotFoundException {
		File log = new File(filename);
		if (!log.exists()) {
			AssertJUnit.fail("Log file "+filename+" does not exist");
		}
		System.out.println("Log file length: "+log.length());
		if (log.length() == 0) {
			AssertJUnit.fail("Log file "+filename+" is empty");
		}
		String auditLine = null;
		Scanner input = new Scanner(log);

		while(input.hasNext()) {
		    String line = input.nextLine();
		    if (line.matches(".*"+LoggingConfigurationManager.AUDIT_LOGGER_NAME+".*")) {
		    	auditLine = line;
		    }
		}
		input.close();
		return auditLine;
	}

}
