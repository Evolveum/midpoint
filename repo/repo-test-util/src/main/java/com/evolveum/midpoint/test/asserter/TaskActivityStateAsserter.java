/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTreeRealizationStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskActivityStateType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * TODO
 */
@SuppressWarnings("WeakerAccess")
public class TaskActivityStateAsserter<RA> extends AbstractAsserter<RA> {

    private final TaskActivityStateType activityState;

    TaskActivityStateAsserter(TaskActivityStateType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.activityState = information;
    }

    public TaskActivityStateAsserter<RA> assertTreeRealizationComplete() {
        return assertTreeRealizationState(ActivityTreeRealizationStateType.COMPLETE);
    }

    public TaskActivityStateAsserter<RA> assertTreeRealizationInProgress() {
        return assertTreeRealizationState(ActivityTreeRealizationStateType.IN_PROGRESS);
    }

    private @NotNull TaskActivityStateAsserter<RA> assertTreeRealizationState(ActivityTreeRealizationStateType expected) {
        assertThat(activityState.getTree().getRealizationState())
                .as("tree realization state")
                .isEqualTo(expected);
        return this;
    }

    public TaskActivityStateAsserter<RA> assertRole(TaskRoleType expected) {
        assertThat(activityState.getTaskRole()).as("role").isEqualTo(expected);
        return this;
    }

    public TaskActivityStateAsserter<RA> assertLocalRoot(ActivityPath expected) {
        assertThat(activityState.getLocalRoot()).as("local root").isEqualTo(expected.toBean());
        return this;
    }

    public ActivityStateAsserter<TaskActivityStateAsserter<RA>> rootActivity() {
        ActivityStateAsserter<TaskActivityStateAsserter<RA>> asserter =
                new ActivityStateAsserter<>(activityState.getActivity(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityStateOverviewAsserter<TaskActivityStateAsserter<RA>> rootActivityOverview() {
        var overview =
                Objects.requireNonNull(
                        Objects.requireNonNull(activityState.getTree(), "no tree")
                                .getActivity(), "no root activity overview");

        ActivityStateOverviewAsserter<TaskActivityStateAsserter<RA>> asserter =
                new ActivityStateOverviewAsserter<>(overview, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public TaskActivityStateAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(activityState));
        return this;
    }
}
