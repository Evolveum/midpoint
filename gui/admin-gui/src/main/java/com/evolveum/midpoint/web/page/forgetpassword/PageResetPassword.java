package com.evolveum.midpoint.web.page.forgetpassword;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.page.self.PageAbstractSelfCredentials;
import com.evolveum.midpoint.web.page.self.PageSelf;

@PageDescriptor(url = "/resetPassword", action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL,
                label = "PageSelfCredentials.auth.credentials.label",
                description = "PageSelfCredentials.auth.credentials.description")})
public class PageResetPassword extends PageAbstractSelfCredentials{
	

	private static final long serialVersionUID = 1L;

	
	public PageResetPassword() {
		super();
	}
	
	@Override
	protected boolean isSideMenuVisible(boolean visibleIfLoggedIn) {
		return false;
	}
		
	
	@Override
	protected boolean isCheckOldPassword() {
		return false;
	}
	
	@Override
	protected void finishChangePassword(final OperationResult result, AjaxRequestTarget target) {
		
		
		if (result.getStatus() == OperationResultStatus.SUCCESS) {
			result.setMessage(getString("PageResetPassword.reset.successful"));
			setResponsePage(PageLogin.class);
			SecurityContextHolder.getContext().setAuthentication(null);
		}
		
		showResult(result);
		target.add(getFeedbackPanel());
//		get(ID_MAIN_FORM).setVisible(false);
		
		
	}
	
	
	

}
