/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

/**
 * TODO
 */
@SuppressWarnings({ "WeakerAccess", "UnusedReturnValue" })
public class ActivityStateAsserter<RA> extends AbstractAsserter<RA> {

    private final ActivityStateType activityState;

    ActivityStateAsserter(ActivityStateType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.activityState = information;
    }

    public ActivityStateAsserter<RA> assertExecutionAttempts(Integer expected) {
        assertThat(activityState.getExecutionAttempt())
                .as("execution attempts")
                .isEqualTo(expected);

        return this;
    }

    public ActivityStateAsserter<RA> assertComplete() {
        return assertRealizationState(ActivityRealizationStateType.COMPLETE);
    }

    public ActivityStateAsserter<RA> assertNotStarted() {
        return assertRealizationState(null);
    }

    public ActivityStateAsserter<RA> assertInProgressDelegated() {
        return assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_DELEGATED);
    }

    public ActivityStateAsserter<RA> assertInProgressLocal() {
        return assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_LOCAL);
    }

    public ActivityStateAsserter<RA> assertRealizationState(ActivityRealizationStateType expected) {
        assertThat(activityState.getRealizationState()).as("realization state").isEqualTo(expected);
        return this;
    }

    public ActivityStateAsserter<RA> assertSuccess() {
        return assertResultStatus(OperationResultStatusType.SUCCESS);
    }

    public ActivityStateAsserter<RA> assertFatalError() {
        return assertResultStatus(OperationResultStatusType.FATAL_ERROR);
    }

    public ActivityStateAsserter<RA> assertStatusInProgress() {
        return assertResultStatus(OperationResultStatusType.IN_PROGRESS);
    }

    public ActivityStateAsserter<RA> assertResultStatus(OperationResultStatusType expected) {
        assertThat(activityState.getResultStatus()).as("result status").isEqualTo(expected);
        return this;
    }

    public ActivityStateAsserter<RA> assertDelegationWorkStateWithTaskRef() {
        AbstractActivityWorkStateType workState = activityState.getWorkState();
        assertThat(workState).as("work state").isInstanceOf(DelegationWorkStateType.class);
        DelegationWorkStateType delegation = (DelegationWorkStateType) workState;
        assertThat(delegation.getTaskRef()).as("taskRef").isNotNull();
        return this;
    }

