/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;

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
    public void testAuditSimple() throws IOException {
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
    private String parseAuditLineFromLogFile(String filename) throws IOException {
        File log = new File(filename);
        if (!log.exists()) {
            fail("Log file " + filename + " does not exist");
        }
        System.out.println("Log file length: " + log.length());
        if (log.length() == 0) {
            fail("Log file " + filename + " is empty");
        }
        try (BufferedReader br = new BufferedReader(new FileReader(log))) {
            for (;;) {
                String line = br.readLine();
                if (line == null) {
                    return null;
                } else if (line.contains(LoggingConfigurationManager.AUDIT_LOGGER_NAME)) {
                    return line;
                }
            }
        }
    }
}
