package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ObjectTemplateTests extends AbstractSchrodingerTest {

    private static final String COMPONENT_OBJECTS_DIRECTORY = "./src/test/resources/configuration/objects/";
    private static final String COMPONENT_OBJECT_TEMPLATE = COMPONENT_OBJECTS_DIRECTORY + "objecttemplate/";
    private static final String COMPONENT_SYSTEM_CONFIGURATION = COMPONENT_OBJECTS_DIRECTORY + "systemconfig/";
    private static final File FULL_NAME_OBJECT_TEMPLATE_FILE = new File(COMPONENT_OBJECT_TEMPLATE + "object-template-full-name.xml");
    private static final File SYSTEM_CONFIGURATION_WITH_OBJ_TEMPLATE_FILE = new File(COMPONENT_SYSTEM_CONFIGURATION + "system-configuration-with-object-template.xml");
    private static final File SYSTEM_CONFIGURATION_INITIAL_FILE = new File(COMPONENT_SYSTEM_CONFIGURATION + "system-configuration-initial.xml");

    @Test
    public void test00100addObjectTemplateCreateUser() {
        Map<String, String> attributesMap = new HashMap<>();
        attributesMap.put("Name", "userWithoutFullname");
        attributesMap.put("Given name", "Mark");
        attributesMap.put("Family name", "Sadman");
        createUser(attributesMap);

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

        addObjectFromFile(FULL_NAME_OBJECT_TEMPLATE_FILE, true);
        addObjectFromFile(SYSTEM_CONFIGURATION_WITH_OBJ_TEMPLATE_FILE, true);
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);

        basicPage.loggedUser().logout();
        FormLoginPage loginPage = midPoint.formLogin();
        loginPage.login(getUsername(), getPassword());

        attributesMap.clear();
        attributesMap.put("Name", "userWithFullname");
        attributesMap.put("Given name", "Emil");
        attributesMap.put("Family name", "Happyman");
        createUser(attributesMap);

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

    @Test (dependsOnMethods = {"test00100createUserWithObjectTemplateTest"})
    public void test00200deleteObjectTemplateCreateUser() {

    }

    @Test
    public void test00300createArchetypeObjectTemplateExists() {

    }

}
