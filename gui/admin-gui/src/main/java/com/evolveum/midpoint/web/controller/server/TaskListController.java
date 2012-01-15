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

import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.bean.TaskItem;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.controller.util.SortableListController;
import com.evolveum.midpoint.web.repo.RepositoryManager;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import java.util.List;
import java.util.Set;

@Controller("taskList")
@Scope("session")
public class TaskListController extends SortableListController<TaskItem> {

    private static final long serialVersionUID = 1L;
    @Autowired(required = true)
    private transient TaskManager taskManager;
    @Autowired(required = true)
    private transient TaskDetailsController taskEdit;
    private TaskItem selectedTask;
    @Autowired(required = true)
    private transient RepositoryManager repositoryManager;

    private boolean listAll = false;
    private boolean selectAll = false;

    // private Set<TaskItem> runningTasks;
    private boolean activated;

    public static final String PAGE_NAVIGATION = "/admin/server/index?faces-redirect=true";
    public static final String PAGE_LEFT_NAVIGATION = "leftRunnableTasks";
    private static final String PARAM_TASK_OID = "taskOid";

    public TaskListController() {
        super();
    }

    @Override
    protected String listObjects() {
        List<TaskType> taskTypeList = repositoryManager.listObjects(TaskType.class, getOffset(),
                getRowsCount());
        List<TaskItem> runningTasks = getObjects();
        runningTasks.clear();
        for (TaskType taskType : taskTypeList) {
            runningTasks.add(new TaskItem(taskType, taskManager));
        }

        listAll = false;
        return PAGE_NAVIGATION;
    }

    public String listRunningTasks() {
        Set<Task> tasks = taskManager.getRunningTasks();
        List<TaskItem> runningTasks = getObjects();
        runningTasks.clear();
        for (Task task : tasks) {
            runningTasks.add(new TaskItem(task, taskManager));
        }
        listAll = true;
        return PAGE_NAVIGATION;
    }

    private TaskItem getSelectedTaskItem() {
        String taskOid = FacesUtils.getRequestParameter(PARAM_TASK_OID);
        if (StringUtils.isEmpty(taskOid)) {
            FacesUtils.addErrorMessage("Task oid not defined in request.");
            return null;
        }

        TaskItem taskItem = getTaskItem(taskOid);
        if (StringUtils.isEmpty(taskOid)) {
            FacesUtils.addErrorMessage("Task for oid '" + taskOid + "' not found.");
            return null;
        }

        return taskItem;
    }

    private TaskItem getTaskItem(String resourceOid) {
        for (TaskItem item : getObjects()) {
            if (item.getOid().equals(resourceOid)) {
                return item;
            }
        }

        return null;
    }

    public String deleteTask() {

        if (selectedTask.getOid() == null) {
            FacesUtils.addErrorMessage("No task to delete defined");
            throw new IllegalArgumentException("No task to delete defined.");
        }

        OperationResult result = new OperationResult(TaskDetailsController.class.getName() + ".deleteTask");
        result.addParam("taskOid", selectedTask.getOid());

        try {
            taskManager.deleteTask(selectedTask.getOid(), result);
            getObjects().remove(selectedTask);
            FacesUtils.addSuccessMessage("Task deleted sucessfully");
        } catch (ObjectNotFoundException ex) {
            FacesUtils.addErrorMessage(
                    "Task with oid " + selectedTask.getOid() + " not found. Reason: " + ex.getMessage(), ex);
            result.recordFatalError(
                    "Task with oid " + selectedTask.getOid() + " not found. Reason: " + ex.getMessage(), ex);
            return null;
        }

        return PAGE_NAVIGATION;
    }

