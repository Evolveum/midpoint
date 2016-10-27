package com.evolveum.midpoint.web.page.login;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.common.policy.StringPolicyUtils;
import com.evolveum.midpoint.common.policy.ValuePolicyGenerator;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

//"http://localhost:8080/midpoint/confirm/registrationid=" + newUser.getOid()
//+ "/token=" + userType.getCostCenter() + "/roleId=00000000-0000-0000-0000-000000000008";
@PageDescriptor(url = "/registration")
public class PageSelfRegistration extends PageRegistrationBase {

	private static final Trace LOGGER = TraceManager.getTrace(PageSelfRegistration.class);

	private static final String DOT_CLASS = PageSelfRegistration.class.getName() + ".";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_FIRST_NAME = "firstName";
	private static final String ID_LAST_NAME = "lastName";
	private static final String ID_EMAIL = "email";
	private static final String ID_ORGANIZATION = "organization";
	private static final String ID_PASSWORD = "password";
	private static final String ID_SUBMIT_REGISTRATION = "submitRegistration";
	private static final String ID_REGISTRATION_SUBMITED = "registrationInfo";
	private static final String ID_IMAGE = "image";
	private static final String ID_CHANGE_LINK = "changeLink";
	private static final String ID_USER_TEXT = "text";

	private static final String OPERATION_SAVE_USER = DOT_CLASS + "saveUser";
	private static final String OPERATION_LOAD_ORGANIZATIONS = DOT_CLASS + "loadOrganization";

	private static final long serialVersionUID = 1L;

	private IModel<UserType> userModel;

	private boolean submited = false;
	String randomString = null;
	String captchaString = null;

	public PageSelfRegistration() {
		this(null);
	}

	public PageSelfRegistration(PageParameters pageParameters) {
		super();

		userModel = new LoadableModel<UserType>(true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected UserType load() {
				return createUser();
			}
		};
		
		initLayout();

	}

	private UserType createUser() {
		PrismObjectDefinition<UserType> userDef = getPrismContext().getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(UserType.class);
		PrismObject<UserType> user;
		try {
			user = userDef.instantiate();
		} catch (SchemaException e) {
			UserType userType = new UserType();
			user = userType.asPrismObject();

		}

		return user.asObjectable();
	}

	private void initLayout() {
		Form<?> mainForm = new Form<>(ID_MAIN_FORM);
		mainForm.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !submited;
			}

