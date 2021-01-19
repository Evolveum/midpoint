/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.evolveum.midpoint.schrodinger.component.DateTimePanel;
import com.evolveum.midpoint.schrodinger.component.configuration.ClockTab;
import com.evolveum.midpoint.schrodinger.page.configuration.InternalsConfigurationPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * @author Hiroyuki Wada
 */
public class InternalsConfigurationPageTest extends AbstractSchrodingerTest {

    @AfterClass
    @Override
    public void afterClass() {
        InternalsConfigurationPage configPage = basicPage.internalsConfiguration();
        configPage = basicPage.internalsConfiguration();
        configPage.clockTab().resetTime();
        super.afterClass();
    }

    @Test (enabled = false)
    public void test001changeTime() {
        InternalsConfigurationPage configPage = basicPage.internalsConfiguration();
        ClockTab clockTab = configPage.clockTab();

        clockTab.changeTime("5/15/2099", "10", "30", DateTimePanel.AmOrPmChoice.PM);

        basicPage.feedback().assertSuccess();

        basicPage.aboutPage();
        basicPage
                .internalsConfiguration()
                    .clockTab()
                        .getOffsetPanel()
                            .assertDateValueEquals("5/15/2099")
                            .assertHoursValueEquals("10")
                            .assertMinutesValueEquals("30")
                            .assertAmPmValueEquals(DateTimePanel.AmOrPmChoice.PM.name());
    }

    @Test
    public void test010resetTime() {
        InternalsConfigurationPage configPage = basicPage.internalsConfiguration();
        ClockTab clockTab = configPage.clockTab();

        clockTab.resetTime();

        basicPage.feedback().assertSuccess();
    }
}
