/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.task;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * @author skublik
 */

public class OperationStatisticsTab<T> extends Component<T> {

    public OperationStatisticsTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public int getSuccessfullyProcessed() {
        return Integer.valueOf(getParentElement().$(Schrodinger.byDataId("span", "objectsProcessedSuccess")).getText());
    }
}
