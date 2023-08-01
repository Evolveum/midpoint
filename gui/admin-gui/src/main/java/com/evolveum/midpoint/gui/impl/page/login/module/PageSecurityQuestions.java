/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.authentication.BadCredentialsException;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.CredentialModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.login.PageAbstractAuthenticationModule;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.security.util.SecurityQuestionDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/securityquestions", matchUrlForSecurity = "/securityquestions")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.SECURITY_QUESTIONS_FORM)
public class PageSecurityQuestions extends PageAbstractAuthenticationModule<CredentialModuleAuthentication> {
    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSecurityQuestions.class);

    private static final String ID_USERNAME = "username";
    private static final String ID_USER = "user";
    private static final String ID_ANSWER_FIELD = "answer";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_INSIDE_FORM = "insideForm";
    private static final String ID_FIRST_LEVEL_BUTTONS = "firstLevelButtons";
    private static final String ID_SHOW_QUESTIONS_BUTTON = "showQuestions";
    private static final String ID_QUESTIONS = "questions";
    private static final String ID_QUESTION_TEXT = "questionText";
    private static final String ID_QUESTION_ANSWER = "questionAnswer";

    private IModel<String> answerModel;
    private LoadableModel<List<SecurityQuestionDto>> questionsModel;
    private LoadableDetachableModel<UserType> userModel;


    public PageSecurityQuestions() {
        initModels();
    }

