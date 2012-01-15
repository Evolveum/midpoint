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

package com.evolveum.midpoint.web.controller.server;

import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.TaskItem;
import com.evolveum.midpoint.web.bean.TaskItemBinding;
import com.evolveum.midpoint.web.bean.TaskItemExclusivityStatus;
import com.evolveum.midpoint.web.bean.TaskItemExecutionStatus;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Element;

import it.sauronsoftware.cron4j.InvalidPatternException;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.xml.bind.JAXBException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@Controller("taskDetails")
@Scope("session")
public class TaskDetailsController implements Serializable {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskDetailsController.class);

    private static final long serialVersionUID = -5990159771865483929L;
    public static final String PAGE_NAVIGATION = "/admin/server/taskDetails?faces-redirect=true";

    @Autowired(required = true)
    private transient TemplateController template;

    @Autowired(required = true)
    private transient TaskManager taskManager;
    @Autowired(required = true)
    private transient ObjectTypeCatalog objectTypeCatalog;

    private boolean adding = false;
    private boolean editMode = false;

    private List<ResourceDto> resources;

    private TaskItem task;

    public TaskItemExclusivityStatus[] getExclusivityStatus() {
        return TaskItemExclusivityStatus.values();
    }

    public TaskItemExecutionStatus[] getExecutionStatus() {
        return TaskItemExecutionStatus.values();
    }

    public TaskItemBinding[] getBinding() {
        return TaskItemBinding.values();
    }

    public boolean isEditMode() {
        return editMode || adding;
    }

    public boolean isAdding() {
        return adding;
    }

    void setAdding(boolean adding) {
        this.adding = adding;
    }

    public List<SelectItem> getResourceRefList() {
        List<SelectItem> list = new ArrayList<SelectItem>();
        if (resources == null) {
            return list;
        }

        for (ResourceDto resource : resources) {
            list.add(new SelectItem(resource.getOid(), resource.getName()));
        }

        return list;
    }

    public TaskItem getTask() {
        return task;
    }

    public void setTask(TaskItem task) {
        this.task = task;

        resources = listResources();
        adding = false;
        editMode = false;
    }

    public String getTaskRef() {
        return getTask().getObjectRef();
    }

    public void setTaskRef(String taskRef) {
        getTask().setObjectRef(taskRef);
    }

    public List<OperationResultType> getResults() {
        List<OperationResultType> results = new ArrayList<OperationResultType>();
        if (getTask() == null || getTask().getResult() == null) {
            return results;
        }

        OperationResultType opResultType = getTask().getResult().createOperationResultType();
        results.add(opResultType);
        getResult(opResultType, results, 1220000000000000000L);

        return results;
    }

    private void getResult(OperationResultType opResult, List<OperationResultType> opResultList, long token) {
        for (OperationResultType result : opResult.getPartialResults()) {
            result.setToken(result.getToken() + token);
            opResultList.add(result);
            token += 1110000000000000000L;
            getResult(result, opResultList, token);
        }
    }

    private List<ResourceDto> listResources() {
        ResourceManager resManager = ControllerUtil.getResourceManager(objectTypeCatalog);

        List<ResourceDto> resources = new ArrayList<ResourceDto>();
        try {
            Collection<GuiResourceDto> list = resManager.list();
            if (list != null) {
                resources.addAll(list);
            }
        } catch (Exception ex) {
            FacesUtils.addErrorMessage("Couldn't list resources.", ex);
        }

        return resources;
    }

    public String backPerformed() {
        editMode = false;
        adding = false;
        task = null;

        template.setSelectedLeftId(TaskListController.PAGE_LEFT_NAVIGATION);

        return TaskListController.PAGE_NAVIGATION;
    }

    public void editPerformed() {
        editMode = true;
    }

    public String savePerformed() {
        //todo check if adding or modifying
        if (isAdding()) {
            return addTask();
        }

        OperationResult result = new OperationResult(TaskDetailsController.class.getName() + ".modifyTask");
        try {
//            task.setObjectRef(getRefFromName(getSelectedResurceRef()));
            TaskType oldObject = taskManager.getTask(task.getOid(), result).getTaskTypeObject();
            TaskType newObject = task.toTaskType();
            
            ObjectModificationType modification = CalculateXmlDiff.calculateChanges(oldObject,
                    newObject);

            //todo fix this modification cleanup mess - remove calculate diff later...
            // original modification
            try {
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Modification: " + JAXBUtil.marshalWrap(modification));
            } catch (JAXBException e) {
                e.printStackTrace();
            }

            // now let us remove OperationResult from the modification request
            // also remove other information about task execution
            for (PropertyModificationType pmt : new ArrayList<PropertyModificationType>(modification.getPropertyModification())) {
                if (pmt.getPath() != null) {
                    if (pmt.getPath().getTextContent().startsWith("c:result/") ||
                            pmt.getPath().getTextContent().equals("c:result")) {
                        modification.getPropertyModification().remove(pmt);
                    }
                }
                List<String> namesToRemove = new ArrayList<String>();
                namesToRemove.add("lastRunStartTimestamp");
                namesToRemove.add("lastRunFinishTimestamp");
                namesToRemove.add("progress");        // is this ok?
                namesToRemove.add("extension");        // is this ok?

                if (pmt.getPath() == null || ".".equals(pmt.getPath().getTextContent())) {
                    List<Object> values = pmt.getValue().getAny();
                    if (values.size() == 1) {
                        Object value = values.get(0);
                        if (LOGGER.isTraceEnabled())
                            LOGGER.trace("Value: " + value + " (class: " + value.getClass().getName() + ")");
                        if (value instanceof Element) {
                            String name = ((Element) value).getLocalName();
                            if (namesToRemove.contains(name)) {
                                if (LOGGER.isTraceEnabled())
                                    LOGGER.trace("Skipping modification of " + name);
                                modification.getPropertyModification().remove(pmt);
                            }
                        }
                    }
                }
            }

            // reduced modification
            try {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Modification without task execution information: " + JAXBUtil.marshalWrap(modification));
                }
            } catch (JAXBException e) {
                e.printStackTrace();
            }

            if (!modification.getPropertyModification().isEmpty()) {
                taskManager.modifyTask(modification, result);
                FacesUtils.addSuccessMessage("Task modified successfully");
            } else
                FacesUtils.addSuccessMessage("You have done no modifications.");

            result.recordSuccess();
        } catch (ObjectNotFoundException ex) {
            result.recordFatalError(
                    "Couldn't get task, oid: " + task.getOid() + ". Reason: " + ex.getMessage(), ex);
            FacesUtils.addErrorMessage(
                    "Couldn't get task, oid: " + task.getOid() + ". Reason: " + ex.getMessage(), ex);
            return null;
        } catch (SchemaException ex) {
            result.recordFatalError("Couldn't modify task. Reason: " + ex.getMessage(), ex);
            FacesUtils.addErrorMessage("Couldn't modify task. Reason: " + ex.getMessage(), ex);
            return null;
        } catch (DiffException ex) {
            result.recordFatalError("Couldn't get object change for task " + task.getName() + ". Reason: "
                    + ex.getMessage(), ex);
            FacesUtils.addErrorMessage("Couldn't get object change for task " + task.getName() + ". Reason: "
                    + ex.getMessage(), ex);
            return null;
        } catch (InvalidPatternException ex) {
            result.recordFatalError("Cron-like scheduling pattern is invalid: "
                    + ex.getMessage(), ex);
            FacesUtils.addErrorMessage("Cron-like scheduling pattern is invalid: "
                    + ex.getMessage(), ex);
            return null;
        } finally {
            result.computeStatus();
            if (!result.isSuccess()) {
                FacesUtils.addMessage(result);
            }
        }

        return TaskListController.PAGE_NAVIGATION;
    }

    private String addTask() {
        OperationResult result = new OperationResult(TaskDetailsController.class.getName() + ".addTask");
        try {
            taskManager.addTask(task.toTaskType(), result);
            FacesUtils.addSuccessMessage("Task added successfully");
            result.recordSuccess();
        } catch (InvalidPatternException ex) {
            result.recordFatalError("Cron-like scheduling pattern is invalid: "
                    + ex.getMessage(), ex);
            FacesUtils.addErrorMessage("Cron-like scheduling pattern is invalid: "
                    + ex.getMessage(), ex);
            return null;
        } catch (Exception ex) {
            result.recordFatalError("Couldn't add task. Reason: " + ex.getMessage(), ex);
            FacesUtils.addErrorMessage("Couldn't add task. Reason: " + ex.getMessage(), ex);
            return null;
        } finally {
            result.computeStatus();
            if (!result.isSuccess()) {
                FacesUtils.addMessage(result);
            }
        }

        template.setSelectedLeftId(TaskListController.PAGE_LEFT_NAVIGATION);

        return TaskListController.PAGE_NAVIGATION;
    }

    public void resumeTaskPerformed(ActionEvent evt) {
        OperationResult result = new OperationResult(TaskDetailsController.class.getName() + ".resumeTask");
        try {
            Task taskToResume = taskManager.getTask(task.getOid(), result);
            taskManager.resumeTask(taskToResume, result);
            FacesUtils.addSuccessMessage("Task resumed successfully.");
        } catch (Exception ex) {
            FacesUtils.addErrorMessage("Failed to resume task. Reason: " + ex.getMessage(), ex);
        } finally {
            result.computeStatus();
            if (!result.isSuccess()) {
                FacesUtils.addMessage(result);
            }
        }
    }

    public void suspendTaskPerformed(ActionEvent evt) {
        OperationResult result = new OperationResult(TaskDetailsController.class.getName() + ".suspendTask");
        try {
            Task task = taskManager.getTask(this.task.getOid(), result);
            boolean down = taskManager.suspendTask(task, 1000L, result);
            if (down)
            	FacesUtils.addSuccessMessage("Task has been successfully suspended.");
            else
            	FacesUtils.addWarnMessage("Task suspension has been successfully requested; please check for its completion using task list.");
        } catch (Exception ex) {
            FacesUtils.addErrorMessage("Failed to suspend task. Reason: " + ex.getMessage(), ex);
        } finally {
            result.computeStatus();
            if (!result.isSuccess()) {
                FacesUtils.addMessage(result);
            }
        }
    }

    public void releaseTaskPerformed(ActionEvent evt) {
        // TODO: check on input values
        OperationResult result = new OperationResult(TaskDetailsController.class.getName() + ".releaseTask");
        try {
            Task taskToRelease = taskManager.getTask(task.getOid(), result);

            taskManager.releaseTask(taskToRelease, result);
            FacesUtils.addSuccessMessage("Task released successfully");
        } catch (Exception ex) {
            FacesUtils.addErrorMessage("Failed to release task. Reason: " + ex.getMessage(), ex);
        } finally {
            result.computeStatus();
            if (!result.isSuccess()) {
                FacesUtils.addMessage(result);
            }
        }
    }

    public String addPerformed() {
        TaskItem task = new TaskItem(taskManager);
        task.setHandlerUri("http://midpoint.evolveum.com/model/sync/handler-1");
        setTask(task);

        adding = true;

        return PAGE_NAVIGATION;
    }

    public void importTaskPerformed(ActionEvent evt) {
        template.setSelectedTopId(TemplateController.TOP_CONFIGURATION);
    }

//    public void createInstancePerformed() {
//        OperationResult result = new OperationResult("Create task instance");
//        try {
//            taskManager.createTaskInstance(task.toTaskType(), result);
//            FacesUtils.addSuccessMessage("Task instance created successfully");
//        } catch (SchemaException ex) {
//            FacesUtils.addErrorMessage("Failed to create task. Reason: " + ex.getMessage(), ex);
//        } finally {
//            result.computeStatus();
//            if (!result.isSuccess()) {
//                FacesUtils.addMessage(result);
//            }
//        }
//    }
}
