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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.model.delta.ContainerValuePanel;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.delta.DeltaPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatusFilter;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDetailedDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.api.WorkflowService;
import com.evolveum.midpoint.wf.api.WorkItemDetailed;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
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
    private static final String OPERATION_SAVE_WORK_ITEM = DOT_CLASS + "saveWorkItem";

    private static final String ID_ACCORDION = "accordion";
    private static final String ID_ADDITIONAL_INFO = "additionalInfo";
    private static final String ID_DELTA_PANEL = "deltaPanel";

    private static final Trace LOGGER = TraceManager.getTrace(PageWorkItem.class);
    private static final String ID_DELTA_ACCORDION = "deltaAccordion";
    private static final String ID_DELTA_INFO = "deltaInfo";

    private static final String ID_REQUESTER_ACCORDION = "requesterAccordion";
    private static final String ID_REQUESTER_ACCORDION_INFO = "requesterAccordionInfo";
    private static final String ID_REQUESTER_PANEL = "requesterPanel";
    private static final String ID_OBJECT_OLD_ACCORDION = "objectOldAccordion";
    private static final String ID_OBJECT_OLD_ACCORDION_INFO = "objectOldAccordionInfo";
    private static final String ID_OBJECT_OLD_PANEL = "objectOldPanel";
    private static final String ID_OBJECT_NEW_ACCORDION = "objectNewAccordion";
    private static final String ID_OBJECT_NEW_ACCORDION_INFO = "objectNewAccordionInfo";
    private static final String ID_OBJECT_NEW_PANEL = "objectNewPanel";
    private static final String ID_ADDITIONAL_DATA_ACCORDION = "additionalDataAccordion";
    private static final String ID_ADDITIONAL_DATA_ACCORDION_INFO = "additionalDataAccordionInfo";
    private static final String ID_ADDITIONAL_DATA_PANEL = "additionalDataPanel";
    private static final String ID_PROCESS_INSTANCE_ACCORDION = "processInstanceAccordion";
    private static final String ID_PROCESS_INSTANCE_ACCORDION_INFO = "processInstanceAccordionInfo";
    private static final String ID_PROCESS_INSTANCE_PANEL = "processInstancePanel";
    private static final String ID_SHOW_TECHNICAL_INFORMATION = "showTechnicalInformation";

    private PageParameters parameters;
    private Page previousPage;      // where to return

    private IModel<WorkItemDetailedDto> workItemDtoModel;

    private IModel<ObjectWrapper> requesterModel;
    private IModel<ObjectWrapper> objectOldModel;
    private IModel<ObjectWrapper> objectNewModel;
    private IModel<ObjectWrapper> requestSpecificModel;
    private IModel<ObjectWrapper> additionalDataModel;
    private IModel<ObjectWrapper> trackingDataModel;
    private IModel<ProcessInstanceDto> processInstanceDtoModel;
    private IModel<DeltaDto> deltaModel;

    private IModel<Boolean> showTechnicalInformationModel = new Model<Boolean>();
    private Accordion additionalInfoAccordion;
    private AccordionItem additionalInfoAccordionItem;

    public PageWorkItem() {
        this(new PageParameters(), null);
    }

    public PageWorkItem(PageParameters parameters, Page previousPage) {

        this.parameters = parameters;
        this.previousPage = previousPage;

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
        deltaModel = new PropertyModel<DeltaDto>(workItemDtoModel, WorkItemDetailedDto.F_DELTA);

        initLayout();
    }

    private ObjectWrapper getRequesterWrapper() {
        PrismObject<UserType> prism = workItemDtoModel.getObject().getWorkItem().getRequester();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(
                createStringResource("pageWorkItem.requester.description").getString(),     // name (large font)
                PolyString.getOrig(prism.asObjectable().getName()),                         // description (smaller font)
                prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setShowAssignments(false);
        wrapper.setReadonly(true);

        return wrapper;
    }

    private ObjectWrapper getObjectOldWrapper() {
        PrismObject<? extends ObjectType> prism = workItemDtoModel.getObject().getWorkItem().getObjectOld();

        if (prism == null) {
            prism = createEmptyUserObject();
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(
                createStringResource("pageWorkItem.objectOld.description").getString(),     // name (large font)
                PolyString.getOrig(prism.asObjectable().getName()),                         // description (smaller font)
                prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
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
        ObjectWrapper wrapper = new ObjectWrapper(
                createStringResource("pageWorkItem.objectNew.description").getString(),     // name (large font)
                PolyString.getOrig(prism.asObjectable().getName()),                         // description (smaller font)
                prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
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

    private ObjectWrapper getRequestSpecificWrapper() {
        PrismObject<?> prism = workItemDtoModel.getObject().getWorkItem().getRequestSpecificData();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
        wrapper.setShowEmpty(true);
        wrapper.setMinimalized(false);
        wrapper.setShowInheritedObjectAttributes(false);

        return wrapper;
    }


    private ObjectWrapper getAdditionalDataWrapper() {
        PrismObject<? extends ObjectType> prism = workItemDtoModel.getObject().getWorkItem().getAdditionalData();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(
                createStringResource("pageWorkItem.additionalData.description").getString(),     // name (large font)
                PolyString.getOrig(prism.asObjectable().getName()),                         // description (smaller font)
                prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setReadonly(true);

        return wrapper;
    }

    private ObjectWrapper getTrackingDataWrapper() {
        PrismObject<? extends ObjectType> prism = workItemDtoModel.getObject().getWorkItem().getTrackingData();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
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
            workItem = wfm.getWorkItemDetailsByTaskId(parameters.get(PARAM_TASK_ID).toString(), result);
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't get work item.", ex);
        }

        if (!result.isSuccess()) {
            showResultInSession(result);
            if (previousPage != null) {
                throw new RestartResponseException(previousPage);
            } else {
                throw new RestartResponseException(PageWorkItems.class);
            }
        }

        return new WorkItemDetailedDto(workItem);
    }

    private ProcessInstanceDto loadProcessInstanceDto() {
        OperationResult result = new OperationResult(OPERATION_LOAD_PROCESS_INSTANCE);
        ProcessInstance processInstance;
        try {
            WorkflowService wfm = getWorkflowService();
            processInstance = wfm.getProcessInstanceByTaskId(parameters.get(PARAM_TASK_ID).toString(), result);
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
        
        additionalInfoAccordion = new Accordion(ID_ACCORDION);
        additionalInfoAccordion.setOutputMarkupId(true);
        additionalInfoAccordion.setMultipleSelect(true);
        additionalInfoAccordion.setExpanded(true);
        mainForm.add(additionalInfoAccordion);

        additionalInfoAccordionItem = new AccordionItem(ID_ADDITIONAL_INFO, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getString("pageWorkItem.additionalInfo");
            }
        });
        additionalInfoAccordionItem.setOutputMarkupId(true);
        additionalInfoAccordion.getBodyContainer().add(additionalInfoAccordionItem);

        PrismObjectPanel requesterForm = new PrismObjectPanel("requesterForm", requesterModel,
        		new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), mainForm);
        requesterForm.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible() {
                return requesterModel != null && !requesterModel.getObject().getObject().isEmpty();
            }
        });
        additionalInfoAccordionItem.getBodyContainer().add(requesterForm);

        PrismObjectPanel objectOldForm = new PrismObjectPanel("objectOldForm", objectOldModel,
        		new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), mainForm);
        objectOldForm.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return workItemDtoModel.getObject().getWorkItem().getObjectOld() != null;
            }
        });
        additionalInfoAccordionItem.getBodyContainer().add(objectOldForm);

        PrismObjectPanel objectNewForm = new PrismObjectPanel("objectNewForm", objectNewModel,
        		new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), mainForm);
        objectNewForm.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return workItemDtoModel.getObject().getWorkItem().getObjectNew() != null;
            }
        });
        additionalInfoAccordionItem.getBodyContainer().add(objectNewForm);

        PrismObjectPanel additionalDataForm = new PrismObjectPanel("additionalDataForm", additionalDataModel,
        		new PackageResourceReference(ImgResources.class, ImgResources.ROLE_PRISM), mainForm);
        additionalInfoAccordionItem.getBodyContainer().add(additionalDataForm);

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
        additionalInfoAccordionItem.getBodyContainer().add(trackingDataForm);

        Accordion deltaAccordion = new Accordion(ID_DELTA_ACCORDION);
        deltaAccordion.setOutputMarkupId(true);
        deltaAccordion.setMultipleSelect(true);
        deltaAccordion.setExpanded(false);
        additionalInfoAccordionItem.getBodyContainer().add(deltaAccordion);

        AccordionItem deltaInfo = new AccordionItem(ID_DELTA_INFO, new ResourceModel("pageWorkItem.delta"));
        deltaInfo.setOutputMarkupId(true);
        deltaAccordion.getBodyContainer().add(deltaInfo);

        DeltaPanel deltaPanel = new DeltaPanel(ID_DELTA_PANEL, deltaModel);
        deltaInfo.getBodyContainer().add(deltaPanel);

        additionalInfoAccordionItem.getBodyContainer().add(createObjectAccordion(ID_REQUESTER_ACCORDION, ID_REQUESTER_ACCORDION_INFO, ID_REQUESTER_PANEL, "pageWorkItem.accordionLabel.requester", new PropertyModel(workItemDtoModel, WorkItemDetailedDto.F_REQUESTER), true));
        additionalInfoAccordionItem.getBodyContainer().add(createObjectAccordion(ID_OBJECT_OLD_ACCORDION, ID_OBJECT_OLD_ACCORDION_INFO, ID_OBJECT_OLD_PANEL, "pageWorkItem.accordionLabel.objectOld", new PropertyModel(workItemDtoModel, WorkItemDetailedDto.F_OBJECT_OLD), true));
        additionalInfoAccordionItem.getBodyContainer().add(createObjectAccordion(ID_OBJECT_NEW_ACCORDION, ID_OBJECT_NEW_ACCORDION_INFO, ID_OBJECT_NEW_PANEL, "pageWorkItem.accordionLabel.objectNew", new PropertyModel(workItemDtoModel, WorkItemDetailedDto.F_OBJECT_NEW), true));
        additionalInfoAccordionItem.getBodyContainer().add(createObjectAccordion(ID_ADDITIONAL_DATA_ACCORDION, ID_ADDITIONAL_DATA_ACCORDION_INFO, ID_ADDITIONAL_DATA_PANEL, "pageWorkItem.accordionLabel.additionalData", new PropertyModel(workItemDtoModel, WorkItemDetailedDto.F_ADDITIONAL_DATA), true));

        String detailsPageClassName = getWorkflowService().getProcessInstanceDetailsPanelName(processInstanceDtoModel.getObject().getProcessInstance());

        additionalInfoAccordionItem.getBodyContainer().add(createAccordion(ID_PROCESS_INSTANCE_ACCORDION,
                ID_PROCESS_INSTANCE_ACCORDION_INFO,
                "pageWorkItem.accordionLabel.processInstance",
                new ProcessInstancePanel(ID_PROCESS_INSTANCE_PANEL, processInstanceDtoModel, detailsPageClassName), true));
        initButtons(mainForm);
    }

    private Component createAccordion(String idAccordion, String idAccordionInfo, String key, Panel panel, boolean isTechnical) {

        Accordion accordion = new Accordion(idAccordion);
        accordion.setOutputMarkupId(true);
        accordion.setMultipleSelect(true);
        accordion.setExpanded(false);

        AccordionItem info = new AccordionItem(idAccordionInfo, new ResourceModel(key));
        info.setOutputMarkupId(true);
        accordion.getBodyContainer().add(info);

        info.getBodyContainer().add(panel);

        if (isTechnical) {
            accordion.add(new VisibleEnableBehaviour() {
                @Override
                public boolean isVisible() {
                    return showTechnicalInformationModel.getObject() == Boolean.TRUE;
                }
            });
        }

        return accordion;
    }

    private Component createObjectAccordion(String idAccordion, String idAccordionInfo, String idPanel, String key, IModel model, boolean isTechnical) {
        Accordion accordion = new Accordion(idAccordion);
        accordion.setOutputMarkupId(true);
        accordion.setMultipleSelect(true);
        accordion.setExpanded(false);

        AccordionItem info = new AccordionItem(idAccordionInfo, new ResourceModel(key));
        info.setOutputMarkupId(true);
        accordion.getBodyContainer().add(info);

        ContainerValuePanel panel = new ContainerValuePanel(idPanel, model);
        info.getBodyContainer().add(panel);

        if (isTechnical) {
            accordion.add(new VisibleEnableBehaviour() {
                @Override
                public boolean isVisible() {
                    return showTechnicalInformationModel.getObject() == Boolean.TRUE;
                }
            });
        }

        return accordion;
    }


    private void initButtons(Form mainForm) {

        // todo authorization

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

        CheckBox showTechnicalInformationBox = new CheckBox(ID_SHOW_TECHNICAL_INFORMATION, showTechnicalInformationModel);
        showTechnicalInformationBox.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(additionalInfoAccordion);
            }
        });
        mainForm.add(showTechnicalInformationBox);
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        goBack();
    }

    private void savePerformed(AjaxRequestTarget target, boolean decision) {
        LOGGER.debug("Saving work item changes.");

        OperationResult result = new OperationResult(OPERATION_SAVE_WORK_ITEM);

        ObjectWrapper rsWrapper = requestSpecificModel.getObject();
        try {
            PrismObject object = rsWrapper.getObject();
            ObjectDelta delta = rsWrapper.getObjectDelta();
            delta.applyTo(object);

            getWorkflowService().approveOrRejectWorkItemWithDetails(workItemDtoModel.getObject().getWorkItem().getTaskId(), object, decision, result);
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
            goBack();
        }
    }

    private void goBack() {
        if (previousPage != null) {
            setResponsePage(previousPage);
        } else {
            setResponsePage(PageWorkItems.class);
        }
    }


}
