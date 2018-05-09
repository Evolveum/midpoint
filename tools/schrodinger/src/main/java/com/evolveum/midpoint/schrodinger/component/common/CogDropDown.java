package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;

/**
 * Created by matus on 5/2/2018.
 */
public class CogDropDown<T> extends Component<T> {
    public CogDropDown(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
