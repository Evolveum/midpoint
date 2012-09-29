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
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.wf.WfDataAccessor;
import com.evolveum.midpoint.wf.WorkItem;
import com.evolveum.midpoint.wf.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;
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
    private static final String OPERATION_LOAD_REQUESTER = DOT_CLASS + "loadRequester";
    private static final String OPERATION_LOAD_OBJECT_OLD = DOT_CLASS + "loadObjectOld";
    private static final String OPERATION_LOAD_OBJECT_NEW = DOT_CLASS + "loadObjectNew";
    private static final String OPERATION_SAVE_WORK_ITEM = DOT_CLASS + "saveWorkItem";
    private static final String OPERATION_LOAD_REQUEST_COMMON = DOT_CLASS + "loadRequestCommon";
    private static final String OPERATION_LOAD_REQUEST_SPECIFIC = DOT_CLASS + "loadRequestSpecific";
    private static final String OPERATION_LOAD_ADDITIONAL_DATA = DOT_CLASS + "loadAdditionalData";

    private static final Trace LOGGER = TraceManager.getTrace(PageWorkItem.class);
//    private IModel<ObjectWrapper> workItemModel;

    private IModel<ObjectWrapper> requesterModel;
    private IModel<ObjectWrapper> objectOldModel;
    private IModel<ObjectWrapper> objectNewModel;
    private IModel<ObjectWrapper> requestCommonModel;
    private IModel<ObjectWrapper> requestSpecificModel;
    private IModel<ObjectWrapper> additionalDataModel;

    public PageWorkItem() {
        requesterModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                return loadRequesterWrapper();
            }
        };
        objectOldModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                return loadObjectOldWrapper();
            }
        };
        objectNewModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                return loadObjectNewWrapper();
            }
        };
        requestCommonModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                return loadRequestCommonWrapper();
            }
        };
        requestSpecificModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                return loadRequestSpecificWrapper();
            }
        };
        additionalDataModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                return loadAdditionalDataWrapper();
            }
        };

        initLayout();
    }

    private ObjectWrapper loadRequesterWrapper() {
        OperationResult result = new OperationResult(OPERATION_LOAD_REQUESTER);
        PrismObject<UserType> requester = null;
        try {
            WfDataAccessor wfm = getWorkflowDataAccessor();
            requester = wfm.getRequester(getPageParameters().get(PARAM_TASK_ID).toString());
            if (requester == null) {
                requester = createEmptyUserObject();
            }
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get requester for work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't get requester for work item.", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
            return null;
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, requester, status);
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);

        return wrapper;
    }

    private ObjectWrapper loadObjectOldWrapper() {
        OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT_OLD);
        PrismObject<? extends ObjectType> objectOld = null;
        try {
            WfDataAccessor wfm = getWorkflowDataAccessor();
            objectOld = wfm.getObjectOld(getPageParameters().get(PARAM_TASK_ID).toString());
            if (objectOld == null) {
                objectOld = createEmptyUserObject();
            }
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get original object for work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't get original object for work item.", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, objectOld, status);
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);

        return wrapper;
    }

    private PrismObject<UserType> createEmptyUserObject() throws SchemaException {
        PrismObject<UserType> p = new PrismObject<UserType>(UserType.COMPLEX_TYPE, UserType.class);
        getWorkflowManager().getPrismContext().adopt(p);
        return p;
    }

    private ObjectWrapper loadObjectNewWrapper() {
        OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT_NEW);
        PrismObject<? extends ObjectType> objectNew = null;
        try {
            WfDataAccessor wfm = getWorkflowDataAccessor();
            objectNew = wfm.getObjectNew(getPageParameters().get(PARAM_TASK_ID).toString());
            if (objectNew == null) {
                objectNew = createEmptyUserObject();
            }
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get changed (target) form of object for work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't get changed (target) form of object for work item.", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, objectNew, status);
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);

        return wrapper;
    }

    private ObjectWrapper loadRequestCommonWrapper() {
        OperationResult result = new OperationResult(OPERATION_LOAD_REQUEST_COMMON);
        PrismObject<? extends ObjectType> requestCommon = null;
        try {
            WfDataAccessor wfm = getWorkflowDataAccessor();
            requestCommon = wfm.getRequestCommon(getPageParameters().get(PARAM_TASK_ID).toString());
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get request common data for work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't get request common data for work item.", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, requestCommon, status);
        wrapper.setShowEmpty(false);

        return wrapper;
    }

    private ObjectWrapper loadRequestSpecificWrapper() {
        OperationResult result = new OperationResult(OPERATION_LOAD_REQUEST_SPECIFIC);
        PrismObject<? extends ObjectType> requestSpecific = null;
        try {
            WfDataAccessor wfm = getWorkflowDataAccessor();
            requestSpecific = wfm.getRequestSpecific(getPageParameters().get(PARAM_TASK_ID).toString());
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get request common data for work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't get request common data for work item.", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, requestSpecific, status);
        wrapper.setShowEmpty(true);

        return wrapper;
    }

    private ObjectWrapper loadAdditionalDataWrapper() {
        OperationResult result = new OperationResult(OPERATION_LOAD_ADDITIONAL_DATA);
        PrismObject<? extends ObjectType> additionalData = null;
        try {
            WfDataAccessor wfm = getWorkflowDataAccessor();
            additionalData = wfm.getAdditionalData(getPageParameters().get(PARAM_TASK_ID).toString());
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get additional data for work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't get additional data for work item.", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, additionalData, status);
        wrapper.setShowEmpty(false);

        return wrapper;
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        PrismObjectPanel requesterForm = new PrismObjectPanel("requesterForm", requesterModel,
                new PackageResourceReference(PageWorkItem.class, "User.png"), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.requester.description");
            }
        };
        mainForm.add(requesterForm);

        PrismObjectPanel objectOldForm = new PrismObjectPanel("objectOldForm", objectOldModel,
                new PackageResourceReference(PageWorkItem.class, "User.png"), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.objectOld.description");
            }
        };
        mainForm.add(objectOldForm);

        PrismObjectPanel objectNewForm = new PrismObjectPanel("objectNewForm", objectNewModel,
                new PackageResourceReference(PageWorkItem.class, "User.png"), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.objectNew.description");
            }
        };
        mainForm.add(objectNewForm);

        PrismObjectPanel requestCommonForm = new PrismObjectPanel("requestCommonForm", requestCommonModel,
                new PackageResourceReference(PageWorkItem.class, "User.png"), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.requestCommon.description");
            }
        };
        mainForm.add(requestCommonForm);

        PrismObjectPanel requestSpecificForm = new PrismObjectPanel("requestSpecificForm", requestSpecificModel,
                new PackageResourceReference(PageWorkItem.class, "User.png"), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.requestSpecific.description");
            }
        };
        mainForm.add(requestSpecificForm);

        PrismObjectPanel additionalDataForm = new PrismObjectPanel("additionalDataForm", additionalDataModel,
                new PackageResourceReference(PageWorkItem.class, "User.png"), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.additionalData.description");
            }
        };
        mainForm.add(additionalDataForm);

        initButtons(mainForm);
    }


    private void initButtons(Form mainForm) {

//        AjaxSubmitLinkButton approve = new AjaxSubmitLinkButton("approve",
//                createStringResource("pageWorkItem.button.approve")) {
//
//            @Override
//            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
//                // todo
//            }
//
//            @Override
//            protected void onError(AjaxRequestTarget target, Form<?> form) {
//                target.add(getFeedbackPanel());
//            }
//        };
//        mainForm.add(approve);
//
//        AjaxSubmitLinkButton reject = new AjaxSubmitLinkButton("reject",
//                createStringResource("pageWorkItem.button.reject")) {
//
//            @Override
//            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
//                // todo
//            }
//
//            @Override
//            protected void onError(AjaxRequestTarget target, Form<?> form) {
//                target.add(getFeedbackPanel());
//            }
//        };
//        mainForm.add(reject);

        AjaxSubmitLinkButton done = new AjaxSubmitLinkButton("done",
                createStringResource("pageWorkItem.button.done")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    savePerformed(target);
                } catch(RuntimeException e) {
                    LoggingUtils.logException(LOGGER, "Exception in savePerformed", e);
                    throw e;
                }
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

        ObjectWrapper rsWrapper = requestSpecificModel.getObject();
        ObjectWrapper rcWrapper = requestCommonModel.getObject();
        try {
            PrismObject object = rsWrapper.getObject();
            ObjectDelta delta = rsWrapper.getObjectDelta();
            delta.applyTo(object);

            getWorkflowDataAccessor().saveWorkItemPrism(object, rcWrapper.getObject(), result);
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
