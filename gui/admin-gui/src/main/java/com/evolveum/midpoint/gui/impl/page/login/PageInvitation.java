package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.impl.page.forgotpassword.PageResetPassword;
import com.evolveum.midpoint.security.api.AuthorizationConstants;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/invitation", matchUrlForSecurity = "/invitation")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_INVITATION_URL) })
public class PageInvitation extends PageResetPassword {

    private static final long serialVersionUID = 1L;

    public PageInvitation() {
    }

}
