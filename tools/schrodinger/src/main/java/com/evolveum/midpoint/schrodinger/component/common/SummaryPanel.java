/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.summarytagbox.SummaryBox;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 3/21/2018.
 */
public class SummaryPanel<T> extends Component<T> {
    public SummaryPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public String fetchDisplayName() {

        return $(Schrodinger.byDataId("summaryDisplayName")).getText();

    }

    public SummaryBox<SummaryPanel<T>> summaryBox() {

        SelenideElement summaryBox = $(Schrodinger.byDataId("summaryTagBox")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new SummaryBox<>(this, summaryBox);

    }

}
