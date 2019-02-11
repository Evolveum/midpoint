package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
import com.evolveum.midpoint.testing.schrodinger.TestBase;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Created by honchar
 */
public class ObjectListArchetypeTests extends TestBase {

    private static final File EMPLOYEE_ARCHETYPE_FILE = new File("src/test/resources/configuration/objects/archetypes/archetype-employee.xml");

    @Test
    public void importEmployeeArchetype() throws IOException, ConfigurationException {

        ImportObjectPage importPage = basicPage.importObject();
        Assert.assertTrue(
                importPage
                        .getObjectsFromFile()
                        .chooseFile(EMPLOYEE_ARCHETYPE_FILE)
                        .checkOverwriteExistingObject()
                        .clickImport()
                        .feedback()
                        .isSuccess()
        );
    }
}
