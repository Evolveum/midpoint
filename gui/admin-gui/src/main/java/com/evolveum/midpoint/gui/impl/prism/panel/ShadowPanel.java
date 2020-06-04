/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author skublik
 *
 */
public class ShadowPanel extends BasePanel<ShadowWrapper> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowPanel.class);

    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ASSOCIATIONS = "associations";
    private static final String ID_ACTIVATION = "activation";
    private static final String ID_PASSWORD = "password";


    public ShadowPanel(String id, IModel<ShadowWrapper> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {


        try {

            long attributesStart = System.currentTimeMillis();
            ItemPanelSettingsBuilder attributesSettingsBuilder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
                Panel attributesPanel = getPageBase().initItemPanel(ID_ATTRIBUTES, ShadowAttributesType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_ATTRIBUTES),
                    attributesSettingsBuilder.build());
            add(attributesPanel);
            long attributesEnd = System.currentTimeMillis();
            LOGGER.info("Attributes finished in {} ms", attributesEnd - attributesStart);

            long associationStart = System.currentTimeMillis();
            ItemPanelSettingsBuilder associationBuilder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
            Panel associationsPanel = getPageBase().initItemPanel(ID_ASSOCIATIONS, ShadowAssociationType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_ASSOCIATION),
                    associationBuilder.build());
            associationsPanel.add(new VisibleBehaviour(() -> checkAssociationsVisibility()));
            add(associationsPanel);
            long associationEnd = System.currentTimeMillis();
            LOGGER.info("Association finished in {} ms", associationEnd - associationStart);

            long activationStart = System.currentTimeMillis();
            ItemPanelSettingsBuilder activationBuilder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
            Panel activationPanel = getPageBase().initItemPanel(ID_ACTIVATION, ActivationType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_ACTIVATION),
                    activationBuilder.build());
            activationPanel.add(new VisibleBehaviour(() -> isActivationSupported()));
            add(activationPanel);
            long activationEnd = System.currentTimeMillis();
            LOGGER.info("Activation finished in {} ms", activationEnd - activationStart);

            long passwordStart = System.currentTimeMillis();
            ItemPanelSettingsBuilder passwordSettingsBuilder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
            Panel passwordPanel = getPageBase().initItemPanel(ID_PASSWORD, PasswordType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ItemPath.create(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD)),
                    passwordSettingsBuilder.build());
            passwordPanel.add(new VisibleBehaviour(() -> isCredentialsSupported()));
            add(passwordPanel);
            long passwordEnd = System.currentTimeMillis();
            LOGGER.info("Password finished in {} ms", passwordEnd - passwordStart);
        } catch (SchemaException e) {
            getSession().error("Cannot create panels for shadow, reason: " + e.getMessage());
            LOGGER.error("Cannot create panels for shadow, reason: {}", e.getMessage(), e);

        }
    }

    private ItemVisibility checkShadowContainerVisibility(ItemWrapper itemWrapper, IModel<ShadowWrapper> model) {

        ShadowType shadowType = model.getObject().getObjectOld().asObjectable();
        return WebComponentUtil.checkShadowActivationAndPasswordVisibility(itemWrapper, shadowType);
    }

    private boolean checkAssociationsVisibility() {

        ShadowType shadowType = getModelObject().getObjectOld().asObjectable();
        return WebComponentUtil.isAssociationSupported(shadowType);

    }

    private boolean isActivationSupported() {
        ShadowType shadowType = getModelObject().getObjectOld().asObjectable();
        return WebComponentUtil.isActivationSupported(shadowType);
    }

    private boolean isCredentialsSupported() {
        ShadowType shadowType = getModelObject().getObjectOld().asObjectable();
        return WebComponentUtil.isPasswordSupported(shadowType);
    }
}