//    @Override
    protected void initModels() {
        answerModel = Model.of();
        userModel = new LoadableDetachableModel<>() {
            @Override
            protected UserType load() {
                MidPointPrincipal principal = AuthUtil.getPrincipalUser();
                return principal != null ? (UserType) principal.getFocus() : PageSecurityQuestions.this.searchUser();
            }
        };
        questionsModel = new LoadableModel<>(false) {
            @Override
            protected List<SecurityQuestionDto> load() {
                try {
                    return createUsersSecurityQuestionsList();
                } catch (BadCredentialsException e) {
                    LOGGER.debug(getString(e.getMessage()));
                    saveException(e);
                    throw new RestartResponseException(getMidpointApplication().getHomePage());
                }
            }
        };
    }

    @Override
    protected UserType searchUser() {
        if (StringUtils.isEmpty(getUsernameFieldValue())) {
            return null;
        }
        return super.searchUser();
    }

    @Override
    protected void initModuleLayout(MidpointForm form) {
        initStaticLayout(form);

        initButtons(form);

        initQuestionsSection(form);

        initSendingInformation(form);

    }


    private void initSendingInformation(MidpointForm<?> form) {
        HiddenField<String> answer = new HiddenField<>(ID_ANSWER_FIELD, answerModel);
        answer.setOutputMarkupId(true);
        form.add(answer);

        HiddenField<String> username = new HiddenField<>(ID_USER, new Model<> ());
        username.setOutputMarkupId(true);
        form.add(username);
    }

    private void initQuestionsSection(MidpointForm<?> form) {
        WebMarkupContainer questionsContainer = new WebMarkupContainer(ID_INSIDE_FORM);
        questionsContainer.setOutputMarkupId(true);
        questionsContainer.add(new VisibleBehaviour(() -> userModel.getObject() != null));
        form.add(questionsContainer);

        ListView<SecurityQuestionDto> questionsView = new ListView<>(ID_QUESTIONS, questionsModel) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<SecurityQuestionDto> item) {
                Label questionText = new Label(ID_QUESTION_TEXT, new PropertyModel<String>(item.getModel(), "questionText"));
                item.add(questionText);
                RequiredTextField<String> questionAnswer = new RequiredTextField<>(ID_QUESTION_ANSWER, new PropertyModel<>(item.getModel(), "questionAnswer"));
                questionAnswer.setOutputMarkupId(true);
                questionAnswer.add(new AjaxFormComponentUpdatingBehavior("blur") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        answerModel.setObject(generateAnswer());
                        target.add(getHiddenAnswer());
                    }
                });
                item.add(questionAnswer);
            }
        };
        questionsView.setOutputMarkupId(true);
        questionsContainer.add(questionsView);

    }

    private String generateAnswer() {
        JSONArray answers = new JSONArray();
        for (SecurityQuestionDto question : questionsModel.getObject()) {
            if (StringUtils.isNotBlank(question.getQuestionAnswer())) {
                JSONObject json  = new JSONObject();
                json.put(AuthConstants.SEC_QUESTION_J_QID, question.getIdentifier());
                json.put(AuthConstants.SEC_QUESTION_J_QANS, question.getQuestionAnswer());
                answers.put(json);
            }
        }
        if (answers.length() == 0) {
            return null;
        }
        return answers.toString();
    }

    private void initButtons(MidpointForm<?> form) {
        WebMarkupContainer firstLevelButtonContainer = new WebMarkupContainer(ID_FIRST_LEVEL_BUTTONS);
        firstLevelButtonContainer.setOutputMarkupId(true);
        form.add(firstLevelButtonContainer);

        AjaxButton showQuestion = new AjaxButton(ID_SHOW_QUESTIONS_BUTTON) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showQuestions(target);
            }
        };
        firstLevelButtonContainer.add(showQuestion);
    }

    private void initStaticLayout(MidpointForm<?> form) {
        RequiredTextField<String> visibleUsername = new RequiredTextField<>(ID_USERNAME, Model.of());
        visibleUsername.setOutputMarkupId(true);
        visibleUsername.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        form.add(visibleUsername);
    }

    private void showQuestions(AjaxRequestTarget target) {
        userModel.detach();
        UserType user = userModel.getObject();

        if (user == null) {
            getSession().error(getString("pageForgetPassword.message.user.not.found"));
            throw new RestartResponseException(PageSecurityQuestions.class);
        }
        LOGGER.trace("Reset Password user: {}", user);
        getHiddenUsername().getModel().setObject(user.getName().getOrig());
        target.add(getMainForm());
    }

    private List<SecurityQuestionDto> createUsersSecurityQuestionsList() throws BadCredentialsException {
        UserType user = searchUser();//userModel.getObject();

        if (user == null) {
            return new ArrayList<>();
        }

        SecurityQuestionsCredentialsType credentialsPolicyType = user.getCredentials()
                .getSecurityQuestions();
        if (credentialsPolicyType == null
                || credentialsPolicyType.getQuestionAnswer() == null
                || credentialsPolicyType.getQuestionAnswer().isEmpty()) {
            // This is better than "web.security.flexAuth.any.security.questions", because it is specific to the context
            // of resetting the password through security questions. (At least I suppose these questions are used only for
            // this reason.)
            throw new BadCredentialsException("pageForgetPassword.message.ContactAdminQuestionsNotSet");
        }
        List<SecurityQuestionAnswerType> secQuestAnsList = credentialsPolicyType.getQuestionAnswer();

        SecurityPolicyType securityPolicy = resolveSecurityPolicy(user.asPrismObject());
        LOGGER.trace("Found security policy: {}", securityPolicy);

        if (securityPolicy == null) {
            LOGGER.error("No security policy, cannot process security questions");
            // Just log the error, but do not display it. We are still in unprivileged part of the web
            // we do not want to provide any information to the attacker.
            throw new RestartResponseException(PageError.class);
        }
        if (securityPolicy.getCredentials() == null) {
            LOGGER.error("No credential for security policy, cannot process security questions");
            // Just log the error, but do not display it. We are still in unprivileged part of the web
            // we do not want to provide any information to the attacker.
            throw new RestartResponseException(PageError.class);
        }

        SecurityQuestionsCredentialsPolicyType secQuestionsPolicy = securityPolicy.getCredentials().getSecurityQuestions();

        List<SecurityQuestionDefinitionType> questionList = secQuestionsPolicy != null ? secQuestionsPolicy.getQuestion() : new ArrayList<>();

        List<SecurityQuestionDto> questionsDto = new ArrayList<>();
        int questionNumber = secQuestionsPolicy != null && secQuestionsPolicy.getQuestionNumber() != null ? secQuestionsPolicy.getQuestionNumber() : 1;
        for (SecurityQuestionDefinitionType question : questionList) {
            if (!Boolean.FALSE.equals(question.isEnabled())) {
                for (SecurityQuestionAnswerType userAnswer : secQuestAnsList) {
                    if (question.getIdentifier().equals(userAnswer.getQuestionIdentifier())) {
                        SecurityQuestionDto questionDto = new SecurityQuestionDto(question.getIdentifier());
                        questionDto.setQuestionText(question.getQuestionText());
                        questionsDto.add(questionDto);
                        break;
                    }
                }
            }
            if (questionNumber == questionsDto.size()) {
                break;
            }
        }
        if (questionsDto.size() < questionNumber) {
            String key = "pageForgetPassword.message.ContactAdminQuestionsNotSetEnough";
            throw new BadCredentialsException(key);
        }

        return questionsDto;
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    private String getUsernameFieldValue() {
        RequiredTextField<String> usernameTextFiled = getVisibleUsername();
        return usernameTextFiled != null ? usernameTextFiled.getModelObject() : null;
    }

    private MidpointForm<?> getMainForm() {
        return (MidpointForm) get(ID_MAIN_FORM);
    }

    private HiddenField<String> getHiddenUsername(){
        return (HiddenField) getMainForm().get(ID_USER);
    }

    private HiddenField<String> getHiddenAnswer(){
        return (HiddenField) getMainForm().get(ID_ANSWER_FIELD);
    }

    private RequiredTextField getVisibleUsername(){
        return (RequiredTextField) getMainForm().get(ID_USERNAME);
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource("PageSecurityQuestions.questions");
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource("PageSecurityQuestions.description");
    }

}
