/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;

import java.io.Serial;

@PanelType(name = "schema")
@PanelInstance(
        identifier = "schema",
        applicableForType = SchemaType.class,
        display = @PanelDisplay(label = "SchemaPanel.title", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 25
        ),
        containerPath = "prismSchema",
        type = "PrismSchemaType",
        expanded = true
)
public class SchemaPanel<S extends SchemaType, ADM extends AssignmentHolderDetailsModel<S>> extends AbstractObjectMainPanel<S, ADM> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PANEL = "panel";

    public SchemaPanel(String id, ADM model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel panel =
                new SingleContainerPanel<>(ID_PANEL,
                        PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.EMPTY_PATH),
                        getPanelConfiguration());
        add(panel);
    }
}
