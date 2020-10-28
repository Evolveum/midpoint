/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import static com.evolveum.midpoint.schrodinger.util.ConstantsUtil.*;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.TabWithContainerWrapper;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AdminGuiTab extends TabWithContainerWrapper<SystemPage> {

    public AdminGuiTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public SystemPage addNewObjectCollection(String identifier, String type, String objectCollectionType, String objectCollectionName) {
        this.form()
                .expandContainerPropertiesPanel(OBJECT_COLLECTION_VIEWS_HEADER)
                .addNewContainerValue(OBJECT_COLLECTION_VIEW_HEADER, NEW_GUI_OBJECT_LIST_VIEW_HEADER)
                    .getPrismContainerPanel(NEW_GUI_OBJECT_LIST_VIEW_HEADER)
                        .getContainerFormFragment()
                        .addAttributeValue("Identifier", identifier)
                        .setDropDownAttributeValue("Type", type)
                            .getPrismContainerPanel("Display")
                                .getContainerFormFragment()
                                .addAttributeValue("Label", identifier)
                                .addAttributeValue("Singular label", identifier)
                                .addAttributeValue("Plural label", identifier)
                            .and()
                        .and()
                            .getPrismContainerPanel("Collection")
                                .getContainerFormFragment()
                                .editRefValue("Collection ref")
                                    .selectType(objectCollectionType)
                                    .table()
                                        .search()
                                            .byName()
                                            .inputValue(objectCollectionName)
                                        .updateSearch()
                                    .and()
                                    .clickByName(objectCollectionName)
                                .and()
                            .and()
                        .and()
                    .and()
                .and()
                .clickSave();
        return getParent();
    }
}
