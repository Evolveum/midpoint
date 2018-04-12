/*
 * Portions Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.web.page.admin.home;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.home.component.MyPasswordQuestionsPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordQuestionsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SecurityQuestionAnswerDTO;
import com.evolveum.midpoint.web.page.self.PageSelf;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;


@PageDescriptor(url = "/PasswordQuestions", action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL,
                label = "PageSelfCredentials.auth.credentials.label",
                description = "PageSelfCredentials.auth.credentials.description")})
public class PageMyPasswordQuestions extends PageAdminHome {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageMyPasswordQuestions.class);

	private static final String DOT_CLASS = PageMyPasswordQuestions.class.getName() + ".";
	private static final String OPERATION_LOAD_USER = DOT_CLASS + "loaduser";
	private static final String OPERATION_LOAD_QUESTION_POLICY = DOT_CLASS + "LOAD Question Policy";
	private String ID_PASSWORD_QUESTIONS_PANEL = "pwdQuestionsPanel";
	private static final String OPERATION_SAVE_QUESTIONS="Save Security Questions";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_BACK = "back";
	private static final String ID_SAVE = "save";
	private LoadableModel<ObjectWrapper> userModel;


	private List<MyPasswordQuestionsPanel> pqPanels;
	private IModel<PasswordQuestionsDto> model;
	private List<SecurityQuestionDefinitionType> policyQuestionList;
	private MyPasswordQuestionsPanel pwPanel;
	int questionNumber;


	public PageMyPasswordQuestions() {


		model = new LoadableModel<PasswordQuestionsDto>(false) {

			private static final long serialVersionUID = 1L;

			@Override
			protected PasswordQuestionsDto load() {
				return loadPageModel();
			}
		};

		initLayout();

	}

    public PageMyPasswordQuestions(IModel<PasswordQuestionsDto> model){
        this.model = model;
        initLayout();
    }

	public PageMyPasswordQuestions(final PrismObject<UserType> userToEdit) {
		userModel = new LoadableModel<ObjectWrapper>(false) {

			@Override
			protected ObjectWrapper load() {
				return loadUserWrapper(userToEdit);
			}
		};
		initLayout();
	}



	private PasswordQuestionsDto loadPageModel() {
		LOGGER.debug("Loading user for Security Question Page.");

		PasswordQuestionsDto dto =new PasswordQuestionsDto();
		OperationResult result = new OperationResult(OPERATION_LOAD_USER);
		try{


			String userOid = SecurityUtils.getPrincipalUser().getOid();
			Task task = createSimpleTask(OPERATION_LOAD_USER);
			OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);

			PrismObject<UserType> user = getModelService().getObject(UserType.class, userOid, null, task, subResult);

			dto.setSecurityAnswers(createUsersSecurityQuestionsList(user));

			subResult.recordSuccessIfUnknown();

		}
		catch (Exception ex) {
			LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get user Questions, Probably not set yet", ex);

		} finally {
			result.recomputeStatus();
		}
		return dto;
	}

	public List<SecurityQuestionAnswerDTO> createUsersSecurityQuestionsList(PrismObject<UserType> user){
		LOGGER.debug("Security Questions Loading for user: "+ user.getOid());
        if (user.asObjectable().getCredentials() != null && user.asObjectable().getCredentials().getSecurityQuestions() != null) {
            List<SecurityQuestionAnswerType> secQuestAnsList = user.asObjectable().getCredentials().getSecurityQuestions().getQuestionAnswer();

            if (secQuestAnsList != null) {

                LOGGER.debug("User SecurityQuestion ANswer List is Not null");
                List<SecurityQuestionAnswerDTO> secQuestAnswListDTO = new ArrayList<>();
                for (Iterator iterator = secQuestAnsList.iterator(); iterator
                        .hasNext(); ) {
                    SecurityQuestionAnswerType securityQuestionAnswerType = (SecurityQuestionAnswerType) iterator
                            .next();

                    Protector protector = getPrismContext().getDefaultProtector();
                    String decoded = "";
                    if (securityQuestionAnswerType.getQuestionAnswer().getEncryptedDataType() != null) {
                        try {
                            decoded = protector.decryptString(securityQuestionAnswerType.getQuestionAnswer());

                        } catch (EncryptionException e) {
                            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't decrypt user answer", e);

                        }
                    }
                    //LOGGER.debug("SecAnswerIdentifier:"+securityQuestionAnswerType.getQuestionIdentifier());
                    secQuestAnswListDTO.add(new SecurityQuestionAnswerDTO(securityQuestionAnswerType.getQuestionIdentifier(), decoded));
                }

                return secQuestAnswListDTO;
            }
        }
        return null;
    }


	public void initLayout(){


		Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);

		//question panel list
		pqPanels = new ArrayList<>();
		OperationResult result = new OperationResult(OPERATION_LOAD_QUESTION_POLICY);
		try{


			Task task = getPageBase().createSimpleTask(OPERATION_LOAD_QUESTION_POLICY);
			OperationResult subResult = result.createSubresult(OPERATION_LOAD_QUESTION_POLICY);
			try{
			//PrismObject<SystemConfigurationType> config = getPageBase().getModelService().getObject(
				//	SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null,
					//task, result);

			CredentialsPolicyType credPolicy=getModelInteractionService().getCredentialsPolicy(null, null, result);

		//	PrismObject<SecurityPolicyType> securityPolicy = getModelService().getObject(SecurityPolicyType.class,config.asObjectable().getGlobalSecurityPolicyRef().getOid(), null, task, subResult);
			//Global Policy set question numbers
                if (credPolicy != null && credPolicy.getSecurityQuestions() != null) {
                    questionNumber = credPolicy.getSecurityQuestions().getQuestionNumber();

                    // Actual Policy Question List
                    policyQuestionList = credPolicy.getSecurityQuestions().getQuestion();
                } else {
                    questionNumber = 0;
                    policyQuestionList = new ArrayList<>();
                }
			}catch(Exception ex){
				ex.printStackTrace();

			/*	List<SecurityQuestionAnswerDTO> userQuestionList= model.getObject().getSecurityAnswers();
				int panelNumber=0;
				PrismObject<UserType> user = null;



				Collection options = SelectorOptions.createCollection(UserType.F_CREDENTIALS,
						GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
				Task taskTwo = createSimpleTask("LOAD USER WRAPPER");
				user = getModelService().getObject(UserType.class, SecurityUtils.getPrincipalUser().getOid(), options, taskTwo, result);

				OperationResult parentResult = new OperationResult(OPERATION_LOAD_QUESTION_POLICY);
				questionNumber = getModelInteractionService().getCredentialsPolicy(user, parentResult).getSecurityQuestions().getQuestionNumber();

				policyQuestionList=getModelInteractionService().getCredentialsPolicy(user, parentResult).getSecurityQuestions().getQuestion();
				if(userQuestionList==null){

					executeAddingQuestions(questionNumber, 0, policyQuestionList);

					LOGGER.info(getModelInteractionService().getCredentialsPolicy(user, parentResult).getSecurityQuestions().getQuestionNumber().toString());

				}else{
					for(int userQuestint=0;userQuestint<userQuestionList.size();userQuestint++){
						SecurityQuestionAnswerDTO answerDTO=  checkIfQuestionisValid(userQuestionList.get(userQuestint), policyQuestionList);
						if (userQuestionList.get(userQuestint)!=null){
							LOGGER.debug("Questitself"+userQuestionList.get(userQuestint).getQuestionItself());
							MyPasswordQuestionsPanel panel=new MyPasswordQuestionsPanel(ID_PASSWORD_QUESTIONS_PANEL+ panelNumber,userQuestionList.get(userQuestint));
							pqPanels.add(panel);
							panelNumber++;
						}

					}
					//TODO same questions check should be implemented

				}
				add(mainForm);
				mainForm.add(getPanels(pqPanels));

				initButtons(mainForm);
				return;
			*/
			}

			/*User's Pre-Set Question List*/
			List<SecurityQuestionAnswerDTO> userQuestionList= model.getObject().getSecurityAnswers();

			/* check if user's set number of
			 * questions matches the policy or not*/

			//Case that policy have more than users's number of numbers
			if((userQuestionList==null) || (questionNumber>userQuestionList.size())){
				if(userQuestionList==null){
					executeAddingQuestions(questionNumber, 0, policyQuestionList);
					//TODO same questions check should be implemented

				}else{
					executePasswordQuestionsAndAnswers(userQuestionList, policyQuestionList, userQuestionList.size());
					//QUESTION NUMBER BIGGER THAN QUESTION LIST
					//rest of the questions
					int difference=questionNumber-userQuestionList.size();
					executeAddingQuestions(difference, userQuestionList.size(), policyQuestionList);

				}


			}else if(questionNumber==userQuestionList.size()){
				//QUESTION NUMBER EQUALS TO QUESTION LIST
				executePasswordQuestionsAndAnswers(userQuestionList, policyQuestionList, 0);

				//TODO PART2: Case that policy have smaller than users's number of numbers
			}else if(questionNumber < userQuestionList.size()){

				//QUESTION NUMBER SMALLER THAN QUESTION LIST
				executePasswordQuestionsAndAnswers(userQuestionList, policyQuestionList, 0);


				//this part will be using at remove operation in the future
			/*	int diff = userQuestionList.size()-questionNumber;
				for(Iterator iterator = userQuestionList.iterator(); iterator.hasNext();){

					SecurityQuestionAnswerDTO element = (SecurityQuestionAnswerDTO)iterator.next();
					for(int i=0; i<diff;i++){
						if(element == userQuestionList.get(questionNumber+i)){

							try{
								//LOGGER.info("REMOVE");
								iterator.remove();
							} catch (UnsupportedOperationException uoe) {
					            LOGGER.info(uoe.getStackTrace().toString());
					        }
					     }
					}
				}*/
			}

		} catch (Exception ex) {


			result.recordFatalError("Couldn't load system configuration.", ex);
		}


		add(mainForm);
		mainForm.add(getPanels(pqPanels));
		initButtons(mainForm);

	}

	/**
	 * method for adding questions to user credentials
	 * @author oguzhan
	 * @param questionNumber
	 * @param panelNumber
	 * @param policyQuestionList
	 */
	public void executeAddingQuestions(int questionNumber,int panelNumber,List<SecurityQuestionDefinitionType> policyQuestionList){
		LOGGER.debug("executeAddingQuestions");
		for(int i=0;i<questionNumber;i++){
			//LOGGER.info("\n\n Adding panel element");
		SecurityQuestionAnswerDTO a=new SecurityQuestionAnswerDTO(policyQuestionList.get(panelNumber).getIdentifier(),"",policyQuestionList.get(panelNumber).getQuestionText());
		MyPasswordQuestionsPanel panel=new MyPasswordQuestionsPanel(ID_PASSWORD_QUESTIONS_PANEL+ panelNumber,a);
		pqPanels.add(panel);
		panelNumber++;

		}

	}

	/**
	 * method for get existing questions and answer from user credentials
	 * @author oguzhan
	 * @param userQuestionList
	 * @param policyQuestionList
	 * @param panelNumber
	 */
	public void executePasswordQuestionsAndAnswers(List<SecurityQuestionAnswerDTO> userQuestionList,List<SecurityQuestionDefinitionType> policyQuestionList, int panelNumber ){
		int userQuest =0;
		LOGGER.debug("executePasswordQuestionsAndAnswers");
		for (Iterator iterator = policyQuestionList.iterator(); iterator.hasNext();) {


		/* Loop for finding the Existing Questions
		 * and Answers according to Policy*/

			SecurityQuestionDefinitionType securityQuestionDefinitionType = (SecurityQuestionDefinitionType) iterator
					.next();
			//user's question List loop to match the questions
			for(int i=userQuest;i<userQuestionList.size();i++){

				if(userQuestionList.get(i).getPwdQuestion().trim().compareTo(securityQuestionDefinitionType.getIdentifier().trim())==0)
				{

					SecurityQuestionAnswerDTO a=new SecurityQuestionAnswerDTO(userQuestionList.get(i).getPwdQuestion(),userQuestionList.get(i).getPwdAnswer(),userQuestionList.get(i).getQuestionItself());

					a= checkIfQuestionisValidSingle(a, securityQuestionDefinitionType);
					MyPasswordQuestionsPanel panel=new MyPasswordQuestionsPanel(ID_PASSWORD_QUESTIONS_PANEL+ panelNumber,a);
					pqPanels.add(panel);
					panelNumber++;
					userQuest++;
					break;

				}
				else if(userQuestionList.get(i).getPwdQuestion().trim().compareTo(securityQuestionDefinitionType.getIdentifier().trim())!=0){

					SecurityQuestionAnswerDTO a=new SecurityQuestionAnswerDTO(policyQuestionList.get(panelNumber).getIdentifier(),"",policyQuestionList.get(panelNumber).getQuestionText());
					a.setQuestionItself(securityQuestionDefinitionType.getQuestionText());
					userQuestionList.get(i).setPwdQuestion(securityQuestionDefinitionType.getIdentifier().trim());

					MyPasswordQuestionsPanel panel=new MyPasswordQuestionsPanel(ID_PASSWORD_QUESTIONS_PANEL+ panelNumber,a);
					pqPanels.add(panel);
					panelNumber++;

					userQuest++;
					break;

				}

			}

		}

	}




	public ListView<MyPasswordQuestionsPanel> getPanels(List<MyPasswordQuestionsPanel> p){
		ListView lw = new ListView(ID_PASSWORD_QUESTIONS_PANEL,p){
			@Override
			protected void populateItem(ListItem item) {

				item.add((MyPasswordQuestionsPanel)item.getModelObject());
			}
		};
		return lw;
	}

	public void initButtons(Form mainForm){
		AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE, createStringResource("PageBase.button.save")) {

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

	private void savePerformed(AjaxRequestTarget target) {

		/*
		 * Oguzhan: added target variable to the updateQuestions method.
		 */
		updateQuestions(SecurityUtils.getPrincipalUser().getOid(), target);

	}

	private void cancelPerformed(AjaxRequestTarget target){
		setResponsePage(getMidpointApplication().getHomePage());
    }

	private ObjectWrapper loadUserWrapper(PrismObject<UserType> userToEdit) {
		OperationResult result = new OperationResult(OPERATION_LOAD_USER);
		PrismObject<UserType> user = null;
		Task task = createSimpleTask(OPERATION_LOAD_USER);
		try {
			Collection options = SelectorOptions.createCollection(UserType.F_CREDENTIALS,
					GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));

			user = getModelService().getObject(UserType.class, SecurityUtils.getPrincipalUser().getOid(), options, task, result);


			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get user.", ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load user PageMyQuestions", ex);
		}

			showResult(result, false);

		if (user == null) {

			throw new RestartResponseException(PageDashboard.class);
		}

		ContainerStatus status = ContainerStatus.MODIFYING;
		ObjectWrapperFactory owf = new ObjectWrapperFactory(this);
		ObjectWrapper wrapper;
		try{
			wrapper = owf.createObjectWrapper("pageMyPasswordQuestions.userDetails", null, user, status, task);
		} catch (Exception ex){
			result.recordFatalError("Couldn't get user.", ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load user", ex);
			try {
				wrapper = owf.createObjectWrapper("pageMyPasswordQuestions.userDetails", null, user, null, null, status, task);
			} catch (SchemaException e) {
				throw new SystemException(e.getMessage(), e);
			}
		}
		//        ObjectWrapper wrapper = new ObjectWrapper("pageUser.userDetails", null, user, status);
			showResult(wrapper.getResult(), false);


		return wrapper;
	}

	private SecurityQuestionAnswerDTO checkIfQuestionisValid(SecurityQuestionAnswerDTO questionIdentifier,List<SecurityQuestionDefinitionType> securityQuestionList){


		for (Iterator iterator = securityQuestionList.iterator(); iterator
				.hasNext();) {
			SecurityQuestionDefinitionType securityQuestionDefinitionType = (SecurityQuestionDefinitionType) iterator
					.next();
			LOGGER.debug("List For"+securityQuestionDefinitionType.getIdentifier().trim());
			if(securityQuestionDefinitionType.getIdentifier().trim().equalsIgnoreCase((questionIdentifier.getPwdQuestion().trim()))){
				questionIdentifier.setQuestionItself(securityQuestionDefinitionType.getQuestionText());

				LOGGER.info(": TRUE QUESTION");
				return questionIdentifier;
			}else{
				return null;
			}


		}

		return null;
	}

