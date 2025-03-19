/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.self.requestAccess.RequestAccess;
import com.evolveum.midpoint.gui.impl.session.ContainerTabStorage;
import com.evolveum.midpoint.gui.impl.session.WorkItemsStorage;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.page.admin.roles.SearchBoxConfigurationHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author lazyman
 */
public class SessionStorage implements Serializable, DebugDumpable {

    @Serial private static final long serialVersionUID = 1L;

    public static final String KEY_SIMULATION = "simulation";
    public static final String KEY_CONFIGURATION = "configuration";
    public static final String KEY_ROLE_MEMBERS = "roleMembers";
    public static final String KEY_ROLE_CATALOG = "roleCatalog";
    public static final String KEY_AUDIT_LOG = "auditLog";
    public static final String KEY_USER_HISTORY_AUDIT_LOG = "userHistoryAuditLog";
    public static final String KEY_OBJECT_HISTORY_AUDIT_LOG = "objectHistoryAuditLog";
    public static final String KEY_EVENT_DETAIL_AUDIT_LOG = "eventDetailAuditLog";
    public static final String KEY_RESOURCE_ACCOUNT_CONTENT = "resourceAccountContent";
    public static final String KEY_RESOURCE_ENTITLEMENT_CONTENT = "resourceEntitlementContent";
    public static final String KEY_RESOURCE_GENERIC_CONTENT = "resourceGenericContent";
    public static final String KEY_RESOURCE_OBJECT_CLASS_CONTENT = "resourceObjectClassContent";
    public static final String KEY_RESOURCE_PAGE_RESOURCE_CONTENT = "Resource";
    public static final String KEY_RESOURCE_PAGE_REPOSITORY_CONTENT = "Repository";
    public static final String KEY_ASSIGNMENTS_TAB = "assignmentsTab";
    public static final String KEY_INDUCEMENTS_TAB = "inducementsTab";
    public static final String KEY_TRIGGERS_TAB = "triggersTab";
    public static final String KEY_INDUCED_ENTITLEMENTS_TAB = "inducedEntitlementsTab";
    public static final String KEY_OBJECT_POLICIES_TAB = "objectPoliciesTab";
    public static final String KEY_GLOBAL_POLICY_RULES_TAB = "globalPolicyRulesTab";
    public static final String KEY_LOGGING_TAB_APPENDER_TABLE = "loggingTabAppenderTable";
    public static final String KEY_LOGGING_TAB_LOGGER_TABLE = "loggingTabLoggerTable";
    public static final String KEY_FOCUS_PROJECTION_TABLE = "focusProjectionTable";
    public static final String KEY_FOCUS_CASES_TABLE = "focusCasesTable";
    public static final String KEY_NOTIFICATION_TAB_MAIL_SERVER_TABLE = "notificationTabMailServerTable";
    public static final String KEY_ROLE_MEMBER_PANEL = UserProfileStorage.TableId.ROLE_MEMBER_PANEL.name();
    public static final String KEY_ORG_MEMBER_PANEL = UserProfileStorage.TableId.ORG_MEMBER_PANEL.name();
    public static final String KEY_SERVICE_MEMBER_PANEL = UserProfileStorage.TableId.SERVICE_MEMBER_PANEL.name();
    public static final String KEY_POLICY_MEMBER_PANEL = UserProfileStorage.TableId.POLICY_MEMBER_PANEL.name();
    public static final String KEY_ARCHETYPE_MEMBER_PANEL = UserProfileStorage.TableId.ARCHETYPE_MEMBER_PANEL.name();

    public static final String KEY_GOVERNANCE_CARDS_PANEL = UserProfileStorage.TableId.PANEL_GOVERNANCE_CARDS.name();
    public static final String KEY_WORK_ITEMS = "workItems";
    public static final String KEY_OBJECT_LIST = "containerListPage";
    public static final String KEY_CASE_WORKITEMS_TAB = "workitemsTab";
    public static final String KEY_CHILD_CASES_TAB = "childCasesTab";
    public static final String KEY_CASE_EVENTS_TAB = "caseEventsTab";
    public static final String KEY_ORG_STRUCTURE_PANEL_STORAGE = "orgStructurePanelStorage";

    private static final String KEY_TASKS = "tasks";
    private static final String KEY_SUBTASKS = "subtasks";
    private static final String KEY_CERT_CAMPAIGNS = "certCampaigns";
    public static final String KEY_CERT_DECISIONS = "certDecisions";

