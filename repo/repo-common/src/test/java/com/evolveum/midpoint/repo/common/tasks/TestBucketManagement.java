/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.util.task.BucketingUtil.sortBucketsBySequentialNumber;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;

import com.evolveum.midpoint.schema.util.task.ActivityPath;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;

import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.repo.common.task.work.BucketingManager;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketAllocator;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketContentFactory;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketContentFactoryCreator;
import com.evolveum.midpoint.repo.common.task.work.segmentation.StringBucketContentFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Low level tests of bucket management.
 *
 * No tasks are run here. (After all, bucketed handlers are present only in repo-common module.)
 *
 * Tests 010-099 only check that the bucket allocator provides correct sequence of buckets (plus check query narrowing).
 * Tests 100-199 exercise get bucket / complete bucket cycle within a single (standalone) task.
 * Tests 200-299 check the situation with multiple tasks (coordinators + workers).
 */
@ContextConfiguration(locations = { "classpath:ctx-repo-common-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestBucketManagement extends AbstractRepoCommonTest {

    @Autowired private BucketingManager bucketingManager;
    @Autowired private BucketContentFactoryCreator contentFactoryCreator;

    private static final File TEST_DIR = new File("src/test/resources/tasks/buckets");

    private static final TestResource<TaskType> TASK_010 = new TestResource<>(TEST_DIR, "task-010.xml", "758f79bb-800c-42b2-8d85-44416a29d956");
    private static final TestResource<TaskType> TASK_020 = new TestResource<>(TEST_DIR, "task-020.xml", "62490d1d-244c-4f36-b0d1-cc3a8df26743");
    private static final TestResource<TaskType> TASK_030 = new TestResource<>(TEST_DIR, "task-030.xml", "6fdcfd71-ded8-4ce1-8197-0a785695fe70");
    private static final TestResource<TaskType> TASK_040 = new TestResource<>(TEST_DIR, "task-040.xml", "31d5e04a-871e-42cf-8ec1-35fab4f0d706");
    private static final TestResource<TaskType> TASK_050 = new TestResource<>(TEST_DIR, "task-050.xml", "7c86fb96-b959-4469-9c26-1e7fc5bc79ef");

    private static final TestResource<TaskType> TASK_100 = new TestResource<>(TEST_DIR, "task-100.xml", "b19b16ff-fe18-40ac-bf70-859f546a67ea");
    private static final TestResource<TaskType> TASK_110 = new TestResource<>(TEST_DIR, "task-110.xml", "a4de3ebf-c9bb-4d25-bbaf-e73d89eea873");
    private static final TestResource<TaskType> TASK_120 = new TestResource<>(TEST_DIR, "task-120.xml", "ec87b7cf-b7ee-43ce-92a2-4bc08c137b72");
    private static final TestResource<TaskType> TASK_130 = new TestResource<>(TEST_DIR, "task-130.xml", "1339d0e9-eb92-4e1a-a48e-86406d1f37c1");
    private static final TestResource<TaskType> TASK_140 = new TestResource<>(TEST_DIR, "task-140.xml", "c65a656e-75e9-4d03-a5bc-7e082a19bfb1");
    private static final TestResource<TaskType> TASK_150 = new TestResource<>(TEST_DIR, "task-150.xml", "0a278190-ba8e-4684-b95a-57f5198ac8b3");

    private static final TestResource<TaskType> TASK_200_COORDINATOR = new TestResource<>(TEST_DIR, "task-200-c.xml", "a021d1b0-eea3-4378-87b6-7ab8bf9ab537");
    private static final TestResource<TaskType> TASK_200_WORKER = new TestResource<>(TEST_DIR, "task-200-w.xml", "432c09ba-27d5-4012-a30e-6bd16a3ca07f");

    private static final TestResource<TaskType> TASK_210_COORDINATOR = new TestResource<>(TEST_DIR, "task-210-c.xml", "336e4ded-82a6-432c-ade0-43844e59c3e4");
    private static final TestResource<TaskType> TASK_210_WORKER_1 = new TestResource<>(TEST_DIR, "task-210-1.xml", "9ea3fea0-6028-47d8-81d6-304512dcbac3");
    private static final TestResource<TaskType> TASK_210_WORKER_2 = new TestResource<>(TEST_DIR, "task-210-2.xml", "202ad211-43e9-416e-9e6e-dcb4c2c4d89f");
    private static final TestResource<TaskType> TASK_210_WORKER_3 = new TestResource<>(TEST_DIR, "task-210-3.xml", "f9298cc6-174f-4a20-8703-bacc500fc53e");
    private static final TestResource<TaskType> TASK_210_WORKER_4 = new TestResource<>(TEST_DIR, "task-210-4.xml", "f1bb0e85-abac-4e61-8a3a-f72d40f3e8d6");
    private static final TestResource<TaskType> TASK_210_WORKER_5 = new TestResource<>(TEST_DIR, "task-210-5.xml", "81e31c90-6546-4055-8371-a34ef79f5117");

    @PostConstruct
    public void initialize() throws Exception {
        displayTestTitle("Initializing TEST CLASS: " + getClass().getName());
        bucketingManager.setFreeBucketWaitIntervalOverride(1000L);
        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    /**
     * Tests obtaining buckets with prefix-based string segmentation using legacy specification of boundary characters.
     */
    @Test
    public void test010StringPrefixBucketsLegacy() throws Exception {
        OperationResult result = createOperationResult();
        taskAdd(TASK_010, result);

        Task task = taskManager.getTaskPlain(TASK_010.oid, result);
        ActivityStateType workState = new ActivityStateType(prismContext);

        when();

        BucketAllocator allocator = BucketAllocator.create(getDistributionDefinition(task), contentFactoryCreator);
        BucketContentFactory contentFactory = allocator.getContentFactory();

        then();

        assertBoundariesAndBucketCount(contentFactory, Arrays.asList("a", "01abc", "01abc"), 25);

        WorkBucketType bucket = assumeNextPrefix(allocator, workState, "a00", 1);

        assertNarrowedQuery(task, bucket,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_NAME).startsWith("a00").matchingNorm()
                        .build()
        );

        assumeNextPrefix(allocator, workState, "a01", 2);
        assumeNextPrefix(allocator, workState, "a0a", 3);
        assumeNextPrefix(allocator, workState, "a0b", 4);
        assumeNextPrefix(allocator, workState, "a0c", 5);
        assumeNextPrefix(allocator, workState, "a10", 6);
        assumeNextPrefix(allocator, workState, "a11", 7);
        assumeNextPrefix(allocator, workState, "a1a", 8);
        assumeNextPrefix(allocator, workState, "a1b", 9);
        assumeNextPrefix(allocator, workState, "a1c", 10);
        assumeNextPrefix(allocator, workState, "aa0", 11);
        assumeNextPrefix(allocator, workState, "aa1", 12);
        assumeNextPrefix(allocator, workState, "aaa", 13);
        assumeNextPrefix(allocator, workState, "aab", 14);
        assumeNextPrefix(allocator, workState, "aac", 15);
        assumeNextPrefix(allocator, workState, "ab0", 16);
        assumeNextPrefix(allocator, workState, "ab1", 17);
        assumeNextPrefix(allocator, workState, "aba", 18);
        assumeNextPrefix(allocator, workState, "abb", 19);
        assumeNextPrefix(allocator, workState, "abc", 20);
        assumeNextPrefix(allocator, workState, "ac0", 21);
        assumeNextPrefix(allocator, workState, "ac1", 22);
        assumeNextPrefix(allocator, workState, "aca", 23);
        assumeNextPrefix(allocator, workState, "acb", 24);
        assumeNextPrefix(allocator, workState, "acc", 25);
        assumeNoNextBucket(allocator, workState);
    }

    /**
     * Tests obtaining buckets with exact-match-based string segmentation using new specification of boundary characters.
     */
    @Test
    public void test020StringExactValueBuckets() throws Exception {
        OperationResult result = createOperationResult();
        taskAdd(TASK_020, result);

        Task task = taskManager.getTaskPlain(TASK_020.oid, result);
        ActivityStateType workState = new ActivityStateType(prismContext);

        when();

        BucketAllocator allocator = BucketAllocator.create(getDistributionDefinition(task), contentFactoryCreator);
        BucketContentFactory contentFactory = allocator.getContentFactory();

        then();

        assertBoundariesAndBucketCount(contentFactory, Arrays.asList("a", "01abc", "01abc"), 25);

        WorkBucketType bucket = assumeNextValue(allocator, workState, "a00", 1);

        assertNarrowedQuery(task, bucket,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_NAME).eq("a00").matchingNorm()
                        .build()
        );

        assumeNextValue(allocator, workState, "a01", 2);
        assumeNextValue(allocator, workState, "a0a", 3);
        assumeNextValue(allocator, workState, "a0b", 4);
        assumeNextValue(allocator, workState, "a0c", 5);
        assumeNextValue(allocator, workState, "a10", 6);
        assumeNextValue(allocator, workState, "a11", 7);
        assumeNextValue(allocator, workState, "a1a", 8);
        assumeNextValue(allocator, workState, "a1b", 9);
        assumeNextValue(allocator, workState, "a1c", 10);
        assumeNextValue(allocator, workState, "aa0", 11);
        assumeNextValue(allocator, workState, "aa1", 12);
        assumeNextValue(allocator, workState, "aaa", 13);
        assumeNextValue(allocator, workState, "aab", 14);
        assumeNextValue(allocator, workState, "aac", 15);
        assumeNextValue(allocator, workState, "ab0", 16);
        assumeNextValue(allocator, workState, "ab1", 17);
        assumeNextValue(allocator, workState, "aba", 18);
        assumeNextValue(allocator, workState, "abb", 19);
        assumeNextValue(allocator, workState, "abc", 20);
        assumeNextValue(allocator, workState, "ac0", 21);
        assumeNextValue(allocator, workState, "ac1", 22);
        assumeNextValue(allocator, workState, "aca", 23);
        assumeNextValue(allocator, workState, "acb", 24);
        assumeNextValue(allocator, workState, "acc", 25);
        assumeNoNextBucket(allocator, workState);
    }

    /**
     * Tests obtaining buckets with interval-based string segmentation using new specification of boundary characters.
     */
    @Test
    public void test030StringIntervalBuckets() throws Exception {
        OperationResult result = createOperationResult();
        taskAdd(TASK_030, result);

        Task task = taskManager.getTaskPlain(TASK_030.oid, result);
        ActivityStateType workState = new ActivityStateType(prismContext);

        when();

        BucketAllocator allocator = BucketAllocator.create(getDistributionDefinition(task), contentFactoryCreator);
        BucketContentFactory contentFactory = allocator.getContentFactory();

        then();

        assertBoundariesAndBucketCount(contentFactory, Arrays.asList("05am", "0am"), 13);

        WorkBucketType bucket = assumeNextInterval(allocator, workState, null, "00", 1);

        assertNarrowedQuery(task, bucket,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_NAME).lt("00").matchingNorm()
                        .build()
        );

        bucket = assumeNextInterval(allocator, workState, "00", "0a", 2);

        assertNarrowedQuery(task, bucket,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_NAME).ge("00").matchingNorm()
                        .and().item(UserType.F_NAME).lt("0a").matchingNorm()
                        .build()
        );

        assumeNextInterval(allocator, workState, "0a", "0m", 3);
        assumeNextInterval(allocator, workState, "0m", "50", 4);
        assumeNextInterval(allocator, workState, "50", "5a", 5);
        assumeNextInterval(allocator, workState, "5a", "5m", 6);
        assumeNextInterval(allocator, workState, "5m", "a0", 7);
        assumeNextInterval(allocator, workState, "a0", "aa", 8);
        assumeNextInterval(allocator, workState, "aa", "am", 9);
        assumeNextInterval(allocator, workState, "am", "m0", 10);
        assumeNextInterval(allocator, workState, "m0", "ma", 11);
        assumeNextInterval(allocator, workState, "ma", "mm", 12);
        assumeNextInterval(allocator, workState, "mm", null, 13);
        assumeNoNextBucket(allocator, workState);
    }

    /**
     * Tests the OID buckets strategy. Checks that it provides the correct bucket boundaries.
     */
    @Test
    public void test040OidBuckets() throws Exception {
        OperationResult result = createOperationResult();
        taskAdd(TASK_040, result);

        Task task = taskManager.getTaskPlain(TASK_040.oid, result);

        when();

        BucketContentFactory contentFactory = contentFactoryCreator.createContentFactory(getDistributionDefinition(task));

        then();

        assertBoundaries(contentFactory, singletonList("0123456789abcdef"));
    }

    /**
     * Tests the OID depth 2 buckets strategy. Checks that it provides the correct bucket boundaries.
     */
    @Test
    public void test050OidDepth2() throws Exception {
        OperationResult result = createOperationResult();
        taskAdd(TASK_050, result);

        Task task = taskManager.getTaskPlain(TASK_050.oid, result);

        when();

        BucketContentFactory contentFactory = contentFactoryCreator.createContentFactory(getDistributionDefinition(task));

        then();

        assertBoundaries(contentFactory, Arrays.asList("0123456789abcdef", "0123456789abcdef"));
    }

    /**
     * Tests the get-complete cycle (4x) with explicit, numeric interval segmentation providing 3 buckets.
     */
    @Test
    public void test100NumericExplicitBuckets() throws Exception {
        OperationResult result = createOperationResult();
        taskAdd(TASK_100, result);

        Task task = taskManager.getTaskPlain(TASK_100.oid, result);

        when("1st get");

        WorkBucketType bucket = getWorkBucket(task, result);

        then("1st get");

        assertNumericBucket(bucket, null, 1, null, 123);
        assertNarrowedQuery(task, bucket,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_ITERATION).lt(BigInteger.valueOf(123))
                        .build()
        );

        getTaskAndAssertOptimizedBuckets(task, result);

        when("complete and 2nd get");

        bucketingManager.completeWorkBucket(task.getOid(), ActivityPath.empty(), 1, null, result);
        bucket = getWorkBucket(task, result);

        then("complete and 2nd get");

        assertNumericBucket(bucket, null, 2, 123, 200);
        assertNarrowedQuery(task, bucket,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_ITERATION).ge(BigInteger.valueOf(123))
                        .and().item(UserType.F_ITERATION).lt(BigInteger.valueOf(200))
                        .build()
        );

        getTaskAndAssertOptimizedBuckets(task, result);

        when("complete and 3rd get");

        bucketingManager.completeWorkBucket(task.getOid(), ActivityPath.empty(), 2, null, result);
        bucket = getWorkBucket(task, result);

        then("complete and 3rd get");

        assertNumericBucket(bucket, null, 3, 200, null);
        assertNarrowedQuery(task, bucket,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_ITERATION).ge(BigInteger.valueOf(200))
                        .build()
        );

        getTaskAndAssertOptimizedBuckets(task, result);

        when("complete and 4th get");

        bucketingManager.completeWorkBucket(task.getOid(), ActivityPath.empty(), 3, null, result);
        bucket = getWorkBucket(task, result);

        then("complete and 4th get");

        assertNull("Non-null bucket obtained", bucket);

        getTaskAndAssertOptimizedBuckets(task, result);
    }

    private WorkBucketType getWorkBucket(Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, InterruptedException {
        return getWorkBucket(task, task, 0, result);
    }

    private WorkBucketType getWorkBucket(Task workerTask, Task coordinatorTask, int freeBucketWaitTime, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, InterruptedException {
        ActivityDistributionDefinition distributionDefinition =
                ActivityDistributionDefinition.create(coordinatorTask.getRootActivityDefinitionOrClone(), () -> null);
        return bucketingManager.getWorkBucket(workerTask.getOid(), distributionDefinition, freeBucketWaitTime, result);
    }

    /**
     * Tests the get-complete cycle (4x) with explicit, filter-based segmentation providing 3 buckets.
     */
    @Test
    public void test110FilterExplicitBuckets() throws Exception {
        OperationResult result = createOperationResult();
        taskAdd(TASK_110, result);

        Task task = taskManager.getTaskPlain(TASK_110.oid, result);
        BucketContentFactory contentFactory = contentFactoryCreator.createContentFactory(getDistributionDefinition(task));

        when("1st get");

        WorkBucketType bucket = getWorkBucket(task, result);
        Integer numberOfBuckets = contentFactory.estimateNumberOfBuckets();

        then("1st get");

        assertBucket(bucket, null, 1);
        assertNarrowedQuery(task, bucket,
                prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_NAME).lt(new PolyString("a", "a"))
                        .build()
        );

        Task taskAfter = getTaskAndAssertOptimizedBuckets(task, result);
        assertEquals("Wrong # of estimated buckets (task)", Integer.valueOf(3), getNumberOfBuckets(taskAfter));
        assertEquals("Wrong # of estimated buckets (API)", Integer.valueOf(3), numberOfBuckets);

        when("complete and 2nd get");

        bucketingManager.completeWorkBucket(task.getOid(), ActivityPath.empty(), 1, null, result);
        bucket = getWorkBucket(task, result);

        then("complete and 2nd get");

        assertBucket(bucket, null, 2);
        assertNarrowedQuery(task, bucket,
                prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_NAME).ge(new PolyString("a", "a"))
                        .and().item(ShadowType.F_NAME).lt(new PolyString("m", "m"))
                        .build()
        );
        getTaskAndAssertOptimizedBuckets(task, result);

        when("complete and 3rd get");

        bucketingManager.completeWorkBucket(task.getOid(), ActivityPath.empty(), 2, null, result);
        bucket = getWorkBucket(task, result);

        then("complete and 3rd get");

        assertBucket(bucket, null, 3);
        assertNarrowedQuery(task, bucket,
                prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_NAME).ge(new PolyString("m", "m"))
                        .build()
        );
        getTaskAndAssertOptimizedBuckets(task, result);

        when("complete and 4th get");

        bucketingManager.completeWorkBucket(task.getOid(), ActivityPath.empty(), 3, null, result);
        bucket = getWorkBucket(task, result);

        then("complete and 4th get");

        assertNull("Non-null bucket obtained", bucket);
        getTaskAndAssertOptimizedBuckets(task, result);
    }

    /**
     * Simply checks "get bucket" without any buckets definition.
     */
    @Test
    public void test120GetBucketNoDefinition() throws Exception {
        given();

        OperationResult result = createOperationResult();

        taskAdd(TASK_120, result); // suspended

        Task task = taskManager.getTaskPlain(TASK_120.oid, result);

        when();

        WorkBucketType bucket = getWorkBucket(task, result);

        then();

        assertThat(bucket).isNotNull();

        Task taskAfter = getTaskAndAssertOptimizedBuckets(task, result);

        List<WorkBucketType> wBuckets = getBuckets(taskAfter);
        assertEquals("Wrong # of buckets", 1, wBuckets.size());
        assertBucket(wBuckets.get(0), WorkBucketStateType.READY, 1);
        assertEquals(wBuckets.get(0).getContent(), new NullWorkBucketContentType());
        assertNumberOfBuckets(taskAfter, 1, ActivityPath.empty());
    }

    /**
     * Checks "get bucket" call with bucket creation batch size of 7.
     */
    @Test
    public void test130GetBucketBatched() throws Exception {
        given();

        OperationResult result = createOperationResult();
        taskAdd(TASK_130, result); // suspended

        Task task = taskManager.getTaskPlain(TASK_130.oid, result);

        when();

        WorkBucketType bucket = getWorkBucket(task, result);

        then();

        assertThat(bucket).isNotNull();

        Task taskAfter = getTaskAndAssertOptimizedBuckets(task, result);

        List<WorkBucketType> wBuckets = getBuckets(taskAfter);
        assertEquals("Wrong # of buckets", 7, wBuckets.size());
        assertBucket(wBuckets.get(0), WorkBucketStateType.READY, 1);
        assertNumberOfBuckets(taskAfter, 1000, ActivityPath.empty());
    }

    /**
     * Checks 2x get bucket call, with some complete calls.
     */
    @Test
    public void test140GetTwoBuckets() throws Exception {
        given();

        OperationResult result = createOperationResult();
        taskAdd(TASK_140, result);

        Task task = taskManager.getTaskPlain(TASK_140.oid, result);

        when();

        WorkBucketType bucket1 = getWorkBucket(task, result);
        WorkBucketType bucket2 = getWorkBucket(task, result);

        then();

        displayValue("1st obtained bucket", bucket1);
        displayValue("2nd obtained bucket", bucket2);
        task = getTaskAndAssertOptimizedBuckets(task, result);

        assertNumericBucket(bucket1, WorkBucketStateType.READY, 1, 0, 100);
        assertNumericBucket(bucket2, WorkBucketStateType.READY, 1, 0, 100); // should be the same

        List<WorkBucketType> buckets = new ArrayList<>(getBuckets(task));
        assertEquals(1, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 1, 0, 100);

        when("complete");

        bucketingManager.completeWorkBucket(task.getOid(), ActivityPath.empty(), 1, null, result);
        WorkBucketType bucket3 = getWorkBucket(task, result);

        then("complete");

        displayValue("bucket obtained after complete", bucket3);
        task = getTaskAndAssertOptimizedBuckets(task, result);

        assertNumericBucket(bucket3, WorkBucketStateType.READY, 2, 100, 200);

        buckets = new ArrayList<>(getBuckets(task));
        sortBucketsBySequentialNumber(buckets);
        assertEquals(2, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 1, 0, 100);
        assertNumericBucket(buckets.get(1), WorkBucketStateType.READY, 2, 100, 200);

        when("complete 2");

        bucketingManager.completeWorkBucket(task.getOid(), ActivityPath.empty(), 2, null, result);
        WorkBucketType bucket4 = getWorkBucket(task, result);

        then("complete 2");

        displayValue("bucket obtained after 2nd complete", bucket4);
        task = getTaskAndAssertOptimizedBuckets(task, result);

        assertNumericBucket(bucket4, WorkBucketStateType.READY, 3, 200, 300);

        buckets = new ArrayList<>(getBuckets(task));
        sortBucketsBySequentialNumber(buckets);
        assertEquals(2, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 2, 100, 200);
        assertNumericBucket(buckets.get(1), WorkBucketStateType.READY, 3, 200, 300);
    }

    /**
     * Invalid task: no segmentation specified.
     */
    @Test
    public void test150NoSegmentation() throws Exception {
        given();

        OperationResult result = createOperationResult();
        taskAdd(TASK_150, result); // suspended

        Task task = taskManager.getTaskPlain(TASK_150.oid, result);

        try {
            when();

            getWorkBucket(task, result);
            fail("unexpected success");
        } catch (IllegalStateException e) {

            then();

            System.out.println("Got expected exception: " + e.getMessage());
        }
    }

    /**
     * Checks "get bucket" operation on task pair (coordinator + worker).
     */
    @Test
    public void test200GetBucket() throws Exception {
        given();
        OperationResult result = createOperationResult();
        taskAdd(TASK_200_COORDINATOR, result); // suspended
        taskAdd(TASK_200_WORKER, result); // suspended

        Task worker = taskManager.getTaskPlain(TASK_200_WORKER.oid, result);
        Task coordinator = taskManager.getTaskPlain(TASK_200_COORDINATOR.oid, result);

        when();

        WorkBucketType bucket = getWorkBucket(worker, coordinator, 0, result);

        then();

        displayValue("allocated bucket", bucket);
        Task coordinatorAfter = taskManager.getTaskPlain(TASK_200_COORDINATOR.oid, result);
        Task workerAfter = taskManager.getTaskPlain(worker.getOid(), result);
        displayDumpable("coordinator task after", coordinatorAfter);
        displayDumpable("worker task after", workerAfter);

        assertNumericBucket(bucket, null, 1, 0, 1000);
        List<WorkBucketType> wBuckets = getBuckets(workerAfter);
        assertNumericBucket(wBuckets.get(0), WorkBucketStateType.READY, 1, 0, 1000);
        List<WorkBucketType> cBuckets = getBuckets(coordinatorAfter);
        assertNumericBucket(cBuckets.get(0), WorkBucketStateType.DELEGATED, 1, 0, 1000);
        assertNumberOfBuckets(coordinatorAfter, 100, ActivityPath.empty());

        assertOptimizedCompletedBuckets(coordinatorAfter, ActivityPath.empty());
    }

    /**
     * Check a sequence of get / release / complete operations on coordinator + 5 worker tasks.
     */
    @Test
    public void test210GetReleaseCompleteSequence() throws Exception {
        OperationResult result = createOperationResult();
        taskAdd(TASK_210_COORDINATOR, result); // suspended
        taskAdd(TASK_210_WORKER_1, result); // suspended
        taskAdd(TASK_210_WORKER_2, result); // suspended
        taskAdd(TASK_210_WORKER_3, result); // suspended
        taskAdd(TASK_210_WORKER_4, result); // suspended
        taskAdd(TASK_210_WORKER_5, result); // suspended

        Task coordinator = taskManager.getTaskPlain(TASK_210_COORDINATOR.oid, result);
        Task worker1 = taskManager.getTaskPlain(TASK_210_WORKER_1.oid, result);
        Task worker2 = taskManager.getTaskPlain(TASK_210_WORKER_2.oid, result);
        Task worker3 = taskManager.getTaskPlain(TASK_210_WORKER_3.oid, result);
        Task worker4 = taskManager.getTaskPlain(TASK_210_WORKER_4.oid, result);
        Task worker5 = taskManager.getTaskPlain(TASK_210_WORKER_5.oid, result);

        when();

        WorkBucketType bucket1 = getWorkBucket(worker1, coordinator, 0, result);
        WorkBucketType bucket2 = getWorkBucket(worker2, coordinator, 0, result);
        WorkBucketType bucket3 = getWorkBucket(worker3, coordinator, 0, result);
        WorkBucketType bucket4 = getWorkBucket(worker4, coordinator, 0, result);
        WorkBucketType bucket4a = getWorkBucket(worker4, coordinator, 0, result);     // should be the same as bucket4

        then();

        displayValue("1st allocated bucket", bucket1);
        displayValue("2nd allocated bucket", bucket2);
        displayValue("3rd allocated bucket", bucket3);
        displayValue("4th allocated bucket", bucket4);
        displayValue("4+th allocated bucket", bucket4a);
        worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
        worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
        worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
        worker4 = taskManager.getTaskPlain(worker4.getOid(), result);
        coordinator.refresh(result);
        displayDumpable("coordinator task after 4+1x allocation", coordinator);
        displayDumpable("worker1 task after 4+1x allocation", worker1);
        displayDumpable("worker2 task after 4+1x allocation", worker2);
        displayDumpable("worker3 task after 4+1x allocation", worker3);
        displayDumpable("worker4 task after 4+1x allocation", worker4);

        assertNumericBucket(bucket1, null, 1, 0, 1);
        assertNumericBucket(bucket2, null, 2, 1, 2);
        assertNumericBucket(bucket3, null, 3, 2, 3);
        assertNumericBucket(bucket4, null, 4, 3, 4);
        assertNumericBucket(bucket4a, null, 4, 3, 4);
        List<WorkBucketType> buckets = new ArrayList<>(getBuckets(coordinator));
        sortBucketsBySequentialNumber(buckets);
        assertEquals(5, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 1, 0, 1);
        assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 2, 1, 2);
        assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 3, 2, 3);
        assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 4, 3, 4);
        assertNumericBucket(buckets.get(4), WorkBucketStateType.READY, 5, 4, 5);        // pre-created

        buckets = new ArrayList<>(getBuckets(worker1));
        assertEquals(1, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 1, 0, 1);
        buckets = new ArrayList<>(getBuckets(worker2));
        assertEquals(1, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 2, 1, 2);
        buckets = new ArrayList<>(getBuckets(worker3));
        assertEquals(1, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 3, 2, 3);
        buckets = new ArrayList<>(getBuckets(worker4));
        assertEquals(1, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 4, 3, 4);

        when("complete bucket #2");

        bucketingManager.completeWorkBucket(worker2.getOid(), ActivityPath.empty(), 2, null, result);

        then("complete bucket #2");

        worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
        displayDumpable("worker2 after completion of 2nd bucket", worker2);
        coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
        displayDumpable("coordinator after completion of 2nd bucket", coordinator);

        buckets = new ArrayList<>(getBuckets(coordinator));
        sortBucketsBySequentialNumber(buckets);

        assertEquals(5, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 1, 0, 1);
        assertNumericBucket(buckets.get(1), WorkBucketStateType.COMPLETE, 2, 1, 2);
        assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 3, 2, 3);
        assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 4, 3, 4);
        assertNumericBucket(buckets.get(4), WorkBucketStateType.READY, 5, 4, 5);        // pre-created

        assertNoWorkBuckets(worker2.getWorkState().getActivity());

        when("complete bucket #1");

        bucketingManager.completeWorkBucket(worker1.getOid(), ActivityPath.empty(), 1, null, result);
        WorkBucketType bucket = getWorkBucket(worker1, coordinator, 0, result);

        then("complete bucket #1");

        worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
        displayDumpable("worker1 after completion of 1st bucket and fetching next one", worker1);
        coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
        displayDumpable("coordinator after completion of 1st bucket and fetching next one", coordinator);

        assertNumericBucket(bucket, null, 5, 4, 5);

        buckets = new ArrayList<>(getBuckets(coordinator));
        sortBucketsBySequentialNumber(buckets);

        assertEquals(4, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 2, 1, 2);
        assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 3, 2, 3);
        assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 4, 3, 4);
        assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 5, 4, 5);

        buckets = new ArrayList<>(getBuckets(worker1));
        assertEquals(1, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 5, 4, 5);

        when("no more buckets");

        WorkBucketType nothing = getWorkBucket(worker5, coordinator, 0, result);

        then("no more buckets");

        assertNull("Found bucket even if none should be found", nothing);

        when("release bucket #4");

        // TODO set some state here and check its transfer to coordinator task
        bucketingManager.releaseWorkBucket(worker4.getOid(), ActivityPath.empty(), 4, null, result);

        then("release bucket #4");

        worker4 = taskManager.getTaskPlain(worker4.getOid(), result);
        displayDumpable("worker4 after releasing of 4th bucket", worker4);
        coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
        displayDumpable("coordinator after releasing of 4th bucket", coordinator);

        buckets = new ArrayList<>(getBuckets(coordinator));
        sortBucketsBySequentialNumber(buckets);

        assertEquals(4, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 2, 1, 2);
        assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 3, 2, 3);
        assertNumericBucket(buckets.get(2), WorkBucketStateType.READY, 4, 3, 4);
        assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 5, 4, 5);

        assertNoWorkBuckets(worker4.getWorkState().getActivity());

        when("complete bucket #3");

        bucketingManager.completeWorkBucket(worker3.getOid(), ActivityPath.empty(), 3, null, result);
        bucket = getWorkBucket(worker5, coordinator, 0, result);

        then("complete bucket #3");

        worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
        displayDumpable("worker3 after completion of 3rd bucket and getting next one", worker3);
        worker5 = taskManager.getTaskPlain(worker5.getOid(), result);
        displayDumpable("worker5 after completion of 3rd bucket and getting next one", worker5);
        coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
        displayDumpable("coordinator after completion of 3rd bucket and getting next one", coordinator);

        assertNumericBucket(bucket, null, 4, 3, 4);

        buckets = new ArrayList<>(getBuckets(coordinator));
        sortBucketsBySequentialNumber(buckets);
        assertEquals(3, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 3, 2, 3);
        assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 4, 3, 4);
        assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 5, 4, 5);

        assertNoWorkBuckets(worker3.getWorkState().getActivity());

        buckets = new ArrayList<>(getBuckets(worker5));
        assertEquals(1, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 4, 3, 4);

        when("complete bucket #5");

        bucketingManager.completeWorkBucket(worker1.getOid(), ActivityPath.empty(), 5, null, result);
        taskManager.closeTask(worker5, result);

        then("complete bucket #5");

        worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
        displayDumpable("worker1 after completion of 5th bucket and closing worker5", worker1);
        worker5 = taskManager.getTaskPlain(worker5.getOid(), result);
        displayDumpable("worker5 after completion of 5th bucket and closing worker5", worker5);
        coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
        displayDumpable("coordinator after completion of 5th bucket and closing worker5", coordinator);

        buckets = new ArrayList<>(getOrCreateBuckets(coordinator.getWorkState().getActivity()));
        assertEquals(2, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 4, 3, 4);
        assertNumericBucket(buckets.get(1), WorkBucketStateType.COMPLETE, 5, 4, 5);

        assertNoWorkBuckets(worker1.getWorkState().getActivity());

        when("reclaiming mis-allocated bucket");

        bucket = getWorkBucket(worker1, coordinator, -1, result);
        assertThat(bucket).isNotNull();

        then("reclaiming mis-allocated bucket");

        worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
        displayDumpable("worker1 after reclaiming mis-allocated bucket", worker1);
        coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
        displayDumpable("coordinator after reclaiming mis-allocated bucket", coordinator);

        assertNumericBucket(bucket, null, 4, 3, 4);

        buckets = new ArrayList<>(getBuckets(coordinator));
        assertEquals(2, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 4, 3, 4);
        assertNumericBucket(buckets.get(1), WorkBucketStateType.COMPLETE, 5, 4, 5);

        buckets = new ArrayList<>(getBuckets(worker1));
        assertEquals(1, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 4, 3, 4);

        when("complete bucket #4");

        bucketingManager.completeWorkBucket(worker1.getOid(), ActivityPath.empty(), 4, null, result);

        then("complete bucket #4");

        worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
        displayDumpable("worker1 after completion of 4th bucket", worker1);
        coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
        displayDumpable("coordinator after completion of 4th bucket", coordinator);

        buckets = new ArrayList<>(getBuckets(coordinator));
        assertEquals(1, buckets.size());
        assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 5, 4, 5);

        assertNoWorkBuckets(worker1.getWorkState().getActivity());
    }

    private WorkBucketType assumeNextValue(BucketAllocator allocator, ActivityStateType workState,
            String expectedNextValue, int expectedSequentialNumber) throws SchemaException {
        WorkBucketType newBucket = getNextBucket(allocator, workState, expectedSequentialNumber);
        AbstractWorkBucketContentType content = newBucket.getContent();
        assertEquals("Wrong content class", StringValueWorkBucketContentType.class, content.getClass());
        StringValueWorkBucketContentType prefixContent = (StringValueWorkBucketContentType) content;
        assertEquals("Wrong # of values generated", 1, prefixContent.getValue().size());
        assertEquals("Wrong next value", expectedNextValue, prefixContent.getValue().get(0));

        getOrCreateBuckets(workState)
                .add(newBucket.clone().state(WorkBucketStateType.COMPLETE));
        return newBucket;
    }

    private WorkBucketType assumeNextPrefix(BucketAllocator allocator, ActivityStateType workState,
            String expectedNextPrefix, int expectedSequentialNumber) throws SchemaException {
        WorkBucketType newBucket = getNextBucket(allocator, workState, expectedSequentialNumber);
        AbstractWorkBucketContentType content = newBucket.getContent();
        assertEquals("Wrong content class", StringPrefixWorkBucketContentType.class, content.getClass());
        StringPrefixWorkBucketContentType prefixContent = (StringPrefixWorkBucketContentType) content;
        assertEquals("Wrong # of prefixes generated", 1, prefixContent.getPrefix().size());
        assertEquals("Wrong next prefix", expectedNextPrefix, prefixContent.getPrefix().get(0));

        getOrCreateBuckets(workState) // assuming the bucketing is initialized
                .add(newBucket.clone().state(WorkBucketStateType.COMPLETE));
        return newBucket;
    }

    private WorkBucketType assumeNextInterval(BucketAllocator allocator, ActivityStateType workState,
            String expectedNextFrom, String expectedNextTo, int expectedSequentialNumber) throws SchemaException {
        WorkBucketType newBucket = getNextBucket(allocator, workState, expectedSequentialNumber);
        AbstractWorkBucketContentType content = newBucket.getContent();
        assertEquals("Wrong content class", StringIntervalWorkBucketContentType.class, content.getClass());
        StringIntervalWorkBucketContentType intervalContent = (StringIntervalWorkBucketContentType) content;
        assertEquals("Wrong next 'from'", expectedNextFrom, intervalContent.getFrom());
        assertEquals("Wrong next 'to'", expectedNextTo, intervalContent.getTo());

        getOrCreateBuckets(workState)
                .add(newBucket.clone().state(WorkBucketStateType.COMPLETE));
        return newBucket;
    }

    @NotNull
    private WorkBucketType getNextBucket(BucketAllocator allocator, ActivityStateType workState,
            int expectedSequentialNumber) throws SchemaException {
        BucketAllocator.Response response = allocator.getBucket(getOrCreateBuckets(workState));
        displayValue("get bucket response", response);
        assertTrue("Wrong answer", response instanceof BucketAllocator.Response.NewBuckets);
        BucketAllocator.Response.NewBuckets nbr = (BucketAllocator.Response.NewBuckets) response;
        displayValue("new buckets obtained", nbr.newBuckets);
        assertEquals("Wrong new buckets count", 1, nbr.newBuckets.size());
        WorkBucketType newBucket = nbr.newBuckets.get(0);
        assertEquals("Wrong sequential number", expectedSequentialNumber, newBucket.getSequentialNumber());
        return newBucket;
    }

    private void assumeNoNextBucket(BucketAllocator allocator, ActivityStateType workState) throws SchemaException {
        BucketAllocator.Response response = allocator.getBucket(getBuckets(workState));
        displayValue("get bucket response", response);
        assertTrue("Wrong answer", response instanceof BucketAllocator.Response.NothingFound);
        BucketAllocator.Response.NothingFound nothingFound = (BucketAllocator.Response.NothingFound) response;
        //noinspection SimplifiedTestNGAssertion
        assertEquals("Wrong definite flag", true, nothingFound.definite);
    }

    private List<WorkBucketType> getBuckets(Task task) {
        return getOrCreateBuckets(task.getWorkState().getActivity());
    }

    private List<WorkBucketType> getBuckets(ActivityStateType workState) {
        return BucketingUtil.getBuckets(workState);
    }

    private List<WorkBucketType> getOrCreateBuckets(ActivityStateType workState) {
        if (workState.getBucketing() == null) {
            workState.setBucketing(new ActivityBucketingStateType(prismContext));
        }
        return workState.getBucketing().getBucket();
    }

    private Integer getNumberOfBuckets(Task task) {
        return BucketingUtil.getNumberOfBuckets(task.getWorkState().getActivity());
    }

    private ActivityDistributionDefinition getDistributionDefinition(Task task) {
        return ActivityDistributionDefinition.create(task.getRootActivityDefinitionOrClone(), () -> null);
    }

    private void assertBoundariesAndBucketCount(BucketContentFactory contentFactory,
            List<String> expectedBoundaries, int expectedCount) {
        assertBoundaries(contentFactory, expectedBoundaries);
        assertEquals("Wrong # of estimated buckets", Integer.valueOf(expectedCount), contentFactory.estimateNumberOfBuckets());
    }

    private void assertBoundaries(BucketContentFactory contentFactory, List<String> expectedBoundaries) {
        assertEquals("Wrong expanded boundaries", expectedBoundaries,
                ((StringBucketContentFactory) contentFactory).getBoundaries());
    }

    private void assertNarrowedQuery(Task task, WorkBucketType bucket, ObjectQuery expectedQuery)
            throws SchemaException {
        ActivityDistributionDefinition distributionDefinition = getDistributionDefinition(task);
        ObjectQuery narrowedQuery = bucketingManager
                .narrowQueryForWorkBucket(UserType.class, null, distributionDefinition, null, bucket);
        displayDumpable("narrowed query", narrowedQuery);
        PrismAsserts.assertQueriesEquivalent("Wrong narrowed query", expectedQuery, narrowedQuery);
    }

    private Task getTaskAndAssertOptimizedBuckets(Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        Task taskAfter = taskManager.getTaskPlain(task.getOid(), result);
        displayDumpable("task after", taskAfter);
        assertOptimizedCompletedBuckets(taskAfter, ActivityPath.empty());
        return taskAfter;
    }
}
