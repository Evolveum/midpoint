package com.evolveum.midpoint.schrodinger.component.common.summarytagbox;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;

public class SummaryBox <T> extends Component<T> {
    public SummaryBox(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
