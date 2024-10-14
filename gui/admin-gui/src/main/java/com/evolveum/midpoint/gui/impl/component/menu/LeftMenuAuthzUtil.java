/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.menu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.gui.impl.page.admin.policy.PagePolicies;
import com.evolveum.midpoint.gui.impl.page.admin.policy.PagePolicy;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.mining.PageRoleSuggestions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.PageOutliers;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.cases.PageCases;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgs;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;

import org.apache.wicket.markup.html.WebPage;

// TODO Temporary, should be removed when MID-5718 is resolved. We need it now
// to distinguish between All users and Users view authorizations
public class LeftMenuAuthzUtil {

    private static Map<Class<? extends WebPage>, List<String>> pageAuthorizationMaps;
    private static Map<Class<? extends WebPage>, List<String>> viewsAuthorizationMaps;

    static {
        pageAuthorizationMaps = new HashMap<>();
        pageAuthorizationMaps.put(PageUsers.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_USERS_URL));
        pageAuthorizationMaps.put(PageOrgs.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_ORG_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_ORGS_URL));
        pageAuthorizationMaps.put(PageRoles.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_ROLES_URL));
        pageAuthorizationMaps.put(PageServices.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_SERVICES_URL));
        pageAuthorizationMaps.put(PagePolicies.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_POLICIES_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_POLICIES_URL));
        pageAuthorizationMaps.put(PageResources.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_RESOURCES_URL));
        pageAuthorizationMaps.put(PageCases.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_CASES_URL));
        pageAuthorizationMaps.put(PageTasks.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_TASKS_URL));
        pageAuthorizationMaps.put(PageOutliers.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_OUTLIERS_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_OUTLIERS_URL));
        pageAuthorizationMaps.put(PageRoleSuggestions.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_ROLE_SUGGESTIONS_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_ROLE_SUGGESTION_URL));
    }

    static {
        viewsAuthorizationMaps = new HashMap<>();
        viewsAuthorizationMaps.put(PageUsers.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_USERS_VIEW_URL));
        viewsAuthorizationMaps.put(PageOrgs.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_ORG_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_ORGS_VIEW_URL));
        viewsAuthorizationMaps.put(PageRoles.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_ROLES_VIEW_URL));
        viewsAuthorizationMaps.put(PageServices.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_SERVICES_VIEW_URL));
        viewsAuthorizationMaps.put(PagePolicy.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_POLICIES_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_POLICIES_VIEW_URL));
        viewsAuthorizationMaps.put(PageResources.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_RESOURCES_VIEW_URL));
        viewsAuthorizationMaps.put(PageCases.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_CASES_VIEW_URL));
        viewsAuthorizationMaps.put(PageTasks.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_TASKS_VIEW_URL));
        viewsAuthorizationMaps.put(PageOutliers.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_OUTLIERS_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_OUTLIERS_VIEW_URL));
        viewsAuthorizationMaps.put(PageRoleSuggestions.class,
                Arrays.asList(AuthorizationConstants.AUTZ_UI_ROLE_SUGGESTIONS_ALL_URL,
                        AuthorizationConstants.AUTZ_UI_ROLE_SUGGESTIONS_VIEW_URL));
    }

    public static List<String> getAuthorizationsForPage(Class<? extends WebPage> pageClass) {
        if (pageClass == null) {
            return null;
        }

        return pageAuthorizationMaps.get(pageClass);
    }

    public static List<String> getAuthorizationsForView(Class<? extends WebPage> pageClass) {
        if (pageClass == null) {
            return null;
        }

        return viewsAuthorizationMaps.get(pageClass);
    }

}
