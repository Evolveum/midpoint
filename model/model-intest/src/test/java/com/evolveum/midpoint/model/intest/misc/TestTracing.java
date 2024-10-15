/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.misc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;

import com.evolveum.midpoint.util.exception.SchemaException;

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
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests various bugs related to tracing.
 *
 * E.g. illegal chars (not serializable to XML) in mapping output.
 * Or compile-class-less PCVs in mapping output.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestTracing extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/tracing");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<UserType> USER_JOE = TestObject.file(TEST_DIR, "user-joe.xml", "c279c1c8-a160-4442-88b2-72b358e0c745");
    private static final TestObject<RoleType> ROLE_ILLEGAL = TestObject.file(TEST_DIR, "role-illegal.xml", "13ca97ae-5919-42fb-91fb-cbc88704fd91");
    private static final DummyTestResource RESOURCE_ILLEGAL = new DummyTestResource(TEST_DIR, "resource-illegal.xml", "793bb9f5-edae-4251-bce7-4e99a72ac23f", "illegal");

    private static final TestObject<UserType> USER_JIM = TestObject.file(TEST_DIR, "user-jim.xml", "5a85ea58-ecf7-4e23-ab4f-750f877dc13a");
    private static final TestObject<RoleType> ROLE_CLASS_LESS_VALUES = TestObject.file(TEST_DIR, "role-class-less-values.xml", "c903aee4-8726-47cd-99e9-8aad7a60b12f");
    private static final TestObject<FunctionLibraryType> FUNCTION_LIBRARY_HACKING = TestObject.file(TEST_DIR, "function-library-hacking.xml", "87b91749-5f92-4328-bcc3-6f1b6e6e8364");

    private static final String CONTAINERS_NS = "http://super.org/midpoint";
    private static final ItemName NAME_MY_CONTAINER = ItemName.from(CONTAINERS_NS, "myContainer");
    public static final ItemName NAME_VALUE = ItemName.from(CONTAINERS_NS, "value");
    public static final ItemName NAME_EMBEDDED = ItemName.from(CONTAINERS_NS, "embedded");
    public static final ItemName TYPE_MY_CONTAINER = ItemName.from(CONTAINERS_NS, "MyContainerType");

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_JOE, initResult);
        repoAdd(ROLE_ILLEGAL, initResult);

        initDummyResource(RESOURCE_ILLEGAL, initTask, initResult);
        assertSuccess(modelService.testResource(RESOURCE_ILLEGAL.oid, initTask, initResult));

        repoAdd(USER_JIM, initResult);
        repoAdd(ROLE_CLASS_LESS_VALUES, initResult);
        repoAdd(FUNCTION_LIBRARY_HACKING, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * Tests illegal chars in mapping output.
     */
    @Test
    public void test100IllegalChars() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        deleteReportDataObjects(result);

        when();
        ModelExecuteOptions options = executeOptions();
        options.tracingProfile(createModelAndProvisioningLoggingTracingProfile());

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(
                        new AssignmentType()
                                .targetRef(ROLE_ILLEGAL.oid, RoleType.COMPLEX_TYPE),
                        new AssignmentType()
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
                    .assertLiveLinks(1);

        RESOURCE_ILLEGAL.controller.assertAccountByUsername("joe")
                .assertFullName("A\u0007B");

        assertTraceCanBeParsed(result);
    }

    /**
     * Tests class-less PCVs.
     */
    @Test
    public void test200ClassLessValues() throws Exception {
        skipIfNotNativeRepository();
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        deleteReportDataObjects(result);

        when();
        ModelExecuteOptions options = executeOptions();
        options.tracingProfile(createModelAndProvisioningLoggingTracingProfile());

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType()
                                .targetRef(ROLE_CLASS_LESS_VALUES.oid, RoleType.COMPLEX_TYPE))
                .asObjectDelta(USER_JIM.oid);

        executeChanges(delta, options, task, result);

        then();
        assertSuccess(result);
        assertUserAfter(USER_JIM.oid)
                .assignments()
                    .assertAssignments(1)
                .end()
                .extensionContainer(NAME_MY_CONTAINER)
                    .assertSize(1)
                    .value(0)
                        .assertItemsExactly(NAME_VALUE, NAME_EMBEDDED);

        assertTraceCanBeParsed(result);
    }

    private void deleteReportDataObjects(OperationResult result) throws ObjectNotFoundException, SchemaException {
        SearchResultList<PrismObject<ReportDataType>> objects = repositoryService.searchObjects(
                ReportDataType.class, null, null, result);
        for (PrismObject<ReportDataType> object : objects) {
            repositoryService.deleteObject(ReportDataType.class, object.getOid(), result);
        }
    }

    private void assertTraceCanBeParsed(OperationResult result)
            throws SchemaException, IOException {
        SearchResultList<PrismObject<ReportDataType>> reportDataObjects =
                repositoryService.searchObjects(ReportDataType.class, null, null, result);
        assertThat(reportDataObjects.size()).as("# of report outputs").isEqualTo(1);
        String file = reportDataObjects.get(0).asObjectable().getFilePath();

        TraceParser parser = new TraceParser(prismContext);
        TracingOutputType parsed = parser.parse(new File(file));
        System.out.println("Tracing output parsed OK: " + parsed);
    }
}
