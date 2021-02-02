package com.evolveum.midpoint.schrodinger.page.report;

import com.codeborne.selenide.Condition;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.prism.show.ScenePanel;
import com.evolveum.midpoint.schrodinger.page.BasicPage;

import static com.codeborne.selenide.Selenide.$x;

public class AuditLogViewerDetailsPage extends BasicPage {

    public ScenePanel<AuditLogViewerDetailsPage> deltaListPanel() {
        SelenideElement el = $x(".//div[@data-s-id='deltaListPanel']").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new ScenePanel<>(this, el);
    }

}
