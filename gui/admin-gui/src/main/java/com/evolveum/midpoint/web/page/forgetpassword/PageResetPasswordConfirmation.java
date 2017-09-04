package com.evolveum.midpoint.web.page.forgetpassword;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.model.api.context.NonceAuthenticationContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.login.PageRegistrationBase;
import com.evolveum.midpoint.web.page.login.PageRegistrationConfirmation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;

@PageDescriptor(urls = {@Url(mountUrl = SchemaConstants.PASSWORD_RESET_CONFIRMATION_PREFIX)})
public class PageResetPasswordConfirmation extends PageRegistrationBase{


private static final Trace LOGGER = TraceManager.getTrace(PageRegistrationConfirmation.class);

	private static final String DOT_CLASS = PageRegistrationConfirmation.class.getName() + ".";


	private static final String ID_LABEL_ERROR = "errorLabel";
	private static final String ID_ERROR_PANEL = "errorPanel";

	private static final String OPERATION_ASSIGN_DEFAULT_ROLES = DOT_CLASS + ".assignDefaultRoles";
	private static final String OPERATION_FINISH_REGISTRATION = DOT_CLASS + "finishRegistration";

	private static final long serialVersionUID = 1L;

	public PageResetPasswordConfirmation() {
		super();
		init(null);
	}

	public PageResetPasswordConfirmation(PageParameters params) {
		super();
		init(params);
	}

	private void init(final PageParameters pageParameters) {

		PageParameters params = pageParameters;
		if (params == null) {
			params = getPageParameters();
		}

		OperationResult result = new OperationResult(OPERATION_FINISH_REGISTRATION);
		if (params == null) {
			LOGGER.error("Confirmation link is not valid. No credentials provided in it");
			String msg = createStringResource("PageSelfRegistration.invalid.registration.link").getString();
			getSession().error(createStringResource(msg));
			result.recordFatalError(msg);
			initLayout(result);
			return;
		}

		StringValue userNameValue = params.get(SchemaConstants.USER_ID);
		Validate.notEmpty(userNameValue.toString());
		StringValue tokenValue = params.get(SchemaConstants.TOKEN);
		Validate.notEmpty(tokenValue.toString());

		UsernamePasswordAuthenticationToken token = authenticateUser(userNameValue.toString(), tokenValue.toString(), result);
		if (token == null) {
			initLayout(result);
			return;
		} else {
//			SecurityContextHolder.getContext().setAuthentication(token);
			MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();
			Collection<Authorization> authz = principal.getAuthorities();

			if (authz != null) {
				Iterator<Authorization> authzIterator = authz.iterator();
				while (authzIterator.hasNext()) {
					Authorization authzI= authzIterator.next();
					Iterator<String> actionIterator = authzI.getAction().iterator();
					while (actionIterator.hasNext()) {
						String action = actionIterator.next();
						if (action.contains(AuthorizationConstants.NS_AUTHORIZATION_UI)) {
							actionIterator.remove();
						}
					}

				}

			}

			AuthorizationType authorizationType = new AuthorizationType();
			authorizationType.getAction().add(AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL);
			Authorization selfServiceCredentialsAuthz = new Authorization(authorizationType);
			authz.add(selfServiceCredentialsAuthz);
			SecurityContextHolder.getContext().setAuthentication(token);
			setResponsePage(PageResetPassword.class);
		}

		initLayout(result);
	}

	private UsernamePasswordAuthenticationToken authenticateUser(String username, String nonce, OperationResult result) {
		ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_GUI_SELF_REGISTRATION_URI);
		try {
			return getAuthenticationEvaluator().authenticate(connEnv, new NonceAuthenticationContext(username,
					nonce, getResetPasswordPolicy().getNoncePolicy()));
		} catch (AuthenticationException ex) {
			getSession()
					.error(getString(ex.getMessage()));
			result.recordFatalError("Failed to validate user");
			LoggingUtils.logException(LOGGER, ex.getMessage(), ex);
			 return null;
		} catch (Exception ex) {
			getSession()
			.error(createStringResource("PageResetPasswordConfirmation.authnetication.failed").getString());
			LoggingUtils.logException(LOGGER, "Failed to confirm registration", ex);
			return null;
		}
	}




	private void initLayout(final OperationResult result) {

		WebMarkupContainer errorPanel = new WebMarkupContainer(ID_ERROR_PANEL);
		add(errorPanel);
		errorPanel.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled() {
				return result.getStatus() == OperationResultStatus.FATAL_ERROR;
			}

			@Override
			public boolean isVisible() {
				return result.getStatus() == OperationResultStatus.FATAL_ERROR;
			}
		});
		Label errorMessage = new Label(ID_LABEL_ERROR,
				createStringResource("PageResetPasswordConfirmation.confirmation.error"));
		errorPanel.add(errorMessage);

	}

	@Override
	protected void createBreadcrumb() {
		// don't create breadcrumb for registration confirmation page
	}
}
