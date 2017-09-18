/**
 * Copyright (c) 2012-2017 Biznet, Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.forgetpassword;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.home.component.MyPasswordQuestionsPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordQuestionsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SecurityQuestionAnswerDTO;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.page.self.PageSelfDashboard;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsResetTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailServerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;


@PageDescriptor(url = "/securityquestions")
public class PageSecurityQuestions extends PageBase {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageSecurityQuestions.class);

	private static final String DOT_CLASS = PageSecurityQuestions.class.getName() + ".";
	private static final String OPERATION_LOAD_USER = DOT_CLASS + "loaduser";
	private static final String OPERATION_LOAD_QUESTION_POLICY = DOT_CLASS + "LOAD Question Policy";
	private static final String ID_PASSWORD_QUESTIONS_PANEL = "pwdQuestionsPanel";
	private static final String OPERATION_RESET_PASSWORD = DOT_CLASS + "resetPassword";
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_BACK = "back";
	private static final String ID_SAVE = "send";
	private static final String OPERATION_LOAD_RESET_PASSWORD_POLICY = "LOAD PASSWORD RESET POLICY";
	public static final String SESSION_ATTRIBUTE_POID = "pOid";

	private List<MyPasswordQuestionsPanel> pqPanels;

	private List<SecurityQuestionDefinitionType> policyQuestionList;
	private MyPasswordQuestionsPanel pwPanel;

	int questionNumber;

	private final Model<PrismObject<UserType>> principalModel = new Model<PrismObject<UserType>>();
	private PasswordQuestionsDto dto = new PasswordQuestionsDto();
	private IModel<PasswordQuestionsDto> model;
	PageBase page = (PageBase) getPage();

	public PageSecurityQuestions() {

		model = new LoadableModel<PasswordQuestionsDto>(false) {

			private static final long serialVersionUID = 1L;

			@Override
			protected PasswordQuestionsDto load() {
				return loadPageModel();
			}
		};

		initLayout();

	}

	@Override
	protected void createBreadcrumb() {
		//don't create breadcrumb for this page
	}

	public void initLayout() {

		Form mainForm = new Form(ID_MAIN_FORM);

		pqPanels = new ArrayList<MyPasswordQuestionsPanel>();

		PrismObject<SecurityPolicyType> securityPolicy = getSecurityPolicy();
		LOGGER.trace("Found security policy: {}", securityPolicy);

		if (securityPolicy == null) {
			LOGGER.error("No security policy, cannot process security questions");
			// Just log the error, but do not display it. We are still in unprivileged part of the web
			// we do not want to provide any information to the attacker.
			throw new RestartResponseException(PageError.class);
		}

		questionNumber = securityPolicy.asObjectable().getCredentials() != null &&
		        securityPolicy.asObjectable().getCredentials().getSecurityQuestions() != null ?
		        securityPolicy.asObjectable().getCredentials().getSecurityQuestions().getQuestionNumber() : 0;
		policyQuestionList = securityPolicy.asObjectable().getCredentials() != null &&
		        securityPolicy.asObjectable().getCredentials().getSecurityQuestions() != null ?
		        securityPolicy.asObjectable().getCredentials().getSecurityQuestions().getQuestion() :
		        new ArrayList<SecurityQuestionDefinitionType>();

		List<SecurityQuestionAnswerDTO> userQuestionList = model.getObject().getSecurityAnswers();

		if (userQuestionList == null) {
			getSession().error(getString("pageForgetPassword.message.ContactAdminQuestionsNotSet"));
			SecurityContext securityContext = SecurityContextHolder.getContext();
			securityContext.setAuthentication(null);
			throw new RestartResponseException(PageForgotPassword.class);
		}

		if (questionNumber <= userQuestionList.size()) {

		// Loop for finding the preset questions from the Policy
		// Questions
		for (Iterator iterator = policyQuestionList.iterator(); iterator.hasNext();) {

			SecurityQuestionDefinitionType securityQuestionDefinitionType = (SecurityQuestionDefinitionType) iterator
					.next();

			// user's question List loop to match the questions
			for (int userQuestint = 0; userQuestint < userQuestionList.size(); userQuestint++) {

				// if the question is in the policy check
				int panelNumber = 0;
				if (userQuestionList.get(userQuestint).getPwdQuestion()
						.equalsIgnoreCase(securityQuestionDefinitionType.getIdentifier())) {

					SecurityQuestionAnswerDTO a = new SecurityQuestionAnswerDTO(userQuestionList
							.get(userQuestint).getPwdQuestion(), "", userQuestionList.get(
							userQuestint).getQuestionItself());
					a = checkIfQuestionisValid(a, policyQuestionList);
					MyPasswordQuestionsPanel panel = new MyPasswordQuestionsPanel(
							ID_PASSWORD_QUESTIONS_PANEL + panelNumber, a);
					pqPanels.add(panel);
					panelNumber++;

					// This is the Question!

					}
				}

			}

		}

		add(mainForm);
		mainForm.add(getPanels(pqPanels));

		initButtons(mainForm);

	}

	private PrismObject<SecurityPolicyType> getSecurityPolicy() {

		return runPrivileged(new Producer<PrismObject<SecurityPolicyType>>() {

			@Override
			public PrismObject<SecurityPolicyType> run() {

				Task task = getPageBase().createAnonymousTask(OPERATION_LOAD_QUESTION_POLICY);
				OperationResult result = task.getResult();

				PrismObject<SystemConfigurationType> config;
				try {
					config = getPageBase().getModelService().getObject(SystemConfigurationType.class,
								SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, task, result);
				} catch (ObjectNotFoundException | SchemaException | SecurityViolationException
						| CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
					LOGGER.error("Error getting system configuration: {}", e.getMessage(), e);
					return null;
				}

                if (config.asObjectable().getGlobalSecurityPolicyRef() != null) {
	                    try {
							return getModelService().getObject(SecurityPolicyType.class,
							        config.asObjectable().getGlobalSecurityPolicyRef().getOid(), null, task, result);
						} catch (ObjectNotFoundException | SchemaException | SecurityViolationException
								| CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
							LOGGER.error("Error getting global security policy: {}", e.getMessage(), e);
							return null;
						}
                } else {
                	LOGGER.error("No global security policy reference in system configuration");
                	return null;
                }
			}

		});
	}

	public ListView<MyPasswordQuestionsPanel> getPanels(List<MyPasswordQuestionsPanel> p) {
		ListView lw = new ListView(ID_PASSWORD_QUESTIONS_PANEL, p) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem item) {

				item.add((MyPasswordQuestionsPanel) item.getModelObject());
			}
		};
		return lw;
	}

	public void initButtons(Form mainForm) {
		AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE, createStringResource("PageBase.button.send")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

				savePerformed(target);
			}
		};
		mainForm.add(save);

		AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageBase.button.back")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				cancelPerformed(target);
			}
		};
		mainForm.add(back);
	}

	private void savePerformed(final AjaxRequestTarget target) {

		int correctAnswers = 0;
		for (Iterator iterator = pqPanels.iterator(); iterator.hasNext();) {
			MyPasswordQuestionsPanel type = (MyPasswordQuestionsPanel) iterator.next();

			List<SecurityQuestionAnswerDTO> userQuestionList = model.getObject().getSecurityAnswers();
            if (userQuestionList != null) {
                for (Iterator iterator2 = userQuestionList.iterator(); iterator2.hasNext(); ) {
                    SecurityQuestionAnswerDTO securityQuestionAnswerDTO = (SecurityQuestionAnswerDTO) iterator2
                            .next();
                    // TODO do this in a proper way, what is this.
                    String results = StringEscapeUtils.unescapeHtml((type
                            .get(MyPasswordQuestionsPanel.F_QUESTION)).getDefaultModelObjectAsString());
                    if (getQuestionIdentifierFromQuestion(results).trim().equalsIgnoreCase(
                            securityQuestionAnswerDTO.getPwdQuestion().trim())) {

                        if (((TextField<String>) type.get(MyPasswordQuestionsPanel.F_ANSWER)).getModelObject()
                                .equalsIgnoreCase(securityQuestionAnswerDTO.getPwdAnswer())) {
                            correctAnswers++;
                        }
                    }

                }
            }

		}

		if (questionNumber == correctAnswers) {
			getSession().removeAttribute(SESSION_ATTRIBUTE_POID);

			runPrivileged(new Producer<Object>() {
				@Override
				public Object run() {
					resetPassword(principalModel.getObject().asObjectable(), target);
					return null;
				}
			});

		} else {

			warn(getString("pageSecurityQuestions.message.WrongAnswer"));
			target.add(getFeedbackPanel());
			return;
		}

	}


	private PasswordQuestionsDto loadPageModel() {
		LOGGER.debug("Loading user.");

		final String userOid = getPageParameters().get(SESSION_ATTRIBUTE_POID).toString();

		PrismObject<UserType> user = runPrivileged(new Producer<PrismObject<UserType>>() {
			@Override
			public PrismObject<UserType> run() {
				Task task = createAnonymousTask(OPERATION_LOAD_USER);
				OperationResult subResult = task.getResult();
				try {
					Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
					return getModelService().getObject(UserType.class, userOid, options, task,
							subResult);
				} catch (ObjectNotFoundException | SchemaException | SecurityViolationException
						| CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
					LOGGER.error("Error getting user {}: {}", userOid, e.getMessage(), e);
					// Just log the error, but do not display it. We are still in unprivileged part of the web
					// we do not want to provide any information to the attacker.
					return null;
				}

			}
		});

		principalModel.setObject(user);

		PasswordQuestionsDto dto = new PasswordQuestionsDto();
		dto.setSecurityAnswers(createUsersSecurityQuestionsList(user));

		return dto;
	}


	public List<SecurityQuestionAnswerDTO> createUsersSecurityQuestionsList(PrismObject<UserType> user) {

		SecurityQuestionsCredentialsType credentialsPolicyType = user.asObjectable().getCredentials()
				.getSecurityQuestions();
		if (credentialsPolicyType == null) {
			return null;
		}
		List<SecurityQuestionAnswerType> secQuestAnsList = credentialsPolicyType.getQuestionAnswer();

		if (secQuestAnsList != null) {
			List<SecurityQuestionAnswerDTO> secQuestAnswListDTO = new ArrayList<SecurityQuestionAnswerDTO>();
			for (Iterator iterator = secQuestAnsList.iterator(); iterator.hasNext();) {
				SecurityQuestionAnswerType securityQuestionAnswerType = (SecurityQuestionAnswerType) iterator
						.next();
				Protector protector = getPrismContext().getDefaultProtector();
				String decoded = "";
				if (securityQuestionAnswerType.getQuestionAnswer().getEncryptedDataType() != null) {
					try {
						decoded = protector.decryptString(securityQuestionAnswerType.getQuestionAnswer());
					} catch (EncryptionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				secQuestAnswListDTO.add(new SecurityQuestionAnswerDTO(securityQuestionAnswerType
						.getQuestionIdentifier(), decoded));
			}

			return secQuestAnswListDTO;

		} else {
			return null;
		}

	}

	private void cancelPerformed(AjaxRequestTarget target) {
		setResponsePage(getMidpointApplication().getHomePage());
    }

	private SecurityQuestionAnswerDTO checkIfQuestionisValid(SecurityQuestionAnswerDTO questionIdentifier,
			List<SecurityQuestionDefinitionType> securityQuestionList) {

		for (Iterator iterator = securityQuestionList.iterator(); iterator.hasNext();) {
			SecurityQuestionDefinitionType securityQuestionDefinitionType = (SecurityQuestionDefinitionType) iterator
					.next();
			if (securityQuestionDefinitionType.getIdentifier().trim()
					.equalsIgnoreCase(questionIdentifier.getPwdQuestion().trim())) {
				questionIdentifier.setQuestionItself(securityQuestionDefinitionType.getQuestionText());
				return questionIdentifier;
			}

		}
		return null;
	}

	private String getQuestionIdentifierFromQuestion(String questionItself) {
		for (Iterator iterator = policyQuestionList.iterator(); iterator.hasNext();) {
			SecurityQuestionDefinitionType securityQuestionDefinitionType = (SecurityQuestionDefinitionType) iterator
					.next();
			if (questionItself.equalsIgnoreCase(securityQuestionDefinitionType.getQuestionText()))
				return securityQuestionDefinitionType.getIdentifier();

		}
		return null;
	}

	public PageBase getPageBase() {
		return (PageBase) getPage();
	}

	private void resetPassword(UserType user, AjaxRequestTarget target) {

		Task task = createAnonymousTask(OPERATION_RESET_PASSWORD);
		OperationResult result = task.getResult();

		LOGGER.debug("Resetting password for {}", user);

		ProtectedStringType password = new ProtectedStringType();
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
				GetOperationOptions.createResolve(), SystemConfigurationType.F_DEFAULT_USER_TEMPLATE,
				SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY);
		PrismObject<SystemConfigurationType> systemConfig = null;
		String newPassword = "";
		PageBase page = (PageBase) getPage();

		ModelService modelService = page.getModelService();
		try {

			systemConfig = modelService.getObject(SystemConfigurationType.class,
					SystemObjectsType.SYSTEM_CONFIGURATION.value(), options, task, result);
			LOGGER.trace("system config {}", systemConfig);
			if (systemConfig.asObjectable().getNotificationConfiguration() != null) {
				// New password is automatically reset according to the global
				// Security policy with the minumum number of chars
				if (systemConfig.asObjectable().getGlobalPasswordPolicyRef() != null) {
					PrismObject<ValuePolicyType> valPolicy = modelService.getObject(ValuePolicyType.class,
							systemConfig.asObjectable().getGlobalPasswordPolicyRef().getOid(), options, task,
							result);
					LOGGER.trace("password policy {}", valPolicy);
					newPassword = getModelInteractionService().generateValue(valPolicy.asObjectable().getStringPolicy(),
							valPolicy.asObjectable().getStringPolicy().getLimitations().getMinLength(), false,
							user.asPrismObject(), "security questions password generation", task, result);
				} else {
					// TODO What if there is no policy? What should be done to
					// provide a new automatic password
					warn(getString("pageSecurityQuestions.message.noPolicySet"));
					target.add(getFeedbackPanel());
					return;
				}

			} else {
				// TODO localization
				getSession().error(getString("pageSecurityQuestions.message.notificationsNotSet"));
				LOGGER.trace("Notifications not set, returning to login page");
				throw new RestartResponseException(PageLogin.class);

			}
		} catch (ObjectNotFoundException | ExpressionEvaluationException e1) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reset password", e1);

		} catch (SchemaException e1) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reset password", e1);
			e1.printStackTrace();
		} catch (SecurityViolationException e1) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reset password", e1);
		} catch (CommunicationException e1) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reset password", e1);
		} catch (ConfigurationException e1) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reset password", e1);
		}

		password.setClearValue(newPassword);

		WebComponentUtil.encryptProtectedString(password, true, getMidpointApplication());
		final ItemPath valuePath = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
				CredentialsType.F_PASSWORD, PasswordType.F_VALUE);

		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(UserType.class);

		PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(valuePath, objDef, password);
		Class<? extends ObjectType> type = UserType.class;

		deltas.add(ObjectDelta.createModifyDelta(user.getOid(), delta, type, getPrismContext()));
		try {

			modelService.executeChanges(deltas, null, task, result);

			OperationResult parentResult = new OperationResult(OPERATION_LOAD_RESET_PASSWORD_POLICY);
			try {

				if (getModelInteractionService().getCredentialsPolicy(null, null, parentResult)
						.getSecurityQuestions().getResetMethod().getResetType()
						.equals(CredentialsResetTypeType.SECURITY_QUESTIONS)) {
					getSession().setAttribute("pwdReset", newPassword);
					setResponsePage(PageShowPassword.class);
				} else if (getModelInteractionService().getCredentialsPolicy(null, null, parentResult)
						.getSecurityQuestions().getResetMethod().getResetType()
						.equals(CredentialsResetTypeType.SECURITY_QUESTIONS_EMAIL)) {
					if (systemConfig.asObjectable().getNotificationConfiguration() != null
							&& systemConfig.asObjectable().getNotificationConfiguration().getMail() != null) {
						MailConfigurationType mailConfig = systemConfig.asObjectable()
								.getNotificationConfiguration().getMail();

						if (mailConfig.getServer() != null) {
							List serverList = mailConfig.getServer();

							if (serverList.size() > 0) {
								MailServerConfigurationType mailServerType = mailConfig.getServer().get(0);
								sendMailToUser(mailServerType.getUsername(), getMidpointApplication()
										.getProtector().decryptString(mailServerType.getPassword()),
										newPassword, mailServerType.getHost(), mailServerType.getPort()
												.toString(), mailConfig.getDefaultFrom(),
										user.getEmailAddress());
							} else {
								getSession()
										.error(getString("pageLogin.message.ForgetPasswordSettingsWrong"));
								throw new RestartResponseException(PageLogin.class);
							}

						} else {
							getSession().error(getString("pageLogin.message.ForgetPasswordSettingsWrong"));
							throw new RestartResponseException(PageLogin.class);
						}

					} else {
						// System.out.println("ifff4");
						getSession().error(getString("pageLogin.message.ForgetPasswordSettingsWrong"));
						throw new RestartResponseException(PageLogin.class);
					}

				}
			} catch (ObjectNotFoundException | SchemaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// TODO ASAP a message should be shown as the result of the process
			// MailMessage mailMessage=new MailMessage(, port);
			// mailTransport.send(mailMessage, transportName, task,
			// parentResult);
		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException | EncryptionException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "reset password exception", e);
		}
	}


	public void sendMailToUser(final String userLogin, final String password, String newPassword,
			String host, String port, String sender, String receiver) {
		try {

			Properties props = new Properties();

			props.put("mail.transport.protocol", "smtp");
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.host", host);
			props.put("mail.smtp.port", port);
			props.put("mail.smtp.starttls.enable", "true");

			Session session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(userLogin, password);
				}
			});

			Message message = new MimeMessage(session);

			// TODO Localization
			message.setSubject("New Midpoint Password");

			message.setText("Password : " + newPassword + "\n");
			message.setFrom(new InternetAddress(sender));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));

			Transport.send(message);

			/*
			 * Session mailSession = Session.getDefaultInstance(props);
			 * MimeMessage message = new MimeMessage(mailSession);
			 *
			 * message.setSubject("Engerek KYS Yeni Şifreniz");
			 *
			 * message.setText("User Login : " + userLogin + "\n Password : " +
			 * password + "\n"); message.setFrom(new InternetAddress(sender));
			 * message.addRecipient(Message.RecipientType.TO, new
			 * InternetAddress(receiver)); Transport transport =
			 * mailSession.getTransport(); transport.connect();
			 * transport.sendMessage(message,
			 * message.getRecipients(Message.RecipientType.TO));
			 * transport.close();
			 */
		} catch (MessagingException ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Mail send Exception", ex);
		}

	}

}