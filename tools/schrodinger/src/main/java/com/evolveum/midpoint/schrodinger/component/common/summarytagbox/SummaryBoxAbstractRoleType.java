package com.evolveum.midpoint.schrodinger.component.common.summarytagbox;

import com.codeborne.selenide.SelenideElement;

public class SummaryBoxAbstractRoleType <T> extends SummaryBox<T> {
    public SummaryBoxAbstractRoleType(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
