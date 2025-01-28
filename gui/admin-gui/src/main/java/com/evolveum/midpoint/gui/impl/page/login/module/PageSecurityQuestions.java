/*
 * Copyright (c) 2010-2023 Evolveum and contributors
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
import org.apache.commons.collections4.CollectionUtils;
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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.authentication.BadCredentialsException;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.CredentialModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
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

    private static final String ID_USER = "user";
    private static final String ID_ANSWER_FIELD = "answer";
    private static final String ID_INSIDE_FORM = "insideForm";
    private static final String ID_QUESTIONS = "questions";
    private static final String ID_QUESTION_TEXT = "questionText";
    private static final String ID_QUESTION_ANSWER = "questionAnswer";

    private IModel<String> answerModel;
    private LoadableModel<List<SecurityQuestionDto>> questionsModel;

    public PageSecurityQuestions() {
        initModels();
    }

    protected void initModels() {
        answerModel = Model.of();
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
    protected void initModuleLayout(MidpointForm form) {
        validateUserNotNullOrFail(searchUser());
        initQuestionsSection(form);
        initSendingInformation(form);
    }


    private void initSendingInformation(MidpointForm<?> form) {
        HiddenField<String> answer = new HiddenField<>(ID_ANSWER_FIELD, answerModel);
        answer.setOutputMarkupId(true);
        form.add(answer);

        //TODO do we need this? user was identifier before, so we already know
        HiddenField<String> username = new HiddenField<>(ID_USER, new Model<> ());
        username.setOutputMarkupId(true);
        form.add(username);
    }

    private void initQuestionsSection(MidpointForm<?> form) {
        WebMarkupContainer questionsContainer = new WebMarkupContainer(ID_INSIDE_FORM);
        questionsContainer.setOutputMarkupId(true);
        questionsContainer.add(new VisibleBehaviour(() -> searchUser() != null));
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

    private List<SecurityQuestionDto> createUsersSecurityQuestionsList() throws BadCredentialsException {
        UserType user = searchUser();

        if (user == null) {
            //TODO probably we should throw an exception
            return new ArrayList<>();
        }

        if (shouldThrowException(user.getCredentials())) {
            // This is better than "web.security.flexAuth.any.security.questions", because it is specific to the context
            // of resetting the password through security questions. (At least I suppose these questions are used only for
            // this reason.)
            throw new BadCredentialsException("pageForgetPassword.message.ContactAdminQuestionsNotSet");
        }
        SecurityQuestionsCredentialsType credentialsPolicyType = user.getCredentials()
                .getSecurityQuestions();
        List<SecurityQuestionAnswerType> secQuestAnsList = credentialsPolicyType.getQuestionAnswer();

        SecurityPolicyType securityPolicy = resolveSecurityPolicy(user.asPrismObject());
        SecurityQuestionsCredentialsPolicyType secQuestionsPolicy = SecurityUtil.getEffectiveSecurityQuestionsCredentialsPolicy(securityPolicy);
        LOGGER.trace("Found security questions policy: {}", secQuestionsPolicy);

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

    private boolean shouldThrowException(CredentialsType credentials) {
        return credentials == null || credentials.getSecurityQuestions() == null
                || CollectionUtils.isEmpty(credentials.getSecurityQuestions().getQuestionAnswer());
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }


    private HiddenField<String> getHiddenAnswer(){
        return (HiddenField) getForm().get(ID_ANSWER_FIELD);
    }

    @Override
    protected IModel<String> getDefaultLoginPanelTitleModel() {
        return createStringResource("PageSecurityQuestions.questions");
    }

    @Override
    protected IModel<String> getDefaultLoginPanelDescriptionModel() {
        return createStringResource("PageSecurityQuestions.description");
    }

    @Override
    protected String getModuleTypeName() {
        return "securityQuestionsForm";
    }
}
