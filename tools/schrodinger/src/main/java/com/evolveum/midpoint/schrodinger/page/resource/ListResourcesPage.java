/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.resource.ResourcesPageTable;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListResourcesPage extends BasicPage {

    public ResourcesPageTable<ListResourcesPage> table() {
        SelenideElement table = $(By.cssSelector(".box.boxed-table.object-resource-box")).waitUntil(Condition.exist, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ResourcesPageTable<>(this, table);
    }

    public ListResourcesPage testConnectionClick(String resourceName){
        table()
                .search()
                .byName()
                .inputValue(resourceName)
                .updateSearch();

        SelenideElement testConnectionIcon = $(Schrodinger
                .byElementAttributeValue("i", "class","fa fa-question fa-fw")).waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        testConnectionIcon.click();

        return this;

    }
}