private SecurityQuestionAnswerDTO checkIfQuestionisValidSingle(SecurityQuestionAnswerDTO questionIdentifier,SecurityQuestionDefinitionType securityQuestion){

		if(securityQuestion.getIdentifier().trim().compareTo(questionIdentifier.getPwdQuestion().trim())==0){
			questionIdentifier.setQuestionItself(securityQuestion.getQuestionText());

			//LOGGER.info("\n\n: TRUE QUESTION");
			return questionIdentifier;
		}else{
			return null;
		}
}



	private void updateQuestions(String useroid, AjaxRequestTarget target){


		Task task = createSimpleTask(OPERATION_SAVE_QUESTIONS);
		OperationResult result = new OperationResult(OPERATION_SAVE_QUESTIONS);
		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		SecurityQuestionAnswerType[] answerTypeList=new SecurityQuestionAnswerType[questionNumber];

		try {
			int listnum=0;
			for (Iterator iterator = pqPanels.iterator(); iterator.hasNext();) {
				MyPasswordQuestionsPanel type = (MyPasswordQuestionsPanel) iterator.next();

				SecurityQuestionAnswerType answerType = new SecurityQuestionAnswerType();
				ProtectedStringType answer = new ProtectedStringType();

				answer.setClearValue(((TextField<String>)type.get(MyPasswordQuestionsPanel.F_ANSWER)).getModelObject());
				answerType.setQuestionAnswer(answer);

				//used apache's unescapeHtml method for special chars like \'
				String results = StringEscapeUtils.unescapeHtml((type.get(MyPasswordQuestionsPanel.F_QUESTION)).getDefaultModelObjectAsString());
				answerType.setQuestionIdentifier(getQuestionIdentifierFromQuestion(results));
				answerTypeList[listnum]=answerType;
				listnum++;

			}

			//if(answerTypeList.length !=)


			// fill in answerType data here
			ItemPath path = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_SECURITY_QUESTIONS, SecurityQuestionsCredentialsType.F_QUESTION_ANSWER);
			ObjectDelta<UserType> objectDelta = ObjectDelta.createModificationReplaceContainer(UserType.class, useroid,
					path, getPrismContext(), answerTypeList);

			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
			getModelService().executeChanges(deltas, null, task, result);

			/*
			System.out.println("getModel");
			 Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
			PasswordQuestionsDto dto = new PasswordQuestionsDto();
			 PrismObjectDefinition objDef =registry.findObjectDefinitionByCompileTimeClass(UserType.class);
			 Class<? extends ObjectType> type =  UserType.class;

			 final ItemPath valuePath = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
	                  CredentialsType.F_SECURITY_QUESTIONS, SecurityQuestionsCredentialsType.F_QUESTION_ANSWER);
			 SecurityQuestionAnswerType secQuesAnsType= new SecurityQuestionAnswerType();
			 ProtectedStringType protStrType= new ProtectedStringType();
			 protStrType.setClearValue("deneme");
			 secQuesAnsType.setQuestionAnswer(protStrType);
			 dto.setSecurityAnswers(new ArrayList<SecurityQuestionAnswerType>());
			 dto.getSecurityAnswers().add(secQuesAnsType);

			PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(valuePath, objDef, dto.getSecurityAnswers().get(0).getQuestionAnswer());
		//	PropertyDelta delta= PropertyDelta.createModifica

			 System.out.println("Update Questions3");
			deltas.add(ObjectDelta.createModifyDelta(useroid, delta, type, getPrismContext()));
			System.out.println("Update Questions4");
			getModelService().executeChanges(deltas, null, createSimpleTask(OPERATION_SAVE_QUESTIONS), result);
			System.out.println("Update Questions5");

			 */
			success(getString("message.success"));
		    target.add(getFeedbackPanel());
		} catch(Exception ex){

			error(getString("message.error"));
			target.add(getFeedbackPanel());
			ex.printStackTrace();
		}
	}

	private String getQuestionIdentifierFromQuestion(String questionItself){
		//LOGGER.info("\n\n QUESTION: "+questionItself);
		for (Iterator iterator = policyQuestionList.iterator(); iterator
				.hasNext();) {
			SecurityQuestionDefinitionType securityQuestionDefinitionType = (SecurityQuestionDefinitionType) iterator
					.next();
			if(questionItself.equalsIgnoreCase(securityQuestionDefinitionType.getQuestionText()))
				return securityQuestionDefinitionType.getIdentifier();



		}
		return null;
	}
	public PageBase getPageBase() {
		return (PageBase) getPage();
	}

}
