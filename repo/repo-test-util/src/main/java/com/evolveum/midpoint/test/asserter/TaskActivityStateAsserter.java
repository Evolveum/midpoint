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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityExecutionRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskActivityStateType;

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

    public TaskActivityStateAsserter<RA> assertAllWorkComplete() {
        assertThat(activityState.isAllWorkComplete()).as("allWorkComplete").isTrue();
        return this;
    }

    public TaskActivityStateAsserter<RA> assertNotAllWorkComplete() {
        assertThat(activityState.isAllWorkComplete()).as("allWorkComplete").isNotEqualTo(Boolean.TRUE);
        return this;
    }

    public TaskActivityStateAsserter<RA> assertRole(ActivityExecutionRoleType expected) {
        assertThat(activityState.getRole()).as("role").isEqualTo(expected);
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

    @Override
    protected String desc() {
        return getDetails();
    }

    public TaskActivityStateAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(activityState));
        return this;
    }
}
