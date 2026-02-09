/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.gui.impl.session.ContainerTabStorage;
import com.evolveum.midpoint.gui.impl.session.WorkItemsStorage;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.page.admin.roles.SearchBoxConfigurationHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowserTabSessionStorage implements Serializable, DebugDumpable {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * Store session information for user preferences about paging size in midPoint GUI
     */
    private UserProfileStorage userProfile;
    private SuggestionsStorage suggestions; //todo separate for each browser tab/window?

    /**
     * place to store information in session for various pages
     */
    private final Map<String, PageStorage> pageStorageMap = new HashMap<>();
    private final Map<String, ObjectDetailsStorage> detailsStorageMap = new HashMap<>();

    /**
     * Contains state for the first level menu items.
     * Key is menu label text, value if true then menu is expanded, if false menu is minimized.
     */
    private final Map<String, Boolean> mainMenuState = new HashMap<>();

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

    public PageStorage getOrCreatePageStorage(@NotNull String key) {
        PageStorage ps =  pageStorageMap.get(key);
        if (ps == null) {
            ps = initPageStorage(key);
        }

        return ps;
    }

    public GenericPageStorage getConfiguration() {
        return getPageStorage(SessionStorage.KEY_CONFIGURATION, new GenericPageStorage());
    }

    public OrgStructurePanelStorage getOrgStructurePanelStorage() {
        return getPageStorage(SessionStorage.KEY_ORG_STRUCTURE_PANEL_STORAGE, new OrgStructurePanelStorage());
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
        return getPageStorage(SessionStorage.KEY_AUDIT_LOG, new AuditLogStorage());
    }

    public AuditLogStorage getObjectHistoryAuditLog(QName objectType) {
        String key = objectType.getLocalPart() + "." + SessionStorage.KEY_OBJECT_HISTORY_AUDIT_LOG;
        return getPageStorage(key, new AuditLogStorage());
    }

    public void setObjectHistoryAuditLog(QName objectType, AuditLogStorage storage) {
        setPageStorage(objectType.getLocalPart() + "." + SessionStorage.KEY_OBJECT_HISTORY_AUDIT_LOG, storage);
    }

    @Deprecated
    public ResourceContentStorage getResourceContentStorage(ShadowKindType kind, String searchMode) {
        String key = getContentStorageKey(kind, searchMode);
        return getPageStorage(key, new ResourceContentStorage(kind));

    }

    public ResourceContentStorage getResourceContentStorage(ShadowKindType kind) {
        String key = getContentStorageKey(kind, SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        return getPageStorage(key, new ResourceContentStorage(kind));

    }

    private ContainerTabStorage getContainerTabStorage(String key) {
        return getPageStorage(key, new ContainerTabStorage());
    }

    public ContainerTabStorage getNotificationConfigurationTabMailServerTableStorage() {
        return getContainerTabStorage(SessionStorage.KEY_NOTIFICATION_TAB_MAIL_SERVER_TABLE);
    }

    private String getContentStorageKey(ShadowKindType kind, String searchMode) {
        if (kind == null) {
            return SessionStorage.KEY_RESOURCE_OBJECT_CLASS_CONTENT;
        }

        switch (kind) {
            case ACCOUNT:
                return SessionStorage.KEY_RESOURCE_ACCOUNT_CONTENT + searchMode;

            case ENTITLEMENT:
                return SessionStorage.KEY_RESOURCE_ENTITLEMENT_CONTENT + searchMode;

            case GENERIC:
                return SessionStorage.KEY_RESOURCE_GENERIC_CONTENT + searchMode;
            default:
                return SessionStorage.KEY_RESOURCE_OBJECT_CLASS_CONTENT;

        }
    }

    public WorkItemsStorage getWorkItemStorage() {
        return getPageStorage(SessionStorage.KEY_WORK_ITEMS, new WorkItemsStorage());
    }

    public CertCampaignsStorage getCertCampaigns() {
        return getPageStorage(SessionStorage.KEY_CERT_CAMPAIGNS, new CertCampaignsStorage());
    }

    public CertDecisionsStorage getCertDecisions() {
        return getPageStorage(SessionStorage.KEY_CERT_DECISIONS, new CertDecisionsStorage());
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
        if (key.startsWith(SessionStorage.KEY_OBJECT_LIST)) {
            pageStorage = new ObjectListStorage();
        } else if (key.startsWith(SessionStorage.KEY_ORG_MEMBER_PANEL)
                || key.startsWith(SessionStorage.KEY_ROLE_MEMBER_PANEL)
                || key.startsWith(SessionStorage.KEY_SERVICE_MEMBER_PANEL)
                || key.startsWith(SessionStorage.KEY_POLICY_MEMBER_PANEL)
                || key.startsWith(SessionStorage.KEY_ARCHETYPE_MEMBER_PANEL)
                || key.startsWith(SessionStorage.KEY_GOVERNANCE_CARDS_PANEL)) {
            pageStorage = new MemberPanelStorage();
        } else if (SessionStorage.KEY_ASSIGNMENTS_TAB.equals(key)
                || SessionStorage.KEY_INDUCEMENTS_TAB.equals(key)
                || SessionStorage.KEY_CASE_EVENTS_TAB.equals(key)
                || SessionStorage.KEY_TRIGGERS_TAB.equals(key)
                || SessionStorage.KEY_INDUCED_ENTITLEMENTS_TAB.equals(key)
                || SessionStorage.KEY_CASE_WORKITEMS_TAB.equals(key)
                || SessionStorage.KEY_OBJECT_POLICIES_TAB.equals(key)
                || SessionStorage.KEY_GLOBAL_POLICY_RULES_TAB.equals(key)
                || SessionStorage.KEY_LOGGING_TAB_APPENDER_TABLE.equals(key)
                || SessionStorage.KEY_LOGGING_TAB_LOGGER_TABLE.equals(key)
                || SessionStorage.KEY_FOCUS_PROJECTION_TABLE.equals(key)) {
            pageStorage = getContainerTabStorage(key);
        } else if (SessionStorage.KEY_AUDIT_LOG.equals(key)
                || key.startsWith(SessionStorage.KEY_OBJECT_HISTORY_AUDIT_LOG)) {
            pageStorage = new AuditLogStorage();
        } else if (key.startsWith(SessionStorage.KEY_TASK_ERRORS_PANEL)) {
            pageStorage = new GenericPageStorage();
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

    public SuggestionsStorage getSuggestions() {
        if (suggestions == null) {
            suggestions = new SuggestionsStorage();
        }
        return suggestions;
    }

    public void setSuggestions(SuggestionsStorage suggestions) {
        this.suggestions = suggestions;
    }

    public void clearResourceContentStorage() {
        pageStorageMap.remove(SessionStorage.KEY_RESOURCE_ACCOUNT_CONTENT + SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        pageStorageMap.remove(SessionStorage.KEY_RESOURCE_ACCOUNT_CONTENT + SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT);
        pageStorageMap.remove(SessionStorage.KEY_RESOURCE_ENTITLEMENT_CONTENT + SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        pageStorageMap.remove(SessionStorage.KEY_RESOURCE_ENTITLEMENT_CONTENT + SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT);
        pageStorageMap.remove(SessionStorage.KEY_RESOURCE_GENERIC_CONTENT + SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT);
        pageStorageMap.remove(SessionStorage.KEY_RESOURCE_GENERIC_CONTENT + SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT);
        pageStorageMap.remove(SessionStorage.KEY_RESOURCE_OBJECT_CLASS_CONTENT);
    }

    public void clearTaskErrorsStorage(String taskOidToExclude) {
        List<String> keysToRemove = new ArrayList<>();
        pageStorageMap.keySet()
                .forEach(key -> {
                    if (key.startsWith(SessionStorage.KEY_TASK_ERRORS_PANEL) && !key.contains(taskOidToExclude)) {
                        keysToRemove.add(key);
                    }
                });
        keysToRemove.forEach(key -> pageStorageMap.remove(key));
    }

    public GenericPageStorage getSimulation() {
        return getPageStorage(SessionStorage.KEY_SIMULATION, new GenericPageStorage());
    }

    public void clearPageStorage() {
        this.pageStorageMap.clear();
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("BrowserWindowStorage\n");
        //todo
        return sb.toString();
    }
}
