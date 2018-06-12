package schrodinger.scenarios;

import com.evolveum.midpoint.schrodinger.page.self.HomePage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by matus on 5/9/2018.
 */
public class UserAccountTests extends AccountTests {

    private static final String DISABLE_MP_USER_DEPENDENCY = "disableUser";
    private static final String ENABLE_MP_USER_DEPENDENCY = "enableUser";
    private static final String BULK_DISABLE_MP_USER_DEPENDENCY = "bulkDisableUsers";



    @Test (dependsOnMethods = {CREATE_MP_USER_DEPENDENCY}, groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void modifyUserAttribute(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                    .table()
                        .search()
                        .byName()
                            .inputValue(TEST_USER_MIKE_NAME)
                            .updateSearch()
                    .and()
                    .clickByName(TEST_USER_MIKE_NAME)
                        .selectTabBasic()
                            .form()
                            .changeAttributeValue("Given name","Michelangelo","Michael Angelo")
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess();
    }
    @Test (dependsOnMethods = {CREATE_MP_USER_DEPENDENCY, MODIFY_ACCOUNT_PASSWORD_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void modifyUserPassword(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                    .table()
                        .search()
                        .byName()
                            .inputValue(TEST_USER_MIKE_NAME)
                            .updateSearch()
                    .and()
                    .clickByName(TEST_USER_MIKE_NAME)
                        .selectTabBasic()
                            .form()
                            .showEmptyAttributes("Password")
                            .addProtectedAttributeValue("Value","S36re7")
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess();

    }

    @Test (dependsOnMethods = {CREATE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void disableUser(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                .table()
                    .search()
                    .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                        .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabBasic()
                        .form()
                        .showEmptyAttributes("Activation")
                        .selectOption("Administrative status","Disabled")
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();
    }

    @Test (dependsOnMethods = {DISABLE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void enableUser(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                .table()
                    .search()
                    .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                        .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabBasic()
                        .form()
                        .showEmptyAttributes("Activation")
                        .selectOption("Administrative status","Enabled")
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();
    }

    @Test (dependsOnMethods = {ENABLE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void bulkDisableUsers(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .selectAll()
                    .clickActionDropDown()
                        .clickDisable()
                    .clickYes()
                .and()
            .and()
                .feedback()
                .isSuccess()
            ;
    }

    @Test (dependsOnMethods = {BULK_DISABLE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void bulkEnableUsers(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .selectAll()
                    .clickEnable()
                .clickYes()
            .and()
                .feedback()
                .isSuccess()
        ;
    }

    @Test (dependsOnMethods = {CREATE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void searchUser(){
    ListUsersPage usersPage = basicPage.listUsers();
    Assert.assertTrue(
               usersPage
                       .table()
                            .search()
                                .byName()
                                .inputValue(TEST_USER_MIKE_NAME)
                            .updateSearch()
                       .and()
                       .currentTableContains(TEST_USER_MIKE_NAME)
       );
    }

    @Test (dependsOnMethods = {CREATE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void searchUserFromHome(){
        HomePage homePage = basicPage.home();
        Assert.assertTrue(
                homePage
                    .search()
                        .clickSearchFor()
                    .clickUsers()
                    .inputValue(TEST_USER_MIKE_NAME)
                        .clickSearch()
                        .currentTableContains(TEST_USER_MIKE_NAME)
        );

    }

    @Test (dependsOnGroups = {TEST_GROUP_BEFORE_USER_DELETION})
    public void bulkDeleteUsers(){
        ListUsersPage usersPage = basicPage.listUsers();
        usersPage
                .table()
                    .search()
                        .byName()
                    .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .selectAll()
                    .clickActionDropDown()
                        .clickDelete()
                    .clickYes()
                .and()
        .and()
            .feedback()
            .isSuccess()
        ;}
}
