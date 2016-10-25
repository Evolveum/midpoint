package com.evolveum.midpoint.web.page.login;

import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.w3c.tools.codec.Base64Decoder;
import org.w3c.tools.codec.Base64Encoder;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.MidPointPageParametersEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;


//CONFIRMATION_LINK = "http://localhost:8080/midpoint/confirm/registration/";
@PageDescriptor(url = "/confirm", encoder = MidPointPageParametersEncoder.class)
public class PageRegistrationConfirmation extends PageBase {
	
	private static final String DOT_CLASS = PageRegistrationConfirmation.class.getName() + ".";

	private static final String ID_LABEL_SUCCESS = "successLabel";
	private static final String ID_LABEL_ERROR = "errorLabel";
	private static final String ID_LINK_LOGIN = "linkToLogin";
	private static final String ID_SUCCESS_PANEL = "successPanel";
	private static final String ID_ERROR_PANEL = "errorPanel";
	
	private static final String OPERATION_FINISH_REGISTRATION = DOT_CLASS + "finishRegistration";
	
	private static final long serialVersionUID = 1L;

	IModel<UserType> userModel;
	boolean submited = false;

	public PageRegistrationConfirmation() {
		init(null);
	}
	
	public PageRegistrationConfirmation(PageParameters params) {
		init(params);
	}
	
	private void init(final PageParameters pageParameters){
OperationResult result = runPrivileged(new Producer<OperationResult>() {
			
	
	/*
	 * private String createConfirmationLink(UserType userType){
		StringBuilder confirmLinkBuilder = new StringBuilder(CONFIRMATION_LINK);
			
				StringBuilder suffixBuilder = new StringBuilder("registrationId");
		suffixBuilder.append(userType.getOid()).append("/token/").append(userType.getCostCenter()).append("/roleId/00000000-0000-0000-0000-000000000008");
		String suffix = suffixBuilder.toString();
		Base64Encoder base64Encoder = new Base64Encoder(suffix);
		String encoded = base64Encoder.processString();
		
		String urlSuffix;
		try {
			ProtectedStringType protectedString = prismContext.getDefaultProtector().encryptString(encoded);
			 urlSuffix = new String(protectedString.getEncryptedDataType().getCipherData().getCipherValue());
		} catch (EncryptionException e) {
			urlSuffix = encoded;
		}
		
		confirmLinkBuilder.append(urlSuffix);
		return confirmLinkBuilder.toString();
		
	}(non-Javadoc)
	 * @see 
	 * 
	  com.evolveum.midpoint.util.Producer#run()
	 */
	
	
			@Override
			public OperationResult run() {
				PageParameters params = pageParameters;
				if (params == null) {
					params = getPageParameters();
				}
//				StringValue registrationLink = params.get("registration");
//				Validate.notEmpty(registrationLink.toString());
//				
//				String encoded = registrationLink.toString();
//				Base64Decoder decoder = new Base64Decoder(encoded);
//				String decoded = decoder.processString();
				
				StringValue userOidValue = params.get("registrationId");
				Validate.notEmpty(userOidValue.toString());
				StringValue tokenValue = params.get("token");
				Validate.notEmpty(tokenValue.toString());
				StringValue roleIdValue = params.get("roleId");
				Validate.notEmpty(roleIdValue.toString());
				
				Task task = createAnonymousTask(OPERATION_FINISH_REGISTRATION);
			
				OperationResult result=  new OperationResult(OPERATION_FINISH_REGISTRATION);
				PrismObject<UserType> user = WebModelServiceUtils.loadObject(UserType.class, userOidValue.toString(), PageRegistrationConfirmation.this, task, result);
				
				UserType userType = user.asObjectable();
				if (userType.getCostCenter().equals(tokenValue.toString())) {
					
					AssignmentType endUserRoleAssignment = new AssignmentType();
					endUserRoleAssignment.setTargetRef(ObjectTypeUtil.createObjectRef(roleIdValue.toString(), ObjectTypes.ROLE));
					
					ObjectDelta<UserType> assignRoleDelta;
					try {
						assignRoleDelta = ObjectDelta.createModificationAddContainer(UserType.class, userOidValue.toString(), UserType.F_ASSIGNMENT, getPrismContext(), endUserRoleAssignment);
					} catch (SchemaException e) {
						result.recordFatalError("Could not create delta");
						return result;
					}
					assignRoleDelta.setPrismContext(getPrismContext());
					
					
					
					WebModelServiceUtils.save(assignRoleDelta, result, task, PageRegistrationConfirmation.this);
					result.computeStatus();
					return result;
				} else {
					result.computeStatus();
					result.recordFatalError("some error message");
					return result;
				}
			}
		});
		
		

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
		
		Label successMessage = new Label(ID_LABEL_SUCCESS, createStringResource("PageRegistrationConfirmation.confirmation.successful"));
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
		Label errorMessage = new Label(ID_LABEL_ERROR, createStringResource("PageRegistrationConfirmation.confirmation.error"));
		errorPanel.add(errorMessage);
		
	}

}
