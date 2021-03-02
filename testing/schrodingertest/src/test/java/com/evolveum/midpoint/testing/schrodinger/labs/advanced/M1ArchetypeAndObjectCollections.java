package com.evolveum.midpoint.testing.schrodinger.labs.advanced;

import com.evolveum.midpoint.schrodinger.util.Utils;
import com.evolveum.midpoint.testing.schrodinger.labs.AbstractLabTest;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class M1ArchetypeAndObjectCollections extends AbstractLabTest {
    protected static final String LAB_OBJECTS_DIRECTORY = LAB_ADVANCED_DIRECTORY + "M1/";
    private static final File OBJECT_COLLECTION_ACTIVE_EMP_FILE = new File(LAB_OBJECTS_DIRECTORY + "objectcollections/objectCollection-active-employees.xml");
    private static final File OBJECT_COLLECTION_INACTIVE_EMP_FILE = new File(LAB_OBJECTS_DIRECTORY + "objectcollections/objectCollection-inactive-employees.xml");
    private static final File OBJECT_COLLECTION_FORMER_EMP_FILE = new File(LAB_OBJECTS_DIRECTORY + "objectcollections/objectCollection-former-employees.xml");
    private static final File ARCHETYPE_EMPLOYEE_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-employee.xml");
    private static final File ARCHETYPE_EXTERNAL_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-external.xml");
    private static final File KIRK_USER_FILE = new File("./src/test/resources/labs/objects/users/kirk-user.xml");
    private static final File SECRET_I_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-secret-i.xml");
    private static final File SECRET_II_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-secret-ii.xml");
    private static final File INTERNAL_EMPLOYEE_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-internal-employee.xml");
    private static final File CSV_1_SIMPLE_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access-simple.xml");
    private static final File CSV_2_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-2-canteen.xml");
    private static final File CSV_3_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap.xml");

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextPrepareTestInstance" })
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
        csv1TargetFile = new File(getTestTargetDir(), CSV_1_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_1_SOURCE_FILE, csv1TargetFile);
        csv2TargetFile = new File(getTestTargetDir(), CSV_2_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_2_SOURCE_FILE, csv2TargetFile);
        csv3TargetFile = new File(getTestTargetDir(), CSV_3_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_3_SOURCE_FILE, csv3TargetFile);
    }

    @Test(groups={"advancedM1"})
    public void mod01test01environmentInitialization() {
        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Internal Employee");
        basicPage.listUsers()
                .table()
                    .search()
                        .byName()
                        .inputValue("kirk")
                        .updateSearch()
                    .and()
                    .clickByName("kirk")
                        .selectTabProjections()
                        .assertProjectionExist("")
                        .assertProjectionExist("")
                        .assertProjectionExist("");

        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Secret Projects I", "Secret Projects II");
        //TODO check CSV-1 groups
        Utils.removeAssignments(showUser("kirk").selectTabAssignments(), "Secret Projects I", "Secret Projects II");
        //TODO check CSV-1 groups
        Utils.removeAllAssignments(showUser("kirk").selectTabAssignments());
        basicPage.listUsers()
                .table()
                    .search()
                        .byName()
                        .inputValue("kirk")
                        .updateSearch()
                        .and()
                    .clickByName("kirk")
                        .selectTabProjections()
                            .assertProjectionDisabled("")
                            .assertProjectionDisabled("")
                            .assertProjectionDisabled("");
        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Internal Employee");
        basicPage.listUsers()
                .table()
                    .search()
                        .byName()
                        .inputValue("kirk")
                        .updateSearch()
                        .and()
                    .clickByName("kirk")
                        .selectTabProjections()
                            .assertProjectionEnabled("")
                            .assertProjectionEnabled("")
                            .assertProjectionEnabled("");
    }
}
