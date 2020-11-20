/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.PageMyPasswordQuestions;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordQuestionsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SecurityQuestionAnswerDTO;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 */
public class UserMenuPanel extends BasePanel<UserMenuPanel> {

    private static final Trace LOGGER = TraceManager.getTrace(UserMenuPanel.class);

    private static final String DOT_CLASS = UserMenuPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loaduser";

    private static final String ID_USERNAME_LINK = "usernameLink";
    private static final String ID_LOGOUT_FORM = "logoutForm";
    private static final String ID_CSRF_FIELD = "csrfField";
    private static final String ID_USERNAME = "username";
    private static final String ID_FOCUS_TYPE = "focusType";
    private static final String ID_PASSWORD_QUESTIONS = "passwordQuestions";
    private static final String ID_ICON_BOX = "menuIconBox";
    private static final String ID_PHOTO = "menuPhoto";
    private static final String ID_PANEL_ICON_BOX = "menuPanelIconBox";
    private static final String ID_PANEL_PHOTO = "menuPanelPhoto";

    private final IModel<PrismObject<UserType>> userModel;

    public UserMenuPanel(String id) {
        super(id);

        userModel = new LoadableModel<PrismObject<UserType>>(false) {

            @Override
            protected PrismObject<UserType> load() {
                return loadUser();
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer iconBox = new WebMarkupContainer(ID_ICON_BOX);
        add(iconBox);

        IModel<AbstractResource> jpegPhotoModel = loadJpegPhotoModel();

        NonCachingImage img = new NonCachingImage(ID_PHOTO, jpegPhotoModel);
        iconBox.add(img);

        Label usernameLink = new Label(ID_USERNAME_LINK, (IModel<String>) this::getShortUserName);
        add(usernameLink);

        WebMarkupContainer panelIconBox = new WebMarkupContainer(ID_PANEL_ICON_BOX);
        add(panelIconBox);

        NonCachingImage panelImg = new NonCachingImage(ID_PANEL_PHOTO, jpegPhotoModel);
        panelIconBox.add(panelImg);

        Label username = new Label(ID_USERNAME, (IModel<String>) this::getShortUserName);
        username.setRenderBodyOnly(true);
        add(username);

        Label focusType = new Label(ID_FOCUS_TYPE, getPageBase().createStringResource("PageTemplate." + getFocusType()));
        add(focusType);

        MidpointForm<?> form = new MidpointForm<>(ID_LOGOUT_FORM);
        form.add(AttributeModifier.replace("action",
                (IModel<String>) () -> SecurityUtils.getPathForLogoutWithContextPath(
                        getRequest().getContextPath(), getAuthenticatedModule())));
        add(form);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);

        AjaxButton editPasswordQ = new AjaxButton(ID_PASSWORD_QUESTIONS,
                createStringResource("UserMenuPanel.editPasswordQuestions")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PageMyPasswordQuestions myPasswordQuestions = new PageMyPasswordQuestions(Model.of(getPasswordQuestions()));
                setResponsePage(myPasswordQuestions);
            }

        };
        editPasswordQ.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return hasQuestions() || (CollectionUtils.isNotEmpty(getSecurityQuestions()));
            }
        });
        add(editPasswordQ);


