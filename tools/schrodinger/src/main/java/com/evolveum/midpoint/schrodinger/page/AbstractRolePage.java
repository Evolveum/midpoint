/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.InducementsTab;

/**
 * @author skublik
 */

public class AbstractRolePage<A extends AbstractRolePage> extends FocusPage<A> {

    public InducementsTab<A> selectTabInducements() {
        SelenideElement element = getTabPanel().clickTab("FocusType.inducement");

        return new InducementsTab<A>((A) this, element);
    }
}
