/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.PageMyPasswordQuestions;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordQuestionsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SecurityQuestionAnswerDTO;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

/**
 * @author lazyman
 */
public class UserMenuPanel extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(UserMenuPanel.class);
    private static final String ID_USERNAME_LINK = "usernameLink";
    private static final String ID_LOGOUT_LINK = "logoutLink";

    private static final String ID_USERNAME = "username";

    private static final String ID_EDIT_PROFILE = "editProfile";
    private static final String ID_PASSWORD_QUESTIONS = "passwordQuestions";
    private IModel<PasswordQuestionsDto> passwordQuestionsDtoIModel;
    private IModel<List<SecurityQuestionDefinitionType>> securityPolicyQuestionsModel;
//    private PrismObject<UserType> userModel;
    private Model<PrismObject<UserType>> userModel = new Model<PrismObject<UserType>>();
    private static final String DOT_CLASS = UserMenuPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loaduser";
    private static final String OPERATION_LOAD_QUESTION_POLICY = DOT_CLASS + "LOAD Question Policy";
    private static final String ID_ICON_BOX = "menuIconBox";
    private static final String ID_PHOTO = "menuPhoto";
    private static final String ID_ICON = "menuIcon";
    private static final String ID_PANEL_ICON_BOX = "menuPanelIconBox";
    private static final String ID_PANEL_PHOTO = "menuPanelPhoto";
    private static final String ID_PANEL_ICON = "menuPanelIcon";

    private boolean isUserModelLoaded = false;
    private boolean isPasswordModelLoaded = false;
    private  byte[] jpegPhoto = null;
    private List<SecurityQuestionDefinitionType> securityPolicyQuestions = new ArrayList<>();

    public UserMenuPanel(String id) {
        super(id);
        initLayout();
        if (!isPasswordModelLoaded) {
            passwordQuestionsDtoIModel = new LoadableModel<PasswordQuestionsDto>(false) {

                private static final long serialVersionUID = 1L;

                @Override
                protected PasswordQuestionsDto load() {
                    return loadModel(null);
                }
            };
            isPasswordModelLoaded = true;
        }
        securityPolicyQuestionsModel = new LoadableModel<List<SecurityQuestionDefinitionType>>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<SecurityQuestionDefinitionType> load() {
                return loadSecurityPloicyQuestionsModel();
            }
        };
    }

    private void initLayout() {
        WebMarkupContainer iconBox = new WebMarkupContainer(ID_ICON_BOX);
        add(iconBox);

        NonCachingImage img = new NonCachingImage(ID_PHOTO, new AbstractReadOnlyModel<AbstractResource>() {

            @Override
            public AbstractResource getObject() {
                if(jpegPhoto == null) {
                    return null;
                } else {
                    return new ByteArrayResource("image/jpeg",jpegPhoto);
                }
            }
        });
        img.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                if (userModel != null && userModel.getObject() == null){
                    loadModel(null);
                }
                return jpegPhoto != null;
            }
        });
        iconBox.add(img);

        ContextImage icon = new ContextImage(ID_ICON, "img/placeholder.png");
        icon.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                if (userModel != null && userModel.getObject() == null){
                    loadModel(null);
                }
                return jpegPhoto == null;


            }
        });
        iconBox.add(icon);

        Label usernameLink = new Label(ID_USERNAME_LINK, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getShortUserName();
            }
        });
        add(usernameLink);


        WebMarkupContainer panelIconBox = new WebMarkupContainer(ID_PANEL_ICON_BOX);
        add(panelIconBox);

        NonCachingImage panelImg = new NonCachingImage(ID_PANEL_PHOTO, new AbstractReadOnlyModel<AbstractResource>() {

            @Override
            public AbstractResource getObject() {
                if(jpegPhoto == null) {
                    return null;
                } else {
                    return new ByteArrayResource("image/jpeg",jpegPhoto);
                }
            }
        });
        panelImg.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                if (userModel != null && userModel.getObject() == null){
                    loadModel(null);
                }
                return jpegPhoto != null;
            }
        });
        panelIconBox.add(panelImg);

        ContextImage panelIcon = new ContextImage(ID_PANEL_ICON,"img/placeholder.png");
        panelIcon.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                if (userModel != null && userModel.getObject() == null){
                    loadModel(null);
                }
                return jpegPhoto == null;
            }
        });
        panelIconBox.add(panelIcon);

        Label username = new Label(ID_USERNAME, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getShortUserName();
            }
        });
        username.setRenderBodyOnly(true);
        add(username);

        ExternalLink logoutLink = new ExternalLink(ID_LOGOUT_LINK,
                new Model<>(RequestCycle.get().getRequest().getContextPath() + "/j_spring_security_logout"),
                createStringResource("UserMenuPanel.logout"));
        add(logoutLink);

        AjaxButton editPasswordQ = new AjaxButton(ID_PASSWORD_QUESTIONS,
                createStringResource("UserMenuPanel.editPasswordQuestions")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PageMyPasswordQuestions myPasswordQuestions = new PageMyPasswordQuestions(passwordQuestionsDtoIModel);
                setResponsePage(myPasswordQuestions);
            }

        };
        add(editPasswordQ);

        if (!isPasswordModelLoaded ){
            passwordQuestionsDtoIModel = new LoadableModel<PasswordQuestionsDto>(false) {

                private static final long serialVersionUID = 1L;

                @Override
                protected PasswordQuestionsDto load() {
                    return loadModel(null);
                }
            };
            isPasswordModelLoaded = true;
        }
        securityPolicyQuestionsModel = new LoadableModel<List<SecurityQuestionDefinitionType>>(false) {
            @Override
            protected List<SecurityQuestionDefinitionType> load() {
                return loadSecurityPloicyQuestionsModel();
            }
        };
        editPasswordQ.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                if (securityPolicyQuestionsModel == null || securityPolicyQuestionsModel.getObject() == null) {
                    loadSecurityPloicyQuestionsModel();
                }
                return hasQuestions() || (securityPolicyQuestionsModel.getObject() != null &&
                        securityPolicyQuestionsModel.getObject().size() > 0);
            }
        });
    }

    private String getShortUserName() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal == null) {
            return "Unknown";
        }

        if (principal instanceof MidPointPrincipal) {
            MidPointPrincipal princ = (MidPointPrincipal) principal;

            return WebComponentUtil.getOrigStringFromPoly(princ.getName());
        }

        return principal.toString();
    }

    private PasswordQuestionsDto loadModel(PageBase parentPage) {
        LOGGER.trace("Loading user for Security Question Page.");

        PasswordQuestionsDto dto =new PasswordQuestionsDto();
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);

        if (parentPage == null) {
        	parentPage = ((PageBase)getPage());
        }

        try {

        	MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        	if (principal == null) {
        		result.recordNotApplicableIfUnknown();
        		return null;
        	}
            String userOid = principal.getOid();
            Task task = parentPage.createSimpleTask(OPERATION_LOAD_USER);
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);

            Collection options = SelectorOptions.createCollection(UserType.F_JPEG_PHOTO,
                    GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
            PrismObject<UserType> user = parentPage.getModelService().getObject(UserType.class, userOid, options, task, subResult);
            userModel.setObject(user);
            jpegPhoto = user == null ? null :
                    (user.asObjectable() == null ? null : user.asObjectable().getJpegPhoto());
            dto.setSecurityAnswers(createUsersSecurityQuestionsList(user));

            subResult.recordSuccessIfUnknown();

        }
        catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get user Questions, Probably not set yet", ex);

        } finally {
            result.recomputeStatus();
            isUserModelLoaded = true;
        }
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
                Protector protector = ((PageBase) getPage()).getPrismContext().getDefaultProtector();
                if (securityQuestionAnswerType.getQuestionAnswer() != null && securityQuestionAnswerType.getQuestionAnswer().getEncryptedDataType() != null) {
                    try {
                    	String decoded = protector.decryptString(securityQuestionAnswerType.getQuestionAnswer());
                        secQuestAnswListDTO.add(new SecurityQuestionAnswerDTO(securityQuestionAnswerType
                                .getQuestionIdentifier(), decoded));
                    } catch (EncryptionException e) {
                        // TODO do we need to thrown exception here?
                    	LOGGER.error("Could not get security questions. Error: "  + e.getMessage(), e);
                        continue;
                    }
                }

            }

            return secQuestAnswListDTO;

        } else {
            return null;
        }

    }


    private List<SecurityQuestionDefinitionType> loadSecurityPloicyQuestionsModel() {
        List<SecurityQuestionDefinitionType> questionList = new ArrayList<SecurityQuestionDefinitionType>();
        OperationResult result = new OperationResult(OPERATION_LOAD_QUESTION_POLICY);
        try {
            Task task = ((PageBase) getPage()).createSimpleTask(OPERATION_LOAD_QUESTION_POLICY);
            CredentialsPolicyType credPolicy = ((PageBase) getPage()).getModelInteractionService().getCredentialsPolicy(null, task, result);
            if (credPolicy != null && credPolicy.getSecurityQuestions() != null) {
                // Actual Policy Question List
                questionList = credPolicy.getSecurityQuestions().getQuestion();
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't load system security policy" + ex.getMessage(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load system security policy", ex);
        }finally {
            result.computeStatus();
        }
        return questionList;
    }

    private boolean hasQuestions(){
        return passwordQuestionsDtoIModel != null &&
                (passwordQuestionsDtoIModel.getObject() != null &&
                        (passwordQuestionsDtoIModel.getObject().getPwdQuestion() != null
                                && !passwordQuestionsDtoIModel.getObject().getPwdQuestion().trim().equals("")));
    }
}
