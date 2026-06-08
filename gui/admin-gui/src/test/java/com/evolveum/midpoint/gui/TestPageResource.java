/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.RESOURCE_DUMMY_OID;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertSame;

import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.component.ResourceOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
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
}
