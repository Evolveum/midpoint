/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class TaskInformationAsserter<RA> extends AbstractAsserter<RA> {

    private final TaskInformation information;

    private TaskInformationAsserter(TaskInformation information) {
        this.information = information;
    }

    private TaskInformationAsserter(TaskInformation information, String details) {
        super(details);
        this.information = information;
    }

    public TaskInformationAsserter(TaskInformation information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public static @NotNull TaskInformationAsserter<Void> forInformation(@NotNull TaskInformation information) {
        return new TaskInformationAsserter<>(information);
    }

    public static @NotNull TaskInformationAsserter<Void> forInformation(@NotNull TaskType task) {
        TaskInformation info = TaskInformation.createForTask(task, null);
        return forInformation(info);
    }

    @Override
    protected String desc() {
        if (information == null) {
            return "null TaskInformation";
        }
        return information.toString();
    }

    public TaskInformationAsserter<RA> assertTaskHealthDescriptionCount(int count) {
        int actualCount = information.getTaskHealthUserFriendlyMessages().size();
        Assertions.assertThat(actualCount)
                .as("Task health description localization messages count in %s", desc())
                .isEqualTo(count);
        return this;
    }

    public TaskInformationAsserter<RA> assertTaskHealthDescriptionDefaultMessages(String... defaultMessages) {
        List<String> msgs = information.getTaskHealthUserFriendlyMessages().stream()
                .map(m -> m.getFallbackMessage())
                .toList();

        Assertions.assertThat(msgs).containsExactlyInAnyOrder(defaultMessages);

        return this;
    }
}
