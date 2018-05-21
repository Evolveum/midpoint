package schrodinger.scenarios;

import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.Assert;
import org.testng.annotations.Test;
import schrodinger.TestBase;

import java.io.File;

/**
 * Created by matus on 5/11/2018.
 */
public class UserPhotoTests extends TestBase {

    private static final String TEST_USER_LEO_NAME= "leonardo";
    protected static final File PHOTO_SOURCE_FILE_LARGE = new File("C:\\Users\\matus\\Documents\\apache-tomcat-8.5.16\\target\\leonardo_large.jpg");
    protected static final File PHOTO_SOURCE_FILE_SMALL= new File("C:\\Users\\matus\\Documents\\apache-tomcat-8.5.16\\target\\leonardo_small.jpg");

    protected static final String CREATE_USER_WITH_LARGE_PHOTO_DEPENDENCY = "createMidpointUserWithPhotoLarge";
    protected static final String CREATE_USER_WITH_NORMAL_PHOTO_DEPENDENCY = "createMidpointUserWithPhotoJustRight";

    @Test
    public void createMidpointUserWithPhotoLarge(){
        UserPage user = basicPage.newUser();

        Assert.assertTrue(
                user
                    .selectTabBasic()
                        .form()
                        .addAttributeValue("name", TEST_USER_LEO_NAME)
                        .addAttributeValue(UserType.F_GIVEN_NAME, "Leonardo")
                        .addAttributeValue(UserType.F_FAMILY_NAME, "di ser Piero da Vinci")
                        .setFileForUploadAsAttributeValue("Jpeg photo", PHOTO_SOURCE_FILE_LARGE)
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isError()
        );
    }

    @Test (dependsOnMethods = {CREATE_USER_WITH_LARGE_PHOTO_DEPENDENCY})
    public void createMidpointUserWithPhotoJustRight(){
        UserPage user = basicPage.newUser();
        Assert.assertTrue(
                    user
                        .selectTabBasic()
                            .form()
                            .addAttributeValue("name", TEST_USER_LEO_NAME)
                            .addAttributeValue(UserType.F_GIVEN_NAME, "Leonardo")
                            .addAttributeValue(UserType.F_FAMILY_NAME, "di ser Piero da Vinci")
                            .setFileForUploadAsAttributeValue("Jpeg photo", PHOTO_SOURCE_FILE_SMALL)
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess()
        );
    }

    @Test (dependsOnMethods = {CREATE_USER_WITH_NORMAL_PHOTO_DEPENDENCY})
    public void deleteUserPhoto(){
         ListUsersPage usersPage = basicPage.listUsers();
         Assert.assertTrue(
                 usersPage
                    .table()
                        .search()
                        .byName()
                            .inputValue(TEST_USER_LEO_NAME)
                            .updateSearch()
                    .and()
                    .clickByName(TEST_USER_LEO_NAME)
                        .selectTabBasic()
                            .form()
                            .removeFileAsAttributeValue("Jpeg photo")
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess()
         );
    }
}
