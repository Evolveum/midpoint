package com.evolveum.midpoint.web.page.login;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.MidPointPageParametersEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import net.sf.jasperreports.components.map.ItemData;

//CONFIRMATION_LINK = "http://localhost:8080/midpoint/confirm/registration/";
@PageDescriptor(url = "/confirm", encoder = MidPointPageParametersEncoder.class)
public class PageRegistrationConfirmation extends PageRegistrationBase {

	private static final String DOT_CLASS = PageRegistrationConfirmation.class.getName() + ".";

	private static final String ID_LABEL_SUCCESS = "successLabel";
	private static final String ID_LABEL_ERROR = "errorLabel";
	private static final String ID_LINK_LOGIN = "linkToLogin";
	private static final String ID_SUCCESS_PANEL = "successPanel";
	private static final String ID_ERROR_PANEL = "errorPanel";

	private static final String OPERATION_FINISH_REGISTRATION = DOT_CLASS + "finishRegistration";

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

		StringValue userNameValue = params.get(SchemaConstants.REGISTRATION_ID);
		Validate.notEmpty(userNameValue.toString());
		StringValue tokenValue = params.get(SchemaConstants.REGISTRATION_TOKEN);
		Validate.notEmpty(tokenValue.toString());
		ConnectionEnvironment connEnv = new ConnectionEnvironment();
		connEnv.setChannel(SchemaConstants.CHANNEL_GUI_SELF_REGISTRATION_URI);

		OperationResult result = new OperationResult(OPERATION_FINISH_REGISTRATION);
		UsernamePasswordAuthenticationToken token = null;
		try {
			token = getAuthenticationEvaluator().authenticateUserNonce(connEnv, userNameValue.toString(),
					tokenValue.toString(), getSelfRegistrationConfiguration().getNoncePolicy());
		} catch (AuthenticationException ex) {
			getSession()
					.error(createStringResource("PageRegistrationConfirmation.bad.credentials").getString());
			result.recordFatalError("Failed to validate user");
			 initLayout(result);
			 return;
		}
		
		final MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();
		final NonceType nonceClone = principal.getUser().getCredentials().getNonce().clone();
		
		result = runPrivileged(new Producer<OperationResult>() {
			
			@Override
			public OperationResult run() {
				OperationResult result = new OperationResult("assignDefaultRoles");
				Task task = createAnonymousTask("assignDefaultRoles");
				
				ObjectDelta<UserType> userAssignmentsDelta;
				try {
					userAssignmentsDelta = ObjectDelta.createModificationDeleteContainer(UserType.class, principal.getOid(), new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_NONCE),  getPrismContext(), nonceClone);
				} catch (SchemaException e) {
					result.recordFatalError("Could not create delta");
					return result;
				}
				WebModelServiceUtils.save(userAssignmentsDelta, result, task, PageRegistrationConfirmation.this);
				result.computeStatusIfUnknown();
				return result;
			}
		});
		
//		if (result.getS)
		
//		final String oid = principal.getOid();
//		result = runPrivileged(new Producer<OperationResult>() {
//			
//			@Override
//			public OperationResult run() {
//				OperationResult result = new OperationResult("assignDefaultRoles");
//				Task task = createAnonymousTask("assignDefaultRoles");
//				List<ContainerDelta<AssignmentType>> assignmentDelta = new ArrayList<ContainerDelta<AssignmentType>>();
//				for (ObjectReferenceType defaultRole : getSelfRegistrationConfiguration().getDefaultRoles()) {
//					AssignmentType assignment = new AssignmentType();
//					assignment.setTargetRef(defaultRole);
//					try {
//						assignmentDelta.add(ContainerDelta.createModificationAdd(UserType.F_ASSIGNMENT, UserType.class, getPrismContext(), assignment));
//						getPrismContext().adopt(assignment);
//					} catch (SchemaException e) {
//						//nothing to do
//					}
//					
//				}
//				ObjectDelta<UserType> userAssignmentsDelta = ObjectDelta.createModifyDelta(oid, assignmentDelta, UserType.class, getPrismContext());
//				WebModelServiceUtils.save(userAssignmentsDelta, result, task, PageRegistrationConfirmation.this);
//				result.computeStatusIfUnknown();
//				return result;
//			}
//		});
		
//		token = getAuthenticationEvaluator().authenticateUserNonce(connEnv, userNameValue.toString(),
//				tokenValue.toString(), getSelfRegistrationConfiguration().getNoncePolicy());
//		principal = (MidPointPrincipal) token.getPrincipal();
//		
		List<ItemDelta> userDeltas = new ArrayList<>();
//		userDeltas.add(PropertyDelta.createModificationReplaceProperty(
//				SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
//				principal.getUser().asPrismObject().getDefinition(), (ActivationStatusType) null));

		
		SecurityContextHolder.getContext().setAuthentication(token);
		
		if (nonceClone.getResetType() != null) {

			Task task = createSimpleTask(OPERATION_FINISH_REGISTRATION);

			ObjectDelta<UserType> assignRoleDelta = null;

			try {
				AssignmentType assignment = new AssignmentType();
				assignment.setTargetRef(
						ObjectTypeUtil.createObjectRef(nonceClone.getResetType(), ObjectTypes.ABSTRACT_ROLE));
				getPrismContext().adopt(assignment);
				userDeltas.add((ItemDelta) ContainerDelta.createModificationAdd(UserType.F_ASSIGNMENT,
						UserType.class, getPrismContext(), assignment));

				assignRoleDelta = ObjectDelta.createModifyDelta(principal.getOid(), userDeltas,
						UserType.class, getPrismContext());
				assignRoleDelta.setPrismContext(getPrismContext());
			} catch (SchemaException e) {
				result.recordFatalError("Could not create delta");
				initLayout(result);
				return;

			}

			WebModelServiceUtils.save(assignRoleDelta, result, task, PageRegistrationConfirmation.this);
			result.computeStatusIfUnknown();

		}
		SecurityContextHolder.getContext().setAuthentication(null);
		
		
		
		
		initLayout(result);
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

}
