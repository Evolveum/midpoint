package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.forgotpassword.PageResetPassword;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.model.IModel;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/invitation", matchUrlForSecurity = "/invitation")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_INVITATION_URL) })
public class PageInvitation extends PageSelfRegistration {

    private static final long serialVersionUID = 1L;

    public PageInvitation() {
    }

    @Override
    public void initializeModel() {
        userModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected UserType load() {
                PrismObject<? extends FocusType> focus = getPrincipalFocus().asPrismObject();
                if (focus == null) {
                    AuthUtil.clearMidpointAuthentication();
                    return null;
                }
                return (UserType) focus.asObjectable();
            }
        };
    }

    @Override
    protected IModel<String> getTitleModel() {
        return createStringResource("PageInvitation.title");
    }

    @Override
    protected IModel<String> getDescriptionModel() {
        return createStringResource("PageInvitation.description");
    }

    @Override
    protected boolean isNewUserRegistration() {
        return false;
    }
}
