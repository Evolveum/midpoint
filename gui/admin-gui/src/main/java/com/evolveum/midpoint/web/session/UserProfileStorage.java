/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shood
 * @author Viliam Repan (lazyman)
 */
public class UserProfileStorage implements Serializable {

    public static final int DEFAULT_PAGING_SIZE = 10;

    /*
    *   Enum containing IDs of all tables. where paging size can be adjusted
    * */
    public enum TableId {
        TABLE_ROLES,
        TABLE_USERS,
        TREE_TABLE_PANEL_CHILD,
        TREE_TABLE_PANEL_MEMBER,
        TREE_TABLE_PANEL_MANAGER,
        CONF_PAGE_ACCOUNTS,
        CONF_DEBUG_LIST_PANEL,
        PAGE_CREATED_REPORTS_PANEL,
        PAGE_RESOURCE_PANEL,
        PAGE_RESOURCES_PANEL,
        PAGE_RESOURCE_ACCOUNTS_PANEL,
        PAGE_TASKS_PANEL,
        PAGE_USERS_PANEL,
        PAGE_WORK_ITEMS,
        PAGE_RESOURCES_CONNECTOR_HOSTS,
        PAGE_REPORTS
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
}
