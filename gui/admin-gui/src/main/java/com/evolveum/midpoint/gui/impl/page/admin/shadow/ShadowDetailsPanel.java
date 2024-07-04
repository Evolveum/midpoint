/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.shadow;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ShadowDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "shadowBasic", defaultContainerPath = "attributes")
@PanelInstance(
        identifier = "shadowBasic",
        applicableForType = ShadowType.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "pageAdminFocus.basic", order = 10
        )
)
public class ShadowDetailsPanel extends AbstractObjectMainPanel<ShadowType, ShadowDetailsModel> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ACCOUNT = "account";

    public ShadowDetailsPanel(String id, ShadowDetailsModel objectWrapperModel, ContainerPanelConfigurationType config) {
        super(id, objectWrapperModel, config);
    }

    protected void initLayout() {
        ShadowBasicPanel shadowPanel = new ShadowBasicPanel(ID_ACCOUNT, (IModel) getObjectWrapperModel(), getPanelConfiguration());
        add(shadowPanel);
    }
}
