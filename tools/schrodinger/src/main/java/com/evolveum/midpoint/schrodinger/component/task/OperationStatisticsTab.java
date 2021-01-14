/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.task;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * @author skublik
 */

public class OperationStatisticsTab extends Component<TaskPage> {

    public OperationStatisticsTab(TaskPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public Integer getSuccessfullyProcessed() {
        return getIntegerValueForTextField("objectsProcessedSuccess");
    }

    public Integer getObjectsFailedToBeProcessed() {
        return getIntegerValueForTextField("objectsProcessedFailure");
    }

    public Integer getObjectsTotalCount() {
        return getIntegerValueForTextField("objectsTotal");
    }

    private Integer getIntegerValueForTextField(String fieldName) {
        String textValue = getParentElement().$(Schrodinger.byDataId("span", fieldName)).getText();
        if (textValue == null || textValue.trim().equals("")) {
            return null;
        }
        return Integer.valueOf(textValue);
    }

    public void assertSuccessfullyProcessedCountMatch(int expectedCount) {
        if (getObjectsFailedToBeProcessed() != expectedCount) {
            throw new AssertionError("The count of successfully processed objects doesn't match to " + expectedCount);
        }
    }

    public void assertSuccessfullyProcessedIsNull() {
        if (getObjectsFailedToBeProcessed() != null) {
            throw new AssertionError("The value of successfully processed objects should be null.");
        }
    }

    public void assertObjectsFailedToBeProcessedCountMatch(int expectedCount) {
        if (getObjectsFailedToBeProcessed() != expectedCount) {
            throw new AssertionError("The count of failed objects doesn't match to " + expectedCount);
        }
    }

    public void assertObjectsFailedToBeProcessedIsNull() {
        if (getObjectsFailedToBeProcessed() != null) {
            throw new AssertionError("The value of failed objects should be null.");
        }
    }

    public void assertObjectsTotalCountMatch(int expectedCount) {
        if (getObjectsTotalCount() != expectedCount) {
            throw new AssertionError("The total count of processed objects doesn't match to " + expectedCount);
        }
    }

    public void assertObjectsTotalIsNull() {
        if (getObjectsTotalCount() != null) {
            throw new AssertionError("The total count of processed objects should be null.");
        }
    }
}