    public enum Mode {

        LIGHT, DARK;
    }

    private Mode mode = Mode.LIGHT;

    /**
     * Store session information for user preferences about paging size in midPoint GUI
     */
    private UserProfileStorage userProfile;

    /**
     * place to store information in session for various pages
     */
    private Map<String, PageStorage> pageStorageMap = new HashMap<>();
    private Map<String, ObjectDetailsStorage> detailsStorageMap = new HashMap<>();

    /**
     * Contains state for the first level menu items.
     * Key is menu label text, value if true then menu is expanded, if false menu is minimized.
     */
    private Map<String, Boolean> mainMenuState = new HashMap<>();

    private RequestAccess requestAccess = new RequestAccess();

    public RequestAccess getRequestAccess() {
        return requestAccess;
    }

    public void setRequestAccess(RequestAccess requestAccess) {
        this.requestAccess = requestAccess;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(@NotNull Mode mode) {
        this.mode = mode;
    }

    public Map<String, PageStorage> getPageStorageMap() {
        return pageStorageMap;
    }

    public Map<String, Boolean> getMainMenuState() {
        return mainMenuState;
    }

    public <T extends PageStorage> T getPageStorage(@NotNull String key, @NotNull Class<T> type) {
        PageStorage ps = pageStorageMap.get(key);
        if (ps == null) {
            return null;
        }

        if (!type.isAssignableFrom(ps.getClass())) {
            throw new IllegalStateException("Page storage map contains key '" + key + "' with different type of object ("
                    + ps.getClass().getName() + ") that expected '" + type.getClass().getName() + "'");
        }

        return (T) ps;
    }

    public <T extends PageStorage> void setPageStorage(@NotNull String key, T value) {
        if (value == null) {
            pageStorageMap.remove(key);
        } else {
            pageStorageMap.put(key, value);
        }
    }

    private <T extends PageStorage> T getPageStorage(@NotNull String key, @NotNull T defaultValue) {
        T ps = getPageStorage(key, (Class<T>) defaultValue.getClass());
        if (ps == null) {
            ps = defaultValue;
            setPageStorage(key, ps);
        }
        return ps;
    }

    public GenericPageStorage getConfiguration() {
        return getPageStorage(KEY_CONFIGURATION, new GenericPageStorage());
    }

    public OrgStructurePanelStorage getOrgStructurePanelStorage() {
        return getPageStorage(KEY_ORG_STRUCTURE_PANEL_STORAGE, new OrgStructurePanelStorage());
    }

    public ObjectListStorage getObjectListStorage(String key) {
        return getPageStorage(key, new ObjectListStorage());
    }

    public ObjectDetailsStorage getObjectDetailsStorage(String key) {
        return detailsStorageMap.get(key);
    }

    public void setObjectDetailsStorage(String key, ContainerPanelConfigurationType config) {
        ObjectDetailsStorage storage = new ObjectDetailsStorage();
        storage.setDefaultConfiguration(config);
        detailsStorageMap.put(key, storage);
    }

    public AuditLogStorage getAuditLog() {
        return getPageStorage(KEY_AUDIT_LOG, new AuditLogStorage());
    }

    public AuditLogStorage getObjectHistoryAuditLog(QName objectType) {
        String key = objectType.getLocalPart() + "." + KEY_OBJECT_HISTORY_AUDIT_LOG;
        return getPageStorage(key, new AuditLogStorage());
    }

    public void setObjectHistoryAuditLog(QName objectType, AuditLogStorage storage) {
        setPageStorage(objectType.getLocalPart() + "." + KEY_OBJECT_HISTORY_AUDIT_LOG, storage);
    }

    @Deprecated
    public ResourceContentStorage getResourceContentStorage(ShadowKindType kind, String searchMode) {
        String key = getContentStorageKey(kind, searchMode);
        return getPageStorage(key, new ResourceContentStorage(kind));

    }

    public ResourceContentStorage getResourceContentStorage(ShadowKindType kind) {
        String key = getContentStorageKey(kind, KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        return getPageStorage(key, new ResourceContentStorage(kind));

    }

    private ContainerTabStorage getContainerTabStorage(String key) {
        return getPageStorage(key, new ContainerTabStorage());
    }

    public ContainerTabStorage getNotificationConfigurationTabMailServerTableStorage() {
        return getContainerTabStorage(KEY_NOTIFICATION_TAB_MAIL_SERVER_TABLE);
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
        return getPageStorage(KEY_WORK_ITEMS, new WorkItemsStorage());
    }

    public CertCampaignsStorage getCertCampaigns() {
        return getPageStorage(KEY_CERT_CAMPAIGNS, new CertCampaignsStorage());
    }

    public CertDecisionsStorage getCertDecisions() {
        return getPageStorage(KEY_CERT_DECISIONS, new CertDecisionsStorage());
    }

    public UserProfileStorage getUserProfile() {
        if (userProfile == null) {
            userProfile = new UserProfileStorage();
        }
        return userProfile;
    }

    public PageStorage initPageStorage(String key) {
        if (key == null) {
            return null;
        }

        PageStorage pageStorage = null;
        if (key.startsWith(KEY_OBJECT_LIST)) {
            pageStorage = new ObjectListStorage();
        } else if (key.startsWith(KEY_ORG_MEMBER_PANEL)
                || key.startsWith(KEY_ROLE_MEMBER_PANEL)
                || key.startsWith(KEY_SERVICE_MEMBER_PANEL)
                || key.startsWith(KEY_POLICY_MEMBER_PANEL)
                || key.startsWith(KEY_ARCHETYPE_MEMBER_PANEL)
                || key.startsWith(KEY_GOVERNANCE_CARDS_PANEL)) {
            pageStorage = new MemberPanelStorage();
        } else if (KEY_ASSIGNMENTS_TAB.equals(key)
                || KEY_INDUCEMENTS_TAB.equals(key)
                || KEY_CASE_EVENTS_TAB.equals(key)
                || KEY_TRIGGERS_TAB.equals(key)
                || KEY_INDUCED_ENTITLEMENTS_TAB.equals(key)
                || KEY_CASE_WORKITEMS_TAB.equals(key)
                || KEY_OBJECT_POLICIES_TAB.equals(key)
                || KEY_GLOBAL_POLICY_RULES_TAB.equals(key)
                || KEY_LOGGING_TAB_APPENDER_TABLE.equals(key)
                || KEY_LOGGING_TAB_LOGGER_TABLE.equals(key)
                || KEY_FOCUS_PROJECTION_TABLE.equals(key)) {
            pageStorage = getContainerTabStorage(key);
        } else if (KEY_AUDIT_LOG.equals(key)
                || key.startsWith(KEY_OBJECT_HISTORY_AUDIT_LOG)) {
            pageStorage = new AuditLogStorage();
        }
        if (pageStorage != null) {
            pageStorageMap.put(key, pageStorage);
        }
        return pageStorage;
        //TODO: fixme
    }

    public MemberPanelStorage initMemberStorage(String storageKey, SearchBoxConfigurationHelper searchBoxConfig) {
        PageStorage pageStorage = initPageStorage(storageKey);
        if (!(pageStorage instanceof MemberPanelStorage)) {
            return null;
        }
        return (MemberPanelStorage) pageStorage;
    }

    public MemberPanelStorage initMemberStorage(String storageKey) {
        PageStorage pageStorage = initPageStorage(storageKey);
        if (!(pageStorage instanceof MemberPanelStorage)) {
            return null;
        }
        return (MemberPanelStorage) pageStorage;
    }

    public void setUserProfile(UserProfileStorage profile) {
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
        DebugUtil.debugDumpWithLabelLn(sb, "userProfile", userProfile, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "pageStorageMap", pageStorageMap, indent + 1);
        return sb.toString();
    }

    public void dumpSizeEstimates(StringBuilder sb, int indent) {
        DebugUtil.dumpObjectSizeEstimate(sb, "SessionStorage", this, indent);
        if (userProfile != null) {
            sb.append("\n");
            DebugUtil.dumpObjectSizeEstimate(sb, "userProfile", userProfile, indent + 1);
        }
        sb.append("\n");
        DebugUtil.dumpObjectSizeEstimate(sb, "pageStorageMap", (Serializable) pageStorageMap, indent + 1);
        for (Entry<String, PageStorage> entry : pageStorageMap.entrySet()) {
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

    public GenericPageStorage getSimulation() {
        return getPageStorage(KEY_SIMULATION, new GenericPageStorage());
    }

    public void clearPageStorage() {
        this.pageStorageMap.clear();
    }
}
