package schrodinger.scenarios;

import com.codeborne.selenide.Selenide;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import schrodinger.TestBase;

import java.io.File;
import java.io.IOException;

/**
 * Created by matus on 5/21/2018.
 */
public class SynchronizationTests extends TestBase {

    private static final File CSV_SOURCE_FILE = new File("./src/test/resources/midpoint-gorups-authoritative.csv");
    private static final File CSV_INITIAL_SOURCE_FILE = new File("./src/test/resources/midpoint-gorups-authoritative-initial.csv");
    private static final File CSV_UPDATED_SOURCE_FILE = new File("./src/test/resources/midpoint-gorups-authoritative-updated.csv");
    private static final File CSV_TARGET_FILE = new File("C:\\Users\\matus\\Documents\\apache-tomcat-8.5.16\\target\\midpoint-advanced-sync.csv"); //TODO change hard coded path to local web container
    private static final File RESOURCE_CSV_GROUPS_AUTHORITATIVE_FILE= new File("./src/test/resources/resource-csv-groups-authoritative.xml");

    protected static final String TEST_USER_DON_NAME= "donatello";

    private static final String RESOURCE_AND_SYNC_TASK_SETUP_DEPENDENCY = "setUpResourceAndSynchronizationTask";
    private static final String NEW_USER_AND_ACCOUNT_CREATED_DEPENDENCY = "newResourceAccountUserCreated";
    private static final String NEW_USER_ACCOUNT_CREATED_LINKED_DEPENDENCY = "newResourceAccountCreatedLinked";

    @BeforeSuite
    private void init() throws IOException {
        FileUtils.copyFile(CSV_INITIAL_SOURCE_FILE,CSV_TARGET_FILE);

    }

    @Test
    public void setUpResourceAndSynchronizationTask(){
        importObject(RESOURCE_CSV_GROUPS_AUTHORITATIVE_FILE,true);
        importObject(OrganizationStructureTests.USER_TEST_RAPHAEL_FILE,true);

         ListResourcesPage listResourcesPage = basicPage.listResources();

        Assert.assertTrue(
                listResourcesPage
                        .table()
                            .clickByName("CSV (target with groups) authoritative")
                                .clickEditResourceConfiguration()
                                    .form()
                                    .changeAttributeValue("File path" ,AccountTests.CSV_SOURCE_OLDVALUE, CSV_TARGET_FILE.getAbsolutePath())
                                .and()
                            .and()
                                .clickSaveAndTestConnection()
                                    .isTestSuccess()
        );

        listResourcesPage = basicPage.listResources();
        listResourcesPage
                .table()
                    .clickByName("CSV (target with groups) authoritative")
                        .clicAccountsTab()
                        .liveSyncTask()
                            .clickCreateNew()
                                .basicTable()
                                    .addAttributeValue("Task name","LiveSyncTest")
                            .and()
                                .schedulingTable()
                                    .clickCheckBox("Recurring task")
                                    .addAttributeValue("Schedule interval (seconds)","3")
                            .and()
                                .clickSave()
                                    .feedback()
                                    .isSuccess();

    }

    @Test (dependsOnMethods = {RESOURCE_AND_SYNC_TASK_SETUP_DEPENDENCY})
    public void newResourceAccountUserCreated() throws IOException {
    FileUtils.copyFile(CSV_SOURCE_FILE,CSV_TARGET_FILE);
        Selenide.sleep(3000);

        ListUsersPage usersPage = basicPage.listUsers();
        Assert.assertTrue(
              usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(TEST_USER_DON_NAME)
                    .updateSearch()
                .and()
                .currentTableContains(TEST_USER_DON_NAME)
        );
    }

    @Test (dependsOnMethods = {NEW_USER_AND_ACCOUNT_CREATED_DEPENDENCY})
    public void newResourceAccountCreatedLinked() throws IOException {

        ListUsersPage usersPage = basicPage.listUsers();
        usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(TEST_USER_DON_NAME)
                    .updateSearch()
                .and()
                    .clickByName(TEST_USER_DON_NAME)
                        .selectTabProjections()
                            .table()
                                .selectCheckboxByName("CSV (target with groups) authoritative")
                        .and()
                            .clickCog()
                                .delete()
                                .clickYes()
                        .and()
                    .and()
                        .clickSave()
                            .feedback()
                            .isSuccess();

        Selenide.sleep(3000);
        FileUtils.copyFile(CSV_SOURCE_FILE,CSV_TARGET_FILE);
        Selenide.sleep(3000);


             usersPage = basicPage.listUsers();
      Assert.assertTrue(
              usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(TEST_USER_DON_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_DON_NAME)
                      .selectTabProjections()
                        .table()
                        .currentTableContains("CSV (target with groups) authoritative")
      );

    }

    @Test (dependsOnMethods = {NEW_USER_ACCOUNT_CREATED_LINKED_DEPENDENCY})
    public void alreadyLinkedResourceAccountModified() throws IOException {

        FileUtils.copyFile(CSV_UPDATED_SOURCE_FILE,CSV_TARGET_FILE);
        Selenide.sleep(3000);

        ListUsersPage usersPage = basicPage.listUsers();
     Assert.assertTrue(
            usersPage
                    .table()
                        .search()
                            .byName()
                            .inputValue(TEST_USER_DON_NAME)
                        .updateSearch()
                    .and()
                    .clickByName(TEST_USER_DON_NAME)
                        .selectTabBasic()
                            .form()
                                .compareAttibuteValue("Given name","Donato")
     );
    }
//TODO checkbox selection not ideal rethink!
    @Test (dependsOnMethods = {RESOURCE_AND_SYNC_TASK_SETUP_DEPENDENCY})
    public void resourceAccountDeleted(){

        ListUsersPage usersPage = basicPage.listUsers();
         Assert.assertFalse(
                 usersPage
                    .table()
                        .search()
                            .byName()
                            .inputValue("raphael")
                        .updateSearch()
                    .and()
                        .clickByName("raphael")
                            .selectTabProjections()
                                .table()
                                .currentTableContains("CSV (target with groups) authoritative")
         );

        ListResourcesPage resourcesPage = basicPage.listResources();
        Assert.assertFalse(
                resourcesPage
                    .table()
                        .search()
                            .byName()
                            .inputValue("CSV (target with groups) authoritative")
                        .updateSearch()
                    .and()
                    .clickByName("CSV (target with groups) authoritative")
                        .clicAccountsTab()
                        .clickSearchInResource()
                            .table()
                            .selectCheckboxByName("raphael")
                                .clickCog()
                                    .clickDelete()
                            .clickYes()
                            .currentTableContains("raphael")
        );

         Assert.assertFalse(
                 usersPage
                    .table()
                        .search()
                            .byName()
                            .inputValue("raphael")
                        .updateSearch()
                    .and()
                        .clickByName("raphael")
                            .selectTabProjections()
                                .table()
                                .currentTableContains("CSV (target with groups) authoritative")
         );

    }

}
