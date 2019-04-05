package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by matus on 5/2/2018.
 */
public class DropDown<T> extends Component<T> {
    public DropDown(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public DropDown<T> selectOption(String option){
        getParentElement()
                .$(Schrodinger.byElementAttributeValue("option", "value", option))
                .click();

        getParentElement()
                .$(Schrodinger.byElementAttributeValue("option", "value", option))
                .waitUntil(Condition.selected, MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }
}
