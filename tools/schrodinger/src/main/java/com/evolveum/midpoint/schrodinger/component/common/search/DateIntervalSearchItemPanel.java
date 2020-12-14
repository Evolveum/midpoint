/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common.search;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.DateTimePanel;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public class DateIntervalSearchItemPanel<T> extends Component<T> {

    public DateIntervalSearchItemPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public DateTimePanel<DateIntervalSearchItemPanel<T>> getFromDateTimeFieldPanel() {
        return new DateTimePanel<>(this, getPopupPanel().$(Schrodinger.byDataId("dateFromValue")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }

    public DateTimePanel<DateIntervalSearchItemPanel<T>> getToDateTimeFieldPanel() {
        return new DateTimePanel<>(this, getPopupPanel().$(Schrodinger.byDataId("dateToValue")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }

    public T confirm() {
        getParentElement().$x(".//a[@data-s-id='confirmButton']").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return getParent();
    }

    private SelenideElement getPopupPanel() {
        getParentElement().$x(".//a[@" + Schrodinger.DATA_S_ID + "='editButton']")
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return getParentElement().$x(".//div[@" + Schrodinger.DATA_S_ID + "='popoverPanel']")
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

}
