package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.component.common.ModalBox;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/11/2018.
 */
public class FocusSetAssignmentsModal<T> extends ModalBox<T> {
    public FocusSetAssignmentsModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public FocusSetAssignmentsModal<T> selectType(String option) {
        $(By.name("mainPopup:content:popupBody:type:input"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).selectOption(option);

        return this;
    }

    public FocusSetAssignmentsModal<T> selectKind(String option) {
        $(By.name("mainPopup:content:popupBody:kindContainer:kind:input"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).selectOption(option);

        return this;
    }

    public FocusSetAssignmentsModal<T> selectIntent(String option) {
        $(By.name("mainPopup:content:popupBody:intentContainer:intent:input"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).selectOption(option);

        return this;
    }

    public FocusTableWithChoosableElements<FocusSetAssignmentsModal<T>> table() {
        SelenideElement resourcesBox = $(By.cssSelector(".box.boxed-table"));

        return new FocusTableWithChoosableElements<>(this, resourcesBox);
    }

    public T clickAdd() {

        $(Schrodinger.byDataResourceKey("userBrowserDialog.button.addButton"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }

}
