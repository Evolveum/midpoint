/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.util.task.*;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateOverviewProgressInformationVisibilityType.HIDDEN;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateOverviewProgressInformationVisibilityType.VISIBLE;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts activity state in the tree overview.
 */
@SuppressWarnings({ "WeakerAccess", "UnusedReturnValue", "unused" })
public class ActivityStateOverviewAsserter<RA> extends AbstractAsserter<RA> {

    private final ActivityStateOverviewType information;

    ActivityStateOverviewAsserter(ActivityStateOverviewType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public ActivityStateOverviewAsserter<RA> assertComplete() {
        return assertRealizationState(ActivitySimplifiedRealizationStateType.COMPLETE);
    }

    public ActivityStateOverviewAsserter<RA> assertNotStarted() {
        return assertRealizationState(null);
    }

    public ActivityStateOverviewAsserter<RA> assertRealizationInProgress() {
        return assertRealizationState(ActivitySimplifiedRealizationStateType.IN_PROGRESS);
    }

    public ActivityStateOverviewAsserter<RA> assertRealizationState(ActivitySimplifiedRealizationStateType expected) {
        assertThat(information.getRealizationState()).as("realization state").isEqualTo(expected);
        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertSuccess() {
        return assertResultStatus(OperationResultStatusType.SUCCESS);
    }

    public ActivityStateOverviewAsserter<RA> assertFatalError() {
        return assertResultStatus(OperationResultStatusType.FATAL_ERROR);
    }

    public ActivityStateOverviewAsserter<RA> assertStatusInProgress() {
        return assertResultStatus(OperationResultStatusType.IN_PROGRESS);
    }

    public ActivityStateOverviewAsserter<RA> assertResultStatus(OperationResultStatusType expected) {
        assertThat(information.getResultStatus()).as("result status").isEqualTo(expected);
        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertProgressHidden() {
        assertThat(information.getProgressInformationVisibility()).as("progress visibility").isEqualTo(HIDDEN);
        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertProgressVisible() {
        assertThat(information.getProgressInformationVisibility()).as("progress visibility").isEqualTo(VISIBLE);
        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertNoTask() {
        assertThat(information.getTask()).as("tasks").isEmpty();
        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertSingleTask(String oid, ActivityTaskExecutionStateType state) {
        assertThat(information.getTask()).as("tasks").hasSize(1);

        ActivityTaskStateOverviewType task = information.getTask().get(0);
        assertThat(getOid(task.getTaskRef())).as("task OID").isEqualTo(oid);
        assertThat(task.getExecutionState()).as("execution state").isEqualTo(state);

        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertBucketProgress(Integer total, Integer complete) {
        BucketsProgressInformation info = BucketsProgressInformation.fromOverview(information);
        Integer totalBuckets = info != null ? info.getExpectedBuckets() : null;
        Integer completeBuckets = info != null ? info.getCompleteBuckets() : null;
        assertThat(totalBuckets).as("total buckets").isEqualTo(total);
        assertThat(completeBuckets).as("complete buckets").isEqualTo(complete);
        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertNoItemsProgress() {
        ItemsProgressInformation info = ItemsProgressInformation.fromOverview(information);
        assertThat(info).as("items progress").isNull();
        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertItemsProgress(Integer expected, int real) {
        ItemsProgressInformation info = ItemsProgressInformation.fromOverview(information);
        Integer expectedProgress = info != null ? info.getExpectedProgress() : null;
        int progress = info != null ? info.getProgress() : 0;

        assertThat(expectedProgress).as("expected progress").isEqualTo(expected);
        assertThat(progress).as("real progress").isEqualTo(real);
        return this;
    }

    public ActivityStateOverviewAsserter<ActivityStateOverviewAsserter<RA>> child(String identifier) {
        ActivityStateOverviewType childState =
                ActivityStateOverviewUtil.findOrCreateChildEntry(information, identifier, false);
        assertThat(childState)
                .withFailMessage(() -> "No child '" + identifier + "' in " + information)
                .isNotNull();
        ActivityStateOverviewAsserter<ActivityStateOverviewAsserter<RA>> asserter =
                new ActivityStateOverviewAsserter<>(childState, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityStateOverviewAsserter<RA> assertChildren(int expected) {
        assertThat(information.getActivity().size())
                .as("children #")
                .isEqualTo(expected);
        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertNoChildren() {
        return assertChildren(0);
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityStateOverviewAsserter<RA> display() {
        try {
            IntegrationTestTools.display(desc(),
                    PrismTestUtil.serializeAnyDataWrapped(information));
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertPersistenceSingleRealization() {
        return assertPersistence(ActivityStatePersistenceType.SINGLE_REALIZATION);
    }

    public ActivityStateOverviewAsserter<RA> assertPersistencePerpetual() {
        return assertPersistence(ActivityStatePersistenceType.PERPETUAL);
    }

    public ActivityStateOverviewAsserter<RA> assertPersistencePerpetualExceptStatistics() {
        return assertPersistence(ActivityStatePersistenceType.PERPETUAL_EXCEPT_STATISTICS);
    }

    public ActivityStateOverviewAsserter<RA> assertPersistence(ActivityStatePersistenceType expected) {
        assertThat(information.getPersistence()).as("persistence").isEqualTo(expected);
        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertErrors(int expected) {
        assertThat(getErrors()).as("# of errors").isEqualTo(expected);
        return this;
    }

    public ActivityStateOverviewAsserter<RA> assertNoErrors() {
        return assertErrors(0);
    }

    public int getErrors() {
        ItemsProgressInformation info = ItemsProgressInformation.fromOverview(information);
        return info != null ? info.getErrors() : 0;
    }
}
