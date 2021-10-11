/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.page.forgetpassword.PageForgotPassword;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.filter.SecurityQuestionsAuthenticationFilter;
import com.evolveum.midpoint.web.security.module.authentication.SecurityQuestionFormModuleAuthentication;
import com.evolveum.midpoint.web.security.util.SecurityQuestionDto;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
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
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;

/**
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/securityquestions", matchUrlForSecurity = "/securityquestions")
}, permitAll = true, loginPage = true)
public class PageSecurityQuestions extends PageAuthenticationBase {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSecurityQuestions.class);

    private static final String DOT_CLASS = com.evolveum.midpoint.web.page.forgetpassword.PageSecurityQuestions.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loaduser";
    private static final String OPERATION_LOAD_QUESTION_POLICY = DOT_CLASS + "LOAD Question Policy";

    private static final String ID_STATIC_LAYOUT = "staticLayout";
    private static final String ID_USERNAME = "username";
    private static final String ID_DYNAMIC_LAYOUT = "dynamicLayout";
    private static final String ID_DYNAMIC_FORM = "dynamicForm";
    private static final String ID_USER = "user";
    private static final String ID_CSRF_FIELD = "csrfField";
    private static final String ID_ANSWER_FIELD = "answer";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_INSIDE_FORM = "insideForm";
    private static final String ID_FIRST_LEVEL_BUTTONS = "firstLevelButtons";
    private static final String ID_BACK_1_BUTTON = "back1";
    private static final String ID_SHOW_QUESTIONS_BUTTON = "showQuestions";
    private static final String ID_QUESTIONS = "questions";
    private static final String ID_QUESTION_TEXT = "questionText";
    private static final String ID_QUESTION_ANSWER = "questionAnswer";
    private static final String ID_BACK_2_BUTTON = "back2";

    private IModel<String> answerModel;
    private IModel<List<SecurityQuestionDto>> questionsModel;
    private boolean showedQuestions = false;

    public PageSecurityQuestions() {
        answerModel = Model.of();
        questionsModel = new ListModel<SecurityQuestionDto>(new ArrayList<SecurityQuestionDto>());
        initLayout();
    }

    private void initLayout() {
        Form form = new Form(ID_MAIN_FORM);
        form.add(AttributeModifier.replace("action", new IModel<String>() {
            @Override
            public String getObject() {
                return getUrlProcessingLogin();
            }
        }));
        add(form);

        initStaticLayout(form);

        initDynamicLayout(form, PageSecurityQuestions.this);

        initButtons(form);

        initQuestionsSection(form);

        initSendingInformation(form);

    }

    private void initSendingInformation(Form form) {
        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);

        HiddenField<String> answer = new HiddenField(ID_ANSWER_FIELD, answerModel);
        answer.setOutputMarkupId(true);
        form.add(answer);

        HiddenField<String> username = new HiddenField(ID_USER, new Model<String> ());
        username.setOutputMarkupId(true);
        form.add(username);
    }

    private void initQuestionsSection(Form form) {
        WebMarkupContainer questionsContainer = new WebMarkupContainer(ID_INSIDE_FORM);
        questionsContainer.setOutputMarkupId(true);
        questionsContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return showedQuestions;
            }
        });
        form.add(questionsContainer);

        ListView<SecurityQuestionDto> questionsView = new ListView<SecurityQuestionDto>(ID_QUESTIONS, questionsModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<SecurityQuestionDto> item) {
                SecurityQuestionDto question = item.getModelObject();

                Label questionText = new Label(ID_QUESTION_TEXT, new PropertyModel<String>(item.getModel(), "questionText"));
                item.add(questionText);
                RequiredTextField<String> questionAnswer = new RequiredTextField<>(ID_QUESTION_ANSWER, new PropertyModel<String>(item.getModel(), "questionAnswer"));
                questionAnswer.setOutputMarkupId(true);
                questionAnswer.add(new AjaxFormComponentUpdatingBehavior("blur") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        String answer = generateAnswer();
                        answerModel.setObject(answer);
                        target.add(getHiddenAnswer());
                    }
                });
                item.add(questionAnswer);
            }
        };
        questionsView.setOutputMarkupId(true);
        questionsContainer.add(questionsView);

        AjaxButton back = new AjaxButton(ID_BACK_2_BUTTON) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showedQuestions = false;
                questionsModel.setObject(new ArrayList<SecurityQuestionDto>());
                getHiddenUsername().getModel().setObject(null);
                getHiddenAnswer().getModel().setObject(null);
                target.add(getMainForm());
            }
        };
        questionsContainer.add(back);
    }

    private String generateAnswer() {
        JSONArray answers = new JSONArray();
        for (SecurityQuestionDto question : questionsModel.getObject()) {
            if (StringUtils.isNotBlank(question.getQuestionAnswer())) {
                JSONObject json  = new JSONObject();
                json.put(SecurityQuestionsAuthenticationFilter.J_QID, question.getIdentifier());
                json.put(SecurityQuestionsAuthenticationFilter.J_QANS, question.getQuestionAnswer());
                answers.put(json);
            }
        }
        if (answers.length() == 0) {
            return null;
        }
        return answers.toString();
    }

    private void initButtons(Form form) {

        WebMarkupContainer firstLevelButtonContainer = new WebMarkupContainer(ID_FIRST_LEVEL_BUTTONS);
        firstLevelButtonContainer.setOutputMarkupId(true);
        firstLevelButtonContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !showedQuestions;
            }
        });
        form.add(firstLevelButtonContainer);

        AjaxSubmitButton showQuestion = new AjaxSubmitButton(ID_SHOW_QUESTIONS_BUTTON) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                showQuestions(target);
            }
        };
        firstLevelButtonContainer.add(showQuestion);
        firstLevelButtonContainer.add(createBackButton(ID_BACK_1_BUTTON));
    }

    private void initStaticLayout(Form form) {

        WebMarkupContainer staticLayout = new WebMarkupContainer(ID_STATIC_LAYOUT);
        staticLayout.setOutputMarkupId(true);
        staticLayout.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !showedQuestions && !isDynamicForm();
            }
        });
        form.add(staticLayout);

        RequiredTextField<String> visibleUsername = new RequiredTextField<>(ID_USERNAME, new Model<>());
        visibleUsername.setOutputMarkupId(true);
        staticLayout.add(visibleUsername);
    }

    private void showQuestions(AjaxRequestTarget target) {
        UserType user = searchUser();

        if (user == null) {
            getSession().error(getString("pageForgetPassword.message.user.not.found"));
            throw new RestartResponseException(PageSecurityQuestions.class);
        }
        LOGGER.trace("Reset Password user: {}", user);

        List<SecurityQuestionDto> questions = createUsersSecurityQuestionsList(user.asPrismObject());

        if (!questions.isEmpty()) {
            showedQuestions = true;
            questionsModel.setObject(questions);
            getHiddenUsername().getModel().setObject(user.getName().getOrig());
            target.add(getMainForm());
        }
    }

    private List<SecurityQuestionDto> createUsersSecurityQuestionsList(PrismObject<UserType> user) {

        SecurityQuestionsCredentialsType credentialsPolicyType = user.asObjectable().getCredentials()
                .getSecurityQuestions();
        if (credentialsPolicyType == null || credentialsPolicyType.getQuestionAnswer() == null
                || credentialsPolicyType.getQuestionAnswer().isEmpty()) {
            String key = "web.security.flexAuth.any.security.questions";
            error(getString(key));
            LOGGER.error(key);
            throw new RestartResponseException(PageSecurityQuestions.class);
        }
        List<SecurityQuestionAnswerType> secQuestAnsList = credentialsPolicyType.getQuestionAnswer();

        SecurityPolicyType securityPolicy = resolveSecurityPolicy(user);
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

        List<SecurityQuestionDefinitionType> questionList = secQuestionsPolicy != null ? secQuestionsPolicy.getQuestion() : new ArrayList<SecurityQuestionDefinitionType>();

        List<SecurityQuestionDto> questionsDto = new ArrayList<SecurityQuestionDto>();
        int questionNumber = secQuestionsPolicy != null ? secQuestionsPolicy.getQuestionNumber() : 1;
        for (SecurityQuestionDefinitionType question : questionList) {
            if (Boolean.TRUE.equals(question.isEnabled())) {
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
            error(getString(key));
            LOGGER.error(key);
            throw new RestartResponseException(PageSecurityQuestions.class);
        }

        return questionsDto;
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    protected ObjectQuery createStaticFormQuery() {
        RequiredTextField<String> usernameTextFiled = getVisibleUsername();
        String username = usernameTextFiled != null ? usernameTextFiled.getModelObject() : null;
        LOGGER.debug("Reset Password user info form submitted. username={}", username);

//        ResetPolicyDto resetPasswordPolicy = getResetPasswordPolicy();
//        if (resetPasswordPolicy == null) {
//            passwordResetNotSupported();
//        }

        return getPrismContext().queryFor(UserType.class).item(UserType.F_NAME)
                .eqPoly(username).matchingNorm().build();

    }

//    private void passwordResetNotSupported() {
//        getSession().error(getString("PageForgotPassword.unsupported.reset.type"));
//        throw new RestartResponseException(PageForgotPassword.this);
//    }

    private Form getMainForm() {
        return (Form) get(ID_MAIN_FORM);
    }

    private HiddenField getHiddenUsername(){
        return (HiddenField) getMainForm().get(ID_USER);
    }

    private HiddenField getHiddenAnswer(){
        return (HiddenField) getMainForm().get(ID_ANSWER_FIELD);
    }

    protected DynamicFormPanel getDynamicForm(){
        return (DynamicFormPanel) getMainForm().get(createComponentPath(ID_DYNAMIC_LAYOUT, ID_DYNAMIC_FORM));
    }

    private RequiredTextField getVisibleUsername(){
        return (RequiredTextField) getMainForm().get(createComponentPath(ID_STATIC_LAYOUT, ID_USERNAME));
    }

    private String getUrlProcessingLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null && moduleAuthentication instanceof SecurityQuestionFormModuleAuthentication){
                String prefix = ((SecurityQuestionFormModuleAuthentication) moduleAuthentication).getPrefix();
                return stripSlashes(prefix) + "/spring_security_login";
            }
        }

        String key = "web.security.flexAuth.unsupported.auth.type";
        error(getString(key));
        return "/midpoint/spring_security_login";
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        ServletWebRequest req = (ServletWebRequest) RequestCycle.get().getRequest();
        HttpServletRequest httpReq = req.getContainerRequest();
        HttpSession httpSession = httpReq.getSession();

        Exception ex = (Exception) httpSession.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (ex == null) {
            return;
        }

        String msg = ex.getMessage();
        if (StringUtils.isEmpty(msg)) {
            msg = "web.security.provider.unavailable";
        }

        String[] msgs = msg.split(";");
        for (String message : msgs) {
            message = getLocalizationService().translate(message, null, getLocale(), message);
            error(message);
        }

        httpSession.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        clearBreadcrumbs();
    }

    @Override
    protected void createBreadcrumb() {
        //don't create breadcrumb for login page
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if (SecurityUtils.getPrincipalUser() != null) {
            MidPointApplication app = getMidpointApplication();
            throw new RestartResponseException(app.getHomePage());
        }
    }
}
