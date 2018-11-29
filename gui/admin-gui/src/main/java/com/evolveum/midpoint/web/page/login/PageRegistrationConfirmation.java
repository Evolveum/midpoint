package com.evolveum.midpoint.web.page.login;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDeltaImpl;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.springframework.security.core.AuthenticationException;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.context.NonceAuthenticationContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

//CONFIRMATION_LINK = "http://localhost:8080/midpoint/confirm/registration/";
@PageDescriptor(urls = {@Url(mountUrl = SchemaConstants.REGISTRATION_CONFIRAMTION_PREFIX)}, permitAll = true)
public class PageRegistrationConfirmation extends PageRegistrationBase {

	private static final Trace LOGGER = TraceManager.getTrace(PageRegistrationConfirmation.class);

	private static final String DOT_CLASS = PageRegistrationConfirmation.class.getName() + ".";

	private static final String ID_LABEL_SUCCESS = "successLabel";
	private static final String ID_LABEL_ERROR = "errorLabel";
	private static final String ID_LINK_LOGIN = "linkToLogin";
	private static final String ID_SUCCESS_PANEL = "successPanel";
	private static final String ID_ERROR_PANEL = "errorPanel";

	private static final String OPERATION_ASSIGN_DEFAULT_ROLES = DOT_CLASS + "assignDefaultRoles";
	private static final String OPERATION_ASSIGN_ADDITIONAL_ROLE = DOT_CLASS + "assignAdditionalRole";
	private static final String OPERATION_FINISH_REGISTRATION = DOT_CLASS + "finishRegistration";
	private static final String OPERATION_CHECK_CREDENTIALS = DOT_CLASS + "checkCredentials";
	private static final String OPERATION_REMOVE_NONCE_AND_SET_LIFECYCLE_STATE = DOT_CLASS + "removeNonceAndSetLifecycleState";

	private static final long serialVersionUID = 1L;

	public PageRegistrationConfirmation() {
		super();
		init(null);
	}

	public PageRegistrationConfirmation(PageParameters params) {
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

		try {
			UserType user = checkUserCredentials(userNameValue.toString(), tokenValue.toString(), result);
			PrismObject<UserType> administrator = getAdministratorPrivileged(result);

			assignDefaultRoles(user.getOid(), administrator, result);
			result.computeStatus();
			if (result.getStatus() == OperationResultStatus.FATAL_ERROR) {
				LOGGER.error("Failed to assign default roles, {}", result.getMessage());
			} else {
				NonceType nonceClone = user.getCredentials().getNonce().clone();
				removeNonceAndSetLifecycleState(user.getOid(), nonceClone, administrator, result);
				assignAdditionalRoleIfPresent(user.getOid(), nonceClone, administrator, result);
				result.computeStatus();
			}
			initLayout(result);
		} catch (CommonException|AuthenticationException e) {
			result.computeStatus();
			initLayout(result);
		}
	}

