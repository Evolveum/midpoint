/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.cases;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by Kate Honchar
 */
public class WorkitemDetailsPanel<P extends BasicPage> extends Component<P> {

    public WorkitemDetailsPanel(P parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public void approveButtonClick(){
        getParentElement()
                .$(Schrodinger.byDataId("workItemApproveButton"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
    }

}
