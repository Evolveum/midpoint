/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.activity.TaskActivityManager;
import com.evolveum.midpoint.repo.common.task.work.BucketingManager;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.asserter.ActivityPerformanceInformationAsserter;
import com.evolveum.midpoint.test.asserter.ActivityProgressInformationAsserter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionLimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(locations = { "classpath:ctx-repo-common-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AbstractRepoCommonTest extends AbstractIntegrationTest {

    private static final TestResource<UserType> USER_ADMINISTRATOR = new TestResource<>(COMMON_DIR, "user-administrator.xml", "00000000-0000-0000-0000-000000000002");

    private static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/repo-common-test/extension";
    private static final ItemName EXT_DISCRIMINATOR = new ItemName(NS_EXT, "discriminator");

    @Autowired protected TaskActivityManager activityManager;
    @Autowired protected BucketingManager bucketingManager;
    @Autowired protected MockRecorder mockRecorder;
    @Autowired protected LocalScheduler localScheduler;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Currently just for sure. We do not start cluster manager thread in these tests.
        ClusterManager.setUpdateNodeExecutionLimitations(false);
        setPermissiveExecutionLimitations();

        repoAdd(USER_ADMINISTRATOR, initResult);
    }

    // TODO deduplicate with model integration test
    protected ActivityProgressInformationAsserter<Void> assertProgress(String rootOid, String message)
            throws SchemaException, ObjectNotFoundException {
        return assertProgress(
                activityManager.getProgressInformation(rootOid, getTestOperationResult()),
                message);
    }

    // TODO deduplicate with model integration test
    ActivityPerformanceInformationAsserter<Void> assertPerformance(String rootOid, String message)
            throws SchemaException, ObjectNotFoundException {
        return assertPerformance(
                activityManager.getPerformanceInformation(rootOid, getTestOperationResult()),
                message);
    }

    void setDiscriminator(ObjectType object, int value) throws SchemaException {
        ObjectTypeUtil.setExtensionPropertyRealValues(
                prismContext, object.asPrismContainerValue(), EXT_DISCRIMINATOR, value);
    }

    void assertExecutions(List<? extends ObjectType> objects, int taskRuns) {
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
