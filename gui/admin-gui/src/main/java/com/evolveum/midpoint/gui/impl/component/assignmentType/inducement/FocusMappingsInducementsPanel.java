/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.assignmentType.inducement;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleInducementPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.model.IModel;

@PanelType(name = "focusMappingsInducements")
@PanelInstance(identifier = "focusMappingsInducements",
        applicableFor = AbstractRoleType.class,
        childOf = AbstractRoleInducementPanel.class)
@PanelDisplay(label = "Focus mappings", icon = GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, order = 80)
public class FocusMappingsInducementsPanel<AR extends AbstractRoleType> extends AbstractInducementPanel<AR> {

    public FocusMappingsInducementsPanel(String id, IModel<PrismObjectWrapper<AR>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }


}
