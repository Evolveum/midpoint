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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *  @author shood
 * */
public class UserProfileStorage implements Serializable{

    public static Integer DEFAULT_PAGING_SIZE = 10;

    /*
    *   Enum containing IDs of all tables. where paging size can be adjusted
    * */
    public enum TableId{
        TABLE_ROLES,
        TABLE_USERS,
        TREE_TABLE_PANEL,
        CONF_PAGE_ACCOUNTS,
        CONF_DEBUG_LIST_PANEL,
        PAGE_CREATED_REPORTS_PANEL,
        PAGE_RESOURCE_PANEL,
        PAGE_RESOURCES_PANEL,
        PAGE_RESOURCE_ACCOUNTS_PANEL,
        PAGE_TASKS_PANEL,
        PAGE_USERS_PANEL,
        PAGE_WORK_ITEMS

    }

    private Map<TableId, Integer> pagingSizeMap = new HashMap<TableId, Integer>(){{
        put(TableId.TABLE_ROLES, null);
        put(TableId.TABLE_USERS, null);
        put(TableId.TREE_TABLE_PANEL, null);
        put(TableId.CONF_PAGE_ACCOUNTS, null);
        put(TableId.CONF_DEBUG_LIST_PANEL, null);
        put(TableId.PAGE_CREATED_REPORTS_PANEL, null);
        put(TableId.PAGE_RESOURCE_PANEL, null);
        put(TableId.PAGE_RESOURCES_PANEL, null);
        put(TableId.PAGE_RESOURCE_ACCOUNTS_PANEL, null);
        put(TableId.PAGE_TASKS_PANEL, null);
        put(TableId.PAGE_USERS_PANEL, null);
        put(TableId.PAGE_WORK_ITEMS, null);
    }};

    public Map<TableId, Integer> getPageSizingMap(){
        return pagingSizeMap;
    }

    public Integer getPagingSize(TableId key){
        if(key == null){
            return DEFAULT_PAGING_SIZE;
        }

        Integer size = pagingSizeMap.get(key);

        if(size == null){
            return DEFAULT_PAGING_SIZE;
        } else {
            return size;
        }
    }

    public void setPagingSize(TableId key, Integer size){
        pagingSizeMap.put(key, size);
    }
}
