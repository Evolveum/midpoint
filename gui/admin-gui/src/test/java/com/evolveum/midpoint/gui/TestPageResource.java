/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.RESOURCE_DUMMY_OID;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStatePanel;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.page.admin.component.ResourceOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ContainerWithLifecyclePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ConfigurationStepPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormContainerHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
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
import org.apache.wicket.request.mapper.parameter.PageParameters;
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

    /**
     * Verifies that the resource configuration wizard step groups the connector configuration
     * properties into labeled sections (from the ICF {@code groupMessageKey} annotation) and that,
     * when there are more than three groups, the sections without a mandatory item start collapsed
     * while the section with the mandatory item stays expanded.
     */
    @Test
    public void test130ConfigurationStepCollapsesNonMandatorySections() throws Exception {
        PageParameters params = new PageParameters();
        params.set(TestConfigurationCollapsePage.PARAM_RESOURCE_OID, resourceDummy.getOid());
        tester.startPage(TestConfigurationCollapsePage.class, params);
        tester.assertRenderedPage(TestConfigurationCollapsePage.class);

        ConfigurationStepPanel configurationStep = findComponent(ConfigurationStepPanel.class);
        assertNotNull("Configuration step panel was not found", configurationStep);

        List<VerticalFormPrismContainerPanel> sections =
                findChildComponents(configurationStep, VerticalFormPrismContainerPanel.class);
        assertEquals(
                "Unexpected number of configuration sections (4 labeled groups + main)",
                5,
                sections.size());

        Map<String, Boolean> expandedByTitle = new TreeMap<>();
        Map<String, java.util.Set<String>> itemsByTitle = new TreeMap<>();
        for (VerticalFormPrismContainerPanel section : sections) {
            AjaxButton titleLabel = findChildComponent(section, AjaxButton.class, "label");
            assertNotNull("Section title label was not found", titleLabel);
            PrismContainerWrapper<?> sectionWrapper = (PrismContainerWrapper<?>) section.getModelObject();
            String title = titleLabel.getModel().getObject();
            assertNotNull("Section title must not be null", title);
            expandedByTitle.put(title, sectionWrapper.isExpanded());
            itemsByTitle.put(title, collectRenderedItemNames(section));
        }

        logger.info("Configuration step sections and their expanded state: {}", expandedByTitle);
        logger.info("Configuration step sections and their rendered items: {}", itemsByTitle);

        assertTrue(
                "Section with the mandatory item (General) should be expanded",
                expandedByTitle.getOrDefault("General", false));
        assertFalse(
                "Section without mandatory items (Schema) should start collapsed",
                expandedByTitle.getOrDefault("Schema", true));
        assertFalse(
                "Section without mandatory items (Validation) should start collapsed",
                expandedByTitle.getOrDefault("Validation", true));
        assertFalse(
                "Section without mandatory items (Support) should start collapsed",
                expandedByTitle.getOrDefault("Support", true));

        // The sections are backed by virtual containers; the connector configuration properties
        // must actually be rendered inside them (not just the empty section shells). The General
        // section starts expanded, so its grouped property must be rendered right away.
        java.util.Set<String> generalItems = itemsByTitle.getOrDefault("General", java.util.Set.of());
        assertTrue(
                "The General section must render its grouped property (instanceId), but rendered: " + generalItems,
                generalItems.contains("instanceId"));

        // Collapsed sections render their items lazily, so simulate a user clicking the header of
        // the collapsed main (ungrouped) section to expand it and verify the properties show up.
        VerticalFormPrismContainerPanel configurationSection = findSection(sections, "Configuration");
        VerticalFormContainerHeaderPanel configurationHeader =
                findChildComponent(configurationSection, VerticalFormContainerHeaderPanel.class, null);
        assertNotNull("Configuration section header was not found", configurationHeader);
        tester.executeAjaxEvent(configurationHeader.getPageRelativePath(), "click");

        VerticalFormPrismContainerPanel expandedConfigurationSection = findSection(sections, "Configuration");

        // The properties of a new resource are empty; the section hides empty fields by default.
        // Simulate a user clicking the "show empty fields" button and verify the properties show up.
        java.util.Set<String> configurationItemsBefore = collectRenderedItemNames(expandedConfigurationSection);
        logger.info("Configuration section items after expansion (empty fields hidden): {}", configurationItemsBefore);
        AjaxIconButton showEmptyButton = findChildComponent(expandedConfigurationSection, AjaxIconButton.class, "showEmptyButton");
        assertNotNull("Show empty fields button was not found in the Configuration section", showEmptyButton);
        tester.executeAjaxEvent(showEmptyButton.getPageRelativePath(), "click");

        VerticalFormPrismContainerPanel updatedConfigurationSection = findSection(sections, "Configuration");
        java.util.Set<String> configurationItems = collectRenderedItemNames(updatedConfigurationSection);
        logger.info("Configuration section items after showing empty fields: {}", configurationItems);
        assertTrue(
                "The expanded main section must render the ungrouped properties (supportValidity) "
                        + "after showing empty fields, but rendered: " + configurationItems,
                configurationItems.contains("supportValidity"));
    }

    private VerticalFormPrismContainerPanel findSection(List<VerticalFormPrismContainerPanel> sections, String title) {
        return sections.stream()
                .filter(section -> {
                    AjaxButton titleLabel = findChildComponent(section, AjaxButton.class, "label");
                    return titleLabel != null && title.equals(titleLabel.getModel().getObject());
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("Section with title " + title + " was not found"));
    }

    /**
     * Collects the local names of the configuration properties rendered inside the given section.
     */
    private java.util.Set<String> collectRenderedItemNames(VerticalFormPrismContainerPanel section) {
        java.util.Set<String> itemNames = new java.util.TreeSet<>();
        for (Component component : findChildComponents(section, Component.class)) {
            if (!(component instanceof ItemPanel<?, ?> itemPanel)) {
                continue;
            }
            Object modelObject = itemPanel.getModelObject();
            if (modelObject instanceof ItemWrapper<?, ?> itemWrapper) {
                itemNames.add(itemWrapper.getItemName().getLocalPart());
            }
        }
        return itemNames;
    }

    /**
     * Verifies that the resource "Connector configuration" details panel groups the connector
     * configuration properties into collapsible cards (virtual containers). Each labeled group
     * (from the ICF {@code groupMessageKey} annotation) is rendered as a separate collapsible card.
     */
    @Test
    public void test140ResourceConfigurationRendersGroupCards() throws Exception {
        PageParameters params = new PageParameters();
        params.set(TestResourceConfigurationGroupingPage.PARAM_RESOURCE_OID, resourceDummy.getOid());
        tester.startPage(TestResourceConfigurationGroupingPage.class, params);
        tester.assertRenderedPage(TestResourceConfigurationGroupingPage.class);

        SingleContainerPanel<?> configurationPanel = findComponent(SingleContainerPanel.class);
        assertNotNull("Configuration container panel was not found", configurationPanel);

        List<PrismContainerPanel> cards = findChildComponents(configurationPanel, PrismContainerPanel.class);
        logger.info("Configuration group cards rendered: {}", cards.size());
        assertFalse("No configuration group cards were rendered", cards.isEmpty());
        assertTrue(
                "Expected at least 4 configuration group cards (General, Schema, Validation, Support), got " + cards.size(),
                cards.size() >= 4);

        // The group sections must be backed by materialized virtual containers (on the object
        // wrapper) that together contain all the configuration properties; otherwise the sections
        // render empty (the items were not resolved into the virtual containers).
        PrismContainerWrapper<?> configurationWrapper = configurationPanel.getModelObject();
        assertNotNull("Configuration container wrapper was not found in the panel model", configurationWrapper);
        PrismContainerValueWrapper<?> configurationValue = configurationWrapper.getValue();
        List<? extends ItemWrapper<?, ?>> allProperties = configurationValue.getNonContainers();
        assertTrue("No configuration properties found on " + configurationWrapper.getItemName(),
                !allProperties.isEmpty());

        // Walk up to the object value wrapper, where the virtual (group) containers live.
        PrismContainerValueWrapper<?> connectorConfigurationValue = configurationWrapper.getParent();
        ItemWrapper<?, ?> connectorConfigurationWrapper = connectorConfigurationValue.getParent();
        PrismContainerValueWrapper<?> objectValue = connectorConfigurationWrapper.getParent();
        assertNotNull("Object value wrapper was not found", objectValue);

        // The virtual (group) sections are materialized at the object level; note the same section
        // can be materialized more than once, so deduplicate by identifier before counting.
        int totalSectionItems = 0;
        int virtualSections = 0;
        java.util.Set<String> seenIdentifiers = new java.util.LinkedHashSet<>();
        for (ItemWrapper<?, ?> item : objectValue.getItems()) {
            if (!(item instanceof PrismContainerWrapper<?> sectionWrapper) || !sectionWrapper.isVirtual()) {
                continue;
            }
            String identifier = sectionWrapper.getIdentifier();
            if (!seenIdentifiers.add(identifier)) {
                continue;
            }
            virtualSections++;
            PrismContainerValueWrapper<?> sectionValue = sectionWrapper.getValue();
            if (sectionValue == null) {
                continue;
            }
            List<? extends ItemWrapper<?, ?>> sectionItems = sectionValue.getNonContainers();
            logger.info("Virtual section {} contains {} items", identifier, sectionItems.size());
            totalSectionItems += sectionItems.size();
        }
        assertTrue("No virtual (group) sections were materialized", virtualSections > 0);
        assertEquals(
                "The group sections should together contain all configuration properties",
                allProperties.size(), totalSectionItems);
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
