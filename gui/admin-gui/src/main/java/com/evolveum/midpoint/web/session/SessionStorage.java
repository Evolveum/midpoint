/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.gui.impl.session.WorkItemsStorage;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.apache.commons.lang.StringUtils;

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
    public static final String KEY_ASSIGNMENTS_TAB = "assignmentsTab";
    public static final String KEY_INDUCEMENTS_TAB = "inducementsTab";
    public static final String KEY_INDUCED_ENTITLEMENTS_TAB = "inducedEntitlementsTab";
    public static final String KEY_OBJECT_POLICIES_TAB = "objectPoliciesTab";
    public static final String KEY_GLOBAL_POLICY_RULES_TAB = "globalPolicyRulesTab";
    public static final String KEY_LOGGING_TAB_APPENDER_TABLE = "loggingTabAppenderTable";
    public static final String KEY_LOGGING_TAB_LOGGER_TABLE = "loggingTabLoggerTable";
    public static final String KEY_FOCUS_PROJECTION_TABLE = "focusProjectionTable";
    public static final String KEY_NOTIFICATION_TAB_MAIL_SERVER_TABLE = "notificationTabMailServerTable";
    public static final String KEY_ROLE_MEMEBER_PANEL = "roleMemberPanel";
    public static final String KEY_ORG_MEMEBER_PANEL = "orgMemberPanel";
    public static final String KEY_SERVICE_MEMEBER_PANEL = "serviceMemberPanel";
    public static final String KEY_WORK_ITEMS = "workItems";
    public static final String KEY_OBJECT_LIST = "objectListPage";
    public static final String KEY_CASE_WORKITEMS_TAB = "workitemsTab";
    public static final String KEY_CASE_EVENTS_TAB = "caseEventsTab";
    public static final String KEY_ORG_STRUCTURE_PANEL_STORAGE = "orgStructurePanelStorage";

    private static final String KEY_TASKS = "tasks";
    private static final String KEY_SUBTASKS = "subtasks";
    private static final String KEY_CERT_CAMPAIGNS = "certCampaigns";
    private static final String KEY_CERT_DECISIONS = "certDecisions";

    /**
     * Contains state for first level menu items. Key is menu label text, value if true then
     * menu is expanded, if false menu is minimized.
     */
    private Map<String, Boolean> mainMenuState = new HashMap<>();

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

    public Map<String, Boolean> getMainMenuState() {
        return mainMenuState;
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

    public OrgStructurePanelStorage getOrgStructurePanelStorage() {
        if (pageStorageMap.get(KEY_ORG_STRUCTURE_PANEL_STORAGE) == null) {
            pageStorageMap.put(KEY_ORG_STRUCTURE_PANEL_STORAGE, new OrgStructurePanelStorage());
        }
        return (OrgStructurePanelStorage) pageStorageMap.get(KEY_ORG_STRUCTURE_PANEL_STORAGE);
    }

    public ObjectListStorage getObjectListStorage(String key) {
        if (pageStorageMap.get(key) != null) {
            pageStorageMap.put(key, new ObjectListStorage());
        }
        return (ObjectListStorage) pageStorageMap.get(key);
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

    public ResourceContentStorage getResourceContentStorage(ShadowKindType kind, String searchMode) {
        String key = getContentStorageKey(kind, searchMode);
        if (pageStorageMap.get(key) == null) {
            pageStorageMap.put(key, new ResourceContentStorage(kind));
        }
        return (ResourceContentStorage)pageStorageMap.get(key);

    }

    private ObjectTabStorage getObjectTabStorage(String key) {
        if (pageStorageMap.get(key) == null) {
            pageStorageMap.put(key, new ObjectTabStorage());
        }
        return (ObjectTabStorage)pageStorageMap.get(key);
    }

    public ObjectTabStorage getAssignmentsTabStorage() {
        return getObjectTabStorage(KEY_ASSIGNMENTS_TAB);
    }

    public ObjectTabStorage getInducementsTabStorage() {
        return getObjectTabStorage(KEY_INDUCEMENTS_TAB);
    }

    public ObjectTabStorage getInducedEntitlementsTabStorage() {
        return getObjectTabStorage(KEY_INDUCED_ENTITLEMENTS_TAB);
    }

    public ObjectTabStorage getCaseWorkitemsTabStorage() {
        return getObjectTabStorage(KEY_CASE_WORKITEMS_TAB);
    }

    public ObjectTabStorage getCaseEventsTabStorage() {
        return getObjectTabStorage(KEY_CASE_EVENTS_TAB);
    }

    public ObjectTabStorage getObjectPoliciesConfigurationTabStorage() {
        return getObjectTabStorage(KEY_OBJECT_POLICIES_TAB);
    }

    public ObjectTabStorage getGlobalPolicyRulesTabStorage() {
        return getObjectTabStorage(KEY_GLOBAL_POLICY_RULES_TAB);
    }

    public ObjectTabStorage getLoggingConfigurationTabAppenderTableStorage() {
        return getObjectTabStorage(KEY_LOGGING_TAB_APPENDER_TABLE);
    }

    public ObjectTabStorage getLoggingConfigurationTabLoggerTableStorage() {
        return getObjectTabStorage(KEY_LOGGING_TAB_LOGGER_TABLE);
    }

    public ObjectTabStorage getFocusProjectionTableStorage() {
        return getObjectTabStorage(KEY_FOCUS_PROJECTION_TABLE);
    }

    public ObjectTabStorage getNotificationConfigurationTabMailServerTableStorage() {
        return getObjectTabStorage(KEY_NOTIFICATION_TAB_MAIL_SERVER_TABLE);
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

    public WorkItemsStorage getWorkItemStorage() {
        if (pageStorageMap.get(KEY_WORK_ITEMS) == null) {
            pageStorageMap.put(KEY_WORK_ITEMS, new WorkItemsStorage());
        }
        return (WorkItemsStorage)pageStorageMap.get(KEY_WORK_ITEMS);
    }

    public TasksStorage getTasks() {
        if (pageStorageMap.get(KEY_TASKS) == null) {
            pageStorageMap.put(KEY_TASKS, new TasksStorage());
        }
        return (TasksStorage)pageStorageMap.get(KEY_TASKS);
    }

    public TasksStorage getSubtasks() {
        if (pageStorageMap.get(KEY_SUBTASKS) == null) {
            pageStorageMap.put(KEY_SUBTASKS, new TasksStorage());
        }
        return (TasksStorage)pageStorageMap.get(KEY_SUBTASKS);
    }

    public CertCampaignsStorage getCertCampaigns() {
        if (pageStorageMap.get(KEY_CERT_CAMPAIGNS) == null) {
            pageStorageMap.put(KEY_CERT_CAMPAIGNS, new CertCampaignsStorage());
        }
        return (CertCampaignsStorage)pageStorageMap.get(KEY_CERT_CAMPAIGNS);
    }

    public CertDecisionsStorage getCertDecisions() {
        if (pageStorageMap.get(KEY_CERT_DECISIONS) == null) {
            pageStorageMap.put(KEY_CERT_DECISIONS, new CertDecisionsStorage());
        }
        return (CertDecisionsStorage)pageStorageMap.get(KEY_CERT_DECISIONS);
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
        if (key.startsWith(KEY_OBJECT_LIST)) {
            pageStorage = new ObjectListStorage();
            pageStorageMap.put(key, pageStorage);
        } else  if (KEY_USERS.equals(key)){
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
        } else if (KEY_ORG_MEMEBER_PANEL.equals(key)) {
            pageStorage = new MemberPanelStorage();
            pageStorageMap.put(KEY_ORG_MEMEBER_PANEL, pageStorage);
        } else if (KEY_ROLE_MEMEBER_PANEL.equals(key)) {
            pageStorage = new MemberPanelStorage();
            pageStorageMap.put(KEY_ROLE_MEMEBER_PANEL, pageStorage);
        } else if (KEY_SERVICE_MEMEBER_PANEL.equals(key)) {
            pageStorage = new MemberPanelStorage();
            pageStorageMap.put(KEY_SERVICE_MEMEBER_PANEL, pageStorage);
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

    public void clearResourceContentStorage() {
        pageStorageMap.remove(KEY_RESOURCE_ACCOUNT_CONTENT + KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_ACCOUNT_CONTENT + KEY_RESOURCE_PAGE_RESOURCE_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_ENTITLEMENT_CONTENT + KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_ENTITLEMENT_CONTENT + KEY_RESOURCE_PAGE_RESOURCE_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_GENERIC_CONTENT + KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_GENERIC_CONTENT + KEY_RESOURCE_PAGE_RESOURCE_CONTENT);
        pageStorageMap.remove(KEY_RESOURCE_OBJECT_CLASS_CONTENT);
    }
}
