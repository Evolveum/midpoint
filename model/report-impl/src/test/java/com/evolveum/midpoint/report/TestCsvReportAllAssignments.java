/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.expr.MidpointFunctionsImpl;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvReportAllAssignments extends TestCsvReport {

    private static final int USERS = 50;

    static final TestResource<ReportType> REPORT_INDIRECT_ASSIGNMENTS = new TestResource<>(TEST_DIR_REPORTS,
            "report-indirect-assignments.xml", "7f1695f2-d826-4d78-a046-b8249b79d2b5");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // Only for Native repo, as Generic repo does not support reference search.
        if (!isNativeRepository()) {
            return;
        }
        super.initSystem(initTask, initResult);

        repoAdd(TASK_EXPORT_CLASSIC, initResult);

        repoAdd(REPORT_INDIRECT_ASSIGNMENTS, initResult);

        String appServiceOid = addObject(new ServiceType().name("appService").asPrismObject(), initTask, initResult);
        String appRoleOid = addObject(new RoleType().name("appRole")
                .inducement(new AssignmentType().targetRef(appServiceOid, ServiceType.COMPLEX_TYPE))
                .asPrismObject(), initTask, initResult);
        String businessRoleOid = addObject(new RoleType().name("businessRole")
                .inducement(new AssignmentType().targetRef(appRoleOid, RoleType.COMPLEX_TYPE))
                .asPrismObject(), initTask, initResult);

        // one user without metadata to check the report's robustness
        switchAccessesMetadata(false, initTask, initResult);
        addObject(new UserType().name("user-without-metadata")
                .assignment(new AssignmentType().targetRef(businessRoleOid, RoleType.COMPLEX_TYPE))
                .asPrismObject(), initTask, initResult);
        switchAccessesMetadata(true, initTask, initResult);

        for (int i = 1; i <= USERS; i++) {
            UserType user = new UserType()
                    .name(String.format("user-%05d", i))
                    .assignment(new AssignmentType().targetRef(businessRoleOid, RoleType.COMPLEX_TYPE));
            if (i % 3 == 0) {
                // To mix it up, every third user has also direct assignment to the service.
                user.assignment(new AssignmentType()
                        .targetRef(appServiceOid, ServiceType.COMPLEX_TYPE)
                        .activation(new ActivationType()
                                // for some output variation
                                .validFrom(MiscUtil.asXMLGregorianCalendar(Instant.now().minus(10, ChronoUnit.DAYS)))
                                .validTo(MiscUtil.asXMLGregorianCalendar(Instant.now().plus(1, ChronoUnit.DAYS)))));
            }
            addObject(user.asPrismObject(), initTask, initResult);
        }
    }

    @Test
    public void test100RunReport() throws Exception {
        skipIfNotNativeRepository();
        // 50 * 3 (normal) + 50 // 3 (direct assignments) + 3 (without metadata) + jack + header
        // (subscription footer is considered automatically later, do not count it here)
        testClassicExport(REPORT_INDIRECT_ASSIGNMENTS, 171, 7, null);
    }
}
