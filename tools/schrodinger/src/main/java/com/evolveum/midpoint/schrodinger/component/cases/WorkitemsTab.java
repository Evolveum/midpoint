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
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithComponentRedirect;
import com.evolveum.midpoint.schrodinger.page.cases.CasePage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate Honchar
 */
public class WorkitemsTab extends Component<CasePage> {

    public WorkitemsTab(CasePage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public TableWithComponentRedirect<WorkitemsTab, WorkitemDetailsPanel> table() {
        SelenideElement tableBox = $(By.cssSelector(".box.boxed-table")).waitUntil(Condition.exist, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new TableWithComponentRedirect<WorkitemsTab, WorkitemDetailsPanel>(this, tableBox) {
            @Override
            public WorkitemDetailsPanel<CasePage> clickByName(String name) {
                    getParentElement()
                            .$(Schrodinger.byDataId("tableContainer"))
                            .$(By.partialLinkText(name))
                            .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                SelenideElement detailsPanel = $(Schrodinger.byDataId("div", "details"))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

                return new WorkitemDetailsPanel<CasePage>(WorkitemsTab.this.getParent(), detailsPanel);
            }

            @Override
            public TableWithComponentRedirect<WorkitemsTab, WorkitemDetailsPanel> selectCheckboxByName(String name) {
                //do nothing as there is no checkbox column in the table
                return this;
            }
        };
    }
}
