/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * Tests selected methods in MidpointFunctions.
 * In the future we can test custom functions mechanism here.
 */
@SuppressWarnings({ "FieldCanBeLocal", "unused", "SameParameterValue" })
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestFunctions extends AbstractInitializedModelIntegrationTest {

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws CommonException;
    }

    /**
     * MID-6133
     */
    @Test
    public void test100ResolveReferenceIfExists() throws Exception {
        // GIVEN
        Task task = createTask("test100ResolveReferenceIfExists");
        OperationResult result = task.getResult();

        ObjectReferenceType broken = ObjectTypeUtil.createObjectRef("non-existing-oid", ObjectTypes.USER);

        // WHEN
        execute(task, result, () -> libraryMidpointFunctions.resolveReferenceIfExists(broken));

        // THEN
        assertSuccess(result);
    }

    /**
     * MID-6133
     */
    @Test
    public void test110ResolveReference() throws Exception {
        // GIVEN
        Task task = createTask("test110ResolveReference");
        OperationResult result = task.getResult();

        ObjectReferenceType broken = ObjectTypeUtil.createObjectRef("non-existing-oid", ObjectTypes.USER);

        // WHEN
        try {
            execute(task, result, () -> libraryMidpointFunctions.resolveReference(broken));
            fail("unexpected success");
        } catch (ObjectNotFoundException e) {
            System.out.println("expected failure: " + e.getMessage());
        }

        // THEN
        assertFailure(result);
    }

    private void execute(Task task, OperationResult result, CheckedRunnable runnable) throws CommonException {
        ExpressionEnvironment<?,?,?> environment = new ExpressionEnvironment<>(task, result);
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(environment);
        try {
            runnable.run();
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }
}
