package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;

/**
 * Created by matus on 5/2/2018.
 */
public class DropDown<T> extends Component<T> {
    public DropDown(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
