/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static com.evolveum.midpoint.report.AbstractReportIntegrationTest.*;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * Common superclass for "empty" report integration tests.
 *
 * VERY EXPERIMENTAL
 *
 * TODO reconsider
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EmptyReportIntegrationTest extends AbstractModelIntegrationTest {

    static final File TEST_DIR_REPORTS = new File("src/test/resources/reports");
    static final File TEST_DIR_COMMON = new File("src/test/resources/common");
    private static final File EXPORT_DIR = new File("target/midpoint-home/export");

    static final TestResource<TaskType> TASK_EXPORT_CLASSIC = new TestResource<>(TEST_DIR_REPORTS,
            "task-export.xml", "d3a13f2e-a8c0-4f8c-bbf9-e8996848bddf");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        commonInitialization(initResult);
    }

    // TODO deduplicate
    void commonInitialization(OperationResult initResult)
            throws CommonException, EncryptionException, IOException {
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, RepoAddOptions.createOverwrite(), false, initResult);

        modelService.postInit(initResult);
        try {
            repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, RepoAddOptions.createOverwrite(), false, initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }

        PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, RepoAddOptions.createOverwrite(), false, initResult);
        login(userAdministrator);
    }

    void createUsers(int users, OperationResult initResult) throws CommonException {
        for (int i = 0; i < users; i++) {
            UserType user = new UserType(prismContext)
                    .name(String.format("u%06d", i))
                    .givenName(String.format("GivenNameU%06d", i))
                    .familyName(String.format("FamilyNameU%06d", i))
                    .fullName(String.format("FullNameU%06d", i))
                    .emailAddress(String.format("EmailU%06d@test.com", i));
            repositoryService.addObject(user.asPrismObject(), null, initResult);
        }
        System.out.printf("%d users created", users);
    }

    List<String> getLinesOfOutputFile(PrismObject<ReportType> report) throws IOException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        File outputFile = findOutputFile(report);
        displayValue("Found report file", outputFile);
        assertNotNull("No output file for " + report, outputFile);
        List<String> lines = Files.readAllLines(Paths.get(outputFile.getPath()));
        displayValue("Report content (" + lines.size() + " lines)", String.join("\n", lines));
        outputFile.renameTo(new File(outputFile.getParentFile(), "processed-" + outputFile.getName()));
        return lines;
    }

    File findOutputFile(PrismObject<ReportType> report) {
        String filePrefix = report.getName().getOrig();
        File[] matchingFiles = EXPORT_DIR.listFiles((dir, name) -> name.startsWith(filePrefix));
        if (matchingFiles.length == 0) {
            return null;
        }
        if (matchingFiles.length > 1) {
            throw new IllegalStateException("Found more than one output files for " + report + ": " + Arrays.toString(matchingFiles));
        }
        return matchingFiles[0];
    }

    void changeTaskReport(TestResource<ReportType> reportResource, ItemPath reportRefPath, TestResource<TaskType> taskResource) throws CommonException{
        Task task = getTestTask();
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(reportResource.oid);
        modifyObjectReplaceReference(TaskType.class,
                taskResource.oid,
                reportRefPath,
                task,
                task.getResult(),
                ref
                );
    }

    void runExportTask(TestResource<ReportType> reportResource, OperationResult result) throws CommonException {
        changeTaskReport(reportResource,
                ItemPath.create(TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        WorkDefinitionsType.F_REPORT_EXPORT,
                        ClassicReportImportWorkDefinitionType.F_REPORT_REF
                        ),
                TASK_EXPORT_CLASSIC);
        rerunTask(TASK_EXPORT_CLASSIC.oid, result);
    }
}
