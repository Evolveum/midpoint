/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import static com.evolveum.midpoint.schema.util.ReportParameterTypeUtil.addParameter;
import static com.evolveum.midpoint.schema.util.ReportParameterTypeUtil.addParameters;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.assertj.core.util.Arrays;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A report that is to be used in tests.
 */
@Experimental
public class TestReport extends TestObject<ReportType> {

    private static final long DEFAULT_TIMEOUT = 250_000;

    /** Default timeout when waiting for the report task completion. */
    private final long defaultTimeout;

    private final List<String> defaultParameterNames;

    /** Temporary: this is how we access the necessary functionality. */
    private AbstractIntegrationTest test;

    private TestReport(
            @NotNull TestObjectSource source,
            @NotNull String oid,
            long defaultTimeout,
            @NotNull List<String> defaultParameterNames) {
        super(source, oid);
        this.defaultTimeout = defaultTimeout;
        this.defaultParameterNames = defaultParameterNames;
    }

    public static TestReport classPath(@NotNull String dir, @NotNull String name, String oid) {
        return classPath(dir, name, oid, List.of());
    }

    public static TestReport classPath(@NotNull String dir, @NotNull String name, String oid, List<String> defaultParameterNames) {
        return new TestReport(
                new ClassPathBasedTestObjectSource(dir, name), oid, DEFAULT_TIMEOUT, defaultParameterNames);
    }

    public static TestReport file(@NotNull File dir, @NotNull String name, String oid) {
        return file(dir, name, oid, List.of());
    }

    public static TestReport file(@NotNull File dir, @NotNull String name, String oid, List<String> defaultParameterNames) {
        return new TestReport(
                new FileBasedTestObjectSource(dir, name), oid, DEFAULT_TIMEOUT, defaultParameterNames);
    }

    /**
     * Initializes the report - i.e. imports it into repository (via model).
     *
     * @param test To provide access to necessary functionality. Temporary!
     */
    public void init(AbstractIntegrationTest test, Task task, OperationResult result) throws CommonException {
        commonInit(test, task, result);
        this.test = test;
    }

    /**
     * Starts the task and waits for the completion.
     */
    public void rerun(OperationResult result) throws CommonException {
        long startTime = System.currentTimeMillis();
        test.restartTask(oid, result);
        test.waitForTaskFinish(oid, true, startTime, defaultTimeout, false);
    }

    public Export export() {
        return new Export();
    }

    /** Configures and executes the export. */
    public class Export {

        @NotNull private final ReportParameterType parameters = new ReportParameterType();

        private ObjectCustomizer<TaskType> taskCustomizer;

        /** Set up during execution. */
        private TestTask exportTask;

        /** Assumes default parameter names are set up in the main object. */
        public Export withDefaultParametersValues(Object... parameterValues) throws SchemaException {
            addParameters(parameters, defaultParameterNames, Arrays.asList(parameterValues));
            return this;
        }

        public Export withParameter(String parameterName, Object... parameterValues) throws SchemaException {
            addParameter(parameters, parameterName, parameterValues);
            return this;
        }

        // TODO support for the distributed export

        public Export withTaskCustomizer(ObjectCustomizer<TaskType> customizer) {
            this.taskCustomizer = customizer;
            return this;
        }

        public @NotNull List<String> execute(OperationResult result) throws CommonException, IOException {
            stateCheck(test != null, "The test object %s is not initialized", this);

            TaskType newTask = new TaskType()
                    .name("report export task for " + getObjectable().getName())
                    .executionState(TaskExecutionStateType.CLOSED)
                    .activity(new ActivityDefinitionType()
                            .work(new WorkDefinitionsType()
                                    .reportExport(new ClassicReportExportWorkDefinitionType()
                                            .reportRef(ref())
                                            .reportParam(parameters))));

            if (taskCustomizer != null) {
                taskCustomizer.customize(newTask);
            }

            exportTask = TestTask.of(newTask, DEFAULT_TIMEOUT);
            exportTask.init(test, test.getTestTask(), result);
            exportTask.rerun(result);
            exportTask.reload(result);

            return ReportTestUtil.getLinesOfOutputFile(exportTask.get(), RepoSimpleObjectResolver.get(), result);
        }

        public TestTask getExportTask() {
            return exportTask;
        }
    }
}
