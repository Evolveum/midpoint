/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import java.io.Serializable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author lazyman
 */
public class TasksSearchDto implements Serializable, DebugDumpable {
    private static final long serialVersionUID = 1L;

    public static final String F_STATUS = "status";
    public static final String F_CATEGORY = "category";
    public static final String F_SHOW_SUBTASKS = "showSubtasks";
    public static final String F_SHOW_PROGRESS = "showProgress";

    private TaskDtoExecutionStatusFilter status;
    private String category;
    private boolean showSubtasks;
    private boolean showProgress;

    public boolean isShowSubtasks() {
        return showSubtasks;
    }

    public void setShowSubtasks(boolean showSubtasks) {
        this.showSubtasks = showSubtasks;
    }

    public boolean isShowProgress() {
        return showProgress;
    }

    public void setShowProgress(boolean showProgress) {
        this.showProgress = showProgress;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public TaskDtoExecutionStatusFilter getStatus() {
        return status;
    }

    public void setStatus(TaskDtoExecutionStatusFilter status) {
        this.status = status;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("TasksSearchDto\n");
        DebugUtil.debugDumpWithLabelLn(sb, "status", status==null?null:status.toString(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "category", category, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "showSubtasks", showSubtasks, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "showProgress", showProgress, indent+1);
        return sb.toString();
    }
}
