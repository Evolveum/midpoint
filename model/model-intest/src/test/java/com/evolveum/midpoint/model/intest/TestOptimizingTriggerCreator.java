/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.model.api.expr.OptimizingTriggerCreator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

/**
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestOptimizingTriggerCreator extends AbstractInitializedModelIntegrationTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test100CreateThreeTriggers() throws Exception {
        OptimizingTriggerCreator creator = libraryMidpointFunctions.getOptimizingTriggerCreator(60000, 2000);
        boolean first = creator.createForNamedUser(USER_JACK_USERNAME);
        boolean second = creator.createForNamedUser(USER_JACK_USERNAME);
        boolean third = creator.createForNamedUser(USER_JACK_USERNAME);
        assertTrue("first trigger should be created", first);
        assertFalse("second trigger should NOT be created", second);
        assertFalse("third trigger should NOT be created", third);
        assertUser(USER_JACK_OID, "")
                .triggers()
                .assertTriggers(1);
    }

    @Test
    public void test110CreateTriggersWithUserDeletion() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        OptimizingTriggerCreator creator = libraryMidpointFunctions.getOptimizingTriggerCreator(60000, 2000);
        boolean first = creator.createForNamedUser(USER_GUYBRUSH_USERNAME);
        boolean second = creator.createForNamedUser(USER_GUYBRUSH_USERNAME);
        assertTrue("first trigger should be created", first);
        assertFalse("second trigger should NOT be created", second);
        assertUser(USER_GUYBRUSH_OID, "guybrush old")
                .triggers()
                .assertTriggers(1);

        repositoryService.deleteObject(UserType.class, USER_GUYBRUSH_OID, result);

        UserType guybrushNew = new UserType(prismContext)
                .name(USER_GUYBRUSH_USERNAME);
        repositoryService.addObject(guybrushNew.asPrismObject(), null, result);

        boolean third = creator.createForNamedObject(UserType.class, USER_GUYBRUSH_USERNAME);
        boolean fourth = creator.createForNamedObject(UserType.class, USER_GUYBRUSH_USERNAME);
        assertTrue("third trigger should be created", third);
        assertFalse("fourth trigger should NOT be created", fourth);
        assertUser(guybrushNew.getOid(), "guybrush new")
                .triggers()
                .assertTriggers(1);
    }

    @Test
    public void test200CreateThreeTriggersByOid() throws Exception {
        OptimizingTriggerCreator creator = libraryMidpointFunctions.getOptimizingTriggerCreator(60000, 2000);
        boolean first = creator.createForObject(UserType.class, USER_BARBOSSA_OID);
        boolean second = creator.createForObject(UserType.class, USER_BARBOSSA_OID);
        boolean third = creator.createForObject(UserType.class, USER_BARBOSSA_OID);
        assertTrue("first trigger should be created", first);
        assertFalse("second trigger should NOT be created", second);
        assertFalse("third trigger should NOT be created", third);
        assertUser(USER_BARBOSSA_OID, "")
                .triggers()
                .assertTriggers(1);
    }
}
