/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.cases;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.cases.CasePage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;
import org.testng.Assert;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate Honchar
 */
public class OperationRequestTab extends Component<CasePage> {

    public OperationRequestTab(CasePage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public boolean changesAreApplied(){
        return $(Schrodinger.byDataId("operationRequestCasePanel"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .$(By.className("box-success"))
                .$(byText("Changes applied (successfully or not)"))
                .is(Condition.visible);
    }

    public boolean changesAreRejected(){
        return $(Schrodinger.byDataId("operationRequestCasePanel"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .$(By.className("box-danger"))
                .$(byText("Changes rejected"))
                .is(Condition.visible);
    }

    public OperationRequestTab assertChangesAreApplied() {
        Assert.assertTrue(changesAreApplied(), "Changes are not applied");
        return this;
    }

    public OperationRequestTab assertChangesAreRejected() {
        Assert.assertTrue(changesAreRejected(), "Changes are not rejected");
        return this;
    }
}
