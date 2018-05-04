package schrodinger.core;

import com.codeborne.selenide.Selenide;
import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import schrodinger.TestBase;

import java.io.File;
import java.io.IOException;

/**
 * Created by matus on 3/22/2018.
 */
public class AccountTests extends TestBase {

private static final File CSV_RESOURCE_MEDIUM = new File("../../samples/resources/csv/resource-csv-username.xml");

    private static final File CSV_SOURCE_FILE = new File("../../samples/resources/csv/midpoint-username.csv");
    private static final File CSV_TARGET_FILE = new File("C:\\Users\\matus\\Documents\\apache-tomcat-8.5.16\\target\\midpoint.csv"); //TODO change hard coded path to local web container

    private static final String CSV_SOURCE_OLDVALUE = "target/midpoint.csv";

    private static final String IMPORT_CSV_RESOURCE_DEPENDENCY= "importCsvResource";
    private static final String CREATE_MP_USER_DEPENDENCY= "createMidpointUser";
    private static final String CHANGE_RESOURCE_FILE_PATH_DEPENDENCY= "changeResourceFilePath";

    private static final String CSV_RESOURCE_NAME= "Test CSV: username";


    @BeforeMethod
    private void init() throws IOException {
        FileUtils.copyFile(CSV_SOURCE_FILE,CSV_TARGET_FILE);
    }

    @Test(priority = 1)
    public void createMidpointUser(){
        UserPage user = basicPage.newUser();

        Assert.assertTrue(user.selectTabBasic()
                    .form()
                        .addAttributeValue("name", "michelangelo")
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

    @Test
    public void importCsvResource(){
        ImportObjectPage importPage = basicPage.importObject();

        Assert.assertTrue(importPage
                .getObjectsFromFile()
                .chooseFile(CSV_RESOURCE_MEDIUM)
                .clickImport()
                .feedback()
                .isSuccess()
        );
    }


    @Test (dependsOnMethods = {IMPORT_CSV_RESOURCE_DEPENDENCY})
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

    @Test(dependsOnMethods = {CREATE_MP_USER_DEPENDENCY,CHANGE_RESOURCE_FILE_PATH_DEPENDENCY})
    public void addAccount() {
        ListUsersPage users = basicPage.listUsers();

        Assert.assertTrue(users
                .table()
                    .search()
                    .byName()
                    .inputValue("michelangelo")
                    .updateSearch()
                .and()
                .clickByName("michelangelo")
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

    public void modifyAccountAttribute(){}
}
