package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/self/assignmentsConflicts", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_ASSIGNMENTS_CONFLICTS_URL,
                label = "PageAssignmentShoppingKart.auth.assignmentsConflicts.label",
                description = "PageAssignmentShoppingKart.auth.assignmentsConflicts.description")})
public class PageAssignmentConflicts extends PageSelf{
    private static final String ID_CONFLICTS_PANEL = "conflictsPanel";
    public PageAssignmentConflicts(){}
}
