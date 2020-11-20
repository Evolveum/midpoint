package com.evolveum.midpoint.schrodinger.component.self;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

public class ChangePasswordPanel<T> extends Component<T> {

    public ChangePasswordPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ChangePasswordPanel<T> setOldPasswordValue(String value) {
        getParentElement().$(Schrodinger.byDataId("oldPassword"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .setValue(value);
        return this;
    }

    public ChangePasswordPanel<T> setNewPasswordValue(String value) {
        getParentElement().$(Schrodinger.byDataId("password1"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .setValue(value);
        return this;
    }

    public ChangePasswordPanel<T> setRepeatPasswordValue(String value) {
        getParentElement().$(Schrodinger.byDataId("password2"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .setValue(value);
        return this;
    }

    public ChangePasswordPanel<T> expandPasswordPropagationPanel() {
        SelenideElement contentPanelElement = $(Schrodinger.byElementAttributeValue("div", "class", "box-body"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        String displayValue = contentPanelElement.getAttribute("style");
        if (displayValue != null && displayValue.contains("display: none;")) {
            $(Schrodinger.byElementAttributeValue("button", "data-widget", "collapse"))
                    .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
            contentPanelElement.waitUntil(Condition.attribute("style", null), MidPoint.TIMEOUT_DEFAULT_2_S);
        }
        return this;
    }

    public Table<ChangePasswordPanel> accountsTable() {
        return new Table<>(this, $(Schrodinger.byDataId("accounts")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }
}