	private UserType checkUserCredentials(String username, String nonce, OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(OPERATION_CHECK_CREDENTIALS);
		try {
			ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_GUI_SELF_REGISTRATION_URI);
			return getAuthenticationEvaluator().checkCredentials(connEnv, new NonceAuthenticationContext(username,
					nonce, getSelfRegistrationConfiguration().getNoncePolicy()));
		} catch (AuthenticationException ex) {
			getSession().error(getString(ex.getMessage()));
			result.recordFatalError("Failed to validate user", ex);
			LoggingUtils.logException(LOGGER, ex.getMessage(), ex);
			throw ex;
		} catch (Exception ex) {
			getSession().error(createStringResource("PageRegistrationConfirmation.authnetication.failed").getString());
			result.recordFatalError("Failed to confirm registration", ex);
			LoggingUtils.logException(LOGGER, "Failed to confirm registration", ex);
			throw ex;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	private void assignDefaultRoles(String userOid, PrismObject<UserType> administrator, OperationResult parentResult) throws CommonException {
		List<ObjectReferenceType> rolesToAssign = getSelfRegistrationConfiguration().getDefaultRoles();
		if (CollectionUtils.isEmpty(rolesToAssign)) {
			return;
		}

		OperationResult result = parentResult.createSubresult(OPERATION_ASSIGN_DEFAULT_ROLES);
		try {
			PrismContext prismContext = getPrismContext();
			List<AssignmentType> assignmentsToCreate = rolesToAssign.stream()
					.map(ref -> ObjectTypeUtil.createAssignmentTo(ref, prismContext))
					.collect(Collectors.toList());
			ObjectDelta<Objectable> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
					.item(UserType.F_ASSIGNMENT).addRealValues(assignmentsToCreate)
					.asObjectDelta(userOid);
			runAsChecked(() -> {
				Task task = createSimpleTask(OPERATION_ASSIGN_DEFAULT_ROLES);
				WebModelServiceUtils.save(delta, result, task, PageRegistrationConfirmation.this);
				return null;
			}, administrator);
		} catch (CommonException|RuntimeException e) {
			result.recordFatalError("Couldn't assign default roles", e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	private void removeNonceAndSetLifecycleState(String userOid, NonceType nonce, PrismObject<UserType> administrator,
			OperationResult parentResult) throws CommonException {
		OperationResult result = parentResult.createSubresult(OPERATION_REMOVE_NONCE_AND_SET_LIFECYCLE_STATE);
		try {
			runAsChecked(() -> {
				Task task = createSimpleTask(OPERATION_REMOVE_NONCE_AND_SET_LIFECYCLE_STATE);
				ObjectDelta<UserType> delta = ObjectDelta.createModificationDeleteContainer(UserType.class, userOid,
						ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_NONCE), getPrismContext(), nonce);
				delta.addModificationReplaceProperty(UserType.F_LIFECYCLE_STATE, SchemaConstants.LIFECYCLE_ACTIVE);
				WebModelServiceUtils.save(delta, result, task, PageRegistrationConfirmation.this);
				return null;
			}, administrator);
		} catch (CommonException|RuntimeException e) {
			result.recordFatalError("Couldn't remove nonce and set lifecycle state", e);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove nonce and set lifecycle state", e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	private void assignAdditionalRoleIfPresent(String userOid, NonceType nonceType,
			PrismObject<UserType> administrator, OperationResult parentResult) throws CommonException {
		if (nonceType.getName() == null) {
			return;
		}
		OperationResult result = parentResult.createSubresult(OPERATION_ASSIGN_ADDITIONAL_ROLE);
		try {
			runAsChecked(() -> {
				Task task = createAnonymousTask(OPERATION_ASSIGN_ADDITIONAL_ROLE);
				ObjectDelta<UserType> assignRoleDelta;
				AssignmentType assignment = new AssignmentType();
				assignment.setTargetRef(ObjectTypeUtil.createObjectRef(nonceType.getName(), ObjectTypes.ABSTRACT_ROLE));
				getPrismContext().adopt(assignment);
				List<ItemDelta> userDeltas = new ArrayList<>();
				userDeltas.add(ContainerDeltaImpl.createModificationAdd(UserType.F_ASSIGNMENT,
						UserType.class, getPrismContext(), assignment));
				assignRoleDelta = ObjectDelta.createModifyDelta(userOid, userDeltas, UserType.class, getPrismContext());
				assignRoleDelta.setPrismContext(getPrismContext());
				WebModelServiceUtils.save(assignRoleDelta, result, task, PageRegistrationConfirmation.this);
				return null;
			}, administrator);
		} catch (CommonException|RuntimeException e) {
			result.recordFatalError("Couldn't assign additional role", e);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't assign additional role", e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	private void initLayout(final OperationResult result) {

		WebMarkupContainer successPanel = new WebMarkupContainer(ID_SUCCESS_PANEL);
		add(successPanel);
		successPanel.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return result.getStatus() != OperationResultStatus.FATAL_ERROR;
			}

			@Override
			public boolean isEnabled() {
				return result.getStatus() != OperationResultStatus.FATAL_ERROR;
			}
		});

		Label successMessage = new Label(ID_LABEL_SUCCESS,
				createStringResource("PageRegistrationConfirmation.confirmation.successful"));
		successPanel.add(successMessage);

		AjaxLink<String> continueToLogin = new AjaxLink<String>(ID_LINK_LOGIN) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageLogin.class);
			}
		};
		successPanel.add(continueToLogin);

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
				createStringResource("PageRegistrationConfirmation.confirmation.error"));
		errorPanel.add(errorMessage);

	}

	@Override
	protected void createBreadcrumb() {
		// don't create breadcrumb for registration confirmation page
	}

}
