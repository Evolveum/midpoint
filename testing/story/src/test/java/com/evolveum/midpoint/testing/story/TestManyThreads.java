/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.internals.CachingStatistics;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.ThreadTestExecutor;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests behavior of selected components when executing in large number of threads.
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestManyThreads extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "threads");

    private static final DummyTestResource RESOURCE_DUMMY =
            new DummyTestResource(TEST_DIR, "resource-dummy.xml", "be4d88ff-bbb7-45f2-91dc-4b0fc9a00ced", null);

    @Autowired RepositoryCache repositoryCache;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_DUMMY.initAndTest(this, initTask, initResult);
    }

    @Test
    public void test100SearchResourceObjects() throws Exception {
//        Task globalTask = getTestTask();
//        OperationResult globalResult = getTestOperationResult();

        RESOURCE_DUMMY.controller.addAccount("jack");

        // trigger version increment (will invalidate cached resource object)
//        repositoryService.modifyObject(ResourceType.class, RESOURCE_DUMMY.oid,
//                deltaFor(ResourceType.class).item(ResourceType.F_DESCRIPTION).replace("aaa").asItemDeltas(),
//                globalResult);

        for (int i = 0; i < 10000; i++) {

            System.out.println("***** STARTING ITERATION #" + i + " *****");

            CachingStatistics statsBefore = InternalMonitor.getResourceCacheStats().clone();

            ThreadTestExecutor executor = new ThreadTestExecutor(20, 60000L);
            executor.execute(() -> {
                login(userAdministrator.clone());
                Task localTask = getTestTask();
                OperationResult localResult = localTask.getResult();

                ObjectQuery query = prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY.oid)
                        .and().item(ShadowType.F_OBJECT_CLASS).eq(RI_ACCOUNT_OBJECT_CLASS)
                        .build();
                SearchResultList<PrismObject<ShadowType>> accounts = modelService
                        .searchObjects(ShadowType.class, query, null, localTask, localResult);
                System.out.println(Thread.currentThread().getName() + ": " + accounts);
                assertEquals("Wrong # of accounts found", 1, accounts.size());

                provisioningService.getObject(ShadowType.class, accounts.get(0).getOid(), null, localTask, localResult);

                assertResourceSanity(RESOURCE_DUMMY.oid, localTask, localResult);
            });

            assertEquals("Wrong # of failed threads", 0, executor.getFailedThreads().size());

            CachingStatistics statsAfter = InternalMonitor.getResourceCacheStats();

            System.out.println("Statistics before: " + statsBefore);
            System.out.println("Statistics after:  " + statsAfter);
        }
    }

    private void assertResourceSanity(String oid, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        ResourceType resource = provisioningService.getObject(ResourceType.class, oid, null, task, result).asObjectable();
        assertNotNull("No schemaHandling", resource.getSchemaHandling());
        assertEquals("Wrong # of object type defs", 1, resource.getSchemaHandling().getObjectType().size());
        ResourceObjectTypeDefinitionType typeDef = resource.getSchemaHandling().getObjectType().get(0);
        assertEquals("Wrong # of attribute defs", 1, typeDef.getAttribute().size());
        ResourceAttributeDefinitionType nameDef = typeDef.getAttribute().get(0);
        assertNotNull("No outbound", nameDef.getOutbound());
        ExpressionType expression = nameDef.getOutbound().getExpression();
        assertNotNull("No outbound expression", expression);
        String code = ((ScriptExpressionEvaluatorType) expression.getExpressionEvaluator().get(0).getValue()).getCode();
        assertNotNull("No <code>", code);
        assertTrue("Wrong <code>", code.contains("name"));

//        LensUtil.refineProjectionIntent(ShadowKindType.ACCOUNT, "default", resource);

//        System.out.println(Thread.currentThread().getName() + ": resource from provisioning is OK");

//        ResourceType resource2 = repositoryCache.getObject(ResourceType.class, oid, null, result).asObjectable();
//        LensUtil.refineProjectionIntent(ShadowKindType.ACCOUNT, "default", resource2, prismContext);
//        System.out.println(Thread.currentThread().getName() + ": resource from repository is OK");
    }
}
