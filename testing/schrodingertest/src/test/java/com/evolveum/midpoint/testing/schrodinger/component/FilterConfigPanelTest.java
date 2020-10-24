/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.component;

import org.testng.annotations.Test;

import com.evolveum.midpoint.schrodinger.page.objectcollection.ObjectCollectionPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

/**
 * Created by honchar
 */
public class FilterConfigPanelTest extends AbstractSchrodingerTest {


//    @Override
//    protected List<File> getObjectListToImport(){
//    }

    @Test
    public void createNewObjectCollectionWithConfiguredFilter() {
        ObjectCollectionPage objectCollectionPage = basicPage.newObjectCollection();
        objectCollectionPage
                .selectTabBasic()
                .form()
                .setAttributeValue(ObjectCollectionType.F_NAME, "TestCollection")
                .setDropDownAttributeValue(ObjectCollectionType.F_TYPE, "User");
        objectCollectionPage
                .configSearch()
                .setPropertyTextValue("Name", "FilterConfigTest", true)
                .setPropertyFilterValue("Name", "Equal", true)
                .confirmConfiguration()
                .clickSave()
                .feedback()
                .isSuccess();
    }

}
