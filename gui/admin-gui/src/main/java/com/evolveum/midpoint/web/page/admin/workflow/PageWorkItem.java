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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDetailedDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.api.WorkflowService;
import com.evolveum.midpoint.wf.api.WorkItemDetailed;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.text.SimpleDateFormat;

/**
 * @author mederly
 */
public class PageWorkItem extends PageAdminWorkItems {

    public static final String PARAM_TASK_ID = "taskId";
    private static final String DOT_CLASS = PageWorkItem.class.getName() + ".";
    private static final String OPERATION_LOAD_WORK_ITEM = DOT_CLASS + "loadWorkItem";
    private static final String OPERATION_LOAD_PROCESS_INSTANCE = DOT_CLASS + "loadProcessInstance";
    private static final String OPERATION_LOAD_REQUESTER = DOT_CLASS + "loadRequester";
    private static final String OPERATION_LOAD_OBJECT_OLD = DOT_CLASS + "loadObjectOld";
    private static final String OPERATION_LOAD_OBJECT_NEW = DOT_CLASS + "loadObjectNew";
    private static final String OPERATION_SAVE_WORK_ITEM = DOT_CLASS + "saveWorkItem";
    private static final String OPERATION_LOAD_REQUEST_COMMON = DOT_CLASS + "loadRequestCommon";
    private static final String OPERATION_LOAD_REQUEST_SPECIFIC = DOT_CLASS + "loadRequestSpecific";
    private static final String OPERATION_LOAD_ADDITIONAL_DATA = DOT_CLASS + "loadAdditionalData";
    private static final String OPERATION_LOAD_TRACKING_DATA = DOT_CLASS + "loadTrackingData";
    
    private static final String ID_ACCORDION = "accordion";
    private static final String ID_ADDITIONAL_INFO = "additionalInfo";

    private static final Trace LOGGER = TraceManager.getTrace(PageWorkItem.class);

    private IModel<WorkItemDetailedDto> workItemDtoModel;

    private IModel<ObjectWrapper> requesterModel;
    private IModel<ObjectWrapper> objectOldModel;
    private IModel<ObjectWrapper> objectNewModel;
    private IModel<ObjectWrapper> requestCommonModel;
    private IModel<ObjectWrapper> requestSpecificModel;
    private IModel<ObjectWrapper> additionalDataModel;
    private IModel<ObjectWrapper> trackingDataModel;
    private IModel<ProcessInstanceDto> processInstanceDtoModel;

