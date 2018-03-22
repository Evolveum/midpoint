package schrodinger.core;

import com.codeborne.selenide.Selenide;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.NewUserPage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.Assert;
import org.testng.annotations.Test;
import schrodinger.TestBase;

/**
 * Created by matus on 3/22/2018.
 */
public class AccountTests extends TestBase {



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
            Selenide.sleep(5000);
    }
}
