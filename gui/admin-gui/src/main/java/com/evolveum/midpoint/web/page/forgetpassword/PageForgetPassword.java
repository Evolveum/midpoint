package com.evolveum.midpoint.web.page.forgetpassword;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.midpoint.common.policy.ValuePolicyGenerator;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;





import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsResetTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
@PageDescriptor(url = "/forgetpassword")
public class PageForgetPassword extends PageBase {


	private static final String ID_PWDRESETFORM = "pwdresetform";

	private static final String ID_USERNAME = "username";
	private static final String ID_EMAIL ="email";
	private static final String DOT_CLASS = PageForgetPassword.class.getName() + ".";
	private static final Trace LOGGER = TraceManager.getTrace(PageForgetPassword.class);
	protected static final String OPERATION_LOAD_RESET_PASSWORD_POLICY = "LOAD PASSWORD RESET POLICY";
	private static final String OPERATION_RESET_PASSWORD = DOT_CLASS + "resetPassword";
	private static final String LOAD_USER = DOT_CLASS + "loadUser";

	PageBase page = (PageBase) getPage();



	public PageForgetPassword() {


		Form form = new Form(ID_PWDRESETFORM) {


			@Override
			protected void onSubmit() {
				LOGGER.info("Reset Password user info form submitted.");
				RequiredTextField<String> username = (RequiredTextField) get(ID_USERNAME);
				RequiredTextField<String> email = (RequiredTextField) get(ID_EMAIL);



				UserType user= checkUser(email.getModelObject(),username.getModelObject() );
				//System.out.println("checkkk");
				if(user!=null){
					//If the parameters are ok reset the password
					
					//		PageParameters parameters = new PageParameters();
					//	PageForgetPasswordQuestions pageForgetPasswordQuestions =new PageForgetPasswordQuestions();
					//		pageForgetPasswordQuestions.setUserTypeObject(user);

					//System.out.println("pOid");
					getSession().setAttribute("pOid", user.getOid());
					getSecurityEnforcer().setupPreAuthenticatedSecurityContext((Authentication) null);
			        setResponsePage(PageSecurityQuestions.class);

				/*	OperationResult parentResult = new OperationResult(OPERATION_LOAD_RESET_PASSWORD_POLICY);
					try {
						
						System.out.println("try");
						if(	getModelInteractionService().getCredentialsPolicy(null, parentResult).getSecurityQuestions().getResetMethod().getResetType().equals(CredentialsResetTypeType.SECURITY_QUESTIONS)){
							System.out.println("ifff");
							getSession().setAttribute("pwdReset", resetPassword(user));	
							setResponsePage(PageShowPassword.class);
						}
					} catch (ObjectNotFoundException | SchemaException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				*/	
					/*
								


								PageParameters parameters = new PageParameters();
						        parameters.add(OnePageParameterEncoder.PARAMETER, user.getOid());
					
					 */
				}
				else{
					
					getSession().error(getString("pageForgetPassword.message.usernotfound"));
					getSession().invalidate();
					SecurityContext securityContext = SecurityContextHolder.getContext();
					securityContext.setAuthentication(null);
					throw new RestartResponseException(PageForgetPassword.class);


				}


			}
		};

		form.add(new RequiredTextField(ID_USERNAME, new Model<String>()));
		form.add(new RequiredTextField(ID_EMAIL, new Model<String>()));

		add(form);


	}

	//Checkd if the user exists with the given email and username in the idm
	public UserType checkUser(String email,String username){

		try{
			SecurityContext securityContext = SecurityContextHolder.getContext();
			UserType userAdministrator = new UserType();
			PrismContext prismContext = page.getPrismContext();
			prismContext.adopt(userAdministrator);
			userAdministrator.setName(new PolyStringType(new PolyString("initAdmin", "initAdmin")));
			MidPointPrincipal principal = new MidPointPrincipal(userAdministrator);
			AuthorizationType superAutzType = new AuthorizationType();
			prismContext.adopt(superAutzType, RoleType.class, new ItemPath(RoleType.F_AUTHORIZATION));
			superAutzType.getAction().add(AuthorizationConstants.AUTZ_ALL_URL);
			Authorization superAutz = new Authorization(superAutzType);
			Collection<Authorization> authorities = principal.getAuthorities();
			authorities.add(superAutz);
			Authentication authentication = new PreAuthenticatedAuthenticationToken(principal, null);
			securityContext.setAuthentication(authentication);

		} catch ( SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}


		OperationResult result = new OperationResult(LOAD_USER);	
		String idmEmail=null;
		Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
		options.add(SelectorOptions.create(ItemPath.EMPTY_PATH,
				GetOperationOptions.createRetrieve(RetrieveOption.DEFAULT)));

		Task task = createSimpleTask(LOAD_USER);
		OperationResult subResult = result.createSubresult(LOAD_USER);

		LOGGER.debug("CheckUser Poly oncesi");
		PolyString userId = new PolyString(username, username);
		PolyString emailAddress = new PolyString(username, username);
		List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

		EqualFilter filter;


		ModelService model = page.getModelService();
		try {
			filters.add(EqualFilter.createEqual(UserType.F_NAME, UserType.class,getPrismContext(),PolyStringOrigMatchingRule.NAME,username));
			filters.add(EqualFilter.createEqual(UserType.F_EMAIL_ADDRESS, UserType.class,getPrismContext(),PolyStringOrigMatchingRule.NAME,email));

			ObjectQuery query = new ObjectQuery().createObjectQuery(AndFilter.createAnd(filters));


			List<PrismObject<UserType>> userList= WebModelUtils.searchObjects(UserType.class, query,
					result, this);

			//	model.searchObjects(UserType.class, query, options, task, subResult);


			if((userList!=null) && (!userList.isEmpty())){

				LOGGER.debug("User found for ForgetPassword");
				UserType user=  userList.get(0).asObjectable();

				if(user.getEmailAddress().equalsIgnoreCase(email)){

					return user;
				}

				else return null;
			}
			else return null;


		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		} 


	}
	
