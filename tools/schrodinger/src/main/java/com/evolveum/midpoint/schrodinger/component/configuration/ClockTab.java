/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.configuration.InternalsConfigurationPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ClockTab extends Component<InternalsConfigurationPage> {

    public enum AmOrPmChoice {
        AM, PM
    }

    public ClockTab(InternalsConfigurationPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public void changeTime(String date, String hours, String minutes, AmOrPmChoice amOrPmChoice) {
        SelenideElement dateEle = findDate();
        dateEle.click();
        dateEle.clear();
        dateEle.setValue(date);

        SelenideElement hoursEle = findHours();
        hoursEle.doubleClick();
        hoursEle.sendKeys(hours);

        SelenideElement minutesEle = findMinutes();
        minutesEle.doubleClick();
        minutesEle.sendKeys(minutes);

        SelenideElement amOrPmChoiceEle = findAmOrPmChoice();
        amOrPmChoiceEle.click();
        amOrPmChoiceEle.selectOption(amOrPmChoice.name());

        $(Schrodinger.byDataId("save")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
    }

    public void resetTime() {
        $(Schrodinger.byDataId("reset")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
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

    private SelenideElement findDate() {
        return $(Schrodinger.byDataId("date")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    private SelenideElement findHours() {
        return $(Schrodinger.byDataId("hours")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    private SelenideElement findMinutes() {
        return $(Schrodinger.byDataId("minutes")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    private SelenideElement findAmOrPmChoice() {
        return $(Schrodinger.byDataId("amOrPmChoice")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }
}

