package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskEditableState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.util.ArrayList;
import java.util.List;

public class TaskSelectableBean extends SelectableBeanImpl<TaskType> {

//    private boolean loadedChildren;
//    private List<TaskSelectableBean> children;

//    private TaskEditableState currentEditableState = new TaskEditableState();
//    private TaskEditableState originalEditableState;

    public TaskSelectableBean(TaskType taskType) {
        super(taskType);
    }

//    public boolean isLoadedChildren() {
//        return loadedChildren;
//    }
//
//    public void setLoadedChildren(boolean loadedChildren) {
//        this.loadedChildren = loadedChildren;
//    }

//    public List<TaskSelectableBean> getChildren() {
//        return children;
////    }
//
//    public void setChildren(List<TaskSelectableBean> children) {
//        this.children = children;
//    }

//    public TaskEditableState getCurrentEditableState() {
//        return currentEditableState;
//    }
//
//    public void setCurrentEditableState(TaskEditableState currentEditableState) {
//        this.currentEditableState = currentEditableState;
//    }

//    public boolean isCoordinator() {
//        return getValue().getWorkManagement() != null && getValue().getWorkManagement().getTaskKind() == TaskKindType.COORDINATOR;
//    }
//
//    public boolean isPartitionedMaster() {
//        return getValue().getWorkManagement() != null && getValue().getWorkManagement().getTaskKind() == TaskKindType.PARTITIONED_MASTER;
//    }

    public static List<String> getOids(List<TaskType> taskDtoList) {
        List<String> retval = new ArrayList<>();
        for (TaskType taskDto : taskDtoList) {
            retval.add(taskDto.getOid());
        }
        return retval;
    }
}
