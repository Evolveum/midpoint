package com.evolveum.midpoint.schrodinger.component.common.summarytagbox;

import com.codeborne.selenide.SelenideElement;

public class SummaryBoxUserType <T> extends SummaryBox<T> {
    public SummaryBoxUserType(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

}
