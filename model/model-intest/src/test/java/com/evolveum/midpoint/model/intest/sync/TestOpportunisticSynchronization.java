/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.sync;

import java.io.File;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.OperationResultAssert;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestOpportunisticSynchronization extends AbstractEmptyModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestOpportunisticSynchronization.class);

    private static final File TEST_DIR = new File("./src/test/resources/synchronization");

    private static final DummyTestResource DUMMY_SOURCE = new DummyTestResource(
            TEST_DIR,
            "dummy-source.xml",
            "d3c8f0b2-4f1e-4c5a-9b6d-7f8e9c1a2b3c",
            "dummy-source",
            TestOpportunisticSynchronization::populateWithSchema);

    private static final DummyTestResource DUMMY_TARGET = new DummyTestResource(
            TEST_DIR,
            "dummy-target.xml",
            "00eb54b9-9436-4e62-a598-248668b7c36f",
            "dummy-target",
            TestOpportunisticSynchronization::populateWithSchema);

    private static final TestObject<ArchetypeType> ARCHETYPE =
            TestObject.file(TEST_DIR, "archetype.xml", "0e36e6a3-9b73-488d-80aa-923f86f6210c");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(DUMMY_SOURCE, initTask, initResult);
        initDummyResource(DUMMY_TARGET, initTask, initResult);

        DUMMY_SOURCE.controller.addAccount("jdoe", "John Doe");

        addObject(ARCHETYPE, initTask, initResult);
    }

    private static void populateWithSchema(DummyResourceContoller controller) throws Exception {
        controller.populateWithDefaultSchema();
    }

    /**
     * Test to cover MID-10729
     */
    @Test
    public void testOpportunisticSynchronization() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery sourceShadowsQuery = PrismTestUtil.getPrismContext()
                .queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(DUMMY_SOURCE.oid)
                .and()
                .item(ShadowType.F_OBJECT_CLASS).eq(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .build();

        LOGGER.info("Discovering shadows");

        SearchResultList<PrismObject<ShadowType>> sourceShadows =
                modelService.searchObjects(ShadowType.class, sourceShadowsQuery, null, task, result);
        Assertions.assertThat(sourceShadows).hasSize(1);

        PrismObject<ShadowType> sourceShadow = sourceShadows.get(0);

        LOGGER.info("Importing shadow {}", sourceShadow);

        modelService.importFromResource(sourceShadow.getOid(), task, result);

        result.computeStatusIfUnknown();
        new OperationResultAssert(result).isSuccess();

        PrismObject<UserType> user = searchObjectByName(UserType.class, "jdoe", task, result);
        Assertions.assertThat(user)
                .isNotNull();

        UserType userType = user.asObjectable();
        Assertions.assertThat(userType.getLinkRef()).hasSize(2);

        ObjectQuery targetShadowsQuery = PrismTestUtil.getPrismContext()
                .queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(DUMMY_TARGET.oid)
                .and()
                .item(ShadowType.F_OBJECT_CLASS).eq(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .build();

        SearchResultList<PrismObject<ShadowType>> targetShadows =
                modelService.searchObjects(ShadowType.class, targetShadowsQuery, null, task, result);
        Assertions.assertThat(targetShadows).hasSize(1);

        LOGGER.info("Deleting user {}", user);

        repositoryService.deleteObject(UserType.class, user.getOid(), result);

        user = searchObjectByName(UserType.class, "jdoe", task, result);
        Assertions.assertThat(user)
                .isNull();

        LOGGER.info("Importing shadow {} again", sourceShadow);

        modelService.importFromResource(sourceShadow.getOid(), task, result);

        user = searchObjectByName(UserType.class, "jdoe", task, result);
        Assertions.assertThat(user)
                .withFailMessage("User jdoe should be re-created after re-import, but it was not found.")
                .isNotNull();

        userType = user.asObjectable();
        Assertions.assertThat(userType.getLinkRef())
                .withFailMessage("User jdoe should have two linkRefs")
                .hasSize(2);

        result.recomputeStatus();
        new OperationResultAssert(result).isSuccess();
    }
}
