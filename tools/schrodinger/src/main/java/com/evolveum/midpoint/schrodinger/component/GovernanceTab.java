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
import com.evolveum.midpoint.schrodinger.component.org.MemberPanel;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * @author honchar
 */
public class GovernanceTab <T> extends Component<T> {

    public GovernanceTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public MemberPanel<GovernanceTab<T>> membersPanel() {
        return new MemberPanel<>(this, $(Schrodinger.byDataId("form")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }

}
