/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.evolveum.midpoint.schrodinger.page.configuration.AboutPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by matus on 3/16/2018.
 */
public class AboutPageTest extends AbstractSchrodingerTest {

    private static final String VERSION_EXPECTED = "4.3-SNAPSHOT"; // Static value, should be changed each version change.
    private static final String HIBERNATE_DIALECT_EXPECTED = "org.hibernate.dialect.H2Dialect";
    private static final String CONNID_VERSION_EXPECTED = "1.5.0.17"; // Static value, should be changed each version change.
    private static final String REINDEX_REPO_TASK_CATEGORY_EXPECTED = "Utility";
    private static final String REINDEX_REPO_TASK_DISPLAY_NAME_EXPECTED = "Reindex repository objects";

    private static final String PROPERTY_JVM_NAME_XMX = "-Xmx";

    private AboutPage aboutPage;

    @BeforeMethod
    private void openPage() {
        aboutPage = basicPage.aboutPage();
    }

    @Test
    public void checkMidpointVersion() {
        aboutPage.assertVersionValueEquals(VERSION_EXPECTED);
    }

    @Test
    public void checkGitDescribeValue() {
        aboutPage.assertGitDescribeValueIsNotEmpty();
    }

    @Test
    public void checkBuildAt() {
        aboutPage.assertBuildAtValueIsNotEmpty();
    }

    @Test // TODO fix select the right element
    public void checkHibernateDialect() {
        aboutPage.assertHibernateDialectValueEquals(HIBERNATE_DIALECT_EXPECTED);
    }

    @Test
    public void checkConnIdVersion() {
        aboutPage.assertConnIdVersionValueEquals(CONNID_VERSION_EXPECTED);
    }

    @Test
    public void repoSelfTestFeedbackPositive() {

        aboutPage
                .repositorySelfTest()
                .feedback()
                .assertSuccess();
    }

    @Test
    public void reindexRepositoryObjectsFeedbackInfo() {
        aboutPage
                .reindexRepositoryObjects()
                .feedback()
                .assertInfo();

    }

    @Test
    public void checkReindexRepositoryObjectsCategory() {
        aboutPage
                .reindexRepositoryObjects()
                    .feedback()
                        .clickShowTask()
                        .assertUtilityValueEquals(REINDEX_REPO_TASK_CATEGORY_EXPECTED);
    }

    @Test
    public void checkReindexRepositoryObjectsDisplayName() {
        // @formatter:off
        aboutPage
                        .reindexRepositoryObjects()
                            .feedback()
                                .clickShowTask()
                                    .and()
                                        .summary()
                                        .assertDisplayNameEquals(REINDEX_REPO_TASK_DISPLAY_NAME_EXPECTED);
        // @formatter:on
    }

    @Test (enabled = false)
    public void checkJVMPropertiesMidpointHome(){
        aboutPage.assertJVMPropertyValueIsNotEmpty(AbstractSchrodingerTest.PROPERTY_NAME_MIDPOINT_HOME);
    }

    @Test
    public void checkJVMPropertiesXmx(){
        aboutPage.assertJVMPropertyValueIsNotEmpty(PROPERTY_JVM_NAME_XMX);
    }
    @Test
    public void checkSystemProperty(){
        aboutPage.assertSystemPropertyValueIsNotEmpty(AbstractSchrodingerTest.PROPERTY_NAME_USER_HOME);
    }
}
