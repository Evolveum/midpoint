package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.web.page.admin.configuration.*;
import com.evolveum.midpoint.web.page.admin.help.PageAbout;
import com.evolveum.midpoint.web.page.admin.help.PageSystem;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.home.PageMyPasswords;
import com.evolveum.midpoint.web.page.admin.internal.PageAccounts;
import com.evolveum.midpoint.web.page.admin.reports.PageReports;
import com.evolveum.midpoint.web.page.admin.resources.PageResource;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceEdit;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.resources.content.PageAccount;
import com.evolveum.midpoint.web.page.admin.resources.content.PageContentAccounts;
import com.evolveum.midpoint.web.page.admin.resources.content.PageContentEntitlements;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.*;
import com.evolveum.midpoint.web.page.admin.workflow.*;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.util.MidPointPageParametersEncoder;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public enum PageUrlMapping {

    LOGIN("/login", PageLogin.class, MidPointPageParametersEncoder.ENCODER, null),

    ADMIN("/admin", PageDashboard.class, MidPointPageParametersEncoder.ENCODER, null),
    ADMIN_DASHBOARD("/admin/dashboard", PageDashboard.class, MidPointPageParametersEncoder.ENCODER, null),
    ADMIN_MY_PASSWORDS("/admin/myPasswords", PageMyPasswords.class, MidPointPageParametersEncoder.ENCODER, null),

    ADMIN_USER("/admin/user", PageUser.class, MidPointPageParametersEncoder.ENCODER, null),
    ADMIN_USER_PREVIEW("/admin/userPreview", PageUserPreview.class, MidPointPageParametersEncoder.ENCODER, null),
    ADMIN_USERS("/admin/users", PageUsers.class, MidPointPageParametersEncoder.ENCODER, null),
    ADMIN_USERS_BULK("/admin/users/bulk", PageBulkUsers.class, MidPointPageParametersEncoder.ENCODER, null),
    ADMIN_ORG_STRUCT("/admin/orgStruct", PageOrgStruct.class, MidPointPageParametersEncoder.ENCODER, null),

    TASK("/admin/task", PageTaskEdit.class, new OnePageParameterEncoder(PageTaskEdit.PARAM_TASK_EDIT_ID), null),
    TASKS("/admin/tasks", PageTasks.class, MidPointPageParametersEncoder.ENCODER, null),
    ADD_TASK("/admin/addTask", PageTaskAdd.class, MidPointPageParametersEncoder.ENCODER, null),

    ROLE("/admin/role", PageRole.class, new OnePageParameterEncoder(PageRole.PARAM_ROLE_ID), null),
    ROLES("/admin/roles", PageRoles.class, MidPointPageParametersEncoder.ENCODER, null),

    RESOURCE("/admin/resource", PageResource.class, new OnePageParameterEncoder(PageResource.PARAM_RESOURCE_ID), null),
    RESOURCE_EDIT("/admin/resourceEdit", PageResourceEdit.class, new OnePageParameterEncoder(PageResourceEdit.PARAM_RESOURCE_ID), null),
    RESOURCE_WIZARD("/admin/resourceWizard", PageResourceWizard.class, new OnePageParameterEncoder(PageResourceWizard.PARAM_RESOURCE_ID), null),
    RESOURCES("/admin/resources", PageResources.class, MidPointPageParametersEncoder.ENCODER, null),
    RESOURCES_ACCOUNT("/admin/resources/account", PageAccount.class, new OnePageParameterEncoder(PageAccount.PARAM_ACCOUNT_ID), null),
    RESOURCES_CONTENT_ACCOUNTS("/admin/resources/content/accounts", PageContentAccounts.class, new OnePageParameterEncoder(PageContentAccounts.PARAM_RESOURCE_ID), null),
    RESOURCES_CONTENT_ENTITLEMENTS("/admin/resources/content/entitlements", PageContentEntitlements.class, new OnePageParameterEncoder(PageContentEntitlements.PARAM_RESOURCE_ID), null),

    WORK_ITEM("/admin/workItem", PageWorkItem.class, new OnePageParameterEncoder(PageWorkItem.PARAM_TASK_ID), null),
    WORK_ITEMS("/admin/workItems", PageWorkItems.class, MidPointPageParametersEncoder.ENCODER, null),
    WORK_ITEMS_ALL_REQUESTS("/admin/workItems/allRequests", PageProcessInstancesAll.class, MidPointPageParametersEncoder.ENCODER, null),
    WORK_ITEMS_MY_REQUESTS("/admin/workItems/myRequests", PageProcessInstancesRequestedBy.class, MidPointPageParametersEncoder.ENCODER, null),
    WORK_ITEMS_ABOUT_ME_REQUESTS("/admin/workItems/aboutMeRequests", PageProcessInstancesRequestedFor.class, MidPointPageParametersEncoder.ENCODER, null),
    WORK_ITEMS_PROCESS_INSTANCE("/admin/workItems/processInstance", PageProcessInstance.class, new OnePageParameterEncoder(PageProcessInstance.PARAM_PROCESS_INSTANCE_ID), null),

    CONFIG("/admin/config", PageLogging.class, MidPointPageParametersEncoder.ENCODER, null),
    CONFIG_DEBUG("/admin/config/debug", PageDebugView.class, MidPointPageParametersEncoder.ENCODER, null),
    CONFIG_DEBUGS("/admin/config/debugs", PageDebugList.class, MidPointPageParametersEncoder.ENCODER, null),
    CONFIG_IMPORT("/admin/config/import", PageImportObject.class, MidPointPageParametersEncoder.ENCODER, null),
    CONFIG_LOGGING("/admin/config/logging", PageLogging.class, MidPointPageParametersEncoder.ENCODER, null),
    CONFIG_SYSTEM_CONFIGURATION("/admin/config/system", PageSystemConfiguration.class, MidPointPageParametersEncoder.ENCODER, null),

    REPORTS("/admin/reports", PageReports.class, MidPointPageParametersEncoder.ENCODER, null),

    ABOUT_MIDPOINT("/admin/about/midPoint", PageAbout.class, MidPointPageParametersEncoder.ENCODER, null),
    ABOUT_SYSTEM("/admin/about/system", PageSystem.class, MidPointPageParametersEncoder.ENCODER, null),

    INTERNAL_ACCOUNTS("/admin/internal/accounts", PageAccounts.class, MidPointPageParametersEncoder.ENCODER, null),

    CONFIG_TEST("/admin/config/test", PageTest.class, MidPointPageParametersEncoder.ENCODER, null);

    private String url;

    private Class<? extends WebPage> page;

    private IPageParametersEncoder encoder;

    private QName role;

    private PageUrlMapping(String url, Class<? extends WebPage> page,
                           IPageParametersEncoder encoder, QName role) {
        this.encoder = encoder;
        this.page = page;
        this.url = url;
        this.role = role;
    }

    public IPageParametersEncoder getEncoder() {
        return encoder;
    }

    public Class<? extends WebPage> getPage() {
        return page;
    }

    public QName getRole() {
        return role;
    }

    public String getUrl() {
        return url;
    }
}
