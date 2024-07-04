/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.shadow;

import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ShadowDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@PanelType(name = "shadowBasic", defaultContainerPath = "attributes")
@PanelInstance(
        identifier = "shadowBasic",
        applicableForType = ShadowType.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "pageAdminFocus.basic", order = 10
        )
)
public class ShadowAssociationMenuPanel extends AbstractObjectMainPanel<ShadowType, ShadowDetailsModel> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ACCOUNT = "account";

    public ShadowAssociationMenuPanel(String id, ShadowDetailsModel objectWrapperModel, ContainerPanelConfigurationType config) {
        super(id, objectWrapperModel, config);
    }

    protected void initLayout() {
        ShadowAssociationsPanel shadowPanel = new ShadowAssociationsPanel(
                ID_ACCOUNT,
                PrismContainerValueWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ShadowType.F_ASSOCIATIONS),
                () -> (ShadowWrapper) getObjectWrapperModel().getObject());
        add(shadowPanel);
    }

    @Override
    protected void onInitialize() {
        add(new VisibleBehaviour(this::isAssociationsVisible));
    }

    private boolean isAssociationsVisible() {
        ShadowType shadowType = getObjectWrapper().getObjectOld().asObjectable();
        return ProvisioningObjectsUtil.isAssociationSupported(
                shadowType,
                () -> WebModelServiceUtils.loadResource(getObjectWrapper(), getPageBase()));

    }

}
