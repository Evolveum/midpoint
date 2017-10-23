package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;

/**
 * @author bpowers
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/casesAllocatedToMe", matchUrlForSecurity = "/admin/casesAllocatedToMe")
        }, action = {
        @AuthorizationAction(actionUri = PageAdminCases.AUTH_CASES_ALLOCATED_TO_ME_LABEL,
                label = PageAdminCases.AUTH_CASES_ALLOCATED_TO_ME_LABEL,
                description = PageAdminCases.AUTH_CASES_ALLOCATED_TO_ME_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_ALLOCATED_TO_ME_URL,
                label = "PageCases.auth.casesAllocatedToMe.label",
                description = "PageCases.auth.casesAllocatedToMe.description")
})
public class PageCasesAllocatedToMe extends PageCases {

    public PageCasesAllocatedToMe() {
        super(false);
    }
}
