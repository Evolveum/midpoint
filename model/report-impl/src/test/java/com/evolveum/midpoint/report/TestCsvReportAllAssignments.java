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

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestReport;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvReportAllAssignments extends TestCsvReport {

    private static final int USERS = 50;
    private static final int REPORT_COLUMN_COUNT = 10;

    private static final TestReport REPORT_INDIRECT_ASSIGNMENTS = TestReport.classPath(DIR_REPORTS,
            "report-indirect-assignments.xml", "7f1695f2-d826-4d78-a046-b8249b79d2b5");

    private static final TestTask TASK_EXPORT_CLASSIC_ROLE_CACHING = new TestTask(TEST_DIR_REPORTS,
            "task-export-role-caching.xml", "0ff414b6-76c6-4d38-95e8-d2d34c7a11cb");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // Only for Native repo, as Generic repo does not support reference search.
        if (!isNativeRepository()) {
            return;
        }
        super.initSystem(initTask, initResult);

        repoAdd(TASK_EXPORT_CLASSIC_ROLE_CACHING, initResult);
        repoAdd(REPORT_INDIRECT_ASSIGNMENTS, initResult);

        String appArchetypeOid = addObject(new ArchetypeType().name("Application")
                .asPrismObject(), initTask, initResult);
        String orgOid = addObject(new OrgType().name("Org1")
                .asPrismObject(), initTask, initResult);

        // adding role chain: businessRole -> appRole -> appService
        String appServiceOid = addObject(new ServiceType().name("appService")
                .assignment(new AssignmentType().targetRef(appArchetypeOid, ArchetypeType.COMPLEX_TYPE))
                .asPrismObject(), initTask, initResult);
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

        // Initialization of the bulk of the users
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
                                .validFrom(MiscUtil.asXMLGregorianCalendar(Instant.now().minus(REPORT_COLUMN_COUNT, ChronoUnit.DAYS)))
                                .validTo(MiscUtil.asXMLGregorianCalendar(Instant.now().plus(1, ChronoUnit.DAYS)))));
                user.assignment(new AssignmentType()
                        .targetRef(orgOid, OrgType.COMPLEX_TYPE,
                                i == 3 ? SchemaConstants.ORG_MANAGER : SchemaConstants.ORG_DEFAULT));
            }
            addObject(user.asPrismObject(), initTask, initResult);
        }

        // One user with metadata pointing to deleted role
        String deletedRoleOid = addObject(new RoleType().name("deletedRole").asPrismObject(), initTask, initResult);
        addObject(new UserType().name("user-with-deleted-role")
                .assignment(new AssignmentType().targetRef(deletedRoleOid, RoleType.COMPLEX_TYPE))
                .asPrismObject(), initTask, initResult);
        deleteObject(RoleType.class, deletedRoleOid);
    }

    @Test
    public void test100RunReport() throws Exception {
        skipIfNotNativeRepository();

        // 50 * 3 (normal) + 50 // 3 * 2 (direct assignments + orgs) + 3 (without metadata) + deleted + jack + header
        // (subscription footer is considered automatically later, do not count it here)
        testExport(TASK_EXPORT_CLASSIC_ROLE_CACHING, REPORT_INDIRECT_ASSIGNMENTS, 188, REPORT_COLUMN_COUNT, null, null);
    }
}
