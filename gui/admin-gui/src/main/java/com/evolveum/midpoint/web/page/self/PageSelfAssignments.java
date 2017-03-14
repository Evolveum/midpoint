package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;

/**
 * @author Viliam Repan (lazyman)
 */
@PageDescriptor(url = {"/self/selfAssignments"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_ASSIGNMENTS_URL,
                label = "PageSelfAssignments.auth.assignments.label",
                description = "PageSelfAssignments.auth.assignments.description")})
public class PageSelfAssignments extends PageSelf {
}
