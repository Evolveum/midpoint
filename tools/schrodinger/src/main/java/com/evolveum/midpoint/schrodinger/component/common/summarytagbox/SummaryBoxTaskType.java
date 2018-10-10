package com.evolveum.midpoint.schrodinger.component.common.summarytagbox;

import com.codeborne.selenide.SelenideElement;

public class SummaryBoxTaskType <T> extends SummaryBox<T> {
    public SummaryBoxTaskType(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
