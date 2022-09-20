/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;

public class TestAuditServiceImpl extends AbstractRepoCommonTest {

    private static final String LOG_FILENAME = "target/test.log";

    @Autowired
    private AuditService auditService;

    @Autowired
    private TaskManager taskManager;

    @Test
    public void testAuditSimple() throws FileNotFoundException {
        given();
        AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.ADD_OBJECT);
        Task task = taskManager.createTaskInstance();

        when();
        auditService.audit(auditRecord, task, task.getResult());

        then();
        System.err.println("FOOOOOOOOOOOOO");
        String auditLine = parseAuditLineFromLogFile(LOG_FILENAME);
        assertThat(auditLine).isNotNull();
        System.out.println("Audit line:");
        System.out.println(auditLine);
    }

    @SuppressWarnings("SameParameterValue")
    private String parseAuditLineFromLogFile(String filename) throws FileNotFoundException {
        File log = new File(filename);
        if (!log.exists()) {
            fail("Log file " + filename + " does not exist");
        }
        System.out.println("Log file length: " + log.length());
        if (log.length() == 0) {
            fail("Log file " + filename + " is empty");
        }
        String auditLine = null;
        Scanner input = new Scanner(log);

        while (input.hasNext()) {
            String line = input.nextLine();
            if (line.matches(".*" + LoggingConfigurationManager.AUDIT_LOGGER_NAME + ".*")) {
                auditLine = line;
            }
        }
        input.close();
        return auditLine;
    }
}
