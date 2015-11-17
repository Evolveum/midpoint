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
import com.evolveum.midpoint.web.component.util.BaseSimplePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.home.PageMyPasswordQuestions;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordQuestionsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SecurityQuestionAnswerDTO;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;
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
//    private PrismObject<UserType> userModel;
    private Model<PrismObject<UserType>> userModel = new Model<PrismObject<UserType>>();
    private static final String DOT_CLASS = UserMenuPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loaduser";
    private static final String ID_ICON_BOX = "menuIconBox";
    private static final String ID_PHOTO = "menuPhoto";
    private static final String ID_ICON = "menuIcon";
    private static final String ID_PANEL_ICON_BOX = "menuPanelIconBox";
    private static final String ID_PANEL_PHOTO = "menuPanelPhoto";
    private static final String ID_PANEL_ICON = "menuPanelIcon";

    private boolean isUserModelLoaded = false;
    private boolean isPasswordModelLoaded = false;

    public UserMenuPanel(String id) {
        super(id);
//        if (!isUserModelLoaded) {
//            loadModel();
//        }
        if (!isPasswordModelLoaded) {
            passwordQuestionsDtoIModel = new LoadableModel<PasswordQuestionsDto>(false) {

                private static final long serialVersionUID = 1L;

                @Override
                protected PasswordQuestionsDto load() {
                    return loadModel();
                }
            };
//            isPasswordModelLoaded = true;
        }
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer iconBox = new WebMarkupContainer(ID_ICON_BOX);
        add(iconBox);

        Image img = new Image(ID_PHOTO, new AbstractReadOnlyModel<AbstractResource>() {

            @Override
            public AbstractResource getObject() {
                byte[] jpegPhoto = userModel.getObject().asObjectable().getJpegPhoto();
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
                byte [] photo = null;
                if (userModel != null && userModel.getObject() == null){
                    loadModel();
                    photo = userModel.getObject().asObjectable().getJpegPhoto();
                }
                return userModel == null ? false :
                        (userModel.getObject() == null ? false : photo != null);
            }
        });
        iconBox.add(img);

        Label icon = new Label(ID_ICON,"");
        icon.add(new AttributeModifier("src", "img/placeholder.png"));
        icon.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                if (userModel != null && userModel.getObject() == null){
                    loadModel();
                }
                return userModel == null ? false :
                        (userModel.getObject() == null ? false : userModel.getObject().asObjectable().getJpegPhoto() == null);


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

        Image panelImg = new Image(ID_PANEL_PHOTO, new AbstractReadOnlyModel<AbstractResource>() {

            @Override
            public AbstractResource getObject() {
                byte[] jpegPhoto = userModel.getObject().asObjectable().getJpegPhoto();
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
                    loadModel();
                }
                return userModel == null ? false :
                        (userModel.getObject() == null ? false : userModel.getObject().asObjectable().getJpegPhoto() != null);
            }
        });
        panelIconBox.add(panelImg);

        Label panelIcon = new Label(ID_PANEL_ICON,"");
        panelIcon.add(new AttributeModifier("src", "img/placeholder.png"));
        panelIcon.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                if (userModel != null && userModel.getObject() == null){
                    loadModel();
                }
                return userModel == null ? false :
                        (userModel.getObject() == null ? false : userModel.getObject().asObjectable().getJpegPhoto() == null);
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

        if (!isUserModelLoaded) {
            loadModel();

        }
        if (!isPasswordModelLoaded ){
            passwordQuestionsDtoIModel = new LoadableModel<PasswordQuestionsDto>(false) {

                private static final long serialVersionUID = 1L;

                @Override
                protected PasswordQuestionsDto load() {
                    return loadModel();
                }
            };
            isPasswordModelLoaded = true;
        }
        if (passwordQuestionsDtoIModel != null &&
                (passwordQuestionsDtoIModel.getObject() == null ||
                ((passwordQuestionsDtoIModel.getObject().getPwdQuestion() == null
                        || passwordQuestionsDtoIModel.getObject().getPwdQuestion().trim().equals(""))
                        && (passwordQuestionsDtoIModel.getObject().getSecurityAnswers() == null
                        || passwordQuestionsDtoIModel.getObject().getSecurityAnswers().size() == 0)
                        && (passwordQuestionsDtoIModel.getObject().getPwdAnswer() == null
                        || passwordQuestionsDtoIModel.getObject().getPwdAnswer().trim().equals(""))))) {
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

            Collection options = SelectorOptions.createCollection(UserType.F_JPEG_PHOTO,
                    GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
            PrismObject<UserType> user = ((PageBase)getPage()).getModelService().getObject(UserType.class, userOid, options, task, subResult);
            userModel.setObject(user);

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
