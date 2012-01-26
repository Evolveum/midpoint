/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
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
@ContextConfiguration(locations = {"classpath:application-context-task.xml",
        "classpath:application-context-task-test.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-configuration-test.xml"})
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
