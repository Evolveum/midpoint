/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger;

import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportTest extends AbstractSchrodingerTest {

    @Test
    public void importXml() throws IOException {
        File user = new File("./src/test/resources/user.xml");
        String xml = FileUtils.readFileToString(user, StandardCharsets.UTF_8);

        ImportObjectPage importObject = basicPage.importObject();
        importObject
                .checkKeepOid()
                .checkOverwriteExistingObject()
                .getObjectsFromEmbeddedEditor()
                .setEditorXmlText(xml);

        importObject.clickImportXmlButton()
                .feedback()
                        .assertSuccess();
    }
}
