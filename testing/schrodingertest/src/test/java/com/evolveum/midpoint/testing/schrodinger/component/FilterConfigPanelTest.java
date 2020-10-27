/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.component;

import com.evolveum.midpoint.schrodinger.component.user.UsersPageTable;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar
 */
public class FilterConfigPanelTest extends AbstractSchrodingerTest {

    private static final File TEST_OBJECT_COLLECTION = new File("./src/test/resources/component/objects/objectCollections/filter-config-test-object-collection.xml");
    private static final File OBJECT_COLLECTION_TEST_USER = new File("./src/test/resources/component/objects/users/object-collection-test-user.xml");
    private static final File NEW_OBJECT_COLLECTION_TEST_USER = new File("./src/test/resources/component/objects/users/new-object-collection-test-user.xml");
    private static final File SYSTEM_CONFIG_WITH_OBJ_COLLECTION = new File("./src/test/resources/configuration/objects/systemconfig/system-configuration-user-obj-collection.xml");

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(TEST_OBJECT_COLLECTION, OBJECT_COLLECTION_TEST_USER, SYSTEM_CONFIG_WITH_OBJ_COLLECTION, NEW_OBJECT_COLLECTION_TEST_USER);
    }

    @Test
    public void configureFilterForObjectCollection() {
        basicPage
                .listObjectCollections()
                .table()
                .clickByName("FilterTestUsers")
                .selectTabBasic()
                .form()
                .showEmptyAttributes("Properties")
                .and()
                .and()
                .configSearch()
                .setPropertyTextValue("Name", "FilterConfigTest", true)
                .setPropertyFilterValue("Name", "Equal", true)
                .confirmConfiguration()
                .clickSave()
                .feedback()
                .isSuccess();

        midPoint.logout();
        midPoint.formLogin().login(username, password);

        UsersPageTable usersPageTable = basicPage.listUsers("FilterTestUsers").table();
        Assert.assertEquals(1, usersPageTable.countTableObjects());
        Assert.assertTrue(usersPageTable.containsText("FilterConfigTest"));
    }

    @Test
    public void createNewObjectCollectionWithConfiguredFilter() {
        Assert.assertTrue(basicPage
                .newObjectCollection()
                    .selectTabBasic()
                        .form()
                        .addAttributeValue("Name", "NewObjCollectionTest")
                        .setDropDownAttributeValue("Type", "User")
                    .and()
                .and()
                    .configSearch()
                    .setPropertyTextValue("Name", "NewObjCollectionTestUser", true)
                    .setPropertyFilterValue("Name", "Equal", true)
                .confirmConfiguration()
                .clickSave()
                .feedback()
                .isSuccess());

        Assert.assertTrue(basicPage
                .adminGui()
                .addNewObjectCollection("NewObjCollectionTest", "User", "Object collection", "NewObjCollectionTest")
                .feedback()
                .isSuccess());

        midPoint.logout();
        midPoint.formLogin().login(username, password);

        UsersPageTable usersPageTable = basicPage.listUsers("NewObjCollectionTest").table();
        Assert.assertEquals(usersPageTable.countTableObjects(), 1);
        Assert.assertTrue(usersPageTable.containsText("NewObjCollectionTestUser"));
    }

}
