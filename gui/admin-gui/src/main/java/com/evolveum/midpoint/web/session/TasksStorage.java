/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.page.admin.server.dto.TasksSearchDto;

/**
 *  @author shood
 * */
public class TasksStorage implements PageStorage{
    private static final long serialVersionUID = 1L;

    /**
     *  DTO used for search purposes in {@link com.evolveum.midpoint.web.page.admin.server.PageTasks}
     * */
    private TasksSearchDto tasksSearch;

    /**
     *  Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.server.PageTasks}
     * */
    private ObjectPaging tasksPaging;

    private Search search;

    public TasksSearchDto getTasksSearch() {
        return tasksSearch;
    }

    public void setTasksSearch(TasksSearchDto tasksSearch) {
        this.tasksSearch = tasksSearch;
    }

    @Override
    public ObjectPaging getPaging() {
        return tasksPaging;
    }

    @Override
    public void setPaging(ObjectPaging tasksPaging) {
        this.tasksPaging = tasksPaging;
    }

    @Override
    public Search getSearch() {
        return search;
    }

    @Override
    public void setSearch(Search search) {
        this.search = search;

    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("TasksStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "tasksSearch", tasksSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "tasksPaging", tasksPaging, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "search", search, indent+1);
        return sb.toString();
    }
}
