/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.evolveum.midpoint.schrodinger.component.report.ReportTable;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by honchar
 */
public class ReportTests extends AbstractSchrodingerTest {

    @Test
    public void test00100createReport() {
        Assert.assertTrue(basicPage.listReports()
        .newReport()
            .selectTabBasic()
                .form()
                    .addAttributeValue("Name", "TestReport")
                    .and()
                .and()
            .clickSave()
            .feedback()
            .isSuccess(),
                "Couldn't create new report");
        Assert.assertEquals("1", basicPage.listReports()
            .table()
                .search()
                    .byName()
                    .inputValue("TestReport")
                    .updateSearch()
                    .and()
                .countTableObjects());
        Assert.assertTrue(basicPage.listReports().table().containsText("TestReport"));
    }

    @Test
    public void test00200runUsersReport() {
        ReportTable reportTable = basicPage.listReports().table();
        reportTable.runReport("All audit records report");
//                .runReport();
        Assert.assertEquals(1, basicPage.createdReports()
            .table()
                .search()
                    .byName()
                    .inputValue("All audit records report")
                    .updateSearch()
                    .and()
                .countTableObjects(),
                "Report isn't found after run");
    }
}
