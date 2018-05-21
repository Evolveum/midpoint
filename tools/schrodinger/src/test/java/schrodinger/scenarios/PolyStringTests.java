package schrodinger.scenarios;

import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.Assert;
import org.testng.annotations.Test;
import schrodinger.TestBase;

/**
 * Created by matus on 5/21/2018.
 */
public class PolyStringTests extends TestBase {

    private static final String TEST_USER_JOZKO_NAME = "džordž";
    private static final String TEST_USER_JOZKO_NAME_NO_DIAC = "dzordz";
    private static final String TEST_USER_JOZKO_GIVEN_NAME = "Jožko";
    private static final String TEST_USER_JOZKO_FAMILY_NAME = "Mrkvička";
    private static final String TEST_USER_JOZKO_FULL_NAME = "Jožko Jörg Nguyễn Trißtan Guðmund Mrkvička";
    private static final String TEST_USER_JOZKO_ADDITIONAL_NAME = "Jörg Nguyễn Trißtan Guðmund ";

    @Test
    public void createUserWithDiacritic(){
        UserPage user = basicPage.newUser();

        Assert.assertTrue(user.selectTabBasic()
                    .form()
                        .addAttributeValue("name", TEST_USER_JOZKO_NAME)
                        .addAttributeValue(UserType.F_GIVEN_NAME, TEST_USER_JOZKO_GIVEN_NAME)
                        .addAttributeValue(UserType.F_FAMILY_NAME, TEST_USER_JOZKO_FAMILY_NAME)
                        .addAttributeValue(UserType.F_FULL_NAME,TEST_USER_JOZKO_FULL_NAME)
                        .addAttributeValue(UserType.F_ADDITIONAL_NAME,TEST_USER_JOZKO_ADDITIONAL_NAME)
                        .and()
                    .and()
                .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess()
        );

    }

    @Test
    public void searchForUserWithDiacritic(){

        ListUsersPage usersPage = basicPage.listUsers();
        Assert.assertTrue(
               usersPage
                       .table()
                            .search()
                                .byName()
                                .inputValue(TEST_USER_JOZKO_NAME)
                            .updateSearch()
                       .and()
                       .currentTableContains(TEST_USER_JOZKO_NAME)
       );
               usersPage
                       .table()
                            .search()
                                .byName()
                                .inputValue(TEST_USER_JOZKO_NAME_NO_DIAC)
                            .updateSearch()
                       .and()
                       .currentTableContains(TEST_USER_JOZKO_NAME_NO_DIAC);

    }

}
