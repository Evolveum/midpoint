/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/25/2018.
 */
public class ResourceShadowTable<T> extends TableWithPageRedirect<T> {
    public ResourceShadowTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public <E extends BasicPage> E clickByName(String name) {
        return null;
    }

    @Override
    public ResourceShadowTable<T> selectCheckboxByName(String name) {

        $(Schrodinger.byAncestorFollowingSiblingDescendantOrSelfElementEnclosedValue("input", "type", "checkbox", "data-s-id", "3", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    public ResourceShadowTableCog<ResourceShadowTable<T>> clickCog() {

        $(Schrodinger.byElementAttributeValue("button", "data-toggle", "dropdown"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement cog = $(Schrodinger.byElementAttributeValue("ul","role","menu"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ResourceShadowTableCog<>(this, cog);
    }

}
