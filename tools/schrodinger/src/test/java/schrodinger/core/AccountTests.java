package schrodinger.core;

import com.codeborne.selenide.Selenide;
import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.NewUserPage;
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

    @BeforeMethod
    private void init() throws IOException {
        FileUtils.copyFile(CSV_SOURCE_FILE,CSV_TARGET_FILE);
    }

    @Test
    public void createMidpointUser(){
        NewUserPage user = basicPage.newUser();

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
    public void listMidpointUser() {
        ListUsersPage users = basicPage.listUsers();
            users
                .table()
                .search()
                .byName()
                .inputValue("michelangelo")
                .updateSearch();

    }

    @Test
    public void importCsvResource(){
        ImportObjectPage importPage = basicPage.importObject();
            importPage
                    .getObjectsFromFile()
                    .chooseFile(CSV_RESOURCE_MEDIUM)
                    .clickImport()
                    .feedback()
                    .isSuccess();

    }
    @Test
    public void changeResourceFilePath(){

        ListResourcesPage listResourcesPage = basicPage.listResources();
            listResourcesPage
                    .table()
                    .clickByName("Test CSV: username")
                    .clickEditResourceConfiguration()
                    .form()
                    .changeAttributeValue("File path",CSV_SOURCE_OLDVALUE,CSV_TARGET_FILE.getAbsolutePath())
                    .and()
                .and()
                .clickSaveAndTestConnection()
                .isTestSuccess()
            ;

            Selenide.sleep(5000);

    }
}
