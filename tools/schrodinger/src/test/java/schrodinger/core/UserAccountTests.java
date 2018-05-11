package schrodinger.core;

import com.evolveum.midpoint.schrodinger.page.self.HomePage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by matus on 5/9/2018.
 */
public class UserAccountTests extends AccountTests {


    @Test
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
    @Test
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
                            .addProtectedAttributeValue("Value","5ecr3t")
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess();

    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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
