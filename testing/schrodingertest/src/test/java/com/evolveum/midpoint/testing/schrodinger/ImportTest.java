/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger;

import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.screenshot;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportTest extends TestBase {

    @Test
    public void tryCheckboxes() {

    }

    @Test
    public void importXml() {
//        File user = new File("./src/test/resources/user.xml");
//        String xml = FileUtils.readFileToString(user, StandardCharsets.UTF_8);
//
//        ImportObjectPage importObject = basic.importObject();
//        importObject
////                .checkKeepOid()
////                .checkOverwriteExistingObject()
//                .getObjectsFromEmbeddedEditor()
//                .setEditorText(xml);
//
////                importObject.clickImport();

        screenshot("asdf");
    }

    @Test
    public void importFile() {

    }
}
