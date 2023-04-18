/*
 * Portions Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.home.component.MyPasswordQuestionsPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordQuestionsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SecurityQuestionAnswerDTO;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.Model;

public class SecurityQuestionsPanel extends BasePanel<PasswordQuestionsDto> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SecurityQuestionsPanel.class);

    private static final String DOT_CLASS = SecurityQuestionsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loaduser";
    private static final String OPERATION_LOAD_QUESTION_POLICY = DOT_CLASS + "LOAD Question Policy";
    private static final String OPERATION_SAVE_QUESTIONS = DOT_CLASS + "saveSecurityQuestions";
    private static final String ID_SECURITY_QUESTIONS_PANEL = "pwdQuestionsPanel";
    private static final String ID_QUESTION_ANSWER_PANEL = "questionAnswerPanel";
    private static final String ID_SAVE_ANSWERS = "saveAnswers";

    public SecurityQuestionsPanel(String id, IModel<PasswordQuestionsDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initSecurityQuestionsModel();
        initLayout();
    }

    private void initSecurityQuestionsModel() {
        if (getModelObject() == null) {
            getModel().setObject(loadPageModel());
        }
    }

    private PasswordQuestionsDto loadPageModel() {
        LOGGER.debug("Loading user for Security Question Page.");

        GuiProfiledPrincipal principalUser = AuthUtil.getPrincipalUser();
        PasswordQuestionsDto dto = new PasswordQuestionsDto(principalUser.getOid());
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        try {
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_USER);
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);

            PrismObject<UserType> user = getPageBase().getModelService().getObject(UserType.class, principalUser.getOid(), null, task, subResult);

            dto.setUserQuestionAnswers(createUsersSecurityQuestionsList(user));
            subResult.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get user Questions, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }

        CredentialsPolicyType credPolicy = principalUser.getApplicableSecurityPolicy().getCredentials();

        List<SecurityQuestionDefinitionType> questionsDef = new ArrayList<>();
        //Security Policy set question numbers
        if (credPolicy != null && credPolicy.getSecurityQuestions() != null) {

            // Actual Policy Question List
            questionsDef = getEnabledSecurityQuestions(credPolicy);
        } else {
            LOGGER.debug("Couldn't load credentials for security questions");
        }

        result = new OperationResult(OPERATION_LOAD_QUESTION_POLICY);
        try {

            /*User's Pre-Set Question List*/
            List<SecurityQuestionAnswerDTO> userQuestionList = dto.getUserQuestionAnswers();

            /* check if user's set number of
             * questions matches the policy or not*/

            //Case that policy have more than users's number of numbers
            int questionSize = questionsDef.size();
            if (userQuestionList == null) {
                dto.getActualQuestionAnswers().addAll(executeAddingQuestions(questionSize, 0, questionsDef));
                //TODO same questions check should be implemented
            } else if (questionSize > userQuestionList.size()){
                dto.getActualQuestionAnswers().addAll(executePasswordQuestionsAndAnswers(userQuestionList, questionsDef, userQuestionList.size()));
                //QUESTION NUMBER BIGGER THAN QUESTION LIST
                //rest of the questions
                int difference = questionSize - userQuestionList.size();
                dto.getActualQuestionAnswers().addAll(executeAddingQuestions(difference, userQuestionList.size(), questionsDef));
            } else {
                //QUESTION NUMBER SMALLER THAN QUESTION LIST OR EQUALS TO QUESTION LIST
                dto.getActualQuestionAnswers().addAll(executePasswordQuestionsAndAnswers(userQuestionList, questionsDef, 0));
            }
        } catch (Exception ex) {
            result.recordFatalError(getString("PageMyPasswordQuestions.message.couldNotLoadSysConfig"), ex);
        }

        return dto;
    }

    public List<SecurityQuestionAnswerDTO> createUsersSecurityQuestionsList(PrismObject<UserType> user) {
        LOGGER.debug("Security Questions Loading for user: " + user.getOid());
        if (user.asObjectable().getCredentials() != null && user.asObjectable().getCredentials().getSecurityQuestions() != null) {
            List<SecurityQuestionAnswerType> secQuestAnsList = user.asObjectable().getCredentials().getSecurityQuestions().getQuestionAnswer();

            if (secQuestAnsList != null) {

                LOGGER.debug("User SecurityQuestion ANswer List is Not null");
                List<SecurityQuestionAnswerDTO> secQuestAnswListDTO = new ArrayList<>();
                for (SecurityQuestionAnswerType securityQuestionAnswerType : secQuestAnsList) {
                    Protector protector = getPrismContext().getDefaultProtector();
                    String decoded = "";
                    if (securityQuestionAnswerType.getQuestionAnswer().getEncryptedDataType() != null) {
                        try {
                            decoded = protector.decryptString(securityQuestionAnswerType.getQuestionAnswer());

                        } catch (EncryptionException e) {
                            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't decrypt user answer", e);

                        }
                    }
                    secQuestAnswListDTO.add(new SecurityQuestionAnswerDTO(
                            securityQuestionAnswerType.getQuestionIdentifier(), decoded));
                }

                return secQuestAnswListDTO;
            }
        }
        return null;
    }

    public void initLayout() {
        add(new ListView<>(ID_SECURITY_QUESTIONS_PANEL, getModelObject().getActualQuestionAnswers()) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void populateItem(ListItem<SecurityQuestionAnswerDTO> item) {
                MyPasswordQuestionsPanel panel = new MyPasswordQuestionsPanel(ID_QUESTION_ANSWER_PANEL, Model.of(item.getModelObject()));
                item.add(panel);
            }
        });

        AjaxSubmitButton saveAnswers = new AjaxSubmitButton(ID_SAVE_ANSWERS,
                createStringResource("SecurityQuestionsPanel.saveAnswers")) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                onSavePerformed(target);
            }
        };
        saveAnswers.setOutputMarkupId(true);
        add(saveAnswers);
    }

    private List<SecurityQuestionDefinitionType> getEnabledSecurityQuestions(CredentialsPolicyType credPolicy) {
        List<SecurityQuestionDefinitionType> actualQuestions = credPolicy.getSecurityQuestions().getQuestion();
        List<SecurityQuestionDefinitionType> enabledQuestions = new ArrayList<>();

        for (SecurityQuestionDefinitionType actualQuestion : actualQuestions) {
            if (!Boolean.FALSE.equals(actualQuestion.isEnabled())) {
                enabledQuestions.add(actualQuestion);
            }
        }
        return enabledQuestions;
    }

    /**
     * method for adding questions to user credentials
     *
     * @author oguzhan
     */
    public List<SecurityQuestionAnswerDTO> executeAddingQuestions(int questionNumber, int panelNumber, List<SecurityQuestionDefinitionType> policyQuestionList) {
        LOGGER.debug("executeAddingQuestions");
        List<SecurityQuestionAnswerDTO> questionsAnswer = new ArrayList<>();
        for (int i = 0; i < questionNumber; i++) {
            SecurityQuestionDefinitionType def = policyQuestionList.get(panelNumber);
            SecurityQuestionAnswerDTO a = new SecurityQuestionAnswerDTO(def.getIdentifier(), "", def.getQuestionText());
            questionsAnswer.add(a);
            panelNumber++;
        }
        return questionsAnswer;
    }

    /**
     * method for get existing questions and answer from user credentials
     *
     * @author oguzhan
     */
    public List<SecurityQuestionAnswerDTO> executePasswordQuestionsAndAnswers(List<SecurityQuestionAnswerDTO> userQuestionList, List<SecurityQuestionDefinitionType> policyQuestionList, int panelNumber) {
        int userQuest = 0;
        LOGGER.debug("executePasswordQuestionsAndAnswers");
        List<SecurityQuestionAnswerDTO> secQuestionAnswer = new ArrayList<>();
        for (SecurityQuestionDefinitionType securityQuestionDefinitionType : policyQuestionList) {
            /* Loop for finding the Existing Questions
             * and Answers according to Policy*/

            //user's question List loop to match the questions
            for (int i = userQuest; i < userQuestionList.size(); i++) {

                SecurityQuestionAnswerDTO dto = userQuestionList.get(i);

                if (dto.getPwdQuestionIdentifier().trim().compareTo(securityQuestionDefinitionType.getIdentifier().trim()) == 0) {
                    SecurityQuestionAnswerDTO a = new SecurityQuestionAnswerDTO(dto.getPwdQuestionIdentifier(), dto.getPwdAnswer(), dto.getPwdQuestion());
                    a = checkIfQuestionIsValidSingle(a, securityQuestionDefinitionType);
                    secQuestionAnswer.add(a);

                    panelNumber++;
                    userQuest++;
                    break;

                } else if (dto.getPwdQuestionIdentifier().trim().compareTo(securityQuestionDefinitionType.getIdentifier().trim()) != 0) {
                    SecurityQuestionDefinitionType def = policyQuestionList.get(panelNumber);
                    SecurityQuestionAnswerDTO a = new SecurityQuestionAnswerDTO(def.getIdentifier(), "", def.getQuestionText());
                    a.setPwdQuestion(securityQuestionDefinitionType.getQuestionText());
                    secQuestionAnswer.add(a);
                    dto.setPwdQuestionIdentifier(securityQuestionDefinitionType.getIdentifier().trim());

                    panelNumber++;

                    userQuest++;
                    break;

                }

            }

        }
        return secQuestionAnswer;
    }

    private SecurityQuestionAnswerDTO checkIfQuestionIsValidSingle(
            SecurityQuestionAnswerDTO questionIdentifier, SecurityQuestionDefinitionType securityQuestion) {
        if (securityQuestion.getIdentifier().trim().compareTo(questionIdentifier.getPwdQuestionIdentifier().trim()) == 0) {
            questionIdentifier.setPwdQuestion(securityQuestion.getQuestionText());
            return questionIdentifier;
        } else {
            return null;
        }
    }

    public void onSavePerformed(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OPERATION_SAVE_QUESTIONS);
        OperationResult result = new OperationResult(OPERATION_SAVE_QUESTIONS);
        List<SecurityQuestionAnswerType> answerTypeList = new ArrayList<>();

        try {
            for (SecurityQuestionAnswerDTO answerDto : getModelObject().getActualQuestionAnswers()) {
                SecurityQuestionAnswerType answerType = new SecurityQuestionAnswerType();
                ProtectedStringType answer = new ProtectedStringType();

                if (StringUtils.isEmpty(answerDto.getPwdAnswer())) {
                    continue;
                }
                answer.setClearValue(answerDto.getPwdAnswer());
                if (!answer.isEncrypted()) {
                    WebComponentUtil.encryptProtectedString(answer, true, getPageBase().getMidpointApplication());
                }
                answerType.setQuestionAnswer(answer);

                answerType.setQuestionIdentifier(answerDto.getPwdQuestionIdentifier());
                answerTypeList.add(answerType);
            }

            // fill in answerType data here
            ItemPath path = ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_SECURITY_QUESTIONS, SecurityQuestionsCredentialsType.F_QUESTION_ANSWER);
            String useroid = getModelObject().getFocusOid();
            ObjectDelta<UserType> objectDelta = getPrismContext().deltaFactory().object()
                    .createModificationReplaceContainer(UserType.class, useroid,
                            path, answerTypeList.toArray(new SecurityQuestionAnswerType[answerTypeList.size()]));

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
            getPageBase().getModelService().executeChanges(deltas, null, task, result);

            success(getString("message.success"));
            target.add(getPageBase().getFeedbackPanel());
        } catch (Exception ex) {

            error(getString("message.error"));
            target.add(getPageBase().getFeedbackPanel());
            ex.printStackTrace();
        }
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

}
