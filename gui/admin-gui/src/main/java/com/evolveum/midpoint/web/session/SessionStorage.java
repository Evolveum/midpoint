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

package com.evolveum.midpoint.web.session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author lazyman
 */
public class SessionStorage implements Serializable, DebugDumpable {

   private static final long serialVersionUID = 1L;

    public static final String KEY_CONFIGURATION = "configuration";
    public static final String KEY_USERS = "users";
    public static final String KEY_REPORTS = "reports";
    public static final String KEY_RESOURCES = "resources";
    public static final String KEY_ROLES = "roles";
    public static final String KEY_SERVICES = "services";
    public static final String KEY_ROLE_MEMBERS = "roleMembers";
    public static final String KEY_ROLE_CATALOG = "roleCatalog";
    public static final String KEY_AUDIT_LOG = "auditLog";
    public static final String KEY_USER_HISTORY_AUDIT_LOG = "userHistoryAuditLog";
    public static final String KEY_RESOURCE_ACCOUNT_CONTENT = "resourceAccountContent";
    public static final String KEY_RESOURCE_ENTITLEMENT_CONTENT = "resourceEntitlementContent";
    public static final String KEY_RESOURCE_GENERIC_CONTENT = "resourceGenericContent";
    public static final String KEY_RESOURCE_OBJECT_CLASS_CONTENT = "resourceObjectClassContent";
    public static final String KEY_RESOURCE_PAGE_RESOURCE_CONTENT = "Resource";
    public static final String KEY_RESOURCE_PAGE_REPOSITORY_CONTENT = "Repository";

    private static final String KEY_TASKS = "tasks";

    /**
    *   Store session information for user preferences about paging size in midPoint GUI
    * */
    private UserProfileStorage userProfile;

    /**
     * place to store information in session for various pages
     */
    private Map<String, PageStorage> pageStorageMap = new HashMap<>();

    public Map<String, PageStorage> getPageStorageMap() {
		return pageStorageMap;
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

    public RoleCatalogStorage getRoleCatalog() {
        if (pageStorageMap.get(KEY_ROLE_CATALOG) == null) {
            pageStorageMap.put(KEY_ROLE_CATALOG, new RoleCatalogStorage());
        }
        return (RoleCatalogStorage)pageStorageMap.get(KEY_ROLE_CATALOG);
    }

    public AuditLogStorage getAuditLog() {
        if (pageStorageMap.get(KEY_AUDIT_LOG) == null) {
            pageStorageMap.put(KEY_AUDIT_LOG, new AuditLogStorage());
        }
        return (AuditLogStorage)pageStorageMap.get(KEY_AUDIT_LOG);
    }

    public AuditLogStorage getUserHistoryAuditLog() {
        if (pageStorageMap.get(KEY_USER_HISTORY_AUDIT_LOG) == null) {
            pageStorageMap.put(KEY_USER_HISTORY_AUDIT_LOG, new AuditLogStorage());
        }
        return (AuditLogStorage)pageStorageMap.get(KEY_USER_HISTORY_AUDIT_LOG);
    }

    public void setUserHistoryAuditLog(AuditLogStorage storage) {
        if (pageStorageMap.containsKey(KEY_USER_HISTORY_AUDIT_LOG)) {
            pageStorageMap.remove(KEY_USER_HISTORY_AUDIT_LOG);
        }
        pageStorageMap.put(KEY_USER_HISTORY_AUDIT_LOG, storage);
    }


    public ServicesStorage getServices() {
        if (pageStorageMap.get(KEY_SERVICES) == null) {
            pageStorageMap.put(KEY_SERVICES, new ServicesStorage());
        }
        return (ServicesStorage)pageStorageMap.get(KEY_SERVICES);
    }

    public RoleMembersStorage getRoleMembers() {
    	if (pageStorageMap.get(KEY_ROLE_MEMBERS) == null) {
            pageStorageMap.put(KEY_ROLE_MEMBERS, new RoleMembersStorage());
        }
        return (RoleMembersStorage)pageStorageMap.get(KEY_ROLE_MEMBERS);
    }

    public ResourceContentStorage getResourceContentStorage(ShadowKindType kind, String searchMode) {
    	String key = getContentStorageKey(kind, searchMode);
    	if (pageStorageMap.get(key) == null) {
            pageStorageMap.put(key, new ResourceContentStorage(kind));
        }
        return (ResourceContentStorage)pageStorageMap.get(key);

	}

    private String getContentStorageKey(ShadowKindType kind, String searchMode) {
    	if (kind == null) {
			return KEY_RESOURCE_OBJECT_CLASS_CONTENT;
		}

		switch (kind) {
			case ACCOUNT:
				return KEY_RESOURCE_ACCOUNT_CONTENT + searchMode;

			case ENTITLEMENT:
				return KEY_RESOURCE_ENTITLEMENT_CONTENT + searchMode;

			case GENERIC:
				return KEY_RESOURCE_GENERIC_CONTENT + searchMode;
			default:
				return KEY_RESOURCE_OBJECT_CLASS_CONTENT;

		}
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

    public PageStorage initPageStorage(String key){
    	PageStorage pageStorage = null;
    	if (KEY_USERS.equals(key)){
    		pageStorage = new UsersStorage();
    		pageStorageMap.put(KEY_USERS, pageStorage);

    	} else if (KEY_ROLES.equals(key)){
    		pageStorage = new RolesStorage();
    		pageStorageMap.put(KEY_ROLES, pageStorage);
    	} else if (KEY_SERVICES.equals(key)) {
    		pageStorage = new ServicesStorage();
    		pageStorageMap.put(KEY_SERVICES, pageStorage);
    	} else if (KEY_RESOURCES.equals(key)) {
    		pageStorage = new ResourcesStorage();
    		pageStorageMap.put(KEY_RESOURCES, pageStorage);
    	}
    	return pageStorage;
    	//TODO: fixme
    }

    public void setUserProfile(UserProfileStorage profile){
        userProfile = profile;
    }

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("SessionStorage\n");
		DebugUtil.debugDumpWithLabelLn(sb, "userProfile", userProfile, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "pageStorageMap", pageStorageMap, indent+1);
		return sb.toString();
	}

	public void dumpSizeEstimates(StringBuilder sb, int indent) {
		DebugUtil.dumpObjectSizeEstimate(sb, "SessionStorage", this, indent);
		if (userProfile != null) {
			sb.append("\n");
			DebugUtil.dumpObjectSizeEstimate(sb, "userProfile", userProfile, indent + 1);
		}
		sb.append("\n");
		DebugUtil.dumpObjectSizeEstimate(sb, "pageStorageMap", (Serializable)pageStorageMap, indent + 1);
		for (Entry<String,PageStorage> entry: pageStorageMap.entrySet()) {
			sb.append("\n");
			DebugUtil.dumpObjectSizeEstimate(sb, entry.getKey(), entry.getValue(), indent + 2);
		}
	}

    public void clearResourceContentStorage(){
        pageStorageMap.remove(KEY_RESOURCE_ACCOUNT_CONTENT + KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_ACCOUNT_CONTENT + KEY_RESOURCE_PAGE_RESOURCE_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_ENTITLEMENT_CONTENT + KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_ENTITLEMENT_CONTENT + KEY_RESOURCE_PAGE_RESOURCE_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_GENERIC_CONTENT + KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_GENERIC_CONTENT + KEY_RESOURCE_PAGE_RESOURCE_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_OBJECT_CLASS_CONTENT);
    }
}
