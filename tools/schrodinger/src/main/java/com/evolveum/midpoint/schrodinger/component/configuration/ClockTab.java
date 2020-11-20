/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.DateTimePanel;
import com.evolveum.midpoint.schrodinger.page.configuration.InternalsConfigurationPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ClockTab extends Component<InternalsConfigurationPage> {

    public ClockTab(InternalsConfigurationPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public void changeTime(String date, String hours, String minutes, DateTimePanel.AmOrPmChoice amOrPmChoice) {
        DateTimePanel<ClockTab> dateTimePanel = getOffsetPanel();
        dateTimePanel.setDateTimeValue(date, hours, minutes, amOrPmChoice);
        $(Schrodinger.byDataId("save")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
    }

    public DateTimePanel<ClockTab> getOffsetPanel() {
        return new DateTimePanel<>(this,
                $(Schrodinger.byDataId("offset")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }

    public void resetTime() {
        $(Schrodinger.byDataId("reset")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
    }

}