//        securityPolicyQuestionsModel = new LoadableModel<List<SecurityQuestionDefinitionType>>(false) {
//            @Override
//            protected List<SecurityQuestionDefinitionType> load() {
//                return loadSecurityPolicyQuestionsModel();
//            }
//        };
//
    }

    private IModel<AbstractResource> loadJpegPhotoModel() {
        return new ReadOnlyModel<>(() -> {
            if (userModel == null || userModel.getObject() == null) {
                return null;
            }

            byte[] jpegPhoto = userModel.getObject().asObjectable().getJpegPhoto();

            if (jpegPhoto == null) {
                URL placeholder = UserMenuPanel.class.getClassLoader().getResource("static/img/placeholder.png");
                if (placeholder == null) {
                    return null;
                }
                try {
                    jpegPhoto = IOUtils.toByteArray(placeholder);
                } catch (IOException e) {
                    LOGGER.error("Cannot load placeholder for photo.");
                    return null;
                }
//                ByteArrayResource("image/png", new InputStream());
//                return new ContextRelativeResource("img/placeholder.png");
            }

            return new ByteArrayResource("image/jpeg", jpegPhoto);
        });
    }

    private ModuleAuthentication getAuthenticatedModule() {
        ModuleAuthentication moduleAuthentication = SecurityUtils.getAuthenticatedModule();

        if (moduleAuthentication == null) {
            String message = "Unauthenticated request";
            throw new IllegalArgumentException(message);
        }
        return moduleAuthentication;
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

    private String getFocusType() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "Unknown";
        }
        Object principal = authentication.getPrincipal();

        if (principal == null || principal.equals("anonymousUser")) {
            return "Unknown";
        }

        QName type = WebComponentUtil.classToQName(getPageBase().getPrismContext(), WebModelServiceUtils.getLoggedInFocus().getClass());
        return type.getLocalPart();
    }

    private PasswordQuestionsDto getPasswordQuestions() {
        PasswordQuestionsDto dto = new PasswordQuestionsDto();
        dto.setSecurityAnswers(createUsersSecurityQuestionsList());
        return dto;
    }

    //TODO this should be probably parametrized to FocusType
    private PrismObject<UserType> loadUser() {
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);

        PageBase parentPage = getPageBase();

        try {

            GuiProfiledPrincipal principal = SecurityUtils.getPrincipalUser();
            if (principal == null) {
                result.recordNotApplicableIfUnknown();
                return null;
            }
            String userOid = principal.getOid();
            Task task = parentPage.createSimpleTask(OPERATION_LOAD_USER);
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);

            Collection<SelectorOptions<GetOperationOptions>> options =
                    getSchemaHelper().getOperationOptionsBuilder()
                            .item(UserType.F_JPEG_PHOTO).retrieve()
                            .build();
            PrismObject<UserType> user =
                    parentPage.getModelService().getObject(
                            UserType.class, userOid, options, task, subResult);
            subResult.recordSuccessIfUnknown();

            return user;
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get user Questions, Probably not set yet", ex);

        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    public List<SecurityQuestionAnswerDTO> createUsersSecurityQuestionsList() {
        GuiProfiledPrincipal principal = SecurityUtils.getPrincipalUser();
        if (principal == null) {
            return null;
        }

        FocusType focus = principal.getFocus();
        if (focus == null) {
            return null;
        }
        CredentialsType credentialsType = focus.getCredentials();
        if (credentialsType == null) {
            return null;
        }

        SecurityQuestionsCredentialsType credentialsPolicyType = credentialsType.getSecurityQuestions();
        if (credentialsPolicyType == null) {
            return null;
        }
        List<SecurityQuestionAnswerType> secQuestAnsList = credentialsPolicyType.getQuestionAnswer();

        if (secQuestAnsList != null) {
            List<SecurityQuestionAnswerDTO> secQuestAnswListDTO = new ArrayList<>();
            for (SecurityQuestionAnswerType securityQuestionAnswerType : secQuestAnsList) {
                Protector protector = ((PageBase) getPage()).getPrismContext().getDefaultProtector();
                if (securityQuestionAnswerType.getQuestionAnswer() != null && securityQuestionAnswerType.getQuestionAnswer().getEncryptedDataType() != null) {
                    try {
                        String decoded = protector.decryptString(securityQuestionAnswerType.getQuestionAnswer());
                        secQuestAnswListDTO.add(new SecurityQuestionAnswerDTO(securityQuestionAnswerType
                                .getQuestionIdentifier(), decoded));
                    } catch (EncryptionException e) {
                        // TODO do we need to thrown exception here?
                        LOGGER.error("Could not get security questions. Error: " + e.getMessage(), e);
                    }
                }
            }

            return secQuestAnswListDTO;
        } else {
            return null;
        }
    }

    private List<SecurityQuestionDefinitionType> getSecurityQuestions() {
            GuiProfiledPrincipal principal = SecurityUtils.getPrincipalUser();

            CredentialsPolicyType credentialsPolicyType = principal.getApplicableSecurityPolicy().getCredentials();
            if (credentialsPolicyType == null) {
                return Collections.emptyList();
            }
            SecurityQuestionsCredentialsPolicyType securityQuestionsPolicy = credentialsPolicyType.getSecurityQuestions();
            if (securityQuestionsPolicy == null) {
                return Collections.emptyList();
            }

            return securityQuestionsPolicy.getQuestion();
    }

    private boolean hasQuestions() {
        PasswordQuestionsDto passwordQuestionsDto = getPasswordQuestions();
        return passwordQuestionsDto != null
                && passwordQuestionsDto.getPwdQuestion() != null
                && !passwordQuestionsDto.getPwdQuestion().trim().equals("");
    }
}
