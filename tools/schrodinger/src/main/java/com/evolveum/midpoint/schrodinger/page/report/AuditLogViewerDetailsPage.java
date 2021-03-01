package com.evolveum.midpoint.schrodinger.page.report;

import com.codeborne.selenide.Condition;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.prism.show.ScenePanel;
import com.evolveum.midpoint.schrodinger.page.BasicPage;

import org.testng.Assert;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

public class AuditLogViewerDetailsPage extends BasicPage {

    public ScenePanel<AuditLogViewerDetailsPage> deltaPanel() {
        SelenideElement el = $x(".//div[@data-s-id='deltaPanel']").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new ScenePanel<>(this, el);
    }

    public AuditLogViewerDetailsPage assertAuditLogViewerDetailsPageIsOpened() {
        assertion.assertTrue($(byText("Audit Log Details")).exists());
        return this;
    }


}
