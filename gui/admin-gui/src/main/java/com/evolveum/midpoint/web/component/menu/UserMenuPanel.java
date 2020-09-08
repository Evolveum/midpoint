/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
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
    private static final String OPERATION_LOAD_QUESTION_POLICY = DOT_CLASS + "LOAD Question Policy";

    private static final String ID_USERNAME_LINK = "usernameLink";
    private static final String ID_LOGOUT_FORM = "logoutForm";
    private static final String ID_CSRF_FIELD = "csrfField";
    private static final String ID_USERNAME = "username";
    private static final String ID_FOCUS_TYPE = "focusType";
    private static final String ID_PASSWORD_QUESTIONS = "passwordQuestions";
    private static final String ID_ICON_BOX = "menuIconBox";
    private static final String ID_PHOTO = "menuPhoto";
    private static final String ID_ICON = "menuIcon";
    private static final String ID_PANEL_ICON_BOX = "menuPanelIconBox";
    private static final String ID_PANEL_PHOTO = "menuPanelPhoto";
    private static final String ID_PANEL_ICON = "menuPanelIcon";

    private final Model<PrismObject<UserType>> userModel = new Model<>();
    private final PageBase pageBase;

    private IModel<PasswordQuestionsDto> passwordQuestionsDtoIModel;
    private IModel<List<SecurityQuestionDefinitionType>> securityPolicyQuestionsModel;
    private boolean isPasswordModelLoaded = false;
    private byte[] jpegPhoto = null;

    public UserMenuPanel(String id, PageBase pageBase) {
        super(id);
        this.pageBase = pageBase;
        initLayout();
        if (!isPasswordModelLoaded) {
            passwordQuestionsDtoIModel = new LoadableModel<PasswordQuestionsDto>(false) {

                private static final long serialVersionUID = 1L;

                @Override
                protected PasswordQuestionsDto load() {
                    return loadModel();
                }
            };
            isPasswordModelLoaded = true;
        }
        securityPolicyQuestionsModel = new LoadableModel<List<SecurityQuestionDefinitionType>>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<SecurityQuestionDefinitionType> load() {
                return loadSecurityPolicyQuestionsModel();
            }
        };
    }

    @Override
    public PageBase getPageBase() {
        return pageBase;
    }

    private void initLayout() {
        WebMarkupContainer iconBox = new WebMarkupContainer(ID_ICON_BOX);
        add(iconBox);

        NonCachingImage img = new NonCachingImage(ID_PHOTO,
                (IModel<AbstractResource>) () -> jpegPhoto != null
                        ? new ByteArrayResource("image/jpeg", jpegPhoto)
                        : null);
        img.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                if (userModel != null && userModel.getObject() == null) {
                    loadModel();
                }
                return jpegPhoto != null;
            }
        });
        iconBox.add(img);

        ContextImage icon = new ContextImage(ID_ICON, "img/placeholder.png");
        icon.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                if (userModel != null && userModel.getObject() == null) {
                    loadModel();
                }
                return jpegPhoto == null;

            }
        });
        iconBox.add(icon);

        Label usernameLink = new Label(ID_USERNAME_LINK, (IModel<String>) () -> getShortUserName());
        add(usernameLink);

        WebMarkupContainer panelIconBox = new WebMarkupContainer(ID_PANEL_ICON_BOX);
        add(panelIconBox);

        NonCachingImage panelImg = new NonCachingImage(ID_PANEL_PHOTO,
                (IModel<AbstractResource>) () -> jpegPhoto != null
                        ? new ByteArrayResource("image/jpeg", jpegPhoto)
                        : null);
        panelImg.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                if (userModel != null && userModel.getObject() == null) {
                    loadModel();
                }
                return jpegPhoto != null;
            }
        });
        panelIconBox.add(panelImg);

        ContextImage panelIcon = new ContextImage(ID_PANEL_ICON, "img/placeholder.png");
        panelIcon.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                if (userModel != null && userModel.getObject() == null) {
                    loadModel();
                }
                return jpegPhoto == null;
            }
        });
        panelIconBox.add(panelIcon);

        Label username = new Label(ID_USERNAME, (IModel<String>) () -> getShortUserName());
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
                PageMyPasswordQuestions myPasswordQuestions = new PageMyPasswordQuestions(passwordQuestionsDtoIModel);
                setResponsePage(myPasswordQuestions);
            }

        };
        add(editPasswordQ);

        if (!isPasswordModelLoaded) {
            passwordQuestionsDtoIModel = new LoadableModel<PasswordQuestionsDto>(false) {

                private static final long serialVersionUID = 1L;

                @Override
                protected PasswordQuestionsDto load() {
                    return loadModel();
                }
            };
            isPasswordModelLoaded = true;
        }
        securityPolicyQuestionsModel = new LoadableModel<List<SecurityQuestionDefinitionType>>(false) {
            @Override
            protected List<SecurityQuestionDefinitionType> load() {
                return loadSecurityPolicyQuestionsModel();
            }
        };
        editPasswordQ.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                if (securityPolicyQuestionsModel == null || securityPolicyQuestionsModel.getObject() == null) {
                    loadSecurityPolicyQuestionsModel();
                }
                return hasQuestions() || (securityPolicyQuestionsModel.getObject() != null &&
                        securityPolicyQuestionsModel.getObject().size() > 0);
            }
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

    private PasswordQuestionsDto loadModel() {
        LOGGER.trace("Loading user for Security Question Page.");

        PasswordQuestionsDto dto = new PasswordQuestionsDto();
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);

        PageBase parentPage = ((PageBase) getPage());

        try {

            MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
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
            userModel.setObject(user);
            user.asObjectable();
            jpegPhoto = user.asObjectable().getJpegPhoto();
            dto.setSecurityAnswers(createUsersSecurityQuestionsList(user));

            subResult.recordSuccessIfUnknown();

        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get user Questions, Probably not set yet", ex);

        } finally {
            result.recomputeStatus();
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

    private List<SecurityQuestionDefinitionType> loadSecurityPolicyQuestionsModel() {
        List<SecurityQuestionDefinitionType> questionList = new ArrayList<>();
        OperationResult result = new OperationResult(OPERATION_LOAD_QUESTION_POLICY);
        try {
            Task task = ((PageBase) getPage()).createSimpleTask(OPERATION_LOAD_QUESTION_POLICY);
            CredentialsPolicyType credPolicy = ((PageBase) getPage()).getModelInteractionService().getCredentialsPolicy(null, task, result);
            if (credPolicy != null && credPolicy.getSecurityQuestions() != null) {
                // Actual Policy Question List
                questionList = credPolicy.getSecurityQuestions().getQuestion();
            }
        } catch (Exception ex) {
            result.recordFatalError(createStringResource("UserMenuPanel.message.loadSecurityPolicyQuestionsModel.fatalError", ex.getMessage()).getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load system security policy", ex);
        } finally {
            result.computeStatus();
        }
        return questionList;
    }

    private boolean hasQuestions() {
        return passwordQuestionsDtoIModel != null
                && passwordQuestionsDtoIModel.getObject() != null
                && passwordQuestionsDtoIModel.getObject().getPwdQuestion() != null
                && !passwordQuestionsDtoIModel.getObject().getPwdQuestion().trim().equals("");
    }
}
