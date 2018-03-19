/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.quartzimpl.work.WorkStateManager;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategyFactory;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.annotation.PostConstruct;

import java.math.BigInteger;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Low level tests of work bucket strategies.
 *
 * @author mederly
 */

@ContextConfiguration(locations = {"classpath:ctx-task-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestWorkBucketStrategies extends AbstractTaskManagerTest {

	private static final transient Trace LOGGER = TraceManager.getTrace(TestWorkBucketStrategies.class);

	@Autowired private WorkStateManager workStateManager;
	@Autowired private WorkSegmentationStrategyFactory strategyFactory;

	private static String taskFilename(String testName, String subId) {
		return "src/test/resources/work-buckets/task-" + testNumber(testName) + "-" + subId + ".xml";
	}

	private static String taskFilename(String testName) {
		return taskFilename(testName, "0");
	}

	private static String taskOid(String testName, String subId) {
		return "44444444-0000-0000-0000-" + testNumber(testName) + subId + "00000000";
	}

	private static String taskOid(String test) {
		return taskOid(test, "0");
	}

	private static String testNumber(String test) {
		return test.substring(4, 7);
	}

	@NotNull
	protected String workerTaskFilename(String TEST_NAME) {
		return taskFilename(TEST_NAME, "w");
	}

	@NotNull
	protected String coordinatorTaskFilename(String TEST_NAME) {
		return taskFilename(TEST_NAME, "c");
	}

	@NotNull
	protected String workerTaskOid(String TEST_NAME) {
		return taskOid(TEST_NAME, "w");
	}

	@NotNull
	protected String coordinatorTaskOid(String TEST_NAME) {
		return taskOid(TEST_NAME, "c");
	}

	@PostConstruct
	public void initialize() throws Exception {
		super.initialize();
		workStateManager.setFreeBucketWaitInterval(1000L);
		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

    @Test
    public void test000Integrity() {
        AssertJUnit.assertNotNull(repositoryService);
        AssertJUnit.assertNotNull(taskManager);
    }

    @Test
    public void test100NumericExplicitBuckets() throws Exception {
        final String TEST_NAME = "test100NumericExplicitBuckets";
        OperationResult result = createResult(TEST_NAME, LOGGER);
        addObjectFromFile(taskFilename(TEST_NAME));

        TaskQuartzImpl task = taskManager.getTask(taskOid(TEST_NAME), result);

        // WHEN
	    WorkBucketType bucket = workStateManager.getWorkBucket(task.getOid(), 0, null, result);
	    ObjectQuery narrowedQuery = workStateManager
			    .narrowQueryForWorkBucket(task, new ObjectQuery(), UserType.class, null, bucket, result);

	    // THEN
	    display("allocated bucket", bucket);
	    TaskQuartzImpl taskAfter = taskManager.getTask(task.getOid(), result);
	    display("task after", taskAfter);
	    display("narrowed query", narrowedQuery);

	    assertNumericBucket(bucket, null, 1, null, 123);
	    assertOptimizedCompletedBuckets(taskAfter);
	    ObjectQuery expectedQuery = QueryBuilder.queryFor(UserType.class, prismContext)
			    .item(UserType.F_ITERATION).lt(BigInteger.valueOf(123))
			    .build();
	    PrismAsserts.assertQueriesEquivalent("Wrong narrowed query", expectedQuery, narrowedQuery);

	    // WHEN (complete and allocate next)
	    workStateManager.completeWorkBucket(task.getOid(), 1, result);
	    bucket = workStateManager.getWorkBucket(task.getOid(), 0, null, result);
	    narrowedQuery = workStateManager
			    .narrowQueryForWorkBucket(task, new ObjectQuery(), UserType.class, null, bucket, result);
	    // THEN
	    display("allocated bucket (2)", bucket);
	    taskAfter = taskManager.getTask(task.getOid(), result);
	    display("task after (2)", taskAfter);
	    display("narrowed query (2)", narrowedQuery);
	    assertNumericBucket(bucket, null, 2, 123, 200);
	    assertOptimizedCompletedBuckets(taskAfter);

	    expectedQuery = QueryBuilder.queryFor(UserType.class, prismContext)
			    .item(UserType.F_ITERATION).ge(BigInteger.valueOf(123))
			    .and().item(UserType.F_ITERATION).lt(BigInteger.valueOf(200))
			    .build();
	    PrismAsserts.assertQueriesEquivalent("Wrong narrowed query (2)", expectedQuery, narrowedQuery);

	    // WHEN (complete and allocate next)
	    workStateManager.completeWorkBucket(task.getOid(), 2, result);
	    bucket = workStateManager.getWorkBucket(task.getOid(), 0, null, result);
	    narrowedQuery = workStateManager
			    .narrowQueryForWorkBucket(task, new ObjectQuery(), UserType.class, null, bucket, result);

	    // THEN
	    display("allocated bucket (3)", bucket);
	    taskAfter = taskManager.getTask(task.getOid(), result);
	    display("task after (3)", taskAfter);
	    display("narrowed query (3)", narrowedQuery);

	    assertNumericBucket(bucket, null, 3, 200, null);
	    assertOptimizedCompletedBuckets(taskAfter);
	    expectedQuery = QueryBuilder.queryFor(UserType.class, prismContext)
			    .item(UserType.F_ITERATION).ge(BigInteger.valueOf(200))
			    .build();
	    PrismAsserts.assertQueriesEquivalent("Wrong narrowed query (3)", expectedQuery, narrowedQuery);

	    // WHEN (complete and allocate next)
	    workStateManager.completeWorkBucket(task.getOid(), 3, result);
	    bucket = workStateManager.getWorkBucket(task.getOid(), 0, null, result);

	    // THEN
	    display("allocated bucket (4)", String.valueOf(bucket));
	    taskAfter = taskManager.getTask(task.getOid(), result);
	    display("task after (4)", taskAfter);

	    assertEquals("Expected null bucket", null, bucket);

	    assertOptimizedCompletedBuckets(taskAfter);

	    suspendAndDeleteTasks(taskAfter.getOid());
    }

    @Test
    public void test110FilterExplicitBuckets() throws Exception {
        final String TEST_NAME = "test110FilterExplicitBuckets";
        OperationResult result = createResult(TEST_NAME, LOGGER);
        addObjectFromFile(taskFilename(TEST_NAME));

        TaskQuartzImpl task = taskManager.getTask(taskOid(TEST_NAME), result);

        // WHEN
	    WorkSegmentationStrategy segmentationStrategy = strategyFactory.createStrategy(task.getWorkManagement());
	    WorkBucketType bucket = workStateManager.getWorkBucket(task.getOid(), 0, null, result);
	    ObjectQuery narrowedQuery = workStateManager
			    .narrowQueryForWorkBucket(task, new ObjectQuery(), ShadowType.class, null, bucket, result);
	    Integer numberOfBuckets = segmentationStrategy.estimateNumberOfBuckets(null);

	    // THEN
	    display("allocated bucket", bucket);
	    TaskQuartzImpl taskAfter = taskManager.getTask(task.getOid(), result);
	    display("task after", taskAfter);
	    display("narrowed query", narrowedQuery);

	    assertEquals("Wrong # of estimated buckets (API)", Integer.valueOf(3), numberOfBuckets);
	    assertEquals("Wrong # of estimated buckets (task)", Integer.valueOf(3), taskAfter.getWorkState().getNumberOfBuckets());

	    assertBucket(bucket, null, 1);
	    assertOptimizedCompletedBuckets(taskAfter);
	    ObjectQuery expectedQuery = QueryBuilder.queryFor(ShadowType.class, prismContext)
			    .item(ShadowType.F_NAME).lt(new PolyString("a", "a"))
			    .build();
	    PrismAsserts.assertQueriesEquivalent("Wrong narrowed query", expectedQuery, narrowedQuery);

	    // WHEN (complete and allocate next)
	    workStateManager.completeWorkBucket(task.getOid(), 1, result);
	    bucket = workStateManager.getWorkBucket(task.getOid(), 0, null, result);
	    narrowedQuery = workStateManager
			    .narrowQueryForWorkBucket(task, new ObjectQuery(), ShadowType.class, null, bucket, result);

	    // THEN
	    display("allocated bucket (2)", bucket);
	    taskAfter = taskManager.getTask(task.getOid(), result);
	    display("task after (2)", taskAfter);
	    display("narrowed query (2)", narrowedQuery);

	    assertBucket(bucket, null, 2);
	    assertOptimizedCompletedBuckets(taskAfter);

	    expectedQuery = QueryBuilder.queryFor(ShadowType.class, prismContext)
			    .item(ShadowType.F_NAME).ge(new PolyString("a", "a"))
			    .and().item(ShadowType.F_NAME).lt(new PolyString("m", "m"))
			    .build();
	    PrismAsserts.assertQueriesEquivalent("Wrong narrowed query (2)", expectedQuery, narrowedQuery);

	    // WHEN (complete and allocate next)
	    workStateManager.completeWorkBucket(task.getOid(), 2, result);
	    bucket = workStateManager.getWorkBucket(task.getOid(), 0, null, result);
	    narrowedQuery = workStateManager
			    .narrowQueryForWorkBucket(task, new ObjectQuery(), ShadowType.class, null, bucket, result);

	    // THEN
	    display("allocated bucket (3)", bucket);
	    taskAfter = taskManager.getTask(task.getOid(), result);
	    display("task after (3)", taskAfter);
	    display("narrowed query (3)", narrowedQuery);

	    assertBucket(bucket, null, 3);
	    assertOptimizedCompletedBuckets(taskAfter);
	    expectedQuery = QueryBuilder.queryFor(ShadowType.class, prismContext)
			    .item(ShadowType.F_NAME).ge(new PolyString("m", "m"))
			    .build();
	    PrismAsserts.assertQueriesEquivalent("Wrong narrowed query (3)", expectedQuery, narrowedQuery);

	    // WHEN (complete and allocate next)
	    workStateManager.completeWorkBucket(task.getOid(), 3, result);
	    bucket = workStateManager.getWorkBucket(task.getOid(), 0, null, result);

	    // THEN
	    display("allocated bucket (4)", String.valueOf(bucket));
	    taskAfter = taskManager.getTask(task.getOid(), result);
	    display("task after (4)", taskAfter);

	    assertEquals("Expected null bucket", null, bucket);

	    assertOptimizedCompletedBuckets(taskAfter);

	    suspendAndDeleteTasks(taskAfter.getOid());
    }

	@Test
	public void test120StringPrefixBuckets() throws Exception {
		final String TEST_NAME = "test120StringPrefixBuckets";
		OperationResult result = createResult(TEST_NAME, LOGGER);
		addObjectFromFile(taskFilename(TEST_NAME));

		TaskQuartzImpl task = taskManager.getTask(taskOid(TEST_NAME), result);

		// WHEN
		WorkSegmentationStrategy segmentationStrategy = strategyFactory.createStrategy(task.getWorkManagement());
		TaskWorkStateType workState = new TaskWorkStateType(prismContext);

		// WHEN+THEN
		// a, 01abc, 01abc
		assertEquals("Wrong # of estimated buckets", Integer.valueOf(25), segmentationStrategy.estimateNumberOfBuckets(null));

		WorkBucketType bucket = assumeNextPrefix(segmentationStrategy, workState, "a00", 1);
		ObjectQuery narrowedQuery = workStateManager
				.narrowQueryForWorkBucket(task, new ObjectQuery(), UserType.class, null, bucket, result);
		display("narrowed query (1)", narrowedQuery);
		ObjectQuery expectedQuery = QueryBuilder.queryFor(UserType.class, prismContext)
				.item(UserType.F_NAME).startsWith("a00").matchingNorm()
				.build();
		PrismAsserts.assertQueriesEquivalent("Wrong narrowed query (1)", expectedQuery, narrowedQuery);

		assumeNextPrefix(segmentationStrategy, workState, "a01", 2);
		assumeNextPrefix(segmentationStrategy, workState, "a0a", 3);
		assumeNextPrefix(segmentationStrategy, workState, "a0b", 4);
		assumeNextPrefix(segmentationStrategy, workState, "a0c", 5);
		assumeNextPrefix(segmentationStrategy, workState, "a10", 6);
		assumeNextPrefix(segmentationStrategy, workState, "a11", 7);
		assumeNextPrefix(segmentationStrategy, workState, "a1a", 8);
		assumeNextPrefix(segmentationStrategy, workState, "a1b", 9);
		assumeNextPrefix(segmentationStrategy, workState, "a1c", 10);
		assumeNextPrefix(segmentationStrategy, workState, "aa0", 11);
		assumeNextPrefix(segmentationStrategy, workState, "aa1", 12);
		assumeNextPrefix(segmentationStrategy, workState, "aaa", 13);
		assumeNextPrefix(segmentationStrategy, workState, "aab", 14);
		assumeNextPrefix(segmentationStrategy, workState, "aac", 15);
		assumeNextPrefix(segmentationStrategy, workState, "ab0", 16);
		assumeNextPrefix(segmentationStrategy, workState, "ab1", 17);
		assumeNextPrefix(segmentationStrategy, workState, "aba", 18);
		assumeNextPrefix(segmentationStrategy, workState, "abb", 19);
		assumeNextPrefix(segmentationStrategy, workState, "abc", 20);
		assumeNextPrefix(segmentationStrategy, workState, "ac0", 21);
		assumeNextPrefix(segmentationStrategy, workState, "ac1", 22);
		assumeNextPrefix(segmentationStrategy, workState, "aca", 23);
		assumeNextPrefix(segmentationStrategy, workState, "acb", 24);
		assumeNextPrefix(segmentationStrategy, workState, "acc", 25);
		assumeNoNextBucket(segmentationStrategy, workState);

		suspendAndDeleteTasks(task.getOid());
	}

	@Test
	public void test130StringIntervalBuckets() throws Exception {
		final String TEST_NAME = "test130StringIntervalBuckets";
		OperationResult result = createResult(TEST_NAME, LOGGER);
		addObjectFromFile(taskFilename(TEST_NAME));

		TaskQuartzImpl task = taskManager.getTask(taskOid(TEST_NAME), result);

		// WHEN
		WorkSegmentationStrategy segmentationStrategy = strategyFactory.createStrategy(task.getWorkManagement());
		TaskWorkStateType workState = new TaskWorkStateType(prismContext);

		// WHEN+THEN
		// 05am, 0am
		assertEquals("Wrong # of estimated buckets", Integer.valueOf(13), segmentationStrategy.estimateNumberOfBuckets(null));
		WorkBucketType bucket = assumeNextInterval(segmentationStrategy, workState, null, "00", 1);
		ObjectQuery narrowedQuery = workStateManager
				.narrowQueryForWorkBucket(task, new ObjectQuery(), UserType.class, null, bucket, result);
		display("narrowed query (1)", narrowedQuery);
		ObjectQuery expectedQuery = QueryBuilder.queryFor(UserType.class, prismContext)
				.item(UserType.F_NAME).lt("00").matchingNorm()
				.build();
		PrismAsserts.assertQueriesEquivalent("Wrong narrowed query (1)", expectedQuery, narrowedQuery);

		bucket = assumeNextInterval(segmentationStrategy, workState, "00", "0a", 2);
		narrowedQuery = workStateManager
				.narrowQueryForWorkBucket(task, new ObjectQuery(), UserType.class, null, bucket, result);
		display("narrowed query (2)", narrowedQuery);
		expectedQuery = QueryBuilder.queryFor(UserType.class, prismContext)
				.item(UserType.F_NAME).ge("00").matchingNorm()
				.and().item(UserType.F_NAME).lt("0a").matchingNorm()
				.build();
		PrismAsserts.assertQueriesEquivalent("Wrong narrowed query (2)", expectedQuery, narrowedQuery);

		assumeNextInterval(segmentationStrategy, workState, "0a", "0m", 3);
		assumeNextInterval(segmentationStrategy, workState, "0m", "50", 4);
		assumeNextInterval(segmentationStrategy, workState, "50", "5a", 5);
		assumeNextInterval(segmentationStrategy, workState, "5a", "5m", 6);
		assumeNextInterval(segmentationStrategy, workState, "5m", "a0", 7);
		assumeNextInterval(segmentationStrategy, workState, "a0", "aa", 8);
		assumeNextInterval(segmentationStrategy, workState, "aa", "am", 9);
		assumeNextInterval(segmentationStrategy, workState, "am", "m0", 10);
		assumeNextInterval(segmentationStrategy, workState, "m0", "ma", 11);
		assumeNextInterval(segmentationStrategy, workState, "ma", "mm", 12);
		assumeNextInterval(segmentationStrategy, workState, "mm", null, 13);
		assumeNoNextBucket(segmentationStrategy, workState);

		suspendAndDeleteTasks(task.getOid());
	}

	private WorkBucketType assumeNextPrefix(WorkSegmentationStrategy segmentationStrategy, TaskWorkStateType workState,
			String expectedNextPrefix, int expectedSequentialNumber) throws SchemaException {
		WorkBucketType newBucket = getNextBucket(segmentationStrategy, workState, expectedSequentialNumber);
		AbstractWorkBucketContentType content = newBucket.getContent();
		assertEquals("Wrong content class", StringPrefixWorkBucketContentType.class, content.getClass());
		StringPrefixWorkBucketContentType prefixContent = (StringPrefixWorkBucketContentType) content;
		assertEquals("Wrong # of prefixes generated", 1, prefixContent.getPrefix().size());
		assertEquals("Wrong next prefix", expectedNextPrefix, prefixContent.getPrefix().get(0));

		workState.getBucket().add(newBucket.clone().state(WorkBucketStateType.COMPLETE));
		return newBucket;
	}

	private WorkBucketType assumeNextInterval(WorkSegmentationStrategy segmentationStrategy, TaskWorkStateType workState,
			String expectedNextFrom, String expectedNextTo, int expectedSequentialNumber) throws SchemaException {
		WorkBucketType newBucket = getNextBucket(segmentationStrategy, workState,
				expectedSequentialNumber);
		AbstractWorkBucketContentType content = newBucket.getContent();
		assertEquals("Wrong content class", StringIntervalWorkBucketContentType.class, content.getClass());
		StringIntervalWorkBucketContentType intervalContent = (StringIntervalWorkBucketContentType) content;
		assertEquals("Wrong next 'from'", expectedNextFrom, intervalContent.getFrom());
		assertEquals("Wrong next 'to'", expectedNextTo, intervalContent.getTo());

		workState.getBucket().add(newBucket.clone().state(WorkBucketStateType.COMPLETE));
		return newBucket;
	}

	@NotNull
	private WorkBucketType getNextBucket(WorkSegmentationStrategy segmentationStrategy, TaskWorkStateType workState,
			int expectedSequentialNumber) throws SchemaException {
		WorkSegmentationStrategy.GetBucketResult gbr = segmentationStrategy.getBucket(workState);
		display("get bucket result", gbr);
		assertTrue("Wrong answer", gbr instanceof WorkSegmentationStrategy.GetBucketResult.NewBuckets);
		WorkSegmentationStrategy.GetBucketResult.NewBuckets nbr = (WorkSegmentationStrategy.GetBucketResult.NewBuckets) gbr;
		display("new buckets obtained", nbr.newBuckets);
		assertEquals("Wrong new buckets count", 1, nbr.newBuckets.size());
		WorkBucketType newBucket = nbr.newBuckets.get(0);
		assertEquals("Wrong sequential number", expectedSequentialNumber, newBucket.getSequentialNumber());
		return newBucket;
	}

	private void assumeNoNextBucket(WorkSegmentationStrategy segmentationStrategy, TaskWorkStateType workState) throws SchemaException {
		WorkSegmentationStrategy.GetBucketResult gbr = segmentationStrategy.getBucket(workState);
		display("get bucket result", gbr);
		assertTrue("Wrong answer", gbr instanceof WorkSegmentationStrategy.GetBucketResult.NothingFound);
		WorkSegmentationStrategy.GetBucketResult.NothingFound nothingFound = (WorkSegmentationStrategy.GetBucketResult.NothingFound) gbr;
		assertEquals("Wrong definite flag", true, nothingFound.definite);
	}
}
