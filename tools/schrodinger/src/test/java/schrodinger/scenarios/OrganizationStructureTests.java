package schrodinger.scenarios;

import com.codeborne.selenide.Selenide;
import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
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
 * Created by matus on 5/11/2018.
 */
public class OrganizationStructureTests extends TestBase {

    private static final File CSV_RESOURCE_ADVANCED_SYNC = new File("../../samples/resources/csv/resource-csv-groups.xml");

    protected static final File CSV_SOURCE_FILE = new File("../../samples/resources/csv/midpoint-groups.csv");
    protected static final File CSV_TARGET_FILE = new File("C:\\Users\\matus\\Documents\\apache-tomcat-8.5.16\\target\\midpoint-advanced-sync.csv"); //TODO change hard coded path to local web container
    protected static final File ORG_ACCOUNT_INDUCEMENT_FILE = new File("./src/test/resources/org-account-inducement.xml");
    protected static final File ORG_MONKEY_ISLAND_SOURCE_FILE = new File("../../samples/org/org-monkey-island-simple.xml");

    protected static final File USER_TEST_RAPHAEL_FILE = new File("./src/test/resources/user-raphael.xml");

    protected static final String CSV_SOURCE_OLDVALUE = "target/midpoint.csv";

    private static final String TEST_USER_GUYBRUSH_NAME = "guybrush";
    private static final String TEST_USER_RAPHAEL_NAME = "raphael";

    private static final String NAME_ORG_UNIT_ASSIGN= "P0001";
    private static final String NAME_ORG_UNIT_UNASSIGN= "Save Elaine";
    private static final String NAME_ORG_UNIT_ASSIGN_AND_INDUCE= "testOrgUnit";

    private static final String TYPE_SELECTOR_ORG= "Org";

    private static final String IMPORT_ORG_STRUCT_DEPENDENCY = "importOrgStructure";
    private static final String ASSIGN_ORG_UNIT_DEPENDENCY = "assignOrgUnit";


    @BeforeSuite
    private void init() throws IOException {
        FileUtils.copyFile(CSV_SOURCE_FILE,CSV_TARGET_FILE);
    }

    @Test
    public void importOrgStructure(){
        ImportObjectPage importPage = basicPage.importObject();
        Assert.assertTrue(
                importPage
                    .getObjectsFromFile()
                    .chooseFile(ORG_MONKEY_ISLAND_SOURCE_FILE)
                    .clickImport()
                        .feedback()
                        .isSuccess()
        );
    }

    @Test (dependsOnMethods ={IMPORT_ORG_STRUCT_DEPENDENCY})
    public void assignOrgUnit(){
         ListUsersPage users = basicPage.listUsers();
            users
                .table()
                    .search()
                    .byName()
                    .inputValue(TEST_USER_GUYBRUSH_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_GUYBRUSH_NAME)
                    .selectTabAssignments()
                        .clickAddAssignemnt()
                            .selectType(TYPE_SELECTOR_ORG)
                            .table()
                                .search()
                                    .byName()
                                    .inputValue(NAME_ORG_UNIT_ASSIGN)
                                .updateSearch()
                            .and()
                            .selectCheckboxByName(NAME_ORG_UNIT_ASSIGN)
                        .and()
                    .clickAdd()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess()
        ;

    }

    @Test (dependsOnMethods ={ASSIGN_ORG_UNIT_DEPENDENCY})
    public void unassignOrgUnit(){
        ListUsersPage users = basicPage.listUsers();
            users
                .table()
                    .search()
                    .byName()
                    .inputValue(TEST_USER_GUYBRUSH_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_GUYBRUSH_NAME)
                    .selectTabAssignments()
                        .table()
                        .unassignByName(NAME_ORG_UNIT_UNASSIGN)
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();
    }

    @Test
    public void orgUnitAccountInducement(){
        importObject(CSV_RESOURCE_ADVANCED_SYNC);
        importObject(ORG_ACCOUNT_INDUCEMENT_FILE);
        importObject(USER_TEST_RAPHAEL_FILE);

        changeResourceFilePath();

         ListUsersPage users = basicPage.listUsers();
            users
                .table()
                    .search()
                    .byName()
                    .inputValue(TEST_USER_RAPHAEL_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_RAPHAEL_NAME)
                    .selectTabAssignments()
                        .clickAddAssignemnt()
                            .selectType(TYPE_SELECTOR_ORG)
                            .table()
                                .search()
                                    .byName()
                                    .inputValue(NAME_ORG_UNIT_ASSIGN_AND_INDUCE)
                                .updateSearch()
                            .and()
                            .selectCheckboxByName(NAME_ORG_UNIT_ASSIGN_AND_INDUCE)
                        .and()
                    .clickAdd()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();
    }

    public void changeResourceFilePath(){
        ListResourcesPage listResourcesPage = basicPage.listResources();

        Assert.assertTrue(listResourcesPage
                .table()
                .clickByName("CSV (target with groups)")
                    .clickEditResourceConfiguration()
                        .form()
                        .changeAttributeValue("File path",CSV_SOURCE_OLDVALUE,CSV_TARGET_FILE.getAbsolutePath())
                    .and()
                .and()
                .clickSaveAndTestConnection()
                .isTestSuccess()
        );
    }


}
