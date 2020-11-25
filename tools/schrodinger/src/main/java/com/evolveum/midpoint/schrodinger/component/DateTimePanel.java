/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;


/**
 * Created by honchar
 */
public class DateTimePanel<T> extends Component<T> {

    public enum AmOrPmChoice {
        AM, PM
    }

    public DateTimePanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public DateTimePanel<T> setDateTimeValue(String date, String hours, String minutes, AmOrPmChoice amOrPmChoice) {
        SelenideElement dateEle = findDate();
        dateEle.click();
        dateEle.clear();
        dateEle.setValue(date);

        SelenideElement hoursEle = findHours();
        hoursEle.doubleClick();
        hoursEle.doubleClick();
        hoursEle.sendKeys(hours);

        SelenideElement minutesEle = findMinutes();
        minutesEle.doubleClick();
        minutesEle.doubleClick();
        minutesEle.sendKeys(minutes);

        SelenideElement amOrPmChoiceEle = findAmOrPmChoice();
        amOrPmChoiceEle.click();
        amOrPmChoiceEle.selectOption(amOrPmChoice.name());

        return this;
    }

    public String date() {
        return findDate().getValue();
    }

    public String hours() {
        return findHours().getValue();
    }

    public String minutes() {
        return findMinutes().getValue();
    }

    public String amOrPmChoice() {
        return findAmOrPmChoice().getSelectedText();
    }

    public SelenideElement findDate() {
        return getParentElement().$(Schrodinger.byDataId("date")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    public SelenideElement findHours() {
        return getParentElement().$(Schrodinger.byDataId("hours")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    public SelenideElement findMinutes() {
        return getParentElement().$(Schrodinger.byDataId("minutes")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    public SelenideElement findAmOrPmChoice() {
        return getParentElement().$(Schrodinger.byDataId("amOrPmChoice")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }
}