//    public ActivityStateAsserter<RA> assertNoTaskRef() {
//        assertThat(activityState.getTaskRef()).as("taskRef").isNull();
//        return this;
//    }

    public ExtensionAsserter<AbstractActivityWorkStateType, ActivityStateAsserter<RA>> workStateExtension() {
        ExtensionAsserter<AbstractActivityWorkStateType, ActivityStateAsserter<RA>> asserter =
                new ExtensionAsserter<>(activityState.getWorkState(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismContainerValueAsserter<AbstractActivityWorkStateType, ActivityStateAsserter<RA>> workState() {
        //noinspection unchecked
        PrismContainerValue<AbstractActivityWorkStateType> pcv = activityState.getWorkState().asPrismContainerValue();
        PrismContainerValueAsserter<AbstractActivityWorkStateType, ActivityStateAsserter<RA>> asserter =
                new PrismContainerValueAsserter<>(pcv, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityProgressAsserter<ActivityStateAsserter<RA>> progress() {
        ActivityProgressAsserter<ActivityStateAsserter<RA>> asserter =
                new ActivityProgressAsserter<>(activityState.getProgress(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityPoliciesStateAsserter<ActivityStateAsserter<RA>> activityPolicyStates() {
        ActivityPoliciesStateType state = Objects.requireNonNull(
                activityState.getPolicies(), "no policy groups");

        ActivityPoliciesStateAsserter<ActivityStateAsserter<RA>> asserter =
                new ActivityPoliciesStateAsserter<>(state, this, "activity policy group in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityCounterGroupAsserter<ActivityStateAsserter<RA>> previewModePolicyRulesCounters() {
        ActivityCounterGroupType counters = Objects.requireNonNull(
                Objects.requireNonNull(
                                activityState.getCounters(), "no counters")
                        .getPreviewModePolicyRules(),
                "no preview mode policy rules counters");

        ActivityCounterGroupAsserter<ActivityStateAsserter<RA>> asserter =
                new ActivityCounterGroupAsserter<>(counters, this, "preview rules counters in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityCounterGroupAsserter<ActivityStateAsserter<RA>> fullExecutionModePolicyRulesCounters() {
        ActivityCounterGroupType counters = Objects.requireNonNull(
                Objects.requireNonNull(
                                activityState.getCounters(), "no counters")
                        .getFullExecutionModePolicyRules(),
                "no full execution mode policy rules counters");

        ActivityCounterGroupAsserter<ActivityStateAsserter<RA>> asserter =
                new ActivityCounterGroupAsserter<>(counters, this,
                        "full execution rules counters in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityStateAsserter<RA> assertNoProgress() {
        assertThat(activityState.getProgress()).as("progress").isNull();
        return this;
    }

    public ActivityItemProcessingStatisticsAsserter<ActivityStateAsserter<RA>> itemProcessingStatistics() {
        ActivityItemProcessingStatisticsAsserter<ActivityStateAsserter<RA>> asserter =
                new ActivityItemProcessingStatisticsAsserter<>(
                        getItemProcessingStatistics(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    private @NotNull ActivityItemProcessingStatisticsType getItemProcessingStatistics() {
        if (activityState.getStatistics() == null || activityState.getStatistics().getItemProcessing() == null) {
            throw new AssertionError("No item processing statistics");
        } else {
            return activityState.getStatistics().getItemProcessing();
        }
    }

    public SynchronizationInfoAsserter<ActivityStateAsserter<RA>> synchronizationStatistics() {
        SynchronizationInfoAsserter<ActivityStateAsserter<RA>> asserter =
                new SynchronizationInfoAsserter<>(
                        getSynchronizationStatistics(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    private @NotNull ActivitySynchronizationStatisticsType getSynchronizationStatistics() {
        if (activityState.getStatistics() == null || activityState.getStatistics().getSynchronization() == null) {
            throw new AssertionError("No synchronization statistics present");
        } else {
            return activityState.getStatistics().getSynchronization();
        }
    }

    public ActivityStateAsserter<RA> assertNoSynchronizationStatistics() {
        if (activityState.getStatistics() != null && activityState.getStatistics().getSynchronization() != null) {
            fail("Synchronization statistics present even if it should not");
        }
        return this;
    }

    public ActionsExecutedInfoAsserter<ActivityStateAsserter<RA>> actionsExecuted() {
        ActionsExecutedInfoAsserter<ActivityStateAsserter<RA>> asserter =
                new ActionsExecutedInfoAsserter<>(
                        getActionsExecuted(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    private @NotNull ActivityActionsExecutedType getActionsExecuted() {
        if (activityState.getStatistics() == null || activityState.getStatistics().getActionsExecuted() == null) {
            throw new AssertionError("No actions executed information present");
        } else {
            return activityState.getStatistics().getActionsExecuted();
        }
    }

    public ActivityStateAsserter<RA> assertNoActionsExecutedInformation() {
        if (activityState.getStatistics() != null && activityState.getStatistics().getActionsExecuted() != null) {
            fail("Actions executed information present even if it should not");
        }
        return this;
    }

    public ActivityBucketManagementStatisticsAsserter<ActivityStateAsserter<RA>> bucketManagementStatistics() {
        ActivityBucketManagementStatisticsAsserter<ActivityStateAsserter<RA>> asserter =
                new ActivityBucketManagementStatisticsAsserter<>(
                        getBucketManagementStatistics(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityStateAsserter<RA> assertBucketManagementStatisticsOperations(int expected) {
        assertThat(getBucketManagementStatistics().getOperation().size())
                .as("bucket mgmt operations #")
                .isEqualTo(expected);
        return this;
    }

    private ActivityBucketManagementStatisticsType getBucketManagementStatistics() {
        if (activityState.getStatistics() == null || activityState.getStatistics().getBucketManagement() == null) {
            throw new AssertionError("No bucket management statistics");
        } else {
            return activityState.getStatistics().getBucketManagement();
        }
    }

    public ActivityStateAsserter<ActivityStateAsserter<RA>> child(String identifier) {
        ActivityStateType childState = ActivityStateUtil.findChildActivityStateRequired(activityState, identifier);
        ActivityStateAsserter<ActivityStateAsserter<RA>> asserter =
                new ActivityStateAsserter<>(childState, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityStateAsserter<RA> assertChildren(int expected) {
        assertThat(activityState.getActivity().size())
                .as("children #")
                .isEqualTo(expected);
        return this;
    }

    public ActivityStateAsserter<RA> assertNoChildren() {
        return assertChildren(0);
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityStateAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(activityState));
        return this;
    }

    public ActivityStateAsserter<RA> assertPersistenceSingleRealization() {
        return assertPersistence(ActivityStatePersistenceType.SINGLE_REALIZATION);
    }

    public ActivityStateAsserter<RA> assertPersistencePerpetual() {
        return assertPersistence(ActivityStatePersistenceType.PERPETUAL);
    }

    public ActivityStateAsserter<RA> assertPersistencePerpetualExceptStatistics() {
        return assertPersistence(ActivityStatePersistenceType.PERPETUAL_EXCEPT_STATISTICS);
    }

    public ActivityStateAsserter<RA> assertPersistence(ActivityStatePersistenceType expected) {
        assertThat(activityState.getPersistence()).as("persistence").isEqualTo(expected);
        return this;
    }

    public ActivityStateAsserter<RA> assertScavenger(boolean value) {
        assertThat(isScavenger()).as("is scavenger").isEqualTo(value);
        return this;
    }

    private boolean isScavenger() {
        return Boolean.TRUE.equals(activityState.getBucketing().isScavenger());
    }

    public ActivityStateAsserter<RA> assertNoReadyBuckets() {
        assertThat(getReadyBuckets()).as("allocated buckets").isEqualTo(0);
        return this;
    }

    private int getReadyBuckets() {
        return (int) BucketingUtil.getBuckets(activityState).stream()
                .filter(b -> b.getState() == WorkBucketStateType.READY)
                .count();
    }
}
