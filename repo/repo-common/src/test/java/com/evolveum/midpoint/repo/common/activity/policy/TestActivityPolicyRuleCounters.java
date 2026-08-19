/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.common.AbstractRepoCommonTest;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyThresholdType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WaterMarkType;

/**
 * Tests the counters carried by {@link ActivityPolicyRule} and its evaluated wrappers.
 *
 * These tests are "static": nothing is run here, the rule objects are created and driven directly.
 *
 * The point of interest is the *scope* of a counter. A single {@link ActivityPolicyRule} is created once per activity
 * run (it is held by {@code ActivityPolicyRulesContext}, a field of {@code AbstractActivityRun}) and is therefore
 * shared by all worker threads of that activity. The count a threshold is evaluated against, on the other hand, belongs
 * to a single evaluation - i.e. to the item being processed. {@link EvaluatedActivityPolicyRuleImpl} is created per
 * evaluation cycle and is the natural owner of that count.
 *
 * @see PolicyRuleCounterUpdater
 */
@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@DirtiesContext
public class TestActivityPolicyRuleCounters extends AbstractRepoCommonTest {

    private static final int THRESHOLD = 5;

    /**
     * A rule with a threshold, so that {@code isOverThreshold()} is driven by the count. The identifier of an activity
     * policy rule is derived from the container id of its bean, hence the explicit id here (normally it is assigned by
     * prism when the rule is stored in the activity definition).
     */
    private ActivityPolicyRule createSharedRule() {
        PolicyRuleType bean = new PolicyRuleType()
                .id(1L)
                .name("counter-scope")
                .policyConstraints(new PolicyConstraintsType())
                .policyThreshold(new PolicyThresholdType()
                        .lowWaterMark(new WaterMarkType().count(THRESHOLD)));

        return new ActivityPolicyRuleBuilder(bean, ActivityPath.empty(), ConfigurationItemOrigin.generated())
                .build();
    }

    /**
     * Two evaluations of the same rule must not see each other's counts.
     *
     * This is what happens when several worker threads of one activity process items concurrently: each of them
     * evaluates the same rule, but each has its own count - the value its own increment returned. If the count is
     * stored in the shared {@link ActivityPolicyRule}, the last writer wins, and an evaluation ends up evaluating the
     * threshold against a count that belongs to another item.
     */
    @Test
    public void test100CountIsPerEvaluationNotShared() {
        given("a single (shared) activity policy rule, as created once per activity run");
        ActivityPolicyRule shared = createSharedRule();

        when("two evaluations of that rule are given their own counts");
        EvaluatedActivityPolicyRuleImpl first = new EvaluatedActivityPolicyRuleImpl(shared);
        EvaluatedActivityPolicyRuleImpl second = new EvaluatedActivityPolicyRuleImpl(shared);

        first.setCount(THRESHOLD - 1, THRESHOLD - 1);   // below the threshold
        second.setCount(THRESHOLD, THRESHOLD);          // reaches the threshold

        then("each evaluation keeps the count it was given");
        assertThat(first.getCount()).as("count of the first evaluation").isEqualTo(THRESHOLD - 1);
        assertThat(second.getCount()).as("count of the second evaluation").isEqualTo(THRESHOLD);

        and("only the evaluation that reached the threshold is over it");
        assertThat(first.isOverThreshold()).as("first evaluation is over threshold").isFalse();
        assertThat(second.isOverThreshold()).as("second evaluation is over threshold").isTrue();
    }

    /**
     * The reverse ordering of the same scenario: an evaluation that reached the threshold must stay over it even when
     * another evaluation (of the same rule) reports a lower count afterwards.
     *
     * This is the ordering that lets an over-threshold item slip through: the item that should be refused is evaluated
     * after some other thread has written a lower count into the shared rule.
     */
    @Test
    public void test110LowerCountFromOtherEvaluationDoesNotHideThreshold() {
        given("a single (shared) activity policy rule");
        ActivityPolicyRule shared = createSharedRule();

        when("an evaluation reaches the threshold, and another one then reports a lower count");
        EvaluatedActivityPolicyRuleImpl overThreshold = new EvaluatedActivityPolicyRuleImpl(shared);
        overThreshold.setCount(THRESHOLD, THRESHOLD);

        EvaluatedActivityPolicyRuleImpl belowThreshold = new EvaluatedActivityPolicyRuleImpl(shared);
        belowThreshold.setCount(THRESHOLD - 2, THRESHOLD - 2);

        then("the evaluation that reached the threshold is still over it");
        assertThat(overThreshold.getCount()).as("count of the over-threshold evaluation").isEqualTo(THRESHOLD);
        assertThat(overThreshold.isOverThreshold()).as("over-threshold evaluation is over threshold").isTrue();
    }
}