    public String showTaskDetails() {
        selectedTask = getSelectedTaskItem();
        if (selectedTask == null) {
            return null;
        }

        taskEdit.setTask(selectedTask);
//		List<OperationResultType> opResultList = new ArrayList<OperationResultType>();
//		if (selectedTask.getResult() != null) {
//
//			OperationResultType opResultType = selectedTask.getResult().createOperationResultType();
//			opResultList.add(opResultType);
//			long token = 1220000000000000000L;
//			getResult(opResultType, opResultList, token);
//			taskDetails.setResults(opResultList);
//		} else {
//			taskDetails.setResults(opResultList);
//		}
        // template.setSelectedLeftId(ResourceDetailsController.NAVIGATION_LEFT);
        return TaskDetailsController.PAGE_NAVIGATION;
    }

//	public void getResult(OperationResultType opResult, List<OperationResultType> opResultList, long token) {
//
//		for (OperationResultType result : opResult.getPartialResults()) {
//			result.setToken(result.getToken() + token);
//			opResultList.add(result);
//			token += 1110000000000000000L;
//			getResult(result, opResultList, token);
//		}
//
//	}

    public void sortList(ActionEvent evt) {
        sort();
    }

    @Override
    protected void sort() {
        // Collections.sort(getObjects(), new
        // SortableListComparator<TaskItem>(getSortColumnName(),isAscending()));

    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public void selectAllPerformed(ValueChangeEvent event) {
        ControllerUtil.selectAllPerformed(event, getObjects());
    }

    public void selectPerformed(ValueChangeEvent evt) {
        this.selectAll = ControllerUtil.selectPerformed(evt, getObjects());
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public String deactivate() {

        // temporary solution: deactivates all running task manager threads

        taskManager.deactivateServiceThreads();
        FacesUtils
                .addWarnMessage("All task manager threads have been stopped. (Although tasks remain in RUNNING state, they will not be executed until you reactivate task manager threads.)");
        return null;

        // boolean selected = false;
        //
        // for (TaskItem task : getObjects()) {
        // if (task != null && task.isSelected()) {
        // selected = true;
        // break;
        // }
        // }
        //
        // if (selected) {
        // // taskManager.deactivateServiceThreads();
        //
        // Set<Task> tasks = taskManager.getRunningTasks();
        // List<TaskItem> runningTasks = getObjects();
        // runningTasks.clear();
        // // System.out.println(">>>>>>>>>>>>>>>> Filling runningTasks");
        // for (Task currTask : tasks) {
        // // System.out.println(">>>>>>>>>>>>>>>> start: " +
        // // currTask.getName());
        // runningTasks.add(new TaskItem(currTask));
        // // System.out.println(">>>>>>>>>>>>>>>> stop: " +
        // // currTask.getName());
        // }
        //
        // for (TaskItem task : getObjects()) {
        // // LOGGER.info("delete user {} is selected {}",
        // // guiUserDto.getFullName(), guiUserDto.isSelected());
        // // System.out.println(">>>>>>>>>>>>>>>> aaa: "+task.getName());
        //
        // if (task.isSelected() && runningTasks.contains(task)) {
        // try {
        // // System.out.println(">>>>>>>>>>>>>>>> deactivate task");
        // taskManager.deactivateServiceThreads();
        // setActivated(isActivated());
        // } catch (Exception ex) {
        // // LoggingUtils.logException(LOGGER,
        // // "Delete user failed", ex);
        // FacesUtils.addErrorMessage("Deactivate task failed: " +
        // ex.getMessage());
        // }
        // }
        // }
        // listAll = false;
        // setSelectAll(false);
        // return PAGE_NAVIGATION;
        //
        // } else {
        // FacesUtils.addErrorMessage("No task selected.");
        // }
        // return null;
    }

    public void reactivate() {

        taskManager.reactivateServiceThreads();
        FacesUtils.addSuccessMessage("All task manager threads have been reactivated.");
        return;

        //
        // boolean selected = false;
        //
        // for (TaskItem task : getObjects()) {
        // if (task != null && task.isSelected()) {
        // selected = true;
        // break;
        // }
        // }
        //
        // if (selected) {
        // taskManager.reactivateServiceThreads();
        // setActivated(isActivated());
        // } else {
        // FacesUtils.addErrorMessage("No task selected.");
        // }

    }

    public boolean isActivated() {
        return taskManager.getServiceThreadsActivationState();
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public TaskItem getSelectedTask() {
        return selectedTask;
    }

    public void setSelectedTask(TaskItem selectedTask) {
        this.selectedTask = selectedTask;
    }

    public boolean isListAll() {
        return listAll;
    }

    public void setListAll(boolean listAll) {
        this.listAll = listAll;
    }
}
