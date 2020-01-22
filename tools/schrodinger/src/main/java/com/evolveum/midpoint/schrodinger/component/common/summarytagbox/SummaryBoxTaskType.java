/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common.summarytagbox;

import com.codeborne.selenide.SelenideElement;

public class SummaryBoxTaskType <T> extends SummaryBox<T> {
    public SummaryBoxTaskType(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
