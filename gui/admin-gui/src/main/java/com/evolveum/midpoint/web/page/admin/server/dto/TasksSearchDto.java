/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    private TaskDtoExecutionStatusFilter status;
    private String category;
    private boolean showSubtasks;

    public boolean isShowSubtasks() {
        return showSubtasks;
    }

    public void setShowSubtasks(boolean showSubtasks) {
        this.showSubtasks = showSubtasks;
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
		return sb.toString();
	}
}
