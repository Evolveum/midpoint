/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.evolveum.midpoint.schrodinger.page.configuration.BulkActionsPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import org.testng.annotations.Test;

/**
 * Created by Kate Honchar
 */
public class BulkActionsTest extends AbstractSchrodingerTest {

    private static final String PARSING_ERROR_MESSAGE = "Couldn't parse bulk action object";

    @Test
    public void wrongBulkActionXmlExecution(){
        BulkActionsPage bulkActionsPage = basicPage.bulkActions();
        bulkActionsPage
                .insertOneLineTextIntoEditor("<objects></objects>")
                .startButtonClick()
                .feedback()
                .assertError()
                .assertMessageExists(PARSING_ERROR_MESSAGE);

        bulkActionsPage.assertAceEditorVisible();
    }
}
