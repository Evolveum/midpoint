package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.annotations.Test;

import java.io.File;

public class ObjectTemplateTests extends AbstractSchrodingerTest {

    private static final String COMPONENT_OBJECTS_DIRECTORY = "./src/test/resources/component/objects/";
    private static final String COMPONENT_OBJECT_TEMPLATE = COMPONENT_OBJECTS_DIRECTORY + "objectTemplate/";
    private static final File SEARCH_CONFIG_SYSTEM_CONFIG_FILE = new File(COMPONENT_OBJECT_TEMPLATE + "object-template-example-user-simple.xml");

    @Test
    public void test00100addObjectTemplateTest() {
        basicPage
                .newUser()
                    .selectTabBasic()
                        .form()
                            .addAttributeValue("Name", "userWithoutFullname")
                            .addAttributeValue("Given name", "Mark")
                            .addAttributeValue("Family name", "Sadman")
                        .and()
                    .and()
                    .clickSave()
                        .feedback()
                        .assertSuccess();
        basicPage
                .listUsers()
                    .table()
                        .search()
                            .byName()
                            .inputValue("userWithoutFullname")
                            .updateSearch()
                        .and()
                        .assertTableContainsColumnWithValue("UserType.familyName", "Sadman")
                        .assertTableContainsColumnWithValue("UserType.givenName", "Mark")
                        .assertTableColumnValueIsEmpty("UserType.fullName");

        addObjectFromFile(SEARCH_CONFIG_SYSTEM_CONFIG_FILE, true);
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);

        basicPage
                .newUser()
                    .selectTabBasic()
                        .form()
                            .addAttributeValue("Name", "userWithFullname")
                            .addAttributeValue("Given name", "Emil")
                            .addAttributeValue("Family name", "Happyman")
                            .and()
                        .and()
                    .clickSave()
                        .feedback()
                            .assertSuccess();
        basicPage
                .listUsers()
                    .table()
                        .search()
                            .byName()
                            .inputValue("userWithFullname")
                            .updateSearch()
                            .and()
                        .assertTableContainsColumnWithValue("UserType.givenName", "Emil")
                        .assertTableContainsColumnWithValue("UserType.familyName", "Happyman")
                        .assertTableContainsColumnWithValue("UserType.fullName", "Emil Happyman");
    }
}
