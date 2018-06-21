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
                @Url(mountUrl = "/admin/casesAll", matchUrlForSecurity = "/admin/casesAll")
        }, action = {
        @AuthorizationAction(actionUri = PageAdminCases.AUTH_CASES_ALL_LABEL,
                label = PageAdminCases.AUTH_CASES_ALL_LABEL,
                description = PageAdminCases.AUTH_CASES_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                label = "PageCases.auth.casesAll.label",
                description = "PageCases.auth.casesAll.description")
})
public class PageCasesAll extends PageCases {

    public PageCasesAll() {
        super(true);
    }
}
