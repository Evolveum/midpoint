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

    protected static final String ID_SHADOWS_CONTAINER = "shadowContainer";


    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ASSOCIATIONS = "associations";
    private static final String ID_ACTIVATION = "activation";
    private static final String ID_PASSWORD = "password";
    private static final String ID_ERROR = "error";


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

            ItemHeaderPanel.ItemPanelSettingsBuilder attributesSettingsBuilder = new ItemHeaderPanel.ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
                Panel attributesPanel = getPageBase().initItemPanel(ID_ATTRIBUTES, ShadowAttributesType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_ATTRIBUTES),
                    attributesSettingsBuilder.build());
            add(attributesPanel);

            ItemHeaderPanel.ItemPanelSettingsBuilder associationBuilder = new ItemHeaderPanel.ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()))
                    .showOnTopLevel(true);
            Panel associationsPanel = getPageBase().initItemPanel(ID_ASSOCIATIONS, ShadowAssociationType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_ASSOCIATION),
                    associationBuilder.build());
            associationsPanel.add(new VisibleBehaviour(() -> checkAssociationsVisibility()));
            add(associationsPanel);


            ItemHeaderPanel.ItemPanelSettingsBuilder activationBuilder = new ItemHeaderPanel.ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()))
                    .showOnTopLevel(true);
            Panel activationPanel = getPageBase().initItemPanel(ID_ACTIVATION, ActivationType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_ACTIVATION),
                    activationBuilder.build());
            activationPanel.add(new VisibleBehaviour(() -> isActivationSupported()));
            add(activationPanel);

            ItemHeaderPanel.ItemPanelSettingsBuilder passwordSettingsBuilder = new ItemHeaderPanel.ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()))
                    .showOnTopLevel(true);
            Panel passwordPanel = getPageBase().initItemPanel(ID_PASSWORD, PasswordType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ItemPath.create(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD)),
                    passwordSettingsBuilder.build());
            passwordPanel.add(new VisibleBehaviour(() -> isCredentialsSupported()));
            add(passwordPanel);
        } catch (SchemaException e) {
            getSession().error("Cannot create panels for shadow, reason: " + e.getMessage());
            LOGGER.error("Cannot create panels for shadow, reason: {}", e.getMessage(), e);
//            ErrorPanel errorPanel = new ErrorPanel(ID_ERROR, createStringResource("Error creatinf shadow panels"));
//            specificContainers.add(errorPanel);
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
