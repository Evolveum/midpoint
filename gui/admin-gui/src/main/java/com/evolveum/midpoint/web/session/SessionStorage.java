/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.session;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

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

    private Map<String, BrowserTabSessionStorage> storageByWindowName = new HashMap<>();

    public enum Mode {

        LIGHT, DARK;
    }

    //todo mode stays the same all across the tabs?
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

    public BrowserTabSessionStorage getBrowserTabSessionStorage(@NotNull String windowName) {
        BrowserTabSessionStorage browserTabSessionStorage = storageByWindowName.get(windowName);
        if (browserTabSessionStorage == null) {
            browserTabSessionStorage = new BrowserTabSessionStorage();
            storageByWindowName.put(windowName, browserTabSessionStorage);
        }
        return browserTabSessionStorage;
    }

    public void clearAllBrowserTabsStorages() {
        storageByWindowName.values()
                .forEach(BrowserTabSessionStorage::clearPageStorage);
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
        //todo fix
//        DebugUtil.debugDumpWithLabelLn(sb, "userProfile", userProfile, indent + 1);
//        DebugUtil.debugDumpWithLabel(sb, "pageStorageMap", pageStorageMap, indent + 1);
        return sb.toString();
    }

    public void dumpSizeEstimates(StringBuilder sb, int indent) {
        //todo fix
//        DebugUtil.dumpObjectSizeEstimate(sb, "SessionStorage", this, indent);
//        if (userProfile != null) {
//            sb.append("\n");
//            DebugUtil.dumpObjectSizeEstimate(sb, "userProfile", userProfile, indent + 1);
//        }
//        sb.append("\n");
//        DebugUtil.dumpObjectSizeEstimate(sb, "pageStorageMap", (Serializable) pageStorageMap, indent + 1);
//        for (Entry<String, PageStorage> entry : pageStorageMap.entrySet()) {
//            sb.append("\n");
//            DebugUtil.dumpObjectSizeEstimate(sb, entry.getKey(), entry.getValue(), indent + 2);
//        }
    }
}
