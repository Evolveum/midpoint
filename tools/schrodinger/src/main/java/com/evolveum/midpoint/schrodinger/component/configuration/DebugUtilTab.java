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
import com.evolveum.midpoint.schrodinger.util.Utils;

import static com.codeborne.selenide.Selenide.$;

import static com.evolveum.midpoint.schrodinger.util.Utils.setCheckFormGroupOptionCheckedById;
import static com.evolveum.midpoint.schrodinger.util.Utils.setOptionCheckedById;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DebugUtilTab extends Component<InternalsConfigurationPage> {

    public DebugUtilTab(InternalsConfigurationPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public DebugUtilTab selectDetailedDebugDump() {
        setCheckFormGroupOptionCheckedById("detailedDebugDump", true);
        return this;
    }

    public DebugUtilTab deselectDetailedDebugDump() {
        setCheckFormGroupOptionCheckedById("detailedDebugDump", false);
        return this;
    }

    public boolean isDetailedDebugDumpSelected() {
        String checked = getDetailedDebugDumpElement().getAttribute("checked");
        return checked != null && "checked".equals(checked);
    }

    private SelenideElement getDetailedDebugDumpElement() {
        return $(Schrodinger.byDataId("detailedDebugDump")).$x("//input[@data-s-id='check']")
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    public DebugUtilTab clickUpdate() {
        $(Schrodinger.byDataId("saveDebugUtil"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }
}

