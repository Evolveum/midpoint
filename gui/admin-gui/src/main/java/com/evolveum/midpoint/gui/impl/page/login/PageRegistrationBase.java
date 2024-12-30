/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;

import com.evolveum.midpoint.gui.impl.page.login.module.PageLogin;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.menu.top.LocaleTextPanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.evaluator.context.NonceAuthenticationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class PageRegistrationBase extends PageAdminLTE {

    private static final long serialVersionUID = 1L;
    private static final String DOT_CLASS = PageRegistrationBase.class.getName() + ".";
    private static final String OPERATION_GET_SECURITY_POLICY = DOT_CLASS + "getSecurityPolicy";

    protected static final String OPERATION_LOAD_DYNAMIC_FORM = DOT_CLASS + "loadDynamicForm";

    private static final Trace LOGGER = TraceManager.getTrace(PageRegistrationBase.class);
    private static final String ID_TITLE = "formTitle";
    private static final String ID_DESCRIPTION = "formDescription";
    private static final String ID_BACK = "back";

    private SelfRegistrationDto selfRegistrationDto;
    private SelfRegistrationDto postAuthenticationDto;

    public PageRegistrationBase() {
        super(null);

        initLayout();
    }

    private void initLayout() {
        Label header = new Label(ID_TITLE, createStringResource("PageSelfRegistration.welcome.message"));
        header.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getTitleModel().getObject())));
        header.setOutputMarkupId(true);
        add(header);

        Label description = new Label(ID_DESCRIPTION, getDescriptionModel());
        description.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getDescriptionModel().getObject())));
        description.setOutputMarkupId(true);
        add(description);

        addBackButton();
        addFeedbackPanel();

        add(new LocaleTextPanel("locale"));
    }

    protected IModel<String> getTitleModel() {
        return Model.of();
    }

    protected IModel<String> getDescriptionModel() {
        return Model.of();
    }

    private void addBackButton() {
        AjaxButton back = new AjaxButton(ID_BACK) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(getMidpointApplication().getHomePage());
            }
        };
        back.add(new VisibleEnableBehaviour(this::isBackButtonVisible));
        add(back);
    }

    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected void addDefaultBodyStyle(TransparentWebMarkupContainer body) {
        body.add(AttributeModifier.replace("class", "register-page py-3 fp-center"));
        body.add(AttributeModifier.remove("style")); //TODO hack :) because PageBase has min-height defined.
    }

    private void initSelfRegistrationConfiguration() {

        SecurityPolicyType securityPolicy = resolveSecurityPolicy();

        this.selfRegistrationDto = new SelfRegistrationDto();
        try {
            this.selfRegistrationDto.initSelfRegistrationDto(securityPolicy);
        } catch (SchemaException e) {
            LOGGER.error("Failed to initialize self registration configuration.", e);
            getSession().error(
                    createStringResource("PageSelfRegistration.selfRegistration.configuration.init.failed")
                            .getString());
            throw new RestartResponseException(PageLogin.class);
        }

    }

    private void initPostAuthenticationConfiguration() {

        SecurityPolicyType securityPolicy = resolveSecurityPolicy();

        this.postAuthenticationDto = new SelfRegistrationDto();
        try {
            this.postAuthenticationDto.initPostAuthenticationDto(securityPolicy);
        } catch (SchemaException e) {
            LOGGER.error("Failed to initialize self registration configuration.", e);
            getSession().error(
                    createStringResource("PageSelfRegistration.selfRegistration.configuration.init.failed")
                            .getString());
            throw new RestartResponseException(PageLogin.class);
        }

    }

    protected SecurityPolicyType resolveSecurityPolicy(Task task, OperationResult result) throws CommonException{
        return getModelInteractionService().getSecurityPolicyForArchetype(getArchetypeOid(), task, result);
    }

    protected final SecurityPolicyType resolveSecurityPolicy() {
        SecurityPolicyType securityPolicy = runPrivileged((Producer<SecurityPolicyType>) () -> {

            Task task = createAnonymousTask(OPERATION_GET_SECURITY_POLICY);
            task.setChannel(SchemaConstants.CHANNEL_SELF_REGISTRATION_URI);
            OperationResult result = new OperationResult(OPERATION_GET_SECURITY_POLICY);

            try {
                return resolveSecurityPolicy(task, result);
            } catch (CommonException e) {
                LOGGER.error("Could not retrieve security policy: {}", e.getMessage(), e);
                return null;
            }

        });

        if (securityPolicy == null) {
            LOGGER.error("No security policy defined.");
            getSession()
                    .error(createStringResource("PageSelfRegistrationOld.securityPolicy.notFound").getString());
            throw new RestartResponseException(PageLogin.class);
        }

        return securityPolicy;
    }

    protected String getArchetypeOid() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            return null;
        }
        return mpAuthentication.getArchetypeOid();
    }

    public SelfRegistrationDto getSelfRegistrationConfiguration() {

        if (selfRegistrationDto == null) {
            initSelfRegistrationConfiguration();
        }

        return selfRegistrationDto;

    }

    public SelfRegistrationDto getPostAuthenticationConfiguration() {

        if (postAuthenticationDto == null) {
            initPostAuthenticationConfiguration();
        }

        return postAuthenticationDto;

    }

    protected AjaxButton getBackButton() {
        return (AjaxButton) get(ID_BACK);
    }

    protected Label getDescription() {
        return (Label) get(ID_DESCRIPTION);
    }
}
