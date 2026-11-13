/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.web;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.page.admin.cases.PageCase;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.page.admin.cases.PageCaseWorkItem;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Reproduces: values typed into a custom approval form (formRef on an approval stage) are not
 * present in the additional delta sent when the approver clicks Approve.
 *
 * The form binds a plain user attribute (description), a plain attribute that is initially empty (costCenter),
 * and an extension property that already has a value (UserExt-2, changed from 1-1TEBN to 1-1FSD87).
 */
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestApprovalFormAdditionalDelta extends AbstractInitializedGuiIntegrationTest {

    static {
        // PostgreSQL for the native repository must be up before the Spring context is created
        TestPostgresContainer.ensureStarted();
    }


    private static final File DIR = new File("./src/test/resources/approval-form");
    private static final File POLICY_RULES_FILE = new File(DIR, "policy-rules.xml");
    private static final File FORM_FILE = new File(DIR, "form-user-manual.xml");
    private static final File USER_MANAGER_FILE = new File(DIR, "user-manager.xml");
    private static final File USER_NEW_JOINER_FILE = new File(DIR, "user-new-joiner.xml");

    private static final String MANAGER_OID = "a0000000-0000-0000-0000-00000000a001";
    private static final String ENTERED_DESCRIPTION = "entered by manager";
    private static final String ENTERED_COST_CENTER = "CC-4711";
    private static final String ENTERED_POSITION = "1-1FSD87";

    private static final String EXT_NS = "http://example.com/xml/ns/midpoint/schema";
    private static final QName EXT_POSITION = new QName(EXT_NS, "UserExt-2");

    // form inputs on PageCaseWorkItem, page-relative (RepeatingView ids start at 1 in form field order)
    private static final String DESCRIPTION_INPUT = "workItemDetails:additionalAttributes:customForm:formFields:property:1"
            + ":valuesContainer:values:0:value:valueForm:valueContainer:input:input";
    private static final String COST_CENTER_INPUT = "workItemDetails:additionalAttributes:customForm:formFields:property:2"
            + ":valuesContainer:values:0:value:valueForm:valueContainer:input:input";
    private static final String POSITION_INPUT = "workItemDetails:additionalAttributes:customForm:formFields:property:3"
            + ":valuesContainer:values:0:value:valueForm:valueContainer:input:input";
    private static final String APPROVE_BUTTON = "caseWorkItemActionsPanel:workItemApproveButton";

    // case page, work items tab (PageCase with panelId=caseWorkItems)
    private static final String WORK_ITEM_ROW_LINK = "detailsView:mainForm:mainPanel:workitemsPanel:workitemsTable"
            + ":itemsTable:tableContainer:table:body:rows:1:cells:1:cell:link";
    private static final String CASE_TAB_APPROVE_BUTTON = "detailsView:mainForm:mainPanel:workitemsPanel:workitemsTable"
            + ":details:caseWorkItemActionsPanel:workItemApproveButton";

    /** Creates the approval case: manager, form, the existing person, the approval rule, and the trigger change. */
    private CaseType setupApprovalCase(Task task, OperationResult result) throws Exception {
        addObject(FORM_FILE, task, result);
        addObject(USER_MANAGER_FILE, task, result);
        String joinerOid = addObject(USER_NEW_JOINER_FILE, task, result).getOid();
        transplantGlobalPolicyRulesAdd(POLICY_RULES_FILE, task, result);
        executeChanges(prismContext.deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).replace(PolyString.fromOrig("Changed Joiner"))
                .asObjectDelta(joinerOid), null, task, result);
        return findOpenApprovalCase(result);
    }

    @Test
    public void test100ApproveWithFormEditsKeepsModifications() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        CaseType approvalCase = setupApprovalCase(task, result);
        String joinerOid = findJoinerOid(result);
        CaseWorkItemType workItem = approvalCase.getWorkItem().get(0);
        String workItemId = approvalCase.getOid() + ":" + workItem.getId();

        // approver (manager) opens the work item and edits the custom form
        PrismObject<UserType> manager = repositoryService.getObject(UserType.class, MANAGER_OID, null, result);
        login(manager);
        renderPage(PageCaseWorkItem.class, workItemId);

        Page page = tester.getLastRenderedPage();
        Component descriptionInput = page.get(DESCRIPTION_INPUT);
        Component costCenterInput = page.get(COST_CENTER_INPUT);
        Component positionInput = page.get(POSITION_INPUT);
        Component approveButton = page.get(APPROVE_BUTTON);
        assertNotNull("description input not found", descriptionInput);
        assertNotNull("cost center input not found", costCenterInput);
        assertNotNull("position (extension) input not found", positionInput);
        assertNotNull("approve button not found", approveButton);

        // The manager's edits are applied the way the browser sends them: each input's value is processed by Wicket
        // (inputChanged, validate, updateModel), and then the approve click runs.
        changeInputs(
                new FormComponent<?>[] { (FormComponent<?>) descriptionInput, (FormComponent<?>) costCenterInput,
                        (FormComponent<?>) positionInput },
                new String[] { ENTERED_DESCRIPTION, ENTERED_COST_CENTER, ENTERED_POSITION });
        tester.executeAjaxEvent(APPROVE_BUTTON, "click");

        // the work item was completed: the case must carry the manager's values in its delta
        waitForCaseClose(approvalCase.getOid());
        CaseType closedCase = getCase(approvalCase.getOid());
        String caseDelta = String.valueOf(closedCase.getApprovalContext() == null
                ? null : closedCase.getApprovalContext().getDeltasToApprove());
        assertTrue("work-item completion must carry the manager's description in the case delta",
                caseDelta.contains(ENTERED_DESCRIPTION));
        assertTrue("work-item completion must carry the manager's extension value in the case delta",
                caseDelta.contains(ENTERED_POSITION));

        // the approved user must contain the manager's values
        PrismObject<UserType> joinerObject = repositoryService.getObject(UserType.class, joinerOid, null, result);
        assertEquals(ENTERED_DESCRIPTION, joinerObject.asObjectable().getDescription());
        assertEquals(ENTERED_COST_CENTER, joinerObject.asObjectable().getCostCenter());
        PrismProperty<String> position = joinerObject.findProperty(ItemPath.create(UserType.F_EXTENSION, EXT_POSITION));
        assertNotNull("extension property UserExt-2 is missing on the approved user", position);
        assertEquals(ENTERED_POSITION, position.getRealValue());
    }

    /**
     * The approver's path in the GUI: the case page, work items tab, row expanded, Approve from the expanded details.
     * The approve button here is CaseWorkItemActionsPanel with getCustomForm() from CaseWorkItemListWithDetailsPanel.
     */
    @Test
    public void test101ApproveFromCaseWorkItemsTabKeepsModifications() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        CaseType approvalCase = setupApprovalCase(task, result);
        String joinerOid = findJoinerOid(result);

        login(repositoryService.getObject(UserType.class, MANAGER_OID, null, result));
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, approvalCase.getOid());
        params.add("panelId", "caseWorkItems");
        tester.startPage(PageCase.class, params);
        tester.assertRenderedPage(PageCase.class);

        // expand the work item row (its name link opens the details with the custom form)
        tester.executeAjaxEvent(WORK_ITEM_ROW_LINK, "click");

        // the custom form inputs of the expanded details, in form field order: description, cost center, position
        List<FormComponent<?>> inputs = findCustomFormInputs(tester.getLastRenderedPage());
        assertEquals("expected the three custom form inputs in the expanded details", 3, inputs.size());

        changeInputs(inputs.toArray(new FormComponent<?>[0]),
                new String[] { ENTERED_DESCRIPTION, ENTERED_COST_CENTER, ENTERED_POSITION });
        tester.executeAjaxEvent(CASE_TAB_APPROVE_BUTTON, "click");

        waitForCaseClose(approvalCase.getOid());
        CaseType closedCase = getCase(approvalCase.getOid());
        String caseDelta = String.valueOf(closedCase.getApprovalContext() == null
                ? null : closedCase.getApprovalContext().getDeltasToApprove());
        assertTrue("approve from the work items tab must carry the manager's description in the case delta",
                caseDelta.contains(ENTERED_DESCRIPTION));
        assertTrue("approve from the work items tab must carry the manager's extension value in the case delta",
                caseDelta.contains(ENTERED_POSITION));

        PrismObject<UserType> joinerObject = repositoryService.getObject(UserType.class, joinerOid, null, result);
        assertEquals(ENTERED_DESCRIPTION, joinerObject.asObjectable().getDescription());
        assertEquals(ENTERED_COST_CENTER, joinerObject.asObjectable().getCostCenter());
        PrismProperty<String> position = joinerObject.findProperty(ItemPath.create(UserType.F_EXTENSION, EXT_POSITION));
        assertNotNull("extension property UserExt-2 is missing on the approved user", position);
        assertEquals(ENTERED_POSITION, position.getRealValue());
    }

    /** The value inputs of the custom form (in document order, which is the form field order). */
    private static List<FormComponent<?>> findCustomFormInputs(Page page) {
        List<FormComponent<?>> inputs = new java.util.ArrayList<>();
        page.visitChildren(FormComponent.class, (component, visit) -> {
            String path = component.getPageRelativePath();
            if (path.contains("customForm") && path.endsWith(":input:input")) {
                inputs.add((FormComponent<?>) component);
            }
        });
        return inputs;
    }

    /**
     * Applies the user's edits of form inputs: each request parameter named after its input carries the new value, and
     * Wicket's input processing (inputChanged, validate, updateModel) runs for each input, which is what the input's
     * AJAX "change" behavior does in the browser. {@code tester.executeAjaxEvent} is not used here: it sends a new
     * request without the parameters, so the values would be lost before the models are updated. All parameters are
     * set before processing, because Wicket caches the request parameters after the first read in a request cycle.
     */
    private void changeInputs(FormComponent<?>[] inputs, String[] values) {
        assertEquals(inputs.length, values.length);
        for (int i = 0; i < inputs.length; i++) {
            tester.getRequest().setParameter(inputs[i].getInputName(), values[i]);
        }
        for (FormComponent<?> input : inputs) {
            input.processInput();
        }
        for (int i = 0; i < inputs.length; i++) {
            assertTrue("input is not valid: " + inputs[i].getInputName(), inputs[i].isValid());
            assertEquals("Wicket did not update the input model: " + inputs[i].getInputName(),
                    values[i], inputs[i].getModelObject());
        }
    }

    private String findJoinerOid(OperationResult result) throws Exception {
        return repositoryService.searchObjects(UserType.class, null, null, result).stream()
                .filter(u -> "new-joiner".equals(u.asObjectable().getName().getOrig()))
                .findFirst().orElseThrow().getOid();
    }

    private CaseType findOpenApprovalCase(OperationResult result) throws Exception {
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, null, null, result);
        for (PrismObject<CaseType> aCase : cases) {
            CaseType caseType = aCase.asObjectable();
            if (caseType.getApprovalContext() != null && !caseType.getWorkItem().isEmpty()) {
                return caseType;
            }
        }
        throw new AssertionError("No open approval case found");
    }
}
