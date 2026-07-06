/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.RESOURCE_DUMMY_OID;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStatePanel;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.page.admin.component.ResourceOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ContainerWithLifecyclePanel;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.page.admin.resources.component.TestConnectionResultPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageResource extends AbstractInitializedGuiIntegrationTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        logger.info("adding system config page");
        addObject(systemConfig, executeOptions().overwrite(), initTask, initResult);
    }

    /**
     * Verifies MID-10966: closing the Test connection result popup must not refresh
     * the resource details page model, because that would recreate the edited
     * {@link PrismObjectWrapper} and discard unsaved form changes.
     */
    @Test
    public void test100TestConnectionDoesNotReloadResourceDetailsWrapper() {
        PageResource pageResource = (PageResource) renderPage(PageResource.class, RESOURCE_DUMMY_OID);
        PrismObjectWrapper<ResourceType> wrapperBefore = pageResource.getObjectDetailsModels().getObjectWrapper();

        ResourceOperationalButtonsPanel buttonsPanel = findComponent(ResourceOperationalButtonsPanel.class);
        assertNotNull("Resource operational buttons panel was not found", buttonsPanel);
        AjaxIconButton testConnectionButton = findAjaxIconButtonByTitle(
                buttonsPanel, pageResource.getString("pageResource.button.test"));
        assertNotNull("Test connection button was not found", testConnectionButton);
        tester.executeAjaxEvent(testConnectionButton.getPageRelativePath(), "click");

        TestConnectionResultPanel resultPanel = findComponent(TestConnectionResultPanel.class);
        assertNotNull("Test connection result popup was not shown", resultPanel);
        AjaxButton okButton = findChildComponent(resultPanel, AjaxButton.class, "ok");
        assertNotNull("Test connection result popup OK button was not found", okButton);
        tester.executeAjaxEvent(okButton.getPageRelativePath(), "click");

        PrismObjectWrapper<ResourceType> wrapperAfter = pageResource.getObjectDetailsModels().getObjectWrapper();
        assertSame("Test connection must not reload resource details wrapper", wrapperBefore, wrapperAfter);
    }

    /**
     * Verifies that the root row in the detailed lifecycle popup edits the real
     * resource lifecycle wrapper, while nested rows keep their own lifecycle wrappers.
     */
    @Test
    public void test110DetailedLifecycleRootLifecycleUsesResourceLifecycleWrapper() throws Exception {
        PageResource pageResource = (PageResource) renderPage(PageResource.class, RESOURCE_DUMMY_OID);
        PrismPropertyWrapper<String> resourceLifecycleWrapper =
                pageResource.getObjectDetailsModels().getObjectWrapper().findProperty(ResourceType.F_LIFECYCLE_STATE);

        ResourceOperationalButtonsPanel buttonsPanel = findComponent(ResourceOperationalButtonsPanel.class);
        assertNotNull("Resource operational buttons panel was not found", buttonsPanel);

        AjaxIconButton detailedLifecycleButton = findAjaxIconButtonByTitle(
                buttonsPanel, pageResource.getString("SchemaHandlingObjectsPanel.button.showLifecycleStates"));
        assertNotNull("Detailed lifecycle button was not found", detailedLifecycleButton);
        tester.executeAjaxEvent(detailedLifecycleButton.getPageRelativePath(), "click");

        ContainerWithLifecyclePanel<?> detailedLifecyclePanel = findComponent(ContainerWithLifecyclePanel.class);
        assertNotNull("Detailed lifecycle popup panel was not shown", detailedLifecyclePanel);

        LifecycleStatePanel rootLifecyclePanel =
                (LifecycleStatePanel) detailedLifecyclePanel.get("lifecycleInput");
        String rootLifecycleValue = differentLifecycleValue(
                resourceLifecycleWrapper, SchemaConstants.LIFECYCLE_PROPOSED);
        setLifecycleValue(rootLifecyclePanel, rootLifecycleValue);
        assertEquals(
                "Detailed lifecycle root row must update the real resource lifecycle wrapper",
                rootLifecycleValue,
                resourceLifecycleWrapper.getValue().getRealValue());

        List<LifecycleStatePanel> lifecyclePanels = findChildComponents(detailedLifecyclePanel, LifecycleStatePanel.class);
        lifecyclePanels.remove(rootLifecyclePanel);
        assertTrue("Expected nested lifecycle rows in detailed lifecycle popup", !lifecyclePanels.isEmpty());

        LifecycleStatePanel nestedLifecyclePanel = lifecyclePanels.stream()
                .filter(Component::isVisibleInHierarchy)
                .findFirst()
                .orElse(null);
        assertNotNull("Expected visible nested lifecycle row in detailed lifecycle popup", nestedLifecyclePanel);
        setLifecycleValue(nestedLifecyclePanel, SchemaConstants.LIFECYCLE_DRAFT);
        assertEquals(
                "Nested lifecycle row must not update the resource lifecycle wrapper",
                rootLifecycleValue,
                resourceLifecycleWrapper.getValue().getRealValue());
    }

    /**
     * Verifies that changing the root lifecycle from the detailed lifecycle popup
     * goes through the same confirmation/save path as the main lifecycle dropdown.
     */
    @Test
    public void test120DetailedLifecycleRootLifecycleChangeShowsConfirmationAndSaves() throws Exception {
        PageResource pageResource = (PageResource) renderPage(PageResource.class, RESOURCE_DUMMY_OID);

        ResourceOperationalButtonsPanel buttonsPanel = findComponent(ResourceOperationalButtonsPanel.class);
        assertNotNull("Resource operational buttons panel was not found", buttonsPanel);

        AjaxIconButton detailedLifecycleButton = findAjaxIconButtonByTitle(
                buttonsPanel, pageResource.getString("SchemaHandlingObjectsPanel.button.showLifecycleStates"));
        assertNotNull("Detailed lifecycle button was not found", detailedLifecycleButton);
        tester.executeAjaxEvent(detailedLifecycleButton.getPageRelativePath(), "click");

        ContainerWithLifecyclePanel<?> detailedLifecyclePanel = findComponent(ContainerWithLifecyclePanel.class);
        assertNotNull("Detailed lifecycle popup panel was not shown", detailedLifecyclePanel);

        LifecycleStatePanel rootLifecyclePanel =
                (LifecycleStatePanel) detailedLifecyclePanel.get("lifecycleInput");
        String newLifecycleState = differentLifecycleValue(
                pageResource.getObjectDetailsModels().getObjectWrapper().findProperty(ResourceType.F_LIFECYCLE_STATE),
                SchemaConstants.LIFECYCLE_PROPOSED);
        setLifecycleValue(rootLifecyclePanel, newLifecycleState);

        AjaxButton doneButton = findChildComponent(tester.getLastRenderedPage(), AjaxButton.class, "doneButton");
        assertNotNull("Detailed lifecycle popup Done button was not found", doneButton);
        tester.executeAjaxEvent(doneButton.getPageRelativePath(), "click");

        ConfirmationPanel confirmationPanel = findComponent(ConfirmationPanel.class);
        assertNotNull("Root lifecycle confirmation popup was not shown", confirmationPanel);

        AjaxButton yesButton = findChildComponent(tester.getLastRenderedPage(), AjaxButton.class, "yes");
        assertNotNull("Lifecycle confirmation Yes button was not found", yesButton);
        tester.executeAjaxEvent(yesButton.getPageRelativePath(), "click");

        OperationResult result = new OperationResult("test120DetailedLifecycleRootLifecycleChangeShowsConfirmationAndSaves");
        PrismObject<ResourceType> resourceAfter =
                repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
        assertEquals(
                "Resource lifecycle state was not persisted",
                newLifecycleState,
                resourceAfter.asObjectable().getLifecycleState());
    }

    private <C extends Component> C findComponent(Class<C> type) {
        return findChildComponent(tester.getLastRenderedPage(), type, null);
    }

    private <C extends Component> C findChildComponent(MarkupContainer parent, Class<C> type, String id) {
        AtomicReference<C> found = new AtomicReference<>();
        parent.visitChildren(Component.class, (component, visit) -> {
            if (type.isInstance(component) && (id == null || id.equals(component.getId()))) {
                found.set(type.cast(component));
                visit.stop();
            }
        });
        return found.get();
    }

    private <C extends Component> List<C> findChildComponents(MarkupContainer parent, Class<C> type) {
        List<C> found = new ArrayList<>();
        parent.visitChildren(Component.class, (component, visit) -> {
            if (type.isInstance(component)) {
                found.add(type.cast(component));
            }
        });
        return found;
    }

    /**
     * Finds an Ajax icon button by its resolved title text instead of by a generated
     * Wicket path, which is unstable for buttons inside repeating views.
     */
    private AjaxIconButton findAjaxIconButtonByTitle(MarkupContainer parent, String title) {
        AtomicReference<AjaxIconButton> found = new AtomicReference<>();
        parent.visitChildren(Component.class, (component, visit) -> {
            if (!(component instanceof AjaxIconButton button)) {
                return;
            }
            if (button.getTitle() != null && title.equals(button.getTitle().getObject())) {
                found.set(button);
                visit.stop();
            }
        });
        return found.get();
    }

    private String differentLifecycleValue(PrismPropertyWrapper<String> lifecycleWrapper, String preferredValue)
            throws Exception {
        String currentValue = lifecycleWrapper.getValue().getRealValue();
        if (!preferredValue.equals(currentValue)) {
            return preferredValue;
        }
        return SchemaConstants.LIFECYCLE_ACTIVE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setLifecycleValue(LifecycleStatePanel lifecyclePanel, String lifecycleValue) {
        lifecyclePanel.getBaseFormComponent().getModel().setObject(new SearchValue<>(lifecycleValue, lifecycleValue));
    }

}
