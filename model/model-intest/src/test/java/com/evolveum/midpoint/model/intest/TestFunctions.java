/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.util.MiscSchemaUtil.getExpressionProfile;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.SystemException;

import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.CheckedRunnable;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests selected methods in MidpointFunctions + parts of "function libraries" feature.
 */
@SuppressWarnings({ "FieldCanBeLocal", "unused", "SameParameterValue", "SimplifiedTestNGAssertion" })
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestFunctions extends AbstractInitializedModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestFunctions.class);

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "functions");

    private static final TestResource<FunctionLibraryType> FUNCTION_LIBRARY_TESTLIB =
            new TestResource<>(TEST_DIR, "function-library-testlib.xml", "19a38b96-8357-473c-b0a2-87e2885503bb");

    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private CacheDispatcher cacheDispatcher;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        repoAdd(FUNCTION_LIBRARY_TESTLIB, initResult);
    }

    /**
     * MID-6133
     */
    @Test
    public void test100ResolveReferenceIfExists() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectReferenceType broken = ObjectTypeUtil.createObjectRef(TestUtil.NON_EXISTENT_OID, ObjectTypes.USER);

        when();
        execute(task, result, () -> libraryMidpointFunctions.resolveReferenceIfExists(broken));

        then();
        assertSuccess(result);
    }

    private void execute(Task task, OperationResult result, CheckedRunnable runnable) throws Exception {
        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                new ExpressionEnvironment(task, result));
        try {
            runnable.run();
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    /**
     * MID-6133
     */
    @Test
    public void test110ResolveReference() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectReferenceType broken = ObjectTypeUtil.createObjectRef(TestUtil.NON_EXISTENT_OID, ObjectTypes.USER);

        when();
        try {
            execute(task, result, () -> libraryMidpointFunctions.resolveReference(broken));
            fail("unexpected success");
        } catch (ObjectNotFoundException e) {
            System.out.println("expected failure: " + e.getMessage());
        }

        then();
        assertFailure(result);
    }

    /**
     * MID-6076
     */
    @Test
    public void test120AddRecomputeTrigger() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> administrator = getUser(USER_ADMINISTRATOR_OID);

        when();
        execute(task, result, () -> libraryMidpointFunctions.addRecomputeTrigger(administrator, null,
                trigger -> trigger.setOriginDescription("test120")));

        then();
        assertSuccess(result);
        // @formatter:off
        assertUserAfter(USER_ADMINISTRATOR_OID)
                .triggers()
                    .single()
                        .assertOriginDescription("test120");
        // @formatter:on
    }

    /**
     * Importing a function library while it's in a heavy use. Short version.
     * (Import is with OID, i.e. *no* deletion and re-creation of the object.)
     *
     * MID-8137
     */
    @Test
    public void test200FunctionLibraryReImportShort() throws CommonException, InterruptedException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("preparation call");
        callCustomLibrary(task, result);

        when("function library is re-imported while it's in use");
        executeWithReImportInBetween(
                FUNCTION_LIBRARY_TESTLIB.getObjectable(), 10_000, 200, 10, task, result);
    }

    /**
     * Importing a function library while it's in a heavy use. Long version, disabled by default.
     *
     * MID-8137
     */
    @Test(enabled = false)
    public void test210FunctionLibraryReImportLong() throws CommonException, InterruptedException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("preparation call");
        callCustomLibrary(task, result);

        when("function library is re-imported while it's in use");
        executeWithReImportInBetween(
                FUNCTION_LIBRARY_TESTLIB.getObjectable(), 600_000, 200, 500, task, result);
    }

    /**
     * Importing a function library "without OID, only by name" (resulting in deletion and re-creation) while it's in a heavy use.
     *
     * This is known to fail occasionally, so disabled for now.
     *
     * MID-8137
     */
    @Test(enabled = false)
    public void test220FunctionLibraryReImportNoOidLong() throws CommonException, InterruptedException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("preparation call");
        callCustomLibrary(task, result);

        when("function library is re-imported (without OID) while it's in use");
        FunctionLibraryType objectWithoutOid =
                FUNCTION_LIBRARY_TESTLIB.getObjectable()
                        .clone()
                        .oid(null);
        executeWithReImportInBetween(objectWithoutOid, 300_000, 200, 100, task, result);
    }

    private void executeWithReImportInBetween(
            FunctionLibraryType objectToImport,
            long duration,
            long importInterval,
            int minImportCount,
            Task task,
            OperationResult result)
            throws CommonException, InterruptedException {
        CheckedRunnable action = () -> {
            login("administrator");
            ImportOptionsType options = new ImportOptionsType();
            options.setOverwrite(true);
            options.setModelExecutionOptions(
                    new ModelExecuteOptionsType()
                            .raw(false));
            modelService.importObject(
                    objectToImport.asPrismObject(), options, task, new OperationResult("dummy"));
        };
        executeWithActionInBetween(duration, importInterval, minImportCount, action, task, result);
    }

    private void executeWithActionInBetween(
            long duration,
            long actionExecutionInterval,
            int minActionExecutionCount,
            CheckedRunnable action,
            Task task,
            OperationResult result) throws CommonException, InterruptedException {
        AtomicInteger actionExecutionCount = new AtomicInteger();
        Holder<Exception> exceptionHolder = new Holder<>();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable runnableAction = () -> {
            try {
                action.run();
                actionExecutionCount.incrementAndGet();
            } catch (Exception e) {
                LOGGER.error("Got unexpected exception while executing 'in-between' action: {}", e.getMessage(), e);
                exceptionHolder.setValue(e);
                throw new SystemException(e);
            }
        };
        ScheduledFuture<?> future =
                scheduler.scheduleAtFixedRate(
                        runnableAction, actionExecutionInterval, actionExecutionInterval, TimeUnit.MILLISECONDS);
        long start = System.currentTimeMillis();
        int iteration = 0;
        while (System.currentTimeMillis() - start < duration) {
            if (++iteration % 10_000 == 0) {
                LOGGER.info("Iteration: {}", iteration);
            }
            callCustomLibrary(task, result);
            result.computeStatus();
            result.cleanupResult();

            Exception exception = exceptionHolder.getValue();
            if (exception != null) {
                throw new AssertionError("Unexpected 'in-between' action exception", exception);
            }
        }

        then("everything is OK");
        System.out.printf("Duration: %d, iterations: %d, 'in-between' actions executed: %d (minimum: %d, interval was %d)",
                duration, iteration, actionExecutionCount.get(), minActionExecutionCount, actionExecutionInterval);
        assertThat(exceptionHolder.getValue()).as("exception").isNull();
        assertThat(actionExecutionCount.get())
                .as("action execution count")
                .isGreaterThanOrEqualTo(minActionExecutionCount);
        future.cancel(false);
        scheduler.shutdown();
        boolean terminated = scheduler.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(terminated).as("action scheduler terminated").isTrue();
    }

    private void callCustomLibrary(Task task, OperationResult result) throws CommonException {
        String libraryMethodExecutionCode = "testlib.execute('test', [:])";
        ExpressionType expressionBean = new ExpressionType();
        expressionBean.getExpressionEvaluator().add(
                new ObjectFactory().createScript(
                        new ScriptExpressionEvaluatorType()
                                .code(libraryMethodExecutionCode)));
        PrismPropertyDefinition<String> outputDefinition =
                PrismContext.get().definitionFactory()
                        .createPropertyDefinition(ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_STRING);
        Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression =
                expressionFactory.makeExpression(
                        expressionBean, outputDefinition, getExpressionProfile(), "", task, result);
        ExpressionEvaluationContext ctx = new ExpressionEvaluationContext(List.of(), new VariablesMap(), "", task);
        Collection<PrismPropertyValue<String>> nonNegativeValues = expression.evaluate(ctx, result).getNonNegativeValues();
        PrismPropertyValue<String> value = MiscUtil.extractSingletonRequired(
                nonNegativeValues,
                () -> new AssertionError("Unexpected multiple values returned: " + nonNegativeValues),
                () -> new AssertionError("No values returned"));
        String realValue = value.getRealValue();
        assertThat(realValue).as("expression result").isEqualTo("test-result");
    }

    /**
     * Invalidating a cached function library while it's in a heavy use. This seems to be the root cause of MID-8137.
     */
    @Test
    public void test230FunctionLibraryInvalidate() throws CommonException, InterruptedException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("preparation call");
        callCustomLibrary(task, result);

        when("cached function library is invalidated while it's in use");
        executeWithInvalidationInBetween(10_000, 50, 190, task, result);
    }

    private void executeWithInvalidationInBetween(
            long duration,
            long invalidationInterval,
            int minInvalidationCount,
            Task task,
            OperationResult result)
            throws CommonException, InterruptedException {
        CheckedRunnable action =
                () -> cacheDispatcher.dispatchInvalidation(FunctionLibraryType.class, null, false, null);
        executeWithActionInBetween(duration, invalidationInterval, minInvalidationCount, action, task, result);
    }
}
