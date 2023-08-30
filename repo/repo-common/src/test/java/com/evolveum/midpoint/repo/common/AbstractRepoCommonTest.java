/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.TaskActivityManager;
import com.evolveum.midpoint.repo.common.activity.run.buckets.BucketingManager;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.ActivityPerformanceInformationAsserter;
import com.evolveum.midpoint.test.asserter.ActivityProgressInformationAsserter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionLimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Super class for common repository tests.
 * Test can decide whether to use {@link #repositoryService} with the cache or the pure
 * implementation using {@link #plainRepositoryService}.
 */
@ContextConfiguration(locations = { "classpath:ctx-repo-common-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AbstractRepoCommonTest extends AbstractIntegrationTest {

    private static final TestObject<UserType> USER_ADMINISTRATOR = TestObject.file(
            COMMON_DIR, "user-administrator.xml", "00000000-0000-0000-0000-000000000002");

    private static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/repo-common-test/extension";
    private static final ItemName EXT_DISCRIMINATOR = new ItemName(NS_EXT, "discriminator");

    @Autowired protected TaskActivityManager activityManager;
    @Autowired protected BucketingManager bucketingManager;
    @Autowired protected MockRecorder mockRecorder;
    @Autowired protected LocalScheduler localScheduler;

    @Autowired @Qualifier("repositoryService") // implementation, no cache
    protected RepositoryService plainRepositoryService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Currently just for sure. (It shouldn't be needed as we do not start cluster manager thread in these tests.)
        ClusterManager.setUpdateNodeExecutionLimitations(false);
        setPermissiveExecutionLimitations();

        repoAdd(USER_ADMINISTRATOR, initResult);
    }

    // TODO deduplicate with model integration test
    protected ActivityProgressInformationAsserter<Void> assertProgress(String rootOid, String message)
            throws SchemaException, ObjectNotFoundException {
        return assertProgress(
                activityManager.getProgressInformationFromTaskTree(rootOid, getTestOperationResult()),
                message);
    }

    protected ActivityProgressInformationAsserter<Void> assertProgress(@NotNull String rootOid,
            @NotNull InformationSource source, String message)
            throws SchemaException, ObjectNotFoundException {
        return assertProgress(
                activityManager.getProgressInformation(rootOid, source, getTestOperationResult()),
                message);
    }

    // TODO deduplicate with model integration test
    protected ActivityPerformanceInformationAsserter<Void> assertPerformance(String rootOid, String message)
            throws SchemaException, ObjectNotFoundException {
        return assertPerformance(
                activityManager.getPerformanceInformation(rootOid, getTestOperationResult()),
                message);
    }

    protected void setDiscriminator(ObjectType object, int value) throws SchemaException {
        ObjectTypeUtil.setExtensionPropertyRealValues(
                prismContext, object.asPrismContainerValue(), EXT_DISCRIMINATOR, value);
    }

    protected void assertExecutions(List<? extends ObjectType> objects, int taskRuns) {
        List<String> roleNames = objects.stream()
                .map(r -> r.getName().getOrig())
                .collect(Collectors.toList());

        List<String> expectedExecutions = new ArrayList<>();
        for (int i = 0; i < taskRuns; i++) {
            expectedExecutions.addAll(roleNames);
        }

        assertThat(mockRecorder.getExecutions())
                .as("executions")
                .containsExactlyInAnyOrderElementsOf(expectedExecutions);
    }

    private void setPermissiveExecutionLimitations() {
        localScheduler.setLocalExecutionLimitations(
                new TaskExecutionLimitationsType());
    }
}
