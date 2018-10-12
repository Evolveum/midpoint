/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.testing.schrodinger.labs;

import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.evolveum.midpoint.testing.schrodinger.TestBase;

import java.io.File;


/**
 * Created by honchar
 */
public class ImportResourceTest extends TestBase {

    private static final File CSV_RESOURCE = new File("./src/test/resources/labs/resources/localhost-csvfile-1-document-access.xml");
    private static final String RESOURCE_NAME = "CSV-1 (Document Access)";

    @Test
    public void test001ImportCsvResource() {
        ImportObjectPage importPage = basicPage.importObject();

        Assert.assertTrue(
                importPage
                        .getObjectsFromFile()
                        .chooseFile(CSV_RESOURCE)
                        .checkOverwriteExistingObject()
                        .clickImport()
                        .feedback()
                        .isSuccess()
        );

        ListResourcesPage listResourcesPage = basicPage.listResources();

        Assert.assertTrue(listResourcesPage
                .testConnectionClick(RESOURCE_NAME)
                .feedback()
                .isSuccess());
    }


}

