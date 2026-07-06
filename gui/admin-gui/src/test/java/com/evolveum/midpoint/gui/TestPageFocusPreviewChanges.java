/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import static com.evolveum.midpoint.test.util.MidPointAsserts.assertSerializable;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.model.IModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.page.admin.component.preview.PreviewChangesTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusPreviewChanges;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageFocusPreviewChanges extends AbstractInitializedGuiIntegrationTest {

    private static final File ORG_PREVIEW_POLICY_RULE_FILE =
            new File("src/test/resources/common/org-preview-policy-rule.xml");
    private static final String ORG_PREVIEW_ROLE_OID = "11111111-1111-1111-1111-000000110082";
    private static final String ORG_PREVIEW_ORG_OID = "33333333-3333-3333-3333-000000110082";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        importObjectFromFile(ORG_PREVIEW_POLICY_RULE_FILE, initTask, initResult);
    }

    @Test
    public void test001PreviewChangesPageSerializationWithPolicyRuleContext() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<OrgType> delta = prismContext.deltaFor(OrgType.class)
                .item(OrgType.F_ASSIGNMENT)
                .add(new AssignmentType()
                        .targetRef(ORG_PREVIEW_ROLE_OID, RoleType.COMPLEX_TYPE))
                .asObjectDelta(ORG_PREVIEW_ORG_OID);

        ModelContext<OrgType> modelContext = modelInteractionService.previewChanges(
                List.of(delta), ModelExecuteOptions.create().reconcile(), task, result);
        assertSuccess(result);
        assertPolicyRuleFocusMappingContext(modelContext);

        PrismObject<OrgType> org = modelService.getObject(OrgType.class, ORG_PREVIEW_ORG_OID, null, task, result);
        PageFocusPreviewChanges<OrgType> page = new PageFocusPreviewChanges<>(Map.of(org, modelContext), null);

        tester.startPage(page);
        tester.assertRenderedPage(PageFocusPreviewChanges.class);

        Page renderedPage = tester.getLastRenderedPage();
        assertNotNull(
                tester.getApplication().getFrameworkSettings().getSerializer().serialize(renderedPage),
                "Rendered preview page is not Wicket-serializable");
        assertNoPreviewTabPanelRetainsModelContext(renderedPage);
        assertSerializable(renderedPage);
    }

    private void assertPolicyRuleFocusMappingContext(ModelContext<OrgType> modelContext) {
        assertFalse(
                modelContext.getFocusContextRequired().getObjectPolicyRules().isEmpty(),
                "Policy rule from the archetype was not evaluated");
        assertTrue(
                modelContext.getAllEvaluatedAssignments().stream()
                        .anyMatch(this::hasFocusMappingRequestWithMagicAssignment),
                "No evaluated assignment contains focus mapping request with magic assignment");
    }

    private boolean hasFocusMappingRequestWithMagicAssignment(EvaluatedAssignment evaluatedAssignment) {
        try {
            Method method = evaluatedAssignment.getClass().getMethod("getFocusMappingEvaluationRequests");
            Collection<?> requests = (Collection<?>) method.invoke(evaluatedAssignment);
            for (Object request : requests) {
                Object variables = request.getClass().getMethod("getAssignmentPathVariables").invoke(request);
                Object magicAssignment = variables.getClass().getMethod("getMagicAssignment").invoke(variables);
                if (magicAssignment != null) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException e) {
            return false;
        }
        return false;
    }

    private void assertNoPreviewTabPanelRetainsModelContext(Page renderedPage) {
        AtomicInteger previewTabPanels = new AtomicInteger();
        renderedPage.visitChildren(PreviewChangesTabPanel.class, (panel, visit) -> {
            previewTabPanels.incrementAndGet();
            IModel<?> model = ((Component) panel).getDefaultModel();
            assertTrue(model == null || !(model.getObject() instanceof ModelContext),
                    "PreviewChangesTabPanel must not retain ModelContext in Wicket page state");
        });
        assertTrue(previewTabPanels.get() > 0, "PreviewChangesTabPanel was not rendered");
    }
}
