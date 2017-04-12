/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.session;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shood
 * @author Viliam Repan (lazyman)
 */
public class UserProfileStorage implements Serializable, DebugDumpable {

	
	private static final long serialVersionUID = 1L;
	public static final int DEFAULT_PAGING_SIZE = 20;

    /*
    *   Enum containing IDs of all tables. where paging size can be adjusted
    * */
    public enum TableId {
        TABLE_ROLES,
        TABLE_USERS,
        TABLE_SERVICES,
        TABLE_RESOURCES,
        ROLE_MEMEBER_PANEL,
        ORG_MEMEBER_PANEL,
        TREE_TABLE_PANEL_CHILD,
        TREE_TABLE_PANEL_MEMBER,
        TREE_TABLE_PANEL_MANAGER,
        CONF_PAGE_ACCOUNTS,
        CONF_DEBUG_LIST_PANEL,
        PAGE_CREATED_REPORTS_PANEL,
        PAGE_RESOURCE_PANEL,
        PAGE_RESOURCES_PANEL,
        PAGE_RESOURCE_TASKS_PANEL,
        PAGE_RESOURCE_ACCOUNTS_PANEL_REPOSITORY_MODE,
        PAGE_RESOURCE_ACCOUNTS_PANEL_RESOURCE_MODE,
        PAGE_RESOURCE_ENTITLEMENT_PANEL_REPOSITORY_MODE,
        PAGE_RESOURCE_ENTITLEMENT_PANEL_RESOURCE_MODE,
        PAGE_RESOURCE_GENERIC_PANEL_REPOSITORY_MODE,
        PAGE_RESOURCE_GENERIC_PANEL_RESOURCE_MODE,
        PAGE_RESOURCE_OBJECT_CLASS_PANEL,
        PAGE_TASKS_PANEL,
        PAGE_TASKS_NODES_PANEL,
        PAGE_USERS_PANEL,
        PAGE_WORK_ITEMS,
        PAGE_WORKFLOW_REQUESTS,
        PAGE_RESOURCES_CONNECTOR_HOSTS,
        PAGE_REPORTS,
        PAGE_CERT_CAMPAIGN_OUTCOMES_PANEL,
        PAGE_CERT_CAMPAIGNS_PANEL,
        PAGE_CERT_DECISIONS_PANEL,
        PAGE_CERT_DEFINITIONS_PANEL,
		PAGE_WORK_ITEM_HISTORY_PANEL,
		PAGE_TASK_HISTORY_PANEL,
		PAGE_TASK_CURRENT_WORK_ITEMS_PANEL,
        PAGE_AUDIT_LOG_VIEWER,
        TASK_EVENTS_TABLE
		
    }

    private Map<TableId, Integer> tables = new HashMap<TableId, Integer>();

    public Integer getPagingSize(TableId key) {
        Validate.notNull(key, "Key must not be null.");

        Integer size = tables.get(key);
        return size == null ? DEFAULT_PAGING_SIZE : size;
    }

    public void setPagingSize(TableId key, Integer size) {
        Validate.notNull(key, "Key must not be null.");

        tables.put(key, size);
    }

    public Map<TableId, Integer> getTables() {
        return tables;
    }

    @Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("UserProfileStorage\n");
		DebugUtil.debugDumpWithLabel(sb, "tables", tables, indent+1);
		return sb.toString();
	}
    
}
