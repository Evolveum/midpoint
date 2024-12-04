/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.shadow;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import org.apache.wicket.model.LoadableDetachableModel;

/**
 * @author skublik
 *
 */
public class ShadowBasicPanel extends AbstractShadowPanel {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowBasicPanel.class);

    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ACTIVATION = "activation";
    private static final String ID_PASSWORD = "password";
    private static final String ID_BEHAVIOR = "behavior";
//    private static final String ID_POLICY_STATEMENT = "policyStatement";

    private ContainerPanelConfigurationType config;

    public ShadowBasicPanel(String id, IModel<ShadowWrapper> model, ContainerPanelConfigurationType config) {
        super(id, model);
        this.config = config;
    }

    public ShadowBasicPanel(String id, IModel<ShadowWrapper> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        try {
            if (config != null) {
                SingleContainerPanel attributesContainer = new SingleContainerPanel(ID_ATTRIBUTES, getModel(), config) {

                    @Override
                    protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                        return checkShadowContainerVisibility(itemWrapper, ShadowBasicPanel.this.getModel());
                    }
                };
                add(attributesContainer);
            } else {
                ItemPanelSettingsBuilder attributesSettingsBuilder = new ItemPanelSettingsBuilder()
                        .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
                Panel attributesPanel = getPageBase().initItemPanel(
                        ID_ATTRIBUTES,
                        ShadowAttributesType.COMPLEX_TYPE,
                        PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_ATTRIBUTES),
                        attributesSettingsBuilder.build());
                add(attributesPanel);
            }

            ItemPanelSettingsBuilder activationBuilder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
            Panel activationPanel = getPageBase().initItemPanel(
                    ID_ACTIVATION,
                    ActivationType.COMPLEX_TYPE,
                    PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_ACTIVATION),
                    activationBuilder.build());
            activationPanel.add(new VisibleBehaviour(() -> isActivationSupported()));
            add(activationPanel);

            ItemPanelSettingsBuilder passwordSettingsBuilder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
            Panel passwordPanel = getPageBase().initItemPanel(
                    ID_PASSWORD,
                    PasswordType.COMPLEX_TYPE,
                    PrismContainerWrapperModel.fromContainerWrapper(getModel(), ItemPath.create(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD)),
                    passwordSettingsBuilder.build());
            passwordPanel.add(new VisibleBehaviour(() -> isCredentialsSupported()));
            add(passwordPanel);

            ItemPanelSettingsBuilder behaviorBuilder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
            Panel behaviorPanel = getPageBase().initItemPanel(
                    ID_BEHAVIOR,
                    ShadowBehaviorType.COMPLEX_TYPE,
                    PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_BEHAVIOR),
                    behaviorBuilder.build());
            add(behaviorPanel);

//            ItemPanelSettingsBuilder markSettingsBuilder = new ItemPanelSettingsBuilder()
//                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
//            Panel markPanel = getPageBase().initItemPanel(ID_POLICY_STATEMENT, PolicyStatementType.COMPLEX_TYPE,
//                    PrismContainerWrapperModel.fromContainerWrapper(getModel(), ItemPath.create(ShadowType.F_POLICY_STATEMENT)),
//                    markSettingsBuilder.build());
//            markPanel.add(new VisibleBehaviour(() -> isPolicyStatementSupported()));
//            add(markPanel);
        } catch (SchemaException e) {
            getSession().error("Cannot create panels for shadow, reason: " + e.getMessage());
            LOGGER.trace("Cannot create panels for shadow, reason: {}", e.getMessage(), e);

        }
    }

    private ItemVisibility checkShadowContainerVisibility(ItemWrapper itemWrapper, IModel<ShadowWrapper> model) {

        ShadowType shadowType = model.getObject().getObjectOld().asObjectable();
        return ProvisioningObjectsUtil.checkShadowActivationAndPasswordVisibility(itemWrapper, shadowType);
    }

    private boolean isActivationSupported() {
        ShadowType shadowType = getModelObject().getObjectOld().asObjectable();
        return ProvisioningObjectsUtil.isActivationSupported(shadowType, getResourceModel());
    }

    private boolean isCredentialsSupported() {
        ShadowType shadowType = getModelObject().getObjectOld().asObjectable();
        return ProvisioningObjectsUtil.isPasswordSupported(shadowType, getResourceModel());
    }

    private boolean isPolicyStatementSupported() {
        return getPageBase().getRepositoryService().supportsMarks();
    }
}
