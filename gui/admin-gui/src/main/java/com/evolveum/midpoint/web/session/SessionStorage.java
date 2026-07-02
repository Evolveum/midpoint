/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.session;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import java.util.Map.Entry;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.self.requestAccess.RequestAccess;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

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
    public static final String KEY_TASK_ERRORS_PANEL = UserProfileStorage.TableId.PANEL_TASK_ERRORS.name();

    public static final String KEY_GOVERNANCE_CARDS_PANEL = UserProfileStorage.TableId.PANEL_GOVERNANCE_CARDS.name();
    public static final String KEY_WORK_ITEMS = "workItems";
    public static final String KEY_OBJECT_LIST = "containerListPage";
    public static final String KEY_CASE_WORKITEMS_TAB = "workitemsTab";
    public static final String KEY_CHILD_CASES_TAB = "childCasesTab";
    public static final String KEY_CASE_EVENTS_TAB = "caseEventsTab";
    public static final String KEY_ORG_STRUCTURE_PANEL_STORAGE = "orgStructurePanelStorage";

    public static final String KEY_TASKS = "tasks";
    public static final String KEY_SUBTASKS = "subtasks";
    public static final String KEY_CERT_CAMPAIGNS = "certCampaigns";
    public static final String KEY_CERT_DECISIONS = "certDecisions";

    private final Map<String, BrowserTabSessionStorage> storageByWindowId = new HashMap<>();
    private SuggestionsStorage suggestions;
    private ResourceWizardStorage resourceWizardStorage;
    private final Set<String> exportProcessIdSet = new HashSet<>();

    public enum Mode {

        LIGHT, DARK;
    }

    private Mode mode = Mode.LIGHT;

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

    public BrowserTabSessionStorage getBrowserTabSessionStorage(@NotNull String windowId) {
        BrowserTabSessionStorage browserTabSessionStorage = storageByWindowId.get(windowId);
        if (browserTabSessionStorage == null) {
            browserTabSessionStorage = new BrowserTabSessionStorage();
            storageByWindowId.put(windowId, browserTabSessionStorage);
        }
        return browserTabSessionStorage;
    }

    public void clearAllBrowserTabsStorages() {
        storageByWindowId.values()
                .forEach(BrowserTabSessionStorage::clearPageStorage);
    }

    public SuggestionsStorage getSuggestionsStorage() {
        if (suggestions == null) {
            suggestions = new SuggestionsStorage();
        }
        return suggestions;
    }

    public void setSuggestions(SuggestionsStorage suggestions) {
        this.suggestions = suggestions;
    }

    public ResourceWizardStorage getResourceWizardStorage() {
        if (resourceWizardStorage == null) {
            resourceWizardStorage = new ResourceWizardStorage();
        }
        return resourceWizardStorage;
    }

    public Set<String> getExportProcessIdSet() {
        return exportProcessIdSet;
    }

    public void addExportProcessId(String processId) {
        exportProcessIdSet.add(processId);
    }

    public void removeExportProcessId(String id) {
        exportProcessIdSet.remove(id);
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

        DebugUtil.debugDumpWithLabelLn(sb, "mode", mode, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "requestAccess", requestAccess, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "storageByWindowName", storageByWindowId, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "suggestions", suggestions, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "resourceWizardStorage", resourceWizardStorage, indent + 1);

        return sb.toString();
    }

    public void dumpSizeEstimates(StringBuilder sb, int indent) {
        DebugUtil.dumpObjectSizeEstimate(sb, "SessionStorage", this, indent);
        if (requestAccess != null) {
            sb.append("\n");
            DebugUtil.dumpObjectSizeEstimate(sb, "requestAccess", requestAccess, indent + 1);
        }
        DebugUtil.dumpObjectSizeEstimate(sb, "storageByWindowName", (Serializable) storageByWindowId, indent + 1);
        for (Entry<String, BrowserTabSessionStorage> entry : storageByWindowId.entrySet()) {
            sb.append("\n");
            DebugUtil.dumpObjectSizeEstimate(sb, entry.getKey(), entry.getValue(), indent + 2);
        }
        if (suggestions != null) {
            sb.append("\n");
            DebugUtil.dumpObjectSizeEstimate(sb, "suggestions", suggestions, indent + 1);
        }
        if (resourceWizardStorage != null) {
            sb.append("\n");
            DebugUtil.dumpObjectSizeEstimate(sb, "resourceWizardStorage", resourceWizardStorage, indent + 1);
        }
        sb.append("\n");

    }
}
