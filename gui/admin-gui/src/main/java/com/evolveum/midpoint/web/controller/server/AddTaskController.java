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

import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
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
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import it.sauronsoftware.cron4j.InvalidPatternException;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@Controller("addTask")
@Scope("session")
public class AddTaskController implements Serializable {

    private static final transient Trace LOGGER = TraceManager.getTrace(AddTaskController.class);

    public static final String PAGE_NAVIGATION = "/admin/server/addTask?faces-redirect=true";

    @Autowired(required = true)
    private transient TemplateController template;

    @Autowired(required = true)
    private transient TaskManager taskManager;
    @Autowired(required = true)
    private transient ObjectTypeCatalog objectTypeCatalog;

	private List<ResourceDto> resources;

    private String categoryRef;
    private String resourceRef;
    private String name;                       // task name
    private boolean startNow = true;           // RUNNABLE or SUSPENDED?
    private boolean tightlyBound = false;
    private String scheduleInterval;
    private String scheduleCronLikePattern;

    public String getCategoryRef() {
        return categoryRef;
    }

    public void setCategoryRef(String categoryRef) {
        this.categoryRef = categoryRef;
    }

    public List<SelectItem> getCategoryRefList() {
        List<SelectItem> list = new ArrayList<SelectItem>();
        list.add(new SelectItem("http://midpoint.evolveum.com/model/sync/handler-1", "Live synchronization"));      // FIXME: remove these hardcoded URIs!!!
        list.add(new SelectItem("http://midpoint.evolveum.com/model/sync/reconciliation-handler-1", "Reconciliation"));
        list.add(new SelectItem("http://midpoint.evolveum.com/model/import/handler-accounts-resource-1", "Import accounts from resource"));
        list.add(new SelectItem("http://midpoint.evolveum.com/model/sync/recompute-handler-1", "User recomputation"));
        list.add(new SelectItem("http://midpoint.evolveum.com/repo/noop-handler-1", "No-operation (demo) task"));
        return list;
    }

    public String getResourceRef() {
        return resourceRef;
    }

    public void setResourceRef(String resourceRef) {
        this.resourceRef = resourceRef;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScheduleCronLikePattern() {
        return scheduleCronLikePattern;
    }

    public void setScheduleCronLikePattern(String scheduleCronLikePattern) {
        this.scheduleCronLikePattern = scheduleCronLikePattern;
    }

    public String getScheduleInterval() {
        return scheduleInterval;
    }

    public void setScheduleInterval(String scheduleInterval) {
        this.scheduleInterval = scheduleInterval;
    }

    public boolean isStartNow() {
        return startNow;
    }

    public void setStartNow(boolean startNow) {
        this.startNow = startNow;
    }

    public boolean isTightlyBound() {
        return tightlyBound;
    }

    public void setTightlyBound(boolean tightlyBound) {
        this.tightlyBound = tightlyBound;
    }

    public String backPerformed() {
        template.setSelectedLeftId(TaskListController.PAGE_LEFT_NAVIGATION);
        return TaskListController.PAGE_NAVIGATION;
    }

    public String savePerformed() {

        OperationResult result = new OperationResult(AddTaskController.class.getName() + ".addTask");
        try {
        	
        	SecurityUtils security = new SecurityUtils();
    		PrincipalUser principal = security.getPrincipalUser();

            Task task = taskManager.createTaskInstance();
            task.setOwner(principal.getUser().asPrismObject());
            task.setHandlerUri(categoryRef);
            if (StringUtils.isNotBlank(resourceRef)) {
                ObjectReferenceType ort = new ObjectReferenceType();
                ort.setOid(resourceRef);
                task.setObjectRef(ort);
            }
            if (StringUtils.isBlank(name)) {
                name = categoryRef + " " + resourceRef; // TODO: resource name instead of OID
            }
            task.setName(name);
            task.setInitialExecutionStatus(startNow ? TaskExecutionStatus.RUNNABLE : TaskExecutionStatus.SUSPENDED);

            task.setBinding(tightlyBound ? TaskBinding.TIGHT : TaskBinding.LOOSE);

            // TODO: handle syntax errors, missing/duplicate values
            if (StringUtils.isNotBlank(scheduleInterval)) {
                task.makeRecurrentSimple(Integer.parseInt(scheduleInterval));
            } else if (StringUtils.isNotBlank(scheduleCronLikePattern)) {
                task.makeRecurrentCron(scheduleCronLikePattern);
            }

            taskManager.switchToBackground(task, result);                   // TODO: find/create better method
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

    public String addPerformed() {

        resources = listResources();
        return PAGE_NAVIGATION;
    }

    public void importTaskPerformed(ActionEvent evt) {
        template.setSelectedTopId(TemplateController.TOP_CONFIGURATION);
    }

}
