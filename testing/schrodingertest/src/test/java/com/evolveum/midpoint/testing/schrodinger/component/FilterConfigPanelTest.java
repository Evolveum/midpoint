/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.component;

import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schrodinger.component.user.UsersPageTable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schrodinger.page.objectcollection.ObjectCollectionPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar
 */
public class FilterConfigPanelTest extends AbstractSchrodingerTest {

    private static final File TEST_OBJECT_COLLECTION = new File("./src/test/resources/component/objects/objectCollections/filter-config-test-object-collection.xml");
    private static final File OBJECT_COLLECTION_TEST_USER = new File("./src/test/resources/component/objects/users/object-collection-test-user.xml");
    private static final File SYSTEM_CONFIG_WITH_OBJ_COLLECTION = new File("./src/test/resources/configuration/objects/systemconfig/system-configuration-user-obj-collection.xml");

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(TEST_OBJECT_COLLECTION, OBJECT_COLLECTION_TEST_USER, SYSTEM_CONFIG_WITH_OBJ_COLLECTION);
    }

    @Test
    public void createNewObjectCollectionWithConfiguredFilter() {
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

}
