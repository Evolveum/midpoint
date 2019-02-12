package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.configuration.AdminGuiTab;
import com.evolveum.midpoint.schrodinger.component.modal.ObjectBrowserModal;
import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.testing.schrodinger.TestBase;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public class ObjectListArchetypeTests extends TestBase {

    private static final File EMPLOYEE_ARCHETYPE_FILE = new File("src/test/resources/configuration/objects/archetypes/archetype-employee.xml");
    private static final String ARCHETYPE_OBJECT_NAME = "Employee";
    private static final String COLLECTION_REF_ATTRIBUTE_NAME = "Collection ref";
    private static final String OBJECT_COLLECTION_VIEWS_HEADER = "Object collection views";
    private static final String OBJECT_COLLECTION_VIEW_HEADER = "Object collection view";
    private static final String NEW_GUI_OBJECT_LIST_VIEW_HEADER = "New gui object list view";
    private static final String NEW_OBJECT_LIST_VIEW_CONTAINER_KEY = "GuiObjectListViewType.details";
    private static final String COLLECTION_HEADER = "Collection";

    @Test(priority = 0)
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

    @Test(priority = 1, dependsOnMethods ={"importEmployeeArchetype"})
    public void configureArchetypeObjectListView(){
        AdminGuiTab adminGuiTab = basicPage.adminGui();
        PrismForm<AdminGuiTab> prismForm = adminGuiTab.form();
        prismForm
                .expandContainerPropertiesPanel(OBJECT_COLLECTION_VIEWS_HEADER)
                .addNewContainerValue(OBJECT_COLLECTION_VIEW_HEADER, NEW_OBJECT_LIST_VIEW_CONTAINER_KEY)
                .expandContainerPropertiesPanel(NEW_OBJECT_LIST_VIEW_CONTAINER_KEY)
                .expandContainerPropertiesPanel(COLLECTION_HEADER);

        SelenideElement collectionRefPropertyPanel = prismForm.findProperty(COLLECTION_REF_ATTRIBUTE_NAME);
        collectionRefPropertyPanel
                .$(Schrodinger.byDataId("edit"))
                .click();

        SelenideElement modalWindow = $(By.className("wicket-modal"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        ObjectBrowserModal objectBrowserModal = new ObjectBrowserModal<>(prismForm, modalWindow);
        objectBrowserModal
                .selectType("Archetype")
                .table()
                    .search()
                        .byName()
                        .inputValue(ARCHETYPE_OBJECT_NAME)
                        .updateSearch();
        objectBrowserModal
                .table()
                    .clickByName(ARCHETYPE_OBJECT_NAME);

        Assert.assertTrue(prismForm
                .compareInputAttributeValue(COLLECTION_REF_ATTRIBUTE_NAME, ARCHETYPE_OBJECT_NAME + ": ArchetypeType"));

        adminGuiTab
                .getParent()
                .save()
                .feedback()
                .isSuccess();
    }
}