	private void setAuthenticationNull(){
		getSecurityEnforcer().setupPreAuthenticatedSecurityContext((Authentication) null);
	}
	
	private void setAuthentication(){
		try{

			//	LOGGER.debug("*******************PageSecurityQuestions2");
			SecurityContext securityContext = SecurityContextHolder.getContext();
			UserType userAdministrator = new UserType();
			PrismContext prismContext = page.getPrismContext();
			prismContext.adopt(userAdministrator);
			//TODO remove initAdmin, meaningless
			userAdministrator.setName(new PolyStringType(new PolyString("initAdmin", "initAdmin")));
			MidPointPrincipal principal = new MidPointPrincipal(userAdministrator);
			AuthorizationType superAutzType = new AuthorizationType();
			prismContext.adopt(superAutzType, RoleType.class, new ItemPath(RoleType.F_AUTHORIZATION));
			superAutzType.getAction().add(AuthorizationConstants.AUTZ_ALL_URL);
			Authorization superAutz = new Authorization(superAutzType);
			Collection<Authorization> authorities = principal.getAuthorities();
			authorities.add(superAutz);
			Authentication authentication = new PreAuthenticatedAuthenticationToken(principal, null);
			securityContext.setAuthentication(authentication);


		} catch ( SchemaException e) {
			LoggingUtils.logException(LOGGER, "Setting preauthentication token exception", e);
			e.printStackTrace();

		}
	}
	
	
	//resets the user password and returns the new password
	private String resetPassword(UserType user){
		
		setAuthentication();
		Task task = createSimpleTask(OPERATION_RESET_PASSWORD);
		System.out.println("Reset Password1");
		OperationResult result = new OperationResult(OPERATION_RESET_PASSWORD);
		ProtectedStringType password = new ProtectedStringType();
		Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createResolve(),
                        SystemConfigurationType.F_DEFAULT_USER_TEMPLATE ,SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY);
	    System.out.println("Reset Password2");
		PrismObject<SystemConfigurationType> systemConfig=null;
		String newPassword="";
		PageBase page = (PageBase) getPage();
 
		ModelService model = page.getModelService();
		try {
			System.out.println("getModel");
			systemConfig = model.getObject(SystemConfigurationType.class,
			        SystemObjectsType.SYSTEM_CONFIGURATION.value(), options, task, result);
			
			PrismObject<ValuePolicyType> valPolicy =model.getObject(ValuePolicyType.class, systemConfig.asObjectable().getGlobalPasswordPolicyRef().getOid(), options, task, result);
			
			newPassword=ValuePolicyGenerator.generate(valPolicy.asObjectable().getStringPolicy(), valPolicy.asObjectable().getStringPolicy().getLimitations().getMinLength(), result);
			
			System.out.println("Reset Password3");
		} catch (ObjectNotFoundException e1) {
			// TODO Auto-generated catch block
			System.out.println("hata1");
			System.out.println(e1);
		} catch (SchemaException e1) {
			System.out.println(e1);
			System.out.println("hata2");
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SecurityViolationException e1) {
			System.out.println(e1);
			System.out.println("hata3");
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (CommunicationException e1) {
			System.out.println("hata4");
			System.out.println(e1);
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ConfigurationException e1) {
			System.out.println("hata");
			System.out.println(e1);
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		System.out.println("Passs:"+newPassword);
		password.setClearValue(newPassword);

		WebMiscUtil.encryptProtectedString(password, true, getMidpointApplication());
		final ItemPath valuePath = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
				CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
		System.out.println("Reset Password4");
		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		PrismObjectDefinition objDef= registry.findObjectDefinitionByCompileTimeClass(UserType.class);

		PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(valuePath, objDef, password);
		Class<? extends ObjectType> type =  UserType.class;
	
		
		deltas.add(ObjectDelta.createModifyDelta(user.getOid(), delta, type, getPrismContext()));
		try {
			//System.out.println("Reset Password5");
		
				getModelService().executeChanges(deltas, null, task, result);
			setAuthenticationNull();
			return newPassword;
		//	MailMessage mailMessage=new MailMessage(, port);
	//		mailTransport.send(mailMessage, transportName, task, parentResult);
		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException
				| CommunicationException | ConfigurationException | PolicyViolationException
				| SecurityViolationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			setAuthenticationNull();
			return null;
		} 

	}

}
