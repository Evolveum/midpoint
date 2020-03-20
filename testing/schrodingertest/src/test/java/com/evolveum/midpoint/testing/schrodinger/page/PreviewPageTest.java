/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger.page;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.util.List;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.prism.show.PartialSceneHeader;

import com.evolveum.midpoint.schrodinger.page.user.ProgressPage;
import com.evolveum.midpoint.schrodinger.util.ConstantsUtil;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;

import org.springframework.test.annotation.DirtiesContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schrodinger.component.prism.show.PreviewChangesTab;
import com.evolveum.midpoint.schrodinger.component.prism.show.ScenePanel;
import com.evolveum.midpoint.schrodinger.page.PreviewPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class PreviewPageTest  extends AbstractSchrodingerTest {

    private static final String TEST_DIR = "./src/test/resources/page/preview";

    private static final File ROLE_USER_PREVIEW_FILE = new File(TEST_DIR, "role-user-preview.xml");
    private static final String ROLE_USER_PREVIEW_NAME = "rolePreviewChanges";

    @Override
    protected void initSystem(Task task, OperationResult initResult) throws Exception {
        super.initSystem(task, initResult);

        repoAddObjectFromFile(ROLE_USER_PREVIEW_FILE, initResult);
    }

    @Test
    public void test001createUser() {

        //@formatter:off
        UserPage user = basicPage.newUser();

        PreviewPage previewPage = null;

        PrismForm<AssignmentHolderBasicTab<UserPage>> f = user.selectTabBasic()
                .form()
                    .addAttributeValue("name", "jack")
                    .addAttributeValue(UserType.F_GIVEN_NAME, "Jack")
                    .setPasswordFieldsValues(PasswordType.F_VALUE,"asd123");

        Selenide.screenshot("after password");
                previewPage = f.and()
                .and()
                .clickPreview();
        //@formatter:on

        Selenide.screenshot("preview");
        ScenePanel<PreviewChangesTab> primaryDeltaScene = previewPage.selectPanelByName("jack").primaryDeltas();
        assertTrue(primaryDeltaScene.isExpanded(), "Primary deltas should be expanded");

        Selenide.screenshot("deltas");
        List<ScenePanel> deltas = primaryDeltaScene.objectDeltas();
        assertEquals(3, deltas.size(), "Unexpected number of primary deltas");

        ScenePanel<ScenePanel> primaryDelta = deltas.get(0);

        PartialSceneHeader header = primaryDelta.header();
        assertEquals(header.getChangeType(), "Add", "Unexpected change type");
        assertEquals(header.getChangedObjectName(), "jack", "Unexpected object name");
        assertTrue(!header.isLink(), "Link not expected for new user");

        ProgressPage progressPage = previewPage.clickSave();
        Selenide.sleep(3000);
        assertTrue(progressPage.feedback().isSuccess());
    }

    @Test
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
                .clickPreviewChanges();
        //@formatter:on

        Selenide.screenshot("modfifyUser, preview change");

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
        assertTrue(progressPage.feedback().isSuccess());
    }

    @Test
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
                                    .and()
                                .and()
                            .selectCheckboxByName(ROLE_USER_PREVIEW_NAME)
                        .and()
                    .clickAdd()
                .and()
                .clickSave();
        //@formatter:on

        assertTrue(previewPage.feedback().isSuccess());

    }

    @Test
    public void test004loginWithUserJack() {

        midPoint.logout();

        basicPage = midPoint.formLogin().login("jack", "asd123");

        PreviewPage previewPage = basicPage.profile()
                .selectTabBasic()
                    .form()
                        .addAttributeValue(UserType.F_FULL_NAME, "Jack Sparrow")
                    .and()
                .and()
                .clickPreviewChanges();

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


}