    public PageWorkItem() {
        requesterModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getRequesterWrapper();
            }
        };
        objectOldModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getObjectOldWrapper();
            }
        };
        objectNewModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getObjectNewWrapper();
            }
        };
        requestCommonModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getRequestCommonWrapper();
            }
        };
        requestSpecificModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getRequestSpecificWrapper();
            }
        };
        additionalDataModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getAdditionalDataWrapper();
            }
        };
        trackingDataModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getTrackingDataWrapper();
            }
        };
        workItemDtoModel = new LoadableModel<WorkItemDetailedDto>(false) {
            @Override
            protected WorkItemDetailedDto load() {
                return loadWorkItemDetailedDtoIfNecessary();
            }
        };
        processInstanceDtoModel = new LoadableModel<ProcessInstanceDto>(false) {
            @Override
            protected ProcessInstanceDto load() {
                return loadProcessInstanceDto();
            }
        };

        initLayout();
    }

    private ObjectWrapper getRequesterWrapper() {
        PrismObject<UserType> prism = workItemDtoModel.getObject().getWorkItem().getRequester();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, prism, status);
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setShowAssignments(true);
        wrapper.setReadonly(true);

        return wrapper;
    }

    private ObjectWrapper getObjectOldWrapper() {
        PrismObject<? extends ObjectType> prism = workItemDtoModel.getObject().getWorkItem().getObjectOld();

        if (prism == null) {
            prism = createEmptyUserObject();
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, prism, status);
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setShowAssignments(true);
        wrapper.setReadonly(true);

        return wrapper;
    }

    private ObjectWrapper getObjectNewWrapper() {
        PrismObject<? extends ObjectType> prism = workItemDtoModel.getObject().getWorkItem().getObjectNew();

        if (prism == null) {
            prism = createEmptyUserObject();
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, prism, status);
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setShowAssignments(true);
        wrapper.setReadonly(true);

        return wrapper;
    }


    private PrismObject<? extends ObjectType> createEmptyUserObject() {
        PrismObject<? extends ObjectType> p = new PrismObject<UserType>(UserType.COMPLEX_TYPE, UserType.class);
        try {
            getWorkflowService().getPrismContext().adopt(p);
        } catch (SchemaException e) {   // safe to convert; this should not occur
            throw new SystemException("Got schema exception when creating empty user object.", e);
        }
        return p;
    }

    private ObjectWrapper getRequestCommonWrapper() {
        PrismObject<ObjectType> prism = workItemDtoModel.getObject().getWorkItem().getRequestCommonData();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, prism, status);
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(false);

        return wrapper;
    }

    private ObjectWrapper getRequestSpecificWrapper() {
        PrismObject<ObjectType> prism = workItemDtoModel.getObject().getWorkItem().getRequestSpecificData();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, prism, status);
        wrapper.setShowEmpty(true);
        wrapper.setMinimalized(false);

        return wrapper;
    }


    private ObjectWrapper getAdditionalDataWrapper() {
        PrismObject<ObjectType> prism = workItemDtoModel.getObject().getWorkItem().getAdditionalData();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, prism, status);
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setReadonly(true);

        return wrapper;
    }

    private ObjectWrapper getTrackingDataWrapper() {
        PrismObject<ObjectType> prism = workItemDtoModel.getObject().getWorkItem().getTrackingData();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, prism, status);
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setReadonly(true);

        return wrapper;
    }

    private WorkItemDetailedDto loadWorkItemDetailedDtoIfNecessary() {

        if (((LoadableModel) workItemDtoModel).isLoaded()) {
            return workItemDtoModel.getObject();
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_WORK_ITEM);
        WorkItemDetailed workItem = null;
        try {
            WorkflowService wfm = getWorkflowService();
            workItem = wfm.getWorkItemByTaskId(getPageParameters().get(PARAM_TASK_ID).toString(), result);
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't get work item.", ex);
        }

        if (!result.isSuccess()) {
            showResultInSession(result);
            throw new RestartResponseException(PageWorkItems.class);
        }

        return new WorkItemDetailedDto(workItem);
    }

    private ProcessInstanceDto loadProcessInstanceDto() {
        OperationResult result = new OperationResult(OPERATION_LOAD_PROCESS_INSTANCE);
        ProcessInstance processInstance;
        try {
            WorkflowService wfm = getWorkflowService();
            processInstance = wfm.getProcessInstanceByTaskId(getPageParameters().get(PARAM_TASK_ID).toString(), result);
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get process instance for work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't get process instance for work item.", ex);
            showResult(result);
            return null;
        }

        return new ProcessInstanceDto(processInstance);
    }


    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        Label title = new Label("title", new PropertyModel(workItemDtoModel, "name"));
        mainForm.add(title);

        Label requestedBy = new Label("requestedBy", new PropertyModel(requesterModel, "object.asObjectable.name"));
        mainForm.add(requestedBy);

        Label requestedByFullName = new Label("requestedByFullName", new PropertyModel(requesterModel, "object.asObjectable.fullName"));
        mainForm.add(requestedByFullName);

        Label requestedOn = new Label("requestedOn", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ProcessInstanceDto dto = processInstanceDtoModel.getObject();
                if (dto.getProcessInstance().getStartTime() == null) {
                    return "";
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss");
                return dateFormat.format(dto.getProcessInstance().getStartTime());
            }

        });
        mainForm.add(requestedOn);

        Label workItemCreatedOn = new Label("workItemCreatedOn", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                WorkItemDetailedDto dto = workItemDtoModel.getObject();
                if (dto.getWorkItem().getCreateTime() == null) {
                    return "";
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss");
                return dateFormat.format(dto.getWorkItem().getCreateTime());
            }

        });
        mainForm.add(workItemCreatedOn);
        
        PrismObjectPanel requestSpecificForm = new PrismObjectPanel("requestSpecificForm", requestSpecificModel,
        		new PackageResourceReference(ImgResources.class, ImgResources.DECISION_PRISM), mainForm) {

            @Override
            protected IModel<String> createDisplayName(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.requestSpecific.description");
            }

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return new Model<String>("");
            }
        };
        mainForm.add(requestSpecificForm);
        
        Accordion accordion = new Accordion(ID_ACCORDION);
        accordion.setOutputMarkupId(true);
        accordion.setMultipleSelect(true);
        accordion.setExpanded(true);
        mainForm.add(accordion);

        AccordionItem additionalInfo = new AccordionItem(ID_ADDITIONAL_INFO, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getString("pageWorkItem.additionalInfo");
            }
        });
        additionalInfo.setOutputMarkupId(true);
        accordion.getBodyContainer().add(additionalInfo);

        PrismObjectPanel requesterForm = new PrismObjectPanel("requesterForm", requesterModel,
        		new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.requester.description");
            }
        };
        requesterForm.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible() {
                return requesterModel != null && !requesterModel.getObject().getObject().isEmpty();
            }
        });
        additionalInfo.getBodyContainer().add(requesterForm);

        PrismObjectPanel objectOldForm = new PrismObjectPanel("objectOldForm", objectOldModel,
        		new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.objectOld.description");
            }
        };
        additionalInfo.getBodyContainer().add(objectOldForm);

        PrismObjectPanel objectNewForm = new PrismObjectPanel("objectNewForm", objectNewModel,
        		new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.objectNew.description");
            }
        };
        additionalInfo.getBodyContainer().add(objectNewForm);

        PrismObjectPanel additionalDataForm = new PrismObjectPanel("additionalDataForm", additionalDataModel,
        		new PackageResourceReference(ImgResources.class, ImgResources.ROLE_PRISM), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.additionalData.description");
            }
        };
        additionalInfo.getBodyContainer().add(additionalDataForm);

        PrismObjectPanel trackingDataForm = new PrismObjectPanel("trackingDataForm", trackingDataModel,
        		new PackageResourceReference(ImgResources.class, ImgResources.TRACKING_PRISM), mainForm) {

            @Override
            protected IModel<String> createDisplayName(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.trackingData.description");
            }

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return new Model("");
            }
        };
        additionalInfo.getBodyContainer().add(trackingDataForm);

        initButtons(mainForm);
    }


    private void initButtons(Form mainForm) {

        AjaxSubmitLinkButton approve = new AjaxSubmitLinkButton("approve",
                createStringResource("pageWorkItem.button.approve")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target, true);
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
                savePerformed(target, false);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(reject);

//        AjaxSubmitLinkButton done = new AjaxSubmitLinkButton("done",
//                createStringResource("pageWorkItem.button.done")) {
//
//            @Override
//            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
//                try {
//                    savePerformed(target);
//                } catch(RuntimeException e) {
//                    LoggingUtils.logException(LOGGER, "Exception in savePerformed", e);
//                    throw e;
//                }
//            }
//
//            @Override
//            protected void onError(AjaxRequestTarget target, Form<?> form) {
//                target.add(getFeedbackPanel());
//            }
//        };
//        mainForm.add(done);

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

    private void savePerformed(AjaxRequestTarget target, boolean decision) {
        LOGGER.debug("Saving work item changes.");

        OperationResult result = new OperationResult(OPERATION_SAVE_WORK_ITEM);

        ObjectWrapper rsWrapper = requestSpecificModel.getObject();
        try {
            PrismObject object = rsWrapper.getObject();
            ObjectDelta delta = rsWrapper.getObjectDelta();
            delta.applyTo(object);

            getWorkflowService().saveWorkItemPrism(workItemDtoModel.getObject().getWorkItem().getTaskId(), object, decision, result);
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
