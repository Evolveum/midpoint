/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.TestBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Created by matus on 5/11/2018.
 */
public class UserPhotoTests extends TestBase {

    private static final String TEST_USER_LEO_NAME= "leonardo";
    private static final File PHOTO_SOURCE_FILE_LARGE = new File("./src/test/resources/images/leonardo_large_nc.jpg");
    private static final File PHOTO_SOURCE_FILE_SMALL = new File("./src/test/resources/images/leonardo_small_nc.jpg");

    private static final String CREATE_USER_WITH_LARGE_PHOTO_DEPENDENCY = "createMidpointUserWithPhotoLarge";
    private static final String CREATE_USER_WITH_NORMAL_PHOTO_DEPENDENCY = "createMidpointUserWithPhotoJustRight";

    //@Test TODO test commented out because of MID-4774
    public void createMidpointUserWithPhotoLarge(){
        UserPage user = basicPage.newUser();

        Assert.assertTrue(
                user
                    .selectTabBasic()
                        .form()
                        .addAttributeValue("name", TEST_USER_LEO_NAME)
                        .addAttributeValue(UserType.F_GIVEN_NAME, "Leonardo")
                        .addAttributeValue(UserType.F_FAMILY_NAME, "di ser Piero da Vinci")
                        .setFileForUploadAsAttributeValue("Jpeg photo", PHOTO_SOURCE_FILE_LARGE)
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isError()
        );
    }

    @Test //(dependsOnMethods = {CREATE_USER_WITH_LARGE_PHOTO_DEPENDENCY}) // TODO uncomment test dependency after MID-4774 fix
    public void createMidpointUserWithPhotoJustRight(){
        UserPage user = basicPage.newUser();
        Assert.assertTrue(
                    user
                        .selectTabBasic()
                            .form()
                            .addAttributeValue("name", TEST_USER_LEO_NAME)
                            .addAttributeValue(UserType.F_GIVEN_NAME, "Leonardo")
                            .addAttributeValue(UserType.F_FAMILY_NAME, "di ser Piero da Vinci")
                            .setFileForUploadAsAttributeValue("Jpeg photo", PHOTO_SOURCE_FILE_SMALL)
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess()
        );
    }

    @Test (dependsOnMethods = {CREATE_USER_WITH_NORMAL_PHOTO_DEPENDENCY})
    public void deleteUserPhoto(){
         ListUsersPage usersPage = basicPage.listUsers();
         Assert.assertTrue(
                 usersPage
                    .table()
                        .search()
                        .byName()
                            .inputValue(TEST_USER_LEO_NAME)
                            .updateSearch()
                    .and()
                    .clickByName(TEST_USER_LEO_NAME)
                        .selectTabBasic()
                            .form()
                            .removeFileAsAttributeValue("Jpeg photo")
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess()
         );
    }
}
