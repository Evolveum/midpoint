/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author skublik
 */

public class SynchronizationFlavours extends AbstractLabTest{

//    @AfterClass
//    @Override
//    public void afterClass() {
//        super.afterClass();
//
//        midPoint.formLogin().loginWithReloadLoginPage(username, password);
//
//        LOG.info("After: Login name " + username + " pass " + password);
//
//        AboutPage aboutPage = basicPage.aboutPage();
//        aboutPage
//                .clickSwitchToFactoryDefaults()
//                .clickYes();
//    }

    @BeforeClass
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
        csv1TargetFile = new File("/home/lskublik/Documents/Evolveum/actual/master/05-02-2020/midpoint/testing/schrodingertest/target/midpoint-home/schrodinger/labTests/csv-1.csv");
        csv2TargetFile = new File ("/home/lskublik/Documents/Evolveum/actual/master/05-02-2020/midpoint/testing/schrodingertest/target/midpoint-home/schrodinger/labTests/csv-2.csv");
        csv3TargetFile = new File ("/home/lskublik/Documents/Evolveum/actual/master/05-02-2020/midpoint/testing/schrodingertest/target/midpoint-home/schrodinger/labTests/csv-3.csv");
    }

    @Test
    public void test0701RunningImportFromResource() {

    }
}
