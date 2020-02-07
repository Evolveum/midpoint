/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.testing.schrodinger.TestBase;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;


public class AdvancedAccountTests extends TestBase {
// TODO in progress
  private static File csvTargetFile;

  private static final String FILE_RESOUCE_NAME = "midpoint-advanced-sync.csv";
  private static final String INITIALIZE_TEST_CONFIGURATION_DEPENDENCY = "initializeTestConfiguration";
  private static final String DIRECTORY_CURRENT_TEST = "advancedAccountTests";

  @Test
  public void initializeTestConfiguration() throws IOException, ConfigurationException {

    initTestDirectory(DIRECTORY_CURRENT_TEST);

    csvTargetFile = new File(csvTargetDir, FILE_RESOUCE_NAME);
    FileUtils.copyFile(ScenariosCommons.CSV_SOURCE_FILE, csvTargetFile);


    importObject(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_FILE,true);
    importObject(ScenariosCommons.USER_TEST_RAPHAEL_FILE,true);
    changeResourceAttribute(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csvTargetFile.getAbsolutePath());
  }

  @Test (dependsOnMethods ={INITIALIZE_TEST_CONFIGURATION_DEPENDENCY})
  public void iterateForUniqueAttribute(){
 ListUsersPage listUsersPage = basicPage.listUsers();
      listUsersPage
              .table()
                .search()
                  .byName()
                  .inputValue(ScenariosCommons.TEST_USER_RAPHAEL_NAME)
                .updateSearch()
              .and()
              .clickByName(ScenariosCommons.TEST_USER_RAPHAEL_NAME)
                .selectTabProjections()
                  .clickHeaderActionDropDown()
                    .addProjection()
                      .table()
                        .search()
                          .byName()
                          .inputValue(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
                        .updateSearch()
                      .and()
                      .selectCheckboxByName(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME)
                    .and()
                .clickAdd()
              .and()
                .clickSave()
                .feedback()
                .isSuccess();

    listUsersPage = basicPage.listUsers();

     listUsersPage
              .table()
                .search()
                  .byName()
                  .inputValue(ScenariosCommons.TEST_USER_RAPHAEL_NAME)
                .updateSearch()
              .and()
              .clickByName(ScenariosCommons.TEST_USER_RAPHAEL_NAME)
                .selectTabProjections()
                  .table()
                    .clickByName(ScenariosCommons.RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME);

    }

}
