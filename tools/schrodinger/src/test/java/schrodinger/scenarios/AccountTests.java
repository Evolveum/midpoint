package schrodinger.scenarios;

import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import schrodinger.TestBase;

import java.io.File;
import java.io.IOException;

/**
 * Created by matus on 3/22/2018.
 */
public class AccountTests extends TestBase {

    private static final File CSV_RESOURCE_MEDIUM = new File("../../samples/resources/csv/resource-csv-username.xml");

    protected static final File CSV_SOURCE_FILE = new File("../../samples/resources/csv/midpoint-username.csv");
    protected static final File CSV_TARGET_FILE = new File("C:\\Users\\matus\\Documents\\apache-tomcat-8.5.16\\target\\midpoint.csv"); //TODO change hard coded path to local web container

    protected static final String CSV_SOURCE_OLDVALUE = "target/midpoint.csv";

    protected static final String IMPORT_CSV_RESOURCE_DEPENDENCY= "importCsvResource";
    protected static final String CREATE_MP_USER_DEPENDENCY= "createMidpointUser";
    protected static final String CHANGE_RESOURCE_FILE_PATH_DEPENDENCY= "changeResourceFilePath";
    protected static final String ADD_ACCOUNT_DEPENDENCY= "addAccount";
    protected static final String DISABLE_ACCOUNT_DEPENDENCY= "disableAccount";
    protected static final String ENABLE_ACCOUNT_DEPENDENCY= "enableAccount";
    protected static final String MODIFY_ACCOUNT_PASSWORD_DEPENDENCY= "modifyAccountPassword";


    protected static final String TEST_GROUP_BEFORE_USER_DELETION = "beforeDelete";

    protected static final String CSV_RESOURCE_NAME= "Test CSV: username";

    protected static final String TEST_USER_MIKE_NAME= "michelangelo";
    protected static final String TEST_USER_MIKE_LAST_NAME_OLD= "di Lodovico Buonarroti Simoni";
    protected static final String TEST_USER_MIKE_LAST_NAME_NEW= "di Lodovico Buonarroti Simoni Il Divino";


    @BeforeSuite
    private void init() throws IOException {
        FileUtils.copyFile(CSV_SOURCE_FILE,CSV_TARGET_FILE);
    }

    @Test(priority = 1, groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void createMidpointUser(){
        UserPage user = basicPage.newUser();

        Assert.assertTrue(user.selectTabBasic()
                    .form()
                        .addAttributeValue("name", TEST_USER_MIKE_NAME)
                        .addAttributeValue(UserType.F_GIVEN_NAME, "Michelangelo")
                        .addAttributeValue(UserType.F_FAMILY_NAME, "di Lodovico Buonarroti Simoni")
                        .and()
                    .and()
                .checkKeepDisplayingResults()
                    .clickSave()
                    .feedback()
                    .isSuccess()
        );
    }

    @Test(groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void importCsvResource(){
        ImportObjectPage importPage = basicPage.importObject();

        Assert.assertTrue(
                importPage
                .getObjectsFromFile()
                .chooseFile(CSV_RESOURCE_MEDIUM)
                .clickImport()
                    .feedback()
                    .isSuccess()
        );
    }


    @Test (dependsOnMethods = {IMPORT_CSV_RESOURCE_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void changeResourceFilePath(){
        ListResourcesPage listResourcesPage = basicPage.listResources();

        Assert.assertTrue(listResourcesPage
                .table()
                .clickByName("Test CSV: username")
                    .clickEditResourceConfiguration()
                        .form()
                        .changeAttributeValue("File path",CSV_SOURCE_OLDVALUE,CSV_TARGET_FILE.getAbsolutePath())
                    .and()
                .and()
                .clickSaveAndTestConnection()
                .isTestSuccess()
        );
    }

    @Test(dependsOnMethods = {CREATE_MP_USER_DEPENDENCY,CHANGE_RESOURCE_FILE_PATH_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void addAccount() {
        ListUsersPage users = basicPage.listUsers();

        Assert.assertTrue(users
                .table()
                    .search()
                    .byName()
                    .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabProjections()
                    .clickCog()
                    .addProjection()
                            .projectionsTable()
                            .selectCheckboxByName(CSV_RESOURCE_NAME)
                        .and()
                        .clickAdd()
                    .and()
                    .checkKeepDisplayingResults()
                        .clickSave()
                        .feedback()
                        .isSuccess()
        );
    }

    @Test (dependsOnMethods = {ADD_ACCOUNT_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void modifyAccountAttribute(){
        ListUsersPage users = basicPage.listUsers();
                users
                    .table()
                        .search()
                        .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                        .updateSearch()
                    .and()
                    .clickByName(TEST_USER_MIKE_NAME)
                        .selectTabProjections()
                            .table()
                            .clickByName(CSV_RESOURCE_NAME)
                                .changeAttributeValue("lastname",TEST_USER_MIKE_LAST_NAME_OLD,TEST_USER_MIKE_LAST_NAME_NEW)
                            .and()
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                        .clickSave()
                        .feedback()
            ;

    }
    @Test (dependsOnMethods = {ADD_ACCOUNT_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void modifyAccountPassword(){
        ListUsersPage users = basicPage.listUsers();
            users
                .table()
                    .search()
                    .byName()
                    .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabProjections()
                        .table()
                        .clickByName(CSV_RESOURCE_NAME)
                            .showEmptyAttributes("Password")
                            .addProtectedAttributeValue("Value","5ecr3t")
                        .and()
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();



    }
    @Test (dependsOnMethods = {ADD_ACCOUNT_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void disableAccount(){
        ListUsersPage users = basicPage.listUsers();
            users
                .table()
                    .search()
                    .byName()
                    .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabProjections()
                        .table()
                        .clickByName(CSV_RESOURCE_NAME)
                            .selectOption("Administrative status","Disabled")
                        .and()
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();
    }

    @Test (dependsOnMethods = {ADD_ACCOUNT_DEPENDENCY, DISABLE_ACCOUNT_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void enableAccount(){
        ListUsersPage users = basicPage.listUsers();
            users
                .table()
                    .search()
                    .byName()
                    .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabProjections()
                        .table()
                        .clickByName(CSV_RESOURCE_NAME)
                            .selectOption("Administrative status","Enabled")
                        .and()
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();
    }

    @Test(dependsOnMethods = {ENABLE_ACCOUNT_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void deleteAccount(){
        ListUsersPage users = basicPage.listUsers();
                users
                    .table()
                        .search()
                        .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                        .updateSearch()
                    .and()
                    .clickByName(TEST_USER_MIKE_NAME)
                        .selectTabProjections()
                            .table()
                            .selectCheckboxByName(CSV_RESOURCE_NAME)
                        .and()
                            .clickCog()
                            .delete()
                                .clickYes()
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess();
    }

}
