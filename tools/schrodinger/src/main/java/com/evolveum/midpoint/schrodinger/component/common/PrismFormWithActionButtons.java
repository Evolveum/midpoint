package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/17/2018.
 */
public class PrismFormWithActionButtons<T> extends PrismForm<T> {
    public PrismFormWithActionButtons(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public T clickDone() {

        $(Schrodinger.byDataResourceKey("div", "AssignmentPanel.doneButton"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }

    public T clickCancel() {

        $(Schrodinger.byDataResourceKey("div", "AssignmentPanel.cancelButton"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }
}