			@Override
			public boolean isEnabled() {
				return !submited;
			}
		});
		add(mainForm);

		TextPanel<String> firstName = new TextPanel<>(ID_FIRST_NAME,
				new PropertyModel<String>(userModel, UserType.F_GIVEN_NAME.getLocalPart() + ".orig") {

					private static final long serialVersionUID = 1L;

					@Override
					public void setObject(String object) {
						userModel.getObject().setGivenName(new PolyStringType(object));
					}
				});
		firstName.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		mainForm.add(firstName);

		TextPanel<String> lastName = new TextPanel<>(ID_LAST_NAME,
				new PropertyModel<String>(userModel, UserType.F_FAMILY_NAME.getLocalPart() + ".orig") {

					private static final long serialVersionUID = 1L;

					@Override
					public void setObject(String object) {
						userModel.getObject().setFamilyName(new PolyStringType(object));
					}

				});
		lastName.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		mainForm.add(lastName);

		TextPanel<String> email = new TextPanel<>(ID_EMAIL,
				new PropertyModel<String>(userModel, UserType.F_EMAIL_ADDRESS.getLocalPart()));
		email.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		mainForm.add(email);

		AutoCompleteTextPanel<String> organization = new AutoCompleteTextPanel<String>(ID_ORGANIZATION,
				Model.of(""), String.class) {
			private static final long serialVersionUID = 1L;

			@Override
			public Iterator<String> getIterator(String input) {
				return prepareAutocompleteValues(input).iterator();
			}

		};
		mainForm.add(organization);

		PasswordPanel password = new PasswordPanel(ID_PASSWORD,
				new PropertyModel<ProtectedStringType>(userModel,
						UserType.F_CREDENTIALS.getLocalPart() + "."
								+ CredentialsType.F_PASSWORD.getLocalPart() + "."
								+ PasswordType.F_VALUE.getLocalPart()));
		password.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		mainForm.add(password);

		final CaptchaImageResource captcha = new CaptchaImageResource(new AbstractReadOnlyModel<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				randomString = generateCaptcha();
				return randomString;
			}

		});

		final Image captchaImage = new Image(ID_IMAGE, captcha);
		captchaImage.setOutputMarkupId(true);
		mainForm.add(captchaImage);

		AjaxLink<Void> changeCaptchaLink = new AjaxLink<Void>(ID_CHANGE_LINK) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				updateCaptcha(captcha, target);
			}
		};
		mainForm.add(changeCaptchaLink);

		RequiredTextField<String> userText = new RequiredTextField<String>(ID_USER_TEXT,
				new PropertyModel<String>(PageSelfRegistration.this, "captchaString"), String.class) {

			private static final long serialVersionUID = 1L;

			@Override
			protected final void onComponentTag(final ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("value", "");
			}

		};
		// userText.add(new EmptyOnBlurAjaxFormUpdatingBehaviour(){
		//
		// @Override
		// protected void onUpdate(AjaxRequestTarget target) {
		// PageSelfRegistration.this.captchaString = (String)
		// getDefaultModelObject();
		// }
		// });
		userText.setOutputMarkupId(true);

		mainForm.add(userText);

		AjaxSubmitButton register = new AjaxSubmitButton(ID_SUBMIT_REGISTRATION) {

			private static final long serialVersionUID = 1L;

			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {

				submitRegistration(target, captcha);

			}

		};

		mainForm.add(register);

		MultiLineLabel label = new MultiLineLabel(ID_REGISTRATION_SUBMITED,
				createStringResource("PageSelfRegistration.registration.confirm.message"));
		add(label);
		label.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return submited;
			}

			@Override
			public boolean isEnabled() {
				return submited;
			}

		});

	}

	private void submitRegistration(AjaxRequestTarget target, CaptchaImageResource captcha) {

		OperationResult result = runPrivileged(new Producer<OperationResult>() {

			@Override
			public OperationResult run() {

				Task task = createAnonymousTask(OPERATION_SAVE_USER);
				task.setChannel(SchemaConstants.CHANNEL_GUI_SELF_REGISTRATION_URI);
				OperationResult result = new OperationResult(OPERATION_SAVE_USER);
				saveUser(task, result);
				result.computeStatus();
				return result;
			}

		});

		if (result.getStatus() == OperationResultStatus.SUCCESS) {
			submited = true;
			getSession()
					.success(createStringResource("PageSelfRegistration.registration.success").getString());
			
//			PageParameters params = getPageParameters();
//
//			if (params != null) {
//				ConnectionEnvironment connEnv = new ConnectionEnvironment();
//				connEnv.setChannel(SchemaConstants.CHANNEL_GUI_SELF_REGISTRATION_URI);
//				UserType userType = userModel.getObject();
//				 ProtectedStringType protectedString = userType.getCredentials().getNonce().getValue();
//				 String nonce = null;
//				 if (protectedString.getClearValue() == null) {
//					 getPrismContext().getDefaultProtector().
//				 }
//				getAuthenticationEvaluator().authenticateUserNonce(connEnv, userType.getName().getOrig(),, noncePolicy)
//				List<NamedPair> values = params.getAllNamed();
//				List<ContainerDelta<AssignmentType>> rolesToAssign = new ArrayList<>();
//				Task task = createSimpleTask("Request roles");
//				for (NamedPair namedPair : values) {
//					PrismObject<AbstractRoleType> abstractRole = WebModelServiceUtils.loadObject(AbstractRoleType.class, namedPair.getValue(), this, task, result);
//					AssignmentType assignment = new AssignmentType();
//					if (abstractRole != null) {
//						assignment.setTarget(abstractRole.asObjectable());
//						try {
//							rolesToAssign.add(ContainerDelta.createModificationAdd(UserType.F_ASSIGNMENT, UserType.class, getPrismContext(), assignment));
//						} catch (SchemaException e) {
//							//just ignore this assignment (for now)
//						}
//					}
//				}
//			}
//			
//			

			switch (getSelfRegistrationConfiguration().getAuthenticationMethod()) {
				case MAIL:
					target.add(PageSelfRegistration.this);
					break;
				case SMS:
					throw new UnsupportedOperationException();
				case NONE:
					setResponsePage(PageLogin.class);
			}

		} else {
			getSession().error(
					createStringResource("PageSelfRegistration.registration.error", result.getMessage())
							.getString());
			throw new RestartResponseException(PageSelfRegistration.class);

		}

		updateCaptcha(captcha, target);
		target.add(getFeedbackPanel());

	}

	private String generateCaptcha() {
		OperationResult result = new OperationResult("generateRandomString");

		StringPolicyType sp = StringPolicyUtils.normalize(new StringPolicyType());
		LimitationsType limits = new LimitationsType();
		limits.setMinLength(8);
		limits.setMaxLength(12);
		limits.setMinUniqueChars(6);
		sp.setLimitations(limits);
		return ValuePolicyGenerator.generate(sp, 8, result);

	}

	private List<String> prepareAutocompleteValues(final String input) {

		return runPrivileged(new Producer<List<String>>() {
			@Override
			public List<String> run() {
				Collection<PrismObject<OrgType>> objects;
				int maxValues = 15;
				List<String> availableNames = new ArrayList<>();
				try {
					ObjectQuery query = QueryBuilder.queryFor(OrgType.class, getPrismContext())
							.item(OrgType.F_DISPLAY_NAME).startsWith(new PolyString(input)).build();
					query.setPaging(ObjectPaging.createPaging(0, maxValues));
					Task task = createAnonymousTask(OPERATION_LOAD_ORGANIZATIONS);
					OperationResult result = new OperationResult(OPERATION_LOAD_ORGANIZATIONS);
					objects = getModelService().searchObjects(OrgType.class, query, null, task, result);

					for (PrismObject<OrgType> o : objects) {
						String objectName = WebComponentUtil.getDisplayNameOrName(o);
						availableNames.add(objectName);
					}

				} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
						| CommunicationException | ConfigurationException e) {
					error("Failed to prepare autocomplete field");
				}
				return availableNames;
			}

		});

	}

	private void updateCaptcha(CaptchaImageResource captcha, AjaxRequestTarget target) {

		captcha.invalidate();
		Image captchaImage = (Image) get(createComponentPath(ID_MAIN_FORM, ID_IMAGE));
		target.add(captchaImage);
	}

	private void saveUser(Task task, OperationResult result) {
		UserType userType = prepareUserToSave(task,
				result);
		ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(userType.asPrismObject());
		userDelta.setPrismContext(getPrismContext());

		WebModelServiceUtils.save(userDelta, result, task, PageSelfRegistration.this);
		result.computeStatus();

	}

	private UserType prepareUserToSave(Task task,
			OperationResult result) {
		String organization = getOrganization();
		UserType userType = userModel.getObject();
		if (organization != null) {
			userType.getOrganization().add(new PolyStringType(organization));
		}

		ProtectedStringType nonceCredentials = new ProtectedStringType();
		nonceCredentials.setClearValue(generateNonce(getSelfRegistrationConfiguration().getNoncePolicy(), task, result));

		NonceType nonceType = new NonceType();
		nonceType.setValue(nonceCredentials);
		
		PageParameters pageParameters = getPageParameters();
		if (pageParameters != null){
			List<NamedPair> namedParameters = pageParameters.getAllNamed();
			if (namedParameters != null && !namedParameters.isEmpty()) {
				NamedPair firstParam = namedParameters.iterator().next();
				if (firstParam != null) {
					nonceType.setResetType(firstParam.getValue());
				}
			}
		}

		userType.getCredentials().setNonce(nonceType);
		
		for (ObjectReferenceType defaultRole : getSelfRegistrationConfiguration().getDefaultRoles()) {
			AssignmentType assignment = new AssignmentType();
			assignment.setTargetRef(defaultRole);
			try {
				getPrismContext().adopt(assignment);
				userType.getAssignment().add(assignment);
			} catch (SchemaException e) {
				//nothing to do
			}
			
		}

		try {
			getPrismContext().adopt(userType);
		} catch (SchemaException e) {
			// nothing to do, try without it
		}

		return userType;

	}

	private String generateNonce(NonceCredentialsPolicyType noncePolicy, Task task, OperationResult result) {
		ValuePolicyType policy = null;
		if (noncePolicy.getValuePolicyRef() != null) {
			PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.loadObject(ValuePolicyType.class,
					noncePolicy.getValuePolicyRef().getOid(), PageSelfRegistration.this, task, result);
			policy = valuePolicy.asObjectable();
		}

		return ValuePolicyGenerator.generate(policy != null ? policy.getStringPolicy() : null, 24, result);
	}

	private String getOrganization() {
		AutoCompleteTextPanel<String> org = (AutoCompleteTextPanel<String>) get(
				createComponentPath(ID_MAIN_FORM, ID_ORGANIZATION));
		return org.getBaseFormComponent().getModel().getObject();
	}

}
