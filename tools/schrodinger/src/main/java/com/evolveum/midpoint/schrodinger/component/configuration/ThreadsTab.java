/*
 * Copyright (c) 2010-2020 Evolveum and contributors
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

public class ThreadsTab extends Component<InternalsConfigurationPage> {

    public ThreadsTab(InternalsConfigurationPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ThreadsTab showAllThreads() {
        $(Schrodinger.byDataId("showAllThreads")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    public ThreadsTab showTasksThreads() {
        $(Schrodinger.byDataId("showTasksThreads")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }
    public ThreadsTab recordTasksThreads() {
        $(Schrodinger.byDataId("recordTasksThreads")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }
    public ThreadsTab stopAllThreads() {
        $(Schrodinger.byDataId("deactivateServiceThreads")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }
    public ThreadsTab startAllThreads() {
        $(Schrodinger.byDataId("reactivateServiceThreads")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }
}
