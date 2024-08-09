/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.expr.OptimizingTriggerCreator;
import com.evolveum.midpoint.model.impl.expr.triggerSetter.OptimizingTriggerCreatorImpl;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestOptimizingTriggerCreator extends AbstractInitializedModelIntegrationTest {

    @Test
    public void test100CreateThreeTriggers() throws Exception {
        doTest100CreateThreeTriggers(true);
        doTest100CreateThreeTriggers(false);
    }

    private void doTest100CreateThreeTriggers(boolean useGlobalState) throws Exception {
        UserType user = createTestUser(useGlobalState, null);
        String userName = user.getName().getOrig();
        String userOid = user.getOid();

        OptimizingTriggerCreatorImpl.useGlobalState = useGlobalState;

        OptimizingTriggerCreator creator = libraryMidpointFunctions.getOptimizingTriggerCreator(60000, 2000);
        boolean first = creator.createForNamedUser(userName);
        boolean second = creator.createForNamedUser(userName);
        boolean third = creator.createForNamedUser(userName);
        assertTrue("first trigger should be created", first);
        assertFalse("second trigger should NOT be created", second);
        assertFalse("third trigger should NOT be created", third);
        assertUser(userOid, "")
                .triggers()
                .assertTriggers(1);
    }

    @Test
    public void test110CreateTriggersWithUserDeletion() throws Exception {
        doTest110CreateTriggersWithUserDeletion(true);
        doTest110CreateTriggersWithUserDeletion(false);
    }

    private void doTest110CreateTriggersWithUserDeletion(boolean useGlobalState) throws Exception {
        UserType user = createTestUser(useGlobalState, null);
        String userName = user.getName().getOrig();
        String userOid = user.getOid();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        OptimizingTriggerCreator creator = libraryMidpointFunctions.getOptimizingTriggerCreator(60000, 2000);
        boolean first = creator.createForNamedUser(userName);
        boolean second = creator.createForNamedUser(userName);
        assertTrue("first trigger should be created", first);
        assertFalse("second trigger should NOT be created", second);
        assertUser(userOid, "user old")
                .triggers()
                .assertTriggers(1);

        repositoryService.deleteObject(UserType.class, userOid, result);

        UserType userNew = new UserType().name(userName);
        String userOidNew = repositoryService.addObject(userNew.asPrismObject(), null, result);

        boolean third = creator.createForNamedObject(UserType.class, userName);
        boolean fourth = creator.createForNamedObject(UserType.class, userName);
        assertTrue("third trigger should be created", third);
        assertFalse("fourth trigger should NOT be created", fourth);
        assertUser(userOidNew, "user new")
                .triggers()
                .assertTriggers(1);
    }

    @Test
    public void test200CreateThreeTriggersByOid() throws Exception {
        doTest200CreateThreeTriggersByOid(true);
        doTest200CreateThreeTriggersByOid(false);
    }

    private void doTest200CreateThreeTriggersByOid(boolean useGlobalState) throws Exception {
        UserType user = createTestUser(useGlobalState, null);
        String userOid = user.getOid();

        OptimizingTriggerCreator creator = libraryMidpointFunctions.getOptimizingTriggerCreator(60000, 2000);
        boolean first = creator.createForObject(UserType.class, userOid);
        boolean second = creator.createForObject(UserType.class, userOid);
        boolean third = creator.createForObject(UserType.class, userOid);
        assertTrue("first trigger should be created", first);
        assertFalse("second trigger should NOT be created", second);
        assertFalse("third trigger should NOT be created", third);
        assertUser(userOid, "")
                .triggers()
                .assertTriggers(1);
    }

    @Test
    public void test210CreateTriggersWithExternallyCreatedOnes() throws Exception {
        doTest210CreateTriggersWithExternallyCreatedOnes(true);
        doTest210CreateTriggersWithExternallyCreatedOnes(false);
    }

    private void doTest210CreateTriggersWithExternallyCreatedOnes(boolean useGlobalState) throws Exception {
        UserType user = createTestUser(useGlobalState, System.currentTimeMillis() + 30000);
        String userOid = user.getOid();

        OptimizingTriggerCreator creator = libraryMidpointFunctions.getOptimizingTriggerCreator(60000, 2000);
        boolean triggerCreated = creator.createForObject(UserType.class, userOid);
        assertFalse("a trigger should be created", triggerCreated);
        assertUser(userOid, "")
                .triggers()
                .assertTriggers(1); // the existing one
    }

    private UserType createTestUser(boolean useGlobalState, Long triggerTimestamp) throws CommonException {
        UserType user = new UserType()
                .name(getTestNameShort() + "_" + useGlobalState);
        if (triggerTimestamp != null) {
            user.getTrigger().add(
                    new TriggerType()
                            .handlerUri(RecomputeTriggerHandler.HANDLER_URI)
                            .timestamp(XmlTypeConverter.createXMLGregorianCalendar(triggerTimestamp)));
        }
        repositoryService.addObject(user.asPrismObject(), null, getTestOperationResult());
        return user;
    }
}
