/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
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
public class ShadowPanel extends BasePanel<ShadowWrapper> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowPanel.class);

    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ASSOCIATIONS = "associations";
    private static final String ID_ACTIVATION = "activation";
    private static final String ID_PASSWORD = "password";
    private static final String ID_POLICY_STATEMENT = "policyStatement";

    private ContainerPanelConfigurationType config;
    private IModel<ResourceType> resourceModel;

    public ShadowPanel(String id, IModel<ShadowWrapper> model, ContainerPanelConfigurationType config) {
        super(id, model);
        this.config = config;
    }

    public ShadowPanel(String id, IModel<ShadowWrapper> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);

        resourceModel = new LoadableDetachableModel<>() {

            @Override
            protected ResourceType load() {
                ShadowWrapper shadowWrapper = getModelObject();
                PrismReference resourceRef = shadowWrapper.getObject().findReference(ShadowType.F_RESOURCE_REF);
                if (resourceRef == null) {
                    return null;
                }
                PrismReferenceValue resourceRefVal = resourceRef.getValue();
                if (resourceRefVal == null || resourceRefVal.getOid() == null) {
                    return null;
                }
                if (resourceRefVal.getObject() != null) {
                    return (ResourceType) resourceRefVal.getObject().asObjectable();
                }

                //TODO wouldn't be noFetch enough?
                PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(resourceRefVal.asReferencable(), getPageBase());
                if (resource == null) {
                    return null;
                }
                return resource.asObjectable();
            }
        };
    }

    private void initLayout() {


        try {

            if (config != null) {
                SingleContainerPanel attributesContainer = new SingleContainerPanel(ID_ATTRIBUTES, getModel(), config) {

                    @Override
                    protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                        return checkShadowContainerVisibility(itemWrapper, ShadowPanel.this.getModel());
                    }
                };
                add(attributesContainer);
            } else {
                ItemPanelSettingsBuilder attributesSettingsBuilder = new ItemPanelSettingsBuilder()
                        .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
                    Panel attributesPanel = getPageBase().initItemPanel(ID_ATTRIBUTES, ShadowAttributesType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_ATTRIBUTES),
                        attributesSettingsBuilder.build());
                add(attributesPanel);
            }

            ItemPanelSettingsBuilder associationBuilder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
            Panel associationsPanel = getPageBase().initItemPanel(ID_ASSOCIATIONS, ShadowAssociationType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_ASSOCIATION),
                    associationBuilder.build());
            associationsPanel.add(new VisibleBehaviour(() -> checkAssociationsVisibility()));
            add(associationsPanel);

            ItemPanelSettingsBuilder activationBuilder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
            Panel activationPanel = getPageBase().initItemPanel(ID_ACTIVATION, ActivationType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowType.F_ACTIVATION),
                    activationBuilder.build());
            activationPanel.add(new VisibleBehaviour(() -> isActivationSupported()));
            add(activationPanel);

            ItemPanelSettingsBuilder passwordSettingsBuilder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
            Panel passwordPanel = getPageBase().initItemPanel(ID_PASSWORD, PasswordType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModel(), ItemPath.create(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD)),
                    passwordSettingsBuilder.build());
            passwordPanel.add(new VisibleBehaviour(() -> isCredentialsSupported()));
            add(passwordPanel);

            ItemPanelSettingsBuilder markSettingsBuilder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper, getModel()));
            Panel markPanel = getPageBase().initItemPanel(ID_POLICY_STATEMENT, PolicyStatementType.COMPLEX_TYPE,
                    PrismContainerWrapperModel.fromContainerWrapper(getModel(), ItemPath.create(ShadowType.F_POLICY_STATEMENT)),
                    markSettingsBuilder.build());
            markPanel.add(new VisibleBehaviour(() -> isPolicyStatementSupported()));
            add(markPanel);
        } catch (SchemaException e) {
            getSession().error("Cannot create panels for shadow, reason: " + e.getMessage());
            LOGGER.trace("Cannot create panels for shadow, reason: {}", e.getMessage(), e);

        }
    }

    private ItemVisibility checkShadowContainerVisibility(ItemWrapper itemWrapper, IModel<ShadowWrapper> model) {

        ShadowType shadowType = model.getObject().getObjectOld().asObjectable();
        return ProvisioningObjectsUtil.checkShadowActivationAndPasswordVisibility(itemWrapper, shadowType);
    }

    private boolean checkAssociationsVisibility() {

        ShadowType shadowType = getModelObject().getObjectOld().asObjectable();

        return ProvisioningObjectsUtil.isAssociationSupported(shadowType, resourceModel);

    }

    private boolean isActivationSupported() {
        ShadowType shadowType = getModelObject().getObjectOld().asObjectable();
        return ProvisioningObjectsUtil.isActivationSupported(shadowType, resourceModel);
    }

    private boolean isCredentialsSupported() {
        ShadowType shadowType = getModelObject().getObjectOld().asObjectable();
        return ProvisioningObjectsUtil.isPasswordSupported(shadowType, resourceModel);
    }

    private boolean isPolicyStatementSupported() {
        return getPageBase().getRepositoryService().supportsMarks();
    }
}
