/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.TabWithContainerWrapper;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar.
 */
public class SchemaStepSchemaTab extends TabWithContainerWrapper<SchemaWizardStep> {

    public SchemaStepSchemaTab(SchemaWizardStep parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public SchemaStepSchemaTab clickObjectClass(String objectClassName) {
        $(Schrodinger.bySelfOrDescendantElementAttributeValue("div", "class", "box box-solid box-primary",
                "class", "box-title"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .$(By.linkText(objectClassName))
                .click();
        $(Schrodinger.byDataId("objectClassInfoContainer")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    public boolean isObjectClassPresent(String objectClassName) {
        return $(Schrodinger.bySelfOrDescendantElementAttributeValue("div", "class", "box box-solid box-primary",
                "class", "box-title"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .$(By.linkText(objectClassName))
                .exists();
    }

    public Table<SchemaStepSchemaTab> getAttributesTable() {
        return new Table<SchemaStepSchemaTab>(this,
                $(Schrodinger.byDataId("attributeTable")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }

}
