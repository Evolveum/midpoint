/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.shadow;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;

import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerValuePanel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
public class ShadowAssociationValuePanel extends BasePanel<PrismContainerValueWrapper<ShadowAssociationValueType>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_OBJECTS = "objects";
//    private static final String ID_ACTIVATION = "activation";

    public ShadowAssociationValuePanel(String id, IModel<PrismContainerValueWrapper<ShadowAssociationValueType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {
        ItemPanelSettingsBuilder attributesSettingsBuilder = new ItemPanelSettingsBuilder()
                .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper));

        try {
            WebMarkupContainer attributesPanel;
            if (getModelObject().findContainer(ShadowAssociationValueType.F_ATTRIBUTES) == null) {
                attributesPanel = new WebMarkupContainer(ID_ATTRIBUTES);
                attributesPanel.add(VisibleBehaviour.ALWAYS_INVISIBLE);
            } else {
                attributesPanel = new VerticalFormPrismContainerValuePanel<>(
                        ID_ATTRIBUTES,
                        PrismContainerValueWrapperModel.fromContainerValueWrapper(getModel(), ShadowAssociationValueType.F_ATTRIBUTES),
                        attributesSettingsBuilder.build());
            }
            add(attributesPanel);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        ItemPanelSettingsBuilder associationBuilder = new ItemPanelSettingsBuilder()
                .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper));
        VerticalFormPrismContainerValuePanel associationsPanel = new VerticalFormPrismContainerValuePanel(
                ID_OBJECTS,
                PrismContainerValueWrapperModel.fromContainerValueWrapper(getModel(), ShadowAssociationValueType.F_OBJECTS),
                associationBuilder.build());
        add(associationsPanel);

//            ItemPanelSettingsBuilder activationBuilder = new ItemPanelSettingsBuilder()
//                    .visibilityHandler(itemWrapper -> checkShadowContainerVisibility(itemWrapper));
//            VerticalFormDefaultContainerablePanel activationPanel = new VerticalFormDefaultContainerablePanel(
//                    ID_ACTIVATION,
//                    PrismContainerWrapperModel.fromContainerWrapper(getModel(), ShadowAssociationValueType.F_ACTIVATION),
//                    activationBuilder.build());
//            activationPanel.add(new VisibleBehaviour(() -> isActivationSupported()));
//            add(activationPanel);
    }

    private ItemVisibility checkShadowContainerVisibility(ItemWrapper itemWrapper) {

        ShadowType shadowType = (ShadowType) getModelObject().getParent().findObjectWrapper().getObjectOld().asObjectable();
        return ProvisioningObjectsUtil.checkShadowActivationAndPasswordVisibility(itemWrapper, shadowType);
    }

//    private boolean checkAssociationsVisibility() {
//
//        ShadowType shadowType = getModelObject().getObjectOld().asObjectable();
//
//        return ProvisioningObjectsUtil.isAssociationSupported(shadowType, resourceModel);
//
//    }

//    private boolean isActivationSupported() {
//        ShadowType shadowType = (ShadowType) getModelObject().findObjectWrapper().getObjectOld().asObjectable();
//        return ProvisioningObjectsUtil.isActivationSupported(shadowType, resourceModel);
//    }

}
