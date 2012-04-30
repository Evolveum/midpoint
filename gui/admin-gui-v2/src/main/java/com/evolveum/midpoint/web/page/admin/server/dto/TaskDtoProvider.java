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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.task.api.ClusterStatusInformation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class TaskDtoProvider extends SortableDataProvider<TaskDto> {

    private List<TaskDto> availableData;

    public TaskDtoProvider() {
        setSort("name", SortOrder.ASCENDING);
    }

    private TaskManager getTaskManager() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getTaskManager();
    }

    @Override
    public Iterator<? extends TaskDto> iterator(int first, int count) {
        getAvailableData().clear();

//        TaskManager manager = getTaskManager();
//        ClusterStatusInformation info = manager.getRunningTasksClusterwide();
//        List<Task> tasks = manager.searchTasks(query, paging, info, result);
//
//        for (Task task : tasks) {
//            getAvailableData().add(new TaskDto(task));
//        }

        return getAvailableData().iterator();
    }

    public List<TaskDto> getAvailableData() {
        if (availableData == null) {
            availableData = new ArrayList<TaskDto>();
        }
        return availableData;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public IModel<TaskDto> model(TaskDto object) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
