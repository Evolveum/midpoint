/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.BaseSimplePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.home.PageMyPasswordQuestions;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordQuestionsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SecurityQuestionAnswerDTO;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class UserMenuPanel extends BaseSimplePanel {

    private static final Trace LOGGER = TraceManager.getTrace(UserMenuPanel.class);
    private static final String ID_USERNAME_LINK = "usernameLink";
    private static final String ID_LOGOUT_LINK = "logoutLink";

    private static final String ID_USERNAME = "username";

    private static final String ID_EDIT_PROFILE = "editProfile";
    private static final String ID_PASSWORD_QUESTIONS = "passwordQuestions";
    private IModel<PasswordQuestionsDto> passwordQuestionsDtoIModel;
    private static final String DOT_CLASS = UserMenuPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loaduser";

    private boolean isModelLoaded = false;

    public UserMenuPanel(String id) {
        super(id);
        if (!isModelLoaded) {
            passwordQuestionsDtoIModel = new LoadableModel<PasswordQuestionsDto>(false) {

                private static final long serialVersionUID = 1L;

                @Override
                protected PasswordQuestionsDto load() {
                    return loadModel();
                }
            };
        }
    }

    @Override
    protected void initLayout() {
        Label usernameLink = new Label(ID_USERNAME_LINK, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getShortUserName();
            }
        });
        add(usernameLink);

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

        if (!isModelLoaded) {
            passwordQuestionsDtoIModel = new LoadableModel<PasswordQuestionsDto>(false) {

                private static final long serialVersionUID = 1L;

                @Override
                protected PasswordQuestionsDto load() {
                    return loadModel();
                }
            };
        }
        if (passwordQuestionsDtoIModel.getObject() == null ||
                ((passwordQuestionsDtoIModel.getObject().getPwdQuestion() == null
                        || passwordQuestionsDtoIModel.getObject().getPwdQuestion().trim().equals(""))
                        && (passwordQuestionsDtoIModel.getObject().getSecurityAnswers() == null
                        || passwordQuestionsDtoIModel.getObject().getSecurityAnswers().size() == 0)
                        && (passwordQuestionsDtoIModel.getObject().getPwdAnswer() == null
                        || passwordQuestionsDtoIModel.getObject().getPwdAnswer().trim().equals("")))) {
            editPasswordQ.setVisible(false);
        }
    }

    private String getShortUserName() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal == null) {
            return "Unknown";
        }

        if (principal instanceof MidPointPrincipal) {
            MidPointPrincipal princ = (MidPointPrincipal) principal;

            return WebMiscUtil.getOrigStringFromPoly(princ.getName());
        }

        return principal.toString();
    }

    private PasswordQuestionsDto loadModel() {
        LOGGER.debug("Loading user for Security Question Page.");

        PasswordQuestionsDto dto =new PasswordQuestionsDto();
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        try{


            String userOid = SecurityUtils.getPrincipalUser().getOid();
            Task task = ((PageBase)getPage()).createSimpleTask(OPERATION_LOAD_USER);
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);

            PrismObject<UserType> user = ((PageBase)getPage()).getModelService().getObject(UserType.class, userOid, null, task, subResult);

            dto.setSecurityAnswers(createUsersSecurityQuestionsList(user));

            subResult.recordSuccessIfUnknown();

        }
        catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get user Questions, Probably not set yet", ex);

        } finally {
            result.recomputeStatus();
            isModelLoaded = true;
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
                // System.out.println(securityQuestionAnswerType.getQuestionIdentifier());
                Protector protector = ((PageBase) getPage()).getPrismContext().getDefaultProtector();
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


}
