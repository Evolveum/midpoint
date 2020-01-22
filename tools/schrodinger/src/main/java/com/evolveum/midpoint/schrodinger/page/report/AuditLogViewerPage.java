/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.report;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.report.AuditRecordTable;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 * @author lskublik
 */
public class AuditLogViewerPage extends BasicPage {

    public AuditRecordTable table() {
        SelenideElement box = $(By.cssSelector(".box.boxed-table"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new AuditRecordTable(this, box);
    }
}
