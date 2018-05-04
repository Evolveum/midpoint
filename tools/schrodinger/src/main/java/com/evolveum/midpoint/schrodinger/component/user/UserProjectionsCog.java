package com.evolveum.midpoint.schrodinger.component.user;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.FocusSetProjectionModal;
import com.evolveum.midpoint.schrodinger.component.common.CogDropDown;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/2/2018.
 */
public class UserProjectionsCog<T> extends CogDropDown<T> {

    public UserProjectionsCog(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public T enable() {
        $(Schrodinger.byDataResourceKey("pageAdminFocus.button.enable"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }

    public T disable() {
        $(Schrodinger.byDataResourceKey("pageAdminFocus.button.disable"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }

    public T unlink() {
        $(Schrodinger.byDataResourceKey("pageAdminFocus.button.unlink"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }

    public T unlock() {
        $(Schrodinger.byDataResourceKey("pageAdminFocus.button.unlock"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this.getParent();
    }

    public FocusSetProjectionModal<T> addProjection() {

        $(Schrodinger.byElementEnclosedTextValue("a", "data-s-id", "menuItemLink", "\n" +
                "        Add projection")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement actualModal = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Choose object"));

        return new FocusSetProjectionModal<>(this.getParent(), actualModal);
    }
}
