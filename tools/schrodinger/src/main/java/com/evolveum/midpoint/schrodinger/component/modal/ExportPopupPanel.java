package com.evolveum.midpoint.schrodinger.component.modal;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

public class ExportPopupPanel<T> extends ModalBox<T> {

    public ExportPopupPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public Table<ExportPopupPanel<T>> table() {
        return new Table<>(this,
                getParentElement().$(Schrodinger.bySelfOrDescendantElementAttributeValue("div", "data-s-id", "table",
                        "style", "float: left; padding-bottom: 5px;"))
                        .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }

    public ExportPopupPanel<T> setReportName(String reportName) {
        getParentElement().$x("//div[@data-s-id='name']").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .setValue(reportName);
        return this;
    }

    public T exportSelectedColumns() {
        $(Schrodinger.byDataId("export")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return getParent();
    }

    public T cancel() {
        $(Schrodinger.byDataId("cancelButton")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return getParent();
    }

}
