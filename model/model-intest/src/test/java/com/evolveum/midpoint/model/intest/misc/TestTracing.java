/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.misc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.traces.TraceParser;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestTracing extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/tracing");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<UserType> USER_JOE = new TestResource<>(TEST_DIR, "user-joe.xml", "c279c1c8-a160-4442-88b2-72b358e0c745");
    private static final TestResource<RoleType> ROLE_ILLEGAL = new TestResource<>(TEST_DIR, "role-illegal.xml", "13ca97ae-5919-42fb-91fb-cbc88704fd91");
    private static final DummyTestResource RESOURCE_ILLEGAL = new DummyTestResource(TEST_DIR, "resource-illegal.xml", "793bb9f5-edae-4251-bce7-4e99a72ac23f", "illegal");

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_JOE, initResult);
        repoAdd(ROLE_ILLEGAL, initResult);

        initDummyResource(RESOURCE_ILLEGAL, initTask, initResult);
        assertSuccess(modelService.testResource(RESOURCE_ILLEGAL.oid, initTask));
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test100IllegalChars() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        ModelExecuteOptions options = executeOptions();
        options.tracingProfile(createModelAndProvisioningLoggingTracingProfile());

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(
                        new AssignmentType(prismContext)
                                .targetRef(ROLE_ILLEGAL.oid, RoleType.COMPLEX_TYPE),
                        new AssignmentType(prismContext)
                                .beginConstruction()
                                    .resourceRef(RESOURCE_ILLEGAL.oid, ResourceType.COMPLEX_TYPE)
                                .<AssignmentType>end()
                )
                .asObjectDelta(USER_JOE.oid);

        executeChanges(delta, options, task, result);

        then();
        assertSuccess(result);
        assertUserAfter(USER_JOE.oid)
                .assignments()
                    .assertAssignments(2)
                    .end()
                .links()
                    .assertLinks(1);

        RESOURCE_ILLEGAL.controller.assertAccountByUsername("joe")
                .assertFullName("A\u0007B");

        SearchResultList<PrismObject<ReportDataType>> reportDatas =
                repositoryService.searchObjects(ReportDataType.class, null, null, result);
        assertThat(reportDatas.size()).as("# of report outputs").isEqualTo(1);
        String file = reportDatas.get(0).asObjectable().getFilePath();

        TraceParser parser = new TraceParser(prismContext);
        TracingOutputType parsed = parser.parse(new File(file));
        System.out.println("Tracing output parsed OK: " + parsed);
    }
}
