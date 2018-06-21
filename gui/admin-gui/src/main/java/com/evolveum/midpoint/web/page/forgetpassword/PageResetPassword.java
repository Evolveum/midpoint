package com.evolveum.midpoint.web.page.forgetpassword;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.page.self.PageAbstractSelfCredentials;
import com.evolveum.midpoint.web.page.self.PageSelf;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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
		// TODO Auto-generated constructor stub
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

			PrismObject<UserType> user = getUser();
			if (user == null) {
				SecurityContextHolder.getContext().setAuthentication(null);
				return;
			}

			UserType userType = user.asObjectable();

			if (userType.getCredentials() != null && userType.getCredentials().getNonce() != null) {

				try {
					ObjectDelta<UserType> deleteNonceDelta = ObjectDelta.createModificationDeleteContainer(UserType.class, userType.getOid(), SchemaConstants.PATH_NONCE, getPrismContext(), userType.getCredentials().getNonce().clone());
					WebModelServiceUtils.save(deleteNonceDelta, result, this);
				} catch (SchemaException e) {
					//nothing to do, just let the nonce here.. it will be invalid
				}
			}

			SecurityContextHolder.getContext().setAuthentication(null);
		}

		showResult(result);
		target.add(getFeedbackPanel());
//		get(ID_MAIN_FORM).setVisible(false);


	}

	@Override
	protected void createBreadcrumb() {
		// we don't want breadcrumbs here
	}


}
