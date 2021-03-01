/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.task;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;

import org.openqa.selenium.By;
import org.testng.Assert;

/**
 * @author lskublik
 */
public class TaskBasicTab extends AssignmentHolderBasicTab<TaskPage> {
    public TaskBasicTab(TaskPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public String utility() {
        return form().findProperty("category").$(By.tagName("input")).getValue();
    }

    public TaskBasicTab assertUtilityValueEquals(String expectedValue) {
        assertion.assertEquals(expectedValue, form().findProperty("category").$(By.tagName("input")).getValue(), "Utility value doesn't match");
        return this;
    }
}
