/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TODO
 */
@SuppressWarnings("WeakerAccess")
public class ActivityStateAsserter<RA> extends AbstractAsserter<RA> {

    private final ActivityStateType activityState;

    ActivityStateAsserter(ActivityStateType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.activityState = information;
    }

    public ActivityStateAsserter<RA> assertComplete() {
        return assertRealizationState(ActivityRealizationStateType.COMPLETE);
    }

    public ActivityStateAsserter<RA> assertRealizationState(ActivityRealizationStateType expected) {
        assertThat(activityState.getRealizationState()).as("realization state").isEqualTo(expected);
        return this;
    }

    public ActivityStateAsserter<RA> assertSuccess() {
        return assertResultStatus(OperationResultStatusType.SUCCESS);
    }

    public ActivityStateAsserter<RA> assertResultStatus(OperationResultStatusType expected) {
        assertThat(activityState.getResultStatus()).as("result status").isEqualTo(expected);
        return this;
    }

    public ActivityStateAsserter<RA> assertHasTaskRef() {
        assertThat(activityState.getTaskRef()).as("taskRef").isNotNull();
        assertThat(activityState.getTaskRef().getOid()).as("taskRef.oid").isNotNull();
        return this;
    }

    public ActivityStateAsserter<RA> assertNoTaskRef() {
        assertThat(activityState.getTaskRef()).as("taskRef").isNull();
        return this;
    }

    public ExtensionAsserter<AbstractActivityWorkStateType, ActivityStateAsserter<RA>> workStateExtension() {
        ExtensionAsserter<AbstractActivityWorkStateType, ActivityStateAsserter<RA>> asserter =
                new ExtensionAsserter<>(activityState.getWorkState(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityStateAsserter<ActivityStateAsserter<RA>> activity(String identifier) {
        ActivityStateType childState = ActivityStateUtil.findActivityStateRequired(activityState, identifier);
        ActivityStateAsserter<ActivityStateAsserter<RA>> asserter =
                new ActivityStateAsserter<>(childState, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityStateAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(activityState));
        return this;
    }
}
