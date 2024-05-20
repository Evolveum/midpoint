/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerValuePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.common.prism_schema_3.ComplexTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.prism_schema_3.PrismSchemaType;

import org.apache.wicket.model.IModel;

import java.io.Serial;

@PanelType(name = "complex-type-definition-basic")
//@PanelInstance(
//        identifier = "complex-type-definition-basic",
//        applicableForType = SchemaType.class,
//        childOf = ComplexTypeDefinitionPanelList.class,
//        display = @PanelDisplay(label = "ComplexTypeDefinitionBasicPanel.title", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 25
//        ),
//        containerPath = "prismSchema/complexType",
//        type = "ComplexTypeDefinitionType",
//        expanded = true
//)
public class ComplexTypeDefinitionBasicPanel<S extends SchemaType, ADM extends AssignmentHolderDetailsModel<S>> extends AbstractObjectMainPanel<S, ADM> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PANEL = "panel";

    private final IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> valueModel;

    public ComplexTypeDefinitionBasicPanel(String id, ADM model,
            IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> valueModel, ContainerPanelConfigurationType config) {
        super(id, model, config);
        this.valueModel = valueModel;
    }

    @Override
    protected void initLayout() {
        add(getPageBase().initContainerValuePanel(ID_PANEL, valueModel, new ItemPanelSettingsBuilder().build()));
    }
}
