/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ItemDefinitionDto;
import com.evolveum.midpoint.web.component.input.TextPanel;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.SchemaDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ComplexTypeDefinitionDto;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.DefinitionDto;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.EnumerationTypeDefinitionDto;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaExtensionType;

import org.apache.wicket.model.PropertyModel;

@PanelType(name = "schemaPanel")
@PanelInstance(identifier = "schemaPanel",
        applicableForType = SchemaExtensionType.class,
        display = @PanelDisplay(label = "SchemaPanel.title", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 25))
public class SchemaPanel<T extends DefinitionDto> extends AbstractObjectMainPanel<SchemaExtensionType, SchemaDetailsModel> {

    private static final String ID_DEFS = "defs";

    public SchemaPanel(String id, SchemaDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }



    @Override
    protected void initLayout() {

        TextPanel<String> namespace = new TextPanel<>("namespace", getObjectDetailsModels().getNamespaceModel());
        namespace.add(new EnableBehaviour(() -> !getObjectDetailsModels().isEditObject()));
        add(namespace);

        ListView<T> list = new ListView<>(ID_DEFS, getObjectDetailsModels().getSchemaModel()) {
            @Override
            protected void populateItem(ListItem<T> item) {
                if (item.getModelObject() instanceof ComplexTypeDefinitionDto) {
                    ComplexTypeDefinitionPanel panel = new ComplexTypeDefinitionPanel("definition", (IModel<ComplexTypeDefinitionDto>) item.getModel());
                    item.add(panel);
                } else if (item.getModelObject() instanceof EnumerationTypeDefinitionDto) {
                    EnumerationTypeDefinitionPanel panel = new EnumerationTypeDefinitionPanel("definition", (IModel<EnumerationTypeDefinitionDto>) item.getModel());
                    item.add(panel);
                } else {
                    DefinitionPanel panel = new DefinitionPanel("definition", item.getModel());
                    item.add(panel);
                }
            }

        };
        add(list);
    }

}
