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

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public class SessionStorage implements Serializable {

    /**
     * place to store "previous page" for back button
     */
    private Class<? extends WebPage> previousPage;
    /**
     * place to store "previous page" parameters for back button
     */
    private PageParameters previousPageParams;

    /**
     * place to store information in session for various pages
     */
    private Map<String, PageStorage> pageStorageMap = new HashMap<>();

    private static final String KEY_CONFIGURATION = "configuration";
    private static final String KEY_USERS = "users";
    private static final String KEY_REPORTS = "reports";
    private static final String KEY_RESOURCES = "resources";
    private static final String KEY_ROLES = "roles";
    private static final String KEY_TASKS = "tasks";

    /**
    *   Store session information for user preferences about paging size in midPoint GUI
    * */
    private UserProfileStorage userProfile;

    public Class<? extends WebPage> getPreviousPage() {
        return previousPage;
    }

    public void setPreviousPage(Class<? extends WebPage> previousPage) {
        this.previousPage = previousPage;
    }

    public PageParameters getPreviousPageParams() {
        return previousPageParams;
    }

    public void setPreviousPageParams(PageParameters previousPageParams) {
        this.previousPageParams = previousPageParams;
    }

    public ConfigurationStorage getConfiguration() {
        if (pageStorageMap.get(KEY_CONFIGURATION) == null) {
            pageStorageMap.put(KEY_CONFIGURATION, new ConfigurationStorage());
        }
        return (ConfigurationStorage)pageStorageMap.get(KEY_CONFIGURATION);
    }

    public UsersStorage getUsers() {
        if (pageStorageMap.get(KEY_USERS) == null) {
            pageStorageMap.put(KEY_USERS, new UsersStorage());
        }
        return (UsersStorage)pageStorageMap.get(KEY_USERS);
    }

    public ResourcesStorage getResources() {
        if (pageStorageMap.get(KEY_RESOURCES) == null) {
            pageStorageMap.put(KEY_RESOURCES, new ResourcesStorage());
        }
        return (ResourcesStorage)pageStorageMap.get(KEY_RESOURCES);
    }

    public RolesStorage getRoles() {
        if (pageStorageMap.get(KEY_ROLES) == null) {
            pageStorageMap.put(KEY_ROLES, new RolesStorage());
        }
        return (RolesStorage)pageStorageMap.get(KEY_ROLES);
    }

    public TasksStorage getTasks() {
        if (pageStorageMap.get(KEY_TASKS) == null) {
            pageStorageMap.put(KEY_TASKS, new TasksStorage());
        }
        return (TasksStorage)pageStorageMap.get(KEY_TASKS);
    }

    public ReportsStorage getReports() {
        if (pageStorageMap.get(KEY_REPORTS) == null) {
            pageStorageMap.put(KEY_REPORTS, new ReportsStorage());
        }
        return (ReportsStorage)pageStorageMap.get(KEY_REPORTS);
    }

    public UserProfileStorage getUserProfile(){
        if(userProfile == null){
            userProfile = new UserProfileStorage();
        }
        return userProfile;
    }

    public void setUserProfile(UserProfileStorage profile){
        userProfile = profile;
    }

    public void clearPagingInSession(boolean clearPaging){
        if(clearPaging){
            pageStorageMap.clear();
        }
    }
}
