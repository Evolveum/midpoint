/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.wf.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author mederly
 */
public class PageWorkItem extends PageAdminWorkItems {

    public static final String PARAM_TASK_ID = "taskId";
    private static final String DOT_CLASS = PageWorkItem.class.getName() + ".";
    private static final String OPERATION_LOAD_WORK_ITEM = DOT_CLASS + "loadWorkItem";
    private static final String OPERATION_SAVE_WORK_ITEM = DOT_CLASS + "saveWorkItem";


    private static final Trace LOGGER = TraceManager.getTrace(PageWorkItem.class);
    private IModel<ObjectWrapper> workItemModel;

    public PageWorkItem() {
        workItemModel = new LoadableModel<ObjectWrapper>(false) {

            @Override
            protected ObjectWrapper load() {
                return loadWorkItemWrapper();
            }
        };
        initLayout();
    }

    private ObjectWrapper loadWorkItemWrapper() {
        OperationResult result = new OperationResult(OPERATION_LOAD_WORK_ITEM);
        PrismObject<? extends ObjectType> workItem = null;
        try {
            WorkflowManager wfm = getWorkflowManager();
            workItem = wfm.getWorkItemPrism(getPageParameters().get(PARAM_TASK_ID).toString());

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get work item.", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, workItem, status);
        wrapper.setShowEmpty(true);

        return wrapper;
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        PrismObjectPanel workItemForm = new PrismObjectPanel("workItemForm", workItemModel,
                new PackageResourceReference(PageWorkItem.class, "User.png"), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.description");
            }
        };
        mainForm.add(workItemForm);

        initButtons(mainForm);
    }


    private void initButtons(Form mainForm) {

        AjaxSubmitLinkButton approve = new AjaxSubmitLinkButton("approve",
                createStringResource("pageWorkItem.button.approve")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                // todo
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(approve);

        AjaxSubmitLinkButton reject = new AjaxSubmitLinkButton("reject",
                createStringResource("pageWorkItem.button.reject")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                // todo
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(reject);

        AjaxSubmitLinkButton done = new AjaxSubmitLinkButton("done",
                createStringResource("pageWorkItem.button.done")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(done);

        AjaxLinkButton cancel = new AjaxLinkButton("cancel",
                createStringResource("pageWorkItem.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(cancel);
    }


    private void cancelPerformed(AjaxRequestTarget target) {
        setResponsePage(PageWorkItems.class);
    }

    private void savePerformed(AjaxRequestTarget target) {
        LOGGER.debug("Saving work item changes.");

        OperationResult result = new OperationResult(OPERATION_SAVE_WORK_ITEM);

        ObjectWrapper itemWrapper = workItemModel.getObject();
        try {
            getWorkflowManager().saveWorkItemPrism(itemWrapper.getObject(), result);
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't save work item", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
            showResultInSession(result);
            setResponsePage(PageWorkItems.class);
        }
    }

}
