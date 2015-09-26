package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.home.PageAdminHome;

/**
 * @author Viliam Repan (lazyman)
 */
@PageDescriptor(url = {"/self/dashboard", "/self"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_DASHBOARD_URL,
                label = "PageSelfDashboard.auth.dashboard.label",
                description = "PageSelfDashboard.auth.dashboard.description")})
public class PageSelfDashboard extends PageSelf {
}
