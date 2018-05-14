package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/9/2018.
 */
public class ConfirmationModal<T> extends ModalBox<T> {
    public ConfirmationModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public T clickYes() {
        $(Schrodinger.byDataResourceKey("a", "confirmationDialog.yes"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();


        return this.getParent();
    }

    public T clickNo() {

        $(Schrodinger.byDataResourceKey("a", "confirmationDialog.no"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }

    public T close() {
        $(By.className("w_close"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }

}
