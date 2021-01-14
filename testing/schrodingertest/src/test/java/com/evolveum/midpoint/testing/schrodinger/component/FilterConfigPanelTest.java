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
    private static final File OBJ_REF_PROPERTY_CONFIG_COLLECTION_TEST = new File("./src/test/resources/component/objects/objectCollections/obj-ref-property-config-test.xml");
    private static final File DROPDOWN_PROPERTY_CONFIG_COLLECTION_TEST = new File("./src/test/resources/component/objects/objectCollections/dropdown-property-config-test.xml");
    private static final File OBJECT_COLLECTION_TEST_USER = new File("./src/test/resources/component/objects/users/object-collection-test-user.xml");
    private static final File OBJ_REF_PROPERTY_CONFIG_TEST_USER = new File("./src/test/resources/component/objects/users/obj-ref-property-config-test-user.xml");
    private static final File DROPDOWN_PROPERTY_CONFIG_TEST_USER = new File("./src/test/resources/component/objects/users/dropdown-property-config-test-user.xml");
    private static final File NEW_OBJECT_COLLECTION_TEST_USER = new File("./src/test/resources/component/objects/users/new-object-collection-test-user.xml");
    private static final File SYSTEM_CONFIG_WITH_OBJ_COLLECTIONS = new File("./src/test/resources/configuration/objects/systemconfig/system-configuration-user-obj-collection.xml");

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(TEST_OBJECT_COLLECTION, OBJECT_COLLECTION_TEST_USER, SYSTEM_CONFIG_WITH_OBJ_COLLECTIONS,
                NEW_OBJECT_COLLECTION_TEST_USER, OBJ_REF_PROPERTY_CONFIG_COLLECTION_TEST, OBJ_REF_PROPERTY_CONFIG_TEST_USER,
                DROPDOWN_PROPERTY_CONFIG_COLLECTION_TEST, DROPDOWN_PROPERTY_CONFIG_TEST_USER);
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
        usersPageTable.assertTableObjectsCountEquals(1);
        usersPageTable.assertTableContainsText("FilterConfigTest");
    }

    @Test
    public void createNewObjectCollectionWithConfiguredFilter() {
        basicPage
                .newObjectCollection()
                    .selectTabBasic()
                        .form()
                        .addAttributeValue("Name", "NewObjCollectionTest")
                        .setDropDownAttributeValue("Type", "User")
                    .and()
                .and()
                    .configSearch()
                    .setPropertyTextValue("Name", "NewObjCollection", true)
                    .setPropertyFilterValue("Name", "Starts with", true)
                .confirmConfiguration()
                .clickSave()
                .feedback()
                .assertSuccess();

        basicPage
                .adminGui()
                .addNewObjectCollection("NewObjCollectionTest", "User", "Object collection", "NewObjCollectionTest")
                .feedback()
                .assertSuccess();

        midPoint.logout();
        midPoint.formLogin().login(username, password);

        UsersPageTable usersPageTable = basicPage.listUsers("NewObjCollectionTest").table();
        usersPageTable.assertTableObjectsCountEquals(1);
        usersPageTable.assertTableContainsText("NewObjCollectionTestUser");
    }

    @Test
    public void configureFilterWithObjectReferenceAttribute() {
        basicPage
                .listObjectCollections()
                    .table()
                        .search()
                            .byName()
                            .inputValue("ObjRefAttributeConfigTest")
                            .updateSearch()
                        .and()
                        .clickByName("ObjRefAttributeConfigTest")
                            .selectTabBasic()
                                .form()
                                .showEmptyAttributes("Properties")
                                .and()
                            .and()
                            .configSearch()
                                .setPropertyObjectReferenceValue("Role membership", "00000000-0000-0000-0000-000000000008", true)
                                .confirmConfiguration()
                            .clickSave()
                        .feedback()
                                .isSuccess();
        midPoint.logout();
        midPoint.formLogin().login(username, password);

        UsersPageTable usersPageTable = basicPage.listUsers("ObjRefAttributeConfigTest").table();
        usersPageTable.assertTableObjectsCountEquals(1);
        usersPageTable.assertTableContainsText("ObjRefPropertyConfigUser");
    }

    @Test
    public void configureFilterWithDropdownAttribute() {
        basicPage
                .listObjectCollections()
                    .table()
                        .search()
                            .byName()
                            .inputValue("DropdownPropertyConfigTest")
                            .updateSearch()
                        .and()
                        .clickByName("DropdownPropertyConfigTest")
                            .selectTabBasic()
                                .form()
                                .showEmptyAttributes("Properties")
                                .and()
                            .and()
                            .configSearch()
                                .setPropertyDropdownValue("Administrative status", "DISABLED", true)
                                .setPropertyFilterValue("Name", "Equal", true)
                                .confirmConfiguration()
                            .clickSave()
                        .feedback()
                                .isSuccess();
        midPoint.logout();
        midPoint.formLogin().login(username, password);

        UsersPageTable usersPageTable = basicPage.listUsers("DropdownPropertyConfigTest").table();
        usersPageTable.assertTableObjectsCountEquals(1);
        usersPageTable.assertTableContainsText("DropdownPropertyConfigUser");
    }

}
