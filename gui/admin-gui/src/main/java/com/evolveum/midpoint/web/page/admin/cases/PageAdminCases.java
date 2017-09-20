package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

import javax.xml.namespace.QName;

/**
 * Created by acope on 9/14/17.
 */
public abstract class PageAdminCases extends PageAdmin {

    public static final String AUTH_CASES_ALL = AuthorizationConstants.AUTZ_UI_CASES_ALL_URL;
    public static final String AUTH_CASES_ALL_LABEL = "PageAdminCases.auth.approvalsAll.label";
    public static final String AUTH_CASES_ALL_DESCRIPTION = "PageAdminCases.auth.approvalsAll.description";

}
