/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.objectcollection;

import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by Kate Honchar.
 */
public class ObjectCollectionsListTable extends AssignmentHolderObjectListTable<ListObjectCollectionsPage, ObjectCollectionPage> {

    public ObjectCollectionsListTable(ListObjectCollectionsPage parent, SelenideElement parentElement){
        super(parent, parentElement);
    }

    @Override
    protected TableHeaderDropDownMenu<ObjectCollectionsListTable> clickHeaderActionDropDown() {
        return null;
    }

    @Override
    public ObjectCollectionPage getObjectDetailsPage(){
        $(Schrodinger.byDataId("mainPanel"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new ObjectCollectionPage();
    }
}
