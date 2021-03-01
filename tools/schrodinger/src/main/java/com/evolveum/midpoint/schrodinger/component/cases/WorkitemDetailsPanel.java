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
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.modal.ForwardWorkitemModal;
import com.evolveum.midpoint.schrodinger.component.modal.ObjectBrowserModal;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.schrodinger.util.Utils;

import org.openqa.selenium.By;
import org.testng.Assert;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

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

    public void rejectButtonClick(){
        getParentElement()
                .$(Schrodinger.byDataId("workItemRejectButton"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
    }

    public ForwardWorkitemModal forwardButtonClick(){
        getParentElement()
                .$(Schrodinger.byDataId("workItemForwardButton"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        ForwardWorkitemModal<P> forwardWorkitemModal = new ForwardWorkitemModal<P>(getParent(), Utils.getModalWindowSelenideElement());
        return forwardWorkitemModal;
    }

    public ConfirmationModal<P> forwardOperationUserSelectionPerformed(){
        return new ConfirmationModal<P>(getParent(), Utils.getModalWindowSelenideElement());
    }

    public void claimButtonClick(){
        getParentElement()
                .$(Schrodinger.byDataId("workItemClaimButton"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
    }

    public WorkitemDetailsPanel<P> assertApproverElementValueMatches(String approver){
        assertion.assertTrue(getParentElement()
                .$(Schrodinger.byDataId("approver"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .$(byText(approver))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .is(Condition.visible));
        return this;
    }
}
