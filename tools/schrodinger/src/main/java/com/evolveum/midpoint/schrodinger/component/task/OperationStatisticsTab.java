/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.task;

import com.codeborne.selenide.SelenideElement;
import org.testng.Assert;

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

    public OperationStatisticsTab assertSuccessfullyProcessedCountMatch(int expectedCount) {
        assertion.assertTrue(getSuccessfullyProcessed() == expectedCount, "The count of successfully processed objects doesn't match to " + expectedCount);
        return this;
    }

    public OperationStatisticsTab assertSuccessfullyProcessedIsNull() {
        assertion.assertNull(getSuccessfullyProcessed(), "The value of successfully processed objects should be null.");
        return this;
    }

    public OperationStatisticsTab assertObjectsFailedToBeProcessedCountMatch(int expectedCount) {
        assertion.assertTrue(getObjectsFailedToBeProcessed() == expectedCount, "The count of failed objects doesn't match to " + expectedCount);
        return this;
    }

    public OperationStatisticsTab assertObjectsFailedToBeProcessedIsNull() {
        assertion.assertNull(getObjectsFailedToBeProcessed(), "The value of failed objects should be null.");
        return this;
    }

    public OperationStatisticsTab assertObjectsTotalCountMatch(int expectedCount) {
        assertion.assertTrue(getObjectsTotalCount() == expectedCount, "The total count of processed objects doesn't match to " + expectedCount);
        return this;
    }

    public OperationStatisticsTab assertObjectsTotalIsNull() {
        assertion.assertNull(getObjectsTotalCount(), "The total count of processed objects should be null.");
        return null;
    }
}
