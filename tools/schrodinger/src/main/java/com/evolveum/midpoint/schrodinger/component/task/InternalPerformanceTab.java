/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.task;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;

/**
 * @author honchar
 */
public class InternalPerformanceTab extends Component<TaskPage> {

    public InternalPerformanceTab(TaskPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
