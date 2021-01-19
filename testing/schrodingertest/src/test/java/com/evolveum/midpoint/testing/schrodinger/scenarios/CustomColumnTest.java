/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.component.modal.ExportPopupPanel;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.codeborne.selenide.Selenide.screenshot;

/**
 * Created by honchar
 */
public class CustomColumnTest extends AbstractSchrodingerTest {

    private static final File CUSTOM_COLUMNS_OBJECT_COLLECTION_SIMPLE_FILE = new File("./src/test/resources/component/objects/objectCollections/object-collection-custom-columns-simple.xml");
    private static final File CUSTOM_COLUMNS_OBJECT_COLLECTION_KEY_LABELS_FILE = new File("./src/test/resources/component/objects/objectCollections/object-collection-custom-columns-key-labels.xml");
    private static final File CUSTOM_COLUMNS_SYSTEM_CONFIGURATION = new File("./src/test/resources/configuration/objects/systemconfig/system-configuration-custom-columns.xml");

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(CUSTOM_COLUMNS_OBJECT_COLLECTION_SIMPLE_FILE, CUSTOM_COLUMNS_SYSTEM_CONFIGURATION,
                CUSTOM_COLUMNS_OBJECT_COLLECTION_KEY_LABELS_FILE);
    }

    @Test(priority = 1)
    public void test00100checkUserCustomColumns() {
        basicPage.listUsers("Custom columns view")
                .table()
                    .assertColumnIndexMatches("Name (custom)", 3);
        basicPage.listUsers("Custom columns view")
                .table()
                    .assertColumnIndexMatches("Role membership", 4);
        basicPage.listUsers("Custom columns view")
                .table()
                    .assertColumnIndexMatches("Preferred language", 5);
    }

    @Test(priority = 2)
    public void test00200checkUserCustomColumnsKeyLabels() {
        ListUsersPage usersPage = basicPage.listUsers("Custom columns label test");
        usersPage
                .table()
                    .assertColumnIndexMatches("Enable", 3);
        usersPage
                .table()
                    .assertColumnIndexMatches("Disable", 4);
        usersPage
                .table()
                    .assertColumnIndexMatches("Unlink", 5);
    }

    @Test(priority = 3)
    public void test00300checkExportColumns() {
        ListUsersPage usersPage = basicPage.listUsers("Custom columns view");
        Table<ExportPopupPanel<ListUsersPage>> exportTable = usersPage.table()
                .clickExportButton()
                    .table();
        screenshot("exportTable");
        exportTable
                .assertTableRowExists("Column name", "Name (custom)")
                .assertTableRowExists("Column name", "Role membership")
                .assertTableRowExists("Column name", "Preferred language");
    }
}
