/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger.page;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.component.prism.show.PartialSceneHeader;

import com.evolveum.midpoint.schrodinger.page.user.ProgressPage;
import com.evolveum.midpoint.schrodinger.util.ConstantsUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;

import org.testng.annotations.Test;

import com.evolveum.midpoint.schrodinger.component.prism.show.PreviewChangesTab;
import com.evolveum.midpoint.schrodinger.component.prism.show.ScenePanel;
import com.evolveum.midpoint.schrodinger.page.PreviewPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import static org.testng.Assert.*;

public class PreviewPageTest  extends AbstractSchrodingerTest {

    private static final String TEST_DIR = "./src/test/resources/page/preview";

    private static final File ROLE_USER_PREVIEW_FILE = new File(TEST_DIR, "role-user-preview.xml");
    private static final String ROLE_USER_PREVIEW_NAME = "rolePreviewChanges";

    private static final File ROLE_USER_NO_PREVIEW_FILE = new File(TEST_DIR, "role-user-no-preview.xml");
    private static final String ROLE_USER_NO_PREVIEW_NAME = "roleNoPreviewChanges";

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(ROLE_USER_PREVIEW_FILE, ROLE_USER_NO_PREVIEW_FILE);
    }

    @Test (priority = 1)
    public void test001createUser() {

        //@formatter:off
        UserPage user = basicPage.newUser();

        PreviewPage previewPage = user.selectTabBasic()
                .form()
                    .addAttributeValue("name", "jack")
                    .addAttributeValue(UserType.F_GIVEN_NAME, "Jack")
                    .setPasswordFieldsValues(PasswordType.F_VALUE,"asd123")
                    .and()
                .and()
                .clickPreview();
        //@formatter:on

        ScenePanel<PreviewChangesTab> primaryDeltaScene = previewPage.selectPanelByName("jack").primaryDeltas();
        assertTrue(primaryDeltaScene.isExpanded(), "Primary deltas should be expanded");

        List<ScenePanel> deltas = primaryDeltaScene.objectDeltas();
        assertEquals(3, deltas.size(), "Unexpected number of primary deltas");

        ScenePanel<ScenePanel> primaryDelta = deltas.get(0);

        PartialSceneHeader header = primaryDelta.header();
        assertEquals(header.getChangeType(), "Add", "Unexpected change type");
        assertEquals(header.getChangedObjectName(), "jack", "Unexpected object name");
        assertTrue(!header.isLink(), "Link not expected for new user");

        ProgressPage progressPage = previewPage.clickSave();
        Selenide.sleep(3000);
        progressPage.feedback().assertSuccess();
    }

    @Test (priority = 2, dependsOnMethods = {"test001createUser"})
    public void test002modifyUser() {

        //@formatter:off
        PreviewPage previewPage = basicPage.listUsers()
                .table()
                    .clickByName("jack")
                    .selectTabBasic()
                        .form()
                            .addAttributeValue(UserType.F_FAMILY_NAME, "Sparrow")
                        .and()
                    .and()
                .clickPreview();
        //@formatter:on

        ScenePanel<PreviewChangesTab> primaryDeltaScene = previewPage.selectPanelByName("jack").primaryDeltas();
        assertTrue(primaryDeltaScene.isExpanded(), "Primary deltas should be expanded");

        List< ScenePanel> deltas = primaryDeltaScene.objectDeltas();
        assertEquals(1, deltas.size(), "Unexpected number of primary deltas");

        ScenePanel<ScenePanel> primaryDelta = deltas.get(0);

        PartialSceneHeader header = primaryDelta.header();
        assertEquals(header.getChangeType(), "Modify", "Unexpected change type");
        assertEquals(header.getChangedObjectName(), "jack", "Unexpected object name");
        assertTrue(header.isLink(), "Link expected for modify user with authorizations");

        ProgressPage progressPage = previewPage.clickSave();
        Selenide.sleep(1000);
        progressPage.feedback().assertSuccess();
    }

    @Test (priority = 3, dependsOnMethods = {"test001createUser"})
    public void test003assignRolePreview() {
        //@formatter:off
        ProgressPage previewPage = basicPage.listUsers()
                .table()
                    .clickByName("jack")
                    .selectTabAssignments()
                        .clickAddAssignemnt()
                            .selectType(ConstantsUtil.ASSIGNMENT_TYPE_SELECTOR_ROLE)
                            .table()
                                .search()
                                    .byName()
                                        .inputValue(ROLE_USER_PREVIEW_NAME)
                                    .updateSearch()
                                .and()
                            .selectCheckboxByName(ROLE_USER_PREVIEW_NAME)
                        .and()
                    .clickAdd()
                .and()
                .clickSave();
        //@formatter:on

        previewPage.feedback().assertSuccess();

    }

    @Test (priority = 4, dependsOnMethods = {"test001createUser"})
    public void test004loginWithUserJack() {

        midPoint.logout();

        basicPage = midPoint.formLogin().login("jack", "asd123");

        PreviewPage previewPage = basicPage.profile()
                .selectTabBasic()
                    .form()
                        .addAttributeValue(UserType.F_FULL_NAME, "Jack Sparrow")
                    .and()
                .and()
                .clickPreview();

        ScenePanel<PreviewChangesTab> primaryDeltaScene = previewPage.selectPanelByName("jack").primaryDeltas();
        assertTrue(primaryDeltaScene.isExpanded(), "Primary deltas should be expanded");

        List<ScenePanel> deltas = primaryDeltaScene.objectDeltas();
        assertEquals(1, deltas.size(), "Unexpected number of primary deltas");

        ScenePanel<ScenePanel> primaryDelta = deltas.get(0);

        PartialSceneHeader header = primaryDelta.header();
        assertEquals(header.getChangeType(), "Modify", "Unexpected change type");
        assertEquals(header.getChangedObjectName(), "jack", "Unexpected object name");
        assertTrue(!header.isLink(), "Link expected for modify user with authorizations");

        midPoint.logout();
        basicPage = midPoint.formLogin().login("administrator", "5ecr3t");

    }

    @Test (priority = 5, dependsOnMethods = {"test001createUser", "test003assignRolePreview"})
    public void test005unassignRolePreview() {
        //@formatter:off
        ProgressPage previewPage = basicPage.listUsers()
                .table()
                    .clickByName("jack")
                        .selectTabAssignments()
                            .table()
                                .selectCheckboxByName(ROLE_USER_PREVIEW_NAME)
                                .removeByName(ROLE_USER_PREVIEW_NAME)
                            .and()
                        .and()
                    .clickSave();
        //@formatter:on

        previewPage.feedback().assertSuccess();

    }

    @Test (priority = 6, dependsOnMethods = {"test001createUser"})
    public void test006assignRoleNoPreview() {
        //@formatter:off
        ProgressPage previewPage = basicPage.listUsers()
                .table()
                    .clickByName("jack")
                        .selectTabAssignments()
                            .clickAddAssignemnt()
                                .selectType(ConstantsUtil.ASSIGNMENT_TYPE_SELECTOR_ROLE)
                                .table()
                                    .search()
                                        .byName()
                                            .inputValue(ROLE_USER_NO_PREVIEW_NAME)
                                            .updateSearch()
                                        .and()
                                    .selectCheckboxByName(ROLE_USER_NO_PREVIEW_NAME)
                                .and()
                            .clickAdd()
                        .and()
                    .clickSave();
        //@formatter:on

        previewPage.feedback().assertSuccess();

    }

    @Test (priority = 7, dependsOnMethods = {"test001createUser", "test003assignRolePreview", "test005unassignRolePreview", "test006assignRoleNoPreview"})
    public void test007loginWithUserJack() {

        midPoint.logout();

        basicPage = midPoint.formLogin().login("jack", "asd123");

        UserPage userPage = basicPage.profile()
                .selectTabBasic()
                    .form()
                        .addAttributeValue(UserType.F_FULL_NAME, "Jack Sparrow")
                    .and()
                .and();

        Selenide.screenshot("previewVisible");
        assertFalse(userPage.isPreviewButtonVisible(), "Preview button should not be visible");
        midPoint.logout();
    }

}
