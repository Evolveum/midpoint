package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

public class SearchItemField<T> extends Component<T> {

    public SearchItemField(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public T inputValue(String input) {
        SelenideElement inputField = getParentElement().parent().$x(".//input[@" + Schrodinger.DATA_S_ID + "='input']")
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        if(!input.equals(inputField.getValue())) {
            inputField.setValue(input);
        }
        return getParent();
    }
}
